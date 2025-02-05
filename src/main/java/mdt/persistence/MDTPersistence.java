package mdt.persistence;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import utils.stream.FStream;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.config.CoreConfig;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationInitializationException;
import de.fraunhofer.iosb.ilt.faaast.service.model.IdShortPath;
import de.fraunhofer.iosb.ilt.faaast.service.model.SubmodelElementIdentifier;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.modifier.Level;
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
import mdt.model.MDTModelSerDe;
import mdt.model.sm.value.ElementValues;
import mdt.model.sm.value.SubmodelElementValue;
import mdt.persistence.asset.AssetUpdateEvent;
import mdt.persistence.asset.AssetVariable;
import mdt.persistence.asset.AssetVariableConfig;
import mdt.persistence.asset.AssetVariableException;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MDTPersistence implements Persistence<MDTPersistenceConfig> {
	@SuppressWarnings("unused")
	private static final Logger s_logger = LoggerFactory.getLogger(MDTPersistence.class);
	
	private MDTPersistenceConfig m_persistConfig;
	private final PersistenceInMemory m_basePersistence;
	private PersistenceInMemoryConfig m_basePersistenceConfig;
	private Multimap<String, AssetVariable> m_variablesBySubmodel = ArrayListMultimap.create();

	public MDTPersistence() {
		m_basePersistence = new PersistenceInMemory();
		m_basePersistenceConfig = new PersistenceInMemoryConfig.Builder().build();
	}

	@Override
	public void init(CoreConfig coreConfig, MDTPersistenceConfig config, ServiceContext serviceContext)
		throws ConfigurationInitializationException {
		m_persistConfig = config;
		m_basePersistenceConfig = new PersistenceInMemoryConfig.Builder()
																.initialModelFile(config.getInitialModelFile())
																.build();
		m_basePersistence.init(coreConfig, m_basePersistenceConfig, serviceContext);

		// SubmodelIdShort -> SubmodelId 매핑을 먼저 생성하고,
		// 이를 통해 SubmodelId에 해당하는 AssetVariable들을 찾을 수 있는 매핑을 생성한다.
		Environment env = m_basePersistence.getEnvironment();
		Map<String,String> smIdMapping = FStream.from(env.getSubmodels())
												.collect(Maps.newHashMap(), (m, sm) -> m.put(sm.getIdShort(), sm.getId()));
		FStream.from(config.getAssetVariableConfigs())
		 		.map(c -> createAssetVariable(c))
		 		.forEach(assetVar -> {
		 			String smId = smIdMapping.get(assetVar.getSubmodelIdShort());
					if ( smId == null ) {
						String msg = String.format("Unknown SubmodelIdShort: assetVariable=%s", assetVar);
						throw new AssetVariableException(msg);
					}
		 			m_variablesBySubmodel.put(smId, assetVar);
		 		});
	}

	@Override
	public MDTPersistenceConfig asConfig() {
		return m_persistConfig;
	}

	@Override
	public AssetAdministrationShell getAssetAdministrationShell(String id, QueryModifier modifier)
		throws ResourceNotFoundException {
		return m_basePersistence.getAssetAdministrationShell(id, modifier);
	}

	@Override
	public Page<AssetAdministrationShell> findAssetAdministrationShells(AssetAdministrationShellSearchCriteria criteria,
																		QueryModifier modifier, PagingInfo paging) {
		return m_basePersistence.findAssetAdministrationShells(criteria, modifier, paging);
	}

	@Override
	public Page<Reference> getSubmodelRefs(String aasId, PagingInfo paging) throws ResourceNotFoundException {
		return m_basePersistence.getSubmodelRefs(aasId, paging);
	}

	@Override
	public void save(AssetAdministrationShell assetAdministrationShell) {
		m_basePersistence.save(assetAdministrationShell);
	}

	@Override
	public void deleteAssetAdministrationShell(String id) throws ResourceNotFoundException {
		m_basePersistence.deleteAssetAdministrationShell(id);
	}

	@Override
	public ConceptDescription getConceptDescription(String id, QueryModifier modifier)
		throws ResourceNotFoundException {
		return m_basePersistence.getConceptDescription(id, modifier);
	}

	@Override
	public Page<ConceptDescription> findConceptDescriptions(ConceptDescriptionSearchCriteria criteria,
															QueryModifier modifier, PagingInfo paging) {
		return m_basePersistence.findConceptDescriptions(criteria, modifier, paging);
	}

	@Override
	public void save(ConceptDescription conceptDescription) {
		m_basePersistence.save(conceptDescription);
	}

	@Override
	public void deleteConceptDescription(String id) throws ResourceNotFoundException {
		m_basePersistence.deleteAssetAdministrationShell(id);
	}
	
	private boolean isNormalModifier(QueryModifier modifier) {
        return modifier.getLevel() == Level.DEFAULT;
	}

	@Override
	public Submodel getSubmodel(String id, QueryModifier modifier) throws ResourceNotFoundException {
		var variables = m_variablesBySubmodel.get(id);
		if ( variables.size() > 0 ) {
			// 대상 submodel과 연관된 AssetVariable들을 통해 외부 asset에서 값을 읽어와서
			// Submodel에 반영시킨다.
			Submodel sm = m_basePersistence.getSubmodel(id, QueryModifier.DEFAULT);
			updateSubmodel(sm, variables);
		
			// Submodel이 갱신되면 사용자가 요청한 modifier에 따라 Submodel을 반환한다.
			// 만일 사용자가 요청한 modifier가 DEFAULT인 경우에는 갱신된 Submodel을 바로 반환한다.
			return (!isNormalModifier(modifier)) ? m_basePersistence.getSubmodel(id, modifier) : sm;
		}
		else {
			return m_basePersistence.getSubmodel(id, modifier);
		}
	}

	@Override
	public Page<Submodel> findSubmodels(SubmodelSearchCriteria criteria, QueryModifier modifier, PagingInfo paging) {
		Page<Submodel> page = m_basePersistence.findSubmodels(criteria, QueryModifier.DEFAULT, paging);
		List<Submodel> submodels = FStream.from(page.getContent())
											.mapOrIgnore(sm -> {
												var variables = m_variablesBySubmodel.get(sm.getId());
												updateSubmodel(sm, variables);
												return sm;
											})
									        .toList();
		
		if ( !isNormalModifier(modifier) ) {
			page.setContent(submodels);
			return page;
		}
		else {
			return m_basePersistence.findSubmodels(criteria, modifier, paging);
		}
	}

	@Override
	public void save(Submodel submodel) {
		m_basePersistence.save(submodel);
		
		// 갱신된 Submodel에 관련된 AssetVariable들을 찾아서
		// update된 SubmodelElement 값들을 asset에 반영시킨다.
		updateAssetVariables(submodel, m_variablesBySubmodel.get(submodel.getId()));
	}

	@Override
	public void deleteSubmodel(String id) throws ResourceNotFoundException {
		m_basePersistence.deleteSubmodel(id);
	}
	
	@Override
	public SubmodelElement getSubmodelElement(SubmodelElementIdentifier identifier, QueryModifier modifier)
		throws ResourceNotFoundException {	
		List<AssetVariable> relevantVariables = findMatchingAssetVariables(identifier, false);
		if ( relevantVariables.size() > 0 ) {
			String submodelId = identifier.getSubmodelId();
			
			// 검색 대상 SubmodelElement와 연결된 AssetVariable들이 존재하는 경우,
			// AssetVariable들을 통해 외부 asset에서 값을 읽어와서 SubmodelElement에 반영시킨다.
			Submodel sm = m_basePersistence.getSubmodel(submodelId, QueryModifier.DEFAULT);
			updateSubmodel(sm, relevantVariables);
        }
		return m_basePersistence.getSubmodelElement(identifier, modifier);
	}
	
	@Override
	public Page<SubmodelElement> findSubmodelElements(SubmodelElementSearchCriteria criteria,
														QueryModifier modifier,
														PagingInfo paging) throws ResourceNotFoundException {
		// m_basePersistence의 findSubmodelElements()를 호출해서 반환된 SubmodelElement들이
		// 어떤 Submodel에 속하는지를 알 수 없기 때문에, criteria와 관련된 모든 Submodel에 속한
		// AssetVariable들을 통해 Submodel을 먼저 update 시킨다.
		m_basePersistence.getAllSubmodels(QueryModifier.DEFAULT, PagingInfo.ALL)
			            .getContent().stream()
			            .forEach(sm -> {
			        		var variables = m_variablesBySubmodel.get(sm.getId());
			        		updateSubmodel(sm, variables);
                        });
		return m_basePersistence.findSubmodelElements(criteria, modifier, paging);
	}

	@Override
	public void insert(SubmodelElementIdentifier parentIdentifier, SubmodelElement submodelElement)
		throws ResourceNotFoundException, ResourceNotAContainerElementException {
		// 새로운 SubmodelElement를 m_basePersistence을 통해 삽입시킨다.
		m_basePersistence.insert(parentIdentifier, submodelElement);

		var matches = findMatchingAssetVariables(parentIdentifier, true);
		if ( matches.size() > 0 ) {
			String submodelId = parentIdentifier.getSubmodelId();
			
			Submodel sm = m_basePersistence.getSubmodel(submodelId, QueryModifier.DEFAULT);
			updateAssetVariables(sm, matches);
		}
	}

	@Override
	public void update(SubmodelElementIdentifier identifier, SubmodelElement submodelElement)
		throws ResourceNotFoundException {
		// 변경된 SubmodelElement를 m_basePersistence을 통해 반영한다.
		m_basePersistence.update(identifier, submodelElement);

		var matches = findMatchingAssetVariables(identifier, true);
		if ( matches.size() > 0 ) {
			String submodelId = identifier.getSubmodelId();
			
			Submodel sm = m_basePersistence.getSubmodel(submodelId, QueryModifier.DEFAULT);
			updateAssetVariables(sm, matches);
		}
	}

	@Override
	public void deleteSubmodelElement(SubmodelElementIdentifier identifier) throws ResourceNotFoundException {
		m_basePersistence.deleteSubmodelElement(identifier);
	}	

	@Override
	public OperationResult getOperationResult(OperationHandle handle) throws ResourceNotFoundException {
		return m_basePersistence.getOperationResult(handle);
	}
	
	@Override
	public void save(OperationHandle handle, OperationResult result) {
		m_basePersistence.save(handle, result);
	}
	
	public void handleAssetUpdateEvent(AssetUpdateEvent event) {
		SubmodelElementIdentifier id = SubmodelElementIdentifier.builder()
                                                                .submodelId(event.getSubmodel())
                                                                .idShortPath(IdShortPath.builder().path(event.getPath()).build())
                                                                .build();
		try {
			SubmodelElement element = m_basePersistence.getSubmodelElement(id, QueryModifier.DEFAULT);
			updateWithValueJsonString(element, event.getUpdate());
			m_basePersistence.update(id, element);
		}
		catch ( ResourceNotFoundException | IOException e ) {
			s_logger.warn("Failed to handle AssetUpdateEvent: {}, cause={}", event, e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private AssetVariable createAssetVariable(AssetVariableConfig config) {
		try {
			String configClassName = config.getClass().getName();
			int length = configClassName.length();
			String assetVarClassName = configClassName.substring(0, length-6);
			
			Class assetVarClass = Class.forName(assetVarClassName);
			Constructor ctor = assetVarClass.getDeclaredConstructor(config.getClass());
			return (AssetVariable)ctor.newInstance(config);
		}
		catch ( Exception e ) {
			throw new AssetVariableException("Failed to create an AssetVariable", e);
		}
	}
	
	private List<AssetVariable> findMatchingAssetVariables(SubmodelElementIdentifier identifier, boolean forUpdate) {
		String submodelId = identifier.getSubmodelId();
		String elementPath = identifier.getIdShortPath().toString();
		
		var variables = m_variablesBySubmodel.get(submodelId);
		if ( variables.size() > 0 ) {
			return FStream.from(variables)
						    .filter(var -> forUpdate ? var.isUpdateable() : true)
							.filter(var -> var.overlaps(elementPath))
							.toList();
		}
		else {
			return Collections.emptyList();
		}
	}
	
	/**
	 * 주어진 Submodel에 해당하는 AssetVariable들을 통해 외부 asset에서 값을 읽어와서
	 * Submodel에 반영시킨다.
	 * 
	 * @param sm			Submodel
	 * @param variables		외부 asset의 데이터를 읽어 올 AssetVariable들
	 */
	private void updateSubmodel(Submodel sm, Collection<AssetVariable> variables) {
		FStream.from(variables)
				.forEachOrThrow(var -> {
					// AssetVariable과 연결된 SubmodelElement를 Submodel에서 찾아 바인딩시킨다.
					var.bind(sm);
					// SubmodelElement를 외부 asset에서 읽어와서 바인딩된 SubmodelElement에 반영시킨다.
					SubmodelElement updated = var.load();
					
					// 변경된 SubmodelElement를 m_basePersistence에도 반영시킨다.
					IdShortPath idShortPath = IdShortPath.builder().path(var.getElementPath()).build();
					SubmodelElementIdentifier id = SubmodelElementIdentifier.builder()
																			.submodelId(sm.getId())
																			.idShortPath(idShortPath)
																			.build();
					try {
						m_basePersistence.update(id, updated);
					}
					catch ( ResourceNotFoundException neverHappens ) {}
				});
	}
	
	/**
	 * 주어진 Submodel에서 AssetVariable들을 통해 해당 Element 영역을 외부 asset에 반영시킨다.
	 * <p>
	 * 입력으로 주어진 AssetVariable들은 updateable한 것들 이어야 한다.
	 * 
	 * @param sm			Submodel
	 * @param variables		반영 대상 AssetVariable들의 목록
	 * @return	반영 여부가 있었는지 여부
	 */
	private void updateAssetVariables(Submodel sm, Collection<AssetVariable> variables) {
		FStream.from(variables)
				.forEachOrThrow(var -> {
					var.bind(sm);
					var.save();
				});
	}

	static void update(SubmodelElement buffer, SubmodelElementValue smev) {
		ElementValues.update(buffer, smev);
	}

	static void updateWithValueJsonNode(SubmodelElement buffer, JsonNode valueNode) throws IOException {
		ElementValues.update(buffer, valueNode);
	}

	static void updateWithValueJsonString(SubmodelElement buffer, String valueJsonString) throws IOException {
		updateWithValueJsonNode(buffer, MDTModelSerDe.readJsonNode(valueJsonString));
	}
}
