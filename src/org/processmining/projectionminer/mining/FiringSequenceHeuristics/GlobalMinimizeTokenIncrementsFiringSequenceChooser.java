package org.processmining.projectionminer.mining.FiringSequenceHeuristics;

import org.processmining.projectionminer.utils.TokenSequences.TokenSequenceCalculator;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

import java.util.*;

public class GlobalMinimizeTokenIncrementsFiringSequenceChooser extends AbstractFiringSequenceChooser {
    private ArrayList<Place> oneLoopingPlaces;
    private Marking initMarking;

    public void init(Collection<Place> places, Marking initMarking) {
        oneLoopingPlaces = new ArrayList<>(places);
        this.initMarking = initMarking;
    }

    @Override
    public LinkedList<Transition> chooseTransition(Collection<LinkedList<Transition>> validSequences, boolean tieBreak) {
        HashMap<LinkedList<Transition>, Integer> firingSequenceToScore = new HashMap<>();

        for (LinkedList<Transition> validSequence : validSequences) {
            int sum = 0;
            for (Place oneLoopingPlace : oneLoopingPlaces) {
                LinkedList<Integer> tokenSequence = TokenSequenceCalculator.getInstance().calculateTokenSequence(validSequence, oneLoopingPlace, initMarking.contains(oneLoopingPlace));
                int lastTokenNumber = 0;

                Iterator<Integer> iterator = tokenSequence.iterator();
                while (iterator.hasNext()) {
                    Integer next = iterator.next();
                    sum = sum + ((next > lastTokenNumber) ? 1 : 0);

                    lastTokenNumber = next;
                }

                sum = sum + tokenSequence.stream().mapToInt(Integer::intValue).sum();
            }

            firingSequenceToScore.put(validSequence, sum);
        }

        if (tieBreak) {
            return validSequences.stream().min(Comparator.comparingInt(firingSequenceToScore::get)).get();
        } else {
            Integer minValue = firingSequenceToScore.values().stream().min(Integer::compare).get();
            LinkedList<LinkedList<Transition>> validSequencesInTie = new LinkedList<>();
            for (Map.Entry<LinkedList<Transition>, Integer> entry : firingSequenceToScore.entrySet()) {
                if (entry.getValue() == minValue) {
                    validSequencesInTie.add(entry.getKey());
                }
            }

            LinkedList<Transition> result;
            if (validSequencesInTie.size() == 1) {
                result = validSequencesInTie.getFirst();
            } else {
                GlobalMinimizeTokensFiringSequenceChooser tieBreaker = new GlobalMinimizeTokensFiringSequenceChooser();
                tieBreaker.init(oneLoopingPlaces, initMarking);
                result = tieBreaker.chooseTransition(validSequencesInTie, true);
            }

            return result;
        }
    }

    @Override
    public String toString() {
        return "Minimize all token increments in looping places.";
    }
}
