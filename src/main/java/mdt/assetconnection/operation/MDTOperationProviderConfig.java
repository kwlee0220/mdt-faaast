package mdt.assetconnection.operation;

import javax.annotation.Nullable;

import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.common.provider.config.AbstractMultiFormatOperationProviderConfig;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter @Setter
public class MDTOperationProviderConfig extends AbstractMultiFormatOperationProviderConfig {
	@Nullable private JavaOperationProviderConfig java;
	@Nullable private ProgramOperationProviderConfig program;
}
