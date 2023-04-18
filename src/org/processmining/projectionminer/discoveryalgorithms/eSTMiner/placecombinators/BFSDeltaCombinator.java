package org.processmining.projectionminer.discoveryalgorithms.eSTMiner.placecombinators;

import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyLog;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyPlace;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyProcessModel;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.plugins.PlugInStatistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class BFSDeltaCombinator {
    private final int deltaAbsolute;
    private final int tauAbsolute;
    private final MyLog log;
    private final int[] traceCounts;
    private final int maxDepth;
    private final String chosenAdaptiveDeltaStrategy;
    private final int adaptiveDeltaSteepness;
    private final int potentialPlacesLimit;
    private int currentDepth;
    private String adaptiveDeltaComputationStrategySummary;

    public BFSDeltaCombinator(String adaptiveDeltaStrategy, int adaptiveDeltaSteepness, int tauAbsolute, int deltaAbsolute, int[] traceCounts, int maxDepth, int potentialPlacesLimit, MyLog log) {
        this.tauAbsolute = tauAbsolute;
        this.deltaAbsolute = deltaAbsolute;
        this.traceCounts = traceCounts;
        this.currentDepth = 2;
        this.maxDepth = maxDepth;
        this.adaptiveDeltaComputationStrategySummary = "";
        this.potentialPlacesLimit = potentialPlacesLimit;
        this.log = log;
        this.chosenAdaptiveDeltaStrategy = adaptiveDeltaStrategy;
        this.adaptiveDeltaSteepness = adaptiveDeltaSteepness;
    }

    //TODO replace trace vector arrays by binary number for increased speed! also, call them variantVectors

    //tries adding the given place to the given PM, both expressed by their variant vectors.
    //uses (a possibly modified) delta and tau for decision
    // Returns -1 if generally impossible, 0 if currently impossible, 1 if possible (with resulting vector)

    //Note: statistics are changed, wherethe places are actually discarded/added/etc (discovery)
    public Object[] combinePlace(boolean[] pMVariantVector, MyPlace place) {
        boolean[] placeVariantVector = place.getVariantVector();
        int previousFittingTraces = countFittingTraces(pMVariantVector);
        boolean[] variantVectorIfCombined = computeVariantVectorIfCombined(pMVariantVector, placeVariantVector);
        int remainingFittingTraces = countFittingTraces(variantVectorIfCombined);
        if (remainingFittingTraces < tauAbsolute) {// adding the place reduces the global fitness below the threshold tau, discard
            return new Object[]{-1, variantVectorIfCombined};
        }
        int diff = (previousFittingTraces - remainingFittingTraces); //when adding place, PM will have diff less fitting traces
        //set the chosen adaptive delta strategy to compute the delta needed to decide on acceptance or delay of place
        float adaptiveDelta = 0;

        switch (chosenAdaptiveDeltaStrategy) {
            case "Sigmoid":
                adaptiveDelta = getAdaptedDeltaSigmoid(place);
                break;
            case "MaxDelta":
                adaptiveDelta = getAdaptedDeltaConstant(); // always equals delta
                break;
            case "NoDelta":
                adaptiveDelta = getAdaptedDeltaNoDelta(); //max integer (allows adding everything)
                break;
            case "Linear":
                adaptiveDelta = getAdaptedDeltaLinear(place);
                break;
            default:
                System.out.println("WARNING - Combinator: no valid delta adaption detected!");
                break;
        }
        //System.out.println("Check addability of place: "+place.toBinaryString());
        if (diff > adaptiveDelta) {//adding the place now would remove more than (adaptive) delta fitting traces, delay place
//			System.out.println("Place "+ place.toBinaryString()+ " delayed with adaptive delta "+adaptiveDelta+ "(modifier:" + adaptiveDelta/deltaAbsolute+ ")" );
            return new Object[]{0, variantVectorIfCombined};
        }
        //place can be added now
        //System.out.println(diff+ "<" +adaptiveDelta + " (place added)");
//		System.out.println("Place "+ place.toBinaryString()+ " added with adaptive delta "+adaptiveDelta+ "(modifier:" + adaptiveDelta/deltaAbsolute+ "\")" );
//		printAdaptiveDeltaComputation(place);
        return new Object[]{1, variantVectorIfCombined};             //accept place
    }


    //computes the variantVector resulting from adding the new VV to the current VV (and - gate)
    private boolean[] computeVariantVectorIfCombined(boolean[] currentVV, boolean[] addedVV) {
        boolean[] resultVV = currentVV.clone();
        for (int i = 0; i < addedVV.length; i++) {
            if (!addedVV[i]) {
                resultVV[i] = false;
            }
        }
        return resultVV;
    }


    //use traceCounts to get the actual number of traces for each variant
    private int countFittingTraces(boolean[] traceVector) {
        int count = 0;
        for (int i = 0; i < traceVector.length; i++) {
            if (traceVector[i]) {
                count = count + this.traceCounts[i];
            }
        }
        return count;
    }


    //can be used to sort places in the queue, e.g. by interest score
    private ArrayList<MyPlace> sortPotentialPlaces(ArrayList<MyPlace> places) {
        Comparator<MyPlace> comp = new PlaceComparatorNumOfTransitions();//comparator based on number of transition
//		Comparator<MyPlace> comp = new PlaceComparatorFittingTraces(); // comparator based on number of fitting races
        ArrayList<MyPlace> result = places;
        result.sort(comp);
        return result;
    }

    public int getCurrentDepth() {
        return currentDepth;
    }

    public void setCurrentDepth(int newDepth) {
        this.currentDepth = newDepth;
//		System.out.println("Combinator current depth set to "+currentDepth);
    }

    //shortens potential places of the given PM with respect to the given PM threshold
    public MyProcessModel shortenPotentialPlaces(MyProcessModel pM) {
        pM.setPotentialPlaces(shortenPotentialPlaces(pM.getPotentialPlaces()));
        return pM;
    }

    //shorten given places list with respect to the limit
    public ArrayList<MyPlace> shortenPotentialPlaces(ArrayList<MyPlace> potentialPlaces) {
        int lengthBeforeShortening = potentialPlaces.size();
        if (lengthBeforeShortening > this.potentialPlacesLimit) {
            potentialPlaces = sortPotentialPlaces(potentialPlaces);
            potentialPlaces = new ArrayList<MyPlace>(potentialPlaces.subList(0, this.potentialPlacesLimit));
            PlugInStatistics.instance().incSkippedPlaces((lengthBeforeShortening - potentialPlaces.size()));
        }
        return potentialPlaces;
    }

    //revisits the PRE-SORTED queue of potential places ONCE and trys adding them to model
    public MyProcessModel revisitQueueOfPlaces(MyProcessModel pM) {
/*		MyProcessModel resultPM = new MyProcessModel(inputPM.getPlaces(), inputPM.getTransitions(), inputPM.getVariantVector().length);
        resultPM.setDiscardedPlaces(inputPM.getDiscardedPlaces());
        ArrayList<MyPlace> currentPotentialPlaces = inputPM.getPotentialPlaces();
        ArrayList<MyPlace> nextPotentialPlaces = new ArrayList<MyPlace>();
*/
        ArrayList<MyPlace> currentPotentialPlaces = pM.getPotentialPlaces();
        boolean[] currentPMVariantVector = pM.getVariantVector();
        pM.setPotentialPlaces(new ArrayList<MyPlace>());
        int replayableBeforeAdding = countFittingTraces(currentPMVariantVector);
        while (!currentPotentialPlaces.isEmpty()) {
            MyPlace place = currentPotentialPlaces.remove(0);
            Object[] combinationResults = combinePlace(currentPMVariantVector, place);
            if ((int) combinationResults[0] == -1) {// discard place
                pM.getDiscardedPlaces().add(place);
                PlugInStatistics.instance().incDiscardedPlaces(1);
            } else if ((int) combinationResults[0] == 0) { //delay place
                pM.getPotentialPlaces().add(place);
                PlugInStatistics.instance().incDelayedPlaces(1);
            } else if ((int) combinationResults[0] == 1) { //add place
                pM.addPlace(place);
                PlugInStatistics.instance().incAcceptedPlaces(1);
            }
        }
        pM.updateStatus(log);
        System.out.println("Replayable traces: " + replayableBeforeAdding + " --> " + countFittingTraces(pM.getVariantVector()));
        return pM;
    }

    //for debugging. returns a list of depths
    private String depthsOfPotentialPlaces(ArrayList<MyPlace> potentialPlaces) {
        String result = "";
        for (MyPlace place : potentialPlaces) {
            result = result + getPlaceDepth(place) + ", ";
        }
        return result;
    }


    //--------------- adaptive delta----------------------


    //return a non-adapted delta (always use maximum  delta)
    private float getAdaptedDeltaConstant() {
        this.adaptiveDeltaComputationStrategySummary = "Delta Strategy: MaxDelta = " + deltaAbsolute;
        return deltaAbsolute;
    }


    //return a large number to simulate non-existance of delta (nullification of delta, no limitation of fitness decrease)
    private float getAdaptedDeltaNoDelta() {
        this.adaptiveDeltaComputationStrategySummary = "Delta Strategy: NoDelta - Add every fitting place, while preserving global fitness.";
        return Integer.MAX_VALUE;
    }


    // these LINEAR functions can be used to modify delta dynamically
    // parameterized based on place depth
    // adapted delta between 0 (no deviation allowed) and delta (max deviation allowed)
    // prefer low depths (steepness and rightshift based on depth)
    private float getAdaptedDeltaLinear(MyPlace place) {
        float placeDepth = getPlaceDepth(place);
        float steepness = (float) adaptiveDeltaSteepness / placeDepth; //lower place depth results in steeper function
        float rightShift = placeDepth; // every place starts at 0
        //linear modification factor computation
        float modificationFactor = steepness * ((float) this.currentDepth - rightShift) / ((float) this.maxDepth - 2); //-2 because we start at level 2
        //describe what happens for potential debugging
        this.adaptiveDeltaComputationStrategySummary = "Linear Adaptive Delta Modifier: \n"
                + "steepness*(currentDepth - placeDepth)/(maxDepth-2)) = "
                + steepness + "*(" + this.currentDepth + "-" + placeDepth + ")/(" + this.maxDepth + "-2))= " + modificationFactor;
        return deltaAbsolute * modificationFactor;
    }


    // these SIGMOID functions can be used to modify delta dynamically
    // parameterized based on place depth
    // adapted delta between 0 (no deviation allowed) and delta (max deviation allowed)
    // prefer low depths (steepness and rightshift based on depth)
    private float getAdaptedDeltaSigmoid(MyPlace place) {
        float placeDepth = getPlaceDepth(place);
        float steepness = (float) adaptiveDeltaSteepness / placeDepth; //increase for steeper function (simple places get more steepness)
        float rightShift = placeDepth; // every place starts at 0
        //sigmoid modification factor computation
        float f = (-1) * steepness * ((float) this.currentDepth - rightShift);
        float modificationFactor = (2 / (1 + (float) Math.exp(f)) - 1); //2/(...)-1 results ins maximum 1, minimum 0, with proper behaviour in between
        //describe what happens for potential debugging
        this.adaptiveDeltaComputationStrategySummary = "Sigmoid Adaptive Delta Modifier: "
                + "2/(1+exp(f))+1, with \n"
                + "f = steepness*(currentDepth - placeDepth)/(maxDepth-2)) \n = "
                + steepness + "(" + this.currentDepth + "-" + placeDepth + ")/(" + this.maxDepth + "-2))= " + modificationFactor;
        return deltaAbsolute * modificationFactor;
    }


    private void printAdaptiveDeltaComputation(MyPlace place) {
        System.out.println(this.adaptiveDeltaComputationStrategySummary);
    }


    //Utility Methods_______________________________________________________________________________________________

    //sum of place transitions
    private int getPlaceDepth(MyPlace place) {
        return (Integer.bitCount(place.getInputTrKey()) + Integer.bitCount(place.getOutputTrKey()));
    }

    private String placeToNamedString(final MyPlace p, final String[] transitions) {
        return getTransitionNames(p.getInputTrKey(), transitions).toString() + "|"
                + getTransitionNames(p.getOutputTrKey(), transitions).toString() + ",";
    }

    //returns a collection containing all transitions names from the given transitions array
    private Collection<String> getTransitionNames(final int key, final String[] transitions) {
        Collection<String> result = new ArrayList<String>();
        if (key > (Math.pow(2, transitions.length))) {
            return null;
        }
        for (int i = 0; i < transitions.length; i++) {
            if ((key & getMask(i, transitions)) > 0) { //test key for ones
                result.add(transitions[i]);
            }
        }

        return result;
    }

    //return the transitions corresponding to the given key
    private Collection<String> getTransitions(final int key, final MyProcessModel pM) {
        Collection<String> result = new ArrayList<String>();
        if (key > Math.pow(2, pM.getTransitions().length)) {
            return null;
        }
        for (int i = 0; i < pM.getTransitions().length; i++) {
            if ((key & getMask(i, pM.getTransitions())) > 0) { //test key for ones
                result.add(pM.getTransitions()[i]);
            }
        }
        return result;
    }


    //return bitmask corresponding to position in the transition array
    private int getMask(final int position, final String[] transitions) {
        return (1 << (transitions.length - 1 - position));
    }

    public int getMaxDepth() {
        return maxDepth;
    }


}
