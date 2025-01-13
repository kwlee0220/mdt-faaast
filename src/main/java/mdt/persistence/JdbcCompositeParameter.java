package mdt.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import utils.func.FOption;
import utils.jdbc.JdbcConfiguration;
import utils.jdbc.JdbcProcessor;
import utils.stream.FStream;

import mdt.MDTGlobalConfigurations;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class JdbcCompositeParameter extends AbstractAssetParameter {
	private JdbcCompositeParameterConfig m_config;
	private final List<JdbcAssetParameter> m_parameters;
	
	private Submodel m_submodel;
	private JdbcProcessor m_jdbc;
	
	public JdbcCompositeParameter(JdbcCompositeParameterConfig config) {
		super(config.getSubmodel(), config.getPath());
		
		m_config = config;
		m_parameters = FStream.from(config.getParameters())
								.map(JdbcAssetParameter::new)
								.toList();
	}

	@Override
	public void initialize(Environment env) {
		super.initialize(env);
		FStream.from(m_parameters).forEach(param -> param.initialize(env));

		try {
			if ( m_config.getJdbcConfigKey() != null ) {
				JdbcConfiguration jdbcConf = MDTGlobalConfigurations.loadJdbcConfiguration(m_config.getJdbcConfigKey());
				m_jdbc = JdbcProcessor.create(jdbcConf);
				FStream.from(m_parameters).forEach(param -> param.setJdbcProcessor(m_jdbc));
			}
		}
		catch ( Exception e ) {
			String msg = String.format("Failed to initialize %s", this);
			throw new MDTPersistenceException(msg, e);
		}
	}
	
	public List<JdbcAssetParameter> findRelevantParameters(String path) {
		return FStream.from(m_parameters)
						.filter(p -> path.startsWith(p.getElementPath()))
						.toList();
	}

	@Override
	public void load(SubmodelElement buffer, String path) throws AssetParameterException {
		if ( !contains(path) && !isContained(path) ) {
			return;
		}
		
		if ( m_jdbc != null ) {
			try ( Connection conn = m_jdbc.connect();
					Statement stmt = conn.createStatement(); ) {
				FStream.from(m_parameters).forEach(param -> param.load(buffer, path, stmt));
			}
			catch ( SQLException e ) {
				String msg = String.format("Failed to read %s", this);
				throw new AssetParameterException(msg, e);
			}
		}
		else {
			FStream.from(m_parameters).forEach(param -> param.load(buffer, path));
		}
	}
	
	public void load(SubmodelElement buffer, String path, Statement stmt) throws AssetParameterException {
		if ( overlaps(path) ) {
			FStream.from(m_parameters)
					.forEachOrThrow(param -> param.load(buffer, path, stmt));
		}
	}

	@Override
	public boolean save(String path, SubmodelElement element) throws AssetParameterException {
		if ( !overlaps(path) ) {
			return false;
		}
		
		try ( Connection conn = m_jdbc.connect(); ) {
			boolean saved = false;
			for ( JdbcAssetParameter param: m_parameters ) {
				saved |= param.save(conn, path, element);
			}
			
			return saved;
		}
		catch ( SQLException e ) {
			String msg = String.format("Failed to read %s", this);
			throw new AssetParameterException(msg, e);
		}
	}
	
	public void save(Connection conn, String path, SubmodelElement element) throws AssetParameterException {
		if ( overlaps(path) ) {
			FStream.from(m_parameters)
					.forEach(param -> param.save(conn, path, element));
		}
	}
	
	@Override
	public String toString() {
		String smId = FOption.mapOrElse(m_submodel, Submodel::getIdShort, "?");
		return String.format("%s/%s, jdbc=%s", smId, getElementPath(), m_jdbc);
	}
}
