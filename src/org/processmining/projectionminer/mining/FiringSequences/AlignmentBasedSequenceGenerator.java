package org.processmining.projectionminer.mining.FiringSequences;

import nl.tue.astar.AStarException;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.projectionminer.mining.LocalPNReplayer.PNLogMatchInstancesReplayer;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.connectionfactories.logpetrinet.EvClassLogPetrinetConnectionFactoryUI;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.matchinstances.algorithms.express.AllOptAlignmentsGraphAlg;
import org.processmining.plugins.petrinet.replayresult.PNMatchInstancesRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.AllSyncReplayResult;

import java.util.*;

public class AlignmentBasedSequenceGenerator {
    public static TransEvClassMapping getEvClassMapping(PetrinetGraph sNet, XLog log, XEventClassifier classifier) {
        XEventClass evClassDummy = EvClassLogPetrinetConnectionFactoryUI.DUMMY;
        XEventClasses ecLog = XLogInfoFactory.createLogInfo(log, classifier).getEventClasses();
        TransEvClassMapping mapping = new TransEvClassMapping(classifier, evClassDummy);
        Iterator transIt = sNet.getTransitions().iterator();

        label45:
        while (transIt.hasNext()) {
            Transition trans = (Transition) transIt.next();
            Iterator var8 = ecLog.getClasses().iterator();

            while (true) {
                XEventClass ec;
                String transitionLabel;
                String ecBaseName;
                do {
                    do {
                        if (!var8.hasNext()) {
                            if (mapping.get(trans) == null) {
                                mapping.put(trans, evClassDummy);
                            }
                            continue label45;
                        }

                        ec = (XEventClass) var8.next();
                        String[] ecBaseParts = ec.getId().split("\\+|_ITERATION_");
                        String label = trans.getLabel() == null ? "" : trans.getLabel();
                        if (ec.getId().equals(label)) {
                            mapping.put(trans, ec);
                        }

                        transitionLabel = label.split("_ITERATION_")[0];
                        ecBaseName = ecBaseParts[0];
                    } while (mapping.containsKey(trans));
                } while (!ecBaseName.equals(transitionLabel) && !(ecBaseName + "+complete").equals(transitionLabel));

                mapping.put(trans, ec);
            }
        }

        return mapping;
    }

    public PNMatchInstancesRepResult discover(UIPluginContext context, AcceptingPetriNet net, XLog log) throws AStarException {
        TransEvClassMapping evClassMapping = getEvClassMapping(net.getNet(), log, XLogInfoImpl.STANDARD_CLASSIFIER);
        PNLogMatchInstancesReplayer replayer = new PNLogMatchInstancesReplayer();
        AllOptAlignmentsGraphAlg alignmentAlgo = new AllOptAlignmentsGraphAlg();
        PNMatchInstancesRepResult allSyncReplayResults = replayer.replayLog(context, net, log, evClassMapping,
                alignmentAlgo);

        return allSyncReplayResults;
    }

    /**
     * Projects a given log on a safe or an unsafe place with the firing sequence method.
     *
     * @param context
     * @param log                    Log to be projected.
     * @param inputAcceptingPetriNet
     * @return Returns a log that contains the projected traces of the log on the place.
     */
    public HashMap<XTrace, HashSet<LinkedList<Transition>>> generateFiringSequencesAlignmentBased(UIPluginContext context, XLog log, AcceptingPetriNet inputAcceptingPetriNet) {
        PNMatchInstancesRepResult alignments = null;
        try {
            alignments = discover(context, inputAcceptingPetriNet, log);
        } catch (Exception e) {
            System.out.println(e);
        }

        HashMap<XTrace, HashSet<LinkedList<Transition>>> possibleFiringSequencesPerTrace = new HashMap<>();
        if (alignments != null) {
            for (AllSyncReplayResult alignment : alignments) {
                SortedSet<Integer> traceIDs = alignment.getTraceIndex();
                HashSet<LinkedList<Transition>> possibleFiringSequences = new HashSet<>();

                for (int i = 0; i < alignment.getNodeInstanceLst().size(); i++) {
                    List<Object> nodeInstances = alignment.getNodeInstanceLst().get(i);
                    List<StepTypes> stepTypes = alignment.getStepTypesLst().get(i);

                    LinkedList<Transition> firingSequence = new LinkedList<>();

                    for (int j = 0; j < nodeInstances.size(); j++) {
                        StepTypes stepType = stepTypes.get(j);

                        if (stepType.equals(StepTypes.LMGOOD) || stepType.equals(StepTypes.MINVI)) {
                            Transition alignedTransition = (Transition) nodeInstances.get(j);
                            firingSequence.addLast(alignedTransition);
                        }
                    }

                    possibleFiringSequences.add(firingSequence);
                }

                for (Integer traceID : traceIDs) {
                    possibleFiringSequencesPerTrace.put(log.get(traceID), possibleFiringSequences);
                }
            }
        }

        return possibleFiringSequencesPerTrace;
    }
}
