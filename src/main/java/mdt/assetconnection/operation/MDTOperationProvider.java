package mdt.assetconnection.operation;

import java.util.List;
import java.util.function.UnaryOperator;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperationVariable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import utils.Throwables;
import utils.stream.FStream;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionException;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.common.provider.MultiFormatOperationProvider;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ResourceNotFoundException;
import de.fraunhofer.iosb.ilt.faaast.service.util.ReferenceHelper;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MDTOperationProvider extends MultiFormatOperationProvider<MDTOperationProviderConfig> {
	private final ServiceContext m_serviceContext;
	private final Reference m_opRef;
	private final List<OperationVariable> m_outputVariables;
	private final MDTOperationProviderConfig m_config;
	
	public MDTOperationProvider(ServiceContext serviceContext, Reference reference,
								MDTOperationProviderConfig config) {
		super(config);

		m_serviceContext = serviceContext;
		m_opRef = reference;
		
		Preconditions.checkArgument(config.getJava() != null || config.getProgram() != null,
									"No operation provider is specified");
		m_config = config;
		
        try {
        	m_outputVariables = Lists.newArrayList(m_serviceContext.getOperationOutputVariables(m_opRef)); 
        }
        catch ( ResourceNotFoundException e ) {
        	String msg = String.format("operation not defined in AAS model (reference: %s)",
            							ReferenceHelper.toString(m_opRef));
            throw new IllegalStateException(msg, e);
        }
	}

	@Override
	protected OperationVariable[] getOutputParameters() {
		return m_outputVariables.toArray(new OperationVariable[m_outputVariables.size()]);
	}
	
    @Override
    public OperationVariable[] invoke(OperationVariable[] inputVars,
    									OperationVariable[] inoutputVars) throws AssetConnectionException {
		OperationVariable[] outputVars = FStream.from(m_outputVariables)
												.map(v -> v.getValue().getIdShort())
												.map(this::emptyOperationVariable)
												.toArray(OperationVariable.class);
		
		OperationProvider prvd = null;
    	if ( m_config.getJava() != null ) {
    		prvd = new JavaOperationProvider(m_config.getJava());
    	}
    	else if ( m_config.getProgram() != null ) {
    		prvd = new ProgramOperationProvider(m_config.getProgram());
    	}
    	else {
    		throw new AssertionError();
    	}

		try {
			prvd.invoke(inputVars, inoutputVars, outputVars);
		}
		catch ( Exception e ) {
			Throwables.throwIfInstanceOf(e, AssetConnectionException.class);
			throw new AssetConnectionException(e);
		}
		
    	return outputVars;
    }

	@Override
	protected byte[] invoke(byte[] input, UnaryOperator<String> variableReplacer)
		throws AssetConnectionException {
		throw new AssertionError();
	}
	
	private OperationVariable emptyOperationVariable(String idShort) {
		return new DefaultOperationVariable.Builder().build();
	}
}
