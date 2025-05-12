package mdt.persistence.old;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

import de.fraunhofer.iosb.ilt.faaast.service.persistence.PersistenceConfig;
import mdt.persistence.asset.AssetVariableConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MDTPersistenceConfig extends PersistenceConfig<MDTPersistence> {
	private final List<AssetVariableConfig> m_assetVariableConfigs;
	
	public MDTPersistenceConfig(@JsonProperty("assetVariables") List<AssetVariableConfig> configs) {
		Preconditions.checkArgument(configs != null, "null assetVariables");
		
		m_assetVariableConfigs = configs;
	}
	
	@JsonProperty("assetVariables")
	public List<AssetVariableConfig> getAssetVariableConfigs() {
		return m_assetVariableConfigs;
	}
}
