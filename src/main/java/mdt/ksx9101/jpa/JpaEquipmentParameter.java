package mdt.ksx9101.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mdt.ksx9101.jpa.JpaEquipmentParameter.Key;
import mdt.ksx9101.model.EquipmentParameter;
import mdt.model.SubmodelElementCollectionEntity;
import mdt.model.PropertyField;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Entity
@Table(name="V2_EQUIPMENTPARAMETER")
@IdClass(Key.class)
@Getter @Setter
public class JpaEquipmentParameter extends SubmodelElementCollectionEntity
									implements EquipmentParameter {
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Key {
		private String equipmentId;
		private String parameterId;
	}
	
	@Id private String equipmentId;
	@Id @PropertyField(idShort="ParameterID") private String parameterId;
	@PropertyField(idShort="ParameterName") private String parameterName;
	@PropertyField(idShort="ParameterType") private String parameterType = "String";
	@PropertyField(idShort="ParameterGrade") private String parameterGrade;
	@PropertyField(idShort="ParameterUOMCode") private String parameterUOMCode;
	@PropertyField(idShort="LSL") private String LSL;
	@PropertyField(idShort="USL") private String USL;
	
	@PropertyField(idShort="PeriodicDataCollectionIndicator")
	@Column(name="periodicDataCollectionIndicator")
	private String periodicDataCollectionIndicator;
	@PropertyField(idShort="DataCollectionPeriod")
	private String dataCollectionPeriod;
	
	public JpaEquipmentParameter() {
		super(null, "parameterId");
	}
	
	@Override
	public String toString() {
		return String.format("%s[%s.%s](%s)", this.getClass().getSimpleName(),
							this.equipmentId, this.parameterId, this.parameterType);
	}
}
