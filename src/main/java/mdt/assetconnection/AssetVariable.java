package mdt.assetconnection;

import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public interface AssetVariable {
	public String getSubmodelIdShort();
	public String getElementPath();
	
	public SubmodelElement getElementBuffer();
	
	public SubmodelElement read() throws AssetVariableException;
	public void write() throws AssetVariableException;
}
