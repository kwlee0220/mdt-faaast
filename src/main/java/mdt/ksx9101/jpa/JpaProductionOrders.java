package mdt.ksx9101.jpa;

import org.eclipse.digitaltwin.aas4j.v3.model.AasSubmodelElements;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.Getter;
import lombok.Setter;
import mdt.ksx9101.JpaEntityLoader;
import mdt.ksx9101.model.ProductionOrder;
import mdt.ksx9101.model.ProductionOrders;
import mdt.model.SubmodelElementListHandle;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter @Setter
public class JpaProductionOrders extends SubmodelElementListHandle<ProductionOrder,JpaProductionOrder>
									implements ProductionOrders {
	public JpaProductionOrders() {
		super("ProductionOrders", null, false, AasSubmodelElements.SUBMODEL_ELEMENT_COLLECTION);
	}

	@Override
	public JpaProductionOrder newElementHandle() {
		return new JpaProductionOrder();
	}
	
	public static class Loader implements JpaEntityLoader<JpaProductionOrders> {
		@Override
		public JpaProductionOrders load(EntityManager em, Object key) {
			JpaProductionOrders entity = new JpaProductionOrders();
			TypedQuery<JpaProductionOrder> query
					= em.createQuery("select r from JpaProductionOrder r", JpaProductionOrder.class);
			entity.setElementHandles(query.getResultList());
			
			return entity;
		}
	}
}
