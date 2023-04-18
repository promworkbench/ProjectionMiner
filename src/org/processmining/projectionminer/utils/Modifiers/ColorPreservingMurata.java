package org.processmining.projectionminer.utils.Modifiers;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.events.Logger;
import org.processmining.models.connections.petrinets.PetrinetGraphConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.reduction.*;

import javax.swing.*;
import java.util.*;

public class ColorPreservingMurata {
    public Object[] runPreserveBehavior(PluginContext context, Petrinet net, Marking marking) throws ConnectionCannotBeObtained {
        MurataParameters parameters = new MurataParameters();
        parameters.setAllowFPTSacredNode(false);
        return this.run(context, net, marking, parameters);
    }

    public Object[] run(PluginContext context, Petrinet net, Marking marking, MurataParameters parameters) throws ConnectionCannotBeObtained {
        MurataInput input = new MurataInput(net, marking);
        input.setVisibleSacred(net);
        MurataOutput output = this.run(context, input, parameters);
        Object[] objects = new Object[]{output.getNet(), output.getMarking(), output.getPlaceMapping()};
        return objects;
    }

    public MurataOutput run(PluginContext context, MurataInput input, MurataParameters parameters) throws ConnectionCannotBeObtained {
        Petrinet net = PetriNetModifier.getInstance().copyPetriNet(input.getNet());
        HashMap<Transition, Transition> transitionMap = PetriNetModifier.getInstance().getLastTransitionToTransition();
        HashMap<Place, Place> placeMap = PetriNetModifier.getInstance().getLastPlaceToPlace();
        Marking marking = PetriNetModifier.getInstance().getLastInitialMarking(input.getMarking());
        if (marking.isEmpty() && !input.getMarking().isEmpty()) {
            context.log("Petri net and marking are not related. Assuming empty initial marking.", Logger.MessageLevel.WARNING);
            if (context instanceof UIPluginContext) {
                JOptionPane.showMessageDialog(null, "Petri net and marking are not related. Assuming empty initial marking.");
            }
        }

        if (context != null) {
            context.getFutureResult(0).setLabel(net.getLabel());
        }

        MurataOutput output = new MurataOutput(net, marking);
        HashSet<PetrinetNode> sacredNodes = new HashSet();
        Iterator var10 = input.getNet().getTransitions().iterator();

        while (var10.hasNext()) {
            Transition transition = (Transition) var10.next();
            if (input.isSacred(transition)) {
                sacredNodes.add(transitionMap.get(transition));
            }
        }

        var10 = input.getNet().getPlaces().iterator();

        while (var10.hasNext()) {
            Place place = (Place) var10.next();
            if (input.isSacred(place)) {
                sacredNodes.add(placeMap.get(place));
            }
        }

        Collection<MurataRule> reductionRules = new ArrayList();
        if (input.isAllowedRule(1)) {
            reductionRules.add(new MurataFST());
        }

        if (input.isAllowedRule(2)) {
            reductionRules.add(new MurataFSP());
        }

        if (input.isAllowedRule(4)) {
            reductionRules.add(new MurataFPT());
        }

        if (input.isAllowedRule(8)) {
            reductionRules.add(new MurataFPP());
        }

        if (input.isAllowedRule(16)) {
            reductionRules.add(new MurataEST());
        }

        if (input.isAllowedRule(32)) {
            reductionRules.add(new MurataESP());
        }

        if (input.isAllowedRule(64)) {
            reductionRules.add(new MurataCSM());
        }

        if (input.isAllowedRule(128)) {
            reductionRules.add(new MurataASM());
        }

        int size = net.getPlaces().size() + net.getTransitions().size();
        if (context != null) {
            context.getProgress().setMinimum(0);
            context.getProgress().setMaximum(size);
            context.getProgress().setCaption("Reducing Petri net");
            context.getProgress().setIndeterminate(false);
        }

        String log;
        do {
            log = null;
            Map<PetrinetNode, Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>> inputEdges = new HashMap();
            Map<PetrinetNode, Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>> outputEdges = new HashMap();
            Iterator var15 = net.getNodes().iterator();

            while (var15.hasNext()) {
                PetrinetNode node = (PetrinetNode) var15.next();
                inputEdges.put(node, new HashSet());
                outputEdges.put(node, new HashSet());
            }

            var15 = net.getEdges().iterator();

            while (var15.hasNext()) {
                PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge = (PetrinetEdge) var15.next();
                outputEdges.get(edge.getSource()).add(edge);
                inputEdges.get(edge.getTarget()).add(edge);
            }

            var15 = reductionRules.iterator();

            while (var15.hasNext()) {
                MurataRule reductionRule = (MurataRule) var15.next();
                if (log == null) {
                    log = reductionRule.reduce(net, sacredNodes, transitionMap, placeMap, marking, inputEdges, outputEdges, parameters);
                }
            }

            if (log != null) {
                int newSize = net.getPlaces().size() + net.getTransitions().size();
                System.out.println("[Murata] new net size: " + newSize);

                for (; size > newSize; --size) {
                    if (context != null) {
                        context.getProgress().inc();
                    }
                }

                output.getLog().add(log);
            }
        } while (log != null);

        for (; size > 0; --size) {
            if (context != null) {
                context.getProgress().inc();
            }
        }

        if (context != null) {
            context.addConnection(new InitialMarkingConnection(net, marking));
            context.addConnection(new PetrinetGraphConnection(input.getNet(), net, transitionMap, placeMap));
        }

        output.setTransitionMapping(transitionMap);
        output.setPlaceMapping(placeMap);
        return output;
    }
}
