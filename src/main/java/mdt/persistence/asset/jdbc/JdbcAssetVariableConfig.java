package mdt.persistence.asset.jdbc;

import java.io.IOException;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import utils.func.FOption;

import mdt.persistence.asset.AssetVariableConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class JdbcAssetVariableConfig extends JdbcAssetVariableConfigBase implements AssetVariableConfig {
	private String m_readQuery;
	@Nullable private String m_updateQuery;
	
	public String getReadQuery() {
		return m_readQuery;
	}
	
	public String getUpdateQuery() {
		return m_updateQuery;
	}
	
	@Override
	public void serialize(JsonGenerator gen) throws IOException {
		gen.writeStartObject();
		
		gen.writeStringField("@class", JdbcAssetVariable.class.getName());
		super.serialize(gen);
		
		gen.writeStringField("readQuery", m_readQuery);
		FOption.acceptOrThrow(m_updateQuery, query -> gen.writeStringField("updateQuery", query));
		
		gen.writeEndObject();
	}
	
	/**
	 * JSON 노드로부터 {@link JdbcAssetVariableConfig} 객체를 생성한다.
	 * <p>
	 * 본 메소드는 {@link AssetVariableConfig.Deserializer}에서 호출된다.
	 * 
	 * @param jnode	JSON 노드
	 * @return	생성된 {@link JdbcAssetVariableConfig} 객체.
	 */
	public static JdbcAssetVariableConfig parseJson(JsonNode jnode) {
		JdbcAssetVariableConfig config = new JdbcAssetVariableConfig();
		parseJson(config, jnode);
		
		config.m_readQuery = FOption.ofNullable(jnode.get("readQuery"))
									.map(JsonNode::asText)
									.getOrThrow(() -> new IllegalArgumentException("missing 'readQuery' field"));
		config.m_updateQuery = FOption.map(jnode.get("updateQuery"), JsonNode::asText);
		
		return config;
	}
}
