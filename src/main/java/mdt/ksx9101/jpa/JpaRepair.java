package mdt.ksx9101.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mdt.ksx9101.model.Repair;
import mdt.model.AbstractMDTSubmodelElementCollection;
import mdt.model.PropertyField;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Entity
//@Table(name="repairs")
@Table(name="V2_REPAIR")
@Getter @Setter
public class JpaRepair extends AbstractMDTSubmodelElementCollection implements Repair {
//	private String lineId;
	@PropertyField(idShort="RepairID") @Id private Long repairID;
	@PropertyField(idShort="GroupID") private String groupID;
	@PropertyField(idShort="DefectRegOperationID") private String defectRegOperationID;
	@PropertyField(idShort="DefectRegEquipmentID") private String defectRegEquipmentID;
	@PropertyField(idShort="DefectRegDateTime") private String defectRegDateTime;
	@PropertyField(idShort="RepairDateTime") private String repairDateTime;
	@PropertyField(idShort="ProductionItemSerialNO") private String productionItemSerialNO;
	@PropertyField(idShort="DetectedProcess") private String detectedProcess;
	@PropertyField(idShort="InitialDefectLevel1") private String initialDefectLevel1;
	@PropertyField(idShort="InitialDefectLevel2") private String initialDefectLevel2;
	@PropertyField(idShort="InitialDefectLevel3") private String initialDefectLevel3;
	
	public JpaRepair() {
		super(null, "repairID");
	}
	
	@Override
	public String toString() {
		return String.format("%s[%s]", getClass().getSimpleName(), getRepairID());
	}
}