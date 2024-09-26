package mdt.ksx9101;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

import utils.func.FOption;
import utils.func.Funcs;
import utils.func.Tuple;
import utils.stream.FStream;

import de.fraunhofer.iosb.ilt.faaast.service.model.IdShortPath;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.PersistenceConfig;
import lombok.Getter;
import lombok.Setter;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter
@Setter
@JsonIgnoreProperties(value = {"entityConfigs"})
public class KSX9101PersistencerConfig extends PersistenceConfig<KSX9101Persistence> {
	@JsonProperty("entities")
	private List<EntityConfiguration> entityConfigs = Lists.newArrayList();
	@JsonProperty("jpa")
	private JpaConfiguration jpaConfig;

    public static Builder builder() {
        return new Builder();
    }
	
	public List<EntityConfiguration> findSubEntityConfigurations(String pathStr) {
		return FStream.from(entityConfigs)
						.filter(conf -> conf.getRootPathString().startsWith(pathStr))
						.toList();
	}
	
	public EntityConfiguration findCoverEntityConfiguration(String pathStr) {
		return Funcs.findFirst(this.entityConfigs,
								conf -> pathStr.startsWith(conf.getRootPathString()))
					.getOrNull();
	}
	
	public FOption<Tuple<EntityConfiguration,String>> toRelativePath(IdShortPath path) {
		String pathStr = path.toString();
		return FStream.from(entityConfigs)
						.filter(conf -> pathStr.startsWith(conf.getRootPathString()))
						.findFirst()
						.map(entityConf -> {
							String prefix = entityConf.getRootPathString();
							String suffix = path.toString().substring(prefix.length());
							return Tuple.of(entityConf, suffix);
						});
	}

    private abstract static class AbstractBuilder<T extends KSX9101PersistencerConfig,
    												B extends AbstractBuilder<T, B>>
            extends PersistenceConfig.AbstractBuilder<KSX9101Persistence, T, B> {
        public B entityConfigs(List<EntityConfiguration> value) {
            getBuildingInstance().setEntityConfigs(value);
            return getSelf();
        }

        public B jpaConfiguration(JpaConfiguration value) {
            getBuildingInstance().setJpaConfig(value);
            return getSelf();
        }
    }

    public static class Builder extends AbstractBuilder<KSX9101PersistencerConfig, Builder> {
        @Override
        protected Builder getSelf() {
            return this;
        }

        @Override
        protected KSX9101PersistencerConfig newBuildingInstance() {
            return new KSX9101PersistencerConfig();
        }
    }
}
