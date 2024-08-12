package mdt.ksx9101.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mdt.ksx9101.model.ProductionPlanning;
import mdt.model.AbstractMDTSubmodelElementCollection;
import mdt.model.PropertyField;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Entity
//@Table(name="production_plannings")
@Table(name="V2_PRODUCTIONPLANNING")
@Getter @Setter
public class JpaProductionPlanning extends AbstractMDTSubmodelElementCollection
									implements ProductionPlanning {
//	private String lineId;
	@PropertyField(idShort="ProductionPlanID") @Id private String productionPlanID;
	@PropertyField(idShort="ItemID") private String itemID;
	@PropertyField(idShort="ProductionPlanQuantity") private String productionPlanQuantity;
	@PropertyField(idShort="ScheduleStartDateTime") private String scheduleStartDateTime;
	@PropertyField(idShort="ScheduleEndDateTime") private String scheduleEndDateTime;
	
	public JpaProductionPlanning() {
		super(null, "productionPlanID");
	}
}
