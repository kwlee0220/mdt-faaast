package mdt.persistence.asset.jdbc;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import utils.func.FOption;
import utils.stream.FStream;

import mdt.model.MDTModelSerDe;
import mdt.persistence.asset.AssetVariableConfig;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MultiRowAssetVariableConfig extends JdbcAssetVariableConfigBase implements AssetVariableConfig {
	private String m_readQuery;
	@Nullable private String m_updateQuery;
	private List<RowDefConfig> m_rowDefs;
	
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
	public void serialize(JsonGenerator gen) throws IOException {
		gen.writeStartObject();
		gen.writeStringField("@class", MultiRowAssetVariable.class.getName());
		super.serialize(gen);

		gen.writeStringField("readQuery", m_readQuery);
		FOption.acceptOrThrow(m_updateQuery, query -> gen.writeStringField("updateQuery", query));
		gen.writeArrayFieldStart("rows");
		FStream.from(this.m_rowDefs).forEachOrThrow(gen::writeObject);
		gen.writeEndArray();
		
		gen.writeEndObject();
	}
	
	public static MultiRowAssetVariableConfig parseJson(JsonNode jnode) throws IOException {
		MultiRowAssetVariableConfig config = new MultiRowAssetVariableConfig();
		parseJson(config, jnode);
		
		config.m_readQuery = FOption.ofNullable(jnode.get("readQuery"))
									.map(JsonNode::asText)
									.getOrThrow(() -> new IllegalArgumentException("missing 'readQuery' field"));
		config.m_updateQuery = FOption.map(jnode.get("updateQuery"), JsonNode::asText);
		config.m_rowDefs = FStream.from(jnode.get("rows").elements())
								.mapOrThrow(json -> MDTModelSerDe.getJsonMapper().treeToValue(json, RowDefConfig.class))
								.toList();
		
		return config;
	}

	public static class RowDefConfig {
		@JsonProperty("key") private final String m_key;
		@JsonProperty("path") private final String m_path;
		
		public RowDefConfig(@JsonProperty("key") String key,
							@JsonProperty("path") String path) {
			m_key = key;
			m_path = path;
		}
		
		public String getKey() {
			return m_key;
		}

		public String getPath() {
			return m_path;
		}
	};
}
