package mdt.ksx9101.jpa;

import org.eclipse.digitaltwin.aas4j.v3.model.AasSubmodelElements;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.Getter;
import lombok.Setter;
import mdt.ksx9101.JpaEntityLoader;
import mdt.ksx9101.model.ItemMaster;
import mdt.ksx9101.model.ItemMasters;
import mdt.model.SubmodelElementListHandle;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter @Setter
public class JpaItemMasters extends SubmodelElementListHandle<ItemMaster,JpaItemMaster>
								implements ItemMasters {
	public JpaItemMasters() {
		super("ItemMasters", null, false, AasSubmodelElements.SUBMODEL_ELEMENT_COLLECTION);
	}

	@Override
	public JpaItemMaster newElementHandle() {
		return new JpaItemMaster();
	}
	
	public static class Loader implements JpaEntityLoader<JpaItemMasters> {
		@Override
		public JpaItemMasters load(EntityManager em, Object key) {
			JpaItemMasters entity = new JpaItemMasters();
			TypedQuery<JpaItemMaster> query = em.createQuery("select r from JpaItemMaster r", JpaItemMaster.class);
			entity.setElementHandles(query.getResultList());
			
			return entity;
		}
	}
}
