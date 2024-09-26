package mdt.assetconnection;


import java.io.File;
import java.nio.file.Path;

import utils.HomeDirPicocliCommand;

import de.fraunhofer.iosb.ilt.faaast.service.Service;
import de.fraunhofer.iosb.ilt.faaast.service.config.ServiceConfig;
import picocli.CommandLine.Option;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MDTFaaastApplication extends HomeDirPicocliCommand {
	@Option(names={"--config", "-c"}, paramLabel="configFile",
			description={"The config file path. Default Value = config.json"})
	private File m_faastConfig;
	
	@Option(names={"--model", "-m"}, paramLabel="modelFile",
			description={"Asset Administration Shell Environment FilePath. Default Value = model.*"})
	private File m_initModel;
	
	@Option(names={"--empty", "-e"}, description={"Starts the FA³ST service with an empty "
												+ "Asset Administration Shell Environment. False by default"})
	private boolean m_emptyModel;
	
	@Option(names={"--no-validation"},
					description={"Disables validation, overrides validation configuration in core configuration."})
	private boolean m_noValidation;
	
	@Option(names={"--endpoint"}, paramLabel="<endpoints>[,<endpoints>...]",
			description={"Additional endpoints that should be started."})
	private String m_endpoints;
	
	@Option(names={"--loglevel-external"}, paramLabel="<logLevelExternal>",
			description={"Sets the log level for external packages. This overrides the log level defined by "
						+ "other commands such as -q or -v."})
	private String m_logLevelExternal;
	
	@Option(names={"--loglevel-faaast"}, paramLabel="<logLevelFaaast>",
			description={"Sets the log level for FA³ST packages. This overrides the log level defined by "
						+ "other commands such as -q or -v."})
	private String m_logLevelFaaast;
	
	@Option(names={"--quite", "-q"},
			description={"Reduces log output (ERROR for FA³ST packages, ERROR for all other packages). "
						+ "Default information about the starting process will still be printed."})
	private boolean m_quite;
	
	@Option(names={"--verbose", "-v"},
			description={"Enables verbose logging (INFO for FA³ST packages, WARN for all other packages)."})
	private boolean m_verbose;
	
	@Option(names={"-vv"},
			description={"Enables very verbose logging (DEBUG for FA³ST packages, INFO for all other packages)."})
	private boolean m_vv;
	
	@Option(names={"-vvv"},
			description={"Enables very very verbose logging (TRACE for FA³ST packages, DEBUG for all other packages)."})
	private boolean m_vvv;
	
	@Option(names={"--version", "-V"}, description={"Print version information and exit."})
	private boolean m_version;

	@Override
	protected void run(Path homeDir) throws Exception {
		ServiceConfig config = ServiceConfig.builder()
											.build();
		Service service = new Service(config);
		service.start();
	}
}
