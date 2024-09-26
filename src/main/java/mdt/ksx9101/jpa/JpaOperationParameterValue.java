package mdt.ksx9101.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mdt.ksx9101.jpa.JpaOperationParameter.Key;
import mdt.ksx9101.model.OperationParameterValue;
import mdt.model.SubmodelElementCollectionEntity;
import mdt.model.PropertyField;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Entity
//@Table(name="operation_parameter_values")
@Table(name="V2_OPERATIONPARAMETERVALUE")
@IdClass(Key.class)
@Getter @Setter
public class JpaOperationParameterValue extends SubmodelElementCollectionEntity
										implements OperationParameterValue {
	@Id private String operationId;
	@Id @PropertyField(idShort="ParameterID") private String parameterId;
	@PropertyField(idShort="ParameterValue", keepNullField=true) private String parameterValue;
	@PropertyField(idShort="EventDateTime") private String eventDateTime;
	@PropertyField(idShort="ValidationResultCode") private String validationResultCode;
	
	public JpaOperationParameterValue() {
		super(null, "parameterId");
	}
	
	@Override
	public String toString() {
		return String.format("%s[%s.%s]=%s", this.getClass().getSimpleName(),
							this.operationId, this.parameterId, this.parameterValue);
	}
}
