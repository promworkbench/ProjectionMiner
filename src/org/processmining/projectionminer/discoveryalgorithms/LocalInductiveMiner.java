package org.processmining.projectionminer.discoveryalgorithms;


import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.plugins.IMPetriNet;
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;

public class LocalInductiveMiner extends DiscoveryPlugin {

    @Override
    public AcceptingPetriNetWrapper discover(UIPluginContext context, XLog log, Object parameter) {
        Object[] objects;
        MiningParameters parameters;

        if (parameter != null) {
            parameters = (MiningParameters) parameter;
        } else {
            parameters = (MiningParameters) startGUI(context, log);
        }

        objects = IMPetriNet.minePetriNet(context, log, parameters);

        Petrinet net = (Petrinet) objects[0];
        Marking initialMarking = (Marking) objects[1];
        Marking finalMarking = (Marking) objects[2];
        AcceptingPetriNetWrapper result = new AcceptingPetriNetWrapper(net, initialMarking, finalMarking, parameters);

        return result;
    }

    @Override
    public Object startGUI(UIPluginContext context, XLog log) {
        IMMiningDialog dialog = new IMMiningDialog(log);
        InteractionResult result = context.showWizard("Mine using Inductive Miner", true, true, dialog);
        context.log("Mining...");
        if (result != InteractionResult.FINISHED) {
            context.getFutureResult(0).cancel(false);
            context.getFutureResult(1).cancel(false);
            context.getFutureResult(2).cancel(false);
            return null;
        }
        return dialog.getMiningParameters();
    }

    @Override
    public String toString() {
        return "Inductive Miner";
    }
}
