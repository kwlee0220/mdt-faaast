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
public class MultiColumnAssetParameterConfig implements AssetParameterConfig {
	private static String READ_QUERY_FORMAT = "select %s from %s %s";
	
	private String submodel;
	private String path;
	private String jdbcConfigKey;
	private String table;
	private String whereClause;
	private String readQuery;
	private List<ColumnConfig> columns;
	
	@Getter
	@NoArgsConstructor
	public static class ColumnConfig {
		private String name;
		private String path;
	};

	@Override
	public void serialize(JsonGenerator gen) throws IOException {
		gen.writeStartObject();
		gen.writeStringField("@class", MultiColumnAssetParameter.class.getName());
		gen.writeStringField("submodel", this.submodel);
		gen.writeStringField("path", this.path);
		gen.writeStringField("jdbcConfigKey", this.jdbcConfigKey);
		gen.writeStringField("table", this.table);
		gen.writeStringField("whereClause", this.whereClause);
		gen.writeArrayFieldStart("columns");
		FStream.from(this.columns).forEachOrThrow(gen::writeObject);
		gen.writeEndArray();
		gen.writeEndObject();
	}
	
	public static MultiColumnAssetParameterConfig parseJson(JsonNode jnode) throws IOException {
		MultiColumnAssetParameterConfig config = new MultiColumnAssetParameterConfig();
		config.submodel = jnode.get("submodel").asText();
		config.path = jnode.get("path").asText();
		config.jdbcConfigKey = FOption.map(jnode.get("jdbcConfigKey"), JsonNode::asText);
		config.table = jnode.get("table").asText();
		config.whereClause = jnode.get("whereClause").asText();
		config.columns = FStream.from(jnode.get("columns").elements())
								.mapOrThrow(json -> MDTModelSerDe.getJsonMapper().treeToValue(json, ColumnConfig.class))
								.toList();
		
		String colCsv = FStream.from(config.columns).map(ColumnConfig::getName).join(", ");
		config.readQuery = String.format(READ_QUERY_FORMAT, colCsv, config.table, config.whereClause);
		
		return config;
	}
	
	@Override
	public String toString() {
		return String.format("%s/%s", this.submodel, this.path);
	}
}
