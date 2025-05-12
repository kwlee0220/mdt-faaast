package mdt.persistence;

import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.func.FOption;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.config.CoreConfig;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationInitializationException;
import de.fraunhofer.iosb.ilt.faaast.service.model.SubmodelElementIdentifier;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.modifier.QueryModifier;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ResourceNotAContainerElementException;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ResourceNotFoundException;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public abstract class MDTParameterPersistence<C extends MDTParameterPersistenceConfig>
										extends PersistenceStack<C>{
	private static final Logger s_logger = LoggerFactory.getLogger(MDTParameterPersistence.class);

	private MDTModelLookup m_lookup;
	abstract protected FOption<SubmodelElement> readParameter(String parameterId);
	abstract protected void parameterRead(String parameterId, SubmodelElement element);
	abstract protected boolean updateParameter(String parameterId, SubmodelElement element);
	abstract protected void parameterUpdated(String parameterId, SubmodelElement element);
	
	public MDTParameterPersistence() {
		setLogger(s_logger);
	}

	@Override
	public void init(CoreConfig coreConfig, MDTParameterPersistenceConfig config, ServiceContext serviceContext)
		throws ConfigurationInitializationException {
		super.init(coreConfig, (C) config, serviceContext);

		m_lookup = MDTModelLookup.getInstanceOrCreate(getBasePersistence());
		if ( getLogger().isInfoEnabled() ) {
			getLogger().info("initialized {}, config={}", this, config);
		}
	}

	@Override
	public SubmodelElement getSubmodelElement(SubmodelElementIdentifier identifier, QueryModifier modifier)
		throws ResourceNotFoundException {
		String path = identifier.getIdShortPath().toString();
		String paramId = m_lookup.getParameterId(path);
		if ( paramId != null ) {
			SubmodelElement result = readParameter(paramId).getOrNull();
			if ( result == null ) {
				result = getBasePersistence().getSubmodelElement(identifier, modifier);
			}
			parameterRead(paramId, result);
			
			return result;
		}
		else {
			return getBasePersistence().getSubmodelElement(identifier, modifier);
		}
	}

	@Override
	public void insert(SubmodelElementIdentifier parentIdentifier, SubmodelElement submodelElement)
		throws ResourceNotFoundException, ResourceNotAContainerElementException {
		String path = parentIdentifier.getIdShortPath().toString() + "/" + submodelElement.getIdShort();

		String paramId = m_lookup.getParameterId(path);
		if ( paramId != null ) {
			throw new UnsupportedOperationException("insert: parameter=" + paramId);
		}
		else {
			getBasePersistence().insert(parentIdentifier, submodelElement);
		}
	}

	@Override
	public void update(SubmodelElementIdentifier identifier, SubmodelElement submodelElement)
		throws ResourceNotFoundException {
		String path = identifier.getIdShortPath().toString();
		String paramId = m_lookup.getParameterId(path);
		if ( paramId != null ) {
			if ( !updateParameter(paramId, submodelElement) ) {
				getBasePersistence().update(identifier, submodelElement);
			}
			parameterUpdated(paramId, submodelElement);
		}
		else {
			getBasePersistence().update(identifier, submodelElement);
		}
	}

	@Override
	public void deleteSubmodelElement(SubmodelElementIdentifier identifier) throws ResourceNotFoundException {
		String path = identifier.getIdShortPath().toString();
		String paramId = m_lookup.getParameterId(path);
		if ( paramId != null ) {
			throw new UnsupportedOperationException("deleteSubmodelElement: parameter=" + paramId);
		}
		else {
			getBasePersistence().deleteSubmodelElement(identifier);
		}
	}
}
