package mdt.persistence.asset.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import com.fasterxml.jackson.databind.JsonNode;

import utils.InternalException;
import utils.stream.FStream;

import mdt.aas.DataType;
import mdt.aas.DataTypes;
import mdt.model.MDTModelSerDe;
import mdt.model.ResourceNotFoundException;
import mdt.model.sm.SubmodelUtils;
import mdt.model.sm.value.ElementValues;
import mdt.persistence.asset.AssetVariableException;
import mdt.persistence.asset.jdbc.MultiRowAssetVariableConfig.RowDefConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MultiRowAssetVariable extends AbstractJdbcAssetVariable<MultiRowAssetVariableConfig> {
	private final Map<String,RowDef> m_rowDefs;
	
	public MultiRowAssetVariable(MultiRowAssetVariableConfig config) {
		super(config);
		
		m_rowDefs = FStream.from(config.getRowDefs())
							.map(RowDef::new)
							.toMap(rd -> rd.m_key);
	}

	@Override
	public boolean isUpdateable() {
        return m_config.getUpdateQuery() != null;
    }

	@Override
	public void bind(Submodel submodel) {
		super.bind(submodel);
		
		m_rowDefs.values().forEach(rd -> rd.bind(submodel));
	}

	@Override
	protected void load(Connection conn) throws AssetVariableException, ResourceNotFoundException {
		try ( Statement stmt = conn.createStatement(); ) {
			ResultSet rs = stmt.executeQuery(m_config.getReadQuery());
			while ( rs.next() ) {
				String key = rs.getString(1);
				Object cvalue = rs.getObject(2);
				
				RowDef row = m_rowDefs.get(key);
				if ( row != null ) {
					row.update(cvalue);
				}
				else {
					if ( getLogger().isDebugEnabled() ) {
						getLogger().debug("Skip Row: key={}", key);
					}
				}
			}
		}
		catch ( SQLException e ) {
			String msg = String.format("Failed to read %s", this);
			throw new AssetVariableException(msg, e);
		}
	}

	@Override
	protected void save(Connection conn) throws AssetVariableException {
		try ( PreparedStatement pstmt = conn.prepareStatement(m_config.getUpdateQuery()); ) {
			for ( RowDef rowDef: m_rowDefs.values() ) {
				pstmt.setObject(1, rowDef.toJdbcObject(rowDef.m_buffer));
				pstmt.setString(2, rowDef.m_key);
				pstmt.executeUpdate();
			}
		}
		catch ( SQLException e ) {
			String msg = String.format("Failed to save %s: path=%s", this, getElementPath());
			throw new AssetVariableException(msg, e);
		}
	}

	private static class RowDef {
		private final String m_key;
		private final String m_path;
		private SubmodelElement m_buffer;
		@SuppressWarnings("rawtypes")
		private DataType m_type = null;
		
		public RowDef(RowDefConfig conf) {
			m_key = conf.getKey();
			m_path = conf.getPath();
		}
		
		public void bind(Submodel submodel) {
			m_buffer = SubmodelUtils.traverse(submodel, m_path);
			if ( m_buffer instanceof Property prop ) {
				m_type = DataTypes.fromAas4jDatatype(prop.getValueType());
			}
			else {
				m_type = null;
			}
		}
		
		@SuppressWarnings("unchecked")
		Object toJdbcObject(SubmodelElement element) {
			if ( element instanceof Property prop ) {
				Object propValue = m_type.parseValueString(prop.getValue());
				return m_type.toJdbcObject(propValue);
			}
			else {
				return MDTModelSerDe.toJsonString(ElementValues.getValue(element));
			}
		}
		
		@SuppressWarnings("unchecked")
		public void update(Object value) {
			if ( m_type != null ) {
				((Property)m_buffer).setValue(m_type.toValueString(value));
			}
			else {
				try {
					updateWithValueJsonString((String)value);
				}
				catch ( IOException e ) {
					String msg = String.format("Failed to update %s with value=%s", this, value);
					throw new InternalException(msg, e);
				}
			}
		}

		public void updateWithValueJsonNode(JsonNode valueNode) throws IOException {
			ElementValues.update(m_buffer, valueNode);
		}

		public void updateWithValueJsonString(String valueJsonString) throws IOException {
			updateWithValueJsonNode(MDTModelSerDe.readJsonNode(valueJsonString));
		}
	}
}
