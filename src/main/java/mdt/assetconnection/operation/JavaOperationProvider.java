package mdt.assetconnection.operation;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.InternalException;
import utils.Throwables;
import utils.Utilities;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.model.IdShortPath;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class JavaOperationProvider implements OperationProvider {
	private static final Logger s_logger = LoggerFactory.getLogger(ProgramOperationProvider.class);
	
//	private final ServiceContext m_svcContext;
//	private final Reference m_operationRef;
	private final JavaOperationProviderConfig m_config;
	private final OperationProvider m_opPrvd;
	
	JavaOperationProvider(ServiceContext serviceContext, Reference operationRef,
							JavaOperationProviderConfig config) {
//		m_svcContext = serviceContext;
//		m_operationRef = operationRef;
		m_config = config;

		try {
			String opClsName = m_config.getOperationClassName();
			@SuppressWarnings("unchecked")
			Class<? extends OperationProvider> opCls = (Class<? extends OperationProvider>)Class.forName(opClsName);
			m_opPrvd = Utilities.newInstance(opCls);
			
			if ( s_logger.isInfoEnabled() ) {
				IdShortPath idShortPath = IdShortPath.fromReference(operationRef);
				s_logger.info("Operation: Java ({}), op-ref={}", m_config.getOperationClassName(), idShortPath);
			}
		}
		catch ( Exception e ) {
			Throwable cause = Throwables.unwrapThrowable(e);
			String msg = String.format("Failed to load JavaOperationProvider: class=%s", m_config.getOperationClassName());
			s_logger.info("{}, cause={}", msg, cause);
			throw new InternalException(msg, cause);
		}
	}
	
	@Override
	public void invokeSync(OperationVariable[] inputVars, OperationVariable[] inoutputVars,
							OperationVariable[] outputVars) throws Exception {
		m_opPrvd.invokeSync(inputVars, inoutputVars, outputVars);
	}
}
