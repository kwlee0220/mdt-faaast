package mdt.persistence;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;

import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import com.google.common.base.Preconditions;

import utils.InternalException;
import utils.Throwables;
import utils.jdbc.JdbcConfiguration;
import utils.jdbc.JdbcProcessor;

import mdt.MDTGlobalConfigurations;
import mdt.model.MDTModelSerDe;
import mdt.model.ResourceNotFoundException;
import mdt.model.sm.SubmodelUtils;
import mdt.model.sm.value.ElementValues;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class JdbcAssetParameter extends AbstractAssetParameter implements AssetParameter {
	private final JdbcAssetParameterConfig m_config;
	private JdbcProcessor m_jdbc;
	
	public JdbcAssetParameter(JdbcAssetParameterConfig config) {
		super(config.getSubmodel(), config.getPath());
		
		m_config = config;
	}

	@Override
	public void initialize(Environment env) {
		super.initialize(env);

		if ( m_config.getJdbcConfigKey() != null ) {
			try {
				JdbcConfiguration jdbcConf = MDTGlobalConfigurations.loadJdbcConfiguration(m_config.getJdbcConfigKey());
				m_jdbc = JdbcProcessor.create(jdbcConf);
			}
			catch ( Exception e ) {
				String msg = String.format("Failed to initialize %s", this);
				throw new MDTPersistenceException(msg, e);
			}
		}
	}
	
	public void setJdbcProcessor(JdbcProcessor jdbc) {
		m_jdbc = jdbc;
	}

	@Override
	public void load(SubmodelElement buffer, String path) throws AssetParameterException, ResourceNotFoundException {
		if ( contains(path) || isContained(path) ) {
			assertJdbcProcessor();
			
			String relativePath = SubmodelUtils.toRelativeIdShortPath(path, getElementPath());
			buffer = SubmodelUtils.traverse(buffer, relativePath);
			
			try ( Connection conn = m_jdbc.connect();
					Statement stmt = conn.createStatement(); ) {
				_load(stmt, buffer);
			}
			catch ( SQLException e ) {
				String msg = String.format("Failed to read %s", this);
				throw new AssetParameterException(msg, e);
			}
		}
	}
	
	public void load(SubmodelElement buffer, String path, Statement stmt) throws AssetParameterException,
																					ResourceNotFoundException {
		if ( contains(path) || isContained(path) ) {
			_load(stmt, buffer);
		}
	}
	
	private void _load(Statement stmt, SubmodelElement buffer) throws AssetParameterException,
																					ResourceNotFoundException {
		try {
			ResultSet rs = stmt.executeQuery(m_config.getReadQuery());
			if ( rs.next() ) {
				String value = rs.getString(1);
				if ( getDataType() != null ) {
					((Property)buffer).setValue(getDataType().toValueString(value));
				}
				else {
					try {
						updateWithValueJsonString(buffer, value);
					}
					catch ( IOException e ) {
						String msg = String.format("Failed to update %s with value=%s", this, value);
						throw new InternalException(msg, e);
					}
				}
			}
			else {
				throw new ResourceNotFoundException("JdbcAssetVariable", m_config.getReadQuery());
			}
		}
		catch ( SQLException e ) {
			String msg = String.format("Failed to read %s", this);
			throw new AssetParameterException(msg, e);
		}
	}

	@Override
	public boolean save(String path, SubmodelElement element) throws AssetParameterException,
																		ResourceNotFoundException {
		if ( isContained(path) ) {
			// 주어진 SubmodelElement 영역과 본 parameter 영역의 교집합을 구한다.
			String relativePath = SubmodelUtils.toRelativeIdShortPath(path, getElementPath());
			element = SubmodelUtils.traverse(element, relativePath);
		}
		else if ( contains(path) ) { }
		else {
			return false;
		}

		assertJdbcProcessor();
		Object jdbcValue = toJdbcObject(element);
		try {
			m_jdbc.executeUpdate(m_config.getUpdateQuery(), pstmt -> {
				pstmt.setObject(1, jdbcValue);
				pstmt.execute();
			});
			
			return true;
		}
		catch ( ExecutionException e ) {
			throw new AssetParameterException("Failed to update Element: value=" + jdbcValue,
												Throwables.unwrapThrowable(e));
		}
		catch ( SQLException e ) {
			String msg = String.format("Failed to write %s with query: %s", this, m_config.getUpdateQuery());
			throw new AssetParameterException(msg, e);
		}
	}
	
	public boolean save(Connection conn, String path, SubmodelElement element) throws AssetParameterException,
																						ResourceNotFoundException {
		if ( isContained(path) ) {
			// 주어진 SubmodelElement 영역과 본 parameter 영역의 교집합을 구한다.
			String relativePath = SubmodelUtils.toRelativeIdShortPath(path, getElementPath());
			element = SubmodelUtils.traverse(element, relativePath);
		}
		else if ( contains(path) ) { }
		else {
			return false;
		}
		
		Object jdbcValue = toJdbcObject(element);
		try ( PreparedStatement pstmt = conn.prepareStatement(m_config.getUpdateQuery()) ) {
			pstmt.setObject(1, jdbcValue);
			pstmt.execute();
			
			return true;
		}
		catch ( SQLException e ) {
			String msg = String.format("Failed to write %s with query: %s", this, m_config.getUpdateQuery());
			throw new AssetParameterException(msg, e);
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
	private Object toJdbcObject(SubmodelElement element) {
		if ( element instanceof Property prop ) {
			Object propValue = getDataType().parseValueString(prop.getValue());
			return getDataType().toJdbcObject(propValue);
		}
		else {
			return MDTModelSerDe.toJsonString(ElementValues.getValue(element));
		}
	}
}
