package mdt.persistence.current;

import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class ConcurrentPersistence implements Persistence<ConcurrentPersistenceConfig> {
	private static final Logger s_logger = LoggerFactory.getLogger(ConcurrentPersistence.class);
	private ConcurrentPersistenceConfig m_config;
	
	private final ReentrantLock m_lock = new ReentrantLock();
	private final PersistenceInMemory m_basePersistence = new PersistenceInMemory();

	@Override
	public void init(CoreConfig coreConfig, ConcurrentPersistenceConfig config, ServiceContext serviceContext)
		throws ConfigurationInitializationException {
		m_config = config;
		PersistenceInMemoryConfig baseConfig = PersistenceInMemoryConfig.builder()
																		.initialModel(config.getInitialModel())
																		.initialModelFile(config.getInitialModelFile())
																		.build();
		m_basePersistence.init(coreConfig, baseConfig, serviceContext);
		
		if ( s_logger.isInfoEnabled() ) {
			s_logger.info("initialized: config={}", config);
		}
	}

	@Override
	public ConcurrentPersistenceConfig asConfig() {
		return m_config;
	}

	@Override
	public AssetAdministrationShell getAssetAdministrationShell(String id, QueryModifier modifier)
		throws ResourceNotFoundException {
		m_lock.lock();
		try {
			return m_basePersistence.getAssetAdministrationShell(id, modifier);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public Page<Reference> getSubmodelRefs(String aasId, PagingInfo paging) throws ResourceNotFoundException {
		m_lock.lock();
		try {
			return m_basePersistence.getSubmodelRefs(aasId, paging);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public Submodel getSubmodel(String id, QueryModifier modifier) throws ResourceNotFoundException {
		m_lock.lock();
		try {
			return m_basePersistence.getSubmodel(id, modifier);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public ConceptDescription getConceptDescription(String id, QueryModifier modifier)
		throws ResourceNotFoundException {
		m_lock.lock();
		try {
			return m_basePersistence.getConceptDescription(id, modifier);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public SubmodelElement getSubmodelElement(SubmodelElementIdentifier identifier, QueryModifier modifier)
		throws ResourceNotFoundException {
		m_lock.lock();
		try {
			return m_basePersistence.getSubmodelElement(identifier, modifier);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public OperationResult getOperationResult(OperationHandle handle) throws ResourceNotFoundException {
		m_lock.lock();
		try {
			return m_basePersistence.getOperationResult(handle);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public Page<AssetAdministrationShell> findAssetAdministrationShells(AssetAdministrationShellSearchCriteria criteria,
																		QueryModifier modifier, PagingInfo paging) {
		m_lock.lock();
		try {
			return m_basePersistence.findAssetAdministrationShells(criteria, modifier, paging);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public Page<Submodel> findSubmodels(SubmodelSearchCriteria criteria, QueryModifier modifier, PagingInfo paging) {
		m_lock.lock();
		try {
			return m_basePersistence.findSubmodels(criteria, modifier, paging);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public Page<SubmodelElement> findSubmodelElements(SubmodelElementSearchCriteria criteria, QueryModifier modifier,
														PagingInfo paging) throws ResourceNotFoundException {
		m_lock.lock();
		try {
			return m_basePersistence.findSubmodelElements(criteria, modifier, paging);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public Page<ConceptDescription> findConceptDescriptions(ConceptDescriptionSearchCriteria criteria,
			QueryModifier modifier, PagingInfo paging) {
		m_lock.lock();
		try {
			return m_basePersistence.findConceptDescriptions(criteria, modifier, paging);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public void save(AssetAdministrationShell assetAdministrationShell) {
		m_lock.lock();
		try {
			m_basePersistence.save(assetAdministrationShell);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public void save(ConceptDescription conceptDescription) {
		m_lock.lock();
		try {
			m_basePersistence.save(conceptDescription);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public void save(Submodel submodel) {
		m_lock.lock();
		try {
			m_basePersistence.save(submodel);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public void insert(SubmodelElementIdentifier parentIdentifier, SubmodelElement submodelElement)
		throws ResourceNotFoundException, ResourceNotAContainerElementException {
		m_lock.lock();
		try {
			m_basePersistence.insert(parentIdentifier, submodelElement);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public void update(SubmodelElementIdentifier identifier, SubmodelElement submodelElement)
		throws ResourceNotFoundException {
		m_lock.lock();
		try {
			m_basePersistence.update(identifier, submodelElement);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public void save(OperationHandle handle, OperationResult result) {
		m_lock.lock();
		try {
			m_basePersistence.save(handle, result);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public void deleteAssetAdministrationShell(String id) throws ResourceNotFoundException {
		m_lock.lock();
		try {
			m_basePersistence.deleteAssetAdministrationShell(id);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public void deleteSubmodel(String id) throws ResourceNotFoundException {
		m_lock.lock();
		try {
			m_basePersistence.deleteSubmodel(id);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public void deleteConceptDescription(String id) throws ResourceNotFoundException {
		m_lock.lock();
		try {
			m_basePersistence.deleteConceptDescription(id);
		}
		finally {
			m_lock.unlock();
		}
	}

	@Override
	public void deleteSubmodelElement(SubmodelElementIdentifier identifier) throws ResourceNotFoundException {
		m_lock.lock();
		try {
			m_basePersistence.deleteSubmodelElement(identifier);
		}
		finally {
			m_lock.unlock();
		}
	}
}
