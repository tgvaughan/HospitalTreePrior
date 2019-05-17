package constrainedtree;

import beast.core.BEASTObject;
import beast.core.Input;

import java.util.ArrayList;
import java.util.List;

public class PatientStateModel extends BEASTObject {

    public Input<String> stateLabelInput = new Input<>("stateLabel",
            "Label for this state element",
            Input.Validate.REQUIRED);

    public Input<List<PatientStateModel>> childModelsInput = new Input<>(
            "subModel",
            "One or more patient sub-models.",
            new ArrayList<>());

    private String stateLabel;

    private List<PatientStateModel> childModels;
    private List<String> allStateLabels;

    @Override
    public void initAndValidate() {
        stateLabel = stateLabelInput.get();
        childModels = childModelsInput.get();
    }

    public String getStateLabel() {
        return stateLabel;
    }

    public List<String> getAllStateLabels() {
        if (allStateLabels == null) {
            allStateLabels = new ArrayList<>();
            allStateLabels.add(stateLabel);

            for (PatientStateModel childModel : childModels)
                allStateLabels.addAll(childModel.getAllStateLabels());
        }

        return allStateLabels;
    }
}
