package org.processmining.projectionminer.mining.FiringSequenceHeuristics;

import org.processmining.projectionminer.utils.TokenSequences.TokenSequenceCalculator;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

import java.util.*;
import java.util.stream.Collectors;

public class AvoidFewOneLoopsFiringSequenceChooser extends AbstractFiringSequenceChooser {
    private ArrayList<Place> oneLoopingPlaces;
    private Marking initMarking;
    private HashMap<Place, Integer> malusOfPlaces;

    public void init(Collection<Place> places, Marking initMarking) {
        oneLoopingPlaces = new ArrayList<>(places);
        this.initMarking = initMarking;
        malusOfPlaces = new HashMap<>();

        int maxCount = 0;
        HashMap<Place, Integer> numbOfOneLoopingTransitions = new HashMap<>();
        for (Place p : places) {
            int count = 0;
            ArrayList<PetrinetNode> ingoingTransitions = new ArrayList<>();
            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : p.getGraph().getInEdges(p)) {
                ingoingTransitions.add(inEdge.getSource());
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : p.getGraph().getOutEdges(p)) {
                if (ingoingTransitions.contains(outEdge.getTarget())) {
                    count++;
                }
            }

            numbOfOneLoopingTransitions.put(p, count);
            if (count > maxCount) {
                maxCount = count;
            }
        }

        for (Place p : oneLoopingPlaces) {
            malusOfPlaces.put(p, 1 << ((maxCount - numbOfOneLoopingTransitions.get(p)) << 1));
        }

    }

    @Override
    public LinkedList<Transition> chooseTransition(Collection<LinkedList<Transition>> validSequences, boolean tieBreaker) {
        HashMap<LinkedList<Transition>, Integer> firingSequenceToScore = new HashMap<>();

        for (LinkedList<Transition> validSequence : validSequences) {
            int sum = 0;

            for (Place oneLoopingPlace : oneLoopingPlaces) {
                LinkedList<Integer> tokenSequence = TokenSequenceCalculator.getInstance().calculateTokenSequence(validSequence, oneLoopingPlace, initMarking.contains(oneLoopingPlace));
                if (tokenSequence.stream().mapToInt(Integer::intValue).sum() != 0) {
                    sum = sum + malusOfPlaces.get(oneLoopingPlace);
                }
            }

            firingSequenceToScore.put(validSequence, sum);
        }

        Optional<Integer> minScore = firingSequenceToScore.values().stream().min(Integer::compare);
        List<LinkedList<Transition>> minimalSequences = validSequences.stream().filter(elem -> Objects.equals(firingSequenceToScore.get(elem), minScore.get())).collect(Collectors.toList());

        LinkedList<Transition> result;
        if (minimalSequences.size() == 1) {
            result = minimalSequences.get(0);
        } else {
            GlobalMinimizeTokenIncrementsFiringSequenceChooser helpChooser = new GlobalMinimizeTokenIncrementsFiringSequenceChooser();
            helpChooser.init(oneLoopingPlaces, initMarking);
            result = helpChooser.chooseTransition(minimalSequences, true);
        }

        return result;
    }

    @Override
    public String toString() {
        return "Avoid tokens completely in places with few one-loops.";
    }
}
