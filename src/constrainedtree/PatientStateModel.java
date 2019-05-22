/*
 * Copyright (C) 2019. Tim Vaughan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
