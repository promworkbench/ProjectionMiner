package org.processmining.projectionminer.utils.Modifiers;

import org.processmining.projectionminer.discoveryalgorithms.AcceptingPetriNetWrapper;
import org.processmining.projectionminer.utils.TauNameGenerator;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetImpl;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.semantics.petrinet.Marking;

import java.awt.*;
import java.util.*;

/**
 * Class for Petri net modifications.
 */
public class PetriNetModifier {
    private static final ArrayList<Color> colors = new ArrayList<>(Arrays.asList(
            new Color(0.12156862745098039f, 0.4666666666666667f, 0.7058823529411765f),
            new Color(1.0f, 0.4980392156862745f, 0.054901960784313725f),
            new Color(0.17254901960784313f, 0.6274509803921569f, 0.17254901960784313f),
            new Color(0.8392156862745098f, 0.15294117647058825f, 0.1568627450980392f),
            new Color(0.5803921568627451f, 0.403921568627451f, 0.7411764705882353f),
            new Color(0.5490196078431373f, 0.33725490196078434f, 0.29411764705882354f),
            new Color(0.8901960784313725f, 0.4666666666666667f, 0.7607843137254902f),
            new Color(0.4980392156862745f, 0.4980392156862745f, 0.4980392156862745f),
            new Color(0.7372549019607844f, 0.7411764705882353f, 0.13333333333333333f),
            new Color(0.09019607843137255f, 0.7450980392156863f, 0.8117647058823529f))
    );
    private static final ArrayList<Color> lightColors = new ArrayList<>(Arrays.asList(
            new Color(0.6313725490196078f, 0.788235294117647f, 0.9568627450980393f),
            new Color(1.0f, 0.7058823529411765f, 0.5098039215686274f),
            new Color(0.5529411764705883f, 0.8980392156862745f, 0.6313725490196078f),
            new Color(1.0f, 0.6235294117647059f, 0.6078431372549019f),
            new Color(0.8156862745098039f, 0.7333333333333333f, 1.0f),
            new Color(0.8705882352941177f, 0.7333333333333333f, 0.6078431372549019f),
            new Color(0.9803921568627451f, 0.6901960784313725f, 0.8941176470588236f),
            new Color(0.8117647058823529f, 0.8117647058823529f, 0.8117647058823529f),
            new Color(1.0f, 0.996078431372549f, 0.6392156862745098f),
            new Color(0.7254901960784313f, 0.9490196078431372f, 0.9411764705882353f))
    );
    private static PetriNetModifier INSTANCE;
    private static Color currentColor = colors.get(0);
    private static Color currentLightColor = lightColors.get(0);
    private static int colorIndex = 0;
    private static boolean shouldColor = false;
    private final HashMap<Color, Integer> colorToId = new HashMap<>();
    private HashMap<Place, Place> lastPlaceToPlace;
    private HashMap<Transition, Transition> lastTransitionToTransition;

    public PetriNetModifier() {
        for (int i = 0; i < colors.size(); i++) {
            colorToId.put(colors.get(i), i);
        }

        for (int i = 0; i < lightColors.size(); i++) {
            colorToId.put(lightColors.get(i), i);
        }
    }

    public static PetriNetModifier getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PetriNetModifier();
        }
        return INSTANCE;
    }

    public void updateShouldColor(boolean update) {
        shouldColor = update;
    }

    /**
     * Copies a given Petri net and stores the mappings between the original and the copy for the last input.
     *
     * @param net A Petri net to be copied.
     * @return The copy of the Petri net.
     */
    public Petrinet copyPetriNet(Petrinet net) {
        return copyPetriNet(net, false);
    }

    public AcceptingPetriNetImpl copyAcceptingPetriNet(AcceptingPetriNet accNet) {
        Petrinet resultNet = copyPetriNet(accNet.getNet(), false);
        Marking initialMarking = getLastInitialMarking(accNet.getInitialMarking());
        Marking finalMarking = getLastInitialMarking(accNet.getFinalMarkings().iterator().next());
        return new AcceptingPetriNetImpl(resultNet, initialMarking, finalMarking);
    }


    public Object[] copyAcceptingPetriNetNumbered(AcceptingPetriNet accNet) {
        Petrinet resultNet = copyPetriNet(accNet.getNet(), true);
        Marking initialMarking = getLastInitialMarking(accNet.getInitialMarking());
        Marking finalMarking = getLastInitialMarking(accNet.getFinalMarkings().iterator().next());
        return new Object[]{resultNet, initialMarking, finalMarking};
    }

    private Petrinet copyPetriNet(Petrinet net, boolean numberPlaces) {
        Petrinet newNet = new PetrinetImpl("");
        HashMap<Transition, Transition> transitionTransition = new HashMap<>();
        HashMap<Place, Place> placeToPlace = new HashMap<>();

        for (Transition oldTransition : net.getTransitions()) {
            Transition newTransition = newNet.addTransition(oldTransition.getLabel());
            transitionTransition.put(oldTransition, newTransition);
            newTransition.setInvisible(oldTransition.isInvisible());
            preserveColor(newTransition, oldTransition);
        }

        int placeCounter = 0;
        for (Place oldPlace : net.getPlaces()) {
            Place newPlace;
            if (numberPlaces) {
                newPlace = newNet.addPlace(Integer.toString(placeCounter));
                placeCounter++;
            } else {
                newPlace = newNet.addPlace(oldPlace.getLabel());
            }
            placeToPlace.put(oldPlace, newPlace);

            preserveColor(newPlace, oldPlace);

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> oldInEdge : net.getInEdges(oldPlace)) {
                Transition source = transitionTransition.get((Transition) oldInEdge.getSource());
                Arc newinEdge = newNet.addArc(source, newPlace);

                preserveColor(newinEdge, oldInEdge);
            }
            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> oldOutEdge : net.getOutEdges(oldPlace)) {
                Transition target = transitionTransition.get((Transition) oldOutEdge.getTarget());
                Arc newOutEdge = newNet.addArc(newPlace, target);

                preserveColor(newOutEdge, oldOutEdge);
            }
        }

        lastPlaceToPlace = placeToPlace;
        lastTransitionToTransition = transitionTransition;

        return newNet;
    }

    private PetrinetEdge preserveColor(PetrinetEdge newEdge, PetrinetEdge oldEdge) {
        AttributeMap attributeMap = oldEdge.getAttributeMap();
        if (attributeMap.get(AttributeMap.EDGECOLOR) != null) {
            newEdge.getAttributeMap().put(AttributeMap.EDGECOLOR, attributeMap.get(AttributeMap.EDGECOLOR));
        }

        return newEdge;
    }

    public PetrinetNode preserveColor(PetrinetNode new_node, PetrinetNode old_node) {
        AttributeMap attributeMap = old_node.getAttributeMap();
        if (attributeMap.get(AttributeMap.LABELCOLOR) != null) {
            new_node.getAttributeMap().put(AttributeMap.LABELCOLOR, attributeMap.get(AttributeMap.LABELCOLOR));
        }
        if (attributeMap.get(AttributeMap.STROKECOLOR) != null) {
            new_node.getAttributeMap().put(AttributeMap.STROKECOLOR, attributeMap.get(AttributeMap.STROKECOLOR));
        }
        if (attributeMap.get(AttributeMap.FILLCOLOR) != null) {
            new_node.getAttributeMap().put(AttributeMap.FILLCOLOR, attributeMap.get(AttributeMap.FILLCOLOR));
        }

        return new_node;
    }

    public Petrinet colorPetrinet(Petrinet net) {
        if (shouldColor) {

            for (PetrinetNode node : net.getNodes()) {
                color(node);
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getEdges()) {
                color(edge);
            }

            nextColor();
        }

        return net;
    }

    public PetrinetNode colorIfNeeded(PetrinetNode new_node, PetrinetNode old_node) {
        if (old_node.getAttributeMap().get(AttributeMap.STROKECOLOR) != null) {
            preserveColor(new_node, old_node);
        } else if (shouldColor) {
            color(new_node);
        }

        return new_node;
    }

    public PetrinetEdge colorIfNeeded(PetrinetEdge newEdge, PetrinetEdge oldEdge) {
        if (oldEdge.getAttributeMap().get(AttributeMap.EDGECOLOR) != null) {
            preserveColor(newEdge, oldEdge);
        } else if (shouldColor) {
            color(newEdge);
        }

        return newEdge;
    }

    public PetrinetNode color(PetrinetNode node) {
        if (shouldColor) {
            AttributeMap attributeMap = node.getAttributeMap();

            attributeMap.put(AttributeMap.STROKECOLOR, currentColor);
            attributeMap.put(AttributeMap.LABELCOLOR, currentColor);

            if (attributeMap.get(AttributeMap.FILLCOLOR) != null) {
                attributeMap.put(AttributeMap.FILLCOLOR, currentColor);
            }
        }

        return node;
    }

    public PetrinetNode color(PetrinetNode node, Color color) {
        if (shouldColor) {
            AttributeMap attributeMap = node.getAttributeMap();

            attributeMap.put(AttributeMap.STROKECOLOR, color);
            attributeMap.put(AttributeMap.LABELCOLOR, color);

            if (attributeMap.get(AttributeMap.FILLCOLOR) != null) {
                attributeMap.put(AttributeMap.FILLCOLOR, color);
            }
        }

        return node;
    }

    public PetrinetEdge color(PetrinetEdge edge) {
        if (shouldColor) {
            AttributeMap attributeMap = edge.getAttributeMap();
            attributeMap.put(AttributeMap.EDGECOLOR, currentColor);
        }

        return edge;
    }

    public PetrinetEdge color(PetrinetEdge edge, Color color) {
        if (shouldColor) {
            AttributeMap attributeMap = edge.getAttributeMap();
            attributeMap.put(AttributeMap.EDGECOLOR, color);
        }

        return edge;
    }

    public PetrinetNode colorLight(PetrinetNode node) {
        if (shouldColor) {
            AttributeMap attributeMap = node.getAttributeMap();

            attributeMap.put(AttributeMap.STROKECOLOR, currentLightColor);
            attributeMap.put(AttributeMap.LABELCOLOR, currentLightColor);

            if (attributeMap.get(AttributeMap.FILLCOLOR) != null) {
                attributeMap.put(AttributeMap.FILLCOLOR, currentLightColor);
            }
        }

        return node;
    }

    public PetrinetEdge colorLight(PetrinetEdge edge) {
        if (shouldColor) {
            AttributeMap attributeMap = edge.getAttributeMap();
            attributeMap.put(AttributeMap.EDGECOLOR, currentLightColor);
        }
        return edge;
    }

    public void nextColor() {
        colorIndex++;
        currentColor = colors.get(colorIndex % colors.size());
        currentLightColor = lightColors.get(colorIndex % lightColors.size());
    }

    public void resetColor() {
        resetColor(0);
    }

    public void resetColor(int color) {
        colorIndex = color;
        currentColor = colors.get(colorIndex);
        currentLightColor = lightColors.get(colorIndex);
    }

    public HashMap<Place, Place> getLastPlaceToPlace() {
        return lastPlaceToPlace;
    }

    public HashMap<Transition, Transition> getLastTransitionToTransition() {
        return lastTransitionToTransition;
    }

    /**
     * Finds the initial marking for the last copied Petri net.
     *
     * @param initialMarking The initial marking of the Petri net that was copied.
     * @return An initial marking for the copy.
     */
    public Marking getLastInitialMarking(Marking initialMarking) {
        Marking newInitialMarking = new Marking();
        for (Place p : initialMarking) {
            newInitialMarking.add(lastPlaceToPlace.get(p));
        }

        return newInitialMarking;
    }

    /**
     * Finds the final marking for the last copied Petri net.
     *
     * @param finalMarking The final marking of the Petri net that was copied.
     * @return A final marking for the copy.
     */
    public Marking getLastFinalMarking(Marking finalMarking) {
        Marking newFinalMarking = new Marking();
        for (Place p : finalMarking) {
            newFinalMarking.add(lastPlaceToPlace.get(p));
        }

        return newFinalMarking;
    }

    public void makePetriNetSkippable(Petrinet net, Marking initialMarking, Marking finalMarking) {
        Transition skipTransition = net.addTransition(TauNameGenerator.getInstance().getTauName());
        skipTransition.setInvisible(true);
        color(skipTransition);

        for (Place p_init : initialMarking) {
            Arc edge = net.addArc(p_init, skipTransition);
            color(edge);
        }
        for (Place p_final : finalMarking) {
            Arc edge = net.addArc(skipTransition, p_final);
            color(edge);
        }
    }

    public AcceptingPetriNetWrapper connectConcurrent(LinkedList<AcceptingPetriNetWrapper> subnets) {
        Petrinet resultNet = new PetrinetImpl("Result net");

        AcceptingPetriNetWrapper firstNet = subnets.removeFirst();
        HashMap<Place, Place> actualPlaceToPlace = new HashMap<>();
        for (Place place : firstNet.getNet().getPlaces()) {
            actualPlaceToPlace.put(place, resultNet.addPlace(place.getLabel()));
        }

        HashMap<Transition, Transition> actualTransitionToTransition = new HashMap<>();
        for (Transition transition : firstNet.getNet().getTransitions()) {
            actualTransitionToTransition.put(transition, resultNet.addTransition(transition.getLabel()));
            actualTransitionToTransition.get(transition).setInvisible(transition.isInvisible());
        }

        for (Place place : actualPlaceToPlace.keySet()) {
            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : firstNet.getNet().getInEdges(place)) {
                resultNet.addArc(actualTransitionToTransition.get((Transition) inEdge.getSource()), actualPlaceToPlace.get((Place) inEdge.getTarget()));
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : firstNet.getNet().getOutEdges(place)) {
                resultNet.addArc(actualPlaceToPlace.get((Place) outEdge.getSource()), actualTransitionToTransition.get((Transition) outEdge.getTarget()));
            }
        }

        Marking initialMarking = new Marking();
        Place initialPlace = resultNet.addPlace("Init");
        initialMarking.add(initialPlace);
        Transition initialTau = resultNet.addTransition(TauNameGenerator.getInstance().getTauName());
        initialTau.setInvisible(true);
        resultNet.addArc(initialPlace, initialTau);
        for (Place place : firstNet.getInitialMarking()) {
            resultNet.addArc(initialTau, actualPlaceToPlace.get(place));
        }

        Marking finalMarking = new Marking();
        Place finalPlace = resultNet.addPlace("Final");
        finalMarking.add(finalPlace);
        Transition finalTau = resultNet.addTransition(TauNameGenerator.getInstance().getTauName());
        finalTau.setInvisible(true);
        resultNet.addArc(finalTau, finalPlace);
        for (Place place : firstNet.getFinalMarking()) {
            resultNet.addArc(actualPlaceToPlace.get(place), finalTau);
        }

        AcceptingPetriNetWrapper actualResult = new AcceptingPetriNetWrapper(resultNet, initialMarking, finalMarking, firstNet.getParameters());

        for (AcceptingPetriNetWrapper dResult : subnets) {
            appendResultConcurrently(actualResult, dResult, initialTau, finalTau);
        }

        actualResult.polishNet();

        return actualResult;
    }

    private void appendResultConcurrently(AcceptingPetriNetWrapper actualResult, AcceptingPetriNetWrapper
            resultToAppend, Transition initialTau, Transition finalTau) {
        HashMap<Place, Place> placeToPlace = new HashMap<>();
        for (Place place : resultToAppend.getNet().getPlaces()) {
            placeToPlace.put(place, actualResult.getNet().addPlace(place.getLabel()));
        }
        HashMap<Transition, Transition> transitionToTransition = new HashMap<>();
        for (Transition transition : resultToAppend.getNet().getTransitions()) {
            transitionToTransition.put(transition, actualResult.getNet().addTransition(transition.getLabel()));
            transitionToTransition.get(transition).setInvisible(transition.isInvisible());
        }

        for (Place place : placeToPlace.keySet()) {
            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : resultToAppend.getNet().getInEdges(place)) {
                actualResult.getNet().addArc(transitionToTransition.get((Transition) inEdge.getSource()), placeToPlace.get((Place) inEdge.getTarget()));
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : resultToAppend.getNet().getOutEdges(place)) {
                actualResult.getNet().addArc(placeToPlace.get((Place) outEdge.getSource()), transitionToTransition.get((Transition) outEdge.getTarget()));
            }
        }

        for (Place place : resultToAppend.getInitialMarking()) {
            actualResult.getNet().addArc(initialTau, placeToPlace.get(place));
        }

        for (Place place : resultToAppend.getFinalMarking()) {
            actualResult.getNet().addArc(placeToPlace.get(place), finalTau);
        }
    }

    public AcceptingPetriNetWrapper connectExclusiveChoice(LinkedList<AcceptingPetriNetWrapper> subnets) {
        Petrinet resultNet = new PetrinetImpl("Result net");

        AcceptingPetriNetWrapper firstNet = subnets.removeFirst();
        HashMap<Place, Place> actualPlaceToPlace = new HashMap<>();
        for (Place place : firstNet.getNet().getPlaces()) {
            actualPlaceToPlace.put(place, resultNet.addPlace(place.getLabel()));
        }

        HashMap<Transition, Transition> actualTransitionToTransition = new HashMap<>();
        for (Transition transition : firstNet.getNet().getTransitions()) {
            actualTransitionToTransition.put(transition, resultNet.addTransition(transition.getLabel()));
            actualTransitionToTransition.get(transition).setInvisible(transition.isInvisible());
        }

        for (Place place : actualPlaceToPlace.keySet()) {
            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : firstNet.getNet().getInEdges(place)) {
                resultNet.addArc(actualTransitionToTransition.get((Transition) inEdge.getSource()), actualPlaceToPlace.get((Place) inEdge.getTarget()));
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : firstNet.getNet().getOutEdges(place)) {
                resultNet.addArc(actualPlaceToPlace.get((Place) outEdge.getSource()), actualTransitionToTransition.get((Transition) outEdge.getTarget()));
            }
        }

        Marking initialMarking = new Marking();
        Place initialPlace = resultNet.addPlace("Init");
        initialMarking.add(initialPlace);
        Transition initialTau = resultNet.addTransition(TauNameGenerator.getInstance().getTauName());
        initialTau.setInvisible(true);
        resultNet.addArc(initialPlace, initialTau);
        for (Place place : firstNet.getInitialMarking()) {
            resultNet.addArc(initialTau, actualPlaceToPlace.get(place));
        }

        Marking finalMarking = new Marking();
        Place finalPlace = resultNet.addPlace("Final");
        finalMarking.add(finalPlace);
        Transition finalTau = resultNet.addTransition(TauNameGenerator.getInstance().getTauName());
        finalTau.setInvisible(true);
        resultNet.addArc(finalTau, finalPlace);
        for (Place place : firstNet.getFinalMarking()) {
            resultNet.addArc(actualPlaceToPlace.get(place), finalTau);
        }

        AcceptingPetriNetWrapper actualResult = new AcceptingPetriNetWrapper(resultNet, initialMarking, finalMarking, firstNet.getParameters());

        for (AcceptingPetriNetWrapper dResult : subnets) {
            appendResultXOr(actualResult, dResult, initialTau, finalTau);
        }

        actualResult.polishNet();

        return actualResult;
    }

    private void appendResultXOr(AcceptingPetriNetWrapper actualResult, AcceptingPetriNetWrapper
            resultToAppend, Transition
                                         initialTau, Transition finalTau) {
        HashMap<Place, Place> placeToPlace = new HashMap<>();
        for (Place place : resultToAppend.getNet().getPlaces()) {
            placeToPlace.put(place, actualResult.getNet().addPlace(place.getLabel()));
        }
        HashMap<Transition, Transition> transitionToTransition = new HashMap<>();
        for (Transition transition : resultToAppend.getNet().getTransitions()) {
            transitionToTransition.put(transition, actualResult.getNet().addTransition(transition.getLabel()));
            transitionToTransition.get(transition).setInvisible(transition.isInvisible());
        }

        for (Place place : placeToPlace.keySet()) {
            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : resultToAppend.getNet().getInEdges(place)) {
                actualResult.getNet().addArc(transitionToTransition.get((Transition) inEdge.getSource()), placeToPlace.get((Place) inEdge.getTarget()));
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : resultToAppend.getNet().getOutEdges(place)) {
                actualResult.getNet().addArc(placeToPlace.get((Place) outEdge.getSource()), transitionToTransition.get((Transition) outEdge.getTarget()));
            }
        }

        Transition initialChoice = actualResult.getNet().addTransition(TauNameGenerator.getInstance().getTauName());
        initialChoice.setInvisible(true);
        for (Place place : actualResult.getInitialMarking()) {
            actualResult.getNet().addArc(place, initialChoice);
        }

        for (Place place : resultToAppend.getInitialMarking()) {
            actualResult.getNet().addArc(initialChoice, placeToPlace.get(place));
        }

        Transition finalChoice = actualResult.getNet().addTransition(TauNameGenerator.getInstance().getTauName());
        finalChoice.setInvisible(true);
        for (Place place : resultToAppend.getFinalMarking()) {
            actualResult.getNet().addArc(placeToPlace.get(place), finalChoice);
        }

        for (Place place : actualResult.getFinalMarking()) {
            actualResult.getNet().addArc(finalChoice, place);
        }
    }

    public AcceptingPetriNetWrapper connectAnd(LinkedList<AcceptingPetriNetWrapper> subnets) {
        Petrinet resultNet = new PetrinetImpl("Result net");

        AcceptingPetriNetWrapper firstNet = subnets.removeFirst();
        HashMap<Place, Place> actualPlaceToPlace = new HashMap<>();
        for (Place place : firstNet.getNet().getPlaces()) {
            actualPlaceToPlace.put(place, resultNet.addPlace(place.getLabel()));
        }

        HashMap<Transition, Transition> actualTransitionToTransition = new HashMap<>();
        for (Transition transition : firstNet.getNet().getTransitions()) {
            actualTransitionToTransition.put(transition, resultNet.addTransition(transition.getLabel()));
            actualTransitionToTransition.get(transition).setInvisible(transition.isInvisible());
        }

        for (Place place : actualPlaceToPlace.keySet()) {
            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : firstNet.getNet().getInEdges(place)) {
                resultNet.addArc(actualTransitionToTransition.get((Transition) inEdge.getSource()), actualPlaceToPlace.get((Place) inEdge.getTarget()));
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : firstNet.getNet().getOutEdges(place)) {
                resultNet.addArc(actualPlaceToPlace.get((Place) outEdge.getSource()), actualTransitionToTransition.get((Transition) outEdge.getTarget()));
            }
        }

        Marking initialMarking = new Marking();
        Place initialPlace = resultNet.addPlace("Init");
        initialMarking.add(initialPlace);
        Transition initialTau = resultNet.addTransition(TauNameGenerator.getInstance().getTauName());
        initialTau.setInvisible(true);
        resultNet.addArc(initialPlace, initialTau);
        for (Place place : firstNet.getInitialMarking()) {
            resultNet.addArc(initialTau, actualPlaceToPlace.get(place));
        }

        Marking finalMarking = new Marking();
        Place finalPlace = resultNet.addPlace("Final");
        finalMarking.add(finalPlace);
        Transition finalTau = resultNet.addTransition(TauNameGenerator.getInstance().getTauName());
        finalTau.setInvisible(true);
        resultNet.addArc(finalTau, finalPlace);
        for (Place place : firstNet.getFinalMarking()) {
            resultNet.addArc(actualPlaceToPlace.get(place), finalTau);
        }

        AcceptingPetriNetWrapper actualResult = new AcceptingPetriNetWrapper(resultNet, initialMarking, finalMarking, firstNet.getParameters());

        for (AcceptingPetriNetWrapper dResult : subnets) {
            appendResultAnd(actualResult, dResult, initialTau, finalTau);
        }

        actualResult.polishNet();

        return actualResult;
    }

    private void appendResultAnd(AcceptingPetriNetWrapper actualResult, AcceptingPetriNetWrapper
            resultToAppend, Transition
                                         initialTau, Transition finalTau) {
        HashMap<Place, Place> placeToPlace = new HashMap<>();
        for (Place place : resultToAppend.getNet().getPlaces()) {
            placeToPlace.put(place, actualResult.getNet().addPlace(place.getLabel()));
        }
        HashMap<Transition, Transition> transitionToTransition = new HashMap<>();
        for (Transition transition : resultToAppend.getNet().getTransitions()) {
            transitionToTransition.put(transition, actualResult.getNet().addTransition(transition.getLabel()));
            transitionToTransition.get(transition).setInvisible(transition.isInvisible());
        }

        for (Place place : placeToPlace.keySet()) {
            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : resultToAppend.getNet().getInEdges(place)) {
                actualResult.getNet().addArc(transitionToTransition.get((Transition) inEdge.getSource()), placeToPlace.get((Place) inEdge.getTarget()));
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : resultToAppend.getNet().getOutEdges(place)) {
                actualResult.getNet().addArc(placeToPlace.get((Place) outEdge.getSource()), transitionToTransition.get((Transition) outEdge.getTarget()));
            }
        }

        initialTau.setInvisible(true);
        for (Place place : actualResult.getInitialMarking()) {
            actualResult.getNet().addArc(place, initialTau);
        }

        for (Place place : resultToAppend.getInitialMarking()) {
            actualResult.getNet().addArc(initialTau, placeToPlace.get(place));
        }

        finalTau.setInvisible(true);
        for (Place place : resultToAppend.getFinalMarking()) {
            actualResult.getNet().addArc(placeToPlace.get(place), finalTau);
        }

        for (Place place : actualResult.getFinalMarking()) {
            actualResult.getNet().addArc(finalTau, place);
        }
    }

    public AcceptingPetriNetWrapper connectSequentially(LinkedList<AcceptingPetriNetWrapper> subnets) {
        Petrinet resultNet = new PetrinetImpl("Result net");

        AcceptingPetriNetWrapper firstNet = subnets.removeFirst();
        HashMap<Place, Place> actualPlaceToPlace = new HashMap<>();
        for (Place place : firstNet.getNet().getPlaces()) {
            actualPlaceToPlace.put(place, resultNet.addPlace(place.getLabel()));
        }

        HashMap<Transition, Transition> actualTransitionToTransition = new HashMap<>();
        for (Transition transition : firstNet.getNet().getTransitions()) {
            actualTransitionToTransition.put(transition, resultNet.addTransition(transition.getLabel()));
            actualTransitionToTransition.get(transition).setInvisible(transition.isInvisible());
        }

        for (Place place : actualPlaceToPlace.keySet()) {
            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : firstNet.getNet().getInEdges(place)) {
                resultNet.addArc(actualTransitionToTransition.get((Transition) inEdge.getSource()), actualPlaceToPlace.get((Place) inEdge.getTarget()));
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : firstNet.getNet().getOutEdges(place)) {
                resultNet.addArc(actualPlaceToPlace.get((Place) outEdge.getSource()), actualTransitionToTransition.get((Transition) outEdge.getTarget()));
            }
        }

        Marking initialMarking = new Marking();
        for (Place place : firstNet.getInitialMarking()) {
            initialMarking.add(actualPlaceToPlace.get(place));
        }

        Marking finalMarking = new Marking();
        for (Place place : firstNet.getFinalMarking()) {
            finalMarking.add(actualPlaceToPlace.get(place));
        }

        AcceptingPetriNetWrapper actualResult = new AcceptingPetriNetWrapper(resultNet, initialMarking, finalMarking, firstNet.getParameters());
        actualResult.polishNet();

        for (AcceptingPetriNetWrapper dResult : subnets) {
            appendResultSequentially(actualResult, dResult);
            actualResult.polishNet();
        }


        return actualResult;
    }

    public AcceptingPetriNetWrapper connectLooping(LinkedList<AcceptingPetriNetWrapper> subnets) {
        AcceptingPetriNetWrapper connectedNet = subnets.removeFirst();

        for (AcceptingPetriNetWrapper subnet : subnets) {
            appendResultLooping(connectedNet, subnet);
        }

        return connectedNet;
    }

    public void appendResultLooping(AcceptingPetriNetWrapper actualResult, AcceptingPetriNetWrapper resultToAppend) {
        HashMap<PetrinetNode, Place> placeToPlace = new HashMap<>();
        HashMap<PetrinetNode, Transition> transitionToTransition = new HashMap<>();

        for (Place place : resultToAppend.getNet().getPlaces()) {
            Place currPlace = actualResult.getNet().addPlace(place.getLabel());
            placeToPlace.put(place, currPlace);
        }

        for (Transition transition : resultToAppend.getNet().getTransitions()) {
            Transition currTransition = actualResult.getNet().addTransition(transition.getLabel());
            currTransition.setInvisible(transition.isInvisible());
            transitionToTransition.put(transition, currTransition);

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : resultToAppend.getNet().getInEdges(transition)) {
                actualResult.getNet().addArc(placeToPlace.get(inEdge.getSource()), transitionToTransition.get(inEdge.getTarget()));
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : resultToAppend.getNet().getOutEdges(transition)) {
                actualResult.getNet().addArc(transitionToTransition.get(outEdge.getSource()), placeToPlace.get(outEdge.getTarget()));
            }
        }

        Transition silentInLoop = actualResult.getNet().addTransition("Redo-Start-" + TauNameGenerator.getInstance().getTauName());
        silentInLoop.setInvisible(true);
        for (Place place : actualResult.getFinalMarking()) {
            actualResult.getNet().addArc(place, silentInLoop);
        }
        for (Place place : resultToAppend.getInitialMarking()) {
            actualResult.getNet().addArc(silentInLoop, placeToPlace.get(place));
        }

        Transition silentOutLoop = actualResult.getNet().addTransition("Redo-End-" + TauNameGenerator.getInstance().getTauName());
        silentOutLoop.setInvisible(true);
        for (Place place : actualResult.getInitialMarking()) {
            actualResult.getNet().addArc(silentOutLoop, place);
        }
        for (Place place : resultToAppend.getFinalMarking()) {
            actualResult.getNet().addArc(placeToPlace.get(place), silentOutLoop);
        }
    }

    private void appendResultSequentially(AcceptingPetriNetWrapper actualResult, AcceptingPetriNetWrapper
            resultToAppend) {
        HashMap<Place, Place> placeToPlace = new HashMap<>();
        for (Place place : resultToAppend.getNet().getPlaces()) {
            placeToPlace.put(place, actualResult.getNet().addPlace(place.getLabel()));
        }
        HashMap<Transition, Transition> transitionToTransition = new HashMap<>();
        for (Transition transition : resultToAppend.getNet().getTransitions()) {
            transitionToTransition.put(transition, actualResult.getNet().addTransition(transition.getLabel()));
            transitionToTransition.get(transition).setInvisible(transition.isInvisible());
        }

        for (Place place : placeToPlace.keySet()) {
            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : resultToAppend.getNet().getInEdges(place)) {
                actualResult.getNet().addArc(transitionToTransition.get((Transition) inEdge.getSource()), placeToPlace.get((Place) inEdge.getTarget()));
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : resultToAppend.getNet().getOutEdges(place)) {
                actualResult.getNet().addArc(placeToPlace.get((Place) outEdge.getSource()), transitionToTransition.get((Transition) outEdge.getTarget()));
            }
        }

        Transition connectingTransition = actualResult.getNet().addTransition(TauNameGenerator.getInstance().getTauName());
        connectingTransition.setInvisible(true);

        for (Place place : actualResult.getFinalMarking()) {
            actualResult.getNet().addArc(place, connectingTransition);
        }
        for (Place place : resultToAppend.getInitialMarking()) {
            actualResult.getNet().addArc(connectingTransition, placeToPlace.get(place));
        }

        Marking newFinalMarking = new Marking();
        for (Place place : resultToAppend.getFinalMarking()) {
            newFinalMarking.add(placeToPlace.get(place));
        }

        actualResult.replaceFinalMarking(newFinalMarking);
    }

    public AcceptingPetriNetImpl mergePlaces(org.processmining.acceptingpetrinet.models.AcceptingPetriNet
                                                     net, ArrayList<Place> placesToBeMerged) {
        Petrinet newNet = new PetrinetImpl("Merged Accepting Petri net");
        HashMap<Transition, Transition> transitionTransition = new HashMap<>();
        HashMap<Place, Place> placeToPlace = new HashMap<>();

        for (Transition transition : net.getNet().getTransitions()) {
            Transition newTransition = newNet.addTransition(transition.getLabel());
            transitionTransition.put(transition, newTransition);
            newTransition.setInvisible(transition.isInvisible());
            preserveColor(newTransition, transition);
        }

        Place mergingPlace = newNet.addPlace("Merging place");

        for (Place place : net.getNet().getPlaces()) {
            Place correspondingPlace;
            if (placesToBeMerged.contains(place)) {
                correspondingPlace = mergingPlace;
            } else {
                correspondingPlace = newNet.addPlace(place.getLabel());
                preserveColor(correspondingPlace, place);
            }
            placeToPlace.put(place, correspondingPlace);

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : net.getNet().getInEdges(place)) {
                Transition source = transitionTransition.get((Transition) inEdge.getSource());
                preserveColor(newNet.addArc(source, correspondingPlace), inEdge);
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : net.getNet().getOutEdges(place)) {
                Transition target = transitionTransition.get((Transition) outEdge.getTarget());
                preserveColor(newNet.addArc(correspondingPlace, target), outEdge);
            }
        }

        Marking newInitialMarking = new Marking();
        for (Place place : net.getInitialMarking()) {
            Place correspondingPlace = placeToPlace.get(place);
            if (!newInitialMarking.contains(correspondingPlace)) {
                newInitialMarking.add(correspondingPlace);
            }
        }

        Marking newFinalMarking = new Marking();
        for (Place place : net.getFinalMarkings().iterator().next()) {
            Place correspondingPlace = placeToPlace.get(place);
            if (!newFinalMarking.contains(correspondingPlace)) {
                newFinalMarking.add(correspondingPlace);
            }
        }

        lastPlaceToPlace = placeToPlace;

        return new AcceptingPetriNetImpl(newNet, newInitialMarking, newFinalMarking);
    }

    public void fixMurataColors(Petrinet petrinet) {
        for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : petrinet.getEdges()) {
            AttributeMap inEdgeAttributeMap = edge.getSource().getAttributeMap();
            AttributeMap outEdgeAttributeMap = edge.getTarget().getAttributeMap();

            if (inEdgeAttributeMap.containsKey(AttributeMap.STROKECOLOR)) {
                Color colorSourceNode = (Color) inEdgeAttributeMap.get(AttributeMap.STROKECOLOR);
                if (!colorToId.containsKey(colorSourceNode)) {
                    continue;
                }

                int colorSourceNodeId = colorToId.get(colorSourceNode);

                if (outEdgeAttributeMap.containsKey(AttributeMap.STROKECOLOR)) {
                    Color colorTargetNode = (Color) outEdgeAttributeMap.get(AttributeMap.STROKECOLOR);
                    if (!colorToId.containsKey(colorTargetNode)) {
                        color(edge, lightColors.get(colorSourceNodeId));
                    }

                    int colorTargetNodeId = colorToId.get(colorTargetNode);

                    if (colorSourceNodeId == colorTargetNodeId || lightColors.contains(colorTargetNode)) {
                        color(edge, colors.get(colorSourceNodeId));
                    } else {
                        color(edge, lightColors.get(colorSourceNodeId));
                    }
                } else {
                    color(edge, lightColors.get(colorSourceNodeId));
                }

            }
        }
    }

    public void removeUselessSilentTransitions(AcceptingPetriNet net) {
        Collection<Transition> silentTransitionsToBeRemoved = new HashSet<>();

        for (Transition transition : net.getNet().getTransitions()) {
            if (transition.isInvisible()) {
                Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = net.getNet().getInEdges(transition);
                Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = net.getNet().getOutEdges(transition);

                boolean equalEdges = true;
                for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : inEdges) {
                    if (net.getNet().getArc(inEdge.getTarget(), inEdge.getSource()) == null) {
                        equalEdges = false;
                        break;
                    }
                }

                if (inEdges.size() == outEdges.size() && equalEdges) {
                    silentTransitionsToBeRemoved.add(transition);
                }
            }
        }

        for (Transition transition : silentTransitionsToBeRemoved) {
            removeTransition(net, transition);
        }
    }

    public void removeTransition(AcceptingPetriNet net, Transition transition) {
        net.getNet().getInEdges(transition).forEach(inEdge -> net.getNet().removeEdge(inEdge));
        net.getNet().getOutEdges(transition).forEach(outEdge -> net.getNet().removeEdge(outEdge));
        net.getNet().removeTransition(transition);
    }
}
