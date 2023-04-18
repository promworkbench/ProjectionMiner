package org.processmining.projectionminer.mining.Projectors;

import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.HashMap;
import java.util.LinkedList;

public abstract class AbstractProjector {
    public abstract void projectAndAddTraces(XLog log, Place projectionPlace, LinkedList<Integer> tokenSequence, LinkedList<Transition> firingSequence);

    public abstract void apply(XLog log, Place projectionPlace, HashMap<LinkedList<Transition>, LinkedList<Integer>> chosenTokenAndFiringSequences);

    public boolean checkIfIngoingTransitionOfPlace(Place place, Transition transition) {
        for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : place.getGraph().getInEdges(place)) {
            if (inEdge.getSource().equals(transition)) {
                return true;
            }
        }

        return false;
    }

    public boolean checkIfOutgoingTransitionOfPlace(Place place, Transition transition) {
        for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : place.getGraph().getOutEdges(place)) {
            if (outEdge.getTarget().equals(transition)) {
                return true;
            }
        }

        return false;

    }
}
