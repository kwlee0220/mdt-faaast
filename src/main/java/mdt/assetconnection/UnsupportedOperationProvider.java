package mdt.assetconnection;

import java.util.function.UnaryOperator;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;

import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionException;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.common.provider.MultiFormatOperationProvider;
import mdt.assetconnection.simulation.SimulationOperationProviderConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class UnsupportedOperationProvider extends MultiFormatOperationProvider<SimulationOperationProviderConfig> {
	protected UnsupportedOperationProvider(SimulationOperationProviderConfig config) {
		super(config);
	}

	@Override
	protected OperationVariable[] getOutputParameters() {
        throw new UnsupportedOperationException("OperationProvider.getOutputParameters");
	}

	@Override
	protected byte[] invoke(byte[] input, UnaryOperator<String> variableReplacer)
		throws AssetConnectionException {
        throw new UnsupportedOperationException("OperationProvider.invoke");
	}
}
