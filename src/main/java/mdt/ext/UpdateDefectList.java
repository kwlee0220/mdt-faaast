package mdt.ext;

import java.util.List;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import com.google.common.base.Preconditions;

import utils.KeyedValueList;
import utils.stream.FStream;

import mdt.assetconnection.operation.OperationProvider;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class UpdateDefectList implements OperationProvider {
	@Override
	public void invokeSync(OperationVariable[] inputVars, OperationVariable[] inoutputVars,
							OperationVariable[] outputVars) throws Exception {
		KeyedValueList<String, SubmodelElement> inputVarList
					= FStream.of(inputVars)
							.map(OperationVariable::getValue)
							.collect(KeyedValueList.newInstance(SubmodelElement::getIdShort), KeyedValueList::add);
		KeyedValueList<String, SubmodelElement> inoutputVarList
					= FStream.of(inoutputVars)
							.map(OperationVariable::getValue)
							.collect(KeyedValueList.newInstance(SubmodelElement::getIdShort), KeyedValueList::add);
		
		SubmodelElement defectProp = inputVarList.getOfKey("Defect");
		Preconditions.checkArgument(defectProp != null, "Input argument is missing: 'Defect'");
		Preconditions.checkArgument(defectProp instanceof Property, "Argument 'Defect' is not Property");
		String defect = ((Property)inputVarList.getOfKey("Defect")).getValue();
		
		SubmodelElement defectListProp = inoutputVarList.getOfKey("DefectList");
		Preconditions.checkArgument(defectProp != null, "Inoutput argument is missing: 'DefectList'");
		Preconditions.checkArgument(defectProp instanceof Property, "Argument 'DefectList' is not Property");
		String defectList = ((Property)defectListProp).getValue();
		
		String updateDefectList = update(defect, defectList);
		((Property)defectListProp).setValue(updateDefectList);
	}
	
	private String update(String defect, String defectList) {
		int isDefect = FStream.of(defect.split(",")).exists(s -> !s.equals("0")) ? 1 : 0;

		List<Integer> window = FStream.of(defectList.trim().split(","))
										.map(s -> s.equals("1") ? 1 : 0)
										.concatWith(isDefect)
										.takeLast(10);
		return FStream.from(window).join(',');
		
	}
	
	public static void main(String... args) throws Exception {
		String result;
		UpdateDefectList updateList = new UpdateDefectList();
		
		result = updateList.update("0,0,0,0,0,0,0,0,0", "0,0,1");
		Preconditions.checkState(result.equals("0,0,1,0"), result);
		
		result = updateList.update("0,0,0,0,0,0,1,0,0", "0,0,1");
		Preconditions.checkState(result.equals("0,0,1,1"), result);
		
		result = updateList.update("0,0,0,0,0,0,1,0,0", "0,0,1,0,0,0,0,1,1");
		Preconditions.checkState(result.equals("0,0,1,0,0,0,0,1,1,1"), result);
		
		result = updateList.update("0,0,0,0,0,0,0,0,0", "0,0,1,0,0,0,0,1,1,0");
		Preconditions.checkState(result.equals("0,1,0,0,0,0,1,1,0,0"), result);
	}
}
