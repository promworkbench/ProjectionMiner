package org.processmining.projectionminer.discoveryalgorithms.eSTMiner.discovery;

import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.candidatetraverser.AbstractCandidateTraverser;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyLog;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyPlace;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyProcessModel;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.implicitplaceremoval.AbstractImplicitPlacesRemover;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.placecombinators.BFSDeltaCombinator;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.placeevaluators.MyPlaceEvaluator;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.plugins.Parameters;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.plugins.PlugInStatistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class DeltaDiscovery extends AbstractDiscovery {
    // discovery using the threshold delta and the potential traces queue to ensure minimal global fitness

    private final BFSDeltaCombinator combinator; //only combinator is BFS, this discovery is tailored towards BFS
    private final int virtualLevelsModifier; //multiply the max tree depth with this modifier to get the max virtual tree depth


    public DeltaDiscovery(final MyProcessModel pM, final AbstractCandidateTraverser candidates,
                          final MyPlaceEvaluator evaluator, final AbstractImplicitPlacesRemover ipRemover,
                          final Parameters parameters, MyLog log, final BFSDeltaCombinator combinator) {
        super(pM, candidates, evaluator, ipRemover, parameters, log);
        this.combinator = combinator;
        this.virtualLevelsModifier = parameters.getVirtualLevelsModifier();
    }


    protected void performNextTreeLevelActions(int currentTreeDepth, int updatedTreeDepth, MyPlace currentPlace) {
        System.out.println("\n New tree level: changed from " + currentTreeDepth + " to " + updatedTreeDepth); //for debugging
        combinator.setCurrentDepth(updatedTreeDepth); //update combinator
        //trigger retesting of potential places queue & trigger IP removal, if concurrent IP removal is set
        System.out.println("\n ___________DeltaDiscovery: Trigger next tree level actions!______________");
        pM.updateAndPrintStatus(log);
        System.out.println("Revisit potential places queue...");
        pM = combinator.revisitQueueOfPlaces(pM); //update statistics in combinator (accept, discard, delay)
        pM.updateAndPrintStatus(log);
        if (this.removeImpsConcurrently) {//if enabled, remove implicit places from current model
//			System.out.println("Removing all currently implicit places...");
            if (repairWhileRemovingIPs) {
                pM = IPRemover.removeAllIPsAndRepair(pM);
            } else {
                pM = IPRemover.removeAllIPs(pM);
            }
            System.out.println("Places after intermediate IP removal: " + pM.getPlaces().size());
            pM.updateAndPrintStatus(log);
        }
        endOfLevelUpdateStatistics(currentTreeDepth);
        System.out.println("_____________end of next level actions______________ \n");
    }


    private void endOfLevelUpdateStatistics(int currentTreeDepth) {
        HashMap<String, Integer> currentLevelStatistics = new HashMap<String, Integer>();
        currentLevelStatistics.put("numPlaces", pM.getPlaces().size());
        currentLevelStatistics.put("numDeadTransitions", pM.getNumDeadTransitions());
        currentLevelStatistics.put("numLiveTraces", pM.getNumLiveTraces(log));
        currentLevelStatistics.put("numLiveVariants", pM.countLiveVariants());
        currentLevelStatistics.put("numDiscardedPlaces", pM.getDiscardedPlaces().size());
        currentLevelStatistics.put("numPotentialPlaces", pM.getPotentialPlaces().size());
        currentLevelStatistics.put("numIPs", PlugInStatistics.instance().getNumImpPlace());
        currentLevelStatistics.put("numDelayedPlaces", PlugInStatistics.instance().getNumDelayedPlaces());
        PlugInStatistics.instance().updateLevelStatistics(currentTreeDepth, currentLevelStatistics);

    }


    //triggered only, if fitness == MyPlaceStatus.FIT
    //use delta to determine global fitness
    protected void handleLocallyFittingPlace(MyPlace current) {
        pM.updateStatus(log);
        Object[] globalFitnessStatus = combinator.combinePlace(pM.getVariantVector(), current);

        // adding this place to this PM will never be possible, discard
        if ((int) globalFitnessStatus[0] == -1) {// adding this place to this PM will never be possible, discard
            pM.getDiscardedPlaces().add(current); //for debugging and statistics
            PlugInStatistics.instance().incDiscardedPlaces(1);
        }
        //place may be addable later, add to potential places. sort&shorten potential places
        if ((int) globalFitnessStatus[0] == 0) {//place might be combinable with PM later
            ArrayList<MyPlace> potentialPlaces = pM.getPotentialPlaces();
            potentialPlaces.add(current);
            PlugInStatistics.instance().incDelayedPlaces(1);
            potentialPlaces = combinator.shortenPotentialPlaces(potentialPlaces); //statistics (skipped places) updated inside of method
            pM.setPotentialPlaces(potentialPlaces);
        }
        //place can be added to PM now. If enabled, check for implicitness.
        if ((int) globalFitnessStatus[0] == 1) {
            PlugInStatistics.instance().incAcceptedPlaces(1);
            //recheck for implicitness with respect to newly added place (if concurrent implicitness check is turned on)
            if (removeImpsConcurrently) {
                ArrayList<MyPlace> pMPlaces = new ArrayList<MyPlace>(pM.getPlaces()); // does not contain current
                pM.addPlace(current);
                pM.updateStatus(log); //ensure reduced variants
                ArrayList<MyPlace> implicitPlaces;
                if (repairWhileRemovingIPs) {
                    Object[] IPRemovalResults = IPRemover.implicitAndRepairRelatedToPlace(current, pMPlaces, log.getReducedTraceVariants(pM.getVariantVector()));
                    implicitPlaces = (ArrayList<MyPlace>) IPRemovalResults[0];
                    pMPlaces.addAll((Collection<? extends MyPlace>) IPRemovalResults[1]);
                } else {
                    implicitPlaces = IPRemover.implicitRelatedToPlace(current, pMPlaces, log.getReducedTraceVariants(pM.getVariantVector()));
                }
                pMPlaces.add(current);//will be removed if implicit
                pMPlaces.removeAll(implicitPlaces);
                pM.setPlaces(pMPlaces);
                pM.updateStatus(log);
            } else {
                pM.addPlace(current);
                pM.updateStatus(log);
            }
        }
    }


    //empty potential places queue as much as possible
    protected MyProcessModel endOfDiscoveryActions(MyProcessModel pM) {
        pM.updateAndPrintStatus(log);
        //try virtual levels
        System.out.println("Revisit potential places using virtual tree levels...");
        int maxVirtualDepth = combinator.getMaxDepth() * virtualLevelsModifier;
        while (combinator.getCurrentDepth() <= maxVirtualDepth) {
            System.out.println("Current virtual depth: " + combinator.getCurrentDepth());
            pM = combinator.revisitQueueOfPlaces(pM);//update statistics in combinator (accept, discard, delay)
            pM.updateAndPrintStatus(log);
            if (removeImpsConcurrently) {
                if (repairWhileRemovingIPs) {
                    pM = IPRemover.removeAllIPsAndRepair(pM);
                } else {
                    pM = IPRemover.removeAllIPs(pM);
                }
            }
            endOfLevelUpdateStatistics(combinator.getCurrentDepth());
            combinator.setCurrentDepth(combinator.getCurrentDepth() + 1);
        }
        pM.updateAndPrintStatus(log);
        if (removeImpsConcurrently) {
            if (repairWhileRemovingIPs) {
                pM = IPRemover.removeAllIPsAndRepair(pM);
            } else {
                pM = IPRemover.removeAllIPs(pM);
            }
        }
        pM.updateAndPrintStatus(log);
        return pM;
    }


}


