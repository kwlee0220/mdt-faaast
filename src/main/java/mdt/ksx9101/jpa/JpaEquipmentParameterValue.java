package mdt.ksx9101.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mdt.ksx9101.jpa.JpaEquipmentParameter.Key;
import mdt.ksx9101.model.EquipmentParameterValue;
import mdt.model.SubmodelElementCollectionEntity;
import mdt.model.PropertyField;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Entity
//@Table(name="equipment_parameter_values")
@Table(name="V2_EQUIPMENTPARAMETERVALUE")
@IdClass(Key.class)
@Getter @Setter
public class JpaEquipmentParameterValue extends SubmodelElementCollectionEntity
										implements EquipmentParameterValue {
	@Id private String equipmentId;
	@Id @PropertyField(idShort="ParameterID") private String parameterId;
	@PropertyField(idShort="ParameterValue", keepNullField=true) private String parameterValue;
	@PropertyField(idShort="EventDateTime") private String eventDateTime;
	@PropertyField(idShort="ValidationResultCode") private String validationResultCode;
	
	public JpaEquipmentParameterValue() {
		super(null, "parameterId");
	}
	
	@Override
	public String toString() {
		return String.format("%s[%s.%s]=%s", this.getClass().getSimpleName(),
							this.equipmentId, this.parameterId, this.parameterValue);
	}
}
