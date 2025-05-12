package mdt.persistence.mqtt;

import java.io.IOException;
import java.util.List;

import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.func.FOption;
import utils.stream.FStream;

import mdt.ElementLocation;
import mdt.MDTGlobalConfigurations;
import mdt.model.sm.SubmodelUtils;
import mdt.model.sm.value.ElementValue;
import mdt.model.sm.value.ElementValues;
import mdt.persistence.MDTModelLookup;
import mdt.persistence.PersistenceStack;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.config.CoreConfig;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationInitializationException;
import de.fraunhofer.iosb.ilt.faaast.service.model.SubmodelElementIdentifier;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ResourceNotFoundException;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.Persistence;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MqttPublishingPersistence extends PersistenceStack<MqttPublishingPersistenceConfig>
													implements Persistence<MqttPublishingPersistenceConfig> {
	private static final Logger s_logger = LoggerFactory.getLogger(MqttPublishingPersistence.class);

	MDTModelLookup m_lookup;
	MqttPublishingPersistenceConfig m_config;
	private PersistenceMqttClient m_mqttClient;
	
	public MqttPublishingPersistence() {
		setLogger(s_logger);
	}

	@Override
	public void init(CoreConfig coreConfig, MqttPublishingPersistenceConfig config, ServiceContext serviceContext)
		throws ConfigurationInitializationException {
		super.init(coreConfig, config, serviceContext);

		m_lookup = MDTModelLookup.getInstanceOrCreate(getBasePersistence());
		m_config = config;
		FStream.from(m_config.getPublishers())
		        .forEach(pub -> {
		        	pub.getElementLocation().activate(m_lookup);
				});
		FStream.from(m_config.getSubscribers())
		        .forEach(sub -> {
		        	sub.getElementLocation().activate(m_lookup);
				});
		
		MqttBrokerConfig brokerConfig;
		try {
			brokerConfig = MDTGlobalConfigurations.loadMqttBrokerAll().get("default");
			if ( brokerConfig == null ) {
				throw new ConfigurationInitializationException("Cannot find 'default' MQTT-broker configuration "
																+ "from the global configuration");
			}
		}
		catch ( IOException e ) {
			throw new ConfigurationInitializationException("Failed to read global configuration, cause=" + e);
		}
		m_mqttClient = new PersistenceMqttClient(this, brokerConfig);
		m_mqttClient.startAsync();
		
		if ( getLogger().isInfoEnabled() ) {
			getLogger().info("initialized {}, config={}", this, config);
		}
	}

	@Override
	public MqttPublishingPersistenceConfig asConfig() {
		return m_config;
	}

	@Override
	public void save(Submodel submodel) {
		getBasePersistence().save(submodel);

		String submodeId = submodel.getId();
		String submodelIdShort = m_lookup.getSubmodelIdShortFromSubmodelId(submodeId);
		String updateElmPath = "";
		
		var matches = findMatchingPublisherAll(submodelIdShort, updateElmPath);
		for ( MqttElementPublisher publisher : matches ) {
			ElementLocation elmLoc = publisher.getElementLocation();
			
			String relPath = SubmodelUtils.toRelativeIdShortPath(updateElmPath, elmLoc.getElementPath());
			SubmodelElement elm = SubmodelUtils.traverse(submodel, relPath);
			ElementValue value = ElementValues.getValue(elm);
			
			m_mqttClient.publishMessage(publisher.getTopic(), value);
		}
	}

	@Override
	public void update(SubmodelElementIdentifier identifier, SubmodelElement submodelElement)
		throws ResourceNotFoundException {
		// 변경된 SubmodelElement를 m_basePersistence을 통해 반영한다.
		getBasePersistence().update(identifier, submodelElement);

		String submodeId = identifier.getSubmodelId();
		String submodelIdShort = m_lookup.getSubmodelIdShortFromSubmodelId(submodeId);
		String updateElmPath = identifier.getIdShortPath().toString();
		
		var matches = findMatchingPublisherAll(submodelIdShort, updateElmPath);
		for ( MqttElementPublisher publisher : matches ) {
			ElementLocation elmLoc = publisher.getElementLocation();
			
			String relPath = SubmodelUtils.toRelativeIdShortPath(updateElmPath, elmLoc.getElementPath());
			SubmodelElement elm = SubmodelUtils.traverse(submodelElement, relPath);
			ElementValue value = ElementValues.getValue(elm);

			m_mqttClient.publishMessage(publisher.getTopic(), value);
		}
	}
	
	private List<MqttElementPublisher> findMatchingPublisherAll(String submodelIdShort, String elementPath) {
		return FStream.from(m_config.getPublishers())
					    .filter(pub -> {
					    	ElementLocation elmLoc = pub.getElementLocation();
					    	return elmLoc.getElementPath().startsWith(elementPath)
					    			&& elmLoc.getSubmodelIdShort().equals(submodelIdShort);
					    })
					    .toList();
	}
	
	FOption<MqttElementSubscriber> findMatchingSubscriber(String topic) {
		return FStream.from(m_config.getSubscribers())
					    .findFirst(sub -> topic.equals(sub.getTopic()));
	}
}
