package mdt;

import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import com.google.common.base.Preconditions;

import mdt.aas.DataType;
import mdt.aas.DataTypes;
import mdt.model.sm.SubmodelUtils;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class ElementHandle {
	private final String m_submodelIdShort;
	private final String m_path;

	// 초기화 (init) 과정에서 설정됨
	private FaaastRuntime m_faaast = null;
	private volatile String m_submodelId = null;
	private volatile SubmodelElement m_element = null;
	@SuppressWarnings("rawtypes")
	private volatile DataType m_type = null;
	
	public ElementHandle(String submodel, String path) {
		m_submodelIdShort = submodel;
		m_path = path;
	}
	
	public void init(FaaastRuntime rt) {
		m_faaast = rt;

		Submodel sm = m_faaast.getSubmodelByIdShort(m_submodelIdShort);
		m_submodelId = sm.getId();
		m_element = SubmodelUtils.traverse(sm, m_path);
		
		if ( m_element instanceof Property prop ) {
			m_type = DataTypes.fromAas4jDatatype(prop.getValueType());
		}
		else {
			m_type = null;
		}
	}
	
	public String getSubmodelId() {
		assertInitialized();
		return m_submodelId;
	}
	
	public String getSubmodelIdShort() {
		return m_submodelIdShort;
	}

	public String getElementPath() {
		return m_path;
	}
	
	public SubmodelElement getElement() {
		assertInitialized();	
		return m_faaast.getSubmodelElementByPath(m_submodelId, m_path);
	}
	
	@SuppressWarnings("rawtypes")
	public DataType getDataType() {
		assertInitialized();
		return m_type;
	}
	
	@Override
	public String toString() {
		return String.format("AssetVariable[%s/%s]", m_submodelIdShort, m_path);
	}
	
	private void assertInitialized() {
		Preconditions.checkState(m_faaast != null, "Not initialized: {}", this);
	}
}