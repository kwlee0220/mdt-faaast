package mdt.persistence.asset.jdbc;

import java.io.IOException;
import java.time.Duration;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import utils.UnitUtils;
import utils.func.FOption;

import mdt.persistence.asset.AssetVariableConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class JdbcAssetVariableConfigBase implements AssetVariableConfig {
	private String m_submodelIdShort;
	private String m_elementPath;
	@Nullable private String m_jdbcConfigKey;
	@Nullable private Duration m_validPeriod;
	
	@Override
	public String getSubmodelIdShort() {
		return m_submodelIdShort;
	}
	
	@Override
	public String getElementPath() {
		return m_elementPath;
	}
	
	public String getJdbcConfigKey() {
		return m_jdbcConfigKey;
	}
	
	public Duration getValidPeriod() {
		return FOption.getOrElse(m_validPeriod, Duration.ZERO);
	}
	
	@JsonProperty("validPeriod")
	public String getValidPeriodString() {
		return FOption.map(m_validPeriod, Duration::toString);
	}

	@Override
	public void serialize(JsonGenerator gen) throws IOException {
		gen.writeStringField("submodel", getSubmodelIdShort());
		gen.writeStringField("path", getElementPath());
		
		FOption.acceptOrThrow(getJdbcConfigKey(), key -> gen.writeStringField("jdbcConfigKey", key));
		FOption.acceptOrThrow(m_validPeriod, period -> gen.writeStringField("validPeriod", period.toString()));
	}
	
	/**
	 * JSON 노드로부터 {@link JdbcAssetVariableConfigBase} 객체를 생성한다.
	 * <p>
	 * 본 메소드는 {@link AssetVariableConfig.Deserializer}에서 호출된다.
	 * 
	 * @param jnode	JSON 노드
	 * @return	생성된 {@link JdbcAssetVariableConfigBase} 객체.
	 */
	protected static void parseJson(JdbcAssetVariableConfigBase config, JsonNode jnode) {
		config.m_submodelIdShort = jnode.get("submodel").asText();
		config.m_elementPath = jnode.get("path").asText();
		config.m_jdbcConfigKey = FOption.map(jnode.get("jdbcConfigKey"), JsonNode::asText);
		config.m_validPeriod = FOption.map(jnode.get("validPeriod"), vn -> UnitUtils.parseDuration(vn.asText()));
	}
	
	@Override
	public String toString() {
		String jdbcKey = FOption.getOrElse(m_jdbcConfigKey, "");
		String validStr = FOption.getOrElse(m_validPeriod, Duration.ZERO).toString();
		return String.format("%s/%s, jdbc=%s, valid=%s", m_submodelIdShort, m_elementPath,
															jdbcKey, validStr);
	}
}
