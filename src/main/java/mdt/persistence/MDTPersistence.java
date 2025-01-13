package mdt.persistence;

import java.lang.reflect.Constructor;
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import utils.stream.FStream;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.config.CoreConfig;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationInitializationException;
import de.fraunhofer.iosb.ilt.faaast.service.model.SubmodelElementIdentifier;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.modifier.QueryModifier;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.operation.OperationHandle;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.operation.OperationResult;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.paging.Page;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.paging.PagingInfo;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.paging.PagingMetadata;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ResourceNotAContainerElementException;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ResourceNotFoundException;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.AssetAdministrationShellSearchCriteria;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.ConceptDescriptionSearchCriteria;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.Persistence;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.SubmodelElementSearchCriteria;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.SubmodelSearchCriteria;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.memory.PersistenceInMemory;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.memory.PersistenceInMemoryConfig;
import mdt.model.sm.SubmodelUtils;


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
	private Map<String,Submodel> m_submodels = Maps.newHashMap();
	private Multimap<String, AssetParameter> m_assetParameters = ArrayListMultimap.create();

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
		
		Environment env = m_basePersistence.getEnvironment();
		FStream.from(env.getSubmodels()).forEach(sm -> m_submodels.put(sm.getId(), sm));
		for ( AssetParameterConfig paramConfig: config.getAssetParameters() ) {
			AssetParameter param = createAssetParameter(paramConfig, env);
			m_assetParameters.put(param.getSubmodelIdShort(), param);
		}
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
	
	

	@Override
	public Submodel getSubmodel(String id, QueryModifier modifier) throws ResourceNotFoundException {
		Submodel sm = m_basePersistence.getSubmodel(id, QueryModifier.DEFAULT);
		FStream.from(m_assetParameters.get(sm.getIdShort()))
				.forEach(param -> {
					SubmodelElement element = SubmodelUtils.traverse(sm, param.getElementPath());
					param.load(element, param.getElementPath());
				});
		return sm;
	}

	@Override
	public Page<Submodel> findSubmodels(SubmodelSearchCriteria criteria, QueryModifier modifier, PagingInfo paging) {
		List<String> smIdList = findSubmodelAll(criteria);
		
		List<Submodel> submodelList = FStream.from(smIdList)
											.mapOrIgnore(id -> getSubmodel(id, modifier))
											.toList();
		Page<Submodel> page = new Page<>();
		page.setContent(submodelList);
		page.setMetadata(PagingMetadata.builder().cursor(submodelList.get(0).getId()).build());
		
		return page;
	}

	@Override
	public void save(Submodel submodel) {
		System.err.println("MDTPersistence.save(Submodel)");
		
		String smIdShort = submodel.getIdShort();
		FStream.from(m_assetParameters.get(smIdShort))
				.forEach(param -> {
					SubmodelElement element = SubmodelUtils.traverse(submodel, param.getElementPath());
					param.save(param.getElementPath(), element);
				});
	}

	@Override
	public void deleteSubmodel(String id) throws ResourceNotFoundException {
		System.err.printf("MDTPersistence.deleteSubmodel(%s)%n", id);
		
		throw new UnsupportedOperationException();
	}
	
	
	private void loadSubmodelElement(SubmodelElement buffer, String submodelId, String path)
		throws ResourceNotFoundException {
		Submodel sm = m_submodels.get(submodelId);
		if ( sm == null ) {
			throw new ResourceNotFoundException(submodelId, Submodel.class);
		}
		
		for ( AssetParameter param: m_assetParameters.get(sm.getIdShort()) ) {
			if ( param.contains(path) ) {
				param.load(buffer, path);
				return;
			}
			else if ( param.isContained(path) ) {
				param.load(buffer, path);
			}
		}
	}
	
	

	@Override
	public SubmodelElement getSubmodelElement(SubmodelElementIdentifier identifier, QueryModifier modifier)
		throws ResourceNotFoundException {
		String submodelId = identifier.getSubmodelId();
		String targetPath = identifier.getIdShortPath().toString();
		
		SubmodelElement buffer = m_basePersistence.getSubmodelElement(identifier, modifier);
		loadSubmodelElement(buffer, submodelId, targetPath);
		return buffer;
	}

	@Override
	public Page<SubmodelElement> findSubmodelElements(SubmodelElementSearchCriteria criteria, QueryModifier modifier,
														PagingInfo paging) throws ResourceNotFoundException {
		System.err.println("MDTPersistence.findSubmodelElements");
		throw new UnsupportedOperationException();
	}

	@Override
	public void insert(SubmodelElementIdentifier parentIdentifier, SubmodelElement submodelElement)
		throws ResourceNotFoundException, ResourceNotAContainerElementException {
		System.err.println("MDTPersistence.insert");
		throw new UnsupportedOperationException();
	}

	@Override
	public void update(SubmodelElementIdentifier identifier, SubmodelElement submodelElement)
		throws ResourceNotFoundException {
		String submodelId = identifier.getSubmodelId();
		Submodel sm = m_submodels.get(submodelId);
		if ( sm == null ) {
			throw new ResourceNotFoundException(submodelId, Submodel.class);
		}
		String smIdShort = sm.getIdShort();
		
		m_basePersistence.update(identifier, submodelElement);
		
		String targetPath = identifier.getIdShortPath().toString();
		for ( AssetParameter param: m_assetParameters.get(smIdShort) ) {
			param.save(targetPath, submodelElement);
		}
		
//		// 요청된 idShortPath를 포함하는 ParameterSet을 찾는다.
//		// 검색된 경우에는 검색된 mount 전체에 해당하는 top SubmodelElement를 생성하고
//		// 이 SubmodelElement부터 탐색을 수행한다.
//		AssetParameter cover = findCoveringParameterSet(submodelId, targetPath);
//		if ( cover != null ) {
////			String relPath = SubmodelUtils.toRelativeIdShortPath(cover.getElementPath(), targetPath);
////			SubmodelElement target = SubmodelUtils.traverse(cover.getElementBuffer(), relPath);
////			ElementValues.update(target, newValue);
//			
//			cover.save(targetPath);
//		}
//		
//		// 요청된 idShortPath가 넓어서 하나 이상의 mount를 포함하는 경우에는
//		// 그 위치에 해당하는 최상위 SubmodelElementCollection 객체를 기존 base-persistence를 통해
//		// 획득한 이후, idShortPath에 의해 포함된 모든 mount를 읽어서 생성된 SubmodelElement들을
//		// 이 최상위 SubmodelElementCollection이 추가시킨다.
//		for ( AssetParameter param: findPertainingParameterSetAll(submodelId, targetPath) ) {
//			param.save(targetPath);
//		}
	}

	@Override
	public void deleteSubmodelElement(SubmodelElementIdentifier identifier) throws ResourceNotFoundException {
		System.err.println("MDTPersistence.deleteSubmodelElement");
		throw new UnsupportedOperationException();
	}
	
	

	@Override
	public OperationResult getOperationResult(OperationHandle handle) throws ResourceNotFoundException {
		return m_basePersistence.getOperationResult(handle);
	}
	
	@Override
	public void save(OperationHandle handle, OperationResult result) {
		m_basePersistence.save(handle, result);
	}
	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private AssetParameter createAssetParameter(AssetParameterConfig config, Environment env) {
		try {
			String configClassName = config.getClass().getName();
			int length = configClassName.length();
			String paramClassName = configClassName.substring(0, length-6);
			
			Class paramClass = Class.forName(paramClassName);
			Constructor ctor = paramClass.getDeclaredConstructor(config.getClass());
			AssetParameter param = (AssetParameter)ctor.newInstance(config);
			
			param.initialize(env);
			
			return param;
		}
		catch ( Exception e ) {
			throw new AssetParameterException("Failed to create AssetParameter", e);
		}
	}
	
	private List<String> findSubmodelAll(SubmodelSearchCriteria criteria) {
		if ( criteria.isIdShortSet() ) {
			return FStream.from(m_submodels.values())
							.filter(sm -> criteria.getIdShort().equals(sm.getIdShort()))
							.map(sm -> sm.getId())
							.toList();
		}
		else if ( criteria.isSemanticIdSet() ) {
			Reference id = criteria.getSemanticId();
			return FStream.from(m_submodels.values())
							.filter(sm -> id.equals(sm.getSemanticId()))
							.map(Submodel::getId)
							.toList();
		}
		else {
			return FStream.from(m_submodels.values())
							.map(sm -> sm.getId())
							.toList();
		}
	}
	
//	private AssetParameter findCoveringParameterSet(String smId, String path) {
//		return FStream.from(m_assetParameters)
//						.filter(param -> path.startsWith(param.getElementPath())
//										&& param.getSubmodelId().equals(smId))
//						.findFirst()
//						.getOrNull();
//	}
//	
//	private List<AssetParameter> findPertainingParameterSetAll(String smId, String path) {
//		return FStream.from(m_assetParameters)
//						.filter(param -> param.getElementPath().startsWith(path)
//										&& param.getSubmodelId().equals(smId))
//						.toList();
//	}
}
