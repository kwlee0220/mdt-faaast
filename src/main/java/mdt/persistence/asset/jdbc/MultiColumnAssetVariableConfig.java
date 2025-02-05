package mdt.persistence.asset.jdbc;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import utils.stream.FStream;

import mdt.model.MDTModelSerDe;
import mdt.persistence.asset.AssetVariableConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MultiColumnAssetVariableConfig extends JdbcAssetVariableConfigBase implements AssetVariableConfig {
	private static String READ_QUERY_FORMAT = "select %s from %s %s";
	private static String UPDATE_QUERY_FORMAT = "update %s set %s %s";
	
	private String m_table;
	private String m_whereClause;
	private List<ColumnConfig> m_columns;
	private String m_readQuery;
	private String m_updateSql;
	
	public List<ColumnConfig> getColumns() {
        return m_columns;
    }
	
	public String getReadQuery() {
		return m_readQuery;
	}
	
	public String getUpdateSql() {
		return m_updateSql;
	}

	@Override
	public void serialize(JsonGenerator gen) throws IOException {
		gen.writeStartObject();
		gen.writeStringField("@class", MultiColumnAssetVariable.class.getName());
		super.serialize(gen);
		
		gen.writeStringField("table", m_table);
		gen.writeStringField("whereClause", m_whereClause);
		gen.writeArrayFieldStart("columns");
		FStream.from(this.m_columns).forEachOrThrow(gen::writeObject);
		gen.writeEndArray();
		
		gen.writeEndObject();
	}
	
	public static MultiColumnAssetVariableConfig parseJson(JsonNode jnode) throws IOException {
		MultiColumnAssetVariableConfig config = new MultiColumnAssetVariableConfig();
		parseJson(config, jnode);
		
		config.m_table = jnode.get("table").asText();
		config.m_whereClause = jnode.get("whereClause").asText();
		config.m_columns = FStream.from(jnode.get("columns").elements())
								.mapOrThrow(json -> MDTModelSerDe.getJsonMapper().treeToValue(json, ColumnConfig.class))
								.toList();
		
		String colCsv = FStream.from(config.m_columns).map(ColumnConfig::getName).join(", ");
		config.m_readQuery = String.format(READ_QUERY_FORMAT, colCsv, config.m_table, config.m_whereClause);
		
		String setClause = FStream.from(config.m_columns)
									.map(col -> String.format("%s = ?", col.m_name))
									.join(", ");
		config.m_updateSql = String.format(UPDATE_QUERY_FORMAT, config.m_table, setClause, config.m_whereClause);
		
		return config;
	}
	
	public static class ColumnConfig {
		private String m_name;
		private String m_path;
		
		public ColumnConfig(@JsonProperty("name") String name, @JsonProperty("path") String path) {
            m_name = name;
            m_path = path;
		}
		
		public String getName() {
            return m_name;
        }
		
		public String getPath() {
			return m_path;
		}
	};
}
