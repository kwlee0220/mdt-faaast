package mdt.persistence.old;

import java.nio.charset.StandardCharsets;

import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import lombok.Getter;
import lombok.experimental.Accessors;

import utils.async.Guard;
import utils.stream.FStream;

import mdt.client.instance.HttpMDTInstanceManager;
import mdt.client.instance.MDTInstanceStatusSubscriber;
import mdt.client.support.AutoReconnectingMqttClient;
import mdt.model.instance.InstanceStatusChangeEvent;
import mdt.model.sm.data.Data;
import mdt.model.sm.data.DataInfo;
import mdt.model.sm.data.DefaultData;
import mdt.model.sm.value.ElementValues;
import mdt.persistence.mqtt.MqttBrokerConfig;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.config.CoreConfig;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationInitializationException;
import de.fraunhofer.iosb.ilt.faaast.service.model.SubmodelElementIdentifier;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.modifier.QueryModifier;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.operation.OperationHandle;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.operation.OperationResult;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.paging.Page;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.paging.PagingInfo;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ResourceNotAContainerElementException;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ResourceNotFoundException;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.AssetAdministrationShellSearchCriteria;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.ConceptDescriptionSearchCriteria;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.Persistence;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.SubmodelElementSearchCriteria;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.SubmodelSearchCriteria;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.memory.PersistenceInMemory;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.memory.PersistenceInMemoryConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MDTParametersPersistence implements Persistence<MDTParameterPersistenceConfig> {
	private static final Logger s_logger = LoggerFactory.getLogger(MDTParametersPersistence.class);
	private ServiceContext m_serviceContext;
	private MDTParameterPersistenceConfig m_config;
	private MDTInstanceParameterMqttClient m_mqttClient;
	private BiMap<String,String> m_pathToParameterMap = HashBiMap.create();
	private BiMap<String,String> m_parameterToPathMap = m_pathToParameterMap.inverse();
	
	private final Guard m_guard = Guard.create();
	private final PersistenceInMemory m_basePersistence = new PersistenceInMemory();

	@Override
	public void init(CoreConfig coreConfig, MDTParameterPersistenceConfig config, ServiceContext serviceContext)
		throws ConfigurationInitializationException {
		m_serviceContext = serviceContext;
		m_config = config;
		PersistenceInMemoryConfig baseConfig = PersistenceInMemoryConfig.builder()
																		.initialModel(config.getInitialModel())
																		.initialModelFile(config.getInitialModelFile())
																		.build();
		m_basePersistence.init(coreConfig, baseConfig, serviceContext);
		
		m_mqttClient = new MDTInstanceParameterMqttClient(m_config.getMqttBroker());
		m_mqttClient.startAsync();
		
		if ( s_logger.isInfoEnabled() ) {
			s_logger.info("initialized: config={}", config);
		}
	}

	@Override
	public MDTParameterPersistenceConfig asConfig() {
		return m_config;
	}

	@Override
	public AssetAdministrationShell getAssetAdministrationShell(String id, QueryModifier modifier)
		throws ResourceNotFoundException {
		return m_basePersistence.getAssetAdministrationShell(id, modifier);
	}

	@Override
	public Page<Reference> getSubmodelRefs(String aasId, PagingInfo paging) throws ResourceNotFoundException {
		return m_basePersistence.getSubmodelRefs(aasId, paging);
	}

	@Override
	public Submodel getSubmodel(String id, QueryModifier modifier) throws ResourceNotFoundException {
		return m_guard.getChecked(() -> m_basePersistence.getSubmodel(id, modifier));
	}

	@Override
	public ConceptDescription getConceptDescription(String id, QueryModifier modifier)
		throws ResourceNotFoundException {
		return m_basePersistence.getConceptDescription(id, modifier);
	}

	@Override
	public SubmodelElement getSubmodelElement(SubmodelElementIdentifier identifier, QueryModifier modifier)
		throws ResourceNotFoundException {
		return m_guard.getChecked(() -> m_basePersistence.getSubmodelElement(identifier, modifier));
	}

	@Override
	public OperationResult getOperationResult(OperationHandle handle) throws ResourceNotFoundException {
		return m_guard.getChecked(() -> m_basePersistence.getOperationResult(handle));
	}

	@Override
	public Page<AssetAdministrationShell> findAssetAdministrationShells(AssetAdministrationShellSearchCriteria criteria,
																		QueryModifier modifier, PagingInfo paging) {
		return m_basePersistence.findAssetAdministrationShells(criteria, modifier, paging);
	}

	@Override
	public Page<Submodel> findSubmodels(SubmodelSearchCriteria criteria, QueryModifier modifier, PagingInfo paging) {
		return m_basePersistence.findSubmodels(criteria, modifier, paging);
	}

	@Override
	public Page<SubmodelElement> findSubmodelElements(SubmodelElementSearchCriteria criteria, QueryModifier modifier,
														PagingInfo paging) throws ResourceNotFoundException {
		return m_guard.getChecked(() -> m_basePersistence.findSubmodelElements(criteria, modifier, paging));
	}

	@Override
	public Page<ConceptDescription> findConceptDescriptions(ConceptDescriptionSearchCriteria criteria,
			QueryModifier modifier, PagingInfo paging) {
		return m_basePersistence.findConceptDescriptions(criteria, modifier, paging);
	}

	@Override
	public void save(AssetAdministrationShell assetAdministrationShell) {
		m_basePersistence.save(assetAdministrationShell);
	}

	@Override
	public void save(ConceptDescription conceptDescription) {
		m_basePersistence.save(conceptDescription);
	}

	@Override
	public void save(Submodel submodel) {
		m_guard.runChecked(() -> m_basePersistence.save(submodel));
	}

	@Override
	public void insert(SubmodelElementIdentifier parentIdentifier, SubmodelElement submodelElement)
		throws ResourceNotFoundException, ResourceNotAContainerElementException {
		m_guard.lock();
		try {
			m_basePersistence.insert(parentIdentifier, submodelElement);
		}
		finally {
            m_guard.unlock();
		}
	}

	@Override
	public void update(SubmodelElementIdentifier identifier, SubmodelElement submodelElement)
		throws ResourceNotFoundException {
		MDTParameterConfig paramConfig = findParameterConfig(identifier);
		
		m_guard.lock();
		try {
			m_basePersistence.update(identifier, submodelElement);
			if ( paramConfig != null && paramConfig.isPublishing() ) {
				String valueStr = ElementValues.getValue(submodelElement).toString();
				m_mqttClient.publishParameterUpdated(paramConfig.getName(), valueStr);
			}
		}
		finally {
            m_guard.unlock();
		}
	}
	
	private final String EQ_FORMAT = "DataInfo.Equipment.EquipmentParameterValues[%d].ParameterValue";
	private final String OP_FORMAT = "DataInfo.Operation.OperationParameterValues[%d].ParameterValue";

	private MDTParameterConfig findParameterConfig(SubmodelElementIdentifier identifier) {
		if ( m_pathToParameterMap == null ) {
			Environment env = m_serviceContext.getAASEnvironment();
			Submodel dataSubmodel = FStream.from(env.getSubmodels())
											.findFirst(sm -> Data.SEMANTIC_ID_REFERENCE.equals(sm.getSemanticId()))
											.getOrNull();
			DefaultData data = new DefaultData();
			data.updateFromAasModel(dataSubmodel);
			DataInfo dataInfo = data.getDataInfo();
			if ( dataInfo.isEquipment() ) {
				FStream.from(dataInfo.getEquipment().getParameterValueList())
						.zipWithIndex()
						.forEach(idxed -> {
                            String path = String.format(EQ_FORMAT, idxed.index());
                            String paramId = idxed.value().getParameterId();
                            m_pathToParameterMap.put(path, paramId);
                        });
			}
			else if ( dataInfo.isOperation() ) {
				FStream.from(dataInfo.getOperation().getParameterValueList())
						.zipWithIndex()
						.forEach(idxed -> {
                            String path = String.format(OP_FORMAT, idxed.index());
                            String paramId = idxed.value().getParameterId();
                            m_pathToParameterMap.put(path, paramId);
                        });
			}
			else {
				throw new IllegalStateException("Invalid DataInfo: " + data.getDataInfo());
			}
		}
		
		String elementPath = identifier.getIdShortPath().toString();
		
		String paramId = m_pathToParameterMap.get(elementPath);
		if ( paramId != null ) {
			return m_config.getParameterConfig(paramId);
		}
		else {
			return null;
		}
	}

	@Override
	public void save(OperationHandle handle, OperationResult result) {
		m_guard.runChecked(() -> m_basePersistence.save(handle, result));
	}

	@Override
	public void deleteAssetAdministrationShell(String id) throws ResourceNotFoundException {
		m_basePersistence.deleteAssetAdministrationShell(id);
	}

	@Override
	public void deleteSubmodel(String id) throws ResourceNotFoundException {
		m_guard.runChecked(() -> m_basePersistence.deleteSubmodel(id));
	}

	@Override
	public void deleteConceptDescription(String id) throws ResourceNotFoundException {
		m_basePersistence.deleteConceptDescription(id);
	}

	@Override
	public void deleteSubmodelElement(SubmodelElementIdentifier identifier) throws ResourceNotFoundException {
		m_guard.runChecked(() -> m_basePersistence.deleteSubmodelElement(identifier));
	}
	
	@Getter
	@Accessors(prefix="m_")
	private static class ParameterValue {
		private final String m_parameter;
		private final String m_value;

		private ParameterValue(@JsonProperty("parameter") String parameterId, @JsonProperty("value") String value) {
			m_parameter = parameterId;
			m_value = value;
		}
	}

	private static class MDTInstanceParameterMqttClient extends AutoReconnectingMqttClient {
		private static final Logger s_logger = LoggerFactory.getLogger(MDTInstanceStatusSubscriber.class);
		private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();
		
		private final String m_topic;
		private final int m_qos;
		
		private MDTInstanceParameterMqttClient(MqttBrokerConfig brokerConfig) {
			super(brokerConfig.getBrokerUrl(), null, brokerConfig.getReconnectTryInterval());
			
			m_topic = "";
			m_qos = 0;
			setLogger(s_logger);
		}
		
		public void publishParameterUpdated(String parameterId, String value) {
			MqttClient client = pollMqttClient();
			if ( client != null ) {
				try {
					String topic = String.format("%s/parameters/%s", m_topic, parameterId);
					String jsonStr = MAPPER.writeValueAsString(new ParameterValue(parameterId, value));
					
					MqttMessage message = new MqttMessage(jsonStr.getBytes(StandardCharsets.UTF_8));
					message.setQos(m_qos);
					client.publish(topic, message);
				}
				catch ( Exception e ) {
					getLogger().warn("Failed to publish event, cause=" + e);
				}
			}
		}
	
		@Override
		public void messageArrived(String topic, MqttMessage msg) throws Exception {
			String json = new String(msg.getPayload(), StandardCharsets.UTF_8);
			InstanceStatusChangeEvent ev = MAPPER.readValue(json, InstanceStatusChangeEvent.class);
			
			HttpMDTInstanceManager.EVENT_BUS.post(ev);
		}
	
		@Override public void deliveryComplete(IMqttDeliveryToken token) { }
		
		@Override protected void mqttBrokerConnected(MqttClient client) throws Exception {
			client.subscribe(m_topic + "/#", m_qos);
		}
		@Override protected void mqttBrokerDisconnected() { }
	}
}
