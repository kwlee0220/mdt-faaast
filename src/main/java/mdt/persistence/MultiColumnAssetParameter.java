package mdt.persistence;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;

import utils.InternalException;
import utils.Throwables;
import utils.jdbc.JdbcConfiguration;
import utils.jdbc.JdbcProcessor;
import utils.stream.FStream;

import mdt.MDTGlobalConfigurations;
import mdt.aas.DataType;
import mdt.aas.DataTypes;
import mdt.model.MDTModelSerDe;
import mdt.model.ResourceNotFoundException;
import mdt.model.sm.SubmodelUtils;
import mdt.model.sm.value.ElementValues;
import mdt.persistence.MultiColumnAssetParameterConfig.ColumnConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MultiColumnAssetParameter extends AbstractAssetParameter {
	private static String UPDATE_QUERY_FORMAT = "update %s set %s %s";
	
	private final MultiColumnAssetParameterConfig m_config;
	private final LinkedHashMap<String,Column> m_columns = new LinkedHashMap<>();
	private JdbcProcessor m_jdbc;

	public static class Column {
		private String m_name;
		private String m_path;
		@SuppressWarnings("rawtypes")
		private volatile DataType m_type = null;
		
		public Column(ColumnConfig conf) {
			m_name = conf.getName();
			m_path = conf.getPath();
		}
		
		public void initialize(Submodel submodel) {
			SubmodelElement element = SubmodelUtils.traverse(submodel, m_path);
			if ( element instanceof Property prop ) {
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
		public void update(SubmodelElement buffer, Object value) {
			if ( this.m_type != null ) {
				((Property)buffer).setValue(this.m_type.toValueString(value));
			}
			else {
				try {
					updateWithValueJsonString(buffer, (String)value);
				}
				catch ( IOException e ) {
					String msg = String.format("Failed to update %s with value=%s", this, value);
					throw new InternalException(msg, e);
				}
			}
		}

		public void updateWithValueJsonNode(SubmodelElement buffer, JsonNode valueNode) throws IOException {
			ElementValues.update(buffer, valueNode);
		}

		public void updateWithValueJsonString(SubmodelElement buffer, String valueJsonString) throws IOException {
			updateWithValueJsonNode(buffer, MDTModelSerDe.readJsonNode(valueJsonString));
		}
	};
	
	public MultiColumnAssetParameter(MultiColumnAssetParameterConfig config) {
		super(config.getSubmodel(), config.getPath());
		
		m_config = config;
		FStream.from(config.getColumns())
				.map(Column::new)
				.forEach(c -> m_columns.put(c.m_name, c));
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
		
		m_submodel = FStream.from(env.getSubmodels())
							.filter(sm -> getSubmodelIdShort().equals(sm.getIdShort()))
							.findFirst()
							.getOrThrow(() -> new ResourceNotFoundException("Submodel",
																			"idShort=" + getSubmodelIdShort()));
		FStream.from(m_columns.values())
				.forEach(c -> c.initialize(m_submodel));
	}

	@Override
	public void load(SubmodelElement buffer, String path) throws AssetParameterException, ResourceNotFoundException {
		if ( contains(path) || isContained(path) ) {
			assertJdbcProcessor();
			
			try ( Connection conn = m_jdbc.connect();
					Statement stmt = conn.createStatement(); ) {
				ResultSet rs = stmt.executeQuery(m_config.getReadQuery());
				if ( rs.next() ) {
					ResultSetMetaData meta = rs.getMetaData();
					for ( int i =0; i < meta.getColumnCount(); ++i ) {
						String cname = meta.getColumnName(i+1);
						Column col = m_columns.get(cname);
						if ( col != null ) {
							String relPath = SubmodelUtils.toRelativeIdShortPath(path, col.m_path);
							if ( relPath != null ) {
								SubmodelElement sub = SubmodelUtils.traverse(buffer, relPath);
								col.update(sub, rs.getObject(i+1));
							}
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
	}

	@Override
	public boolean save(String path, SubmodelElement element) throws AssetParameterException {
		if ( contains(path) || isContained(path) ) {
			assertJdbcProcessor();
			
			List<Column> relevantColumns
								= FStream.from(m_columns.values())
										.filter(col -> col.m_path.startsWith(path) || path.startsWith(col.m_path))
										.toList();
			if ( relevantColumns.size() == 0 ) {
				return false;
			}
			String setClause = FStream.from(relevantColumns)
										.map(col -> String.format("%s = ?", col.m_name))
										.join(", ");
			String updateSql = String.format(UPDATE_QUERY_FORMAT, m_config.getTable(), setClause,
											m_config.getWhereClause());
			try {
				m_jdbc.executeUpdate(updateSql, pstmt -> {
					FStream.from(relevantColumns)
							.zipWithIndex(1)
							.forEachOrThrow(idxed -> {
								Column col = idxed.value();
								String relPath = SubmodelUtils.toRelativeIdShortPath(path, col.m_path);
								if ( relPath != null ) {
									SubmodelElement sub = SubmodelUtils.traverse(element, relPath);
									Object jdbcValue = col.toJdbcObject(sub);
									pstmt.setObject(idxed.index(), jdbcValue);
								}
							});
					pstmt.execute();
				});
				
				return true;
			}
			catch ( ExecutionException e ) {
				throw new AssetParameterException("Failed to update Element: path=" + path,
													Throwables.unwrapThrowable(e));
			}
			catch ( SQLException e ) {
				String msg = String.format("Failed to write %s with query: %s", this, updateSql);
				throw new AssetParameterException(msg, e);
			}
			
		}
		return false;
	}

	private void assertJdbcProcessor() {
		Preconditions.checkState(m_jdbc != null, "JdbcProcess has not been set");
	}
}
