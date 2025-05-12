package mdt.endpoint.audit;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import utils.UnitUtils;
import utils.stream.FStream;

import mdt.ElementColumnConfig;

import de.fraunhofer.iosb.ilt.faaast.service.endpoint.EndpointConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@JsonInclude(Include.NON_NULL)
@NoArgsConstructor
@Getter @Setter
public class PeriodicAssetVariableLoggerConfig extends EndpointConfig<PeriodicAssetVariableLogger> {
	private String table;
	private String jdbcConfigKey = "default";
	private List<ElementColumnConfig> columns;
	private Duration interval;
	private boolean distinct = true;
	private boolean enabled = true;
	
	@JsonProperty("interval")
	public String getIntervalString() {
		return interval.toString();
	}
	
	@JsonProperty("interval")
	public void setIntervalString(String intvlStr) {
		this.interval = UnitUtils.parseDuration(intvlStr);
	}
	
	@Override
	public String toString() {
		String colsStr = FStream.from(this.columns)
								.map(ElementColumnConfig::getName)
								.join(", ");
		return String.format("%s{%s}, interval=%s, distinct=%s", this.table, colsStr, this.interval, this.distinct);
	}
}
