package mdt.persistence;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;

import utils.InternalException;
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
import mdt.persistence.MultiRowAssetParameterConfig.RowDefConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MultiRowAssetParameter extends AbstractAssetParameter {
	private final MultiRowAssetParameterConfig m_config;
	private final Map<String,RowDef> m_rowDefs;
	private JdbcProcessor m_jdbc;

	private static class RowDef {
		private String key;
		private String path;
		@SuppressWarnings("rawtypes")
		private volatile DataType type = null;
		
		public RowDef(RowDefConfig conf) {
			this.key = conf.getKey();
			this.path = conf.getPath();
		}
		
		public boolean overlaps(String queryPath) {
			return this.path.startsWith(queryPath) || queryPath.startsWith(this.path);
		}
		
		public void initialize(Submodel submodel) {
			SubmodelElement element = SubmodelUtils.traverse(submodel, this.path);
			if ( element instanceof Property prop ) {
				type = DataTypes.fromAas4jDatatype(prop.getValueType());
			}
			else {
				type = null;
			}
		}
		
		@SuppressWarnings("unchecked")
		Object toJdbcObject(SubmodelElement element) {
			if ( element instanceof Property prop ) {
				Object propValue = this.type.parseValueString(prop.getValue());
				return this.type.toJdbcObject(propValue);
			}
			else {
				return MDTModelSerDe.toJsonString(ElementValues.getValue(element));
			}
		}
		
		@SuppressWarnings("unchecked")
		public void update(SubmodelElement buffer, Object value) {
			if ( this.type != null ) {
				((Property)buffer).setValue(this.type.toValueString(value));
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
	
	public MultiRowAssetParameter(MultiRowAssetParameterConfig config) {
		super(config.getSubmodel(), config.getPath());
		
		m_config = config;
		m_rowDefs = FStream.from(config.getRows())
							.map(RowDef::new)
							.toMap(rd -> rd.key);
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
		FStream.from(m_rowDefs.values())
				.forEach(c -> c.initialize(m_submodel));
	}

	@Override
	public void load(SubmodelElement buffer, String path) throws AssetParameterException, ResourceNotFoundException {
		if ( contains(path) || isContained(path) ) {
			assertJdbcProcessor();
			
			try ( Connection conn = m_jdbc.connect();
					Statement stmt = conn.createStatement(); ) {
				ResultSet rs = stmt.executeQuery(m_config.getReadQuery());
				while ( rs.next() ) {
					String key = rs.getString(1);
					Object cvalue = rs.getObject(2);
					
					RowDef row = m_rowDefs.get(key);
					if ( row != null ) {
						String relPath = SubmodelUtils.toRelativeIdShortPath(path, row.path);
						if ( relPath != null ) {
							SubmodelElement sub = SubmodelUtils.traverse(buffer, relPath);
							row.update(sub, cvalue);
						}
					}
					else {
						if ( getLogger().isDebugEnabled() ) {
							getLogger().debug("Skip Row: key={}, path={}", key, path);
						}
					}
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
		if ( overlaps(path) ) {
			assertJdbcProcessor();
				
			List<RowDef> overlaps = findOverlappingRowDefs(path);
			try ( Connection conn = m_jdbc.connect();
					PreparedStatement pstmt = conn.prepareStatement(m_config.getUpdateQuery()); ) {
				for ( RowDef rowDef: overlaps ) {
					String relativePath = SubmodelUtils.toRelativeIdShortPath(path, rowDef.path);
					SubmodelElement buffer = SubmodelUtils.traverse(element, relativePath);
					pstmt.setObject(1, rowDef.toJdbcObject(buffer));
					pstmt.setString(2, rowDef.key);
					pstmt.executeUpdate();
				}
				
				return true;
			}
			catch ( SQLException e ) {
				String msg = String.format("Failed to save %s: path=%s", this, path);
				throw new AssetParameterException(msg, e);
			}
		}
		else {
			return false;
		}
	}

	private void assertJdbcProcessor() {
		Preconditions.checkState(m_jdbc != null, "JdbcProcess has not been set");
	}
	
	private List<RowDef> findOverlappingRowDefs(String path) {
		return FStream.from(m_rowDefs.values())
						.filter(rd -> rd.overlaps(path))
						.toList();
	}
}
