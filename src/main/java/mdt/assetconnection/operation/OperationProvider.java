package mdt.assetconnection.operation;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public interface OperationProvider {
	public void invoke(OperationVariable[] inputVars,
						OperationVariable[] inoutputVars,
						OperationVariable[] outputVars) throws Exception;
}
