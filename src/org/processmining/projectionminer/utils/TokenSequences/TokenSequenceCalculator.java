package org.processmining.projectionminer.utils.TokenSequences;

import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.LinkedList;

public class TokenSequenceCalculator {
    private static TokenSequenceCalculator INSTANCE;

    private TokenSequenceCalculator() {
    }


    public static TokenSequenceCalculator getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TokenSequenceCalculator();
        }

        return INSTANCE;
    }

    public LinkedList<Integer> calculateTokenSequence(LinkedList<Transition> firingSequence, Place place, boolean elemOfInitMarking) {
        LinkedList<Integer> tokenSequence = new LinkedList<>();

        if (elemOfInitMarking) {
            tokenSequence.addLast(1);
        } else {
            tokenSequence.addLast(0);
        }

        for (Transition fireTransition : firingSequence) {
            boolean tOutgoingFromP = false;
            boolean tIngoingFromP = false;

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : fireTransition.getGraph().getInEdges(fireTransition)) {
                if (inEdge.getSource().equals(place)) {
                    tOutgoingFromP = true;
                    break;
                }
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : fireTransition.getGraph().getOutEdges(fireTransition)) {
                if (outEdge.getTarget().equals(place)) {
                    tIngoingFromP = true;
                    break;
                }
            }

            int newTokenValue = tokenSequence.getLast() + (tIngoingFromP ? 1 : 0) - (tOutgoingFromP ? 1 : 0);
            tokenSequence.addLast(newTokenValue);
        }

        return tokenSequence;
    }
}
