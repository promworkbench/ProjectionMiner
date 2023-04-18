package org.processmining.projectionminer.mining.Projectors;

import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.HashMap;
import java.util.LinkedList;

public class HeuristicUnsafeProjector extends AbstractProjector {
    private static HeuristicUnsafeProjector INSTANCE;
    private final int traceCounter;

    private HeuristicUnsafeProjector() {
        this.traceCounter = 1;
    }

    public static HeuristicUnsafeProjector getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new HeuristicUnsafeProjector();
        }
        return INSTANCE;
    }

    @Override
    public void projectAndAddTraces(XLog log, Place projectionPlace, LinkedList<Integer> tokenSequence, LinkedList<Transition> firingSequence) {

    }

    @Override
    public void apply(XLog log, Place projectionPlace, HashMap<LinkedList<Transition>, LinkedList<Integer>> chosenTokenAndFiringSequences) {

    }
}
