package mdt.ksx9101;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import utils.Utilities;
import utils.func.FOption;
import utils.stream.FStream;

import lombok.experimental.UtilityClass;
import mdt.model.MDTModelSerDe;
import mdt.model.instance.MDTInstanceManager;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@UtilityClass
public class GlobalConfigurations {
	private static final String ENVVAR_MDT_GLOBAL_CONFIG = "MDT_GLOBAL_CONFIG_FILE";
	private static final File DEFAULT_MDT_GLOBAL_CONFIG = new File(MDTInstanceManager.GLOBAL_CONF_FILE_NAME);
	
	public static FOption<JpaConfiguration> loadJpaConfiguration() {
		File globalConfigFile = Utilities.getEnvironmentVariableFile(ENVVAR_MDT_GLOBAL_CONFIG)
											.getOrElse(DEFAULT_MDT_GLOBAL_CONFIG);
		if ( !globalConfigFile.exists() ) {
			return FOption.empty();
		}
		
		try {
			// "persistent" key에 해당하는 설정 정보를 반환한다.
			//
			
			// 설정 파일을 tree 형태로 읽어 "persistent"에 해당하는 노드를 찾는다
			// 찾은 sub-node를 주어진 class를 기준으로 다시 read하여 configuration 객체를 생성한다.
			JsonMapper mapper = MDTModelSerDe.getJsonMapper();
			return  FStream.from(mapper.readTree(globalConfigFile).properties())
							.findFirst(ent -> ent.getKey().equals("persistence"))
							.mapOrThrow(ent -> mapper.readValue(ent.getValue().traverse(),
																GlobalPersistenceConfig.class))
							.map(GlobalPersistenceConfig::getJpaConfig);
		}
		catch ( JsonMappingException e ) {
			String msg = String.format("Failed to parse global_configuration: file=%s, cause=%s",
										globalConfigFile.getAbsolutePath(), e);
			throw new IllegalStateException(msg);
		}
		catch ( IOException e ) {
			String msg = String.format("Failed to read global_configuration: file=%s, cause=%s",
										globalConfigFile.getAbsolutePath(), e);
			throw new IllegalStateException(msg);
		}
	}
}
