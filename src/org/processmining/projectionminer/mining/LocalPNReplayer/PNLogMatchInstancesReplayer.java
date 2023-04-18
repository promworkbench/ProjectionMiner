/**
 *
 */
package org.processmining.projectionminer.mining.LocalPNReplayer;

import nl.tue.astar.AStarException;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.connections.petrinets.PNMatchInstancesRepResultConnection;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.matchinstances.algorithms.IPNMatchInstancesLogReplayAlgorithm;
import org.processmining.plugins.petrinet.replayresult.PNMatchInstancesRepResult;

import java.text.NumberFormat;

/**
 * This class replay a log on a model and return the set of all best matching
 * alignments for all traces in the log.
 * <p>
 * NOTE: Some algorithms discard final markings, some are not.
 *
 * @author aadrians adapted by Christian Rennert
 */

public class PNLogMatchInstancesReplayer {

    public PNMatchInstancesRepResult replayLog(final UIPluginContext context, AcceptingPetriNet net, XLog log, TransEvClassMapping mapping, IPNMatchInstancesLogReplayAlgorithm selectedAlgorithm)
            throws AStarException {
        return replayLogGUIPrivate(context, net, log, selectedAlgorithm, mapping);
    }

    private PNMatchInstancesRepResult replayLogGUIPrivate(final UIPluginContext context, AcceptingPetriNet net, XLog log, IPNMatchInstancesLogReplayAlgorithm selectedAlgorithm, TransEvClassMapping mapping) throws AStarException {
        PNMatchInstancesReplayerUI pnReplayerUI = new PNMatchInstancesReplayerUI(context, selectedAlgorithm);
        Object[] resultConfiguration = pnReplayerUI.getConfiguration(net.getNet(), log, mapping);
        if (resultConfiguration == null) {
            context.getFutureResult(0).cancel(true);
            return null;
        }

        // check connection between petri net and marking
        Marking initMarking = net.getInitialMarking();

        Marking finalMarking = net.getFinalMarkings().iterator().next();

        // if all parameters are set, replay log
        if (resultConfiguration[PNMatchInstancesReplayerUI.MAPPING] != null) {
            context.log("replay is performed. All parameters are set.");

            // get all parameters
            IPNMatchInstancesLogReplayAlgorithm selectedAlg = (IPNMatchInstancesLogReplayAlgorithm) resultConfiguration[PNMatchInstancesReplayerUI.ALGORITHM];

            PNMatchInstancesRepResult res = replayLogPrivate(context, net.getNet(), log,
                    (TransEvClassMapping) resultConfiguration[PNMatchInstancesReplayerUI.MAPPING], initMarking,
                    finalMarking, selectedAlg, (Object[]) resultConfiguration[PNMatchInstancesReplayerUI.PARAMETERS]);

            // add connection
            PNMatchInstancesRepResultConnection con = context.addConnection(new PNMatchInstancesRepResultConnection(
                    "All results of replaying " + XConceptExtension.instance().extractName(log) + " on "
                            + net.getNet().getLabel(), net.getNet(), initMarking, log, res));
            con.setLabel("Connection between " + net.getNet().getLabel() + ", " + XConceptExtension.instance().extractName(log)
                    + ", and all optimal alignments");

            context.getFutureResult(0).setLabel(
                    "All optimal alignments between log " + XConceptExtension.instance().extractName(log) + " on "
                            + net.getNet().getLabel() + " using " + selectedAlg);

            return res;

        } else {
            context.log("replay is not performed because not enough parameter is submitted");
            context.getFutureResult(0).cancel(true);
            return null;
        }
    }

    private PNMatchInstancesRepResult replayLogPrivate(PluginContext context, PetrinetGraph net, XLog log,
                                                       TransEvClassMapping mapping, Marking initMarking, Marking finalMarking,
                                                       IPNMatchInstancesLogReplayAlgorithm selectedAlg, Object[] parameters) throws AStarException {

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(2);

        long startTime = System.nanoTime();

        // for each trace, replay according to the algorithm. Only returns two objects
        PNMatchInstancesRepResult allReplayRes = selectedAlg.replayLog(context, net, initMarking, finalMarking, log,
                mapping, parameters);
        long duration = System.nanoTime() - startTime;

        if (context != null) {
            context.log("Replay is finished in " + nf.format(duration / 1000000000) + " seconds");
        }
        return allReplayRes;
    }
}
