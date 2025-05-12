package mdt.persistence.mqtt;

import java.util.List;

import javax.annotation.Nullable;

import utils.func.FOption;

import mdt.persistence.PersistenceStackConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MqttPublishingPersistenceConfig extends PersistenceStackConfig<MqttPublishingPersistence> {
	private List<MqttElementPublisher> m_publishers;
	@Nullable private List<MqttElementSubscriber> m_subscribers;

	public List<MqttElementPublisher> getPublishers() {
		return m_publishers;
	}
	
	public void setPublishers(List<MqttElementPublisher> publishers) {
		m_publishers = publishers;
	}
	
	public List<MqttElementSubscriber> getSubscribers() {
		return FOption.getOrElse(m_subscribers, List.of());
	}
	
	public void setSubscribers(List<MqttElementSubscriber> subscribers) {
		m_subscribers = subscribers;
	}
}
