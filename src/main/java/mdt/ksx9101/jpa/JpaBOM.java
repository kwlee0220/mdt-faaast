package mdt.ksx9101.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mdt.ksx9101.model.BOM;
import mdt.model.AbstractMDTSubmodelElementCollection;
import mdt.model.PropertyField;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Entity
//@Table(name="boms")
@Table(name="V2_LINE_BOM")
@Getter @Setter
public class JpaBOM extends AbstractMDTSubmodelElementCollection implements BOM {
//	private String lineId;
	@Id @PropertyField(idShort="BOMID") private String BOMID;
	@PropertyField(idShort="BOMType") private String BOMType;
	@PropertyField(idShort="ItemID") private String itemID;
	@PropertyField(idShort="BOMQuantity") private String BOMQuantity;
	@PropertyField(idShort="ItemUOMCode") private String itemUOMCode;
	@PropertyField(idShort="ValidStartDateTime") private String validStartDateTime;
	@PropertyField(idShort="ValidEndDateTime") private String validEndDateTime;
	
	public JpaBOM() {
		super(null, "BOMID");
	}
	
	@Override
	public String toString() {
		return String.format("%s[%s]", getClass().getSimpleName(), getBOMID());
	}
}
