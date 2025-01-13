package mdt;

import java.io.IOException;

import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import com.google.common.base.Preconditions;

import utils.InternalException;

import mdt.aas.DataType;
import mdt.aas.DataTypes;
import mdt.model.MDTModelSerDe;
import mdt.model.sm.SubmodelUtils;
import mdt.model.sm.value.ElementValues;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class ElementHandle {
	private final String m_path;

	// 초기화 (init) 과정에서 설정됨
	private Submodel m_submodel;
	@SuppressWarnings("rawtypes")
	private volatile DataType m_type = null;
	
	public ElementHandle(String path) {
		m_path = path;
	}
	
	public void initialize(Submodel submodel) {
		m_submodel = submodel;
		
		SubmodelElement element = SubmodelUtils.traverse(submodel, m_path);
		if ( element instanceof Property prop ) {
			m_type = DataTypes.fromAas4jDatatype(prop.getValueType());
		}
		else {
			m_type = null;
		}
	}
	
	public String getSubmodelId() {
		assertInitialized();
		return m_submodel.getId();
	}
	
	public String getSubmodelIdShort() {
		return m_submodel.getIdShort();
	}

	public String getElementPath() {
		return m_path;
	}
	
	@SuppressWarnings("rawtypes")
	public DataType getDataType() {
		assertInitialized();
		return m_type;
	}
	
	@SuppressWarnings("unchecked")
	public void update(SubmodelElement buffer, Object value) {
		if ( m_type != null ) {
			((Property)buffer).setValue(m_type.toValueString(value));
		}
		else {
			try {
				ElementValues.updateWithRawString(buffer, (String)value);
			}
			catch ( IOException e ) {
				String msg = String.format("Failed to update %s with value=%s", this, value);
				throw new InternalException(msg, e);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public Object toJdbcObject(SubmodelElement element) {
		if ( element instanceof Property prop ) {
			Object propValue = m_type.parseValueString(prop.getValue());
			return m_type.toJdbcObject(propValue);
		}
		else {
			return MDTModelSerDe.toJsonString(ElementValues.getValue(element));
		}
	}
	
	@Override
	public String toString() {
		String initStr = (m_submodel != null) ? "initialized" : "uninitialized";
		return String.format("AssetVariable[%s/%s, %s]", m_submodel.getIdShort(), m_path, initStr);
	}
	
	private void assertInitialized() {
		Preconditions.checkState(m_submodel != null, "Not initialized: {}", this);
	}
}