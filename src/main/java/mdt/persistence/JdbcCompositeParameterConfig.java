package mdt.persistence;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import utils.func.FOption;
import utils.stream.FStream;

import lombok.Getter;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter
public class JdbcCompositeParameterConfig implements AssetParameterConfig {
	private String submodel;
	private String path;
	@Nullable private String jdbcConfigKey;
	private List<JdbcAssetParameterConfig> parameters;

	@Override
	public void serialize(JsonGenerator gen) throws IOException {
		gen.writeStartObject();
		gen.writeStringField("@class", JdbcCompositeParameter.class.getName());
		gen.writeStringField("submodel", this.submodel);
		gen.writeStringField("path", this.path);
		if ( this.jdbcConfigKey != null ) {
			gen.writeStringField("jdbcConfigKey", this.jdbcConfigKey);
		}
		gen.writeArrayFieldStart("parameters");
		for ( JdbcAssetParameterConfig paramConf: this.parameters ) {
			gen.writeObject(paramConf);
		}
		gen.writeEndArray();
		gen.writeEndObject();
	}
	
	public static JdbcCompositeParameterConfig parseJson(JsonNode jnode) {
		JdbcCompositeParameterConfig config = new JdbcCompositeParameterConfig();
		config.submodel = jnode.get("submodel").asText();
		config.path = jnode.get("path").asText();
		config.jdbcConfigKey = FOption.map(jnode.get("jdbcConfigKey"), JsonNode::asText);
		config.parameters = FStream.from(jnode.get("parameters").elements())
									.map(JdbcAssetParameterConfig::parseJson)
									.toList();
		return config;
	}
	
	@Override
	public String toString() {
		return String.format("%s/%s", this.submodel, this.path);
	}
}
