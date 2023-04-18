package org.processmining.projectionminer.discoveryalgorithms;

import org.processmining.projectionminer.utils.Modifiers.PetriNetModifier;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

/**
 * Class for storing results of a discovery algorithm, which also further contains the parameters used for the run of
 * the discovery algorithm.
 */
public class AcceptingPetriNetWrapper {
    private final Object parameters;
    private Petrinet net;
    private Marking initialMarking;
    private Marking finalMarking;

    /**
     * Creates an object all the information, that an accepting Petri net contains.
     *
     * @param net            The Petri net.
     * @param initialMarking The initial marking of the corresponding Petri net.
     * @param finalMarking   The final marking of the corresponding Petri net.
     * @param parameters     The parameters used to discover the Petri net for a discovery algorithm.
     */
    public AcceptingPetriNetWrapper(Petrinet net, Marking initialMarking, Marking finalMarking, Object parameters) {
        this.net = net;
        this.initialMarking = initialMarking;
        this.finalMarking = finalMarking;
        this.parameters = parameters;
    }

    /**
     * Returns the net of the accepting Petri net.
     *
     * @return The Petri net stored.
     */
    public Petrinet getNet() {
        return net;
    }

    /**
     * Returns the initial marking of the accepting Petri net.
     *
     * @return The initial marking of the Petri net stored.
     */
    public Marking getInitialMarking() {
        return initialMarking;
    }

    /**
     * Returns the final marking of the accepting Petri net.
     *
     * @return The final marking of the Petri net stored.
     */
    public Marking getFinalMarking() {
        return finalMarking;
    }

    /**
     * The parameters used for a discovery algorithm to find the accepting Petri net.
     *
     * @return Parameters inputted to a discovery algorithm.
     */
    public Object getParameters() {
        return parameters;
    }

    /**
     * Changes the start and end transition with the label "Start" or "End" or anything similar according to the eST-Miner
     * approach to be silent. To avoid modifications of inputs this class also creates a copy of the Petri net.
     */
    public void polishNet() {
        // Copying the Petri net.
        net = PetriNetModifier.getInstance().copyPetriNet(net);
        initialMarking = PetriNetModifier.getInstance().getLastInitialMarking(initialMarking);
        finalMarking = PetriNetModifier.getInstance().getLastFinalMarking(finalMarking);

        // Changing Start and End transitions to be silent.
        for (Transition transition : net.getTransitions()) {
            if (!transition.isInvisible() && (transition.getLabel().equals("Start") || transition.getLabel().equals("End") ||
                    transition.getLabel().equals("ArtificialStart") || transition.getLabel().equals("ArtificialEnd"))) {
                transition.setInvisible(true);
            }
        }
    }

    /**
     * Replaces the final marking stored for the accepting Petri net.
     *
     * @param newFinalMarking The final marking to replace the actual marking with.
     */
    public void replaceFinalMarking(Marking newFinalMarking) {
        finalMarking = newFinalMarking;
    }
}
