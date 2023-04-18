package org.processmining.projectionminer.discoveryalgorithms.eSTMiner.candidatetraverser;

import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyPlace;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyPlaceStatus;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.plugins.Parameters;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.plugins.PlugInStatistics;

import java.util.ArrayList;
import java.util.Collection;


/*
 * This class public method getNext() returns the next candidate place based on
 * the given candidate place.
 * It aims for a BFS search of the candidate tree by maintaining a queue of subtree roots: the first root is evaluated, and its children (next level)
 *  are either cut-off (ignored) or appended at the end of the queue. This results in the current level being evaluated first, before the next level is reached.
 */


//ASSUME THAT POS 0 OF TRANSITIONS IS END, POS 0 OF OUTMAPPING MAPS TO START
public class BFSCandidateTraverser extends AbstractCandidateTraverser {

    public BFSCandidateTraverser(final String[] transitions, final int[] outTrMapping, Parameters parameters) {
        super(transitions, outTrMapping, parameters);
    }


    //returns the next place based on the given last place and additional information
    //uses a tree structure ordered according to transitions array (ingoing) and mapping (outgoing)
    //Makes use of the class variables roots and current root to keep track of tree traversal
    //Cuts off uninteresting branches based on fitness, other criteria planned
    //For a given place, add valid (not cut-off) children to roots queue, then return the next place (roots queue ordering ensures level by level traversal)
    //Return null when queue is empty
    public MyPlace getNext(MyPlace lastP, MyPlaceStatus fitness) {
        long startTime = System.currentTimeMillis();
        //case first place
        if (lastP == null) {
            PlugInStatistics.instance().incTimeCandFind(System.currentTimeMillis() - startTime);
            return currentRoot;
        }
//			fitness = MyPlaceStatus.UNKNOWN; //for debugging
        ArrayList<MyPlace> newChildren = addValidChildrenToQueue(lastP, fitness);
        MyPlace nextP = null;

/*			log("---------------");
            System.out.println("Last Place: "+lastP.toTransitions(transitions));
            rootsPlacesNames = rootsPlacesNames + "\n" + placeSetToString(newChildren);
            System.out.println("Roots: "+ (roots.size()-newChildren.size()) +"+"+ newChildren.size());
            System.out.println(placeSetToString(roots)+"\n"+placeSetToString(newChildren));
*/
        nextP = getNextRoot();
//			System.out.println("Next Place: "+nextP.toTransitions(transitions));		
        PlugInStatistics.instance().incTimeCandFind(System.currentTimeMillis() - startTime);
        return nextP;
    }


    private ArrayList<MyPlace> addValidChildrenToQueue(MyPlace place, MyPlaceStatus fitness) {
        ArrayList<MyPlace> newChildren = new ArrayList<MyPlace>();
        //do not add children if depth limit is reached
        if (this.limitDepth && getCurrentDepth(place) == this.depthLimit) {
//			System.out.println("Depth-limit reached.");			
        }
        //add valid children based on fitness (first outgoing, then ingoing)
        else {
            if (fitness != MyPlaceStatus.UNDERFED && fitness != MyPlaceStatus.MALFED) {
                newChildren.addAll(getValidOutChildren(place));
            } else {//for statistics only
                PlugInStatistics.instance().incNumCutPaths(getValidOutChildren(place).size());
            }
            if ((fitness != MyPlaceStatus.MALFED && fitness != MyPlaceStatus.OVERFED) || !hasSingleMaximalOutTransition(place)) { //asymetric pruning of overfed places
                newChildren.addAll(getValidInChildren(place));
            } else {//for statistics only
                PlugInStatistics.instance().incNumCutPaths(getValidInChildren(place).size());
            }
        }
        this.roots.addAll(newChildren);
        return newChildren;
    }


    //assume fitness and tree level have been tested, other criteria still need to be checked
    private Collection<? extends MyPlace> getValidInChildren(MyPlace place) {
        ArrayList<MyPlace> inChildren = new ArrayList<MyPlace>();
        if (Integer.bitCount(place.getOutputTrKey()) > 1) {
            return inChildren; //this place has no in transition children (more than one out transition)
        }
        int nextOutKey = place.getOutputTrKey();
        int lastInKey = place.getInputTrKey();
        int largestInIndex = getLargestInTrIndex(lastInKey);
        for (int i = largestInIndex + 1; i < transitions.length; i++) {
            int nextInKey = (lastInKey | getMask(i));
            inChildren.add(new MyPlace(nextInKey, nextOutKey));
        }
        return inChildren;
    }

    //assume fitness and tree level have been tested, other criteria still need to be checked
    private Collection<? extends MyPlace> getValidOutChildren(MyPlace place) {
        ArrayList<MyPlace> outChildren = new ArrayList<MyPlace>();
        int nextInKey = place.getInputTrKey();
        int lastOutKey = place.getOutputTrKey();
        int largestOutMappingIndex = getLargestOutTrIndex(lastOutKey);
        for (int i = largestOutMappingIndex + 1; i < outTrMapping.length; i++) {
            int nextOutKey = (lastOutKey | getMask(getMappedTransitionIndex(i)));
            outChildren.add(new MyPlace(nextInKey, nextOutKey));
        }
        return outChildren;
    }

}