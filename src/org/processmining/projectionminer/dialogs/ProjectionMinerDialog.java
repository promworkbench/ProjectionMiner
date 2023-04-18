package org.processmining.projectionminer.dialogs;

import com.fluxicon.slickerbox.factory.SlickerFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.projectionminer.discoveryalgorithms.*;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.plugins.ESTMinerPlugin;
import org.processmining.projectionminer.parameters.ProjectionMinerParameters;
import org.processmining.projectionminer.utils.Modifiers.PetriNetModifier;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ProjectionMinerDialog extends JPanel {
    // All items in the user interface, that can be handled or read.
    public static final String TITLE = "Configure ProjectionMiner parameters:";
    // ProM concerning ID
    private static final long serialVersionUID = 410560729436210971L;
    // Relevant Dialog fields
    private final JComboBox<DiscoveryPlugin> discoveryAlgorithmComboBox;
    private final JCheckBox initialMergeCheckBox;
    private final JSlider maxDepthSlider;
    private final JCheckBox reduceSilentTransitionsCheckBox;
    private final JCheckBox resetColorCodingCheckBox;

    /**
     * Constructs the user interface for the eST-Miner in ProM.
     *
     * @param log The log containing the traces.
     */
    @SuppressWarnings("unchecked")
    public ProjectionMinerDialog(Petrinet net, XLog log) {
        // Constructs an empty UI with the given title.
//    super(TITLE);
        SlickerFactory factory = SlickerFactory.instance();

        int leftColumnWidth = 200;
        int columnMargin = 20;
        int rowHeight = 40;

        SpringLayout layout = new SpringLayout();
        setLayout(layout);


        final JLabel discoverySettingsLabel;
        {
            discoverySettingsLabel = factory.createLabel("   -- Basic discovery settings --   ");
            add(discoverySettingsLabel);
            layout.putConstraint(SpringLayout.NORTH, discoverySettingsLabel, 5, SpringLayout.NORTH, this);
            layout.putConstraint(SpringLayout.EAST, discoverySettingsLabel, leftColumnWidth, SpringLayout.WEST, this);
        }

        // Chooses the Discovery algorithm
        DiscoveryPlugin[] discoveryVariants = new DiscoveryPlugin[]{new ESTMinerPlugin(), new LocalInductiveMiner(), new LocalILPMiner(), new TransitionSystemRegionMiner(), new LocalHeuristicMiner()};
        final JLabel variantLabel;
        {
            variantLabel = factory.createLabel("Discovery algorithm:");
            add(variantLabel);
            layout.putConstraint(SpringLayout.NORTH, variantLabel, 23, SpringLayout.NORTH, discoverySettingsLabel);
            layout.putConstraint(SpringLayout.EAST, variantLabel, leftColumnWidth, SpringLayout.WEST, this);

            discoveryAlgorithmComboBox = factory.createComboBox(discoveryVariants);
            discoveryAlgorithmComboBox.setPreferredSize(discoveryAlgorithmComboBox.getMaximumSize());
            add(discoveryAlgorithmComboBox);
            layout.putConstraint(SpringLayout.WEST, discoveryAlgorithmComboBox, columnMargin, SpringLayout.EAST, variantLabel);
            layout.putConstraint(SpringLayout.VERTICAL_CENTER, discoveryAlgorithmComboBox, 0, SpringLayout.VERTICAL_CENTER, variantLabel);
        }


        final JLabel maxDepthLabel;
        final JLabel maxDepthValue;
        {
            maxDepthLabel = factory.createLabel("Maximal recursion depth:");
            add(maxDepthLabel);
            layout.putConstraint(SpringLayout.NORTH, maxDepthLabel, 23, SpringLayout.NORTH, variantLabel);
            layout.putConstraint(SpringLayout.EAST, maxDepthLabel, leftColumnWidth, SpringLayout.WEST, this);

            maxDepthSlider = factory.createSlider(SwingConstants.HORIZONTAL);
            maxDepthSlider.setMinimum(0);
            maxDepthSlider.setMaximum(10);
            maxDepthSlider.setValue(5);
            add(maxDepthSlider);
            layout.putConstraint(SpringLayout.WEST, maxDepthSlider, columnMargin, SpringLayout.EAST, maxDepthLabel);
            layout.putConstraint(SpringLayout.EAST, maxDepthSlider, -50, SpringLayout.EAST, this);
            layout.putConstraint(SpringLayout.VERTICAL_CENTER, maxDepthSlider, 0, SpringLayout.VERTICAL_CENTER, maxDepthLabel);
            maxDepthValue = factory.createLabel(Integer.toString(maxDepthSlider.getValue()));
            add(maxDepthValue);
            layout.putConstraint(SpringLayout.VERTICAL_CENTER, maxDepthValue, 0, SpringLayout.VERTICAL_CENTER, maxDepthLabel);
            layout.putConstraint(SpringLayout.WEST, maxDepthValue, columnMargin, SpringLayout.EAST, maxDepthSlider);

            maxDepthSlider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    maxDepthValue.setText(Integer.toString(maxDepthSlider.getValue()));
                }
            });
        }

        final JLabel petriNetManipulationLabel;
        {
            petriNetManipulationLabel = factory.createLabel("-- PN Input & Output manipulation --");
            add(petriNetManipulationLabel);
            layout.putConstraint(SpringLayout.NORTH, petriNetManipulationLabel, 40, SpringLayout.NORTH, maxDepthLabel);
            layout.putConstraint(SpringLayout.EAST, petriNetManipulationLabel, leftColumnWidth, SpringLayout.WEST, this);
        }

        final JLabel initialMergeLabel;
        {
            initialMergeLabel = factory.createLabel("Initial place merging?");
            add(initialMergeLabel);
            layout.putConstraint(SpringLayout.NORTH, initialMergeLabel, 23, SpringLayout.NORTH, petriNetManipulationLabel);
            layout.putConstraint(SpringLayout.EAST, initialMergeLabel, leftColumnWidth, SpringLayout.WEST, this);

            initialMergeCheckBox = factory.createCheckBox("", false);
            initialMergeCheckBox.setEnabled(net != null);
            initialMergeCheckBox.setVisible(net != null);
            if (net != null) {
                initialMergeCheckBox.setPreferredSize(initialMergeCheckBox.getMaximumSize());
                add(initialMergeCheckBox);
                layout.putConstraint(SpringLayout.WEST, initialMergeCheckBox, columnMargin, SpringLayout.EAST, initialMergeLabel);
                layout.putConstraint(SpringLayout.VERTICAL_CENTER, initialMergeCheckBox, 0, SpringLayout.VERTICAL_CENTER, initialMergeLabel);
            } else {
                JLabel noNetMergeLabel = factory.createLabel("[Unavailable: no net given]");
                add(noNetMergeLabel);
                layout.putConstraint(SpringLayout.WEST, noNetMergeLabel, columnMargin, SpringLayout.EAST, initialMergeLabel);
                layout.putConstraint(SpringLayout.VERTICAL_CENTER, noNetMergeLabel, 0, SpringLayout.VERTICAL_CENTER, initialMergeLabel);
            }
        }

        final JLabel reduceSilentTransitionsLabel;
        {
            reduceSilentTransitionsLabel = factory.createLabel("Result: Reduce silent transitions?");
            add(reduceSilentTransitionsLabel);
            layout.putConstraint(SpringLayout.NORTH, reduceSilentTransitionsLabel, 23, SpringLayout.NORTH, initialMergeLabel);
            layout.putConstraint(SpringLayout.EAST, reduceSilentTransitionsLabel, leftColumnWidth, SpringLayout.WEST, this);

            reduceSilentTransitionsCheckBox = factory.createCheckBox("", false);
            reduceSilentTransitionsCheckBox.setPreferredSize(reduceSilentTransitionsCheckBox.getMaximumSize());
            add(reduceSilentTransitionsCheckBox);
            layout.putConstraint(SpringLayout.WEST, reduceSilentTransitionsCheckBox, columnMargin, SpringLayout.EAST, reduceSilentTransitionsLabel);
            layout.putConstraint(SpringLayout.VERTICAL_CENTER, reduceSilentTransitionsCheckBox, 0, SpringLayout.VERTICAL_CENTER, reduceSilentTransitionsLabel);
        }

        final JLabel resetColorCodingLabel;
        {
            resetColorCodingLabel = factory.createLabel("Reset color coding?");
            add(resetColorCodingLabel);
            layout.putConstraint(SpringLayout.NORTH, resetColorCodingLabel, 23, SpringLayout.NORTH, reduceSilentTransitionsLabel);
            layout.putConstraint(SpringLayout.EAST, resetColorCodingLabel, leftColumnWidth, SpringLayout.WEST, this);

            resetColorCodingCheckBox = factory.createCheckBox("", net == null);
            resetColorCodingCheckBox.setPreferredSize(resetColorCodingCheckBox.getMaximumSize());
            add(resetColorCodingCheckBox);
            layout.putConstraint(SpringLayout.WEST, resetColorCodingCheckBox, columnMargin, SpringLayout.EAST, resetColorCodingLabel);
            layout.putConstraint(SpringLayout.VERTICAL_CENTER, resetColorCodingCheckBox, 0, SpringLayout.VERTICAL_CENTER, resetColorCodingLabel);
        }
    }

    public ProjectionMinerParameters apply() {
        ProjectionMinerParameters parameters = new ProjectionMinerParameters((DiscoveryPlugin) discoveryAlgorithmComboBox.getSelectedItem(), initialMergeCheckBox.isSelected(), maxDepthSlider.getValue(), reduceSilentTransitionsCheckBox.isSelected());

        PetriNetModifier.getInstance().updateShouldColor(true);

        if (resetColorCodingCheckBox.isSelected()) {
            PetriNetModifier.getInstance().resetColor();
        }

        return parameters;
    }
}


