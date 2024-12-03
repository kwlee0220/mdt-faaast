package mdt.assetconnection.jdbc;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import utils.UnitUtils;

import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetValueProviderConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@NoArgsConstructor
@Getter @Setter
@JsonInclude(Include.NON_NULL)
public class DefaultJdbcValueProviderConfig implements AssetValueProviderConfig {
	private String readQuery;
	private String updateQuery;
	private Duration validPeriod = Duration.ZERO;
	
	@JsonProperty("validPeriod")
	public String getValidPeriodString() {
		return this.validPeriod.toString();
	}
	
	@JsonProperty("validPeriod")
	public void setValidPeriod(String periodStr) {
		validPeriod = UnitUtils.parseDuration(periodStr);
	}
}
