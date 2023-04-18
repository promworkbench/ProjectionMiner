package org.processmining.projectionminer.discoveryalgorithms.eSTMiner.placeevaluators;

import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyPlace;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyPlaceStatus;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyProcessModel;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.plugins.PlugInStatistics;

import java.util.ArrayList;
import java.util.HashMap;


public class MyPlaceEvaluator {

    private final ArrayList<ArrayList<Integer>> log;
    private final double threshold;
    private final MyProcessModel pM;
    private final HashMap<ArrayList<Integer>, Integer> traceCounts;
    private final double[][] scoreDF;

    public MyPlaceEvaluator(final MyProcessModel pM, final ArrayList<ArrayList<Integer>> singleTraceLog,
                            final HashMap<ArrayList<Integer>, Integer> traceCounts, final double threshold, double[][] scoreDF) {
        this.pM = pM;
        this.threshold = threshold;
        log = singleTraceLog; //log consisting of trace variants only
        this.traceCounts = traceCounts; // specifying the frequencies of the trac variants
        this.scoreDF = scoreDF;
    }

    public MyPlaceStatus testPlace(final MyPlace current) {
        MyPlaceStatus result = replayFull(current);
        return result;
    }

    private MyPlaceStatus replayFull(final MyPlace current) {
        long startTime = System.currentTimeMillis();
        //Variables to store results for the current place
        double numUf = 0; //number of underfed traces
        double numOf = 0; //number of overfed traces
        double numActTr = 0; // number of activated traces
        double numFitAct = 0; //number of fitting activated traces
        double numFit = 0; //number of fitting traces
        int currentVariantID = 0; //used for handling variant vectors

        //replay each trace on the place
        for (ArrayList<Integer> trace : log) {
            MyPlace currentCopy = current.clone();
            for (int event : trace) {
                currentCopy.fire(getMask(event));
            }
            //save results of replay of this trace
            if (currentCopy.getActivated()) {
                numActTr = numActTr + traceCounts.get(trace);
                if (currentCopy.getCurrentTokens() > 0) {
                    numOf = numOf + traceCounts.get(trace);
                    current.editVariantVector(currentVariantID, false);
                }
                if (currentCopy.getUnderfed() > 0) {
                    numUf = numUf + traceCounts.get(trace);
                    current.editVariantVector(currentVariantID, false);
                }
                if ((currentCopy.getUnderfed() == 0) && (currentCopy.getCurrentTokens() == 0)) {
                    numFitAct = numFitAct + traceCounts.get(trace);
                    numFit = numFit + traceCounts.get(trace);
                    current.editVariantVector(currentVariantID, true);

                }
            } else {//non activated cannot be malfed
                numFit = numFit + traceCounts.get(trace);
                current.editVariantVector(currentVariantID, true);
            }
            currentVariantID++;
        }

        //evaluate results of replay of the log
        MyPlaceStatus result = MyPlaceStatus.UNFIT;
        if (numActTr > 0) {//if place is never activated, it is useless and thus unfitting
            if ((numFitAct / numActTr >= threshold)) {//enough evidence that place is fit
                result = MyPlaceStatus.FIT;
            } else {
                boolean UF = (numUf / numActTr) > (1.0 - threshold);
                boolean OF = (numOf / numActTr) > (1.0 - threshold);
                if (UF) {
                    result = MyPlaceStatus.UNDERFED;
                }
                if (OF) {
                    result = MyPlaceStatus.OVERFED;
                }
                if (UF && OF) {
                    result = MyPlaceStatus.MALFED;
                }
            }
        }
        PlugInStatistics.instance().incTimeEval(System.currentTimeMillis() - startTime);
        if (result == MyPlaceStatus.FIT) {
            PlugInStatistics.instance().incNumFitting();
        } else if (result == MyPlaceStatus.UNKNOWN) {
            //no statistical count
        } else {//malfed, overfed, underfed or unfit
            PlugInStatistics.instance().incNumUnfitting();
        }
        return result;
    }


    private int getMask(final int position) {
        return (1 << (pM.getTransitions().length - 1 - position));
    }


}
