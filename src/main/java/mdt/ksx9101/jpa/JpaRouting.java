package mdt.ksx9101.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mdt.ksx9101.model.Routing;
import mdt.model.AbstractMDTSubmodelElementCollection;
import mdt.model.PropertyField;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Entity
//@Table(name="routings")
@Table(name="V2_LINE_ROUTING")
@Getter @Setter
public class JpaRouting extends AbstractMDTSubmodelElementCollection implements Routing {
//	private String lineId;
	@Id @PropertyField(idShort="RoutingID") private String routingID;
	@PropertyField(idShort="RoutingName") private String routingName;
	@PropertyField(idShort="ItemID") private String itemID;
	@PropertyField(idShort="SetupTime") private String setupTime;
	
	public JpaRouting() {
		super(null, "routingID");
	}
	
	@Override
	public String toString() {
		return String.format("%s[%s]", getClass().getSimpleName(), getItemID());
	}
}