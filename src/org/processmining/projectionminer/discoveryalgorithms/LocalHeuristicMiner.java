package org.processmining.projectionminer.discoveryalgorithms;

import com.raffaeleconforti.marking.MarkingDiscoverer;
import com.raffaeleconforti.wrapper.LogPreprocessing;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.heuristics.HeuristicsNet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.heuristicsnet.miner.heuristics.converter.HeuristicsNetToPetriNetConverter;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.FlexibleHeuristicsMinerPlugin;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.gui.ParametersPanel;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.settings.HeuristicsMinerSettings;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashSet;

public class LocalHeuristicMiner extends DiscoveryPlugin {
    @Override
    public AcceptingPetriNetWrapper discover(UIPluginContext context, XLog log, Object parameter) {
        HeuristicsMinerSettings settings;
        if (parameter != null) {
            settings = (HeuristicsMinerSettings) parameter;
        } else {
            settings = (HeuristicsMinerSettings) startGUI(context, log);
        }

        LogPreprocessing logPreprocessing = new LogPreprocessing();
        log = logPreprocessing.preprocessLog(context, log);

        HeuristicsNet heuristicsNet = FlexibleHeuristicsMinerPlugin.run(context, log, settings);
        Object[] result = HeuristicsNetToPetriNetConverter.converter(context, heuristicsNet);
        logPreprocessing.removedAddedElements((Petrinet) result[0]);
        if (result[1] == null) {
            result[1] = MarkingDiscoverer.constructInitialMarking(context, (Petrinet) result[0]);
        } else {
            MarkingDiscoverer.createInitialMarkingConnection(context, (Petrinet) result[0], (Marking) result[1]);
        }

        Marking finalMarking = MarkingDiscoverer.constructFinalMarking(context, (Petrinet) result[0]);
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        return new AcceptingPetriNetWrapper((Petrinet) result[0], (Marking) result[1], finalMarking, parameter);
    }

    @Override
    public Object startGUI(UIPluginContext context, XLog log) {
        HashSet classifiers = new HashSet();
        classifiers.add(new XEventNameClassifier());
        ParametersPanel parameters = new ParametersPanel(classifiers);
        parameters.removeAndThreshold();
        context.showConfiguration("Heuristics Miner Parameters", parameters);
        return parameters.getSettings();
    }

    @Override
    public String toString() {
        return "Heuristics Miner";
    }
}
