package mdt.assetconnection.simulation;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import com.google.common.collect.Maps;

import utils.stream.FStream;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionException;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.common.provider.MultiFormatOperationProvider;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ResourceNotFoundException;
import de.fraunhofer.iosb.ilt.faaast.service.util.ReferenceHelper;
import mdt.model.resource.value.ElementValues;
import mdt.model.resource.value.SubmodelElementValue;
import mdt.operation.SimulationRequest;
import mdt.operation.subprocess.SubprocessSimulator;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class SimulationOperationProvider extends MultiFormatOperationProvider<SimulationOperationProviderConfig> {
	private final ServiceContext m_serviceContext;
	private final Reference m_ref;
	private final SubprocessSimulator m_simulator;
	private final JsonSerializer m_ser = new JsonSerializer();
	
	protected SimulationOperationProvider(ServiceContext serviceContext, Reference reference,
									SimulationOperationProviderConfig config) {
		super(config);

		m_serviceContext = serviceContext;
		m_ref = reference;
        m_simulator = new SubprocessSimulator(config.getWorkingDirectory(), config.getCommandPrefix());
	}

	@Override
	protected OperationVariable[] getOutputParameters() {
        try {
            return m_serviceContext.getOperationOutputVariables(m_ref);
        }
        catch (ResourceNotFoundException e) {
        	String msg = String.format("operation not defined in AAS model (reference: %s)",
            							ReferenceHelper.toString(m_ref));
            throw new IllegalStateException(msg, e);
        }
	}
	
    @Override
    public OperationVariable[] invoke(OperationVariable[] inputVars, OperationVariable[] inoutput)
    	throws AssetConnectionException {
    	try {
			Map<String,String> inputValues = Maps.newLinkedHashMap();
			for ( int i =0; i < inputVars.length; ++i ) {
				OperationVariable input = inputVars[i];
				SubmodelElement sme = input.getValue();
				SubmodelElementValue value = ElementValues.getValue(sme);
				String valueStr = m_ser.write(value.toJsonObject());
				inputValues.put(sme.getIdShort(), valueStr);
			}
			
			SimulationRequest req = SimulationRequest.builder()
													.inputValues(inputValues)
													.outputVariableNames(List.of("output"))
													.build();
			List<String> outputValues = m_simulator.run(req);
			OperationVariable[] outputVars = getOutputParameters();
			FStream.of(outputVars)
					.map(OperationVariable::getValue)
					.zipWith(FStream.from(outputValues))
					.forEach(t -> {
						if ( t._1 instanceof Property prop ) {
							prop.setValue(t._2);
						}
					});
					
			return outputVars;
		}
		catch ( Exception e ) {
			throw new AssetConnectionException(e);
		}
    }

	@Override
	protected byte[] invoke(byte[] input, UnaryOperator<String> variableReplacer)
		throws AssetConnectionException {
		throw new AssertionError();
	}
}
