package mdt.persistence.asset;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

import mdt.persistence.PersistenceStackConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class AssertVariableBasedPersistenceConfig extends PersistenceStackConfig<AssertVariableBasedPersistence> {
	private final List<AssetVariableConfig> m_assetVariableConfigs;
	
	public AssertVariableBasedPersistenceConfig(@JsonProperty("assetVariables") List<AssetVariableConfig> configs) {
		Preconditions.checkArgument(configs != null, "null assetVariables");
		
		m_assetVariableConfigs = configs;
	}
	
	@JsonProperty("assetVariables")
	public List<AssetVariableConfig> getAssetVariableConfigs() {
		return m_assetVariableConfigs;
	}
}
