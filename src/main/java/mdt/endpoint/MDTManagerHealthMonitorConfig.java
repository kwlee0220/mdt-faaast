package mdt.endpoint;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

import utils.UnitUtils;
import utils.func.FOption;

import de.fraunhofer.iosb.ilt.faaast.service.endpoint.EndpointConfig;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@JsonInclude(Include.NON_NULL)
public class MDTManagerHealthMonitorConfig extends EndpointConfig<MDTManagerHealthMonitor> {
	private final String m_mdtEndpoint;
	private final Duration m_checkInterval;
	private final boolean m_enabled; 
	
	@JsonCreator
	public MDTManagerHealthMonitorConfig(@JsonProperty("mdtEndpoint") String mdtEndpoint,
										@JsonProperty("checkInterval") String checkInterval,
										@JsonProperty("enabled") Boolean enabled) {
		Preconditions.checkArgument(mdtEndpoint != null, "'mdtEndpoint' is missing");
		Preconditions.checkArgument(checkInterval != null, "'checkInterval' is missing");
		
		m_mdtEndpoint = mdtEndpoint;
		m_checkInterval = UnitUtils.parseDuration(checkInterval);
		m_enabled = FOption.getOrElse(enabled, true);
	}
	
	/**
	 * Health monitoring을 확인할 대상 MDT Manager의 endpoint를 반환한다.
	 * 
	 * @return	Health monitoring 대상 MDT Manager의 endpoint.
	 */
	@JsonProperty("mdtEndpoint")
	public String getMdtEndpoint() {
		return m_mdtEndpoint;
	}
	
	/**
	 * Health monitoring을 수행할 주기를 반환한다.
	 * 
	 * @return Health monitoring 주기.
	 */
	public Duration getCheckInterval() {
		return m_checkInterval;
	}
	
	/**
	 * Health monitoring을 수행할 주기를 반환한다.
	 * 
	 * @return Health monitoring 주기.
	 */
	@JsonProperty("checkInterval")
	public String getCheckIntervalAsString() {
		return m_checkInterval.toString();
	}
	
	/**
	 * Health monitoring 수행 여부를 반환한다.
	 * 
	 * @return Health monitoring 수행 여부.
	 */
	@JsonProperty("enabled")
	public boolean isEnabled() {
		return m_enabled;
	}
}
