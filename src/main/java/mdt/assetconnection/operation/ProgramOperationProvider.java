package mdt.assetconnection.operation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.io.FileUtils;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import mdt.model.MDTModelSerDe;
import mdt.task.Parameter;
import mdt.task.builtin.ProgramOperationDescriptor;
import mdt.task.builtin.ProgramTask;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
class ProgramOperationProvider implements OperationProvider {
	private static final Logger s_logger = LoggerFactory.getLogger(ProgramOperationProvider.class);
	
	private final ServiceContext m_svcContext;
	private final ProgramOperationProviderConfig m_config;
	private final File m_opDescFile;
	
	ProgramOperationProvider(ServiceContext serviceContext,
								ProgramOperationProviderConfig config) throws IOException {
		m_svcContext = serviceContext;
		m_config = config;
		
		m_opDescFile = FileUtils.path(FileUtils.getCurrentWorkingDirectory(),
									m_config.getDescriptorFile());
		if ( m_opDescFile.isFile() && m_opDescFile.canRead() ) {
			if ( s_logger.isInfoEnabled() ) {
				s_logger.info("load Program Operation: path={}", m_opDescFile.getPath());
			}
		}
		else {
			throw new FileNotFoundException("Cannot read ProgramOperationDescriptor: path="
											+ m_opDescFile.getAbsolutePath());
		}
	}
	
	@Override
	public void invokeSync(OperationVariable[] inputVars, OperationVariable[] inoutputVars,
						OperationVariable[] outputVars) throws Exception {
		ProgramOperationDescriptor opDesc = ProgramOperationDescriptor.load(m_opDescFile,
																			MDTModelSerDe.getJsonMapper());
		if ( opDesc.getWorkingDirectory() == null ) {
			opDesc.setWorkingDirectory(m_opDescFile.getParentFile());
		}
		
		ProgramTask task = new ProgramTask(opDesc);
		
		// 모든 입력 변수들의 값을 CommandExecutor의 변수에 등록시킨다.
		for ( OperationVariable var: inputVars ) {
			SubmodelElement sme = var.getValue();
			task.addOrReplaceInputParameter(Parameter.of(sme.getIdShort(), sme));
		}
		// 모든 입출력 변수들의 값을 CommandExecutor의 변수에 등록시킨다.
		for ( OperationVariable var: inoutputVars ) {
			SubmodelElement sme = var.getValue();
			task.addOrReplaceInputParameter(Parameter.of(sme.getIdShort(), sme));
			task.addOrReplaceOutputParameter(Parameter.of(sme.getIdShort(), sme));
		}
		// 모든 출력 변수들의 값을 CommandExecutor의 변수에 등록시킨다.
		for ( OperationVariable var: outputVars ) {
			SubmodelElement sme = var.getValue();
			task.addOrReplaceOutputParameter(Parameter.of(sme.getIdShort(), sme));
		}
		
		task.run();
		
		if ( s_logger.isInfoEnabled() ) {
			s_logger.info("ProgramOperationProvider terminates");
		}
	}
}
