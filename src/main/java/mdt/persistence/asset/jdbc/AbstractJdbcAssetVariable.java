package mdt.persistence.asset.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import utils.jdbc.JdbcConfiguration;
import utils.jdbc.JdbcProcessor;

import mdt.MDTGlobalConfigurations;
import mdt.model.MDTModelSerDe;
import mdt.model.sm.value.ElementValues;
import mdt.persistence.MDTPersistenceException;
import mdt.persistence.asset.AbstractAssetVariable;
import mdt.persistence.asset.AssetVariable;
import mdt.persistence.asset.AssetVariableException;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public abstract class AbstractJdbcAssetVariable<T extends JdbcAssetVariableConfigBase>
																				extends AbstractAssetVariable<T>
																				implements AssetVariable {
	private final JdbcProcessor m_jdbc;

	abstract protected void load(Connection conn) throws AssetVariableException;
	abstract protected void save(Connection conn) throws AssetVariableException;
	
	public AbstractJdbcAssetVariable(T config) {
		super(config);
		setLogger(LoggerFactory.getLogger(getClass()));
		
		if ( config.getJdbcConfigKey() != null ) {
			try {
				JdbcConfiguration jdbcConf = MDTGlobalConfigurations.loadJdbcConfiguration(config.getJdbcConfigKey());
				m_jdbc = JdbcProcessor.create(jdbcConf);
			}
			catch ( Exception e ) {
				String msg = String.format("Failed to initialize %s", this);
				throw new MDTPersistenceException(msg, e);
			}
		}
		else {
			m_jdbc = null;
		}
	}

	@Override
	public SubmodelElement load() {
		assertJdbcProcessor();
		
		if ( isExpired(Instant.now()) ) {
			try ( Connection conn = m_jdbc.connect(); ) {
				load(conn);
				setLastUpdatedTime(Instant.now());
			}
			catch ( SQLException e ) {
				String msg = String.format("Failed to read %s", this);
				throw new AssetVariableException(msg, e);
			}
		}
		return m_buffer;
	}

	@Override
	public void save() throws AssetVariableException {
		if ( !isUpdateable() ) {
			return;
		}
		
		assertJdbcProcessor();
		try ( Connection conn = m_jdbc.connect() ) {
			save(conn);
		}
		catch ( SQLException e ) {
			String msg = String.format("Failed to save %s", this);
			throw new AssetVariableException(msg, e);
		}
	}
	
	
	@Override
	public String toString() {
		return String.format("%s[%s/%s, jdbc=%s]",
							getClass().getSimpleName(), getSubmodelIdShort(), getElementPath(),
							m_config.getJdbcConfigKey());
	}
	
	private void assertJdbcProcessor() {
		Preconditions.checkState(m_jdbc != null, "JdbcProcess has not been set");
	}
	
	@SuppressWarnings("unchecked")
	protected Object toJdbcObject(SubmodelElement element) {
		if ( element instanceof Property prop ) {
			Object propValue = getDataType().parseValueString(prop.getValue());
			return getDataType().toJdbcObject(propValue);
		}
		else {
			return MDTModelSerDe.toJsonString(ElementValues.getValue(element));
		}
	}
}
