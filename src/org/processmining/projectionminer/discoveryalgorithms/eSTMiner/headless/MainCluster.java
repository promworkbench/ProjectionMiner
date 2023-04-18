package org.processmining.projectionminer.discoveryalgorithms.eSTMiner.headless;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.plugins.ESTMinerPlugin;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.plugins.Parameters;
import org.xeslite.parser.XesLiteXmlParser;

import java.io.*;
import java.util.List;

public class MainCluster {

    public static void main(String[] args) {
        System.out.println(System.getProperty("java.library.path"));

        // ???
        ArgumentParser parser = ArgumentParsers.newFor("Delta Discovery").build().description("Configure eST Delta Discovery precomputation parameters.");

        //experimental  settings
        parser.addArgument("--logfile").type(String.class).help("Path to log xes file");
        parser.addArgument("--logname").type(String.class).help("Name of the log");
        parser.addArgument("--outfilesPath").type(String.class).setDefault("./results/").help("Path to out files");
        parser.addArgument("--runID").type(Integer.class);

        //standard plugin parameters (only those interesting for delta discovery testing)
        parser.addArgument("--threshold_tau").type(Double.class);
        parser.addArgument("--max_tree_depth").type(Integer.class);
        parser.addArgument("--threshold_delta").type(Double.class);
        parser.addArgument("--delta_adaption_strategy").type(String.class).help("Sigmoid, Linear, NoDelta, or MaxDelta");
        parser.addArgument("--delta_adaption_steepness").type(Integer.class);
        parser.addArgument("--concurrent_IP_removal").action(Arguments.storeTrue());
        parser.addArgument("--max_potential_places").type(Integer.class);
        parser.addArgument("--virtual_tree_level_modifier").type(Integer.class);

        //standard plugin parameters (the rest)
        //parser.addArgument("--classifier").type(XEventClassifier.class).setDefault("XLogInfoImpl.STANDARD_CLASSIFIER");  //check whether this is robust
        parser.addArgument("--IP_removal").action(Arguments.storeTrue());
        parser.addArgument("--tree_traversal_strategy").type(String.class).setDefault("BFS").help("BFS (Uniwired, Delta) or DFS (Classic).");
        parser.addArgument("--discovery_variant").type(String.class).setDefault("Delta").help("Classic, Uniwired or Delta.");
        parser.addArgument("--IP_removal_strategy").type(String.class).setDefault("Replay").help("Replay or ILP.");
        parser.addArgument("--repair_while_removing_IPs").action(Arguments.storeTrue());
        parser.addArgument("--limit_tree_depth").action(Arguments.storeTrue());


        //get parameters from/for cluster
        Namespace argsParsed = parser.parseArgsOrFail(args);

        // Read the log
        XLog log = readLog((String) argsParsed.get("logfile"));
        String logName = (String) argsParsed.get("logname");
        if (log == null) {
            System.out.println("Could not read log. Terminating program.");
            return;
        }
        log.getClassifiers().add(0, XLogInfoImpl.STANDARD_CLASSIFIER);


        int run_id = (int) argsParsed.get("runID"); // for identifying runs on cluster

        // Create parameters
        Parameters parameters = new Parameters(
                (String) argsParsed.get("delta_adaption_strategy"),
                (int) argsParsed.get("delta_adaption_steepness"),
                (int) argsParsed.get("virtual_tree_level_modifier"),
                (int) argsParsed.get("max_potential_places"),
                (boolean) argsParsed.get("limit_tree_depth"),
                (int) argsParsed.get("max_tree_depth"),
                (String) argsParsed.get("discovery_variant"),
                (String) argsParsed.get("tree_traversal_strategy"),
                (double) argsParsed.get("threshold_tau"),
                (double) argsParsed.get("threshold_delta"),
                XLogInfoImpl.STANDARD_CLASSIFIER, //TODO Choices
                (boolean) argsParsed.get("IP_removal"),
                (boolean) argsParsed.get("concurrent_IP_removal"),
                (boolean) argsParsed.get("repair_while_removing_IPs"),
                (String) argsParsed.get("IP_removal_strategy")
        );

        ESTMinerPlugin myPluginHandle = new ESTMinerPlugin();

        myPluginHandle.runDiscovery(log, logName, parameters, new FakeContext(), run_id, (String) argsParsed.get("outfilesPath"));

    }


    public static XLog readLog(String pathLog) {
        File initialFile = new File(pathLog);
        InputStream inputStream = null;
        XLog log = null;
        XesLiteXmlParser parserLog = new XesLiteXmlParser(true);
        try {
            inputStream = new FileInputStream(initialFile);
            List<XLog> parsedLogs = null;
            try {
                parsedLogs = parserLog.parse(inputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (parsedLogs.size() > 0) {
                log = parsedLogs.get(0);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return log;
    }


}
