package mdt.endpoint.audit;

import java.io.IOException;
import java.util.List;

import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.async.PeriodicLoopExecution;
import utils.func.FOption;
import utils.jdbc.JdbcConfiguration;
import utils.jdbc.JdbcProcessor;
import utils.stream.FStream;

import mdt.ElementColumnConfig;
import mdt.ElementHandle;
import mdt.FaaastRuntime;
import mdt.MDTGlobalConfigurations;
import mdt.model.MDTModelSerDe;
import mdt.model.sm.value.ElementValues;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.config.CoreConfig;
import de.fraunhofer.iosb.ilt.faaast.service.endpoint.Endpoint;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationInitializationException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.EndpointException;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class PeriodicAssetVariableLogger implements Endpoint<PeriodicAssetVariableLoggerConfig> {
	private static final Logger s_logger = LoggerFactory.getLogger(PeriodicAssetVariableLogger.class);

	private JdbcProcessor m_jdbc;
	private PeriodicAssetVariableLoggerConfig m_config;
	private List<ElementColumnConfig> m_columns;
	private List<ElementHandle> m_elementHandles;
	private PeriodicLoopExecution<Void> m_periodicAudit;
	private String m_insertSql;

	@Override
	public void init(CoreConfig coreConfig, PeriodicAssetVariableLoggerConfig config, ServiceContext serviceContext)
		throws ConfigurationInitializationException {
		FaaastRuntime rt = new FaaastRuntime(serviceContext);
		
		m_config = config;
		m_columns = config.getColumns();
		
		m_elementHandles = FStream.from(m_columns)
									.map(cf -> new ElementHandle(cf.getSubmodel(), cf.getPath()))
									.peek(handle -> handle.init(rt))
									.toList();
		
		String colCsv = FStream.from(m_columns).map(ElementColumnConfig::getName).join(',');
		String paramCsv = FStream.range(0, m_columns.size()).map(idx -> "?").join(',');
		m_insertSql = String.format("insert into %s(ts,%s) values (CURRENT_TIMESTAMP, %s)",
									config.getTable(), colCsv, paramCsv);
		
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
		m_jdbc.executeUpdate(m_insertSql, pstmt -> {
			for ( int i =0; i < m_elementHandles.size(); ++i ) {
				ElementHandle handle = m_elementHandles.get(i);
				Object value = toJdbcObject(handle);
				pstmt.setObject(i+1, value);
			}
			pstmt.execute();
		});
	}
	
	@Override
	public String toString() {
		return "Audit: " + m_config;
	}
	
	@SuppressWarnings("unchecked")
	private Object toJdbcObject(ElementHandle handle) {
		SubmodelElement element = handle.getElement();
		if ( element instanceof Property prop ) {
			Object propValue = handle.getDataType().parseValueString(prop.getValue());
			return handle.getDataType().toJdbcObject(propValue);
		}
		else {
			return MDTModelSerDe.toJsonString(ElementValues.getValue(element));
		}
	}
}
