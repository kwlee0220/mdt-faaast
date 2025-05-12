package mdt.persistence.asset;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;

import utils.LoggerSettable;
import utils.func.FOption;

import mdt.ElementLocation;
import mdt.aas.DataType;
import mdt.aas.DataTypes;
import mdt.model.MDTModelSerDe;
import mdt.model.sm.SubmodelUtils;
import mdt.model.sm.value.ElementValue;
import mdt.model.sm.value.ElementValues;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public abstract class AbstractAssetVariable<T extends AssetVariableConfig> implements AssetVariable, LoggerSettable {
	private static final Logger s_logger = LoggerFactory.getLogger(AbstractAssetVariable.class);
	
	protected final T m_config;
	protected SubmodelElement m_buffer;
	private volatile DataType<?> m_type = null;
	private Instant m_lastUpdatedTime = Instant.EPOCH;
	private Logger m_logger = s_logger;
	
	public AbstractAssetVariable(T config) {
		m_config = config;
	}

	@Override
	public ElementLocation getElementLocation() {
		return m_config.getElementLocation();
	}
	
	public Duration getValidPeriod() {
		return m_config.getValidPeriod();
	}
	
	public boolean isExpired(Instant ts) {
		Duration elapsed = Duration.between(getLastUpdatedTime(), ts);
		return elapsed.compareTo(getValidPeriod()) > 0;
	}

	@Override
	public void bind(Submodel submodel) {
		// 검색된 Submodel 내에서 본 element에 해당하는 SubmodelElement를 찾는다.
		m_buffer = SubmodelUtils.traverse(submodel, getElementLocation().getElementPath());
		if ( m_buffer instanceof Property prop ) {
			m_type = DataTypes.fromAas4jDatatype(prop.getValueType());
		}
		else {
			m_type = null;
		}
	}

	@SuppressWarnings("rawtypes")
	public DataType getDataType() {
		assertSubmodelElement();
		return m_type;
	}
	
	protected Instant getLastUpdatedTime() {
        return m_lastUpdatedTime;
	}
	
	protected void setLastUpdatedTime(Instant time) {
		m_lastUpdatedTime = time;
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
		return String.format("%s[%s]", getClass().getSimpleName(), getElementLocation());
	}

	private void assertSubmodelElement() {
		Preconditions.checkState(m_buffer != null, "SubmodelElement has not been bound: {}", this);
	}

	protected void update(SubmodelElement buffer, ElementValue smev) {
		ElementValues.update(buffer, smev);
	}

	protected void updateWithValueJsonNode(SubmodelElement buffer, JsonNode valueNode) throws IOException {
		ElementValues.update(buffer, valueNode);
	}

	protected void updateWithValueJsonString(SubmodelElement buffer, String valueJsonString) throws IOException {
		updateWithValueJsonNode(buffer, MDTModelSerDe.readJsonNode(valueJsonString));
	}
}