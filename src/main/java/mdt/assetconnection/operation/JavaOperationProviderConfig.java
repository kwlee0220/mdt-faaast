package mdt.assetconnection.operation;

import java.time.Duration;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;

import com.fasterxml.jackson.annotation.JsonProperty;

import utils.Utilities;

import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionException;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter @Setter
public class JavaOperationProviderConfig {
	private Class<? extends OperationProvider> operationClass;
	private Duration timeout;
	
	@JsonProperty("operationClass")
	public String getOperationClassAsString() {
		return this.operationClass.toString();
	}

	@SuppressWarnings("unchecked")
	@JsonProperty("operationClass")
	public void setOperationClass(String className) {
		try {
			this.operationClass = (Class<? extends OperationProvider>)Class.forName(className);
		}
		catch ( ClassNotFoundException e ) {
			String msg = String.format("Failed to load OperationHandler class: %s", className);
			throw new IllegalArgumentException(msg);
		}
	}
	
	@JsonProperty("timeout")
	public String getTimeoutAsString() {
		return this.timeout.toString();
	}

	@JsonProperty("timeout")
	public void setTimeout(String str) {
		this.timeout = Duration.parse(str);
	}
	
	public void invoke(OperationVariable[] inputVars, OperationVariable[] inoutputVars,
						OperationVariable[] outputVars) throws Exception {
    	try {
    		OperationProvider jop = Utilities.newInstance(this.operationClass);
    		jop.invoke(inputVars, inoutputVars, outputVars);
		}
		catch ( AssetConnectionException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new AssetConnectionException(e);
		}
	}
}
