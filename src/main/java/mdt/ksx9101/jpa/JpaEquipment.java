package mdt.ksx9101.jpa;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import utils.stream.FStream;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import mdt.ksx9101.JpaEntityLoader;
import mdt.ksx9101.model.Equipment;
import mdt.ksx9101.model.Parameter;
import mdt.ksx9101.model.ParameterValue;
import mdt.model.SubmodelElementCollectionEntity;
import mdt.model.PropertyField;
import mdt.model.SMLField;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Entity
@Table(name="V2_EQUIPMENT")
@Getter @Setter
public class JpaEquipment extends SubmodelElementCollectionEntity
							implements Equipment {
	@PropertyField(idShort="EquipmentID") @Id private String equipmentId;
	@PropertyField(idShort="EquipmentName") private String equipmentName;
	@PropertyField(idShort="EquipmentType") private String equipmentType;
	@PropertyField(idShort="UseIndicator") private String useIndicator;

	@SMLField(idShort="EquipmentParameters", elementClass=JpaEquipmentParameter.class)
	@OneToMany(cascade = CascadeType.PERSIST)
	@JoinColumn(name="equipmentId")
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
	private List<JpaEquipmentParameter> parameters = Lists.newArrayList();

	@SMLField(idShort="EquipmentParameterValues", elementClass=JpaEquipmentParameterValue.class)
	@OneToMany(cascade = CascadeType.PERSIST)
	@JoinColumn(name="equipmentId")
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
	private List<JpaEquipmentParameterValue> parameterValues = Lists.newArrayList();
	
	public JpaEquipment() {
		super("Equipment", null);
	}

	@Override
	public List<Parameter> getParameters() {
		return FStream.from(this.parameters).cast(Parameter.class).toList();
	}

	@Override
	public void setParameters(List<Parameter> parameters) {
		this.parameters = FStream.from(parameters)
								.cast(JpaEquipmentParameter.class)
								.toList();
	}

	@Override
	public List<ParameterValue> getParameterValues() {
		return FStream.from(this.parameterValues).cast(ParameterValue.class).toList();
	}

	@Override
	public void setParameterValues(List<ParameterValue> parameterValues) {
		this.parameterValues = FStream.from(parameterValues)
								.cast(JpaEquipmentParameterValue.class)
								.toList();
	}
	
	@Override
	public String toString() {
		return String.format("%s[%s]", getClass().getSimpleName(), getEquipmentId());
	}

	public static class Loader implements JpaEntityLoader<JpaEquipment> {
		@Override
		public JpaEquipment load(EntityManager em, Object key) {
			Preconditions.checkArgument(key != null && key instanceof String);
			
			return em.find(JpaEquipment.class, key);
		}
	}
}
