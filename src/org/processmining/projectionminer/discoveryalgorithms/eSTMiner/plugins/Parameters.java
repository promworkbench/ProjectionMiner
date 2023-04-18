package org.processmining.projectionminer.discoveryalgorithms.eSTMiner.plugins;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.processmining.basicutils.parameters.impl.PluginParametersImpl;

public class Parameters extends PluginParametersImpl {
    private final String deltaAdaption;
    private final String traversalStrategy;
    private final boolean repairWhileRemovingIPs;
    int potentialPlacesMaxLength;
    int virtualLevelsModifier;
    private double threshold_tau;  // in[0,1], determmines fraction of traces in original log
    private double threshold_delta; // in [0,1], determmines fraction of traces in original log
    private int deltaAdaptionSteepness;
    private long timeAllowance;
    private XEventClassifier classifier;
    private boolean removeImps;
    private String discoveryVariant;
    private String impRemoveVariant;
    private boolean RemoveImpsConcurrently;
    private int max_depth;
    private boolean cut_depth;

    public Parameters(String deltaAdaptionStrategy, int deltaAdaptionSteepness, int virtualLevelsModifier, int potentialPlacesMaxLength, boolean cut_depth, int max_depth, String discoveryMethodToUse, String traversalStrategy, double threshold_tau_result, double threshold_delta_result, XEventClassifier classifier, boolean remIP, boolean remIPconc, boolean repairWhileRemovingIPs, String postprocMethodToUse) {
        this.threshold_tau = threshold_tau_result;
        this.threshold_delta = threshold_delta_result;
        this.classifier = classifier;
        this.removeImps = remIP;
        this.traversalStrategy = traversalStrategy;
        this.RemoveImpsConcurrently = remIPconc;
        this.timeAllowance = Long.MAX_VALUE; //time allowed for finding places in ms // TODO parameter stays fixed for now
        this.discoveryVariant = discoveryMethodToUse;
        this.impRemoveVariant = postprocMethodToUse;
        this.max_depth = max_depth;
        this.cut_depth = cut_depth;
        this.potentialPlacesMaxLength = potentialPlacesMaxLength;
        this.virtualLevelsModifier = virtualLevelsModifier;
        this.deltaAdaption = deltaAdaptionStrategy;
        this.deltaAdaptionSteepness = deltaAdaptionSteepness;
        this.repairWhileRemovingIPs = repairWhileRemovingIPs;
    }

    public Parameters() {
        this.threshold_tau = 0.75; //fraction of fitting traces for a place to be fitting
        this.threshold_delta = 1;
        this.timeAllowance = Long.MAX_VALUE; //time allowed for finding places in ms
        this.classifier = XLogInfoImpl.STANDARD_CLASSIFIER; //classifier used to extract transitions from the log
        this.removeImps = true;
        this.RemoveImpsConcurrently = false;
        this.discoveryVariant = "No discovery variant specified!";
        this.impRemoveVariant = "No implicit place removal variant specified!";
        this.traversalStrategy = "No traversal strategy specified!";
        this.max_depth = 4;
        this.cut_depth = false;
        this.potentialPlacesMaxLength = 100;
        this.virtualLevelsModifier = 2;
        this.deltaAdaption = "No delta adaption strategy specified!";
        this.deltaAdaptionSteepness = 4;
        this.repairWhileRemovingIPs = true;
    }

    public boolean isRemoveImpsConcurrently() {
        return RemoveImpsConcurrently;
    }

    public void setRemoveImpsConcurrently(boolean removeImpsConcurrently) {
        RemoveImpsConcurrently = removeImpsConcurrently;
    }

    public int getVirtualLevelsModifier() {
        return virtualLevelsModifier;
    }

    public void setVirtualLevelsModifier(int virtualLevelsModifier) {
        this.virtualLevelsModifier = virtualLevelsModifier;
    }

    public double getThresholdTau() {
        return threshold_tau;
    }

    public void setThresholdTau(double threshold) {
        this.threshold_tau = threshold;
    }

    public double getThresholdDelta() {
        return threshold_delta;
    }

    public void setThresholdDelta(int threshold_delta) {
        this.threshold_delta = threshold_delta;
    }

    public int getDeltaAdaptionSteepness() {
        return deltaAdaptionSteepness;
    }

    public void setDeltaAdaptionSteepness(int deltaAdaptionSteepness) {
        this.deltaAdaptionSteepness = deltaAdaptionSteepness;
    }

    public long getTimeAllowance() {
        return timeAllowance;
    }

    public void setTimeAllowance(long timeAllowance) {
        this.timeAllowance = timeAllowance;
    }

    public String getDeltaAdaption() {
        return deltaAdaption;
    }

    public XEventClassifier getClassifier() {
        return classifier;
    }

    public void setClassifier(XEventClassifier classifier) {
        this.classifier = classifier;
    }

    public int getMax_depth() {
        return max_depth;
    }

    public void setMax_depth(int max_depth) {
        this.max_depth = max_depth;
    }

    public boolean isCut_depth() {
        return cut_depth;
    }

    public void setCut_depth(boolean cut_depth) {
        this.cut_depth = cut_depth;
    }

    public boolean isRemoveImps() {
        return removeImps;
    }

    public void setRemoveImps(boolean removeImps) {
        this.removeImps = removeImps;
    }

    public String getDiscoveryVariant() {
        return discoveryVariant;
    }

    public void setDiscoveryVariant(String discoveryVariant) {
        this.discoveryVariant = discoveryVariant;
    }

    public String getImpRemoveVariant() {
        return impRemoveVariant;
    }

    public void setImpRemoveVariant(String impRemoveVariant) {
        this.impRemoveVariant = impRemoveVariant;
    }

    public String print() {
        String result = "CHOSEN PARAMETERS: \n";
        result = result + "Selected Classifier: " + this.classifier + "\n";
        result = result + "Discovery Variant: " + this.discoveryVariant + "\n";
        result = result + "Traversal Strategy: " + this.traversalStrategy + "\n";
        result = result + "Threshold Tau: " + this.threshold_tau + "\n";
        result = result + "Threshold Delta (less effect if high): " + this.threshold_delta + "\n";
        result = result + "Delta Adaption Strategy: " + this.deltaAdaption + "\n";
        result = result + "Delta Adaption Steepness: " + deltaAdaptionSteepness + "\n";
        result = result + "Delta: maximum length potential places: " + this.potentialPlacesMaxLength + "_";
        result = result + "Limit Traversal Depth: " + this.cut_depth + "\n";
        result = result + "Traversal Depth: " + this.max_depth + " Transitions \n";
        result = result + "Virtual Depth Modifier (multiplied with traversal depth): " + this.virtualLevelsModifier + "\n";
        result = result + "Implicit places removed?: " + this.isRemoveImps() + "\n";
        result = result + "IPs removed concurrently?: " + this.RemoveImpsConcurrently + "\n";
        result = result + "Repair while removing IPs?: " + this.repairWhileRemovingIPs + "\n";
        result = result + "Removal Variant: " + this.impRemoveVariant + "\n";
        return result;
    }

    // used for file naming - needs to be adapted to contain parameters of interest
    //current state: tau_delta_deltaStrategy_deltaStrategySteepness_maxTreeDepth_maxVirtualTreeDepthModifier_booleanConcurrentIPRemoval_booleanRepairWhileRemovingIPs
    public String getParamString() {
        String result = this.threshold_tau + "_";
        result = result + +this.threshold_delta + "_";
        result = result + this.deltaAdaption + "_";
        result = result + deltaAdaptionSteepness + "_";
        result = result + this.potentialPlacesMaxLength + "_";
        result = result + this.virtualLevelsModifier + "_";
        result = result + this.max_depth + "_";
//		result = result + this.RemoveImpsConcurrently + "_";
//		result = result + this.repairWhileRemovingIPs + "_";
        return result;
    }


    public String getTraversalStrategy() {
        return traversalStrategy;
    }

    public int getPotentialPlacesMaxLength() {
        return potentialPlacesMaxLength;
    }

    public boolean isRepairWhileRemovingIPs() {
        return this.repairWhileRemovingIPs;
    }


}