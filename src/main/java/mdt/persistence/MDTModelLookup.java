package mdt.persistence;

import java.util.List;

import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import utils.stream.FStream;

import mdt.model.ResourceNotFoundException;
import mdt.model.sm.SubmodelUtils;
import mdt.model.sm.data.DataInfo;
import mdt.model.sm.data.DefaultData;

import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationInitializationException;
import de.fraunhofer.iosb.ilt.faaast.service.model.IdShortPath;
import de.fraunhofer.iosb.ilt.faaast.service.model.SubmodelElementIdentifier;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.modifier.QueryModifier;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.paging.PagingInfo;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.Persistence;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MDTModelLookup {
	private static final Logger s_logger = LoggerFactory.getLogger(MDTModelLookup.class);
	
	private static MDTModelLookup s_instance;
	
	private List<Submodel> m_submodels;
	private Submodel m_dataSubmodel;
	private BiMap<String,String> m_submodelIdToSubmodelIdShortMap;
	private BiMap<String,String> m_submodelIdShortToSubmodelIdMap;
	private BiMap<String,String> m_pathToParameterMap;
	private BiMap<String,String> m_parameterToPathMap;
	
	public static MDTModelLookup getInstanceOrCreate(Persistence perst) throws ConfigurationInitializationException {
		if ( s_instance == null ) {
			try {
				s_instance = new MDTModelLookup(perst);
			}
			catch ( ResourceNotFoundException e ) {
				throw new ConfigurationInitializationException(e);
			}
		}
		
		return s_instance;
	}
	
	public static MDTModelLookup getInstance() {
		if ( s_instance == null ) {
			throw new IllegalStateException("MDTModelLookup is not initialized");
		}

		return s_instance;
	}
	
	public List<Submodel> getSubmodelAll() {
		return m_submodels;
	}
	
	public Submodel getDataSubmodel() {
		return m_dataSubmodel;
	}

	/**
	 * 주어진 SubmodelElementIdentifier에 해당하는 parameter 식별자를 반환한다.
	 *
	 * @param idShortPath	SubmodelElement의 idShort 경로
	 * @return
	 */
	public String getParameterId(SubmodelElementIdentifier identifier) {
		Preconditions.checkState(identifier != null, "SubmodelElementIdentifier is null");
		
		if ( m_dataSubmodel.getId().equals(identifier.getSubmodelId()) ) {
			return m_pathToParameterMap.get(identifier.getIdShortPath().toString());
		}
		else {
			return null;
		}
	}
	
	/**
	 * 주어진 SubmodelElement의 idShort 경로에 해당하는 parameter 식별자를 반환한다.
	 *
	 * @param idShortPath	SubmodelElement의 idShort 경로
	 * @return
	 */
	public String getParameterId(String idShortPath) {
		Preconditions.checkState(idShortPath != null, "idShortPath is null");
		
		return m_pathToParameterMap.get(idShortPath);
	}
	
	public String getIdShortPath(Reference reference) {
		return IdShortPath.fromReference(reference).toString();
	}
	
	public IdShortPath toIdShortPath(String path) {
		return IdShortPath.builder().path(path).build();
	}
	
	public String getSubmodelIdFromSubmodelIdShort(String submodelIdShort) {
		Preconditions.checkState(submodelIdShort != null, "submodelIdShort is null");

		return m_submodelIdShortToSubmodelIdMap.get(submodelIdShort);
	}
	
	public String getSubmodelIdShortFromSubmodelId(String submodelId) {
		Preconditions.checkState(submodelId != null, "submodelId is null");

		return m_submodelIdToSubmodelIdShortMap.get(submodelId);
	}
	
	private final String EQ_FORMAT = "DataInfo.Equipment.EquipmentParameterValues[%d].ParameterValue";
	private final String OP_FORMAT = "DataInfo.Operation.OperationParameterValues[%d].ParameterValue";

	private MDTModelLookup(Persistence perst) throws ResourceNotFoundException {
		m_submodels = perst.getAllSubmodels(QueryModifier.DEFAULT, PagingInfo.ALL).getContent();
		m_dataSubmodel = FStream.from(m_submodels)
								.findFirst(SubmodelUtils::isDataSubmodel)
								.getOrThrow(() -> new ResourceNotFoundException("Data Submodel"));
		
		m_submodelIdToSubmodelIdShortMap = HashBiMap.create();
		FStream.from(m_submodels)
				.forEach(sm -> m_submodelIdToSubmodelIdShortMap.put(sm.getId(), sm.getIdShort()));
		m_submodelIdShortToSubmodelIdMap = m_submodelIdToSubmodelIdShortMap.inverse();
		
		m_pathToParameterMap = HashBiMap.create();
		m_parameterToPathMap = m_pathToParameterMap.inverse();
		
		DefaultData data = new DefaultData();
		data.updateFromAasModel(m_dataSubmodel);
		DataInfo dataInfo = data.getDataInfo();
		if ( dataInfo.isEquipment() ) {
			FStream.from(dataInfo.getEquipment().getParameterValueList())
					.zipWithIndex()
					.forEach(idxed -> {
                        String path = String.format(EQ_FORMAT, idxed.index());
                        String paramId = idxed.value().getParameterId();
                        m_pathToParameterMap.put(path, paramId);
                    });
		}
		else if ( dataInfo.isOperation() ) {
			FStream.from(dataInfo.getOperation().getParameterValueList())
					.zipWithIndex()
					.forEach(idxed -> {
                        String path = String.format(OP_FORMAT, idxed.index());
                        String paramId = idxed.value().getParameterId();
                        m_pathToParameterMap.put(path, paramId);
                    });
		}
		else {
			throw new IllegalStateException("Invalid DataInfo: " + data.getDataInfo());
		}
	}
}
