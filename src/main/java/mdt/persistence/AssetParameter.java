package mdt.persistence;

import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public interface AssetParameter {
	public String getSubmodelId();
	public String getSubmodelIdShort();
	public String getElementPath();
	
	public void initialize(Environment env);
	
	public default boolean contains(String path) {
		return path.startsWith(getElementPath());
	}
	public default boolean isContained(String path) {
		return getElementPath().startsWith(path);
	}
	public default boolean overlaps(String path) {
		return contains(path) || isContained(path);
	}
	
	public void load(SubmodelElement buffer, String path) throws AssetParameterException;
	public boolean save(String path, SubmodelElement element) throws AssetParameterException;
}
