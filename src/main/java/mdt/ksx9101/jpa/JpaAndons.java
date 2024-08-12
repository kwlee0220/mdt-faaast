package mdt.ksx9101.jpa;

import org.eclipse.digitaltwin.aas4j.v3.model.AasSubmodelElements;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.Getter;
import lombok.Setter;
import mdt.ksx9101.JpaEntityLoader;
import mdt.ksx9101.model.Andon;
import mdt.ksx9101.model.Andons;
import mdt.model.SubmodelElementListHandle;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter @Setter
public class JpaAndons extends SubmodelElementListHandle<Andon,JpaAndon> implements Andons {
	public JpaAndons() {
		super("Andons", null, false, AasSubmodelElements.SUBMODEL_ELEMENT_COLLECTION);
	}

	@Override
	public JpaAndon newElementHandle() {
		return new JpaAndon();
	}
	
	public static class Loader implements JpaEntityLoader<JpaAndons> {
		@Override
		public JpaAndons load(EntityManager em, Object key) {
			JpaAndons entity = new JpaAndons();
			TypedQuery<JpaAndon> query = em.createQuery("select r from JpaAndon r", JpaAndon.class);
			entity.setElementHandles(query.getResultList());
			
			return entity;
		}
	}
}
