package mdt.ksx9101.jpa;

import org.eclipse.digitaltwin.aas4j.v3.model.AasSubmodelElements;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.Getter;
import lombok.Setter;
import mdt.ksx9101.JpaEntityLoader;
import mdt.ksx9101.model.Repair;
import mdt.ksx9101.model.Repairs;
import mdt.model.SubmodelElementListEntity;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter @Setter
public class JpaRepairs extends SubmodelElementListEntity<Repair,JpaRepair> implements Repairs {
	public JpaRepairs() {
		super("Repairs", null, false, AasSubmodelElements.SUBMODEL_ELEMENT_COLLECTION);
	}

	@Override
	public JpaRepair newElementEntity() {
		return new JpaRepair();
	}
	
	public static class Loader implements JpaEntityLoader<JpaRepairs> {
		@Override
		public JpaRepairs load(EntityManager em, Object key) {
			JpaRepairs entity = new JpaRepairs();
			TypedQuery<JpaRepair> query = em.createQuery("select r from JpaRepair r", JpaRepair.class);
			entity.setElementHandles(query.getResultList());
			
			return entity;
		}
	}
}
