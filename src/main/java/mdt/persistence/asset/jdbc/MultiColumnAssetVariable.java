package mdt.persistence.asset.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;

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
import mdt.persistence.asset.jdbc.MultiColumnAssetVariableConfig.ColumnConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MultiColumnAssetVariable extends AbstractJdbcAssetVariable<MultiColumnAssetVariableConfig> {
	private final LinkedHashMap<String,Column> m_columns = new LinkedHashMap<>();
	
	public MultiColumnAssetVariable(MultiColumnAssetVariableConfig config) {
		super(config);
		
		FStream.from(config.getColumns())
				.map(Column::new)
				.forEach(c -> m_columns.put(c.m_name, c));
	}

	@Override
	public boolean isUpdateable() {
		return false;
	}

	@Override
	public void bind(Submodel submodel) {
		super.bind(submodel);
		
		m_columns.values().forEach(rd -> rd.bind(submodel));
	}

	@Override
	protected void load(Connection conn) throws AssetVariableException {
		try ( Statement stmt = conn.createStatement(); ) {
			ResultSet rs = stmt.executeQuery(m_config.getReadQuery());
			if ( rs.next() ) {
				ResultSetMetaData meta = rs.getMetaData();
				for ( int i =0; i < meta.getColumnCount(); ++i ) {
					String cname = meta.getColumnName(i+1);
					Column col = m_columns.get(cname);
					if ( col != null ) {
						col.update(rs.getObject(i+1));
					}
				}
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

	@Override
	protected void save(Connection conn) throws AssetVariableException {
		try ( PreparedStatement pstmt = conn.prepareStatement(m_config.getUpdateQuery()) ) {
			FStream.from(m_columns.values())
					.zipWithIndex(1)
					.forEachOrThrow(idxed -> {
						Column col = idxed.value();
						Object colValue = col.toJdbcObject();
						pstmt.setObject(idxed.index(), colValue);
					});
			pstmt.execute();
		}
		catch ( SQLException e ) {
			String msg = String.format("Failed to write %s with query: %s", this, m_config.getUpdateQuery());
			throw new AssetVariableException(msg, e);
		}
	}

	public static class Column {
		private String m_name;
		private String m_path;
		private SubmodelElement m_buffer;
		@SuppressWarnings("rawtypes")
		private volatile DataType m_type = null;
		
		public Column(ColumnConfig conf) {
			m_name = conf.getName();
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
		Object toJdbcObject() {
			if ( m_buffer instanceof Property prop ) {
				Object propValue = m_type.parseValueString(prop.getValue());
				return m_type.toJdbcObject(propValue);
			}
			else {
				return MDTModelSerDe.toJsonString(ElementValues.getValue(m_buffer));
			}
		}
		
		@SuppressWarnings("unchecked")
		public void update(Object value) {
			if ( this.m_type != null ) {
				((Property)m_buffer).setValue(this.m_type.toValueString(value));
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
