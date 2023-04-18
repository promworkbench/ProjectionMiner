package org.processmining.projectionminer.utils;

import java.util.concurrent.TimeUnit;

public class PerformancePlugin {
    private static PerformancePlugin INSTANCE;

    private long initialDiscoveryTime;
    private long discoveryAlgTime;
    private long projectionTime;

    private long initialStart;
    private long discoveryStart;
    private long projectionStart;

    private PerformancePlugin() {
        reset();
    }

    public static PerformancePlugin getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PerformancePlugin();
        }
        return INSTANCE;
    }

    public void reset() {
        initialDiscoveryTime = discoveryAlgTime = projectionTime = 0;

        initialStart = -1;
        discoveryStart = -1;
        projectionStart = -1;
    }

    @Override
    public String toString() {
        String result = "\nPerformance Data of the ProjectionMiner run:\n";
        result = result + "---------------------------------------------\n";
        result = result + "Time spent on ...\n";
        result = result + ("Initial discovery:    " + initialDiscoveryTime + "\n");
        result = result + ("                      " + TimeUnit.MILLISECONDS.convert(initialDiscoveryTime, TimeUnit.NANOSECONDS) + " milliseconds\n");
        result = result + ("Discovery runs:       " + discoveryAlgTime + "\n");
        result = result + ("                      " + TimeUnit.MILLISECONDS.convert(discoveryAlgTime, TimeUnit.NANOSECONDS) + " milliseconds\n");
        result = result + ("Preparing/Projecting: " + projectionTime + "\n");
        result = result + ("                      " + TimeUnit.MILLISECONDS.convert(projectionTime, TimeUnit.NANOSECONDS) + " milliseconds\n");

        return result;
    }

    public void finishInitial() {
        if (initialStart == -1) {
            System.out.println("Wrong configuration of end time Initial");
        }

        initialDiscoveryTime = initialDiscoveryTime + (System.nanoTime() - initialStart);
        initialStart = -1;
    }

    public void finishDiscovery() {
        if (discoveryStart == -1) {
            System.out.println("Wrong configuration of end time Discovery");
        }

        discoveryAlgTime = discoveryAlgTime + (System.nanoTime() - discoveryStart);
        discoveryStart = -1;
    }

    public void finishProjection() {
        if (projectionStart == -1) {
            System.out.println("Wrong configuration of end time Projection");
        }

        projectionTime = projectionTime + (System.nanoTime() - projectionStart);
        projectionStart = -1;
    }

    public void startInitial() {
        if (initialStart != -1) {
            System.out.println("Wrong configuration of start time Initial.");
        }

        initialStart = System.nanoTime();
    }

    public void startDiscovery() {
        if (discoveryStart != -1) {
            System.out.println("Wrong configuration of start time Discovery.");
        }

        discoveryStart = System.nanoTime();
    }

    public void startProjection() {
        if (initialStart != -1) {
            System.out.println("Wrong configuration of start time Projection.");
        }

        projectionStart = System.nanoTime();
    }
}
