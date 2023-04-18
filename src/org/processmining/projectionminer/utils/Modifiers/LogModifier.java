package org.processmining.projectionminer.utils.Modifiers;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XLogImpl;
import org.deckfour.xes.model.impl.XTraceImpl;

import java.util.ArrayList;
import java.util.Set;

/**
 * Class to modify logs.
 */
public final class LogModifier {
    private static LogModifier INSTANCE;

    public static LogModifier getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LogModifier();
        }
        return INSTANCE;
    }

    /**
     * Copies a log.
     *
     * @param log A log to be copied.
     * @return A copy of the log.
     */
    public XLog copyLog(XLog log) {
        XLog result = new XLogImpl(log.getAttributes());

        for (XTrace trace : log) {
            XTrace copiedTrace = new XTraceImpl(trace.getAttributes());

            copiedTrace.addAll(trace);

            result.add(copiedTrace);
        }
        return result;
    }

    /**
     * Checks whether empty traces are in a log and modifies the log removing them.
     *
     * @param log The log to be modified.
     * @return True, if the log contains empty traces.
     */
    public boolean removeEmptyTraces(XLog log) {
        ArrayList<XTrace> tracesToBeRemoved = new ArrayList<>();

        // Checks all traces whether they are empty.
        boolean containsEmptyTraces = false;
        for (XTrace trace : log) {
            if (trace.isEmpty()) {
                containsEmptyTraces = true;
                tracesToBeRemoved.add(trace);
            }
        }

        // Removes each empty trace.
        for (XTrace traceToBeRemoved : tracesToBeRemoved) {
            log.remove(traceToBeRemoved);
        }

        return containsEmptyTraces;
    }

    /**
     * Projects a log on a set of activity names.
     *
     * @param log        Input log to be filtered.
     * @param activities Activities to remain in the output log.
     * @return The input log projected on the activities.
     */
    public XLog filter(XLog log, Set<String> activities) {
        XEventClassifier classifier = new XEventNameClassifier();
        XLog result = new XLogImpl(log.getAttributes());

        for (XTrace trace : log) {
            XTrace copyTrace = new XTraceImpl(trace.getAttributes());
            for (XEvent event : trace) {
                if (activities.contains(classifier.getClassIdentity(event))) {
                    copyTrace.add(event);
                }
            }
            if (copyTrace.size() > 0) {
                result.add(copyTrace);
            }
        }

        return result;
    }
}
