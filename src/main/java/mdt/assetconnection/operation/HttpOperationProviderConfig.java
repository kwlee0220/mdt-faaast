package mdt.assetconnection.operation;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

import utils.UnitUtils;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter
@JsonInclude(Include.NON_NULL)
public class HttpOperationProviderConfig {
	private final String endpoint;
	private final String opId;
	private final Duration pollInterval;
	
	@JsonCreator
	public HttpOperationProviderConfig(@JsonProperty("endpoint") String endpoint,
										@JsonProperty("opId") String opId,
										@JsonProperty("pollInterval") String pollInterval) {
		this.endpoint = endpoint;
		this.opId = opId;
		this.pollInterval = UnitUtils.parseDuration(pollInterval);
	}
	
	@JsonProperty("pollInterval")
	public String getpollIntervalAsString() {
		return this.pollInterval.toString();
	}
	
	@Override
	public String toString() {
		return String.format("HttpOperation[server=%s, opId=%s, poll=%s]",
								this.endpoint, this.opId, this.pollInterval);
	}
}
