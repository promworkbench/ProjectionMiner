package org.processmining.projectionminer.utils;

public class TauNameGenerator {
    private static TauNameGenerator instance;
    private int count = 0;

    public static TauNameGenerator getInstance() {
        if (TauNameGenerator.instance == null)
            TauNameGenerator.instance = new TauNameGenerator();
        return instance;
    }

    public String getTauName() {
        count++;
        return "tau-" + count;
    }
}
