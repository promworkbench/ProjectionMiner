package org.processmining.projectionminer.discoveryalgorithms;

import org.deckfour.xes.model.XLog;
import org.processmining.projectionminer.discoveryalgorithms.TransitionSystemLocal.TSMinerLocalPlugin;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.transitionsystem.miner.TSMinerTransitionSystem;
import org.processmining.plugins.transitionsystem.regions.TransitionSystem2Petrinet;

import java.util.concurrent.ExecutionException;

public class TransitionSystemRegionMiner extends DiscoveryPlugin {
    @Override
    public AcceptingPetriNetWrapper discover(UIPluginContext context, XLog log, Object parameter) {
//        String s = XConceptExtension.instance().extractName(log);
//        System.out.println(s);
//        ProMFuture<?> futureResult1 = context.getFutureResult(0);
//
//        UIPluginContext newContext = context.createChildContext("TSMiner");
//        try {
//            newContext.setFuture(new PluginExecutionResultImpl(
//                    new Class[]{TSMinerTransitionSystem.class, DirectedGraphElementWeights.class, StartStateSet.class, AcceptStateSet.class},
//                    new String[]{"Mined Transition System", "Weights", "Start states", "Accept states"},
//                    context.getPluginDescriptor().getFirst())
//            );
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        ProMFuture<?> futureResult = newContext.getFutureResult(0);

//TSMinerTransitionSystem.class, DirectedGraphElementWeights.class, StartStateSet.class, AcceptStateSet.class
        Object[] transitionSystem = TSMinerLocalPlugin.main(context, log);
        TransitionSystem2Petrinet transitionSystem2Petrinet = new TransitionSystem2Petrinet();
        AcceptingPetriNetImpl acceptingPetriNet = null;
        try {
            Object[] objects = transitionSystem2Petrinet.convertToPetrinet(context, (TSMinerTransitionSystem) transitionSystem[0], false);
            acceptingPetriNet = new AcceptingPetriNetImpl((Petrinet) objects[0]);
        } catch (ConnectionCannotBeObtained e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (acceptingPetriNet == null) {
            return null;
        } else {
            return new AcceptingPetriNetWrapper(acceptingPetriNet.getNet(), acceptingPetriNet.getInitialMarking(), acceptingPetriNet.getFinalMarkings().iterator().next(),
                    parameter);
        }
    }

    @Override
    public Object startGUI(UIPluginContext context, XLog log) {
        return null;
    }

    @Override
    public String toString() {
        return "Find transition system and convert to Petri using region theory.";
    }
}
