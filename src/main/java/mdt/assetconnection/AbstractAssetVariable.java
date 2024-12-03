package mdt.assetconnection;

import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import com.google.common.base.Preconditions;

import mdt.FaaastRuntime;
import mdt.aas.DataType;
import mdt.aas.DataTypes;
import mdt.model.sm.SubmodelUtils;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public abstract class AbstractAssetVariable implements AssetVariable {
	private final String m_submodelIdShort;
	private final String m_path;

	// 초기화 (init) 과정에서 설정됨
	private FaaastRuntime m_faaast = null;
	protected SubmodelElement m_buffer;
	@SuppressWarnings("rawtypes")
	private volatile DataType m_type = null;
	
	public AbstractAssetVariable(String submodel, String path) {
		m_submodelIdShort = submodel;
		m_path = path;
	}
	
	public void initialize(FaaastRuntime faaast) {
		m_faaast = faaast;
		
		Submodel sm = faaast.getSubmodelByIdShort(m_submodelIdShort);
		m_buffer = SubmodelUtils.traverse(sm, m_path);
		
		if ( m_buffer instanceof Property prop ) {
			m_type = DataTypes.fromAas4jDatatype(prop.getValueType());
		}
		else {
			m_type = null;
		}
	}
	
	public String getSubmodelIdShort() {
		return m_submodelIdShort;
	}

	public String getElementPath() {
		return m_path;
	}

	public SubmodelElement getElementBuffer() {
		assertInitialized();
		return m_buffer;
	}
	
	@SuppressWarnings("rawtypes")
	public DataType getDataType() {
		assertInitialized();
		return m_type;
	}
	
	@Override
	public String toString() {
		return String.format("AssetVariable[%s/%s]", getSubmodelIdShort(), getElementPath());
	}
	
	private void assertInitialized() {
		Preconditions.checkState(m_faaast != null, "Not initialized: {}", this);
	}
}