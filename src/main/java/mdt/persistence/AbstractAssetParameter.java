package mdt.persistence;

import java.io.IOException;

import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;

import utils.LoggerSettable;
import utils.func.FOption;
import utils.stream.FStream;

import mdt.aas.DataType;
import mdt.aas.DataTypes;
import mdt.model.MDTModelSerDe;
import mdt.model.ResourceNotFoundException;
import mdt.model.sm.SubmodelUtils;
import mdt.model.sm.value.ElementValues;
import mdt.model.sm.value.SubmodelElementValue;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public abstract class AbstractAssetParameter implements AssetParameter, LoggerSettable {
	private static final Logger s_logger = LoggerFactory.getLogger(AbstractAssetParameter.class);
	
	private final String m_submodelIdShort;
	private final String m_path;
	private Logger m_logger = s_logger;

	// 초기화 (init) 과정에서 설정됨
	protected Submodel m_submodel;
	@SuppressWarnings("rawtypes")
	private volatile DataType m_type = null;
	
	public AbstractAssetParameter(String submodelIdShort, String path) {
		m_submodelIdShort = submodelIdShort;
		m_path = path;
	}

	@Override
	public void initialize(Environment env) {
		m_submodel = FStream.from(env.getSubmodels())
							.filter(sm -> m_submodelIdShort.equals(sm.getIdShort()))
							.findFirst()
							.getOrThrow(() -> new ResourceNotFoundException("Submodel", "idShort=" + m_submodelIdShort));

		SubmodelElement buffer = SubmodelUtils.traverse(m_submodel, m_path);
		if ( buffer instanceof Property prop ) {
			m_type = DataTypes.fromAas4jDatatype(prop.getValueType());
		}
		else {
			m_type = null;
		}
	}
	
	@Override
	public String getSubmodelId() {
		return m_submodel.getId();
	}

	@Override
	public String getSubmodelIdShort() {
		return m_submodelIdShort;
	}

	@Override
	public String getElementPath() {
		return m_path;
	}

	@SuppressWarnings("rawtypes")
	public DataType getDataType() {
		assertInitialized();
		return m_type;
	}

	public void update(SubmodelElement buffer, SubmodelElementValue smev) {
		ElementValues.update(buffer, smev);
	}

	public void updateWithValueJsonNode(SubmodelElement buffer, JsonNode valueNode) throws IOException {
		ElementValues.update(buffer, valueNode);
	}

	public void updateWithValueJsonString(SubmodelElement buffer, String valueJsonString) throws IOException {
		updateWithValueJsonNode(buffer, MDTModelSerDe.readJsonNode(valueJsonString));
	}

	@Override
	public Logger getLogger() {
		return m_logger;
	}

	@Override
	public void setLogger(Logger logger) {
		m_logger = FOption.getOrElse(logger, s_logger);
	}
	
	@Override
	public String toString() {
		return String.format("%s[%s/%s]", getClass().getSimpleName(), getSubmodelIdShort(), getElementPath());
	}

	private void assertInitialized() {
		Preconditions.checkState(m_type != null, "Not initialized: {}", this);
	}
}