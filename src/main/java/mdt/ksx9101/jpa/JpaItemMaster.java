package mdt.ksx9101.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mdt.ksx9101.model.ItemMaster;
import mdt.model.AbstractMDTSubmodelElementCollection;
import mdt.model.PropertyField;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Entity
//@Table(name="item_masters")
@Table(name="V2_LINE_ITEMMASTER")
@Getter @Setter
public class JpaItemMaster extends AbstractMDTSubmodelElementCollection implements ItemMaster {
//	private String lineId;
	@Id @PropertyField(idShort="ItemID") private String itemID;
	@PropertyField(idShort="ItemType") private String itemType;
	@PropertyField(idShort="ItemName") private String itemName;
	@PropertyField(idShort="ItemUOMCode") private String itemUOMCode;
	@PropertyField(idShort="LotSize") private String lotSize;
	
	public JpaItemMaster() {
		super(null, "itemID");
	}
	
	@Override
	public String toString() {
		return String.format("%s[%s]", getClass().getSimpleName(), getItemID());
	}
}