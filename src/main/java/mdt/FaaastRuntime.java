package mdt;

import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import utils.func.Funcs;

import mdt.model.ReferenceUtils;
import mdt.model.ResourceNotFoundException;

import de.fraunhofer.iosb.ilt.faaast.service.ServiceContext;
import de.fraunhofer.iosb.ilt.faaast.service.model.IdShortPath;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.request.submodel.GetSubmodelElementByPathRequest;
import de.fraunhofer.iosb.ilt.faaast.service.util.ReferenceHelper;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class FaaastRuntime {
	private final ServiceContext m_service;
	
	public FaaastRuntime(ServiceContext service) {
		m_service = service;
	}
	
	public Submodel getSubmodelById(String submodelId) {
		return Funcs.findFirst(m_service.getAASEnvironment().getSubmodels(),
								sm -> submodelId.equals(sm.getId()))
					.getOrThrow(() -> new ResourceNotFoundException("Submodel", "id=" + submodelId));
	}
	
	public Submodel getSubmodelByIdShort(String submodeIdShort) {
		return Funcs.findFirst(m_service.getAASEnvironment().getSubmodels(),
								sm -> submodeIdShort.equals(sm.getIdShort()))
					.getOrThrow(() -> new ResourceNotFoundException("Submodel", "idShort=" + submodeIdShort));
	}
	
	public SubmodelElement getSubmodelElementByPath(String submodelId, String path) {
		GetSubmodelElementByPathRequest req = GetSubmodelElementByPathRequest.builder()
																			.submodelId(submodelId)
																			.path(path)
																			.build();
		return m_service.execute(req).getPayload();
	}
	
	public SubmodelElement getSubmodelElementByReference(Reference reference) {
		ReferenceUtils.assertSubmodelElementReference(reference);
		String submodelId = ReferenceHelper.getRoot(reference).getValue();
		String path = IdShortPath.fromReference(reference).toString();
		
		return getSubmodelElementByPath(submodelId, path);
	}
}
