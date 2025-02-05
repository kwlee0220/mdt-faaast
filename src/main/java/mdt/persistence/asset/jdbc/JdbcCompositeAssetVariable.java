package mdt.persistence.asset.jdbc;

import java.sql.Connection;
import java.time.Instant;
import java.util.List;

import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;

import utils.stream.FStream;

import mdt.persistence.asset.AssetVariable;
import mdt.persistence.asset.AssetVariableException;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class JdbcCompositeAssetVariable extends AbstractJdbcAssetVariable<JdbcCompositeAssetVariableConfig> {
	private final List<JdbcAssetVariable> m_components;
	
	private final boolean m_isUpdateable;
	
	public JdbcCompositeAssetVariable(JdbcCompositeAssetVariableConfig config) {
		super(config);
		
		m_components = FStream.from(config.getComponents())
								.map(JdbcAssetVariable::new)
								.toList();
		
		// 구성 컴포넌트 AsssetVariable 중 하나라도 updateable이면 updateable로 설정됨.
		m_isUpdateable = FStream.from(m_components)
						        .findFirst(AssetVariable::isUpdateable)
						        .isPresent();
	}

	@Override
	public boolean isUpdateable() {
		return m_isUpdateable;
	}

	@Override
	public boolean isExpired(Instant ts) {
		return FStream.from(m_components)
				        .findFirst(var -> var.isExpired(ts))
				        .isPresent();
	}
	
	@Override
	public void bind(Submodel submodel) {
		m_components.forEach(var -> var.bind(submodel));
		super.bind(submodel);
	}

	@Override
	public void load(Connection conn) throws AssetVariableException {
		m_components.forEach(var -> var.load(conn));
	}

	@Override
	public void save(Connection conn) throws AssetVariableException {
		m_components.forEach(var -> var.save(conn));
	}
}
