package mdt.assetconnection.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import utils.jdbc.JdbcProcessor;
import utils.stream.FStream;

import mdt.assetconnection.AbstractAssetVariable;
import mdt.assetconnection.AssetVariable;
import mdt.assetconnection.AssetVariableException;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class JdbcAssetVariableList extends AbstractAssetVariable implements AssetVariable {
	private JdbcProcessor m_jdbc;
	private List<JdbcAssetVariable> m_components;
	
	public JdbcAssetVariableList(String submodel, String path) {
		super(submodel, path);
	}
	
	public JdbcProcessor getJdbcProcessor() {
		return m_jdbc;
	}
	
	public void setJdbcProcessor(JdbcProcessor jdbc) {
		m_jdbc = jdbc;
		FStream.from(m_components).forEach(var -> var.setJdbcProcessor(jdbc));
	}

	@Override
	public SubmodelElement read() throws AssetVariableException {
		try ( Connection conn = m_jdbc.connect();
				Statement stmt = conn.createStatement(); ) {
			FStream.from(m_components)
					.forEachOrThrow(var -> var.read(stmt));
			
			return getElementBuffer();
		}
		catch ( SQLException e ) {
			throw new AssetVariableException("Failed to read AssetVariable[%s]", e);
		}
	}

	@Override
	public void write() throws AssetVariableException {
		try ( Connection conn = m_jdbc.connect(); ) {
			FStream.from(m_components)
					.forEachOrThrow(var -> var.write(conn));
		}
		catch ( SQLException e ) {
			throw new AssetVariableException("Failed to read AssetVariable[%s]", e);
		}
	}
}
