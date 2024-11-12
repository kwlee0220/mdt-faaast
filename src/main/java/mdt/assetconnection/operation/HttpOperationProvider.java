package mdt.assetconnection.operation;

import java.util.List;
import java.util.Map;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.KeyValue;
import utils.func.Try;
import utils.stream.FStream;

import mdt.client.operation.OperationUtils;
import mdt.model.MDTModelSerDe;
import mdt.model.sm.value.ElementValues;
import mdt.task.Parameter;
import mdt.task.builtin.HttpTask;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
class HttpOperationProvider implements OperationProvider {
	private static final Logger s_logger = LoggerFactory.getLogger(HttpOperationProvider.class);
	
	@SuppressWarnings("unused")
	private final ServiceContext m_svcContext;
	private final HttpOperationProviderConfig m_config;
	
	HttpOperationProvider(ServiceContext context, HttpOperationProviderConfig config) {
		m_svcContext = context;
		m_config = config;
		
		if ( s_logger.isInfoEnabled() ) {
			s_logger.info("Loading HttpOperation Executor: config={}", m_config);
		}
	}
	
	@Override
	public void invokeSync(OperationVariable[] inputVars, OperationVariable[] inoutputVars,
							OperationVariable[] outputVars) throws Exception {
		HttpTask task = new HttpTask(m_config.getEndpoint(), m_config.getOpId(), m_config.getPollInterval(),
									false, null);
		
		FStream.of(inputVars).map(OperationUtils::toParameter).forEach(task::addOrReplaceInputParameter);
		FStream.of(inoutputVars).map(OperationUtils::toParameter).forEach(task::addOrReplaceOutputParameter);
		FStream.of(outputVars).map(OperationUtils::toParameter).forEach(task::addOrReplaceOutputParameter);
		
		List<Parameter> outputValues = task.run();
		if ( s_logger.isInfoEnabled() ) {
			s_logger.info("HttpOperation terminates: result=" + outputValues);
		}
		
		updateOutputVariables(inoutputVars, outputVars, outputValues);
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
	
	private void updateOutputVariables(OperationVariable[] inoutputVars, OperationVariable[] outputVars,
										List<Parameter> outputValues) {
		Map<String,SubmodelElement> outValues = FStream.from(outputValues)
														.toKeyValueStream(p -> KeyValue.of(p.getName(), p.getElement()))
														.toMap();
		FStream.of(inoutputVars)
				.concatWith(FStream.of(outputVars))
				.forEach(var -> {
					SubmodelElement oldSme = var.getValue();
					SubmodelElement outValue = outValues.get(oldSme.getIdShort());
					if ( outValue != null ) {
						try {
							ElementValues.update(oldSme, ElementValues.getValue(outValue));
						}
						catch ( Throwable e1 ) {
							String value = Try.get(() -> MDTModelSerDe.toJsonString(outValue)).getOrElse("failed");
							s_logger.error("(HttpOperation) failed to update output[{}]: {}",
											var.getValue().getIdShort(), value);
						}
					}
				});
	}
}
