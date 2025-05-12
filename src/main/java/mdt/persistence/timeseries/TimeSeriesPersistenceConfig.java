package mdt.persistence.timeseries;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import mdt.persistence.PersistenceStackConfig;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class TimeSeriesPersistenceConfig extends PersistenceStackConfig<TimeSeriesPersistence> {
	private List<TimeSeriesSubmodelConfig> m_timeSeriesSubmodelConfigs;

	@JsonProperty("timeSeriesSubmodels")
	public void setTimeSeriesSubmodelConfigs(List<TimeSeriesSubmodelConfig> tsSubmodels) {
		m_timeSeriesSubmodelConfigs = tsSubmodels;
	}

	@JsonProperty("timeSeriesSubmodels")
	public List<TimeSeriesSubmodelConfig> getTimeSeriesSubmodelConfigs() {
		return m_timeSeriesSubmodelConfigs;
	}
}
