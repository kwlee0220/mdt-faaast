package mdt.assetconnection.operation;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;

import utils.Utilities;

import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionException;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class JavaOperationProvider implements OperationProvider {
	private final JavaOperationProviderConfig m_config;
	
	JavaOperationProvider(JavaOperationProviderConfig config) {
		m_config = config;
	}
	
	@Override
	public void invokeSync(OperationVariable[] inputVars, OperationVariable[] inoutputVars,
						OperationVariable[] outputVars) throws Exception {
    	try {
    		OperationProvider jop = Utilities.newInstance(m_config.getOperationClass());
    		jop.invokeSync(inputVars, inoutputVars, outputVars);
		}
		catch ( AssetConnectionException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new AssetConnectionException(e);
		}
	}
}
