package org.processmining.projectionminer.discoveryalgorithms.eSTMiner.plugins;

import com.fluxicon.slickerbox.components.NiceDoubleSlider;
import com.fluxicon.slickerbox.components.NiceIntegerSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;
import org.processmining.framework.util.ui.wizard.ProMWizardStep;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

//this class provides the userinterface, takes parameter values and stores them in the parameters

public class UI extends ProMPropertiesPanel implements ProMWizardStep<Parameters> {
    public static String TITLE = "Configure eST-Miner parameters:";
    NiceDoubleSlider threshold_tau;
    NiceDoubleSlider threshold_delta;
    ProMComboBox<String> deltaAdaptionStrategyComboBox;
    NiceIntegerSlider deltaAdaptionSteepness;
    ProMComboBox<String> classifierComboBox;
    ProMComboBox<String> discoveryVersionComboBox;
    ProMComboBox<String> traversalStrategyComboBox;
    ProMComboBox<String> postprocVersionComboBox;
    JCheckBox RemoveImplicitPlacesBox;
    JCheckBox RemoveImplicitPlacesConcurrentlyBox;
    JCheckBox LimitTreeDepthBox;
    JCheckBox RepairWhileRemovingIPsBox;
    XLog log;
    NiceIntegerSlider treeDepth;
    NiceIntegerSlider virtualTreeDepth;
    NiceIntegerSlider PotentialPlacesMaxLength;

    public UI(String title) {
        super(title);
        // TODO Auto-generated constructor stub
    }

    //method that actually takes the parameters
    public UI(XLog log) {
        super(TITLE);

        //configure threshold tau
        this.threshold_tau = SlickerFactory.instance().createNiceDoubleSlider("Threshold Tau ", 0, 1, 1, Orientation.HORIZONTAL);
        this.add(this.threshold_tau);

        //configure threshold delta
        this.threshold_delta = SlickerFactory.instance().createNiceDoubleSlider("Threshold Delta ", 0, 1, 0.05, Orientation.HORIZONTAL);
        this.add(this.threshold_delta);

        //configure classifier
        this.log = log;
        List<XEventClassifier> logClassifiers = log.getClassifiers();
        List<String> logClassifiersLabels = new ArrayList<String>();
        for (XEventClassifier classifier : logClassifiers) {
            logClassifiersLabels.add(classifier.toString());
        }
        classifierComboBox = this.addComboBox("Classifier:", logClassifiersLabels);

        //configure candidate traversal
        List<String> traversalStrategies = new ArrayList();
        traversalStrategies.add("BFS");
        traversalStrategies.add("DFS");
        traversalStrategyComboBox = this.addComboBox("Tree Traversal Strategy:", traversalStrategies);

        //configure discovery
        List<String> discoveryVariants = new ArrayList();
        discoveryVariants.add("Delta");
        discoveryVariants.add("Classic");
        discoveryVariants.add("Uniwired - currently not implemented");
        discoveryVersionComboBox = this.addComboBox("Discovery Variant:", discoveryVariants);

        //configure delta adaption strategy
        List<String> deltaAdaptionStrategies = new ArrayList();
        deltaAdaptionStrategies.add("Sigmoid");
        deltaAdaptionStrategies.add("MaxDelta");
        deltaAdaptionStrategies.add("NoDelta");
        deltaAdaptionStrategies.add("Linear");
        deltaAdaptionStrategyComboBox = this.addComboBox("Delta Adaption Strategy:", deltaAdaptionStrategies);

        //configure delta adption strategy steepness (relevant for sigmoid & linear only)
        this.deltaAdaptionSteepness = SlickerFactory.instance().createNiceIntegerSlider("Delta Adaption Steepness: ", 1, 100, 4, Orientation.HORIZONTAL);
        this.add(this.deltaAdaptionSteepness);

        //configure virtual tree traversal depth (delta discovery)
        this.virtualTreeDepth = SlickerFactory.instance().createNiceIntegerSlider("Virtual Tree Depth Modifier: ", 1, 100, 2, Orientation.HORIZONTAL);
        this.add(this.virtualTreeDepth);


        //configure postprocessing
        RemoveImplicitPlacesBox = this.addCheckBox("Remove Implicit Places?", true); //IP removal?
        RemoveImplicitPlacesConcurrentlyBox = this.addCheckBox("Remove IPs concurrently?", true); //IP removal concurrently?
        RepairWhileRemovingIPsBox = this.addCheckBox("Repair whileremoving IPs? - currently not implemented", false); //add repair places when removing IPs by Replay
        //Which removal Strategy
        List<String> postprocVariants = new ArrayList();
        postprocVariants.add("Replay");
        postprocVariants.add("LPP");
        postprocVersionComboBox = this.addComboBox("IP Removal Method:", postprocVariants);

        //delta discovery: configure potential places queue max length
        this.PotentialPlacesMaxLength = SlickerFactory.instance().createNiceIntegerSlider("Potential places max (delta discover): ", 0, 10000, 10000, Orientation.HORIZONTAL);
        this.add(this.PotentialPlacesMaxLength);

        //configure tree traversal depth
        LimitTreeDepthBox = this.addCheckBox("Limit Tree Depth? - currently not (fully) implemented", true); //IP removal?
        this.treeDepth = SlickerFactory.instance().createNiceIntegerSlider("Max Number of Transitions: ", 1, 20, 6, Orientation.HORIZONTAL);
        this.add(this.treeDepth);

    }

    //store input in parameters
    public Parameters apply(Parameters model, JComponent component) {

        List<XEventClassifier> logClassifiers = log.getClassifiers();
        //XEventClassifier classifierToUse = XLogInfoImpl.STANDARD_CLASSIFIER;
        XEventClassifier classifierToUse = XLogInfoImpl.NAME_CLASSIFIER;
        for (XEventClassifier classifier : logClassifiers) {
            if (classifier.toString().equals(classifierComboBox.getSelectedItem().toString())) {
                classifierToUse = classifier;
                break;
            }
        }

        double threshold_tau_result = this.threshold_tau.getValue();

        double threshold_delta_result = this.threshold_delta.getValue();
        String delta_adaption_strategy_result = this.deltaAdaptionStrategyComboBox.getSelectedItem().toString();
        int PotentialPlacesMaxLength = this.PotentialPlacesMaxLength.getValue();
        int deltaAdaptionSteepness_result = this.deltaAdaptionSteepness.getValue();

        boolean limitTreeDepth = LimitTreeDepthBox.isSelected();
        int treeDepth = this.treeDepth.getValue();

        int virtualTreeDepth_result = this.virtualTreeDepth.getValue();

        boolean removeImplicitPlaces = RemoveImplicitPlacesBox.isSelected();
        boolean removeImplicitPlacesConcurrently = RemoveImplicitPlacesConcurrentlyBox.isSelected();
        boolean repairWhileRemovingIPs = RepairWhileRemovingIPsBox.isSelected();
        String postprocMethodToUse = postprocVersionComboBox.getSelectedItem().toString();
        String traversalStrategy = traversalStrategyComboBox.getSelectedItem().toString();
        String discoveryMethodToUse = discoveryVersionComboBox.getSelectedItem().toString();

        Parameters parameters = new Parameters(delta_adaption_strategy_result, deltaAdaptionSteepness_result, virtualTreeDepth_result, PotentialPlacesMaxLength, limitTreeDepth, treeDepth, discoveryMethodToUse, traversalStrategy, threshold_tau_result, threshold_delta_result, classifierToUse, removeImplicitPlaces, removeImplicitPlacesConcurrently, repairWhileRemovingIPs, postprocMethodToUse);

        System.out.println(parameters.print());
        return parameters;
    }

    public boolean canApply(Parameters model, JComponent component) {
        // TODO Auto-generated method stub
        return true;
    }

    public JComponent getComponent(Parameters model) {
        // TODO Auto-generated method stub
        return this;
    }

    public String getTitle() {
        // TODO Auto-generated method stub
        return TITLE;
    }

}