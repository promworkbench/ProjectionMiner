package org.processmining.projectionminer.discoveryalgorithms;

import org.deckfour.uitopia.api.event.TaskListener;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.connections.Connection;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.hybridilpminer.connections.XLogHybridILPMinerParametersConnection;
import org.processmining.hybridilpminer.dialogs.ConnectionsClassifierEngineAndDefaultConfigurationDialogImpl;
import org.processmining.hybridilpminer.parameters.DiscoveryStrategyType;
import org.processmining.hybridilpminer.parameters.XLogHybridILPMinerParametersImpl;
import org.processmining.hybridilpminer.utils.XLogUtils;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.widgets.wizard.Dialog;
import org.processmining.widgets.wizard.Wizard;
import org.processmining.widgets.wizard.WizardResult;

import java.util.Collection;
import java.util.HashSet;

import static org.processmining.hybridilpminer.plugins.HybridILPMinerPlugin.discoverWithArtificialStartEnd;

public class LocalILPMiner extends DiscoveryPlugin {
    XLog artifLog;

    @Override
    public AcceptingPetriNetWrapper discover(UIPluginContext context, XLog log, Object parameter) {
        final String startLabel = "[start>@" + System.currentTimeMillis();
        final String endLabel = "[end]@" + System.currentTimeMillis();
        artifLog = XLogUtils.addArtificialStartAndEnd(log, startLabel, endLabel);

        parameter = startGUI(context, log);

        XLogHybridILPMinerParametersImpl params = (XLogHybridILPMinerParametersImpl) parameter;

        Object[] result = discoverWithArtificialStartEnd(context, log, artifLog, params);
        if (!params.getDiscoveryStrategy().getDiscoveryStrategyType()
                .equals(DiscoveryStrategyType.CAUSAL_FLEX_HEUR)) {
            Connection paramsConnection = new XLogHybridILPMinerParametersConnection(log, params);
            context.getConnectionManager().addConnection(paramsConnection);
        }

        Petrinet net = (Petrinet) result[0];
        Marking initialMarking = (Marking) result[1];
        Marking finalMarking = (Marking) result[2];

        return new AcceptingPetriNetWrapper(net, initialMarking, finalMarking, parameter);
    }

    @Override
    public Object startGUI(UIPluginContext context, XLog log) {
        Collection<XLogHybridILPMinerParametersConnection> connections = new HashSet<>();
        try {
            connections = context.getConnectionManager().getConnections(XLogHybridILPMinerParametersConnection.class,
                    context, log);
        } catch (ConnectionCannotBeObtained e) {
        }
        Dialog<XLogHybridILPMinerParametersImpl> firstDialog = new ConnectionsClassifierEngineAndDefaultConfigurationDialogImpl(
                context, null, artifLog, connections);
        WizardResult<XLogHybridILPMinerParametersImpl> wizardResult = Wizard.show(context, firstDialog);
        if (wizardResult.getInteractionResult().equals(TaskListener.InteractionResult.FINISHED)) {
            XLogHybridILPMinerParametersImpl params = wizardResult.getParameters();
            return params;
        } else if (wizardResult.getInteractionResult().equals(TaskListener.InteractionResult.CANCEL)) {
            context.getFutureResult(0).cancel(true);
        }
        return null;
    }

    @Override
    public String toString() {
        return "ILP-Miner";
    }
}
