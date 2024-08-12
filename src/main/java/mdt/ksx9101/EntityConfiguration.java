package mdt.ksx9101;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import mdt.ksx9101.jpa.JpaMDTEntityFactory;
import mdt.model.MDTSubmodelElement;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Data
public class EntityConfiguration {
	private String type;
	private Object key;
	private String idShort;
	private MountPoint mountPoint;
	
	private static final JpaMDTEntityFactory FACTORY = new JpaMDTEntityFactory();
	
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @JsonIgnore
	private String rootPath = null;
	
	@Data
	public static class MountPoint {
		private String submodel;
		private String parentIdShortPath;
	}
	
	public String getIdShort() {
		return (this.idShort != null) ? this.idShort : this.type;
	}
	
	public MDTSubmodelElement loadJpaEntity(EntityManager em) {
		JpaEntityLoader loader = (JpaEntityLoader)FACTORY.newInstance(this.type);
		return (MDTSubmodelElement)loader.load(em, this.key);
	}
	
	public String getRootPathString() {
		if ( this.rootPath == null ) {
			this.rootPath = getMountPoint().parentIdShortPath + "." + getIdShort();
		}
		
		return this.rootPath;
	}
	
	@Override
	public String toString() {
		return String.format("Entity: type=%s, key=%s, idShort=%s", getType(), getKey(), getIdShort());
	}
}
