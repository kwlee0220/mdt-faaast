package mdt.persistence;

import java.io.IOException;
import java.time.Duration;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import utils.UnitUtils;
import utils.func.FOption;

import lombok.Getter;
import lombok.Setter;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter @Setter
public class JdbcAssetParameterConfig implements AssetParameterConfig {
	private String submodel;
	private String path;
	@Nullable private String jdbcConfigKey;
	private String readQuery;
	private String updateQuery;
	@Nullable private Duration validPeriod;
	
	@JsonProperty("validPeriod")
	public String getValidPeriodString() {
		return this.validPeriod.toString();
	}

	@Override
	public void serialize(JsonGenerator gen) throws IOException {
		gen.writeStartObject();
		gen.writeStringField("@class", JdbcAssetParameter.class.getName());
		gen.writeStringField("submodel", this.submodel);
		gen.writeStringField("path", this.path);
		if ( this.jdbcConfigKey != null ) {
			gen.writeStringField("jdbcConfigKey", this.jdbcConfigKey);
		}
		gen.writeStringField("readQuery", this.readQuery);
		gen.writeStringField("updateQuery", this.updateQuery);
		if ( this.validPeriod != null ) {
			gen.writeStringField("validPeriod", getValidPeriodString());
		}
		gen.writeEndObject();
	}
	
	public static JdbcAssetParameterConfig parseJson(JsonNode jnode) {
		JdbcAssetParameterConfig config = new JdbcAssetParameterConfig();
		config.submodel = jnode.get("submodel").asText();
		config.path = jnode.get("path").asText();
		config.jdbcConfigKey = FOption.map(jnode.get("jdbcConfigKey"), JsonNode::asText);
		config.readQuery = jnode.get("readQuery").asText();
		config.updateQuery = jnode.get("updateQuery").asText();
		
		JsonNode periodNode = jnode.get("validPeriod");
		if ( periodNode != null ) {
			config.validPeriod = UnitUtils.parseDuration(periodNode.asText());
		}
		
		return config;
	}
	
	@Override
	public String toString() {
		return String.format("%s/%s", this.submodel, this.path);
	}
}
