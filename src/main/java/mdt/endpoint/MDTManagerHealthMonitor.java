package mdt.endpoint;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.util.concurrent.AbstractScheduledService;

import utils.Throwables;
import utils.http.HttpRESTfulClient;
import utils.http.JacksonErrorEntityDeserializer;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.config.CoreConfig;
import de.fraunhofer.iosb.ilt.faaast.service.endpoint.Endpoint;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationInitializationException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.EndpointException;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MDTManagerHealthMonitor extends AbstractScheduledService
										implements Endpoint<MDTManagerHealthMonitorConfig> {
	private static final Logger s_logger = LoggerFactory.getLogger(MDTManagerHealthMonitor.class);

	private MDTManagerHealthMonitorConfig m_config;
	private String m_url;
	private HttpRESTfulClient m_restfulClient;
	private Duration m_pollInterval;
	private int m_retryCount;
	
	private int m_retryRemains;

	@Override
	public void init(CoreConfig coreConfig, MDTManagerHealthMonitorConfig config, ServiceContext serviceContext)
		throws ConfigurationInitializationException {
		m_config = config;
		
		m_url = String.format("%s/health", config.getMdtEndpoint());
		JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
		m_restfulClient = HttpRESTfulClient.builder()
											.errorEntityDeserializer(new JacksonErrorEntityDeserializer(mapper))
											.build();
		m_pollInterval = config.getPollInterval();
		m_retryCount = config.getRetryCount();
		m_retryRemains = m_retryCount;
	}

	@Override
	public MDTManagerHealthMonitorConfig asConfig() {
		return m_config;
	}
	
    @Override
    public void start() throws EndpointException {
    	if ( m_config.isEnabled() ) {
    		startAsync();
    	}
    }

    @Override
    public void stop() {
    	if ( m_config.isEnabled() ) {
    		stopAsync();
    	}
    }

	@Override
	protected void runOneIteration() throws Exception {
		try {
			if ( s_logger.isDebugEnabled() ) {
				s_logger.debug("check MDTManager health: url={}", m_url);
			}
			m_restfulClient.get(m_url);
			m_retryRemains = m_retryCount;
		}
		catch ( Exception e ) {
			--m_retryRemains;
			if ( s_logger.isInfoEnabled() ) {
				s_logger.info("Failed to connect MDTManager: url={}, retry-remains={}, cause={}",
								m_url, m_retryRemains, Throwables.unwrapThrowable(e));
			}
			if ( m_retryRemains <= 0 ) {
				if ( s_logger.isWarnEnabled() ) {
					s_logger.warn("Shutting-down MDTInstance because MDTManager is down.");
				}
				System.exit(0);
			}
		}
	}

	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedRateSchedule(0, m_pollInterval.toMillis(), TimeUnit.MILLISECONDS);
	}
	
	@Override
	public String toString() {
		return String.format("mdt-url=%s, poll-interval=%s, retry-count=%d", m_url, m_pollInterval, m_retryRemains);
	}
}
