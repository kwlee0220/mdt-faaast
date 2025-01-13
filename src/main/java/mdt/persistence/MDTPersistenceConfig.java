package mdt.persistence;

import java.util.List;

import de.fraunhofer.iosb.ilt.faaast.service.persistence.PersistenceConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter @Setter
@NoArgsConstructor
public class MDTPersistenceConfig extends PersistenceConfig<MDTPersistence> {
	private List<AssetParameterConfig> assetParameters;
}
