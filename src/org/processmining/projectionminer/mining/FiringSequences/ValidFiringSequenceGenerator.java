package org.processmining.projectionminer.mining.FiringSequences;


import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.projectionminer.utils.Modifiers.PetriNetModifier;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetImpl;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.utils.GraphIterator;
import org.processmining.models.semantics.petrinet.Marking;

import java.util.*;

public class ValidFiringSequenceGenerator {
    private final HashMap<String, ArrayList<Transition>> activityToTransitions;
    private final XEventClassifier eventClassifier;
    private final Marking initMarking;
    private final Transition finalTransition;
    private final int maxDepth;
    private final HashMap<Transition, Transition> inverseTransitionToTransition;
    private final boolean greedy;

    /**
     * A generator to create firing sequences of a trace in a Petri net.
     *
     * @param acceptingPetriNet The net that we focus for the replay on.
     */
    public ValidFiringSequenceGenerator(AcceptingPetriNet acceptingPetriNet, int maxDepth, boolean greedy) {
        activityToTransitions = new HashMap<>();
        this.eventClassifier = new XEventNameClassifier();
        this.maxDepth = maxDepth;
        this.greedy = greedy;

        AcceptingPetriNetImpl netCopy = PetriNetModifier.getInstance().copyAcceptingPetriNet(acceptingPetriNet);

        HashMap<Transition, Transition> transitionToTransition = PetriNetModifier.getInstance().getLastTransitionToTransition();
        inverseTransitionToTransition = new HashMap<>();
        for (Map.Entry<Transition, Transition> entry : transitionToTransition.entrySet()) {
            inverseTransitionToTransition.put(entry.getValue(), entry.getKey());
        }

        initMarking = netCopy.getInitialMarking();

        finalTransition = netCopy.getNet().addTransition("Valid-End-Helper");
        finalTransition.setInvisible(true);
        for (Place p_final : netCopy.getFinalMarkings().iterator().next()) {
            netCopy.getNet().addArc(p_final, finalTransition);
        }

        for (Transition transition : netCopy.getNet().getTransitions()) {
            if (!activityToTransitions.containsKey(transition.getLabel())) {
                activityToTransitions.put(transition.getLabel(), new ArrayList<>());
            }

            activityToTransitions.get(transition.getLabel()).add(transition);
        }
    }

    /**
     * Creates (not necessarily) all possible firing sequences within a Petri net for a given trace.
     *
     * @param trace A trace to be replayed.
     * @return Possible firing sequences in the Petri net for the trace.
     */
    public Set<LinkedList<Transition>> findFiringSequences(XTrace trace) {
        HashMap<LinkedList<Transition>, Marking> currentMarking = new HashMap<>();
        LinkedList<Transition> firingSequenceStart = new LinkedList<>();
//        firingSequenceStart.add(startTransition);
        currentMarking.put(firingSequenceStart, initMarking);

        for (XEvent event : trace) {
            String activity = eventClassifier.getClassIdentity(event);
            HashMap<LinkedList<Transition>, Marking> newCurrentMarking = new HashMap<>();

            if (activityToTransitions.get(activity).size() > 0) {
                for (Map.Entry<LinkedList<Transition>, Marking> firingSequence : currentMarking.entrySet()) {
                    newCurrentMarking.putAll(findFollowingFiringSequences(firingSequence, new ArrayList<>(activityToTransitions.get(activity))));
                }
            } else {
                throw new RuntimeException("No suitable transitions found in the FiringSequenceGenerator.");
            }

            currentMarking = newCurrentMarking;
        }

        HashMap<LinkedList<Transition>, Marking> newCurrentMarking = new HashMap<>();
        for (Map.Entry<LinkedList<Transition>, Marking> firingSequence : currentMarking.entrySet()) {
            newCurrentMarking.putAll(firingSequenceBetweenTransitions(firingSequence, finalTransition));
        }
        currentMarking = newCurrentMarking;


        Set<LinkedList<Transition>> result = new HashSet<>();

        for (LinkedList<Transition> list : currentMarking.keySet()) {
            list.removeLast();

            LinkedList<Transition> newList = new LinkedList<>();
            for (Transition t : list) {
                newList.add(inverseTransitionToTransition.get(t));
            }

            result.add(newList);
        }


        return result;
    }

    public HashMap<LinkedList<Transition>, Marking> findFollowingFiringSequences(Map.Entry<LinkedList<Transition>, Marking> firingSequence,
                                                                                 ArrayList<Transition> transitionsWithNextLabel) {
        HashSet<Transition> visibleSuccessors = new HashSet<>();
        for (Place place : firingSequence.getValue()) {
            visibleSuccessors.addAll(getVisibleSuccessors(place));
        }

        ArrayList<Transition> toBeRemoved = new ArrayList<>();
        for (Transition transition : transitionsWithNextLabel) {
            if (!visibleSuccessors.contains(transition)) {
                toBeRemoved.add(transition);
            }
        }
        transitionsWithNextLabel.removeAll(toBeRemoved);

        HashMap<LinkedList<Transition>, Marking> result = new HashMap<>();
        for (Transition transition : transitionsWithNextLabel) {
            result.putAll(firingSequenceBetweenTransitions(firingSequence, transition));
        }

        return result;
    }

    private HashMap<LinkedList<Transition>, Marking> firingSequenceBetweenTransitions(Map.Entry<LinkedList<Transition>, Marking> firingSequence, Transition transition) {
        HashMap<LinkedList<Transition>, Marking> finishedSequences = new HashMap<>();
        HashMap<LinkedList<Transition>, Marking> unfinishedSequences = new HashMap<>();
        LinkedList<Transition> initialList = new LinkedList<>();
        initialList.add(transition);
        unfinishedSequences.put(initialList, new Marking(getIngoingPlaces(transition)));

        for (int i = 0; i <= maxDepth; i++) {
            Iterator<Map.Entry<LinkedList<Transition>, Marking>> iterator = unfinishedSequences.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<LinkedList<Transition>, Marking> entry = iterator.next();
                if (firingSequence.getValue().containsAll(entry.getValue())) {
                    LinkedList<Transition> resultList = new LinkedList<>(firingSequence.getKey());
                    resultList.addAll(entry.getKey());

                    Marking newMarking = new Marking(firingSequence.getValue());
                    for (Transition t_fire : entry.getKey()) {
                        Marking places1 = new Marking(getIngoingPlaces(t_fire));
                        newMarking.minus(places1);
                        Marking places = new Marking(getOutgoingPlaces(t_fire));
                        newMarking.addAll(places);
                    }

                    finishedSequences.put(resultList, newMarking);
                    iterator.remove();
                }
            }

            if (unfinishedSequences.isEmpty() || (greedy && !finishedSequences.isEmpty())) {
                break;
            }

            HashMap<LinkedList<Transition>, Marking> newUnfinishedSequences = new HashMap<>();
            for (Map.Entry<LinkedList<Transition>, Marking> entry : unfinishedSequences.entrySet()) {
                LinkedList<Transition> actualSequence = entry.getKey();
                Marking actualMarking = entry.getValue();

                HashSet<Transition> invisiblePredecessors = new HashSet<>();
                for (Place place : actualMarking) {
                    invisiblePredecessors.addAll(getInvisiblePredecessors(place));
                }

                for (Transition invisiblePredecessor : invisiblePredecessors) {
                    if (!actualSequence.contains(invisiblePredecessor)) {
                        LinkedList<Transition> appendedSequence = new LinkedList<>(actualSequence);
                        appendedSequence.addFirst(invisiblePredecessor);

                        if (!newUnfinishedSequences.containsKey(appendedSequence)) {
                            Marking appendedMarking = new Marking(actualMarking);
                            appendedMarking.minus(new Marking(getOutgoingPlaces(invisiblePredecessor)));
                            appendedMarking.addAll(new Marking(getIngoingPlaces(invisiblePredecessor)));

                            newUnfinishedSequences.put(appendedSequence, appendedMarking);
                        }
                    }

                }
            }

            unfinishedSequences = newUnfinishedSequences;
        }

        return finishedSequences;
    }

    public Collection<Place> getIngoingPlaces(PetrinetNode transition) {
        HashSet<Place> ingoingPlaces = new HashSet<>();
        for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : transition.getGraph().getInEdges(transition)) {
            ingoingPlaces.add((Place) inEdge.getSource());
        }

        return ingoingPlaces;
    }

    public Collection<Place> getOutgoingPlaces(PetrinetNode transition) {
        HashSet<Place> outgoingPlaces = new HashSet<>();
        for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : transition.getGraph().getOutEdges(transition)) {
            outgoingPlaces.add((Place) outEdge.getTarget());
        }

        return outgoingPlaces;
    }


    public Collection<Transition> getVisibleSuccessors(PetrinetNode place) {
        final GraphIterator.NodeAcceptor<PetrinetNode> nodeAcceptor = (node, depth) -> node instanceof Transition && !((Transition) node).isInvisible();
        Collection<PetrinetNode> transitions = GraphIterator.getDepthFirstSuccessors(place, place.getGraph(), (edge, depth) -> !nodeAcceptor.acceptNode(edge.getSource(), depth), nodeAcceptor);
        return Arrays.asList(transitions.toArray(new Transition[0]));
    }

    public HashSet<Transition> getInvisiblePredecessors(Place place) {
        HashSet<Transition> ingoingSilentTransitions = new HashSet<>();
        for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : place.getGraph().getInEdges(place)) {
            Transition ingoingTransition = (Transition) inEdge.getSource();
            if (ingoingTransition.isInvisible()) {
                ingoingSilentTransitions.add(ingoingTransition);
            }
        }

        return ingoingSilentTransitions;
    }

}
