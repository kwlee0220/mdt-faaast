package mdt.endpoint;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

import utils.UnitUtils;
import utils.func.FOption;

import de.fraunhofer.iosb.ilt.faaast.service.endpoint.EndpointConfig;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter
@JsonInclude(Include.NON_NULL)
public class MDTManagerHealthMonitorConfig extends EndpointConfig<MDTManagerHealthMonitor> {
	private String mdtEndpoint;
	private Duration pollInterval;
	private int retryCount;
	private boolean enabled = true; 
	
	@JsonCreator
	public MDTManagerHealthMonitorConfig(@JsonProperty("mdtEndpoint") String mdtEndpoint,
									@JsonProperty("pollInterval") String pollInterval,
									@JsonProperty("retryCount") Integer retryCount) {
		this.mdtEndpoint = mdtEndpoint;
		this.pollInterval = FOption.mapOrElse(pollInterval, UnitUtils::parseDuration, Duration.ofSeconds(10)); 
		this.retryCount = FOption.getOrElse(retryCount, 2);
	}
	
	@JsonProperty("pollInterval")
	public String getPollIntervalAsString() {
		return this.pollInterval.toString();
	}
}
