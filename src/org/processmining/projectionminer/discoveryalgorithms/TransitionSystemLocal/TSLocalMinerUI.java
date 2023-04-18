package org.processmining.projectionminer.discoveryalgorithms.TransitionSystemLocal;

import com.fluxicon.slickerbox.colors.SlickerColors;
import com.fluxicon.slickerbox.components.NiceSlider;
import com.fluxicon.slickerbox.factory.SlickerDecorator;
import com.fluxicon.slickerbox.factory.SlickerFactory;
import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;
import org.deckfour.uitopia.api.event.TaskListener;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.plugins.transitionsystem.converter.util.TSConversions;
import org.processmining.plugins.transitionsystem.miner.TSMiner;
import org.processmining.plugins.transitionsystem.miner.TSMinerInput;
import org.processmining.plugins.transitionsystem.miner.TSMinerOutput;
import org.processmining.plugins.transitionsystem.miner.modir.TSMinerModirInput;
import org.processmining.plugins.transitionsystem.miner.ui.TSMinerUI;
import org.processmining.plugins.transitionsystem.miner.util.TSAbstractions;
import org.processmining.plugins.transitionsystem.miner.util.TSDirections;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class TSLocalMinerUI extends TSMinerUI {
    private final UIPluginContext context;
    private TSMinerInput settings;
    private Collection<XEventClassifier> classifiers;
    private int introductionStep;
    private int identificationStep;
    private int abstractionStep;
    private int[] classifierStep; // There can be as many classifier steps as classifiers.
    private int transitionStep;
    private int attributeStep;
    private int conversionStep;
    private int overviewStep;
    private int nofSteps;
    private myStep[] mySteps;
    private int currentStep;

    private TSAbstractions abstraction;
    private int horizon;

    public TSLocalMinerUI(final UIPluginContext context) {
        super(context);
        this.context = context;
    }

    public Object[] mine(XLog log, Collection<XEventClassifier> classifiers, XEventClassifier transitionClassifier) {
        TSMinerInput input = new TSMinerInput(context, log, classifiers, transitionClassifier);
        settings = input;
        this.classifiers = classifiers;
        TSMinerOutput output;
        TaskListener.InteractionResult result = TaskListener.InteractionResult.NEXT;

        nofSteps = 0;
        introductionStep = nofSteps++;
        identificationStep = nofSteps++;
        abstractionStep = nofSteps++;
        classifierStep = new int[classifiers.size()];
        for (int i = 0; i < classifiers.size(); i++) {
            classifierStep[i] = nofSteps++;
        }
        attributeStep = nofSteps++;
        transitionStep = nofSteps++;
        conversionStep = nofSteps++;
        overviewStep = nofSteps++;

        mySteps = new myStep[nofSteps];
        mySteps[introductionStep] = new IntroductionStep();
        mySteps[identificationStep] = new IdentificationStep();
        mySteps[abstractionStep] = new AbstractionStep();
        int r = 0;
        for (XEventClassifier classifier : classifiers) {
            mySteps[classifierStep[r++]] = new ClassifierStep(classifier);
        }
        mySteps[attributeStep] = new AttributeStep();
        mySteps[transitionStep] = new TransitionStep();
        mySteps[conversionStep] = new ConversionStep();
        mySteps[overviewStep] = new OverviewStep();

        currentStep = introductionStep;

        while (true) {
            if (currentStep < 0) {
                currentStep = 0;
            }
            if (currentStep >= nofSteps) {
                currentStep = nofSteps - 1;
            }
//			context.log("Current step: " + currentStep);
            result = context
                    .showWizard("TS Miner", currentStep == 0, currentStep == nofSteps - 1, mySteps[currentStep]);
            switch (result) {
                case NEXT:
                    mySteps[currentStep].readSettings();
                    go(1);
                    break;
                case PREV:
                    go(-1);
                    break;
                case FINISHED:
                    TSMiner miner = new TSMiner(context);
                    output = miner.mine(settings);
                    TSMinerLocalPlugin.setLabels(context, log);
                    output.getTransitionSystem().getAttributeMap().put(AttributeMap.LABEL, context.getFutureResult(0).getLabel());

                    return new Object[]{output.getTransitionSystem(), output.getWeights(), output.getStarts(),
                            output.getAccepts()};
                default:
                    context.getFutureResult(0).cancel(true);
                    context.getFutureResult(1).cancel(true);
                    context.getFutureResult(2).cancel(true);
                    context.getFutureResult(3).cancel(true);
                    return new Object[]{null, null, null, null};
            }
        }
    }

    public TSMinerInput getInputWithGUI(XLog log, Collection<XEventClassifier> classifiers, XEventClassifier transitionClassifier) //works the same as mine() but without the mining part, only to use the wizard to set the input object :)
    {
        TSMinerInput input = new TSMinerInput(context, log, classifiers, transitionClassifier);
        settings = input;
        this.classifiers = classifiers;
        TaskListener.InteractionResult result = TaskListener.InteractionResult.NEXT;

        nofSteps = 0;
        introductionStep = nofSteps++;
        identificationStep = nofSteps++;
        abstractionStep = nofSteps++;
        classifierStep = new int[classifiers.size()];
        for (int i = 0; i < classifiers.size(); i++) {
            classifierStep[i] = nofSteps++;
        }
        attributeStep = nofSteps++;
        transitionStep = nofSteps++;
        conversionStep = nofSteps++;
        overviewStep = nofSteps++;

        mySteps = new myStep[nofSteps];
        mySteps[introductionStep] = new IntroductionStep();
        mySteps[identificationStep] = new IdentificationStep();
        mySteps[abstractionStep] = new AbstractionStep();
        int r = 0;
        for (XEventClassifier classifier : classifiers) {
            mySteps[classifierStep[r++]] = new ClassifierStep(classifier);
        }
        mySteps[attributeStep] = new AttributeStep();
        mySteps[transitionStep] = new TransitionStep();
        mySteps[conversionStep] = new ConversionStep();
        mySteps[overviewStep] = new OverviewStep();

        currentStep = introductionStep;

        while (true) {
            if (currentStep < 0) {
                currentStep = 0;
            }
            if (currentStep >= nofSteps) {
                currentStep = nofSteps - 1;
            }
//			context.log("Current step: " + currentStep);
            result = context.showWizard("TS Miner", currentStep == 0, currentStep == nofSteps - 1, mySteps[currentStep]);
            switch (result) {
                case NEXT:
                    mySteps[currentStep].readSettings();
                    go(1);
                    break;
                case PREV:
                    go(-1);
                    break;
                case FINISHED:
                    return settings; //here stops and instead of mining returns the input object
                default:
                    return null;
            }
        }
    }

    private int go(int direction) {
        currentStep += direction;
        if ((currentStep >= 0) && (currentStep < nofSteps)) {
            if (mySteps[currentStep].precondition()) {
                return currentStep;
            } else {
                return go(direction);
            }
        }
        return currentStep;
    }

    private abstract class myStep extends JPanel {
        /**
         *
         */
        private static final long serialVersionUID = 6892655601953727616L;

        public abstract boolean precondition();

        public abstract void readSettings();
    }

    private class IntroductionStep extends myStep {
        /**
         *
         */
        private static final long serialVersionUID = 8620447135225476025L;

        public IntroductionStep() {
            initComponents();
        }

        public boolean precondition() {
            return true;
        }

        private void initComponents() {
            double[][] size = {{TableLayoutConstants.FILL}, {TableLayoutConstants.FILL}};
            setLayout(new TableLayout(size));
            String body = "<p>This wizard will guide you through the process of configuring this miner.</p>";
            body += "<p>The configuration options for this miner can be divided into three categories:<ol>";
            body += "<li>options for configuring state keys (states differ if and only if their keys differ),</li>";
            body += "<li>options for configuring transition labels, and</li>";
            body += "<li>options for configuring post-mining conversions.</li></ol>";
            body += "The wizard will allow you to configure these three categories in the given order.</p>";
            add(SlickerFactory.instance().createLabel("<html><h1>Introduction</h1>" + body), "0, 0, l, t");
        }

        public void readSettings() {
        }
    }

    private class IdentificationStep extends myStep {
        /**
         *
         */
        private static final long serialVersionUID = -5629896729801647063L;
        private JCheckBox jCheckBox1;
        private JList jList1;
        private JList jList2;

        public IdentificationStep() {
            initComponents();
        }

        public boolean precondition() {
            return true;
        }

        private void initComponents() {
            double[][] size = {{TableLayoutConstants.FILL, 20, TableLayoutConstants.FILL},
                    {50, 30, TableLayoutConstants.FILL, 30}};
            setLayout(new TableLayout(size));
            add(SlickerFactory.instance().createLabel("<html><h2>Configure key classifiers</h2>"), "0, 0, 2, 0");

            add(SlickerFactory.instance().createLabel("<html><h3>Select backward keys</h3>"), "0, 1");
            JScrollPane jScrollPane2 = new JScrollPane();
            SlickerDecorator.instance().decorate(jScrollPane2, SlickerColors.COLOR_BG_3, SlickerColors.COLOR_FG,
                    SlickerColors.COLOR_BG_1);
            jList1 = new JList(classifiers.toArray());
            jList1.setSelectionInterval(0, classifiers.size() - 1);
            jScrollPane2.setPreferredSize(new Dimension(250, 300));
            jScrollPane2.setViewportView(jList1);
            add(jScrollPane2, "0, 2");

            add(SlickerFactory.instance().createLabel("<html><h3>Select forward keys</h3>"), "2, 1");
            JScrollPane jScrollPane3 = new JScrollPane();
            SlickerDecorator.instance().decorate(jScrollPane3, SlickerColors.COLOR_BG_3, SlickerColors.COLOR_FG,
                    SlickerColors.COLOR_BG_1);
            jList2 = new JList(classifiers.toArray());
            jScrollPane3.setPreferredSize(new Dimension(250, 300));
            jScrollPane3.setViewportView(jList2);
            add(jScrollPane3, "2, 2");

            jCheckBox1 = SlickerFactory.instance().createCheckBox("Select key data attributes", false);
            add(jCheckBox1, "0, 3, 2, 3");
        }

        public void readSettings() {
            int i = 0;
            for (XEventClassifier classifier : classifiers) {
                settings.getModirSettings(TSDirections.BACKWARD, classifier).setUse(jList1.isSelectedIndex(i));
                settings.getModirSettings(TSDirections.FORWARD, classifier).setUse(jList2.isSelectedIndex(i));
                i++;
            }
            settings.setUseAttributes(jCheckBox1.isSelected());
        }
    }

    private class AbstractionStep extends myStep implements ActionListener {
        /**
         *
         */
        private static final long serialVersionUID = 6933657976761947420L;
        private JRadioButton allEventsRadioButton;
        private JRadioButton nofEventsRadioButton;
        private NiceSlider nofEventsSlider;
        private JRadioButton allStatesRadioButton;
        private JRadioButton nofStatesRadioButton;
        private NiceSlider nofStatesSlider;
        private JRadioButton useMultiset;
        private JRadioButton useSequence;
        private JRadioButton useSet;
        private JRadioButton useFixedLengthSet;

        public AbstractionStep() {
            initComponents();
        }

        public boolean precondition() {
            for (XEventClassifier classifier : settings.getClassifiers()) {
                for (TSDirections direction : TSDirections.values()) {
                    if (settings.getModirSettings(direction, classifier).getUse()) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void initComponents() {
            double[][] size = {{54, TableLayoutConstants.FILL},
                    {50, 30, 20, 20, 20, 20, 30, 20, 20, 30, 20, 20, TableLayoutConstants.FILL}};
            setLayout(new TableLayout(size));
            add(SlickerFactory.instance().createLabel("<html><h2>Configure key classifier collections</h2>"),
                    "0, 0, 1, 0");
            add(SlickerFactory.instance().createLabel("<html><h3>Select collection type</h3>"), "0, 1, 1, 1");

            useSequence = SlickerFactory.instance().createRadioButton("List");
            useMultiset = SlickerFactory.instance().createRadioButton("Multiset");
            useSet = SlickerFactory.instance().createRadioButton("Set");
            useFixedLengthSet = SlickerFactory.instance().createRadioButton("Fixed Length Set");
            ButtonGroup typeGroup = new ButtonGroup();
            typeGroup.add(useSequence);
            typeGroup.add(useMultiset);
            typeGroup.add(useSet);
            typeGroup.add(useFixedLengthSet);
            add(useSequence, "0, 2, 1, 2");
            add(useMultiset, "0, 3, 1, 3");
            add(useSet, "0, 4, 1, 4");
            add(useFixedLengthSet, "0, 5, 1, 5");
            useSet.setSelected(true);

            add(SlickerFactory.instance().createLabel("<html><h3>Select collection size limit</h3>"), "0, 6, 1, 6");
            allEventsRadioButton = SlickerFactory.instance().createRadioButton("No limit");
            nofEventsRadioButton = SlickerFactory.instance().createRadioButton("Limit");
            nofEventsSlider = SlickerFactory.instance().createNiceIntegerSlider("", 0, 100, 1, NiceSlider.Orientation.HORIZONTAL);
            nofEventsSlider.setPreferredSize(new Dimension(420, 20));
            ButtonGroup limitGroup = new ButtonGroup();
            limitGroup.add(allEventsRadioButton);
            limitGroup.add(nofEventsRadioButton);
            add(allEventsRadioButton, "0, 7, 1, 7");
            add(nofEventsRadioButton, "0, 8");
            add(nofEventsSlider, "1, 8");
            nofEventsRadioButton.setSelected(true);

            allEventsRadioButton.addActionListener(this);
            nofEventsRadioButton.addActionListener(this);

            add(SlickerFactory.instance().createLabel("<html><h3>Select transition system size limit</h3>"), "0, 9, 1, 9");
            allStatesRadioButton = SlickerFactory.instance().createRadioButton("No limit");
            nofStatesRadioButton = SlickerFactory.instance().createRadioButton("Limit");
            nofStatesSlider = SlickerFactory.instance().createNiceIntegerSlider("", 0, 1000, settings.getMaxStates(), NiceSlider.Orientation.HORIZONTAL);
            nofStatesSlider.setPreferredSize(new Dimension(420, 20));
            limitGroup = new ButtonGroup();
            limitGroup.add(allStatesRadioButton);
            limitGroup.add(nofStatesRadioButton);
            add(allStatesRadioButton, "0, 10, 1, 10");
            add(nofStatesRadioButton, "0, 11");
            add(nofStatesSlider, "1, 11");
            nofStatesRadioButton.setSelected(true);
        }

        public void readSettings() {
            TSMinerModirInput modirSettings;

            if (useSequence.isSelected()) {
                abstraction = TSAbstractions.SEQUENCE;
            } else if (useMultiset.isSelected()) {
                abstraction = TSAbstractions.BAG;
            } else if (useSet.isSelected()) {
                abstraction = TSAbstractions.SET;
            } else {
                abstraction = TSAbstractions.FIXED_LENGTH_SET;
            }
            if (allEventsRadioButton.isSelected()) {
                horizon = -1;
            } else {
                horizon = nofEventsSlider.getSlider().getValue();
            }
            if (allStatesRadioButton.isSelected()) {
                settings.setMaxStates(-1);
            } else {
                settings.setMaxStates(nofStatesSlider.getSlider().getValue());
            }
            /**
             * Set abstraction and (visible) horizon for all selected
             * combinations of modes (model element, originator, event type) and
             * directions (backward, forward).
             */
            for (TSDirections direction : TSDirections.values()) {
                for (XEventClassifier classifier : settings.getClassifiers()) {
                    modirSettings = settings.getModirSettings(direction, classifier);
                    if (modirSettings.getUse()) {
                        modirSettings.setAbstraction(abstraction);
                        modirSettings.setFilteredHorizon(horizon);
                        modirSettings.setHorizon(-1);
                    }
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            if (e.getSource() == nofEventsRadioButton || e.getSource() == allEventsRadioButton) {
                nofEventsSlider.setEnabled(e.getSource() == nofEventsRadioButton);
            } else if (e.getSource() == nofStatesRadioButton || e.getSource() == allStatesRadioButton) {
                nofStatesSlider.setEnabled(e.getSource() == nofStatesRadioButton);
            }
        }
    }

    private class ClassifierStep extends myStep {

        /**
         *
         */
        private static final long serialVersionUID = -7916606698553515246L;
        private final XEventClassifier classifier;
        Collection<String> s;
        Map<String, Integer> m;
        NiceSlider slider;
        private JScrollPane jScrollPane1;
        private JList nameFilter;

        public ClassifierStep(XEventClassifier classifier) {
            this.classifier = classifier;
            initComponents();
        }

        public boolean precondition() {
            return (settings.getModirSettings(TSDirections.BACKWARD, classifier).getUse() || settings.getModirSettings(
                    TSDirections.FORWARD, classifier).getUse());
        }

        private void initComponents() {
            double[][] size = {{TableLayoutConstants.FILL}, {50, 30, TableLayoutConstants.FILL, 10, 20}};
            setLayout(new TableLayout(size));
            add(SlickerFactory.instance().createLabel("<html><h2>Configure key classifier filter</h2>"), "0, 0");
            add(SlickerFactory.instance().createLabel("<html><h3>Select '" + classifier.name() + "' values</h3>"),
                    "0, 1");

            jScrollPane1 = new JScrollPane();
            SlickerDecorator.instance().decorate(jScrollPane1, SlickerColors.COLOR_BG_3, SlickerColors.COLOR_FG,
                    SlickerColors.COLOR_BG_1);
            XLogInfo info = settings.getLogInfo();
            s = new TreeSet<String>();
            m = new HashMap<String, Integer>();
            for (XEventClass eventClass : info.getEventClasses(classifier).getClasses()) {
                s.add(eventClass.toString());
                m.put(eventClass.toString(), eventClass.size());
            }
            nameFilter = new JList(s.toArray());
            nameFilter.setSelectionInterval(0, info.getEventClasses(classifier).getClasses().size() - 1);
            jScrollPane1.setPreferredSize(new Dimension(450, 300));
            jScrollPane1.setViewportView(nameFilter);

            add(jScrollPane1, "0, 2");

            slider = SlickerFactory.instance().createNiceIntegerSlider("Select top percentage", 0, 100, 80,
                    NiceSlider.Orientation.HORIZONTAL);
            ChangeListener listener = new ChangeListener() {

                public void stateChanged(ChangeEvent e) {
                    // TODO Auto-generated method stub
                    int percentage = slider.getSlider().getValue();
                    int size = 0;
                    TreeSet<Integer> eventSizes = new TreeSet<Integer>();
                    for (String event : s) {
                        size += m.get(event);
                        eventSizes.add(m.get(event));
                    }
                    int treshold = size * percentage;
                    int value = 0;
                    nameFilter.clearSelection();
                    while (100 * value < treshold) {
                        int eventSize = eventSizes.last();
                        eventSizes.remove(eventSize);
                        int index = 0;
                        for (String event : s) {
                            if (m.get(event) == eventSize) {
                                value += eventSize;
                                nameFilter.addSelectionInterval(index, index);
                            }
                            index++;
                        }
                    }
                }

            };
            slider.addChangeListener(listener);
            listener.stateChanged(null);
            add(slider, "0, 4");
        }

        public void readSettings() {
            for (TSDirections direction : TSDirections.values()) {
                if (settings.getModirSettings(direction, classifier).getUse()) {
                    settings.getModirSettings(direction, classifier).setUse(!nameFilter.isSelectionEmpty());
                }
                settings.getModirSettings(direction, classifier).getFilter().clear();
                for (Object object : nameFilter.getSelectedValues()) {
                    if (settings.getModirSettings(direction, classifier).getUse()) {
                        settings.getModirSettings(direction, classifier).getFilter().add(object.toString());
                    }
                }
            }
        }
    }

    private class TransitionStep extends myStep {
        /**
         *
         */
        private static final long serialVersionUID = 5295381313201819465L;
        Collection<String> s;
        Map<String, Integer> m;
        NiceSlider slider;
        private JScrollPane jScrollPane1;
        private JList nameFilter;

        public TransitionStep() {
            initComponents();
        }

        public boolean precondition() {
            return true;
        }

        private void initComponents() {
            double[][] size = {{TableLayoutConstants.FILL}, {50, 30, TableLayoutConstants.FILL, 10, 20}};
            setLayout(new TableLayout(size));
            add(SlickerFactory.instance().createLabel("<html><h2>Configure transition label filter</h2>"), "0, 0");
            add(SlickerFactory.instance().createLabel("<html><h3>Select transition label values</h3>"), "0, 1");

            jScrollPane1 = new JScrollPane();
            XLogInfo info = settings.getLogInfo();
            s = new TreeSet<String>();
            m = new HashMap<String, Integer>();
            /*
             * The transition classifier has been as as default classifier while
             * creating the log info. As a result, getEventClasses() return the
             * classes for this classifier.
             */
            for (XEventClass eventClass : info.getEventClasses().getClasses()) {
                s.add(eventClass.toString());
                m.put(eventClass.toString(), eventClass.size());
            }
            nameFilter = new JList(s.toArray());
            nameFilter.setSelectionInterval(0, info.getEventClasses().getClasses().size() - 1);
            jScrollPane1.setPreferredSize(new Dimension(450, 300));
            jScrollPane1.setViewportView(nameFilter);
            add(jScrollPane1, "0, 2");

            slider = SlickerFactory.instance().createNiceIntegerSlider("Select top percentage", 0, 100, 80,
                    NiceSlider.Orientation.HORIZONTAL);
            ChangeListener listener = new ChangeListener() {

                public void stateChanged(ChangeEvent e) {
                    // TODO Auto-generated method stub
                    int percentage = slider.getSlider().getValue();
                    int size = 0;
                    TreeSet<Integer> eventSizes = new TreeSet<Integer>();
                    for (String event : s) {
                        size += m.get(event);
                        eventSizes.add(m.get(event));
                    }
                    int treshold = size * percentage;
                    int value = 0;
                    nameFilter.clearSelection();
                    while (100 * value < treshold) {
                        int eventSize = eventSizes.last();
                        eventSizes.remove(eventSize);
                        int index = 0;
                        for (String event : s) {
                            if (m.get(event) == eventSize) {
                                value += eventSize;
                                nameFilter.addSelectionInterval(index, index);
                            }
                            index++;
                        }
                    }
                }

            };
            slider.addChangeListener(listener);
            listener.stateChanged(null);
            add(slider, "0, 4");
        }

        public void readSettings() {
            settings.getVisibleFilter().clear();
            for (Object object : nameFilter.getSelectedValues()) {
                settings.getVisibleFilter().add(object.toString());
            }
        }
    }

    private class AttributeStep extends myStep {
        /**
         *
         */
        private static final long serialVersionUID = 7223716214843258396L;
        private JScrollPane jScrollPane1;
        private JList attributeFilter;

        public AttributeStep() {
            initComponents();
        }

        public boolean precondition() {
            return (settings.getUseAttributes());
        }

        private void initComponents() {
            double[][] size = {{TableLayoutConstants.FILL}, {50, 30, TableLayoutConstants.FILL}};
            setLayout(new TableLayout(size));
            add(SlickerFactory.instance().createLabel("<html><h2>Configure key data attributes</h2>"), "0, 0");
            add(SlickerFactory.instance().createLabel("<html><h3>Select key data attributes</h3>"), "0, 1");

            jScrollPane1 = new JScrollPane();
            attributeFilter = new JList(settings.getAttributeFilter().toArray());
            attributeFilter.setSelectionInterval(0, settings.getAttributeFilter().size() - 1);
            jScrollPane1.setPreferredSize(new Dimension(450, 300));
            jScrollPane1.setViewportView(attributeFilter);
            add(jScrollPane1, "0, 2");
        }

        public void readSettings() {
            settings.setUseAttributes(!attributeFilter.isSelectionEmpty());
            settings.getAttributeFilter().clear();
            for (Object object : attributeFilter.getSelectedValues()) {
                settings.getAttributeFilter().add(object.toString());
            }
        }
    }

    private class ConversionStep extends myStep {
        /**
         *
         */
        private static final long serialVersionUID = 4974107535166248951L;
        private JCheckBox improveDiamondCheckBox;
        private JCheckBox mergeInflowCheckBox;
        private JCheckBox mergeOutflowCheckBox;
        private JCheckBox mergeHistoryCheckBox;
        private JCheckBox mergeFutureCheckBox;
        private JCheckBox removeSelfLoopsCheckBox;
        private JCheckBox addArtificalStatesCheckBox;

        public ConversionStep() {
            initComponents();
        }

        public boolean precondition() {
            return true;
        }

        private void initComponents() {
            double[][] size = {{TableLayout.FILL}, {50, 20, 10, 20, 10, 20, 20, 10, 20, 20, 10, 20, TableLayoutConstants.FILL}};
            setLayout(new TableLayout(size));
            add(SlickerFactory.instance().createLabel("<html><h2>Configure post-mining conversions</h2>"), "0, 0");

//			removeSelfLoopsCheckBox = new javax.swing.JCheckBox();
//			improveDiamondCheckBox = new javax.swing.JCheckBox();
//			mergeInflowCheckBox = new javax.swing.JCheckBox();
//			mergeOutflowCheckBox = new javax.swing.JCheckBox();
//			mergeHistoryCheckBox = new javax.swing.JCheckBox();
//			mergeFutureCheckBox = new javax.swing.JCheckBox();
//			addArtificalStatesCheckBox = new javax.swing.JCheckBox();

            removeSelfLoopsCheckBox = SlickerFactory.instance().createCheckBox("Remove self loops", true);
            improveDiamondCheckBox = SlickerFactory.instance().createCheckBox("Improve diamond structure (may be extremely slow)", false);
            mergeInflowCheckBox = SlickerFactory.instance().createCheckBox("Merge states with identical inflow", false);
            mergeOutflowCheckBox = SlickerFactory.instance().createCheckBox("Merge states with identical outflow",
                    false);
            mergeHistoryCheckBox = SlickerFactory.instance().createCheckBox("Merge states with identical history", false);
            mergeFutureCheckBox = SlickerFactory.instance().createCheckBox("Merge states with identical future",
                    false);
            addArtificalStatesCheckBox = SlickerFactory.instance().createCheckBox("Add artifical start and end states",
                    false);

            mergeInflowCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        mergeHistoryCheckBox.setSelected(false);
                    }
                    mergeHistoryCheckBox.setVisible(e.getStateChange() != ItemEvent.SELECTED);
                }
            });
            mergeHistoryCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        mergeInflowCheckBox.setSelected(false);
                    }
                    mergeInflowCheckBox.setVisible(e.getStateChange() != ItemEvent.SELECTED);
                }
            });
            mergeOutflowCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        mergeFutureCheckBox.setSelected(false);
                    }
                    mergeFutureCheckBox.setVisible(e.getStateChange() != ItemEvent.SELECTED);
                }
            });
            mergeFutureCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        mergeOutflowCheckBox.setSelected(false);
                    }
                    mergeOutflowCheckBox.setVisible(e.getStateChange() != ItemEvent.SELECTED);
                }
            });

            add(removeSelfLoopsCheckBox, "0, 1");
            add(improveDiamondCheckBox, "0, 3");
            add(mergeInflowCheckBox, "0, 5");
            add(mergeHistoryCheckBox, "0, 6");
            add(mergeOutflowCheckBox, "0, 8");
            add(mergeFutureCheckBox, "0, 9");
            add(addArtificalStatesCheckBox, "0, 11");

        }

        public void readSettings() {
            settings.getConverterSettings().setUse(TSConversions.KILLSELFLOOPS, removeSelfLoopsCheckBox.isSelected());
            settings.getConverterSettings().setUse(TSConversions.EXTEND, improveDiamondCheckBox.isSelected());
            settings.getConverterSettings().setUse(TSConversions.MERGEBYINPUT, mergeInflowCheckBox.isSelected());
            settings.getConverterSettings().setUse(TSConversions.MERGEBYOUTPUT, mergeOutflowCheckBox.isSelected());
            settings.getConverterSettings().setUse(TSConversions.MERGEBYHISTORY, mergeHistoryCheckBox.isSelected());
            settings.getConverterSettings().setUse(TSConversions.MERGEBYFUTURE, mergeFutureCheckBox.isSelected());
            settings.setAddArtificialStates(addArtificalStatesCheckBox.isSelected());
        }
    }

    private class OverviewStep extends myStep {
        /**
         *
         */
        private static final long serialVersionUID = -2853400854055327898L;
        private JScrollPane jScrollPane1;

        public OverviewStep() {
            initComponents();
        }

        public boolean precondition() {
            makingVisible();
            return true;
        }

        private void initComponents() {
            double[][] size = {{TableLayout.FILL}, {50, TableLayout.FILL}};
            setLayout(new TableLayout(size));
            add(SlickerFactory.instance().createLabel("<html><h2>Check configuration</h2>"), "0, 0");

            jScrollPane1 = new JScrollPane();
            jScrollPane1.setPreferredSize(new Dimension(450, 350));
            jScrollPane1.setViewportView(new JTree());
            add(jScrollPane1, "0, 1");
        }

        /**
         * Updates the overview just before it is being displayed.
         */
        public void makingVisible() {
            /**
             * Construct a new tree.
             */
            DefaultMutableTreeNode topNode = new DefaultMutableTreeNode("TS Miner configuration");
            DefaultMutableTreeNode idNode = new DefaultMutableTreeNode("Key classifiers");
            topNode.add(idNode);
            for (XEventClassifier classifier : classifiers) {
                if (settings.getModirSettings(TSDirections.BACKWARD, classifier).getUse()) {
                    DefaultMutableTreeNode nfNode = new DefaultMutableTreeNode(classifier + " backwards");
                    idNode.add(nfNode);
                    for (String name : settings.getModirSettings(TSDirections.BACKWARD, classifier).getFilter()) {
                        nfNode.add(new DefaultMutableTreeNode(name));
                    }
                }
                if (settings.getModirSettings(TSDirections.FORWARD, classifier).getUse()) {
                    DefaultMutableTreeNode nfNode = new DefaultMutableTreeNode(classifier + " forwards");
                    idNode.add(nfNode);
                    for (String name : settings.getModirSettings(TSDirections.FORWARD, classifier).getFilter()) {
                        nfNode.add(new DefaultMutableTreeNode(name));
                    }
                }
            }
            DefaultMutableTreeNode abNode = new DefaultMutableTreeNode("Collection type");
            idNode.add(abNode);
            if (abstraction == TSAbstractions.SEQUENCE) {
                abNode.add(new DefaultMutableTreeNode("List"));
            } else if (abstraction == TSAbstractions.BAG) {
                abNode.add(new DefaultMutableTreeNode("Multiset"));
            } else if (abstraction == TSAbstractions.SET) {
                abNode.add(new DefaultMutableTreeNode("Set"));
            } else {
                abNode.add(new DefaultMutableTreeNode("Fixed Length Set"));
            }
            abNode = new DefaultMutableTreeNode("Collection size");
            idNode.add(abNode);
            if (horizon == -1) {
                abNode.add(new DefaultMutableTreeNode("No limit"));
            } else {
                abNode.add(new DefaultMutableTreeNode("Limit: " + horizon));
            }
            if (settings.getUseAttributes()) {
                DefaultMutableTreeNode nfNode = new DefaultMutableTreeNode("Key data attributes");
                idNode.add(nfNode);
                for (String name : settings.getAttributeFilter()) {
                    nfNode.add(new DefaultMutableTreeNode(name));
                }
            }
            DefaultMutableTreeNode tNode = new DefaultMutableTreeNode("Transition label filter");
            topNode.add(tNode);
            for (String name : settings.getVisibleFilter()) {
                tNode.add(new DefaultMutableTreeNode(name));
            }
            DefaultMutableTreeNode coNode = new DefaultMutableTreeNode("Post-mining conversions");
            topNode.add(coNode);
            boolean b = false;
            if (settings.getConverterSettings().getUse(TSConversions.KILLSELFLOOPS)) {
                coNode.add(new DefaultMutableTreeNode("Remove self loops"));
                b = true;
            }
            if (settings.getConverterSettings().getUse(TSConversions.EXTEND)) {
                coNode.add(new DefaultMutableTreeNode("Improve diamond structure"));
                b = true;
            }
            if (settings.getConverterSettings().getUse(TSConversions.MERGEBYINPUT)) {
                coNode.add(new DefaultMutableTreeNode("Merge states with identical inflow"));
                b = true;
            }
            if (settings.getConverterSettings().getUse(TSConversions.MERGEBYOUTPUT)) {
                coNode.add(new DefaultMutableTreeNode("Merge states with identical outflow"));
                b = true;
            }
            if (!b) {
                coNode.add(new DefaultMutableTreeNode("None"));
            }
            /**
             * Display the new tree.
             */
            jScrollPane1.setViewportView(new JTree(topNode));
        }

        public void readSettings() {
        }
    }
}
