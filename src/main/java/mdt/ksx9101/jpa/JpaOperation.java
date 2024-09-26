package mdt.ksx9101.jpa;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import utils.stream.FStream;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import mdt.ksx9101.JpaEntityLoader;
import mdt.ksx9101.model.Operation;
import mdt.ksx9101.model.Parameter;
import mdt.ksx9101.model.ParameterValue;
import mdt.ksx9101.model.ProductionOrder;
import mdt.model.SubmodelElementCollectionEntity;
import mdt.model.PropertyField;
import mdt.model.SMLField;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Entity
//@Table(name="operations")
@Table(name="V2_OPERATION")
@Getter @Setter
public class JpaOperation extends SubmodelElementCollectionEntity implements Operation {
	@Id @PropertyField(idShort="OperationID") private String operationId;
	@PropertyField(idShort="OperationName") private String operationName;
	@PropertyField(idShort="OperationType") private String operationType;
	@PropertyField(idShort="UseIndicator") private String useIndicator;

	@SMLField(idShort="ProductionOrders", elementClass=JpaProductionOrder.class)
	@OneToMany(fetch=FetchType.EAGER, mappedBy="operationID")
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
	private List<JpaProductionOrder> productionOrders = Lists.newArrayList();

	@SMLField(idShort="OperationParameters", elementClass=JpaOperationParameter.class)
	@OneToMany(cascade = CascadeType.PERSIST)
	@JoinColumn(name="operationId")
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
	private List<JpaOperationParameter> parameters;

	@SMLField(idShort="OperationParameterValues", elementClass=JpaOperationParameterValue.class)
	@OneToMany(cascade = CascadeType.PERSIST)
	@JoinColumn(name="operationId")
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
	private List<JpaOperationParameterValue> parameterValues;
	
	public JpaOperation() {
		super("Operation", null);
	}
	
	@Override
	public String toString() {
		return String.format("%s[%s]", getClass().getSimpleName(), getOperationId());
	}

	@Override
	public List<ProductionOrder> getProductionOrders() {
		return FStream.from(this.productionOrders).cast(ProductionOrder.class).toList();
	}

	@Override
	public void setProductionOrders(List<ProductionOrder> orders) {
		this.productionOrders = FStream.from(orders)
										.cast(JpaProductionOrder.class)
										.toList();
	}

	@Override
	public List<Parameter> getParameters() {
		return FStream.from(this.parameters).cast(Parameter.class).toList();
	}

	@Override
	public void setParameters(List<Parameter> parameters) {
		this.parameters = FStream.from(parameters)
								.cast(JpaOperationParameter.class)
								.toList();
	}

	@Override
	public List<ParameterValue> getParameterValues() {
		return FStream.from(this.parameterValues).cast(ParameterValue.class).toList();
	}

	@Override
	public void setParameterValues(List<ParameterValue> parameterValues) {
		this.parameterValues = FStream.from(parameterValues)
								.cast(JpaOperationParameterValue.class)
								.toList();
	}
	
	public static JpaOperation load(EntityManager em, Object key) {
		Preconditions.checkArgument(key != null && key instanceof String);
		return em.find(JpaOperation.class, key);
	}

	public static class Loader implements JpaEntityLoader<JpaOperation> {
		@Override
		public JpaOperation load(EntityManager em, Object key) {
			Preconditions.checkArgument(key != null && key instanceof String);
			
			return em.find(JpaOperation.class, key);
		}
	}
}
