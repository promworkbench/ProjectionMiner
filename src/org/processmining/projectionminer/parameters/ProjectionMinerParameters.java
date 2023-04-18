package org.processmining.projectionminer.parameters;

import org.processmining.projectionminer.discoveryalgorithms.DiscoveryPlugin;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.plugins.ESTMinerPlugin;
import org.processmining.projectionminer.mining.FiringSequenceHeuristics.AbstractFiringSequenceChooser;
import org.processmining.projectionminer.mining.FiringSequenceHeuristics.GlobalMinimizeTokensFiringSequenceChooser;

public class ProjectionMinerParameters {
    private final DiscoveryPlugin discoveryPlugin;
    private final boolean initialPlaceMerge;
    private final int maxRecursionDepth;
    private final boolean reduceSilentTransitions;

    public ProjectionMinerParameters() {
        discoveryPlugin = new ESTMinerPlugin();
        initialPlaceMerge = false;
        maxRecursionDepth = 5;
        reduceSilentTransitions = true;
    }

    public ProjectionMinerParameters(DiscoveryPlugin discoveryPlugin, boolean initialPlaceMerge,
                                     int maxRecursionDepth, boolean reduceSilentTransitions) {
        this.discoveryPlugin = discoveryPlugin;
        this.initialPlaceMerge = initialPlaceMerge;
        this.maxRecursionDepth = maxRecursionDepth;
        this.reduceSilentTransitions = reduceSilentTransitions;
    }

    public DiscoveryPlugin getDiscoveryPlugin() {
        return discoveryPlugin;
    }

    public boolean shouldInitialPlaceMerge() {
        return initialPlaceMerge;
    }

    public int getMaxRecursionDepth() {
        return maxRecursionDepth;
    }

    public boolean shouldReduceSilentTransitions() {
        return reduceSilentTransitions;
    }

    public int getSequenceGeneratorDepth() {
        return 15;
    }

    public AbstractFiringSequenceChooser getFiringSequenceChooser() {
        return new GlobalMinimizeTokensFiringSequenceChooser();
    }
}
