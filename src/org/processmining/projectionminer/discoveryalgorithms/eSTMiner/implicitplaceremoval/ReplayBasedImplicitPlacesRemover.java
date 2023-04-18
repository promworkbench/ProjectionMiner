package org.processmining.projectionminer.discoveryalgorithms.eSTMiner.implicitplaceremoval;

import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyLog;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyPlace;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyProcessModel;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.plugins.PlugInStatistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;


public class ReplayBasedImplicitPlacesRemover extends AbstractImplicitPlacesRemover {
    int maxTreeDepth;
    private HashMap<ArrayList<Integer>, Integer> traceVariantCounts;

    public ReplayBasedImplicitPlacesRemover(String[] transitions, MyLog log, int maxTreeDepth) {
        super(transitions, log);
        this.maxTreeDepth = maxTreeDepth;
    }


    //--------------Main Remover Methods------------------------


    //trys to remove all IPs in the given PM
    public MyProcessModel removeAllIPs(MyProcessModel inputPM, final ArrayList<ArrayList<Integer>> relevantTraceVariants) {
        long startTime = System.currentTimeMillis();
        long pauseTime = 0;
        long resumeTime = 0;
        inputPM.updateAndPrintStatus(log);
        ArrayList<MyPlace> placesToCheck = new ArrayList<MyPlace>(inputPM.getPlaces());
        ArrayList<MyPlace> implicitPlaces = new ArrayList<MyPlace>();
        ArrayList<MyPlace> remainingPlaces = inputPM.getPlaces();
//		System.out.println("\n _________________________________Remove ALL IPs_______________________________________________________ ");
//		System.out.println("\n"+ placesToCheck.size()+ " places  to be compared in total. ");
//		printDeadTransitions(computeTransitionsLiveness(relevantTraceVariants));
        while (placesToCheck.size() > 0) {
            MyPlace specificPlace = placesToCheck.remove(0);
            pauseTime = System.currentTimeMillis(); // specific place IP removal takes its own time and adds it
            implicitPlaces.addAll(implicitRelatedToPlace(specificPlace, placesToCheck, relevantTraceVariants));
            resumeTime = System.currentTimeMillis(); // specific place IP removal takes its own time and adds it
            placesToCheck.removeAll(implicitPlaces);
        }
//		printPlaces(implicitPlaces);
        remainingPlaces.removeAll(implicitPlaces);
        inputPM.setPlaces(remainingPlaces);
        inputPM.updateAndPrintStatus(log);
//		System.out.println(" _________________________________End of Remove ALL IPs_______________________________________________________  \n");
        PlugInStatistics.instance().incTimeCandFind(System.currentTimeMillis() - startTime + (resumeTime - pauseTime));
        //NOTE: do not count IPs, because they are counted within the specific place IP removal!!!
        return inputPM;
    }


    //trys to remove all IPs in the given places list that are related to a specific given place
    public ArrayList<MyPlace> implicitRelatedToPlace(final MyPlace specificPlace, ArrayList<MyPlace> placesToCheck,
                                                     final ArrayList<ArrayList<Integer>> relevantTraceVariants) {
        long startTime = System.currentTimeMillis();
//		System.out.println("\n ________________________________________________________________________________________ ");
//		System.out.println("Check with respect to specific place: "+specificPlace.toTransitionsString(transitions));
//		printDeadTransitions(computeTransitionsLiveness(relevantTraceVariants));
//		System.out.println(placesToCheck.size()+ " to check: ");
//		printPlaces(placesToCheck);
        ArrayList<MyPlace> implicitPlaces = new ArrayList<MyPlace>();

        //if specificPlace does not have enough live transitions, it is implicit but cannot be used for IP identification
        if (hasToFewLiveConnections(specificPlace, relevantTraceVariants)) {
            implicitPlaces.add(specificPlace);
//			printDeadTransitions(computeTransitionsLiveness(relevantTraceVariants));
//			System.out.println("Remove place with too few live connections: "+specificPlace.toTransitionsString(transitions));
            PlugInStatistics.instance().incImplicitPlaces(implicitPlaces.size());
            PlugInStatistics.instance().incTimeImpTest(System.currentTimeMillis() - startTime);
            return implicitPlaces;
        }

        //if specific place is life, compare to related places
        ArrayList<MyPlace> relatedPlaces = getRelatedPlaces(specificPlace, placesToCheck);
        PlugInStatistics.instance().incComparisons(relatedPlaces.size());
        for (MyPlace place : relatedPlaces) {
            //if the place is dead, no need to check for implicitness
            if (hasToFewLiveConnections(place, relevantTraceVariants)) {
                implicitPlaces.add(place);
                printDeadTransitions(computeTransitionsLiveness(relevantTraceVariants));
//				System.out.println("Remove place with too few live connections: "+place.toTransitionsString(transitions));
            } else {
                switch (testSubregionRelation(specificPlace, place, relevantTraceVariants)) {//-1: specific < place, 0 : mixed relation, 1:specific > place, 2: specific = place
                    case -1: //specific is a subregion of place
//						System.out.println("Identified "+place.toTransitionsString(transitions)+" as implicit validated by "+specificPlace.toTransitionsString(transitions));
                        if (validateCombinedPlace(place, specificPlace) && !implicitPlaces.contains(place)) {
                            implicitPlaces.add(place);
                        }
                        break;
                    case 0: //no relation between specific & place
                        //					System.out.println("No place could be identified as implicit.");
                        break;
                    case 1: //specific is a superregion of place
//						System.out.println("Identified "+specificPlace.toTransitionsString(transitions)+" as implicit validated by "+place.toTransitionsString(transitions));
                        if (validateCombinedPlace(specificPlace, place) && !implicitPlaces.contains(specificPlace)) {
                            implicitPlaces.add(specificPlace);
                        }
                        break;
                    case 2: //specific and place have equal marking (theoretically, random place can be kept)
                        MyPlace toBeRemoved = pick_place_with_equal_markings(place, specificPlace, relevantTraceVariants);
                        implicitPlaces.add(toBeRemoved);
                        break;
                    default:
//						System.out.println("Places "+place.toTransitionsString(transitions)+" and " +specificPlace.toTransitionsString(transitions)+ " are not handled properly by IP-Remover!");
                        break;
                }
            }
        }
//		printPlaces(implicitPlaces);
        ArrayList<MyPlace> remaining = new ArrayList<MyPlace>(placesToCheck);
        remaining.add(specificPlace);
        remaining.removeAll(implicitPlaces);
//		System.out.println("\n -------------Summary:--------------");
//		System.out.println(implicitPlaces.size()+ " to be removed: ");
//		printPlaces(implicitPlaces);
//		System.out.println(remaining.size()+ " to be kept: ");
//		printPlaces(remaining);
//		System.out.println("\n");
        implicitPlaces = removeDuplicatePlaces(implicitPlaces); //for accurate statistics
        PlugInStatistics.instance().incImplicitPlaces(implicitPlaces.size());
        PlugInStatistics.instance().incTimeImpTest(System.currentTimeMillis() - startTime);
        return implicitPlaces;
    }

    //returns the list without duplicate places (based on transitions)
    private ArrayList<MyPlace> removeDuplicatePlaces(ArrayList<MyPlace> places) {
        ArrayList<MyPlace> uniquePlaces = new ArrayList<MyPlace>();
        while (!places.isEmpty()) {
            MyPlace current = places.remove(0);
            uniquePlaces.add(current);
            places = removePlace(current, places);
        }
        return uniquePlaces;
    }


    // returns the list without copies of current
    private ArrayList<MyPlace> removePlace(MyPlace current, ArrayList<MyPlace> places) {
        ArrayList<MyPlace> uniquePlaces = new ArrayList<MyPlace>(places);
        for (MyPlace place : places) {
            if (current.equals(place)) {// equals overdidden to compare transition sets
                uniquePlaces.remove(place);
            }
        }
        return uniquePlaces;
    }


    //trys to remove all IPs in the given PM while repairing by adding combined places, needs update for statistics
    public MyProcessModel removeAllIPsAndRepair(MyProcessModel inputPM, final ArrayList<ArrayList<Integer>> relevantTraceVariants) {
        System.out.println("ERROR: IP Removal with Repair not properly implemented!");
        inputPM.updateAndPrintStatus(log);
        ArrayList<MyPlace> placesToCheck = new ArrayList<MyPlace>(inputPM.getPlaces());
        ArrayList<MyPlace> implicitPlaces = new ArrayList<MyPlace>();
        ArrayList<MyPlace> repairPlaces = new ArrayList<MyPlace>();
        ArrayList<MyPlace> remainingPlacesForStatistics = new ArrayList<MyPlace>(inputPM.getPlaces());//for statistics
        System.out.println("\n _________________________________Remove ALL IPs_______________________________________________________ ");
        System.out.println("\n" + placesToCheck.size() + " places  to be compared in total. ");
        printDeadTransitions(computeTransitionsLiveness(relevantTraceVariants));
        while (placesToCheck.size() > 0) {
            MyPlace specificPlace = placesToCheck.remove(0);
            Object[] ipRemovalResult = implicitAndRepairRelatedToPlace(specificPlace, placesToCheck, relevantTraceVariants);
            implicitPlaces.addAll((Collection<? extends MyPlace>) ipRemovalResult[0]);
            repairPlaces.addAll((Collection<? extends MyPlace>) ipRemovalResult[1]);
            placesToCheck.removeAll((Collection<? extends MyPlace>) ipRemovalResult[0]);
            placesToCheck.addAll((Collection<? extends MyPlace>) ipRemovalResult[1]);
            remainingPlacesForStatistics.removeAll(implicitPlaces);
        }
        ArrayList<MyPlace> remainingPlaces = new ArrayList<MyPlace>(inputPM.getPlaces());
//		printPlaces(implicitPlaces);
        remainingPlaces.addAll(repairPlaces);
        remainingPlaces.removeAll(implicitPlaces); //make sure that repairplaces, that have later been identified as imlicit, are removed again
        inputPM.setPlaces(remainingPlaces);
        inputPM.updateAndPrintStatus(log);
        System.out.println(" _________________________________End of Remove ALL IPs_______________________________________________________  \n");
        return inputPM;
    }


    //returns the places implicit with respect to the given place as well as the repair places needed for this,  needs update for statistics
    public Object[] implicitAndRepairRelatedToPlace(final MyPlace specificPlace, ArrayList<MyPlace> placesToCheck,
                                                    final ArrayList<ArrayList<Integer>> relevantTraceVariants) {
        System.out.println("ERROR: IP Removal with Repair not properly implemented!");
        System.out.println("\n ________________________________________________________________________________________ ");
        System.out.println("Check with respect to specific place: " + specificPlace.toTransitionsString(transitions));
        printDeadTransitions(computeTransitionsLiveness(relevantTraceVariants));
        System.out.println(placesToCheck.size() + " to check: ");
        printPlaces(placesToCheck);
        ArrayList<MyPlace> implicitPlaces = new ArrayList<MyPlace>();
        ArrayList<MyPlace> repairPlaces = new ArrayList<MyPlace>();

        //if specificPlace does not have enough live transitions, it is implicit but cannot be used for IP identification
        if (hasToFewLiveConnections(specificPlace, relevantTraceVariants)) {
            implicitPlaces.add(specificPlace);
            printDeadTransitions(computeTransitionsLiveness(relevantTraceVariants));
            System.out.println("Remove place with too few live connections: " + specificPlace.toTransitionsString(transitions));
            PlugInStatistics.instance().incImplicitPlaces(implicitPlaces.size());
            Object[] res = {implicitPlaces, repairPlaces};
            return res;
        }

        //if specific place is life, compare to related places
        ArrayList<MyPlace> relatedPlaces = getRelatedPlaces(specificPlace, placesToCheck);
        PlugInStatistics.instance().incComparisons(relatedPlaces.size());
        for (MyPlace place : relatedPlaces) {
            //if the place is dead, no need to check for implicitness
            if (hasToFewLiveConnections(place, relevantTraceVariants)) {
                implicitPlaces.add(place);
                printDeadTransitions(computeTransitionsLiveness(relevantTraceVariants));
                System.out.println("Remove place with too few live connections: " + place.toTransitionsString(transitions));
            } else {
                switch (testSubregionRelation(specificPlace, place, relevantTraceVariants)) {//-1: specific < place, 0 : mixed relation, 1:specific > place, 2: specific = place
                    case -1: //specific is a subregion of place
                        System.out.println("Identified " + place.toTransitionsString(transitions) + " as implicit validated by " + specificPlace.toTransitionsString(transitions));
                        if (validateCombinedPlace(place, specificPlace) && !implicitPlaces.contains(place)) {
                            implicitPlaces.add(place);
                            MyPlace repairPlace = computeCombinedPlace(place, specificPlace);
                            if (repairPlace != null && (Integer.bitCount(repairPlace.getInputTrKey() + Integer.bitCount(repairPlace.getOutputTrKey())) <= this.maxTreeDepth)) {
                                repairPlaces.add(repairPlace);
                            }
                        }
                        break;
                    case 0: //no relation between specific & place
                        //					System.out.println("No place could be identified as implicit.");
                        break;
                    case 1: //specific is a superregion of place
                        System.out.println("Identified " + specificPlace.toTransitionsString(transitions) + " as implicit validated by " + place.toTransitionsString(transitions));
                        if (validateCombinedPlace(specificPlace, place) && !implicitPlaces.contains(specificPlace)) {
                            implicitPlaces.add(specificPlace);
                            MyPlace repairPlace = computeCombinedPlace(specificPlace, place);
                            if (repairPlace != null && (Integer.bitCount(repairPlace.getInputTrKey() + Integer.bitCount(repairPlace.getOutputTrKey())) <= this.maxTreeDepth)) {
                                repairPlaces.add(repairPlace);
                            }
                        }
                        break;
                    case 2: //specific and place have equal marking (theoretically, random place can be kept)
                        MyPlace toBeRemoved = pick_place_with_equal_markings(place, specificPlace, relevantTraceVariants);
                        implicitPlaces.add(toBeRemoved);
                        break;
                    default:
                        System.out.println("Places " + place.toTransitionsString(transitions) + " and " + specificPlace.toTransitionsString(transitions) + " are not handled properly by IP-Remover!");
                        break;
                }
            }
        }
        PlugInStatistics.instance().incImplicitPlaces(implicitPlaces.size());
//		printPlaces(implicitPlaces);
        ArrayList<MyPlace> remaining = new ArrayList<MyPlace>(placesToCheck);
        remaining.add(specificPlace);
        remaining.removeAll(implicitPlaces);
        System.out.println("\n -------------Summary:--------------");
        System.out.println(implicitPlaces.size() + " to be removed: ");
        printPlaces(implicitPlaces);
        System.out.println(remaining.size() + " to be kept: ");
        printPlaces(remaining);
        System.out.println(repairPlaces.size() + " to be added: ");
        printPlaces(repairPlaces);
        System.out.println("\n");
        Object[] res = {implicitPlaces, repairPlaces};
        return res;
    }


    //pick place to be removed. theoretically, the choice should not matter. however, distinguish for debugging.
    private MyPlace pick_place_with_equal_markings(MyPlace place, MyPlace specificPlace, ArrayList<ArrayList<Integer>> relevantTraceVariants) {
        if (specificPlace.isEqual(place)) {//are equal
//			System.out.println("Equal: "+place.toTransitionsString(transitions)+" and " +specificPlace.toTransitionsString(transitions));
            return place;
        } else if (specificPlace.isEqualWithoutDead(place, computeTransitionsLiveness(relevantTraceVariants))) {//are equal after removing dead transitions
//			System.out.println("Equal execpt for dead: "+place.toTransitionsString(transitions)+" and " +specificPlace.toTransitionsString(transitions));
//			System.out.println("Pick for removal: "+choosePlaceToRemove(specificPlace, place).toTransitionsString(transitions));
            return (choosePlaceToRemove(specificPlace, place));
        } else if (specificPlace.isSubPlace(place) || place.isSubPlace(specificPlace)) {//subplace of a place with unsued transitions
//			System.out.println("Subpplace: "+place.toTransitionsString(transitions)+" and " +specificPlace.toTransitionsString(transitions));
//			System.out.println("Pick for removal: "+choosePlaceToRemove(specificPlace, place).toTransitionsString(transitions));
            return (choosePlaceToRemove(specificPlace, place));
        }
        //places with unused transitions but no subplace relation: chose simpler one
        else if (specificPlace.getSumOfTransitions() > place.getSumOfTransitions()) {
//			System.out.println("Weird: Loose "+specificPlace.toTransitionsString(transitions)+", keep " +place.toTransitionsString(transitions));
//			System.out.println("Pick for removal: "+specificPlace.toTransitionsString(transitions));
            return specificPlace;
        } else if (specificPlace.getSumOfTransitions() < place.getSumOfTransitions()) {
//			System.out.println("Weird: Loose "+place.toTransitionsString(transitions)+", keep " +specificPlace.toTransitionsString(transitions));
//			System.out.println("Pick for removal: "+place.toTransitionsString(transitions));
            return place;
        }
        //default: choose random
        return place;
    }


    //check whether the place resulting from p1-p2 is valid
    private boolean validateCombinedPlace(MyPlace p1, MyPlace p2) {//TODO adapt this for 'quasi-regions'?
        for (int i = 0; i < transitions.length; i++) {
            int mask = getMask(i, transitions);
            int inP1 = p1.getInputTrKey();
            int outP1 = p1.getOutputTrKey();
            int inP2 = p2.getInputTrKey();
            int outP2 = p2.getOutputTrKey();
            if ((inP1 & mask) > 0 && (outP2 & mask) > 0 //dangerous constellation
                    && (outP1 & mask) == 0 && (inP2 & mask) == 0) {//no self-loops
//					System.out.println("Found invalid arc weights: "+p1.toTransitions(transitionArray)+ " - "+p2.toTransitions(transitionArray));
                return false; //place is not valid because arcweight 2
            }
            if ((inP2 & mask) > 0 && (outP1 & mask) > 0 //dangerous constellation
                    && (outP2 & mask) == 0 && (inP1 & mask) == 0) {//no self-loops
//					System.out.println("Found invalid arc weights: "+p1.toTransitions(transitionArray)+ " - "+p2.toTransitions(transitionArray));
                return false; //place is not valid because arcweight -2
            }
        }
        return true;
    }


    //compute and return the place defined by p1-p2
    private MyPlace computeCombinedPlace(MyPlace p1, MyPlace p2) {//TODO adapt this for 'quasi-regions'?
        int inP1 = p1.getInputTrKey();
        int outP1 = p1.getOutputTrKey();
        int inP2 = p2.getInputTrKey();
        int outP2 = p2.getOutputTrKey();
        //at this point, the repair place is valid. compute its in and outkey.
        int inKey = 0;
        int outKey = 0;
        for (int i = 0; i < transitions.length; i++) {
            int mask = getMask(i, transitions);
            int combVal = (getBinaryValueAtPos(inP1, mask) - getBinaryValueAtPos(outP1, mask)) - (getBinaryValueAtPos(inP2, mask) - getBinaryValueAtPos(outP2, mask)); // (inp1[i]+outp1[i])-(inp2[i]+outp2[i]) = combp3[i]
            switch (combVal) {
                case 0: //needs check for selfloop or unconnected
                    //selfloop p1 xor p2: add i as ingoing and outgoing transitions.
                    if ((inP1 & mask) > 0 && (outP1 & mask) > 0 && (inP2 & mask) == 0 && (outP2 & mask) == 0) { //only p1 has a self-loop
                        inKey = inKey | mask;
                        outKey = outKey | mask;
                    } else if ((inP1 & mask) == 0 && (outP1 & mask) == 0 && (inP2 & mask) > 0 && (outP2 & mask) > 0) {//only p2 has a self-loop
                        inKey = inKey | mask;
                        outKey = outKey | mask;
                    }
                    //leave 0 in both keys: no connection to i, or two selfloops cancel each other out
                    break;
                case 1: // i is ingoing
                    inKey = inKey | mask;
                    break;
                case -1: //i is outgoing
                    outKey = outKey | mask;
                    break;
                default:
                    System.out.println("Error: combined place has unvalid connections!");
                    break;
            }
        }
        MyPlace result = new MyPlace(inKey, outKey);
        if (result.getNonLoopsInMask() == 0 || result.getNonLoopsOutMask() == 0) {
            return null;
        }
        return result;
    }


    //returns true if the place does not have at least one live connection for ingoing and outgoing
    private boolean hasToFewLiveConnections(MyPlace place, ArrayList<ArrayList<Integer>> relevantTraceVariants) {
        boolean[] transitionsLiveness = computeTransitionsLiveness(relevantTraceVariants);
        MyPlace clonedPlace = place.clone().removeDeadTransitions(transitionsLiveness);
        return Integer.bitCount(clonedPlace.getInputTrKey()) < 1 || Integer.bitCount(clonedPlace.getOutputTrKey()) < 1;
    }


    //compute transitionsLiveness based on given variants
    private boolean[] computeTransitionsLiveness(ArrayList<ArrayList<Integer>> relevantTraceVariants) {
        boolean[] transitionsLiveness = new boolean[transitions.length];
        for (int i = 0; i < transitionsLiveness.length; i++) {
            transitionsLiveness[i] = false;
        }
        for (ArrayList<Integer> trace : relevantTraceVariants) {
            for (Integer event : trace) {
                transitionsLiveness[event] = true;
            }
        }
        return transitionsLiveness;
    }


    private void printDeadTransitions(boolean[] transitionsLiveness) {
        String deadTransitions = "Dead: ";
        for (int i = 0; i < transitionsLiveness.length; i++) {
            if (!transitionsLiveness[i]) {
                deadTransitions = deadTransitions + transitions[i] + ", ";
            }
        }
        System.out.println(deadTransitions);
    }


    //given two places with sub place relation, returns the larger one for removal
    private MyPlace choosePlaceToRemove(MyPlace specificPlace, MyPlace place) {
        if (specificPlace.isSubPlace(place)) {
//			System.out.println("Choose subplace "+place.toTransitionsString(transitions)+ " over superplace "+specificPlace.toTransitionsString(transitions));
            return specificPlace;
        }
//		System.out.println("Choose subplace "+specificPlace.toTransitionsString(transitions)+ " over superplace "+place.toTransitionsString(transitions));
        return place;
    }

//-----------submethods--------------------------------------------------

    private void printPlaces(ArrayList<MyPlace> implicitPlaces) {
        String res = "";
        for (MyPlace place : implicitPlaces) {
            res = res + "\n" + place.toTransitionsString(transitions);
        }
        res = res + "\n";
        System.out.println(res);
    }


    private int getBinaryValueAtPos(int key, int mask) {
        if ((key & mask) > 0) //key has value 1 at pos mask{
            return 1;
        return 0;
    }


    //finds the set of places that share ingoing / outgoing activities with place1
    private ArrayList<MyPlace> getRelatedPlaces(MyPlace place1, ArrayList<MyPlace> places) {
        ArrayList<MyPlace> result = new ArrayList<MyPlace>();
        for (MyPlace place2 : places) {
            if (hasCommonTr(place1.getInputTrKey(), place2.getInputTrKey())) { //focus on common input
//			if(hasCommonTr(place1.getOutputTrKey(), place2.getOutputTrKey())) { //focus on common output
                result.add(place2);
            }
        }
        return result;
    }

    //true if keys have at least one transition in common
    private boolean hasCommonTr(int key1, int key2) {
        boolean result = (key1 & key2) > 0;
        //at least one transition in common
        return result;
    }


    //tests the subregion relation of p1 with p2.
    //returns an Integer with -1 (p1 is a subregion), 0 (mixed relation), 1 (p1 is a superregion), 2 (equal)
    private Integer testSubregionRelation(final MyPlace place1, final MyPlace place2, final ArrayList<ArrayList<Integer>> relevantTraceVariants) {
        boolean[] transitionsLiveness = computeTransitionsLiveness(relevantTraceVariants);
        MyPlace p1Alive = place1.clone();
        p1Alive.removeDeadTransitions(transitionsLiveness);
        MyPlace p2Alive = place2.clone();
        p2Alive.removeDeadTransitions(transitionsLiveness);
        MyPlace p1Replay;
        MyPlace p2Replay;
        Integer result = 2;
        Integer temp_result = 2;
        for (ArrayList<Integer> trace : relevantTraceVariants) {
            p1Replay = p1Alive.clone();
            p2Replay = p2Alive.clone();
            for (Integer event : trace) {
                //compare markings after consumption
                p1Replay.consumefire(getMask(event, transitions));
                p2Replay.consumefire(getMask(event, transitions));
                temp_result = compareMarkings(p1Replay, p2Replay);//-1 for p1<p2, 0 for p1=p2, 1=p1>p2
                switch (temp_result) {
                    case -1: //p1<p2
                        if (result == 2 || result == -1) {//first or consistent result
                            result = -1;
                        } else {
                            result = 0;
                        }
                        break;
                    case 0: //p1=p2
                        break; //no information gain
                    case 1: //p1>p2
                        if (result == 2 || result == 1) {//first or consistent result
                            result = 1;
                        } else {
                            result = 0;
                        }
                        break;
                    default:
                        break;
                }
                if (result == 0) { // the temp result was inconsistent with prior temp-result (==> p1 >< p2)
                    return result;
                }
                //compare markings after production
                p1Replay.producefire(getMask(event, transitions));
                p2Replay.producefire(getMask(event, transitions));
                temp_result = compareMarkings(p1Replay, p2Replay);//-1 for p1<p2, 0 for p1=p2, 1=p1>p2
                switch (temp_result) {
                    case -1: //p1<p2
                        if (result == 2 || result == -1) {//first or consistent result
                            result = -1;
                        } else {
                            result = 0;
                        }
                        break;
                    case 0: //p1=p2
                        break; //no information gain
                    case 1: //p1>p2
                        if (result == 2 || result == 1) {//first or consistent result
                            result = 1;
                        } else {
                            result = 0;
                        }
                        break;
                    default:
                        break;
                }
                if (result == 0) {// the temp result was inconsistent with prior temp-result (==> p1 >< p2)
                    return result;
                }
            }
        }
        return result;
    }


    //compares current marking. returns -1 for p1<p2, 0 for p1=p2, 1=p1>p2
    private Integer compareMarkings(MyPlace p1, MyPlace p2) {
        int result = 0;
        int compareValue = p1.getCurrentTokens() - p2.getCurrentTokens();
        if (compareValue < 0) { //p1 has less tokens than p2
            result = -1;
        } else if (compareValue > 0) { //p1 has more tokens than p2
            result = 1;
        }
        return result;
    }


}