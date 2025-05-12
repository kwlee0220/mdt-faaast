package mdt.persistence.old;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import utils.stream.FStream;

import mdt.persistence.mqtt.MqttBrokerConfig;

import de.fraunhofer.iosb.ilt.faaast.service.persistence.PersistenceConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MDTParameterPersistenceConfig extends PersistenceConfig<MDTParametersPersistence> {
	private MqttBrokerConfig m_mqttBrokerConfig;
	private Map<String,MDTParameterConfig> m_paramConfigs;
	
	public MDTParameterPersistenceConfig(@JsonProperty("mqttBroker") MqttBrokerConfig brokerConfig,
										@JsonProperty("parameters") List<MDTParameterConfig> paramConfigs) {
		Preconditions.checkArgument(paramConfigs != null, "null assetVariables");
		
		m_mqttBrokerConfig = brokerConfig;
		m_paramConfigs = FStream.from(paramConfigs)
								.tagKey(MDTParameterConfig::getName)
								.toMap();
	}
	
	public MDTParameterConfig getParameterConfig(String name) {
		return m_paramConfigs.get(name);
	}
	
	@JsonProperty("parameters")
	public List<MDTParameterConfig> getParameters() {
		return Lists.newArrayList(m_paramConfigs.values());
	}

	@JsonProperty("mqttBroker")
	public MqttBrokerConfig getMqttBroker() {
		return m_mqttBrokerConfig;
	}
    
    @Override
	public String toString() {
		return String.format("%s: model=%s", getClass().getName(), getInitialModelFile());
	}
}
