package mdt.assetconnection.operation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.InternalException;
import utils.KeyValue;
import utils.async.CommandExecution;
import utils.async.CommandVariable;
import utils.async.CommandVariable.FileVariable;
import utils.io.FileUtils;
import utils.io.IOUtils;
import utils.stream.FStream;

import mdt.client.operation.OperationUtils;
import mdt.model.AASUtils;
import mdt.model.MDTModelSerDe;
import mdt.model.sm.value.ElementValues;
import mdt.task.builtin.ProgramOperationDescriptor;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.model.IdShortPath;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
class ProgramOperationProvider implements OperationProvider {
	private static final Logger s_logger = LoggerFactory.getLogger(ProgramOperationProvider.class);
	
//	private final ServiceContext m_svcContext;
//	private final Reference m_operationRef;
	private final ProgramOperationProviderConfig m_config;
	private final File m_opDescFile;
	
	private CommandExecution m_cmdExec;
	
	ProgramOperationProvider(ServiceContext serviceContext, Reference operationRef,
								ProgramOperationProviderConfig config) throws IOException {
//		m_svcContext = serviceContext;
//		m_operationRef = operationRef;
		m_config = config;
		
		m_opDescFile = FileUtils.path(FileUtils.getCurrentWorkingDirectory(),
									m_config.getDescriptorFile());
		if ( m_opDescFile.isFile() && m_opDescFile.canRead() ) {
			if ( s_logger.isInfoEnabled() ) {
				IdShortPath idShortPath = IdShortPath.fromReference(operationRef);
				s_logger.info("Operation: Program ({}), op-ref={}", m_opDescFile.getPath(), idShortPath);
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
		
		File workingDir = opDesc.getWorkingDirectory();
		CommandExecution.Builder builder = CommandExecution.builder()
															.addCommand(opDesc.getCommandLine())
															.setWorkingDirectory(workingDir)
															.setTimeout(opDesc.getTimeout());
		
		FStream.of(inputVars)
				.concatWith(FStream.of(inoutputVars))
				.concatWith(FStream.of(outputVars))
				.map(opv -> newCommandVariable(workingDir, opv))
				.forEach(builder::addVariable);

		// stdout/stderr redirection
		builder.redirectErrorStream();
		builder.redictStdoutToFile(new File(workingDir, "output.log"));
		
		m_cmdExec = builder.build();
		try {
			m_cmdExec.run();
			if ( s_logger.isInfoEnabled() ) {
				s_logger.info("ProgramOperationProvider terminates");
			}

			FStream.of(inoutputVars)
					.innerJoin(FStream.from(m_cmdExec.getVariableMap()), opv -> opv.getValue().getIdShort(), KeyValue::key)
					.forEachOrThrow(match -> {
						OperationVariable opv = match._1;
						CommandVariable var = match._2.value();
						
						SubmodelElement old = opv.getValue();
						ElementValues.updateWithRawString(old, var.getValue());
					});

			FStream.of(outputVars)
					.innerJoin(FStream.from(m_cmdExec.getVariableMap()), opv -> opv.getValue().getIdShort(), KeyValue::key)
					.forEachOrThrow(match -> {
						OperationVariable opv = match._1;
						CommandVariable var = match._2.value();
						
						SubmodelElement old = opv.getValue();
						ElementValues.updateWithRawString(old, var.getValue());
					});
		}
		finally {
			m_cmdExec.close();
		}
	}
	
	private FileVariable newCommandVariable(File workingDir, OperationVariable opv) {
		SubmodelElement data = opv.getValue();
		String name = data.getIdShort();
		
		File cvFile = null;
		try {
			if ( data instanceof org.eclipse.digitaltwin.aas4j.v3.model.File aasFile ) {
				String path = aasFile.getValue();
				cvFile = new File(workingDir, path);
				
				String encodedPath = AASUtils.encodeBase64UrlSafe(path);
				File srcFile = FileUtils.path(FileUtils.getCurrentWorkingDirectory(), "files", encodedPath);
				
				FileUtils.copy(srcFile, cvFile);
				
				return new FileVariable(name, cvFile);
			}
			else if ( data instanceof Property ) {
				// PropertyValue인 경우, 바로 JSON으로 출력하면 double-quote가 추가되기 때문에
				// 이를 막기 위해 값을 직접 저장한다.
				cvFile = new File(workingDir, name);
				String extStr = OperationUtils.toExternalString(data);
				IOUtils.toFile(extStr, StandardCharsets.UTF_8, cvFile);
				
				return new FileVariable(name, cvFile);
			}
			else {
				throw new IllegalArgumentException("Unsupported SubmodelElement type: " + data.getClass());
			}
		}
		catch ( IOException e ) {
			throw new InternalException("Failed to write value to file: name=" + name
										+ ", path=" + cvFile.getAbsolutePath(), e);
		}
	}
}
