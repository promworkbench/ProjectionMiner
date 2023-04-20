/**
 *
 */
package org.processmining.projectionminer.mining.LocalPNReplayer;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.matchinstances.algorithms.IPNMatchInstancesLogReplayAlgorithm;
import org.processmining.plugins.petrinet.replayer.matchinstances.algorithms.express.*;
import org.processmining.plugins.petrinet.replayer.matchinstances.ui.PNParamSettingStep;
import org.processmining.plugins.petrinet.replayer.matchinstances.ui.PNRepMatchInstancesAlgorithmStep;
import org.processmining.plugins.petrinet.replayer.matchinstances.ui.PNReplayStep;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author aadrians
 */
public class PNMatchInstancesReplayerUI {
    public static final int MAPPING = 0;
    public static final int ALGORITHM = 1;
    public static final int PARAMETERS = 2;
    // reference variable
    private final UIPluginContext context;
    // selected algorithm
    IPNMatchInstancesLogReplayAlgorithm selectedAlgorithm;

    // steps
    private int nofSteps;
    private int currentStep;

    private int algorithmStep;
    private int testingParamStep;

    // gui for each steps
    private PNReplayStep[] replaySteps;

    public PNMatchInstancesReplayerUI(final UIPluginContext context, final IPNMatchInstancesLogReplayAlgorithm selectedAlgorithm) {
        this.context = context;
        this.selectedAlgorithm = selectedAlgorithm;
    }

    public Object[] getConfiguration(PetrinetGraph net, XLog log, TransEvClassMapping mapping) {
        // init steps and gui
        nofSteps = 0;

        // other steps
        algorithmStep = nofSteps++;
        testingParamStep = nofSteps++;

        // init gui for each step
        replaySteps = new PNReplayStep[nofSteps];
        replaySteps[algorithmStep] = new PNRepMatchInstancesAlgorithmStep(context);

        // set current step
        currentStep = algorithmStep;

        // how many configuration indexes?
        int[] configIndexes = new int[1];
        configIndexes[0] = testingParamStep;

        return showConfiguration(log, net, configIndexes, mapping);
    }


    private Object[] showConfiguration(XLog log, PetrinetGraph net,
                                       int[] configIndexes, TransEvClassMapping mapping) {
        // init result variable
        InteractionResult result = InteractionResult.NEXT;

        // configure interaction with user
        while (true) {
            if (currentStep <= 0) {
                currentStep = selectedAlgorithm == null ? 0 : 1;
            }
            if (currentStep >= nofSteps) {
                currentStep = nofSteps - 1;
            }
            init(log, net, mapping);

            result = InteractionResult.FINISHED;
//			result = context.showWizard("Replay in Petri net", currentStep == 0, currentStep == nofSteps - 1,
//					replaySteps[currentStep]);
//			System.out.println(result.toString());
//			System.out.println("Hi");
            switch (result) {
                case NEXT:
                    go(1, log, net, mapping);
                    break;
                case PREV:
                    go(-1, log, net, mapping);
                    break;
                case FINISHED:
                    // collect all parameters
                    List<Object> allParameters = new LinkedList<Object>();
                    for (int i = 0; i < configIndexes.length; i++) {
                        PNParamSettingStep testParamGUI = ((PNParamSettingStep) replaySteps[configIndexes[i]]);
                        Object[] params = testParamGUI.getAllParameters();
                        Collections.addAll(allParameters, params);
                    }

                    return new Object[]{mapping,
                            selectedAlgorithm == null ?
                                    ((PNRepMatchInstancesAlgorithmStep) replaySteps[algorithmStep]).getAlgorithm() : selectedAlgorithm,
                            allParameters.toArray()};
                default:
                    return null;
            }
        }
    }

    private void init(XLog log, PetrinetGraph net, TransEvClassMapping mapping) {
        if (selectedAlgorithm != null) {
            if (selectedAlgorithm instanceof NBestAlignmentsAlg) {
                if (selectedAlgorithm instanceof BestWithFitnessBoundAlignmentsTreeAlg) {
                    ParamSettingBestWithFitnessBoundAlg paramSetting = new ParamSettingBestWithFitnessBoundAlg();
                    paramSetting.populateCostPanel(net, log, mapping);
                    replaySteps[testingParamStep] = paramSetting;
                } else {
                    ParamSettingNBestAlg paramSetting = new ParamSettingNBestAlg();
                    paramSetting.populateCostPanel(net, log, mapping);
                    replaySteps[testingParamStep] = paramSetting;
                }
            } else {
                ParamSettingExpressAlg paramSetting = new ParamSettingExpressAlg();
                paramSetting.populateCostPanel(net, log, mapping);
                replaySteps[testingParamStep] = paramSetting;
            }
        }
    }

    private int go(int direction, XLog log, PetrinetGraph net, TransEvClassMapping mapping) {
        currentStep += direction;

        if ((currentStep == algorithmStep) && (selectedAlgorithm != null)) {
            // skip selection of algorithm, proceed with algorithm so far
            currentStep += direction;
        }

        // check which algorithm is selected and adjust parameter as necessary
        if ((currentStep == testingParamStep) && (selectedAlgorithm == null)) {
            // special checking for N-best
            if (replaySteps[algorithmStep] instanceof PNRepMatchInstancesAlgorithmStep) {
                // which algorithm is it?
                PNRepMatchInstancesAlgorithmStep step = (PNRepMatchInstancesAlgorithmStep) replaySteps[algorithmStep];
                if ((step.getAlgorithm() instanceof NBestAlignmentsAlg)) {
                    if (step.getAlgorithm() instanceof BestWithFitnessBoundAlignmentsTreeAlg) {
                        ParamSettingBestWithFitnessBoundAlg paramSetting = new ParamSettingBestWithFitnessBoundAlg();
                        paramSetting.populateCostPanel(net, log, mapping);
                        replaySteps[testingParamStep] = paramSetting;
                    } else {
                        ParamSettingNBestAlg paramSetting = new ParamSettingNBestAlg();
                        paramSetting.populateCostPanel(net, log, mapping);
                        replaySteps[testingParamStep] = paramSetting;
                    }
                } else {
                    ParamSettingExpressAlg paramSetting = new ParamSettingExpressAlg();
                    paramSetting.populateCostPanel(net, log, mapping);
                    replaySteps[testingParamStep] = paramSetting;
                }
            }
        }

        if ((currentStep >= 0) && (currentStep < nofSteps)) {
            return currentStep;
        }
        return currentStep;
    }

}
