package mdt.persistence.old;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter
@Accessors(prefix="m_")
public class MDTParameterConfig {
	private final String m_name;
	private final boolean m_publishing;
	
	public MDTParameterConfig(@JsonProperty("name") String name, @JsonProperty("publishing") boolean publishing) {
		m_name = name;
		m_publishing = publishing;
	}
	
	@Override
	public String toString() {
		return String.format("%s: name=%s, publishing=%s", getClass().getSimpleName(), m_name, m_publishing);
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null || !(obj instanceof MDTParameterConfig) ) {
			return false;
		}

		MDTParameterConfig other = (MDTParameterConfig) obj;
		return m_name.equals(other.m_name);
	}
	
	@Override
	public int hashCode() {
		return m_name.hashCode();
	}
}
