package mdt.assetconnection.operation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import utils.InternalException;
import utils.async.AsyncResult;
import utils.async.CommandExecution;
import utils.async.CommandExecution.FileVariable;
import utils.async.CommandExecution.Variable;
import utils.func.FOption;
import utils.io.FileUtils;
import utils.io.IOUtils;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.request.submodel.GetFileByPathRequest;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.response.submodel.GetFileByPathResponse;
import mdt.model.MDTModelSerDe;
import mdt.model.sm.value.ElementValues;
import mdt.task.TaskException;
import mdt.task.builtin.ProgramOperationDescriptor;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
class ProgramOperationProvider2 implements OperationProvider {
	private static final Logger s_logger = LoggerFactory.getLogger(ProgramOperationProvider2.class);
	
	private final ServiceContext m_svcContext;
	private final ProgramOperationProviderConfig m_config;
	private final File m_opDescFile;
	private final File m_workingDirectory;
	
	ProgramOperationProvider2(ServiceContext serviceContext,
								ProgramOperationProviderConfig config) throws IOException {
		m_svcContext = serviceContext;
		m_config = config;
		
		m_opDescFile = FileUtils.path(FileUtils.getCurrentWorkingDirectory(),
									m_config.getDescriptorFile());
		if ( m_opDescFile.isFile() && m_opDescFile.canRead() ) {
			if ( s_logger.isInfoEnabled() ) {
				s_logger.info("load Program Operation: path={}", m_opDescFile.getPath());
			}
			
			m_workingDirectory = m_opDescFile.getParentFile();
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
		
		CommandExecution.Builder builder = CommandExecution.builder()
															.addCommand(opDesc.getCommandLine())
															.setWorkingDirectory(m_workingDirectory)
															.setTimeout(opDesc.getTimeout());
		
		// 모든 입력 변수들의 값을 CommandExecutor의 변수에 등록시킨다.
		for ( OperationVariable var: inputVars ) {
			builder.addVariable(toCommandVariable(var, m_workingDirectory));
		}
		// 모든 입출력 변수들의 값을 CommandExecutor의 변수에 등록시킨다.
		for ( OperationVariable var: inoutputVars ) {
			builder.addVariable(toCommandVariable(var, m_workingDirectory));
		}
		// 모든 출력 변수들의 값을 CommandExecutor의 변수에 등록시킨다.
		for ( OperationVariable var: outputVars ) {
			builder.addVariable(toCommandVariable(var, m_workingDirectory));
		}
		
		// stdout/stderr redirection
		builder.redictStdoutToFile(new File(m_workingDirectory, "output.log"));
		builder.redirectErrorStream();

		CommandExecution exec = builder.build();
		exec.run();
		
		// CommandExecution이 종료되었을 때 수행하는 작업을
		// CommandExecution.whenFinishe() callback을 통해 수행할 수 있지만,
		// 이 경우 callback 수행 과정에서 발생되는 예외를 제대로 처리할 수 없어서
		// 사용할 수 없었음.
		
		if ( s_logger.isInfoEnabled() ) {
			s_logger.info("ProgramOperationProvider terminates");
		}
		AsyncResult<Void> result = exec.poll();
		
		// 성공적으로 종료한 경우에는 output port를 갱신시킨다.
		if ( result.isCompleted() ) {
    		Map<String,Variable> commandVariables = exec.getVariableMap();
    		
    		// 입출력 연산 변수에 해당하는 CommandVariable 값을 읽어 출력 연산 변수 값을 갱신시킨다.
    		for ( OperationVariable inout: inoutputVars ) {
    			updateOperationVariable(inout, commandVariables);
    		}
    		
    		// 출력 연산 변수에 해당하는 CommandVariable 값을 읽어 출력 연산 변수 값을 갱신시킨다.
    		for ( OperationVariable out: outputVars ) {
    			updateOperationVariable(out, commandVariables);
    		}
		}
		
		// Task 수행을 위해 생성된 모든 임시 파일들을 삭제시킨다.
		exec.close();
	}

	private FileVariable toCommandVariable(OperationVariable opVar, File workingDir) throws TaskException {
		SubmodelElement data = opVar.getValue();
		String name = data.getIdShort();
		
		File file = null;
		try {
			if ( data instanceof Property prop ) {
				// PropertyValue인 경우, 바로 JSON으로 출력하면 double-quote가 추가되기 때문에
				// 이를 막기 위해 값을 직접 저장한다.
				file = new File(workingDir, name);
				IOUtils.toFile(prop.getValue(), StandardCharsets.UTF_8, file);
				return new FileVariable(name, file);
			}
			else if ( data instanceof org.eclipse.digitaltwin.aas4j.v3.model.File aasFile ) {
				GetFileByPathRequest req = new GetFileByPathRequest();
				req.setPath(aasFile.getValue());
				GetFileByPathResponse resp = m_svcContext.execute(req);
				
				file = new File(workingDir, aasFile.getValue());
				IOUtils.toFile(resp.getPayload().getContent(), file);
				return new FileVariable(name, file);
			}
			else {
				throw new IllegalArgumentException("Unsupported Port data type: " + data.getClass());
			}
		}
		catch ( IOException e ) {
			throw new InternalException("Failed to write value to file: name=" + name
										+ ", path=" + file.getAbsolutePath(), e);
		}
	}
	
	private void updateOperationVariable(OperationVariable opVar, Map<String,Variable> commandVarMap)
		throws IOException {
		SubmodelElement oldElement = opVar.getValue();
		String id = oldElement.getIdShort();
		
		FOption.acceptOrThrow(commandVarMap.get(id), cmdVar -> {
			// 해당 command variable의 Json 문자열 값을 읽고 JsonNode로 파싱시킨다.
			// 이때 command variable의 Json 문자열을 SubmodelElementValue의 값을 갖는다.
			JsonNode newJsonNode = MDTModelSerDe.readJsonNode(cmdVar.getValue());
			
			// 얻은 JsonNode로 기존 operation variable 값을 갱신시킨다.
			SubmodelElement newElement = ElementValues.update(oldElement, newJsonNode);
			
			// OperationVariable을 변경시킨다.
			opVar.setValue(newElement);
		});
	}
}
