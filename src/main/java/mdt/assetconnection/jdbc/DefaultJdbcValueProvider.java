package mdt.assetconnection.jdbc;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import utils.jdbc.JdbcProcessor;

import mdt.FaaastRuntime;
import mdt.model.MDTModelSerDe;
import mdt.model.MDTSubstitutor;
import mdt.model.ReferenceUtils;
import mdt.model.ResourceNotFoundException;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionException;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetValueProvider;
import de.fraunhofer.iosb.ilt.faaast.service.model.IdShortPath;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ValueMappingException;
import de.fraunhofer.iosb.ilt.faaast.service.model.value.DataElementValue;
import de.fraunhofer.iosb.ilt.faaast.service.model.value.mapper.ElementValueMapper;
import de.fraunhofer.iosb.ilt.faaast.service.util.ReferenceHelper;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class DefaultJdbcValueProvider implements AssetValueProvider {
	private final DefaultJdbcValueProviderConfig m_config;
	private final JdbcAssetVariable m_variable;
	private Instant m_lastAccessTime;

	public DefaultJdbcValueProvider(ServiceContext serviceContext, Reference reference,
									DefaultJdbcValueProviderConfig config, JdbcProcessor jdbc)
		throws ResourceNotFoundException, IOException {
		m_config = config;
		
		ReferenceUtils.assertSubmodelElementReference(reference);
		String submodelId = ReferenceHelper.getRoot(reference).getValue();
		String path = IdShortPath.fromReference(reference).toString();
		
		String jsonStr = MDTModelSerDe.toJsonString(config);
		String substituted = MDTSubstitutor.substibute(jsonStr);
		config = MDTModelSerDe.readValue(substituted, DefaultJdbcValueProviderConfig.class);

		FaaastRuntime faaast = new FaaastRuntime(serviceContext);
		String smIdShort = faaast.getSubmodelById(submodelId).getIdShort();
		m_variable = new JdbcAssetVariable(smIdShort, path, config.getReadQuery(), config.getUpdateQuery());
		m_variable.initialize(faaast);
		m_variable.setJdbcProcessor(jdbc);
		
		m_lastAccessTime = Instant.now().minus(Duration.ofDays(1));
	}

	@Override
	public DataElementValue getValue() throws AssetConnectionException {
		try {
			SubmodelElement element = m_variable.getElementBuffer();
			if ( m_config.getValidPeriod() != null ) {
				Instant now = Instant.now();
				if ( Duration.between(m_lastAccessTime, Instant.now()).compareTo(m_config.getValidPeriod()) > 0 ) {
					element = m_variable.read();
					m_lastAccessTime = now;
				}
			}
			
			return ElementValueMapper.toValue(element, DataElementValue.class);
		}
		catch ( ValueMappingException e ) {
			String msg = String.format("Failed to get %s, cause=%s", m_variable, e);
			throw new AssetConnectionException(msg, e);
		}
	}

	@Override
	public void setValue(DataElementValue value) throws AssetConnectionException {
		try {
			ElementValueMapper.setValue(m_variable.getElementBuffer(), value);
			m_variable.write();
			
			m_lastAccessTime = Instant.now();
		}
		catch ( ValueMappingException e ) {
			String msg = String.format("Failed to store %s, cause=%s", m_variable, e);
			throw new AssetConnectionException(msg, e);
		}
	}
}
