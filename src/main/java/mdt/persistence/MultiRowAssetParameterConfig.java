package mdt.persistence;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import utils.func.FOption;
import utils.stream.FStream;

import lombok.Getter;
import lombok.NoArgsConstructor;
import mdt.model.MDTModelSerDe;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter
public class MultiRowAssetParameterConfig implements AssetParameterConfig {
	private String submodel;
	private String path;
	private String jdbcConfigKey;
	private String readQuery;
	private String updateQuery;
	private List<RowDefConfig> rows;

	@Getter
	@NoArgsConstructor
	public static class RowDefConfig {
		private String key;
		private String path;
	};

	@Override
	public void serialize(JsonGenerator gen) throws IOException {
		gen.writeStartObject();
		gen.writeStringField("@class", MultiRowAssetParameter.class.getName());
		gen.writeStringField("submodel", this.submodel);
		gen.writeStringField("path", this.path);
		gen.writeStringField("jdbcConfigKey", this.jdbcConfigKey);
		gen.writeStringField("readQuery", this.readQuery);
		gen.writeStringField("updateQuery", this.updateQuery);
		gen.writeArrayFieldStart("rows");
		FStream.from(this.rows).forEachOrThrow(gen::writeObject);
		gen.writeEndArray();
		gen.writeEndObject();
	}
	
	public static MultiRowAssetParameterConfig parseJson(JsonNode jnode) throws IOException {
		MultiRowAssetParameterConfig config = new MultiRowAssetParameterConfig();
		config.submodel = jnode.get("submodel").asText();
		config.path = jnode.get("path").asText();
		config.jdbcConfigKey = FOption.map(jnode.get("jdbcConfigKey"), JsonNode::asText);
		config.readQuery = jnode.get("readQuery").asText();
		config.updateQuery = jnode.get("updateQuery").asText();
		config.rows = FStream.from(jnode.get("rows").elements())
								.mapOrThrow(json -> MDTModelSerDe.getJsonMapper().treeToValue(json, RowDefConfig.class))
								.toList();
		return config;
	}
	
	@Override
	public String toString() {
		return String.format("%s/%s", this.submodel, this.path);
	}
}
