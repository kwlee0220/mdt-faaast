package mdt.endpoint.audit;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.async.PeriodicLoopExecution;
import utils.func.FOption;
import utils.jdbc.JdbcConfiguration;
import utils.jdbc.JdbcProcessor;
import utils.jdbc.JdbcUtils;
import utils.stream.FStream;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.config.CoreConfig;
import de.fraunhofer.iosb.ilt.faaast.service.endpoint.Endpoint;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationInitializationException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.EndpointException;
import mdt.ElementColumnConfig;
import mdt.ElementHandle;
import mdt.FaaastRuntime;
import mdt.MDTGlobalConfigurations;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class PeriodicAssetVariableLogger implements Endpoint<PeriodicAssetVariableLoggerConfig> {
	private static final Logger s_logger = LoggerFactory.getLogger(PeriodicAssetVariableLogger.class);

	private JdbcProcessor m_jdbc;
	private FaaastRuntime m_faaast;
	private PeriodicAssetVariableLoggerConfig m_config;
	private List<ElementColumnConfig> m_columns;
	private List<ElementHandle> m_elementHandles;
	private PeriodicLoopExecution<Void> m_periodicAudit;
	private List<Object> m_lastValues;
	private String m_insertSql;

	@Override
	public void init(CoreConfig coreConfig, PeriodicAssetVariableLoggerConfig config, ServiceContext serviceContext)
		throws ConfigurationInitializationException {
		m_config = config;
		if ( !m_config.isEnabled() ) {
			return;
		}
		
		m_faaast = new FaaastRuntime(serviceContext);
		m_columns = config.getColumns();
		
		String colCsv = FStream.from(m_columns).map(ElementColumnConfig::getName).join(',');
		String paramCsv = FStream.range(0, m_columns.size()).map(idx -> "?").join(',');
		m_lastValues = FStream.from(m_columns).map(c -> new Object()).toList();
		m_insertSql = String.format("insert into %s(ts,%s) values (?, %s)", config.getTable(), colCsv, paramCsv);
		
		try {
			JdbcConfiguration jdbcConfig = MDTGlobalConfigurations.loadJdbcConfiguration(m_config.getJdbcConfigKey());
			m_jdbc = JdbcProcessor.create(jdbcConfig);
		}
		catch ( IOException e ) {
			throw new ConfigurationInitializationException("Failed to find JdbcConfiguration, cause=" + e);
		}
	}

	@Override
	public PeriodicAssetVariableLoggerConfig asConfig() {
		return m_config;
	}
	
    @Override
    public void start() throws EndpointException {
    	if ( !m_config.isEnabled() ) {
    		return;
    	}
    	
    	m_periodicAudit = new PeriodicLoopExecution<Void>(m_config.getInterval()) {
			@Override
			protected FOption<Void> performPeriodicAction(long loopIndex) throws Exception {
				audit(loopIndex);
				return FOption.empty();
			}
		};
		m_periodicAudit.setLogger(s_logger);
		
		m_elementHandles = FStream.from(m_columns)
									.map(cf -> {
										ElementHandle handle = new ElementHandle(cf.getPath());
										Submodel submodel = m_faaast.getSubmodelByIdShort(cf.getSubmodel());
										handle.initialize(submodel);
										return handle;
									})
									.toList();
		
		if ( s_logger.isInfoEnabled() ) {
			s_logger.info("Starting {}, config={}", getClass().getSimpleName(), m_config);
		}
		m_periodicAudit.start();
    }

    @Override
    public void stop() {
    	if ( !m_config.isEnabled() ) {
    		return;
    	}
    	m_periodicAudit.cancel(true);
		if ( s_logger.isInfoEnabled() ) {
			s_logger.info("Stopped {}, config={}", getClass().getSimpleName(), m_config);
		}
    }

	private void audit(long loopIndex) throws Exception {
		List<Object> values
					= FStream.from(m_elementHandles)
							.map(handle -> {
								SubmodelElement element = m_faaast.getSubmodelElementByPath(handle.getSubmodelId(),
																							handle.getElementPath());
								return handle.toJdbcObject(element);
							})
							.toList();
		if ( m_lastValues.equals(values) ) {
			return;
		}
		
		m_jdbc.executeUpdate(m_insertSql, pstmt -> {
			FStream.from(values)
					.zipWithIndex(2)
					.forEachOrThrow(idxed -> pstmt.setObject(idxed.index(), idxed.value()));
			pstmt.setTimestamp(1, JdbcUtils.toTimestamp(Instant.now()));
			pstmt.execute();
			
			m_lastValues = values;
		});
	}
	
	@Override
	public String toString() {
		return "Audit: " + m_config;
	}
}
