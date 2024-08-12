package mdt.ksx9101.jpa;

import org.eclipse.digitaltwin.aas4j.v3.model.AasSubmodelElements;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.Getter;
import lombok.Setter;
import mdt.ksx9101.JpaEntityLoader;
import mdt.ksx9101.model.BOM;
import mdt.ksx9101.model.BOMs;
import mdt.model.SubmodelElementListHandle;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter @Setter
public class JpaBOMs extends SubmodelElementListHandle<BOM,JpaBOM> implements BOMs {
	public JpaBOMs() {
		super("BOMs", null, false, AasSubmodelElements.SUBMODEL_ELEMENT_COLLECTION);
	}

	@Override
	public JpaBOM newElementHandle() {
		return new JpaBOM();
	}
	
	public static class Loader implements JpaEntityLoader<JpaBOMs> {
		@Override
		public JpaBOMs load(EntityManager em, Object key) {
			JpaBOMs entity = new JpaBOMs();
			TypedQuery<JpaBOM> query = em.createQuery("select r from JpaBOM r", JpaBOM.class);
			entity.setElementHandles(query.getResultList());
			
			return entity;
		}
	}
}
