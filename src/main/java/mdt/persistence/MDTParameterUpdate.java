package mdt.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MDTParameterUpdate {
	private final String m_parameterId;
	private final String m_value;

	public MDTParameterUpdate(@JsonProperty("parameter") String parameterId,
							@JsonProperty("value") String value) {
		m_parameterId = parameterId;
		m_value = value;
	}
	
	@JsonProperty("parameter")
	public String getParameterId() {
		return m_parameterId;
	}

	public String getValue() {
		return m_value;
	}
	
	@Override
	public String toString() {
		return String.format("%s: %s=%s", getClass().getSimpleName(), m_parameterId, m_value);
	}
}