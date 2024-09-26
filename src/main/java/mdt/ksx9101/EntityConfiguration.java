package mdt.ksx9101;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import mdt.ksx9101.jpa.JpaMDTEntityFactory;
import mdt.model.SubmodelElementEntity;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter
public class EntityConfiguration {
	private String type;
	private Object key;
	private String idShort;
	private MountPoint mountPoint;
	
	private static final JpaMDTEntityFactory FACTORY = new JpaMDTEntityFactory();
	
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @JsonIgnore
	private String rootPath = null;
	
	public EntityConfiguration(@JsonProperty("type") String type,
								@JsonProperty("key") Object key,
								@JsonProperty("idShort") String idShort,
								@JsonProperty("mountPoint")MountPoint mountPoint) {
		this.type = type;
		this.key = key;
		this.idShort = idShort;
		this.mountPoint = mountPoint;
	}
	
	public String getIdShort() {
		return (this.idShort != null) ? this.idShort : this.type;
	}
	
	public SubmodelElementEntity loadJpaEntity(EntityManager em) {
		JpaEntityLoader loader = (JpaEntityLoader)FACTORY.newInstance(this.type);
		return (SubmodelElementEntity)loader.load(em, this.key);
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
	
	@Getter @ToString
	public static class MountPoint {
		private String submodel;
		private String parentIdShortPath;
		
		@JsonCreator
		public MountPoint(@JsonProperty("submodel") String submodel,
							@JsonProperty("parentIdShortPath") String parentIdShortPath) {
			this.submodel = submodel;
			this.parentIdShortPath = parentIdShortPath;
		}
	}
}
