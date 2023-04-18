package org.processmining.projectionminer.plugins;

import org.deckfour.uitopia.api.event.TaskListener;
import org.deckfour.xes.model.XLog;
import org.processmining.projectionminer.dialogs.MergeDialog;
import org.processmining.projectionminer.dialogs.ProjectionMinerDialog;
import org.processmining.projectionminer.discoveryalgorithms.AcceptingPetriNetWrapper;
import org.processmining.projectionminer.discoveryalgorithms.DiscoveryPlugin;
import org.processmining.projectionminer.mining.RecursiveProjectionMiner;
import org.processmining.projectionminer.parameters.ProjectionMinerParameters;
import org.processmining.projectionminer.utils.Modifiers.ColorPreservingMurata;
import org.processmining.projectionminer.utils.Modifiers.PetriNetModifier;
import org.processmining.projectionminer.utils.PerformancePlugin;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;

import java.util.ArrayList;
import java.util.Map;

@Plugin(name = "ProjectionMiner",
        parameterLabels = {"Log", "Petri Net", "Initial Marking", "Final Marking"},
        returnLabels = {"Petri net", "Initial marking", "Final marking"},
        returnTypes = {Petrinet.class, Marking.class, Marking.class},
        userAccessible = true,
        categories = {PluginCategory.Discovery, PluginCategory.Enhancement},
        keywords = {"Discovery", "Precision", "Iterative discovery", "Repair"},
        help = "Algorithm to increase precision on existing Petri nets for a given log using projections.")

public class ProjectionMinerPlugin {
    @PluginVariant(variantLabel = "Variant that is working on Petri nets.", requiredParameterLabels = {0, 1})
    @UITopiaVariant(affiliation = "PADS", author = "Christian Rennert", email = "christian.rennert@rwth-aachen.de")
    public Object[] discover(UIPluginContext context, XLog inputLog, Petrinet petrinet) throws InterruptedException, ConnectionCannotBeObtained {
        AcceptingPetriNet acceptingPetriNet = new AcceptingPetriNetImpl(petrinet);

        if (acceptingPetriNet.getInitialMarking().isEmpty()) {
            throw new InterruptedException("No valid initial marking found on the Petri net. Please create an intial and final marking on your own.");
        } else if (acceptingPetriNet.getFinalMarkings().isEmpty() || acceptingPetriNet.getFinalMarkings().iterator().next().isEmpty()) {
            throw new InterruptedException("No valid final marking found on the Petri net. Please create an intial and final marking on your own.");
        }

        return discover(context, inputLog, acceptingPetriNet);
    }

    @PluginVariant(variantLabel = "Variant that works only on given Petri nets with initial and final Marking.", requiredParameterLabels = {0, 1, 2, 3})
    @UITopiaVariant(affiliation = "PADS", author = "Christian Rennert", email = "christian.rennert@rwth-aachen.de")
    public Object[] discover(UIPluginContext context, XLog inputLog, Petrinet net, Marking initialMarking, Marking finalMarking) throws InterruptedException, ConnectionCannotBeObtained {
        AcceptingPetriNet acceptingPetriNet = new AcceptingPetriNetImpl(net, initialMarking, finalMarking);
        return discover(context, inputLog, acceptingPetriNet);
    }

    @PluginVariant(variantLabel = "Variant that is mining also the initial Petri net.", requiredParameterLabels = {0})
    @UITopiaVariant(affiliation = "PADS", author = "Christian Rennert", email = "christian.rennert@rwth-aachen.de")
    public Object[] discover(UIPluginContext context, XLog inputLog) throws InterruptedException, ConnectionCannotBeObtained {
        init();
        PerformancePlugin.getInstance().reset();

        ProjectionMinerDialog dialog = new ProjectionMinerDialog(null, inputLog);
        TaskListener.InteractionResult result = context.showWizard(ProjectionMinerDialog.TITLE, true, true, dialog);

        if (result != TaskListener.InteractionResult.FINISHED) {
            context.getFutureResult(0).cancel(false);
            return null;
        }

        ProjectionMinerParameters parameters = dialog.apply();
        DiscoveryPlugin plugin = parameters.getDiscoveryPlugin();

        // Discover and measure the discovery of the initial net.
        PerformancePlugin.getInstance().startInitial();
        AcceptingPetriNetWrapper acceptingPetriNetWrapper = plugin.discover(context, inputLog, null);
        PerformancePlugin.getInstance().finishInitial();

        return discoverAcceptingPetriNetRecursively(context, inputLog, parameters, acceptingPetriNetWrapper);
    }

    public Object[] discover(UIPluginContext context, XLog inputLog, AcceptingPetriNet net) throws InterruptedException, ConnectionCannotBeObtained {
        PerformancePlugin.getInstance().reset();
        net = PetriNetModifier.getInstance().copyAcceptingPetriNet(net);

        ProjectionMinerDialog dialog = new ProjectionMinerDialog(net.getNet(), inputLog);
        TaskListener.InteractionResult result = context.showWizard(ProjectionMinerDialog.TITLE, true, true, dialog);

        if (result != TaskListener.InteractionResult.FINISHED) {
            context.getFutureResult(0).cancel(false);
            return null;
        }

        ProjectionMinerParameters parameters = dialog.apply();

        if (parameters.shouldInitialPlaceMerge()) {
            MergeDialog mergeDialog = new MergeDialog();
            ArrayList<Place> placesToBeMerged = mergeDialog.apply(context, net.getNet());
            AcceptingPetriNetImpl acceptingPetriNet = PetriNetModifier.getInstance().mergePlaces(net, placesToBeMerged);

            PetriNetModifier.getInstance().removeUselessSilentTransitions(acceptingPetriNet);
            net = acceptingPetriNet;
//            ColorPreservingMurata murata = new ColorPreservingMurata();
//            Object[] murataResult = murata.runPreserveBehavior(context, acceptingPetriNet.getNet(), acceptingPetriNet.getInitialMarking());

//            PetriNetModifier.getInstance().fixMurataColors((Petrinet) murataResult[0]);
//
//            Marking newFinalMarking = new Marking();
//            Map<Place, Place> placePlaceMap = (Map<Place, Place>) murataResult[2];
//            for (Place place : acceptingPetriNet.getFinalMarkings().iterator().next()) {
//                newFinalMarking.add(placePlaceMap.get(place));
//            }
//
//            net = new AcceptingPetriNetImpl((Petrinet) murataResult[0], (Marking) murataResult[1], newFinalMarking);
        }

        DiscoveryPlugin plugin = parameters.getDiscoveryPlugin();
        Object parameter = plugin.startGUI(context, inputLog);

        AcceptingPetriNetWrapper acceptingPetriNetWrapper = new AcceptingPetriNetWrapper(net.getNet(), net.getInitialMarking(), net.getFinalMarkings().iterator().next(), parameter);
        acceptingPetriNetWrapper.polishNet();

        return discoverAcceptingPetriNetRecursively(context, inputLog, parameters, acceptingPetriNetWrapper);
    }

    private void init() {
        PetriNetModifier.getInstance().resetColor();
    }


    public Object[] discoverAcceptingPetriNetRecursively(UIPluginContext context, XLog inputLog, ProjectionMinerParameters parameters, AcceptingPetriNetWrapper acceptingPetriNetWrapper) throws InterruptedException, ConnectionCannotBeObtained {
        RecursiveProjectionMiner recursiveProjectionMiner = new RecursiveProjectionMiner(parameters);

        PerformancePlugin.getInstance().startProjection();
        AcceptingPetriNet finalResult = recursiveProjectionMiner.discover(context, inputLog, acceptingPetriNetWrapper);
        PerformancePlugin.getInstance().finishProjection();

        if (parameters.shouldReduceSilentTransitions()) {
            ColorPreservingMurata murata = new ColorPreservingMurata();
            Object[] murataResult = murata.runPreserveBehavior(context, finalResult.getNet(), finalResult.getInitialMarking());

            PetriNetModifier.getInstance().fixMurataColors((Petrinet) murataResult[0]);

            Marking newFinalMarking = new Marking();
            Map<Place, Place> placePlaceMap = (Map<Place, Place>) murataResult[2];
            for (Place place : finalResult.getFinalMarkings().iterator().next()) {
                newFinalMarking.add(placePlaceMap.get(place));
            }

            finalResult = new AcceptingPetriNetImpl((Petrinet) murataResult[0], (Marking) murataResult[1], newFinalMarking);
        }

        System.out.println(PerformancePlugin.getInstance().toString());

        return PetriNetModifier.getInstance().copyAcceptingPetriNetNumbered(finalResult);
    }
}
