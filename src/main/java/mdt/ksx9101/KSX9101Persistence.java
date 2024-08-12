package mdt.ksx9101;

import java.util.Set;

import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;
import org.hibernate.jpa.HibernatePersistenceProvider;

import com.google.common.collect.Sets;

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
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ResourceNotAContainerElementException;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ResourceNotFoundException;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.AssetAdministrationShellSearchCriteria;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.ConceptDescriptionSearchCriteria;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.Persistence;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.SubmodelElementSearchCriteria;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.SubmodelSearchCriteria;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.memory.PersistenceInMemory;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.memory.PersistenceInMemoryConfig;
import jakarta.persistence.EntityManagerFactory;
import mdt.ksx9101.jpa.JpaEntityOperations;
import mdt.ksx9101.jpa.JpaPersistenceUnitInfo;
import mdt.model.SubmodelUtils;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class KSX9101Persistence implements Persistence<KSX9101PersistencerConfig> {
	private KSX9101PersistencerConfig m_persistConfig;
	private final PersistenceInMemory m_basePersistence;
	private PersistenceInMemoryConfig m_basePersistenceConfig;
	
	private EntityManagerFactory m_emFact;
	private Set<String> m_ksx9101SubmodelIds = Sets.newHashSet();
	
	private JpaEntityOperations m_ops;
	
	public KSX9101Persistence() {
		m_basePersistence = new PersistenceInMemory();
		m_basePersistenceConfig = new PersistenceInMemoryConfig.Builder().build();
	}

	@Override
	public void init(CoreConfig coreConfig, KSX9101PersistencerConfig config,
					ServiceContext serviceContext)
		throws ConfigurationInitializationException {
		m_persistConfig = config;
		m_basePersistenceConfig = new PersistenceInMemoryConfig.Builder()
															.initialModelFile(config.getInitialModelFile())
															.build();
		m_basePersistence.init(coreConfig, m_basePersistenceConfig, serviceContext);
		
		FStream.from(m_persistConfig.getEntityConfigs())
				.map(econf -> econf.getMountPoint().getSubmodel())
				.forEach(m_ksx9101SubmodelIds::add);
		
		JPAConfiguration jpaConf = m_persistConfig.getJpaConfig();
		JpaPersistenceUnitInfo puInfo = new JpaPersistenceUnitInfo("mdt-ksx9101", jpaConf.getJdbc());
		m_emFact = new HibernatePersistenceProvider().createContainerEntityManagerFactory(puInfo,
																					jpaConf.getProperties());
		m_ops = new JpaEntityOperations(m_persistConfig, m_emFact);
//		
//		
//
//		try ( EntityManager em = m_emFact.createEntityManager() ) {
//			EntityTransaction tx = em.getTransaction();
//			tx.begin();
//			
//			List<JpaLOT> lotList = em.createQuery("select lot from JpaLOT lot ", JpaLOT.class)
//											.getResultList();
//			for ( JpaLOT lot: lotList ) {
//				System.out.println(lot);
//			}
//			
//			tx.commit();
//		}
//		
//		
	}

	@Override
	public KSX9101PersistencerConfig asConfig() {
		return m_persistConfig;
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
	public Page<AssetAdministrationShell>
	findAssetAdministrationShells(AssetAdministrationShellSearchCriteria criteria,
									QueryModifier modifier, PagingInfo paging) {
		return m_basePersistence.findAssetAdministrationShells(criteria, modifier, paging);
	}

	@Override
	public void deleteAssetAdministrationShell(String id) throws ResourceNotFoundException {
		m_basePersistence.deleteAssetAdministrationShell(id);
	}

	@Override
	public void save(AssetAdministrationShell assetAdministrationShell) {
		m_basePersistence.save(assetAdministrationShell);
	}

	@Override
	public void deleteConceptDescription(String id) throws ResourceNotFoundException {
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
	public Submodel getSubmodel(String id, QueryModifier modifier) throws ResourceNotFoundException {
		Submodel submodel = m_basePersistence.getSubmodel(id, QueryModifier.DEFAULT);
		if ( !isKSX9101Submodel(id) ) {
			return submodel;
		}
		
		if ( m_ops.mount(submodel) > 0 ) {
			return submodel;
		}
		else {
			String msg = String.format("Resource not found: Submodel[%s]", id);
			throw new ResourceNotFoundException(msg);
		}
	}

	@Override
	public Page<Submodel> findSubmodels(SubmodelSearchCriteria criteria, QueryModifier modifier,
										PagingInfo paging) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteSubmodel(String id) throws ResourceNotFoundException {
		if ( isKSX9101Submodel(id) ) {
			throw new UnsupportedOperationException();
		}
		else {
			m_basePersistence.deleteSubmodel(id);
		}
	}

	@Override
	public void save(Submodel submodel) {
		m_basePersistence.save(submodel);
	}

	@Override
	public Page<SubmodelElement> findSubmodelElements(SubmodelElementSearchCriteria criteria,
														QueryModifier modifier,
														PagingInfo paging) throws ResourceNotFoundException {
		return m_basePersistence.findSubmodelElements(criteria, modifier, paging);
	}

	@Override
	public SubmodelElement getSubmodelElement(SubmodelElementIdentifier identifier, QueryModifier modifier)
		throws ResourceNotFoundException {
		String submodelId = identifier.getSubmodelId();
		
		// 주어진 식별자에 해당하는 Submodel에 mount-point가 지정되지 않으면
		// 기존의 방법으로 SubmodelElement를 구한다.
		if ( !isKSX9101Submodel(submodelId) ) {
			return m_basePersistence.getSubmodelElement(identifier, modifier);
		}
		
		String targetPath = identifier.getIdShortPath().toString();
		EntityConfiguration cover = m_persistConfig.findCoverEntityConfiguration(targetPath);
		if ( cover != null ) {
			SubmodelElement root = m_ops.read(cover);
			String relPath = SubmodelUtils.toRelativeIdShortPath(cover.getRootPathString(), targetPath);
			return SubmodelUtils.traverse(root, relPath);
		}
		
		SubmodelElement target = m_basePersistence.getSubmodelElement(identifier, modifier);
		for ( EntityConfiguration partConf: m_persistConfig.findSubEntityConfigurations(targetPath) ) {
			SubmodelElement part = m_ops.read(partConf);
			
			String mountPointPath = partConf.getMountPoint().getParentIdShortPath();
			String relPathStr = SubmodelUtils.toRelativeIdShortPath(targetPath, mountPointPath);
			SubmodelElement mountPt = SubmodelUtils.traverse(target, relPathStr);
			if ( mountPt instanceof SubmodelElementCollection smc ) {
				smc.getValue().add(part);
			}
			else if ( mountPt instanceof SubmodelElementList sml ) {
				sml.getValue().add(part);
			}
			else {
				throw new IllegalStateException("");
			}
		}
		
		return target;
	}

	@Override
	public void update(SubmodelElementIdentifier identifier, SubmodelElement submodelElement)
		throws ResourceNotFoundException {
		String submodelId = identifier.getSubmodelId();
		if ( !isKSX9101Submodel(submodelId) ) {
			m_basePersistence.update(identifier, submodelElement);
		}
		else {
			throw new UnsupportedOperationException(
					"KSX9101Persistence.update(SubmodelElementIdentifier,SubmodelElement)");
		}
	}

	@Override
	public void insert(SubmodelElementIdentifier parentIdentifier, SubmodelElement submodelElement)
		throws ResourceNotFoundException, ResourceNotAContainerElementException {
		String submodelId = parentIdentifier.getSubmodelId();
		if ( !isKSX9101Submodel(submodelId) ) {
			m_basePersistence.insert(parentIdentifier, submodelElement);
		}
		else {
			throw new UnsupportedOperationException(
								"KSX9101Persistence.insert(SubmodelElementIdentifier,SubmodelElement)");
		}
	}

	@Override
	public void deleteSubmodelElement(SubmodelElementIdentifier identifier) throws ResourceNotFoundException {
		String submodelId = identifier.getSubmodelId();
		if ( !isKSX9101Submodel(submodelId) ) {
			m_basePersistence.deleteSubmodelElement(identifier);
		}
		else {
			throw new UnsupportedOperationException(
								"KSX9101Persistence.deleteSubmodelElement(SubmodelElementIdentifier)");
		}
	}

	@Override
	public OperationResult getOperationResult(OperationHandle handle) throws ResourceNotFoundException {
		return m_basePersistence.getOperationResult(handle);
	}

	@Override
	public void save(OperationHandle handle, OperationResult result) {
		m_basePersistence.save(handle, result);
	}
	
	private boolean isKSX9101Submodel(String submodelId) {
		return m_ksx9101SubmodelIds.contains(submodelId);
	}
}
