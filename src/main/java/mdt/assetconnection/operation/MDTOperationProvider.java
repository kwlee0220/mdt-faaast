package mdt.assetconnection.operation;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.digitaltwin.aas4j.v3.model.DataTypeDefXsd;
import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import utils.Throwables;
import utils.stream.FStream;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionException;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetOperationProvider;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ResourceNotFoundException;
import de.fraunhofer.iosb.ilt.faaast.service.util.ReferenceHelper;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MDTOperationProvider implements AssetOperationProvider {
	private final ServiceContext m_serviceContext;
	private final Reference m_opRef;
	private final List<OperationVariable> m_outputVariables;
	private final MDTOperationProviderConfig m_config;
	
	private final OperationProvider m_opProvider;
	
	public MDTOperationProvider(ServiceContext serviceContext, Reference reference,
								MDTOperationProviderConfig config) throws AssetConnectionException {
		Preconditions.checkArgument(config.getJava() != null
									|| config.getProgram() != null
									|| config.getHttp() != null,
									"No operation provider is specified");
		
		m_serviceContext = serviceContext;
		m_opRef = reference;
		m_config = config;
		
        try {
        	m_outputVariables = Lists.newArrayList(m_serviceContext.getOperationOutputVariables(m_opRef)); 
        }
        catch ( ResourceNotFoundException e ) {
        	String msg = String.format("operation not defined in AAS model (reference: %s)",
            							ReferenceHelper.toString(m_opRef));
            throw new IllegalStateException(msg, e);
        }
		
        try {
	    	if ( m_config.getJava() != null ) {
	    		m_opProvider = new JavaOperationProvider(serviceContext, m_opRef, m_config.getJava());
	    	}
	    	else if ( m_config.getProgram() != null ) {
				m_opProvider = new ProgramOperationProvider(serviceContext, m_opRef, m_config.getProgram());
	    	}
	    	else if ( m_config.getHttp() != null ) {
	    		m_opProvider = new HttpOperationProvider(serviceContext, m_opRef, m_config.getHttp());
	    	}
	    	else {
	    		throw new AssertionError();
	    	}
        }
        catch ( IOException e ) {
        	throw new AssetConnectionException(e);
        }
	}
	
    @Override
    public OperationVariable[] invoke(OperationVariable[] inputVars,
    								OperationVariable[] inoutputVars) throws AssetConnectionException {
		OperationVariable[] outputVars = FStream.from(m_outputVariables)
												.map(v -> v.getValue().getIdShort())
												.map(this::emptyStringPropertyOperationVariable)
												.toArray(OperationVariable.class);

		try {
			m_opProvider.invokeSync(inputVars, inoutputVars, outputVars);
			
	    	return outputVars;
		}
		catch ( Exception e ) {
			Throwables.throwIfInstanceOf(e, AssetConnectionException.class);
			throw new AssetConnectionException(e);
		}
    }

    @Override
    public void invokeAsync(OperationVariable[] inputVars,
							OperationVariable[] inoutputVars,
							BiConsumer<OperationVariable[], OperationVariable[]> callbackSuccess,
							Consumer<Throwable> callbackFailure)
            throws AssetConnectionException {
		OperationVariable[] outputVars = FStream.from(m_outputVariables)
												.map(v -> v.getValue().getIdShort())
												.map(this::emptyStringPropertyOperationVariable)
												.toArray(OperationVariable.class);

		try {
			m_opProvider.invokeAsync(inputVars, inoutputVars, outputVars, callbackSuccess, callbackFailure);
		}
		catch ( Exception e ) {
			Throwables.throwIfInstanceOf(e, AssetConnectionException.class);
			throw new AssetConnectionException(e);
		}
    }
	
	private OperationVariable emptyStringPropertyOperationVariable(String idShort) {
		DefaultProperty defProp = new DefaultProperty.Builder()
													.idShort(idShort)
													.valueType(DataTypeDefXsd.STRING)
													.value("")
													.build();
		return new DefaultOperationVariable.Builder().value(defProp).build();
	}
}
