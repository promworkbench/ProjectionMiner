package org.processmining.projectionminer.discoveryalgorithms.eSTMiner.placecombinators;

import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyPlace;

import java.util.Comparator;

//places with few transitions are ordered first
public class PlaceComparatorFittingTraces implements Comparator<MyPlace> {

    //sorts places based on number of transition
    public int compare(MyPlace p1, MyPlace p2) {
        boolean[] variantvector1 = p1.getVariantVector();
        boolean[] variantvector2 = p2.getVariantVector();
        int fitting1 = 0;
        int fitting2 = 0;
        for (int i = 0; i < variantvector1.length; i++) {
            if (variantvector1[i]) {
                fitting1++;
            }
            if (variantvector2[i]) {
                fitting2++;
            }
        }
        if (fitting1 < fitting2) {
            return -1;
        }
        if (fitting1 > fitting2) {
            return 1;
        }
        return 0;
    }


}
