package org.processmining.projectionminer.discoveryalgorithms.TransitionSystemLocal;

import org.deckfour.xes.classification.*;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.transitionsystem.miner.TSMinerPlugin;

import java.util.Arrays;
import java.util.Iterator;

public class TSMinerLocalPlugin extends TSMinerPlugin {
    public static Object[] main(UIPluginContext context, XLog log) {
        XEventClassifier[] classifiers;
        Object transitionClassifier;
        if (log.getClassifiers().size() > 0) {
            classifiers = new XEventClassifier[log.getClassifiers().size()];
            int i = 0;

            XEventClassifier classifier;
            for (Iterator var5 = log.getClassifiers().iterator(); var5.hasNext(); classifiers[i++] = classifier) {
                classifier = (XEventClassifier) var5.next();
            }

            transitionClassifier = classifiers[0];
        } else {
            classifiers = new XEventClassifier[]{new XEventNameClassifier(), new XEventResourceClassifier(), new XEventLifeTransClassifier()};
            transitionClassifier = new XEventAndClassifier(new XEventNameClassifier(), new XEventLifeTransClassifier());
        }

        return main(context, log, classifiers, (XEventClassifier) transitionClassifier);
    }

    public static Object[] main(UIPluginContext context, XLog log, XEventClassifier[] classifiers, XEventClassifier transitionClassifier) {
        TSLocalMinerUI miner = new TSLocalMinerUI(context);
        return miner.mine(log, Arrays.asList(classifiers), transitionClassifier);
    }

    public static void setLabels(PluginContext context, XLog log) {
    }
}
