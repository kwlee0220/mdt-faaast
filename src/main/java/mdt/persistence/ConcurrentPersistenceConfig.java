package mdt.persistence;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class ConcurrentPersistenceConfig extends PersistenceStackConfig<ConcurrentPersistence> {
	@Override
	public String toString() {
		return String.format("%s: %s", getClass().getSimpleName(), super.toString());
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null || !(obj instanceof ConcurrentPersistenceConfig) ) {
			return false;
		}

		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
