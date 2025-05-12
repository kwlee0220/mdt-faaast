package mdt.persistence.asset;

import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import mdt.ElementLocation;


/**
 * 외부 설비 (Asset)가 생산하는 데이터를 다루기 위한 인터페이스를 정의한다.
 * <p>
 * {@code AssetVariable}은 AAS의 하나의 SubmodelElement와 연결되고, 이는 {@link #bind(Submodel)}를 통해
 * 지정된다.
 * {@link #load()}를 통해 연결된 설비에서 읽은 데이터가 SubmodelElement에 저장되고,
 * {@link #save()}를 통해 SubmodelElement의 값이 해당 설비에 전달된다.
 * <p>
 * 일부 설비의 경우에는 설비로 부터 데이터 읽기만 가능하고, 데이터 쓰기는 불가능한 경우가 있다.
 * 이 경우에는 {@link #isUpdateable()}이 false를 반환한다. 
 *
 * @author Kang-Woo Lee (ETRI)
 */
public interface AssetVariable {
	/**
	 * 연결된 SubmodelElement의 위치를 반환한다.
	 * 
	 * @return ElementLocation.
	 */
	public ElementLocation getElementLocation();
	
	/**
	 * 본 {@code AssetVariable}을 통해 SubmodelElement를 갱신할 수 있는지 여부를 반환한다.
	 * 
	 * @return 업데이트 가능 여부.
	 */
	public boolean isUpdateable();
	
	/**
     * 이 {@code AssetVariable}과 AAS내 SubmodelElement를 연결한다.
     * <p>
     * 연결할 SubmodelElement는 {@link #getSubmodelIdShort()}와 {@link #getElementPath()}를 통해
     * 식별된다.
     * 
     * @param submodel	연결할 SubmodelElment가 포함된 Submodel 객체.
     */
	public void bind(Submodel submodel);
	
	/**
	 * 연결된 SubmodelElement의 최신 값을 읽어 반환한다.
	 * <p>
	 * 연결된 datasource에서 최신 값을 읽어 최신의 SubmodelElement를 구성하여 반환한다.
	 * 
	 * @return	SubmodelElement
	 * @throws AssetVariableException	연결된 datasource에서 값 읽기가 실패한 경우.
	 */
	public SubmodelElement load() throws AssetVariableException;
	
	/**
	 * Buffer에 있는 값을 연결된 datasource에 저장한다.
	 * 
	 * @throws AssetVariableException	연결된 datasource에 값 저장이 실패한 경우.
	 */
	public void save() throws AssetVariableException;
	
	public default boolean contains(String elementPath) {
		return elementPath.startsWith(getElementLocation().getElementPath());
	}
	
	public default boolean isContained(String elementPath) {
		return getElementLocation().getElementPath().startsWith(elementPath);
	}
	
	public default boolean overlaps(String elementPath) {
		return contains(elementPath) || isContained(elementPath);
	}
}
