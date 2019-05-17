package constrainedtree;

import beast.core.BEASTObject;
import beast.core.Input;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PatientStateModel extends BEASTObject {

    public Input<String> csvFileNameInput = new Input<>("csvFileName",
            "Name of CSV data file for input.",
            Input.Validate.REQUIRED);

    Map<String, Map<Double, Set<String>>> patientStateMap;

    @Override
    public void initAndValidate() {

        Map<String, List<PatientStateChange>> patientStateChanges;

    }

    public Set<String> getPatientState(double time) {

    }
}
