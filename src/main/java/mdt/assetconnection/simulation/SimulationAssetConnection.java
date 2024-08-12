package mdt.assetconnection.simulation;

import org.eclipse.digitaltwin.aas4j.v3.model.Reference;

import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AbstractAssetConnection;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionException;
import mdt.assetconnection.UnsupportedSubscriptionProvider;
import mdt.assetconnection.UnsupportedSubscriptionProviderConfig;
import mdt.assetconnection.UnsupportedValueProvider;
import mdt.assetconnection.UnsupportedValueProviderConfig;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class SimulationAssetConnection extends AbstractAssetConnection<SimulationAssetConnection,
																		SimulationAssetConnectionConfig,
																		UnsupportedValueProviderConfig,
																		UnsupportedValueProvider,
																		SimulationOperationProviderConfig,
																		SimulationOperationProvider,
																		UnsupportedSubscriptionProviderConfig,
																		UnsupportedSubscriptionProvider> {
	@Override
	public String getEndpointInformation() {
		return null;
	}

	@Override
	protected void doConnect() throws AssetConnectionException { }

	@Override
	protected void doDisconnect() throws AssetConnectionException { }

	@Override
	protected UnsupportedValueProvider createValueProvider(Reference reference,
															UnsupportedValueProviderConfig providerConfig)
		throws AssetConnectionException {
        throw new UnsupportedOperationException("getting value via MQTT currently not supported.");
	}

	@Override
	protected SimulationOperationProvider createOperationProvider(Reference reference,
															SimulationOperationProviderConfig providerConfig)
		throws AssetConnectionException {
		return new SimulationOperationProvider(this.serviceContext, reference, providerConfig);
	}

	@Override
	protected UnsupportedSubscriptionProvider createSubscriptionProvider(Reference reference,
																UnsupportedSubscriptionProviderConfig providerConfig)
		throws AssetConnectionException {
        throw new UnsupportedOperationException("executing subscription via MQTT currently not supported.");
	}
}
