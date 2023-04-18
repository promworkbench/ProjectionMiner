package org.processmining.projectionminer.discoveryalgorithms.eSTMiner.discovery;


//TODO upadte everything, including use of statistics
/*

import java.util.ArrayList;

import org.processmining.est.V9.refactored.MyLog;
import org.processmining.est.V9.refactored.MyPlace;
import org.processmining.est.V9.refactored.MyPlaceEvaluator;
import org.processmining.est.V9.refactored.MyProcessModel;
import org.processmining.est.V9.refactored.Parameters;
import org.processmining.est.V9.refactored.PlugInStatistics;
import org.processmining.est.V9.refactored.ImplicitPlacesRemover.AbstractImplicitPlacesRemover;
import org.processmining.est.V9.refactored.candidateTraverser.AbstractCandidateTraverser;

public class ClassicDiscovery extends AbstractDiscovery {





    public ClassicDiscovery(MyProcessModel pM, AbstractCandidateTraverser candidates, MyPlaceEvaluator evaluator,
            AbstractImplicitPlacesRemover ipRemover, Parameters parameters, MyLog log) {
        super(pM, candidates, evaluator, ipRemover, parameters, log);
    }


    protected void performNextTreeLevelActions(int currentTreeDepth, int updatedTreeDepth, MyPlace currentPlace){
        //no next level actions for classic discovery
    }


    //triggered only, if fitness == MyPlaceStatus.FIT
    protected void handleLocallyFittingPlace(MyPlace current) {
    //place can be added to PM. If enabled, check for implicitness.
        if(removeImpsConcurrently) {
            final long timeIPremovalStart = System.currentTimeMillis();
            ArrayList<MyPlace> pMPlaces = pM.getPlaces();
            ArrayList<MyPlace> implicitPlaces = IPRemover.implicitRelatedToPlace(current, pM, log);//implcit places may include current
            pMPlaces.add(current);
            pMPlaces.removeAll(implicitPlaces);
            System.out.println("Removed implicit places: "+implicitPlaces.toString());
            pM.addPlace(current); //this updates the pM variant vector
            pM.setPlaces(pMPlaces); //this sets the places as all remaining non-implicit places (possily excluding current)
            PlugInStatistics.instance().addCurrentRemainingPlaces(pMPlaces.size());
            PlugInStatistics.instance().incTimeImpTest(System.currentTimeMillis()-timeIPremovalStart);
            //System.out.println(System.currentTimeMillis()-timeIPremovalStart);
        }
        else {
            pM.addPlace(current); //this updates the PM variant vector
        }
    }


    protected MyProcessModel endOfDiscoveryActions(MyProcessModel pM) {
        //nothing to do
        return pM;
    }


}

    */
