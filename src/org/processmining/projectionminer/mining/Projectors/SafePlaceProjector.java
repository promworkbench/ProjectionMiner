package org.processmining.projectionminer.mining.Projectors;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.*;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public final class SafePlaceProjector extends AbstractProjector {
    private static SafePlaceProjector INSTANCE;
    private int traceCounter;

    private SafePlaceProjector() {
        this.traceCounter = 1;
    }

    public static SafePlaceProjector getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SafePlaceProjector();
        }
        return INSTANCE;
    }

    @Override
    public void projectAndAddTraces(XLog log, Place projectionPlace, LinkedList<Integer> tokenSequence, LinkedList<Transition> firingSequence) {
        XAttributeMapImpl xAttributeMap = new XAttributeMapImpl();
        xAttributeMap.put("name", new XAttributeLiteralImpl("name", Integer.toString(traceCounter++)));
        XTrace trace = new XTraceImpl(xAttributeMap);

        Iterator<Integer> itTokenSequence = tokenSequence.iterator();
        Iterator<Transition> itFiringSequence = firingSequence.iterator();

        boolean includeActivities = itTokenSequence.next() > 0;
        boolean startedTrace = false;

        while (itFiringSequence.hasNext()) {
            Transition currTransition = itFiringSequence.next();
            int nextTokenCount = itTokenSequence.next();

            if (includeActivities) {
                startedTrace = true;
                if (!currTransition.isInvisible() && nextTokenCount > 0 &&
                        checkIfIngoingTransitionOfPlace(projectionPlace, currTransition) &&
                        checkIfOutgoingTransitionOfPlace(projectionPlace, currTransition)) {
                    XEventImpl xEvent = new XEventImpl();
                    XExtendedEvent extendedEvent = new XExtendedEvent(xEvent);
                    extendedEvent.setName(currTransition.getLabel());
//                    extendedEvent.setTimestamp(new Date());
//                    extendedEvent.setTransition("complete");
                    trace.add(xEvent);
                }
            } else if (startedTrace) {
                startedTrace = false;
                log.add(trace);
                xAttributeMap = new XAttributeMapImpl();
                xAttributeMap.put("name", new XAttributeLiteralImpl("name", Integer.toString(traceCounter++)));
                trace = new XTraceImpl(xAttributeMap);
            }

            includeActivities = nextTokenCount > 0;
        }

        if (startedTrace) {
            log.add(trace);
        }
    }

    @Override
    public void apply(XLog log, Place projectionPlace, HashMap<LinkedList<Transition>, LinkedList<Integer>> chosenTokenAndFiringSequences) {
        for (Map.Entry<LinkedList<Transition>, LinkedList<Integer>> entry : chosenTokenAndFiringSequences.entrySet()) {
            projectAndAddTraces(log, projectionPlace, entry.getValue(), entry.getKey());
        }
    }

    public XLog project(XLog log, Place projectionPlace, HashMap<LinkedList<Transition>, LinkedList<Integer>> chosenTokenAndFiringSequences) {
        XAttributeMap xAttributeMap = log.getAttributes();
        xAttributeMap.put("concept:name", new XAttributeLiteralImpl("concept:name", "Place projection on " + projectionPlace.getLabel()));
        XLogImpl projectedLog = new XLogImpl(xAttributeMap);
        for (XEventClassifier classifier : log.getClassifiers()) {
            projectedLog.getClassifiers().add(classifier);
            projectedLog.setInfo(classifier, log.getInfo(classifier));
        }

        for (Map.Entry<LinkedList<Transition>, LinkedList<Integer>> entry : chosenTokenAndFiringSequences.entrySet()) {
            projectAndAddTraces(projectedLog, projectionPlace, entry.getValue(), entry.getKey());
        }

        return projectedLog;
    }
}
