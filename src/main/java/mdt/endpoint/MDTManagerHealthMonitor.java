package mdt.endpoint;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.util.concurrent.AbstractScheduledService;

import utils.http.HttpRESTfulClient;
import utils.http.JacksonErrorEntityDeserializer;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.config.CoreConfig;
import de.fraunhofer.iosb.ilt.faaast.service.endpoint.Endpoint;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationInitializationException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.EndpointException;

/**
 * MDTManager의 health를 주기적으로 모니터링하는 서비스.
 * <p>
 * 만일 MDTManager와의 연결이 끊기면, MDTInstance를 종료한다.
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MDTManagerHealthMonitor extends AbstractScheduledService
										implements Endpoint<MDTManagerHealthMonitorConfig> {
	private static final Logger s_logger = LoggerFactory.getLogger(MDTManagerHealthMonitor.class);

	private MDTManagerHealthMonitorConfig m_config;
	private String m_url;
	private HttpRESTfulClient m_restfulClient;

	@Override
	public void init(CoreConfig coreConfig, MDTManagerHealthMonitorConfig config, ServiceContext serviceContext)
		throws ConfigurationInitializationException {
		m_config = config;
		
		m_url = String.format("%s/health", config.getMdtEndpoint());
		JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
		m_restfulClient = HttpRESTfulClient.builder()
											.errorEntityDeserializer(new JacksonErrorEntityDeserializer(mapper))
											.build();
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
		}
		catch ( Exception e ) {
			if ( s_logger.isInfoEnabled() ) {
				s_logger.info("Failed to connect MDTManager: {} -> shutting-down MDTInstance", this);
			}
			System.exit(0);
		}
	}

	@Override
	protected Scheduler scheduler() {
		long intervalMillis = m_config.getCheckInterval().toMillis();
		return Scheduler.newFixedRateSchedule(0, intervalMillis, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public String toString() {
		return String.format("mdt-url=%s, checkInterval=%s", m_url, m_config.getCheckInterval());
	}
}
