package mdt.persistence.asset.jdbc;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import utils.func.FOption;
import utils.json.JacksonUtils;
import utils.stream.FStream;

import mdt.ElementLocation;
import mdt.persistence.asset.AssetVariableConfig;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MultiRowAssetVariableConfig extends AbstractJdbcAssetVariableConfig implements AssetVariableConfig {
	public static final String SERIALIZATION_TYPE = "mdt:asset:jdbc:multi-row";

	private static final String FIELD_READ_QUERY = "readQuery";
	private static final String FIELD_UPDATE_QUERY = "updateQuery";
	private static final String FIELD_ROWS = "rows";
	
	private String m_readQuery;
	@Nullable private String m_updateQuery;
	private List<RowDefConfig> m_rowDefs;
	
	private MultiRowAssetVariableConfig()  { }
	public MultiRowAssetVariableConfig(ElementLocation elementKey,
										@Nullable String jdbcConfigKey, @Nullable Duration validPeriod,
										String readQuery, @Nullable String updateQuery, List<RowDefConfig> rowDefs) {
		super(elementKey, jdbcConfigKey, validPeriod);
		
		m_readQuery = readQuery;
		m_updateQuery = updateQuery;
		m_rowDefs = rowDefs;
	}
	
	public String getReadQuery() {
		return m_readQuery;
	}
	
	public String getUpdateQuery() {
		return m_updateQuery;
	}
	
	public List<RowDefConfig> getRowDefs() {
		return m_rowDefs;
	}

	@Override
	public String getSerializationType() {
		return SERIALIZATION_TYPE;
	}

	@Override
	public void serializeFields(JsonGenerator gen) throws IOException {
		super.serializeFields(gen);
		
		gen.writeStringField(FIELD_READ_QUERY, m_readQuery);
		FOption.acceptOrThrow(m_updateQuery, q -> gen.writeStringField(FIELD_UPDATE_QUERY, q));
		
		gen.writeArrayFieldStart(FIELD_ROWS);
		FStream.from(this.m_rowDefs).forEachOrThrow(col -> col.serialize(gen));
		gen.writeEndArray();
	}

	public static MultiRowAssetVariableConfig deserializeFields(JsonNode jnode) {
		MultiRowAssetVariableConfig config = new MultiRowAssetVariableConfig();
		config.loadFields(jnode);
		
		config.m_readQuery = FOption.ofNullable(jnode.get(FIELD_READ_QUERY))
									.map(JsonNode::asText)
									.getOrThrow(() -> new IllegalArgumentException("missing '" + FIELD_READ_QUERY + "' field"));
		config.m_updateQuery = FOption.map(jnode.get(FIELD_UPDATE_QUERY), JsonNode::asText);
		config.m_rowDefs = FStream.from(jnode.get("rows").elements())
									.mapOrThrow(RowDefConfig::deserialize)
									.toList();
		
		return config;
	}

	public static class RowDefConfig {
		private final String m_key;
		private final String m_path;
		
		public RowDefConfig(String key, String path) {
			m_key = key;
			m_path = path;
		}
		
		public String getKey() {
			return m_key;
		}

		public String getPath() {
			return m_path;
		}
		
		private void serialize(JsonGenerator gen) throws IOException {
			gen.writeStartObject();
			gen.writeStringField("key", m_key);
			gen.writeStringField("path", m_path);
			gen.writeEndObject();
		}
		
		private static RowDefConfig deserialize(JsonNode jnode) {
			return FStream.from(jnode.fields())
							.map(ent -> {
								String key = JacksonUtils.getStringField(jnode, "key");
								String path = JacksonUtils.getStringField(jnode, "path");
								return new RowDefConfig(key, path);
							})
							.findFirst()
							.get();
		}
	};
}
