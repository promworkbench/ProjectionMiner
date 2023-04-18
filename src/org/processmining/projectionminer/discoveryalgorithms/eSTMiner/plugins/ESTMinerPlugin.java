package org.processmining.projectionminer.discoveryalgorithms.eSTMiner.plugins;

import nl.tue.alignment.Progress;
import nl.tue.alignment.Replayer;
import nl.tue.astar.AStarException;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.projectionminer.discoveryalgorithms.AcceptingPetriNetWrapper;
import org.processmining.projectionminer.discoveryalgorithms.DiscoveryPlugin;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.candidatetraverser.AbstractCandidateTraverser;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.candidatetraverser.BFSCandidateTraverser;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.candidatetraverser.DFSCandidateTraverser;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.discovery.AbstractDiscovery;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.discovery.DeltaDiscovery;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyLog;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyPlace;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyProcessModel;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.implicitplaceremoval.AbstractImplicitPlacesRemover;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.implicitplaceremoval.OptimizationBasedImplicitPlaceRemover;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.implicitplaceremoval.ReplayBasedImplicitPlacesRemover;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.placecombinators.BFSDeltaCombinator;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.placeevaluators.MyPlaceEvaluator;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.ui.wizard.ListWizard;
import org.processmining.framework.util.ui.wizard.ProMWizardDisplay;
import org.processmining.framework.util.ui.wizard.ProMWizardStep;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithILP;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.PNLogReplayer;
import org.processmining.plugins.petrinet.replayer.algorithms.IPNReplayAlgorithm;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.pnml.exporting.PnmlExportNetToPNML;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class ESTMinerPlugin extends DiscoveryPlugin {

    ///Provided by Mohammadreza for ETConformance: to return the mapping of activities between Petri net and the log
    private static TransEvClassMapping constructMapping(PetrinetGraph net, XLog log, XEventClass dummyEvClass,
                                                        XEventClassifier eventClassifier) {
        TransEvClassMapping mapping = new TransEvClassMapping(eventClassifier, dummyEvClass);
        XLogInfo summary = XLogInfoFactory.createLogInfo(log, eventClassifier);
        for (Transition t : net.getTransitions()) {
            boolean mapped = false;
            for (XEventClass evClass : summary.getEventClasses().getClasses()) {
                String id = evClass.getId();
                String label = t.getLabel();

                if (label.equals(id)) {
                    mapping.put(t, evClass);
                    mapped = true;
                    break;
                }
            }
        }
//		System.out.println("Mapping computed for precision plugin: ");
//		System.out.println(mapping);
        return mapping;
    }

    public Object[] discover(UIPluginContext context, XLog inputLog) throws InterruptedException {
        System.out.println("_____________ eST - Miner ___________________________________________________________________________________");
        System.out.println(System.getProperty("java.class.path"));
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        System.out.println(Arrays.toString(sysloader.getURLs()));


        Parameters parameters = getGeneralParameters(inputLog, context);  //set general parameters using user input

        String resultBasePath = "resultsCollection/"; //specify path for outputs (if not given by Cluster)
        String logName = inputLog.getAttributes().get("concept:name").toString(); //specify logName (if not given by Cluster)
        return runDiscovery(inputLog, logName, parameters, context, 0, resultBasePath);
    }


    //_____________________________END OF MAIN - Methods and Helper Functions__________________________________________________________

    public Object[] runDiscovery(XLog xlog, String logname, Parameters parameters, PluginContext context, int run_id, String resultBasePath) {

        boolean printResults = false;

        //taking a copy of the input log to work with
        XLog inputLogCopy = (XLog) xlog.clone();
        //Pre-process the Log (create usable log object)
        System.out.println("Preprocessing the Log...");
        MyLog log = new MyLog(inputLogCopy, parameters);
        final String[] transitions = log.getTransitions();
        final int[] outTransitionsMapping = log.getOutTransitionMapping();
        double[][] DFScores = log.computeDFScores();

        log.printBasicLogSummary();
        log.printTransitionOrderings();
        final int P_all = (int) (Math.pow(Math.pow(2, (transitions.length - 1)), 2) - 2 * Math.pow(2, (transitions.length - 1)) + 1);

//	System.out.println("Potential Places Queue Max Length: "+parameters.getPotentialPlacesMaxLength());

        final ArrayList<ArrayList<Integer>> traceVariants = log.getTraceVariants();
        final HashMap<ArrayList<Integer>, Integer> traceVariantCounts = log.getTraceVariantCounts();

        //setting the number of traceVariants within the class MyPlace
        MyPlace.setNumVariants(traceVariants.size());

        //Initialize Process Model (unique transitions and empty list of places)
        ArrayList<MyPlace> places = new ArrayList<MyPlace>();
        MyProcessModel pM = new MyProcessModel(places, transitions, traceVariants.size());

        //-------------Initialize "working classes"-------------------------------------------------------------------------------------------------------
        //TODO ensure all IP removers are usable (currently only replay based works)
        AbstractImplicitPlacesRemover IPRemover = null;
        switch (parameters.getImpRemoveVariant()) {
            case "Replay":
                IPRemover = new ReplayBasedImplicitPlacesRemover(transitions, log, parameters.getMax_depth());
                break;
            case "LPP":
                IPRemover = new OptimizationBasedImplicitPlaceRemover(transitions, log);
                break;
            default:
                System.out.println("WARNING: no valid implicit place removal selected");
                break;
        }

        //select traverser based on chosen traversal strategy
        AbstractCandidateTraverser candidates = null;
        switch (parameters.getTraversalStrategy()) {
            case "DFS":
                System.out.println("Chosen traversal strategy: DFS");
                candidates = new DFSCandidateTraverser(transitions, outTransitionsMapping, parameters);
                break;
            case "BFS":
                System.out.println("Chosen traversal strategy: BFS");
                candidates = new BFSCandidateTraverser(transitions, outTransitionsMapping, parameters);
                break;
            default:
                System.out.println("WARNING: no valid candidate traversal strategy selected.");
                break;
        }


        MyPlaceEvaluator evaluator = new MyPlaceEvaluator(pM, traceVariants, traceVariantCounts, parameters.getThresholdTau(), DFScores);

        int tauAbsolute = (int) (parameters.getThresholdTau() * log.getNumOfTraces());
        int deltaAbsolute = (int) (parameters.getThresholdDelta() * log.getNumOfTraces());
        int[] traceCountsArray = new int[traceVariantCounts.size()];
        //TODO make sure that order of traces remains the same
        for (int i = 0; i < traceVariants.size(); i++) {
            ArrayList<Integer> currentVariant = traceVariants.get(i);
            traceCountsArray[i] = traceVariantCounts.get(currentVariant);
        }

        BFSDeltaCombinator combinator = new BFSDeltaCombinator(parameters.getDeltaAdaption(), parameters.getDeltaAdaptionSteepness(), tauAbsolute, deltaAbsolute, traceCountsArray, parameters.getMax_depth(), parameters.getPotentialPlacesMaxLength(), log);

        AbstractDiscovery discovery = null;
        switch (parameters.getDiscoveryVariant()) {
            case "Classic":
//TODO				discovery = new ClassicDiscovery(pM, candidates, evaluator, IPRemover, parameters, log);
                break;
            case "Delta":
                discovery = new DeltaDiscovery(pM, candidates, evaluator, IPRemover, parameters, log, combinator);
                break;
            default:
                System.out.println("WARNING: no valid discovery method selected.");
                break;
        }

        parameters.print();

        //---------------------Evaluating Places (the important part)---------------------------------------------------------------------

        System.out.println("--------------------------------------------------------------------------");
        System.out.println("Start adding places...");
        //Evaluating and possibly adding the candidate places
        discovery.start();
        try {
            discovery.join(parameters.getTimeAllowance());
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        if (discovery.isAlive()) {
            discovery.interrupt();
        }

        System.out.println("End of Discovery____________________________________________________________________________________________ \n");

        //---------------------------Preliminary Results----------------------------------------------------
        MyProcessModel discoveredPM = discovery.getpM();
        System.out.println("Remaining Potential Places after Discovery: " + discoveredPM.getPotentialPlaces().size());
        PlugInStatistics.instance().setRemainingPotentialPlaces(discoveredPM.getPotentialPlaces().size());
        System.out.println("Discarded Places after Discovery: " + discoveredPM.getDiscardedPlaces().size());
        if (discoveredPM.getDiscardedPlaces().size() != PlugInStatistics.instance().getNumDiscardedPlaces()) {
            System.out.println("ERROR counting discared places!");
        }

        final int P_replayed = PlugInStatistics.instance().getNumFitting() + PlugInStatistics.instance().getNumUnfitting();
        final int cutoffP = (P_all - P_replayed);
        final double cutoffPPercentageP_all = ((cutoffP * 100.0) / P_all);

        //print results for debugging
        System.out.println("------------------------------------------------------------------------------------------------------");
        System.out.println("P-all: " + P_all);
        System.out.println("Number of checked unfitting Places (Tree Traversal): " + PlugInStatistics.instance().getNumUnfitting());
        System.out.println("P-fit (Tree Traversal): " + PlugInStatistics.instance().getNumFitting());
        System.out.println("Cutoff Places: " + cutoffP + " | " + cutoffPPercentageP_all + " % of P-all");
//	System.out.println("------------------------------------------------------------------------------------------------------");
//	System.out.println("Adding Places needed: " + (PlugInStatistics.instance().getTimeEval()) + " ms");
//	System.out.println("of this for finding tree candidates: " + (PlugInStatistics.instance().getTimeCandFind()) + " ms");
//	System.out.println("Of this for Replay (Treetraversal): " + PlugInStatistics.instance().getTimeReplay() + " ms");
        System.out.println("-----------------------------------------------------------------------------------------------------");
        discoveredPM.updateAndPrintStatus(log);
        System.out.println("-----------------------------------------------------------------------------------------------------");


        //--------------------------Post-Processing---------------------------------------------------------------------------------
        System.out.println("Start post-processing...");
        if (parameters.isRemoveImps()) {
            discoveredPM.updateAndPrintStatus(log);

            //final implicit places removal
            final long IPRemoveStart = System.currentTimeMillis();
            System.out.println("\n Number of places before final removing implicit places: " + discoveredPM.getPlaces().size());
            if (parameters.isRepairWhileRemovingIPs()) {
                discoveredPM = IPRemover.removeAllIPsAndRepair(discoveredPM);
            } else {
                discoveredPM = IPRemover.removeAllIPs(discoveredPM);
            }
            PlugInStatistics.instance().incTimeImpTest(System.currentTimeMillis() - IPRemoveStart);
            IPRemover = new OptimizationBasedImplicitPlaceRemover(transitions, log); //for reliable implicit place removal TODO place more elegantly
            discoveredPM = IPRemover.removeAllIPs(discoveredPM);
            System.out.println("Number of places after final removing implicit places: " + discoveredPM.getPlaces().size() + "\n");

            discoveredPM.printPlaceSummary(log);
        } else {
            System.out.println("No IP Removal - Model has " + discoveredPM.getPlaces().size() + " places.");
        }

        // Merging Self Loop Places - important to do AFTER IP Removal
        System.out.println("\n Merging self-loop places...");
        int numPlacesBeforeMerging = discoveredPM.getPlaces().size();
        discoveredPM = discoveredPM.mergeSelfLoopPlaces(log);
        discoveredPM.printPlaceSummary(log);
        PlugInStatistics.instance().setNumMergedPlaces(numPlacesBeforeMerging - pM.getPlaces().size());
        discoveredPM.updateAndPrintStatus(log);


        discoveredPM.updateAndPrintStatus(log);
        System.out.println("P-final: " + discoveredPM.getPlaces().size());
        System.out.println("-------------------------------------------------------------------------------------");


        //-----------------create Petrinet from Process Model-------------------------------------------------------------------------


        //compute transitions and places that remain used
        boolean[] transitionsLiveness = discoveredPM.getTransitionsLiveness();
        int numLiveTransitions = 0;
        int numDeadTransitions = 0;
        int startIndex = log.findStartIndex(parameters, transitions);
        int endIndex = log.getInEndIndex();
        String isRemainingTransition = "";
        String isNotRemainingTransition = "";
        for (int i = 0; i < transitionsLiveness.length; i++) {
            if (transitionsLiveness[i]) {
                isRemainingTransition = isRemainingTransition + transitions[i] + ", ";
                numLiveTransitions++;
            } else {
                isNotRemainingTransition = isNotRemainingTransition + transitions[i] + ", ";
                numDeadTransitions++;
            }
        }
        System.out.println(numLiveTransitions + " live transitions: " + isRemainingTransition);
        System.out.println(numDeadTransitions + " dead transitions: " + isNotRemainingTransition);

        //debugging: identify places with insufficient connections
        discoveredPM = removeDeadPlaces(discoveredPM, transitionsLiveness);


        //initialize PN
        Petrinet net = PetrinetFactory.newPetrinet("Process Model");
        Marking initial_marking = new Marking();
        Marking final_marking = new Marking();
        //add start place and add it to to initial markings
        Place startP = net.addPlace("Start");
        initial_marking.add(startP);
        //add end place and add it to final markings
        Place endP = net.addPlace("End");
        final_marking.add(endP);


        //add transitions and connecting arcs for start and end place (do not add unused transitions)
        for (int i = 0; i < discoveredPM.getTransitions().length; i++) {//add the USED transitions to the petri net
            if (transitionsLiveness[i]) {
                Transition t = net.addTransition(discoveredPM.getTransitions()[i]);
                if (i == startIndex) {
                    net.addArc(startP, t);
                    t.setInvisible(true);
                } else if (i == endIndex) {
                    net.addArc(t, endP);
                    t.setInvisible(true);
                }
            }
        }


        //add places from process model (
        final Collection<Transition> petriTransitions = net.getTransitions();
        for (MyPlace myP : discoveredPM.getPlaces()) {
            Place newP = net.addPlace("");
            for (Transition t : petriTransitions) {
                if (getTransitionNames(myP.getInputTrKey(), transitions).contains(t.getLabel())) {
                    net.addArc(t, newP);
                }
                if (getTransitionNames(myP.getOutputTrKey(), transitions).contains(t.getLabel())) {
                    net.addArc(newP, t);
                }
            }
        }
        context.getConnectionManager().addConnection(new InitialMarkingConnection(net, initial_marking));
        context.getConnectionManager().addConnection(new FinalMarkingConnection(net, final_marking));

        System.out.println("Number of Arcs: " + net.getEdges().size());
        System.out.println("Number of Places: " + net.getPlaces().size());
        System.out.println("____________________________________________________________________________________");


        //-----------------------------------------------------------------------------------------------------------------------------

        //checking fitness and precision using external plugins


//        System.out.println("Computing alignment-based fitness...");
//        //checking fitness using alignments
//        PNRepResult alignedTraces = computeAlignment(log.getxLog(), parameters.getClassifier(), net, initial_marking, final_marking, context);
//        System.out.println("Shortest Path Through Model: " + alignedTraces.getInfo().get(Replayer.MAXMODELMOVECOST));
//        double lengthOfShortestPathTroughModel = Double.parseDouble((String) alignedTraces.getInfo().get(Replayer.MAXMODELMOVECOST));
//        HashMap<Double, Integer> summaryTraceFitness = computeTraceFitnessSummary(alignedTraces, lengthOfShortestPathTroughModel); //key: count of unfitting moves, value: frequency of that move
//        double average_trace_fitness = computeAverageTraceFitness(summaryTraceFitness);
//
//        System.out.println("Computing ETC precision...");
//        /// ETC Plugin - Precision
//        XEventClass dummyEvClass = new XEventClass("DUMMY", 99999);
//        TransEvClassMapping mapping = constructMapping(net, log.getxLog(), dummyEvClass, parameters.getClassifier());
//        ETCResults res = new ETCResults();
////	ETCSettings sett = new ETCSettings(res);
////	sett.initComponents();
////	sett.setSettings();
//        ETCPlugin etcPlugin = new ETCPlugin();
//        Object[] etcResults = etcPlugin.doETC(context, log.getxLog(), net, initial_marking, mapping, res);
//        double precision = res.getEtcp();


        //formatting and summarizing statistics
//        double binaryFitness = (double) discoveredPM.getNumLiveTraces(log) / (double) log.getNumOfTraces();
//        double vaiantFitness = (double) discoveredPM.countLiveVariants() / (double) discoveredPM.getVariantVector().length;
//        precision = Math.round(precision * 10000.0) / 10000.0; //round to fourth decimal
//        average_trace_fitness = Math.round(average_trace_fitness * 10000.0) / 10000.0; //round to fourth decimal //TODO comment out for cluster
//        binaryFitness = Math.round(binaryFitness * 10000.0) / 10000.0; //round to fourth decimal
//        vaiantFitness = Math.round(vaiantFitness * 10000.0) / 10000.0; //round to fourth decimal

//        PlugInStatistics.instance().setPrecision(precision);
////	PlugInStatistics.instance().setPrecision(-1); //TODO set -1 for cluster (cannot compute)
//        PlugInStatistics.instance().setAlignmentBasedFitness(average_trace_fitness);
////	PlugInStatistics.instance().setAlignmentBasedFitness(-1); //TODO set -1 for cluster (cannot compute)
//        PlugInStatistics.instance().setBinaryFitness(binaryFitness);
//        PlugInStatistics.instance().setVariantFitness(vaiantFitness);

        if (printResults) {

            System.out.println("Exporting results...");
            String runIdentifier = createFileID(run_id, parameters, logname); //hopefully, the second option is the name of the log TODO

            //export results accpeting Petri net
            String pnmlLocation = resultBasePath + "PNML/";
            File pnmlFile = new File(pnmlLocation + runIdentifier + ".pnml");
            PnmlExportNetToPNML pnmlExporter = new PnmlExportNetToPNML();
            try {
                pnmlExporter.exportPetriNetToPNMLFile(context, net, pnmlFile);
            } catch (IOException e) {
                //  Auto-generated catch block
                e.printStackTrace();
            }

            //export summary statistics to csv
            String csvLocationTotal = resultBasePath + "Total/";
            FileWriter csvWriter;
            try {
                csvWriter = new FileWriter(csvLocationTotal + runIdentifier + "-Total.csv");
                csvWriter.append(compute_CSV_Header() + "\n" + compute_CSV_Content(parameters, discoveredPM, net, run_id, logname));
                csvWriter.flush();
                csvWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //export level wise results to csv
            String csvLocationLevel = resultBasePath + "Level/";
            //compute file content
            HashMap<Integer, HashMap<String, Integer>> levelstatistics = PlugInStatistics.instance().getLevelStatistics();
            String nl = "\n"; //new line indicator
            String sep = ";"; //separator
            String levelwise_statistics_header = "After this level" + sep;
            //compute headers
            ArrayList<String> columnHeaders = new ArrayList<String>(levelstatistics.get(0).keySet()); //get keyset

            for (String column_header : columnHeaders) {
                levelwise_statistics_header = levelwise_statistics_header + column_header + sep;
            }
            String fileContent = levelwise_statistics_header + nl;
            //compute contents using the keyset above to manatain order
            for (int currentLevel : levelstatistics.keySet()) {//iterate over all levels
                String levelwise_content = +currentLevel + sep;
                HashMap<String, Integer> currentLevelStatistics = levelstatistics.get(currentLevel);
                for (String column : columnHeaders) {
                    levelwise_content = levelwise_content + currentLevelStatistics.get(column) + sep;
                }
                fileContent = fileContent + levelwise_content + nl;
            }
            //export content to file
            try {
                csvWriter = new FileWriter(csvLocationLevel + runIdentifier + "-Level.csv");
                csvWriter.append(fileContent);
                csvWriter.flush();
                csvWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }


        //-----------------------------------------------------------------------------------------------------------------------------

        //reformat and print results
        System.out.println("____________________________________________________________________________________");
        PlugInStatistics.instance().printStatisticsToConsol();
        System.out.println("____________________________________________________________________________________");

        //return results
        PlugInStatistics.instance().resetStatistics();
        return new Object[]{net, initial_marking, final_marking, log.createXLog(discoveredPM.getVariantVector())};
    }


    //unsorted

    private MyProcessModel removeDeadPlaces(MyProcessModel pM, boolean[] transitionsLiveness) {
        ArrayList<MyPlace> livePlaces = pM.getPlaces();
        ArrayList<MyPlace> deadPlaces = new ArrayList<MyPlace>();
        for (MyPlace place : pM.getPlaces()) {
            boolean isNotProperlyConnected = hasToFewLiveConnections(place, transitionsLiveness);
            if (isNotProperlyConnected) {
                System.out.println("Remove place with too few live transitions: " + place.toTransitionsString(pM.getTransitions()));
                deadPlaces.add(place);
            }
        }
        livePlaces.removeAll(deadPlaces);
        pM.setPlaces(livePlaces);
        return pM;
    }

    private String createFileID(int run_id, Parameters parameters, String logName) {
        String filename = logName + "-" + run_id + "-" + parameters.getParamString();
        return filename;
    }

    private String compute_CSV_Header() {
        String nl = "\n"; //new line indicator
        String sep = ";"; //separator

        //log name; run id;
        String input = "Log" + sep + "run_ID";

        //tau; delta; limit_potentialPlaces; delta_adaption_strategy; delta_adaption_steepness; max_traversal_depth;
        //	max_virtual_depth_modifier; concurrent_IP_removal;
        String settings = "Tau" + sep
                + "Delta" + sep
                + "PotentialPlacesMax" + sep
                + "DeltaAdaptionStrat" + sep
                + "DeltaAdaptionSteep" + sep
                + "MaxTraversalDepth" + sep
                + "VirtualLevelsMod" + sep
                + "ConcurrentIPRemoval";

        //alignemntFitness; binaryFitnes; variantFitness; precision; time; numPlaces; numTransitions; numArcs
        String general_outcome = "Fitness Alignment-Based" + sep
                + "FractionFitTraces" + sep
                + "FractionFitVariants" + sep
                + "ETC Precision" + sep
                + "PMnumPlaces" + sep
                + "PMnumTransitions" + sep
                + "PMnumEdges";

        //fittingPlaces; unfittingPlaces; imlicitPlaces; mergedSelfLoopPlaces
        String general_results = "FittingPlacesEvaluated" + sep
                + "UnfittingPlacesEvaluated" + sep
                + "ImplicitPlacesEvaluated" + sep
                + "MergedSelfloopPlaces";

        //deadTransitions; discardedPlaces; acceptedPlaces; skippedPlaces;remainingPlaces; delayedPlaces;
        String delta_results = "NumDeadTransitions" + sep
                + "numDiscardedPlaces" + sep
                + "numAcceptedPlaces" + sep
                + "numSkippedPotentialPlaces" + sep
                + "numRemainingPotentialPlaces" + sep
                + "numDelayedPlaces";


        return input + sep
                + settings + sep
                + general_outcome + sep
                + general_results + sep
                + delta_results;
    }

    private String compute_CSV_Content(Parameters parameters, MyProcessModel pM, Petrinet net, int run_id, String logname) {
        String nl = "\n"; //new line indicator
        String sep = ";"; //separator

        //log name; run id;
        String input = logname + sep + run_id; //TODO check wether there is a better wy to get the log name

        //tau; delta; limit_potentialPlaces; delta_adaption_strategy; delta_adaption_steepness; max_traversal_depth;
        //	max_virtual_depth_modifier; concurrent_IP_removal;
        String settings = parameters.getThresholdTau() + sep
                + parameters.getThresholdDelta() + sep
                + parameters.getPotentialPlacesMaxLength() + sep
                + parameters.getDeltaAdaption() + sep
                + parameters.getDeltaAdaptionSteepness() + sep
                + parameters.getMax_depth() + sep
                + parameters.getVirtualLevelsModifier() + sep
                + parameters.isRemoveImpsConcurrently();

        //alignemntFitness; binaryFitnes; variantFitness; precision; time; numPlaces; numTransitions; numArcs
        String general_outcome = PlugInStatistics.instance().getAlignmentBasedFitness() + sep
                + PlugInStatistics.instance().getBinaryFitness() + sep
                + PlugInStatistics.instance().getVariantFitness() + sep
                + PlugInStatistics.instance().getPrecision() + sep
                + net.getPlaces().size() + sep
                + net.getTransitions().size() + sep
                + net.getEdges().size();

        //fittingPlaces; unfittingPlaces; imlicitPlaces; mergedSelfLoopPlaces
        String general_results = PlugInStatistics.instance().getNumFitting() + sep
                + PlugInStatistics.instance().getNumUnfitting() + sep
                + PlugInStatistics.instance().getNumImpPlace() + sep
                + PlugInStatistics.instance().getNumMergedSelfloopPlaces();

        //deadTransitions; discardedPlaces; acceptedPlaces; skippedPlaces;remainingPlaces; delayedPlaces;
        String delta_results = pM.getNumDeadTransitions() + sep
                + PlugInStatistics.instance().numDiscardedPlaces + sep
                + PlugInStatistics.instance().numAcceptedPlaces + sep
                + PlugInStatistics.instance().numSkippedPotentialPlaces + sep
                + PlugInStatistics.instance().numRemainingPotentialPlaces + sep
                + PlugInStatistics.instance().numDelayedPlaces;


        return input + sep
                + settings + sep
                + general_outcome + sep
                + general_results + sep
                + delta_results;
    }


    //------------------------------------ User Input Handling -----------------------------------------------------

    //returns true if the place does not have at least one live connection for ingoing and outgoing
    private boolean hasToFewLiveConnections(MyPlace place, boolean[] transitionsLiveness) {
        MyPlace clonedPlace = place.clone().removeDeadTransitions(transitionsLiveness);
        return Integer.bitCount(clonedPlace.getInputTrKey()) < 1 || Integer.bitCount(clonedPlace.getOutputTrKey()) < 1;
    }


    //------ Activity Scoring (used in uniqired, needs updating!)-------------------------------------------------------------------------------------------------------

    //get input independent general parameters from user
    private Parameters getGeneralParameters(XLog log, UIPluginContext context) {
        UI myMinerWizardStep = new UI(log);
        List<ProMWizardStep<Parameters>> wizStepList = new ArrayList<>();
        wizStepList.add(myMinerWizardStep);
        ListWizard<Parameters> listWizard = new ListWizard<>(wizStepList);
        return ProMWizardDisplay.show(context, listWizard, new Parameters());
    }

    //choose method for scoring activities as specified by parameters (default: no scoring)
    private HashMap<String, Float> computeActivityScores(XLog log, Parameters parameters) {
        return defaultActivityScores(log, parameters.getClassifier());
        // TODO compute score based on parameter settings (not implemented yet)
    }


    //------ Transition Array Orderings   ----------------------------------------------------------------------------------------

    //scores each activity with 0
    private HashMap<String, Float> defaultActivityScores(XLog log, XEventClassifier classifier) {
        HashMap<String, Float> defaultScores = new HashMap<String, Float>();
        for (XTrace trace : log) {
            for (XEvent event : trace) {
                String key = classifier.getClassIdentity(event);
                defaultScores.put(key, (float) 0);
            }
        }
        return defaultScores;
    }

    //compute in transition order according to scores as specified by parameters
    private String[] computeInTransitions(XLog log, Parameters parameters) {
        final HashMap<String, Float> activityScores = computeActivityScores(log, parameters);
        String[] intransitions = createOrderedTransitions(activityScores);
        return intransitions;
    }


    //--------Scoring of paired sets of transitions (places)---------------------------------------------------------------

    //create transition array ordered according to sorting of scores hash map
    private String[] createOrderedTransitions(HashMap<String, Float> activityScores) {
        Collection<String> scoredActivities = activityScores.keySet();
        ArrayList<String> transitionList = new ArrayList<String>();
        ArrayList<Float> tempScores = new ArrayList<Float>(activityScores.values());
        tempScores.sort(null);
        Iterator<Float> iterator = tempScores.iterator();
        while (iterator.hasNext()) {
            Float value = iterator.next();
            for (String activityName : scoredActivities) {
                if (activityScores.get(activityName) == value) { //the key "activity" is mapped to the value
                    transitionList.add(activityName);
                }
            }
            while (!tempScores.isEmpty() && (tempScores.get(0) == value)) {//ensure the transitions mapped to from the current score are added only once
                tempScores.remove(0);
            }
            iterator = tempScores.iterator();
        }
        String[] transitions = new String[transitionList.size()];
        for (int i = 0; i < transitions.length; i++) {
            transitions[i] = transitionList.get(i);
        }
        return transitions;
    }


    //--------------------- General Helper Functions ------------------------------------------------------------

    //computes directly follows scores
    private double[][] computeDFScores(ArrayList<ArrayList<Integer>> convertedLog, String[] transitions) {
        //compute df occuernces and single occurences
        double[][] result = new double[transitions.length][transitions.length];
        int[] occurences = new int[transitions.length];
        for (ArrayList<Integer> trace : convertedLog) {
            int lastTransitionIndex = trace.get(0);
            occurences[lastTransitionIndex] = occurences[lastTransitionIndex] + 1;
            for (int traceIndex = 1; traceIndex < trace.size(); traceIndex++) {
                int thisTransitionIndex = trace.get(traceIndex);
                occurences[thisTransitionIndex] = occurences[thisTransitionIndex] + 1;
                result[lastTransitionIndex][thisTransitionIndex] = result[lastTransitionIndex][thisTransitionIndex] + 1;
                lastTransitionIndex = thisTransitionIndex;
            }
        }
        //compute scores
        for (int first = 0; first < transitions.length; first++) {
            for (int second = 0; second < transitions.length; second++) {
                result[first][second] = result[first][second] / (occurences[first] * occurences[second]);
            }
        }
        return result;
    }

    //returns a collection containing all transitions names from the given transitions array
    private Collection<String> getTransitionNames(final int key, final String[] transitions) {
        Collection<String> result = new ArrayList<String>();
        if (key > (Math.pow(2, transitions.length))) {
            return null;
        }
        for (int i = 0; i < transitions.length; i++) {
            if ((key & getMask(i, transitions)) > 0) { //test key for ones
                result.add(transitions[i]);
            }
        }
        return result;
    }


    ////// ---------------helper functions: compute fitness & precision---------------------------------

    //for a given position in the transition array return the corresponding bitmask
    private int getMask(final int pos, final String[] transitions) {
        return 1 << (transitions.length - 1 - pos);
    }

    //Provided by Tobias: create mapping transition -> eventclass
    public TransEvClassMapping computeMappingTobiasStyle(XEventClass dummy, Petrinet net, XEventClasses eventClasses) {
        TransEvClassMapping mapping;
        mapping = new TransEvClassMapping(eventClasses.getClassifier(), dummy);
        for (Transition t : net.getTransitions()) {
            if (t.isInvisible()) {
                mapping.put(t, dummy);
            } else {
                mapping.put(t, eventClasses.getByIdentity(t.getLabel()));
            }
        }
        return mapping;
    }


    public PNRepResult computeAlignmentTobias(XLog xlog, XEventClassifier classifier, Petrinet net, Marking iMarking, Marking fMarking) {
        XEventClasses eventClasses = XEventClasses.deriveEventClasses(classifier, xlog);
        XEventClass dummy = new XEventClass("", 1);
        TransEvClassMapping mapping = computeMappingTobiasStyle(dummy, net, eventClasses);

        PNLogReplayer replayer = new PNLogReplayer();
        CostBasedCompleteParam replayParameters = new CostBasedCompleteParam(eventClasses.getClasses(), dummy,
                net.getTransitions(), 1, 1);
        replayParameters.setInitialMarking(iMarking);
        replayParameters.setMaxNumOfStates(Integer.MAX_VALUE);
        IPNReplayAlgorithm algorithm = new PetrinetReplayerWithILP();
        replayParameters.setFinalMarkings(fMarking);
        replayParameters.setCreateConn(false);
        replayParameters.setGUIMode(false);

        PNRepResult result = null;
        try {
            result = replayer.replayLog(null, net, xlog, mapping, algorithm, replayParameters);
        } catch (AStarException e) {
            // Auto-generated catch block
            e.printStackTrace();
        }
        return result;

    }

    //alignment computation (PNRepResult ist ein Set<SyncReplayResult>, SyncReplayResult ist eine synchronisierte trace)
    public PNRepResult computeAlignment(XLog xlog, XEventClassifier classifier, Petrinet net, Marking iMarking, Marking fMarking, PluginContext context) {
        XEventClasses eventClasses = XEventClasses.deriveEventClasses(classifier, xlog);
        XEventClass dummy = new XEventClass("", 1);
        TransEvClassMapping mapping = computeMappingTobiasStyle(dummy, net, eventClasses);
        Replayer replayer = new Replayer(net, iMarking, fMarking, eventClasses, mapping, false);
        PNRepResult result = null;
//			Progress progress = (Progress) context.getProgress(); //this does NOT work. it needs another progress

        try {
            result = replayer.computePNRepResult(new Progress() {

                public boolean isCanceled() {
                    // TODO Auto-generated method stub
                    return false;
                }

                public void setMaximum(int maximum) {
                    // TODO Auto-generated method stub

                }

                public void log(String message) {
                    // TODO Auto-generated method stub

                }

                public void inc() {
                    // TODO Auto-generated method stub

                }
            }, xlog);
        } catch (InterruptedException e) {
            //  Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            //  Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }


    // returns pairs of tracefitness and frequency (PNRepResult ist ein Set<SyncReplayResult>, SyncReplayResult ist eine synchronisierte trace)
    private HashMap<Double, Integer> computeTraceFitnessSummary(PNRepResult alignedTraces, double lengthOfShortestPathTroughModel) {
        HashMap<Double, Integer> result = new HashMap<Double, Integer>();
        for (SyncReplayResult alignedTrace : alignedTraces) {
            int costAlignedTrace = 0;
            int lengthAlignedTrace = 0;
            for (StepTypes move : alignedTrace.getStepTypes()) {
                if (move == StepTypes.MREAL || move == StepTypes.L) {// if it is a model move
                    costAlignedTrace++;
                }
                if (move == StepTypes.LMGOOD || move == StepTypes.L) {// if it is part of the trace
                    lengthAlignedTrace++;
                }
            }
            double traceFitness = (double) 1 - ((double) costAlignedTrace / ((double) lengthAlignedTrace + lengthOfShortestPathTroughModel));
            if (result.containsKey(traceFitness)) {
                result.put(traceFitness, (result.get(traceFitness) + 1)); //increment the frequency of this tracefitness value by one
            } else {
                result.put(traceFitness, 1);
            }
        }
        return result;
    }


    private double computeAverageTraceFitness(HashMap<Double, Integer> summaryTraceFitness) {
        double sum = 0;
        double amount = 0;
        for (Double key : summaryTraceFitness.keySet()) {
            int frequencyOfKey = summaryTraceFitness.get(key);
            sum = sum + (frequencyOfKey * key);
            amount = amount + frequencyOfKey;
        }
        return (sum / amount);
    }

    @Override
    public AcceptingPetriNetWrapper discover(UIPluginContext context, XLog log, Object parameter) throws InterruptedException {
        Parameters parameters;

        if (parameter == null) {
            parameters = (Parameters) startGUI(context, log);
        } else {
            parameters = (Parameters) parameter;
        }

        System.out.println("_____________ eST - Miner ___________________________________________________________________________________");
        System.out.println(System.getProperty("java.class.path"));
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        System.out.println(Arrays.toString(sysloader.getURLs()));

        String resultBasePath = "resultsCollection/"; //specify path for outputs (if not given by Cluster)
        String logName = log.getAttributes().get("concept:name").toString(); //specify logName (if not given by Cluster)
        Object[] objects = runDiscovery(log, logName, parameters, context, 0, resultBasePath);

        return new AcceptingPetriNetWrapper((Petrinet) objects[0], (Marking) objects[1], (Marking) objects[2], parameters);
    }

    @Override
    public Object startGUI(UIPluginContext context, XLog log) {
        return getGeneralParameters(log, context);  //set general parameters using user input
    }

    @Override
    public String toString() {
        return "eST-Miner";
    }
}