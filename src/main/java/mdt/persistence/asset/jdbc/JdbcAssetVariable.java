package mdt.persistence.asset.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

import org.eclipse.digitaltwin.aas4j.v3.model.Property;

import com.google.common.base.Preconditions;

import utils.InternalException;

import mdt.model.ResourceNotFoundException;
import mdt.persistence.asset.AssetVariableException;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class JdbcAssetVariable extends AbstractJdbcAssetVariable<JdbcAssetVariableConfig> {
	public JdbcAssetVariable(JdbcAssetVariableConfig config) {
		super(config);
	}

	@Override
	public boolean isUpdateable() {
        return m_config.getUpdateQuery() != null;
    }

	@SuppressWarnings("unchecked")
	public void load(Connection conn) throws AssetVariableException {
		Preconditions.checkArgument(conn != null, "Connection is null");
		
		try ( Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(m_config.getReadQuery())) {
			if ( rs.next() ) {
				String value = rs.getString(1);
				if ( getDataType() != null ) {
					((Property)m_buffer).setValue(getDataType().toValueString(value));
				}
				else {
					try {
						updateWithValueJsonString(m_buffer, value);
					}
					catch ( IOException e ) {
						String msg = String.format("Failed to update %s with value=%s", this, value);
						throw new InternalException(msg, e);
					}
				}
				setLastUpdatedTime(Instant.now());
			}
			else {
				throw new ResourceNotFoundException("JdbcAssetVariable", m_config.getReadQuery());
			}
		}
		catch ( SQLException e ) {
			String msg = String.format("Failed to read %s", this);
			throw new AssetVariableException(msg, e);
		}
	}
	
	public void save(Connection conn) throws AssetVariableException {
		Object jdbcValue = toJdbcObject(m_buffer);
		try ( PreparedStatement pstmt = conn.prepareStatement(m_config.getUpdateQuery()) ) {
			pstmt.setObject(1, jdbcValue);
			pstmt.execute();
		}
		catch ( SQLException e ) {
			String msg = String.format("Failed to write %s with query: %s", this, m_config.getUpdateQuery());
			throw new AssetVariableException(msg, e);
		}
	}
}
