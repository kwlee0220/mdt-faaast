package mdt.assetconnection.operation;

import java.util.Map;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.KeyValue;
import utils.stream.FStream;

import mdt.client.operation.OperationUtils;
import mdt.task.builtin.HttpTask;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.model.IdShortPath;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
class HttpOperationProvider implements OperationProvider {
	private static final Logger s_logger = LoggerFactory.getLogger(HttpOperationProvider.class);
	
	@SuppressWarnings("unused")
	private final ServiceContext m_svcContext;
	private final HttpOperationProviderConfig m_config;
	
	HttpOperationProvider(ServiceContext context, Reference operationRef, HttpOperationProviderConfig config) {
		m_svcContext = context;
		m_config = config;
		
		if ( s_logger.isInfoEnabled() ) {
			IdShortPath idShortPath = IdShortPath.fromReference(operationRef);
			s_logger.info("Operation: Http ({}), id={}, poll={}, op-ref={}",
							m_config.getEndpoint(), m_config.getOpId(), m_config.getPollInterval(), idShortPath);
		}
	}
	
	@Override
	public void invokeSync(OperationVariable[] inputVars, OperationVariable[] inoutputVars,
							OperationVariable[] outputVars) throws Exception {
		HttpTask.Builder builder = HttpTask.builder()
											.serverEndpoint(m_config.getEndpoint())
											.operationId(m_config.getOpId())
											.pollInterval(m_config.getPollInterval())
											.timeout(m_config.getTimeout())
											.sync(true);
		FStream.of(inputVars).map(OperationUtils::toParameter).forEach(builder::addInputParameter);
		FStream.of(inoutputVars).map(OperationUtils::toParameter).forEach(builder::addInputParameter);
		FStream.of(outputVars).map(OperationUtils::toParameter).forEach(builder::addOutputParameter);
		FStream.of(inoutputVars).map(OperationUtils::toParameter).forEach(builder::addOutputParameter);
		HttpTask task = builder.build();
		
		Map<String,SubmodelElement> result = task.run();
		if ( s_logger.isInfoEnabled() ) {
			s_logger.info("HttpOperation terminates: result=" + result);
		}
		
		updateOutputVariables(result, inoutputVars, outputVars);
	}
	
//	public void invokeAsync(OperationVariable[] inputVars,
//							OperationVariable[] inoutputVars,
//							OperationVariable[] outputVars,
//							BiConsumer<OperationVariable[], OperationVariable[]> callbackSuccess,
//							Consumer<Throwable> callbackFailure) throws Exception {
//		HttpTask task = new HttpTask(m_config.getEndpoint(), m_config.getOpId(), m_config.getPollInterval(),
//									false, null);
//		
//		FStream.of(inputVars).map(OperationUtils::toParameter).forEach(task::addOrReplaceInputParameter);
//		FStream.of(inoutputVars).map(OperationUtils::toParameter).forEach(task::addOrReplaceOutputParameter);
//		FStream.of(outputVars).map(OperationUtils::toParameter).forEach(task::addOrReplaceOutputParameter);
//		
//		task.whenFinished(result -> {
//			if ( result.isSuccessful() ) {
//				updateOutputVariables(inoutputVars, outputVars, result.getUnchecked());
//				Try.run(() -> callbackSuccess.accept(inoutputVars, outputVars));
//			}
//			else if ( result.isFailed() ) {
//				Throwable cause = Throwables.unwrapThrowable(result.getCause());
//				if ( cause instanceof RESTfulRemoteException re ) {
//					cause = re.getCause();
//				}
//				else if ( cause instanceof RESTfulIOException re ) {
//					cause = re.getCause();
//				}
//				if ( cause instanceof TimeoutException
//					|| cause instanceof TaskException
//					|| cause instanceof CancellationException
//					|| cause instanceof ExecutionException
//					|| cause instanceof InterruptedException ) {
//					cause = new ExecutionException(cause);
//				}
//				callbackFailure.accept(cause);
//			}
//			else if ( result.isNone() ) {
//				callbackFailure.accept(new CancellationException());
//			}
//		});
//
//		if ( s_logger.isInfoEnabled() ) {
//			s_logger.info("starting HttpOperation: task=" + task);
//		}
//		task.start();
//	}
	
	private void updateOutputVariables(Map<String,SubmodelElement> result, OperationVariable[] inoutputVars,
										OperationVariable[] outputVars) {
		FStream.of(inoutputVars)
				.concatWith(FStream.of(outputVars))
				.innerJoin(FStream.from(result), opv -> opv.getValue().getIdShort(), KeyValue::key)
				.forEach(match -> {
					OperationVariable outVar = match._1;
					KeyValue<String,SubmodelElement> outValue = match._2;

					outVar.setValue(outValue.value());
				});
	}
}
