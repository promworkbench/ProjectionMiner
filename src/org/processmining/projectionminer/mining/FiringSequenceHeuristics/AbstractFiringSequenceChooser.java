package org.processmining.projectionminer.mining.FiringSequenceHeuristics;

import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

import java.util.Collection;
import java.util.LinkedList;

public abstract class AbstractFiringSequenceChooser {

    public abstract void init(Collection<Place> places, Marking initMarking);

    public abstract LinkedList<Transition> chooseTransition(Collection<LinkedList<Transition>> validSequences, boolean tieBreak);

    public abstract String toString();
}
