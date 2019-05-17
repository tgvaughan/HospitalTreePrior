package constrainedtree;

import beast.core.Input;
import beast.core.parameter.BooleanParameter;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.TreeDistribution;
import beast.evolution.tree.coalescent.PopulationFunction;

import java.util.ArrayList;
import java.util.List;

public class ConstrainedCoalescent extends TreeDistribution {

    public Input<BooleanParameter> nodeOrientationsInput = new Input<>(
            "nodeOrientations",
            "Orientations of nodes in transmission tree.",
            Input.Validate.REQUIRED);

    public Input<List<TraitSet>> classMembershipsInput = new Input<>(
            "classMemberships",
            "Trait set defining when each taxon was a member of a " +
                    "particular class.",
            new ArrayList<>());

    public Input<PopulationFunction> populationFunctionInput = new Input<>(
            "populationFunction",
            "Population function: gives base coalescent rate.",
            Input.Validate.REQUIRED);

    @Override
    public void initAndValidate() {

    }

    @Override
    public double calculateLogP() {
        logP = 0.0;




        return logP;
    }
}
