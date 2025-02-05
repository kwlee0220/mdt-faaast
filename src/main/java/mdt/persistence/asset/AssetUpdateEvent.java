package mdt.persistence.asset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class AssetUpdateEvent {
	@JsonIgnore private final String m_submodelIdShort;
	@JsonIgnore private final String m_elementPath;
	@JsonIgnore private final String m_update;
	
	public AssetUpdateEvent(@JsonProperty("submodel") String submodelIdShort,
							@JsonProperty ("path") String elementPath,
							@JsonProperty ("update") String update) {
		m_submodelIdShort = submodelIdShort;
		m_elementPath = elementPath;
		m_update = update;
	}
	
	@JsonProperty("submodel")
	public String getSubmodel() {
		return m_submodelIdShort;
	}
	
	@JsonProperty ("path")
	public String getPath() {
		return m_elementPath;
	}
	
	@JsonProperty ("update")
	public String getUpdate() {
		return m_update;
	}
}
