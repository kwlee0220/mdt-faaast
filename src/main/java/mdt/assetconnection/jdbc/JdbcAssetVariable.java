package mdt.assetconnection.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;

import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import com.google.common.base.Preconditions;

import utils.InternalException;
import utils.Throwables;
import utils.jdbc.JdbcProcessor;

import mdt.assetconnection.AbstractAssetVariable;
import mdt.assetconnection.AssetVariable;
import mdt.assetconnection.AssetVariableException;
import mdt.model.MDTModelSerDe;
import mdt.model.ResourceNotFoundException;
import mdt.model.sm.value.ElementValues;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class JdbcAssetVariable extends AbstractAssetVariable implements AssetVariable {
	private JdbcProcessor m_jdbc;
	private final String m_readQuery;
	private final String m_writeQuery;
	
	public JdbcAssetVariable(String submodelIdShort, String path, String readQuery, String writeQuery) {
		super(submodelIdShort, path);
		
		m_readQuery = readQuery;
		m_writeQuery = writeQuery;
	}
	
	public JdbcProcessor getJdbcProcessor() {
		return m_jdbc;
	}
	
	public void setJdbcProcessor(JdbcProcessor jdbc) {
		m_jdbc = jdbc;
	}

	@Override
	public SubmodelElement read() throws AssetVariableException {
		assertJdbcProcessor();
		
		try ( Connection conn = m_jdbc.connect();
				Statement stmt = conn.createStatement(); ) {
			return read(stmt);
		}
		catch ( SQLException e ) {
			String msg = String.format("Failed to read AssetVariable[%s]", this);
			throw new AssetVariableException(msg, e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public SubmodelElement read(Statement stmt) throws SQLException {
		ResultSet rs = stmt.executeQuery(m_readQuery);
		if ( rs.next() ) {
			String value = rs.getString(1);
			if ( getDataType() != null ) {
				((Property)getElementBuffer()).setValue(getDataType().toValueString(value));
			}
			else {
				try {
					ElementValues.updateWithExternalString(getElementBuffer(), value);
				}
				catch ( IOException e ) {
					String msg = String.format("Failed to update %s with value=%s", this, value);
					throw new InternalException(msg, e);
				}
			}
			return getElementBuffer();
		}
		else {
			throw new ResourceNotFoundException("JdbcAssetVariable", m_readQuery);
		}
	}
	

	@Override
	public void write() throws AssetVariableException {
		Object jdbcValue = toJdbcObject();
		try {
			m_jdbc.executeUpdate(m_writeQuery, pstmt -> {
				pstmt.setObject(1, jdbcValue);
				pstmt.execute();
			});
		}
		catch ( ExecutionException e ) {
			throw new InternalException("Failed to update Element: value=" + jdbcValue,
											Throwables.unwrapThrowable(e));
		}
		catch ( SQLException e ) {
			String msg = String.format("Failed to write %s with query: %s", this, m_writeQuery);
			throw new AssetVariableException(msg, e);
		}
	}
	
	public void write(Connection conn) throws AssetVariableException {
		try ( PreparedStatement pstmt = conn.prepareStatement(m_writeQuery) ) {
			pstmt.setObject(1, pstmt);
			pstmt.execute();
		}
		catch ( SQLException e ) {
			String msg = String.format("Failed to write %s with query: %s", this, m_writeQuery);
			throw new AssetVariableException(msg, e);
		}
	}
	
	private void assertJdbcProcessor() {
		Preconditions.checkState(m_jdbc != null, "JdbcProcess has not been set");
	}
	
	@SuppressWarnings("unchecked")
	private Object toJdbcObject() {
		if ( getElementBuffer() instanceof Property prop ) {
			Object propValue = getDataType().parseValueString(prop.getValue());
			return getDataType().toJdbcObject(propValue);
		}
		else {
			return MDTModelSerDe.toJsonString(ElementValues.getValue(getElementBuffer()));
		}
	}
}
