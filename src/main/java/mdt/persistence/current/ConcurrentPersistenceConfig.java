package mdt.persistence.current;

import de.fraunhofer.iosb.ilt.faaast.service.persistence.PersistenceConfig;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class ConcurrentPersistenceConfig extends PersistenceConfig<ConcurrentPersistence> {
    public static Builder builder() {
        return new Builder();
    }

    private abstract static class AbstractBuilder<T extends ConcurrentPersistenceConfig, B extends AbstractBuilder<T, B>>
            extends PersistenceConfig.AbstractBuilder<ConcurrentPersistence, T, B> {

    }

    public static class Builder extends AbstractBuilder<ConcurrentPersistenceConfig, Builder> {
        @Override
        protected Builder getSelf() {
            return this;
        }

        @Override
        protected ConcurrentPersistenceConfig newBuildingInstance() {
            return new ConcurrentPersistenceConfig();
        }
    }
    
    @Override
	public String toString() {
		return String.format("ConcurrentPersistenceConfig: model=%s", getInitialModelFile());
	}
}
