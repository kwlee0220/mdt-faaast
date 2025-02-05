package mdt.persistence.asset.jdbc;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import utils.stream.FStream;

import mdt.persistence.asset.AssetVariableConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class JdbcCompositeAssetVariableConfig extends JdbcAssetVariableConfigBase implements AssetVariableConfig {
	private List<JdbcAssetVariableConfig> m_components;
	
	public List<JdbcAssetVariableConfig> getComponents() {
		return m_components;
	}

	@Override
	public void serialize(JsonGenerator gen) throws IOException {
		gen.writeStartObject();
		
		gen.writeStringField("@class", JdbcCompositeAssetVariable.class.getName());
		super.serialize(gen);

		gen.writeArrayFieldStart("components");
		for ( JdbcAssetVariableConfig paramConf: this.m_components ) {
			gen.writeObject(paramConf);
		}
		gen.writeEndArray();
		
		gen.writeEndObject();
	}
	
	public static JdbcCompositeAssetVariableConfig parseJson(JsonNode jnode) {
		JdbcCompositeAssetVariableConfig config = new JdbcCompositeAssetVariableConfig();
		parseJson(config, jnode);
		config.m_components = FStream.from(jnode.get("components").elements())
									.map(JdbcAssetVariableConfig::parseJson)
									.toList();
		
		return config;
	}
}
