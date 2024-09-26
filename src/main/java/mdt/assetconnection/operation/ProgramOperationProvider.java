package mdt.assetconnection.operation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperationVariable;

import utils.func.Funcs;
import utils.stream.FStream;

import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionException;
import mdt.client.operation.ProcessBasedMDTOperation;
import mdt.model.AASUtils;
import mdt.model.resource.value.ElementValues;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class ProgramOperationProvider implements OperationProvider {
	private final ProgramOperationProviderConfig m_config;
	
	ProgramOperationProvider(ProgramOperationProviderConfig config) {
		m_config = config;
	}
	
	@Override
	public void invoke(OperationVariable[] inputVars,
										OperationVariable[] inoutputVars,
										OperationVariable[] outputVars)
		throws AssetConnectionException {
    	try {
    		ProcessBasedMDTOperation.Builder builder
			    			= ProcessBasedMDTOperation.builder()
													.setCommand(m_config.getCommand())
													.setWorkingDirectory(m_config.getWorkingDirectory())
													.setTimeout(m_config.getTimeout());

    		for ( OperationVariable var: inputVars ) {
    			addArgument(builder, var, false);
    		}
    		for ( OperationVariable var: inoutputVars ) {
    			addArgument(builder, var, true);
    		}

    		ProcessBasedMDTOperation op = builder.build();
    		Map<String,String> results = op.run();
    		
    		List<String> inoutputVarNames = FStream.of(inoutputVars)
    												.flatMapNullable(var -> var.getValue().getIdShort())
    												.toList();
    		for ( Map.Entry<String,String> ent: results.entrySet() ) {
				SubmodelElement sme = AASUtils.readJson(ent.getValue(), SubmodelElement.class);
				
    			int idx = inoutputVarNames.indexOf(ent.getKey());
    			if ( idx >= 0 ) {
    				inoutputVars[idx].setValue(sme);
    			}
    			else {
    				Funcs.findFirstIndexed(Arrays.asList(outputVars),
    										v -> v.getValue().getIdShort().equals(ent.getKey()))
    					.ifPresent(idxed -> outputVars[idx] = toOperationVariable(sme))
    					.getOrThrow(() -> {
        					String msg = String.format("Unknown output/inoutput variable: name=%s", ent.getKey());
        					return new AssetConnectionException(msg);
    					});
    			}
    		}
		}
		catch ( AssetConnectionException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new AssetConnectionException(e);
		}
	}
	
	private OperationVariable toOperationVariable(SubmodelElement value) {
		return new DefaultOperationVariable.Builder().value(value).build();
	}
	
	private ProcessBasedMDTOperation.Builder addArgument(ProcessBasedMDTOperation.Builder builder,
															OperationVariable var, boolean output) {
		SubmodelElement sme = var.getValue();
		String argName = sme.getIdShort();
		
		Object argValue = m_config.getValueArguments().contains(argName) ? ElementValues.getValue(sme) : sme;
		String argValueStr = AASUtils.writeJson(argValue);
		return builder.addFileArgument(argName, argValueStr, output);
	}
}
