package org.processmining.projectionminer.discoveryalgorithms;

import org.deckfour.xes.model.XLog;
import org.processmining.projectionminer.utils.PerformancePlugin;
import org.processmining.contexts.uitopia.UIPluginContext;

public abstract class DiscoveryPlugin {

    public AcceptingPetriNetWrapper discoverWithMeasure(UIPluginContext context, XLog log, Object parameter) throws InterruptedException {
        PerformancePlugin.getInstance().finishProjection();
        PerformancePlugin.getInstance().startDiscovery();

        AcceptingPetriNetWrapper result = discover(context, log, parameter);

        PerformancePlugin.getInstance().finishDiscovery();
        PerformancePlugin.getInstance().startProjection();

        return result;
    }

    public abstract AcceptingPetriNetWrapper discover(UIPluginContext context, XLog log, Object parameter) throws InterruptedException;

    public abstract Object startGUI(UIPluginContext context, XLog log);

    @Override
    public abstract String toString();
}
