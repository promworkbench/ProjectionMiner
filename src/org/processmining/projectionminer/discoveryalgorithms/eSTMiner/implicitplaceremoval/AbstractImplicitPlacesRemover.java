package org.processmining.projectionminer.discoveryalgorithms.eSTMiner.implicitplaceremoval;

import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyLog;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyPlace;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyProcessModel;

import java.util.ArrayList;

public abstract class AbstractImplicitPlacesRemover {
    protected final String[] transitions;
    protected final MyLog log;

    public AbstractImplicitPlacesRemover(String[] transitions, MyLog log) {
        this.transitions = transitions;
        this.log = log;
    }


//________________ main methods to remove implicit places____________________________	


    //without repair

    //trys to remove all IPs in the given PM
    public abstract MyProcessModel removeAllIPs(MyProcessModel inputPM,
                                                final ArrayList<ArrayList<Integer>> relevantTraceVariants);

    //trys to remove all IPs in the given PM that are related to a specific given place
    public abstract ArrayList<MyPlace> implicitRelatedToPlace(final MyPlace specificPlace,
                                                              final ArrayList<MyPlace> placesToCheck, ArrayList<ArrayList<Integer>> relevantTraceVariants);


    //with repair TODO currently unusable, adapt the use of the variant vector of new places

    //trys to remove all IPs in the given PM, adds needed repairPlaces
    public abstract MyProcessModel removeAllIPsAndRepair(MyProcessModel inputPM,
                                                         final ArrayList<ArrayList<Integer>> relevantTraceVariants);

    //trys to remove all IPs in the given PM that are related to a specific given place, returns list of needed repairplaces
    public abstract Object[] implicitAndRepairRelatedToPlace(final MyPlace specificPlace,
                                                             final ArrayList<MyPlace> placesToCheck, ArrayList<ArrayList<Integer>> relevantTraceVariants);


    //----------------------------Converter Methods------------------------------------

    //without repair

    public MyProcessModel removeAllIPs(MyProcessModel inputPM) {
        inputPM.updateStatus(log); //just to be safe
        return removeAllIPs(inputPM, log.getReducedTraceVariants(inputPM.getVariantVector()));
    }

    public ArrayList<MyPlace> implicitRelatedToPlace(final MyPlace specificPlace, final MyProcessModel inputPM) {
        ArrayList<ArrayList<Integer>> relevantTraceVariants = log.getReducedTraceVariants(inputPM.getVariantVector());
        return implicitRelatedToPlace(specificPlace, inputPM, relevantTraceVariants);
    }

    public ArrayList<MyPlace> implicitRelatedToPlace(MyPlace specificPlace, MyProcessModel inputPM,
                                                     ArrayList<ArrayList<Integer>> relevantTraceVariants) {
        ArrayList<MyPlace> placesToCheck = inputPM.getPlaces();
        return implicitRelatedToPlace(specificPlace, placesToCheck, relevantTraceVariants);
    }


    //with repair TODO currently unusable, adapt the use of the variant vector of new places

    public MyProcessModel removeAllIPsAndRepair(MyProcessModel inputPM) {
        inputPM.updateStatus(log); //just to be safe
        return removeAllIPsAndRepair(inputPM, log.getReducedTraceVariants(inputPM.getVariantVector()));
    }

    public Object[] implicitAndRepairRelatedToPlace(final MyPlace specificPlace, final MyProcessModel inputPM) {
        ArrayList<ArrayList<Integer>> relevantTraceVariants = log.getReducedTraceVariants(inputPM.getVariantVector());
        return implicitAndRepairRelatedToPlace(specificPlace, inputPM, relevantTraceVariants);
    }

    public Object[] implicitAndRepairRelatedToPlace(MyPlace specificPlace, MyProcessModel inputPM,
                                                    ArrayList<ArrayList<Integer>> relevantTraceVariants) {
        ArrayList<MyPlace> placesToCheck = inputPM.getPlaces();
        return implicitAndRepairRelatedToPlace(specificPlace, placesToCheck, relevantTraceVariants);
    }


    //Utility Methods_______________________________________________________________________________________________

    //return bitmask corresponding to position in the transition array
    protected int getMask(final int position, final String[] transitions) {
        return (1 << (transitions.length - 1 - position));
    }


}
