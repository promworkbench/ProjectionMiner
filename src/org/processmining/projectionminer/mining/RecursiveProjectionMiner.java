package org.processmining.projectionminer.mining;

import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XLogImpl;
import org.processmining.projectionminer.discoveryalgorithms.AcceptingPetriNetWrapper;
import org.processmining.projectionminer.discoveryalgorithms.DiscoveryPlugin;
import org.processmining.projectionminer.mining.FiringSequenceHeuristics.AbstractFiringSequenceChooser;
import org.processmining.projectionminer.mining.FiringSequences.AlignmentBasedSequenceGenerator;
import org.processmining.projectionminer.mining.FiringSequences.ValidFiringSequenceGenerator;
import org.processmining.projectionminer.mining.Projectors.SafePlaceProjector;
import org.processmining.projectionminer.parameters.ProjectionMinerParameters;
import org.processmining.projectionminer.utils.Modifiers.LogModifier;
import org.processmining.projectionminer.utils.Modifiers.PetriNetModifier;
import org.processmining.projectionminer.utils.TokenSequences.TokenSequenceCalculator;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

import java.util.*;

public class RecursiveProjectionMiner {

    private static final LinkedList<XLog> logs = new LinkedList<>();
    /**
     * Unspecific functionalities for this class down here.
     */

    private static int skipCount = 0;
    private final int remainingDepth;
    ProjectionMinerParameters parameters;

    public RecursiveProjectionMiner(ProjectionMinerParameters parameters) {
        this.remainingDepth = parameters.getMaxRecursionDepth();
        this.parameters = parameters;
    }

    private RecursiveProjectionMiner(int remainingDepth, ProjectionMinerParameters parameters) {
        this.remainingDepth = remainingDepth;
        this.parameters = parameters;
    }

    public AcceptingPetriNet discover(UIPluginContext context, XLog log, AcceptingPetriNetWrapper acceptingPetriNetWrapper) throws InterruptedException {
        logs.add((XLog) log.clone());
        AcceptingPetriNetImpl net = new AcceptingPetriNetImpl(acceptingPetriNetWrapper.getNet(), acceptingPetriNetWrapper.getInitialMarking(), acceptingPetriNetWrapper.getFinalMarking());

        if (remainingDepth > 0 && !log.isEmpty()) {
            // Reduces the log to have only one trace per variant as this version of the ProjectionMiner expects perfectly fitting results.
            log = restrictOneTracePerVariant(log);


            AcceptingPetriNetWrapper subnetResult = null;
            Petrinet subnet = null;
            Marking subInitialMarking = null;
            Marking subFinalMarking = null;

            // Computing the tuple of places and their one-looping transitions stored in a HashMap.
            HashMap<Place, ArrayList<Transition>> oneLoopingPlaces = findOneLoopingPlaces(net.getNet());
            if (oneLoopingPlaces.isEmpty()) {
                return net;
            }

            HashSet<LinkedList<Transition>> firingSequences = new HashSet<>();

            firingSequences = findFiringSequencesStructual(net, log, oneLoopingPlaces, parameters);

            for (Map.Entry<Place, ArrayList<Transition>> currTuple : oneLoopingPlaces.entrySet()) {
                Place projectionPlace = currTuple.getKey();
                ArrayList<Transition> oneLoopingTransitions = currTuple.getValue();

                HashMap<LinkedList<Transition>, LinkedList<Integer>> firingSequenceTokenSequenceOnPlace = new HashMap<>();
                for (LinkedList<Transition> firingSequence : firingSequences) {
                    LinkedList<Integer> correspondingTokenSequence = TokenSequenceCalculator.getInstance().calculateTokenSequence(firingSequence, projectionPlace, net.getInitialMarking().contains(projectionPlace));
                    firingSequenceTokenSequenceOnPlace.put(firingSequence, correspondingTokenSequence);
                }

                XLog projectedLog = SafePlaceProjector.getInstance().project(log, projectionPlace, firingSequenceTokenSequenceOnPlace);

                // Checks whether the projected log contains empty traces and removes them.
                // Further, the subnet is optional, if the log contained empty traces.
                boolean optionalSubnet = LogModifier.getInstance().removeEmptyTraces(projectedLog);

                if (projectedLog.isEmpty()) {
                    continue;
                }

                // Unpacks the plugin for the discovery of further subnets.
                DiscoveryPlugin plugin = parameters.getDiscoveryPlugin();

                // Discovers the Petri net on the projected log.
                subnetResult = plugin.discoverWithMeasure(context, projectedLog, acceptingPetriNetWrapper.getParameters());
//                    try {
                subnet = subnetResult.getNet();
//                    }catch(NullPointerException e){
//                        System.out.println("Der Fehler ist ganz klar:" + e);
//                    }
                subInitialMarking = subnetResult.getInitialMarking();
                subFinalMarking = subnetResult.getFinalMarking();


                // Checks whether the subnet contains new information.
                if (!isSubnetContainingNewInformation(oneLoopingTransitions, subnetResult)) {
                    continue;
                }

                AcceptingPetriNetWrapper recursiveCallInputNet = new AcceptingPetriNetWrapper(subnet, subInitialMarking, subFinalMarking, acceptingPetriNetWrapper.getParameters());
                RecursiveProjectionMiner subMiner = new RecursiveProjectionMiner(remainingDepth - 1, parameters);

                AcceptingPetriNet recursiveSubnet = subMiner.discover(context, projectedLog, recursiveCallInputNet);


                if (isSubnetContainingNewInformation(oneLoopingTransitions, recursiveSubnet)) {
                    insertSubnet(optionalSubnet, recursiveSubnet, net, projectionPlace, oneLoopingTransitions);
                }
            }
        }
        return net;
    }

    private HashSet<LinkedList<Transition>> findFiringSequencesAlignments(UIPluginContext context, AcceptingPetriNetImpl net, XLog log, HashMap<Place, ArrayList<Transition>> oneLoopingPlaces, ProjectionMinerParameters parameters) {
        HashSet<LinkedList<Transition>> firingSequences = new HashSet<>();

        AlignmentBasedSequenceGenerator generator = new AlignmentBasedSequenceGenerator();
        HashMap<XTrace, HashSet<LinkedList<Transition>>> firingSequencesPerTrace = generator.generateFiringSequencesAlignmentBased(context, log, net);

        AbstractFiringSequenceChooser chooser = parameters.getFiringSequenceChooser();
        chooser.init(oneLoopingPlaces.keySet(), net.getInitialMarking());
        for (Map.Entry<XTrace, HashSet<LinkedList<Transition>>> entry : firingSequencesPerTrace.entrySet()) {
            Set<LinkedList<Transition>> foundSequences = entry.getValue();
            LinkedList<Transition> chosenSequence = chooser.chooseTransition(foundSequences, false);
            firingSequences.add(chosenSequence);
        }

        return firingSequences;
    }

    private HashSet<LinkedList<Transition>> findFiringSequencesStructual(AcceptingPetriNetImpl net, XLog log, HashMap<Place, ArrayList<Transition>> oneLoopingPlaces, ProjectionMinerParameters parameters) {
        HashSet<LinkedList<Transition>> firingSequences = new HashSet<>();

        int sequenceGeneratorDepth = parameters.getSequenceGeneratorDepth();
        ValidFiringSequenceGenerator generator = new ValidFiringSequenceGenerator(net, sequenceGeneratorDepth, false);

        AbstractFiringSequenceChooser chooser = parameters.getFiringSequenceChooser();
        chooser.init(oneLoopingPlaces.keySet(), net.getInitialMarking());
        for (XTrace trace : log) {
            Set<LinkedList<Transition>> foundSequences = generator.findFiringSequences(trace);
            LinkedList<Transition> chosenSequence = chooser.chooseTransition(foundSequences, false);
            firingSequences.add(chosenSequence);
        }

        return firingSequences;
    }

    /**
     * Checks whether the new Petri net contains some new transitions considering the labels.
     *
     * @param loopingTransitions The looping transitions of a place.
     * @param minedSubnet        A discovered net for the projected log.
     * @return True, if the discovered subnet contains further information.
     */
    protected boolean isSubnetContainingNewInformation(ArrayList<Transition> loopingTransitions, AcceptingPetriNetWrapper minedSubnet) {
        return isSubnetContainingNewInformation(loopingTransitions, new AcceptingPetriNetImpl(minedSubnet.getNet(), minedSubnet.getInitialMarking(), minedSubnet.getFinalMarking()));
    }

    protected boolean isSubnetContainingNewInformation(ArrayList<Transition> loopingTransitions, AcceptingPetriNet minedSubnet) {
        if (minedSubnet == null) {
            return false;
        }
        Petrinet subnet = minedSubnet.getNet();
        HashSet<String> loopingActivities = new HashSet<>();
        for (Transition loopingTransition : loopingTransitions) {
            loopingActivities.add(loopingTransition.getLabel());
        }

        for (Place place : subnet.getPlaces()) {
            if (placeContainsSameActivities(loopingActivities, place, subnet)) {
                return false;
            }
        }

        return true;
    }


    /**
     * Checks for a set of activities and a place in a given net, whether that place has one-looping transitions that
     * have the activities as labels.
     *
     * @param activitiesToBeChecked The activities to be found.
     * @param place                 The place to be checked in the net.
     * @param net                   The net that contains the place to be checked.
     * @return True, if the activities are a subset of the set of labels of the one-looping transitions of the place.
     */
    protected boolean placeContainsSameActivities(HashSet<String> activitiesToBeChecked, Place place, Petrinet net) {
        // The list to keep track of the one-looping transitions
        ArrayList<Transition> loopingTransitions = new ArrayList<>();

        ArrayList<PetrinetNode> ingoingTransitions = new ArrayList<>();
        for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : net.getInEdges(place)) {
            ingoingTransitions.add(inEdge.getSource());
        }
        for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : net.getOutEdges(place)) {
            if (ingoingTransitions.contains(outEdge.getTarget())) {
                loopingTransitions.add((Transition) outEdge.getTarget());
            }
        }

        // Transferring the looping transition labels into an overview.
        HashSet<String> loopingTransitionLabels = new HashSet<>();
        for (Transition loopingTransition : loopingTransitions) {
            if (!loopingTransition.isInvisible()) {
                loopingTransitionLabels.add(loopingTransition.getLabel());
            }
        }

        // If there are less looping transitions than activities in the set, it can clearly be no subset.
        if (loopingTransitionLabels.size() < activitiesToBeChecked.size()) {
            return false;
        }

        // If there is an activity not in the looping transitions it is returned false.
        for (String activityToBeChecked : activitiesToBeChecked) {
            if (!loopingTransitionLabels.contains(activityToBeChecked)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Removes a node from the net by removing the ingoing and outgoing edges of the transition.
     *
     * @param net  The Petri net to be modified.
     * @param node The node object to be removed from the Petri net.
     */
    protected void removeNodeFromNet(Petrinet net, PetrinetNode node) {
        for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : net.getInEdges(node)) {
            net.removeEdge(inEdge);
        }
        for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : net.getOutEdges(node)) {
            net.removeEdge(outEdge);
        }
    }


    protected HashMap<Place, ArrayList<Transition>> findOneLoopingPlaces(Petrinet net) {
        // The set of one-looping places and the corresponding labels of the looping transitions
        HashMap<Place, ArrayList<Transition>> result = new HashMap<>();

        // We check for each place if the place has one-looping transitions
        for (Place p : net.getPlaces()) {
            ArrayList<Transition> loopingtransitions = new ArrayList<>();

            // If a transition is one-looping, it has an ingoing and outgoing arc with the place
            for (Transition t : net.getTransitions()) {
                if (net.getArc(p, t) != null && net.getArc(t, p) != null) {
                    // We avoid adding silent transitions
                    if (!t.getLabel().contains("tau")) {
                        loopingtransitions.add(t);
                    }
                }
            }

            // Storing the place and the looping activities , if they are non-empty
            if (!loopingtransitions.isEmpty()) {
                result.put(p, loopingtransitions);
            }
        }

        return result;
    }

    public XLog restrictOneTracePerVariant(XLog log) {
        XAttributeMap xAttributeMap = log.getAttributes();
        xAttributeMap.put("concept:name", new XAttributeLiteralImpl("concept:name", "Reduced Log"));
        XLogImpl result = new XLogImpl(xAttributeMap);

        HashMap<String, XTrace> filterMap = new HashMap<>();
        for (XTrace trace : log) {
            String encoded = "";
            for (XEvent event : trace) {
                XExtendedEvent extendedEvent = new XExtendedEvent(event);
                encoded = encoded + ", " + extendedEvent.getName();
            }

            if (!filterMap.containsKey(encoded)) {
                filterMap.put(encoded, trace);
            }
        }

        for (XTrace trace : filterMap.values()) {
            result.add((XTrace) trace.clone());
        }

        return result;
    }

    public void insertSubnet(boolean optionalSubnet, AcceptingPetriNet toBeInserted, AcceptingPetriNet origNet, Place projectionPlace, Collection<Transition> oneLoopingTransitions) {
        Marking toBeInsertedFinalMarking = toBeInserted.getFinalMarkings().iterator().next();
        Marking origMarking = origNet.getFinalMarkings().iterator().next();

        // Making subnet optional if needed by adding a silent transition connecting the initial and final places.
        if (optionalSubnet) {
            skipCount++;
            Transition skipTransition = toBeInserted.getNet().addTransition("skip-" + skipCount);
            skipTransition.setInvisible(true);
            for (Place p_init : toBeInserted.getInitialMarking()) {
                toBeInserted.getNet().addArc(p_init, skipTransition);
            }
            for (Place p_final : toBeInsertedFinalMarking) {
                toBeInserted.getNet().addArc(skipTransition, p_final);
            }
        }

        // Inserting the subnet into the supernet
        // First the places
        HashMap<Place, Place> placeToPlace = new HashMap<>();
        for (Place p : toBeInserted.getNet().getPlaces()) {
            Place newPlace = origNet.getNet().addPlace(p.getLabel());
            placeToPlace.put(p, newPlace);
            PetriNetModifier.getInstance().colorIfNeeded(newPlace, p);
        }

        // Next the transitions
        HashMap<Transition, Transition> transitionToTransition = new HashMap<>();
        for (Transition t : toBeInserted.getNet().getTransitions()) {
            Transition correspondingTransition = origNet.getNet().addTransition(t.getLabel());
            correspondingTransition.setInvisible(t.isInvisible());
            transitionToTransition.put(t, correspondingTransition);
            PetriNetModifier.getInstance().colorIfNeeded(correspondingTransition, t);
        }

        for (Transition t : toBeInserted.getNet().getTransitions()) {
            Transition correspondingTransition = transitionToTransition.get(t);
            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : toBeInserted.getNet().getInEdges(t)) {
                Place inPlace = placeToPlace.get((Place) edge.getSource());
                Arc newArc = origNet.getNet().addArc(inPlace, correspondingTransition);
                PetriNetModifier.getInstance().colorIfNeeded(newArc, edge);
            }
            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : toBeInserted.getNet().getOutEdges(t)) {
                Place outPlace = placeToPlace.get((Place) edge.getTarget());
                Arc newArc = origNet.getNet().addArc(correspondingTransition, outPlace);
                PetriNetModifier.getInstance().colorIfNeeded(newArc, edge);
            }
        }

        // Adding ingoing and outgoing edges of the place that is replaced
        Place placeToBeRemoved = projectionPlace;
        for (Place p : toBeInserted.getInitialMarking()) {
            Place currPlace = placeToPlace.get(p);
            PetriNetModifier.getInstance().colorLight(currPlace);

            if (origNet.getInitialMarking().contains(placeToBeRemoved)) {
                origNet.getInitialMarking().add(currPlace);
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : origNet.getNet().getInEdges(placeToBeRemoved)) {
                if (!oneLoopingTransitions.contains((Transition) inEdge.getSource())) {
                    Arc edge = origNet.getNet().addArc((Transition) inEdge.getSource(), currPlace);
                    PetriNetModifier.getInstance().colorLight(edge);
                }
            }
        }
        for (Place p : toBeInsertedFinalMarking) {
            Place currPlace = placeToPlace.get(p);

            if (origMarking.contains(placeToBeRemoved)) {
                origMarking.add(currPlace);
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : origNet.getNet().getOutEdges(placeToBeRemoved)) {
                if (!oneLoopingTransitions.contains((Transition) outEdge.getTarget())) {
                    Arc edge = origNet.getNet().addArc(currPlace, (Transition) outEdge.getTarget());
                    PetriNetModifier.getInstance().colorLight(edge);
                }
            }
        }

        // Adding the other dependencies of the looping transitions
        HashMap<String, Transition> activityToLoopTransition = new HashMap<>();
        for (Transition loopingTransition : oneLoopingTransitions) {
            activityToLoopTransition.put(loopingTransition.getLabel(), loopingTransition);
        }
        for (Transition subnetTransition : transitionToTransition.values()) {
            if (activityToLoopTransition.containsKey(subnetTransition.getLabel())) {
                Transition loopingTransition = activityToLoopTransition.get(subnetTransition.getLabel());

                for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : origNet.getNet().getInEdges(loopingTransition)) {
                    if (!(inEdge.getSource()).equals(placeToBeRemoved)) {
                        Arc edge = origNet.getNet().addArc((Place) inEdge.getSource(), subnetTransition);
                        PetriNetModifier.getInstance().colorLight(edge);
                    }
                }

                for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : origNet.getNet().getOutEdges(loopingTransition)) {
                    if (!(outEdge.getTarget()).equals(placeToBeRemoved)) {
                        Arc edge = origNet.getNet().addArc(subnetTransition, (Place) outEdge.getTarget());
                        PetriNetModifier.getInstance().colorLight(edge);
                    }
                }
            }
        }

        // Removing the place and the transitions that were replaced
        origNet.getInitialMarking().remove(placeToBeRemoved);
        origMarking.remove(placeToBeRemoved);


        removeNodeFromNet(origNet.getNet(), placeToBeRemoved);
        origNet.getNet().removePlace(placeToBeRemoved);

        for (Transition loopingTransition : oneLoopingTransitions) {
            removeNodeFromNet(origNet.getNet(), loopingTransition);
            origNet.getNet().removeTransition(loopingTransition);
        }

        // Restarting the loop in the case that something was changed. Therefore, we need to scan for places
        // with looping transitions again as the previous place could have been removed and others are not
        // considered to be visited in the future yet.
        PetriNetModifier.getInstance().nextColor();
    }
}
