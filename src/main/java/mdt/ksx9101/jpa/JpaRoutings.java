package mdt.ksx9101.jpa;

import org.eclipse.digitaltwin.aas4j.v3.model.AasSubmodelElements;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.Getter;
import lombok.Setter;
import mdt.ksx9101.JpaEntityLoader;
import mdt.ksx9101.model.Routing;
import mdt.ksx9101.model.Routings;
import mdt.model.SubmodelElementListEntity;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter @Setter
public class JpaRoutings extends SubmodelElementListEntity<Routing,JpaRouting>
							implements Routings {
	public JpaRoutings() {
		super("Routings", null, false, AasSubmodelElements.SUBMODEL_ELEMENT_COLLECTION);
	}

	@Override
	public JpaRouting newElementEntity() {
		return new JpaRouting();
	}
	
	public static class Loader implements JpaEntityLoader<JpaRoutings> {
		@Override
		public JpaRoutings load(EntityManager em, Object key) {
			JpaRoutings entity = new JpaRoutings();
			TypedQuery<JpaRouting> query = em.createQuery("select r from JpaRouting r", JpaRouting.class);
			entity.setElementHandles(query.getResultList());
			
			return entity;
		}
	}
}
