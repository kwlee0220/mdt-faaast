package mdt.persistence.asset.jdbc;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import utils.json.JacksonUtils;
import utils.stream.FStream;

import mdt.ElementLocation;
import mdt.persistence.asset.AssetVariableConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MultiColumnAssetVariableConfig extends AbstractJdbcAssetVariableConfig implements AssetVariableConfig {
	public static final String SERIALIZATION_TYPE = "mdt:asset:jdbc:multi-column";
	private static final String FIELD_TABLE = "table";
	private static final String FIELD_WHERE_CLAUSE = "whereClause";
	private static final String FIELD_COLUMNS = "columns";
	
	private static String READ_QUERY_FORMAT = "select %s from %s %s";
	private static String UPDATE_QUERY_FORMAT = "update %s set %s %s";
	
	private String m_table;
	private String m_whereClause;
	private List<ColumnConfig> m_columns;
	private String m_readQuery;
	private String m_updateQuery;
	
	private MultiColumnAssetVariableConfig()  {}
	public MultiColumnAssetVariableConfig(ElementLocation elementKey,
											@Nullable String jdbcConfigKey, @Nullable Duration validPeriod,
											String table, String whereClause, List<ColumnConfig> columns)  {
		super(elementKey, jdbcConfigKey, validPeriod);
		
		m_table = table;
		m_whereClause = whereClause;
		m_columns = columns;
	}
	
	public List<ColumnConfig> getColumns() {
        return m_columns;
    }
	
	public String getReadQuery() {
		if ( m_readQuery == null ) {
			String colCsv = FStream.from(m_columns).map(ColumnConfig::getName).join(", ");
			m_readQuery = String.format(READ_QUERY_FORMAT, colCsv, m_table, m_whereClause);
		}
		return m_readQuery;
	}
	
	public String getUpdateQuery() {
		if ( m_updateQuery == null ) {
			String setClause = FStream.from(m_columns)
										.map(col -> String.format("%s = ?", col.m_column))
										.join(", ");
			m_updateQuery = String.format(UPDATE_QUERY_FORMAT, m_table, setClause, m_whereClause);
		}
		return m_updateQuery;
	}

	@Override
	public String getSerializationType() {
		return SERIALIZATION_TYPE;
	}
	
	@Override
	public void serializeFields(JsonGenerator gen) throws IOException {
		super.serializeFields(gen);
		
		gen.writeStringField(FIELD_TABLE, m_table);
		gen.writeStringField(FIELD_WHERE_CLAUSE, m_whereClause);
		gen.writeArrayFieldStart(FIELD_COLUMNS);
		FStream.from(this.m_columns).forEachOrThrow(col -> col.serialize(gen));
		gen.writeEndArray();
	}

	public static MultiColumnAssetVariableConfig deserializeFields(JsonNode jnode) {
		MultiColumnAssetVariableConfig config = new MultiColumnAssetVariableConfig();
		config.loadFields(jnode);

		config.m_table = JacksonUtils.getStringField(jnode, FIELD_TABLE);
		config.m_whereClause = JacksonUtils.getStringField(jnode, FIELD_WHERE_CLAUSE);
		config.m_columns = FStream.from(jnode.get("columns").elements())
									.mapOrThrow(ColumnConfig::deserialize)
									.toList();
		
		return config;
	}
	
	public static class ColumnConfig {
		private final String m_column;
		private final String m_path;
		
		public ColumnConfig(String column, String path) {
            m_column = column;
            m_path = path;
		}
		
		public String getName() {
            return m_column;
        }
		
		public String getPath() {
			return m_path;
		}
		
		private static ColumnConfig deserialize(JsonNode jnode) {
			return FStream.from(jnode.elements())
							.map(ent -> {
								String name = JacksonUtils.getStringField(jnode, "column");
								String path = JacksonUtils.getStringField(jnode, "path");
								return new ColumnConfig(name, path);
							})
							.findFirst()
							.get();
		}
		
		private void serialize(JsonGenerator gen) throws IOException {
			gen.writeStartObject();
			gen.writeStringField("column", m_column);
			gen.writeStringField("path", m_path);
			gen.writeEndObject();
		}
	};
}
