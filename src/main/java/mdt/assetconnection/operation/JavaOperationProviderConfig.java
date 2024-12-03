package mdt.assetconnection.operation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter
@JsonInclude(Include.NON_NULL)
public class JavaOperationProviderConfig {
	private final String operationClassName;
	
	@JsonCreator
	public JavaOperationProviderConfig(@JsonProperty("operationClassName") String className) {
		this.operationClassName = className;
	}
}
