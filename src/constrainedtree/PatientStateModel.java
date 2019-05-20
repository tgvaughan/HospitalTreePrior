package constrainedtree;

import beast.core.BEASTObject;
import beast.core.Input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PatientStateModel extends BEASTObject {

    public Input<String> stateLabelInput = new Input<>("stateLabel",
            "Label for this state element",
            Input.Validate.REQUIRED);

    public Input<List<PatientStateModel>> childModelsInput = new Input<>(
            "subModel",
            "One or more patient sub-models.",
            new ArrayList<>());

    private String stateName, stateValue;

    private List<PatientStateModel> childModels;

    public PatientStateModel() { }

    public PatientStateModel(String stateLabel, PatientStateModel ... childModels) {

        this.stateName = stateLabel;
        this.childModels = Arrays.asList(childModels);
    }

    @Override
    public void initAndValidate() {
        stateName = stateLabelInput.get();
        childModels = childModelsInput.get();
    }

    public PatientStateModel copy() {
        PatientStateModel newModel = new PatientStateModel();
        newModel.stateName = stateName;
        newModel.stateValue = stateValue;
        newModel.childModels = new ArrayList<>();

        for (PatientStateModel childModel : childModels)
            newModel.childModels.add(childModel.copy());

        return newModel;
    }

    private boolean setValueRecurse (String stateName, String stateValue) {
        if (this.stateName.equals(stateName)) {
            this.stateValue = stateValue;
            return true;
        }

        for (PatientStateModel childModel : childModels) {
            if (childModel.setValueRecurse(stateName, stateValue))
                return true;
        }

        return false;
    }

    public PatientStateModel setValue(String stateName, String stateValue) {
        if (setValueRecurse(stateName, stateValue))
            return this;

        throw new IllegalArgumentException("State named " + stateName + " not found.");
    }
}
