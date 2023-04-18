package org.processmining.projectionminer.discoveryalgorithms.eSTMiner.candidatetraverser;

import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyPlace;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects.MyPlaceStatus;
import org.processmining.projectionminer.discoveryalgorithms.eSTMiner.plugins.Parameters;

/*
 * This class public method getNext() returns the next candidate place based on
 * the given candidate place. It aims for a DFS search of the candidate tree.
 */

//TODO update place limit, update everyuthing, update statiscis collection

//ASSUME THAT POS 0 OF TRANSITIONS IS END, POS 0 OF OUTMAPPING MAPS TO START
public class DFSCandidateTraverser extends AbstractCandidateTraverser {


    public DFSCandidateTraverser(final String[] transitions, final int[] outTrMapping, Parameters parameters) {
        super(transitions, outTrMapping, parameters);
    }


    //returns the next place based on the given last place
    //uses a tree structure ordered according to transitions array (ingoing) and mapping (outgoing).
    //Makes use of the class variables roots and current root to keep track of tree traversal
    // Cuts off uninteresting branches based on fitness, other criteria planned
    //uniwired: cut of branches that would add a connection that already exists (TODO not implemented here, add special traverser for that)
    //Manage Traversal depth - for a given place, either
    // 1) return the next place (within traversal depth)
    // 2) return the next root (no valid children)
    // 3) add all valid children to root queue and return next root (end of travsersal depth)
    public MyPlace getNext(MyPlace lastP, MyPlaceStatus fitness) {
        //case first place
        if (lastP == null) {
            return currentRoot;
        }
/*			System.out.println("Last Place: " + lastP.toBinaryString() 
            + " = (" + getTransitionNames(lastP.getInputTrKey(), transitions).toString()
            + " | " + getTransitionNames(lastP.getOutputTrKey(), transitions).toString()
            + ")" + " = " + fitness.toString());
*/
        //fitness = PlaceStatus.UNKNOWN; // for testing full traversal TODO remove

        MyPlace nextP = new MyPlace(0, 0);
        //Case 1: next place is descendant of last place
        //UNIWIRED: if max depth is reached, store children for later
        nextP = findNextChild(lastP, fitness);
/*				System.out.println("Case 1: " + nextP.toBinaryString() 
                + " = (" + getTransitionNames(nextP.getInputTrKey(), transitions).toString()
                + " | " + getTransitionNames(nextP.getOutputTrKey(), transitions).toString()
                + ")");
*/
        //Case 2: next place contained in same subtree (based on currentRoot) but different branch
        if ((nextP.getInputTrKey() == 0) || (nextP.getOutputTrKey() == 0)) {//UNIWIRED: search depth unimportant: next branch place is of equal depth
            nextP = findNextBranch(lastP);
/*			System.out.println("Case 2: " + nextP.toBinaryString()
                + " = (" + getTransitionNames(nextP.getInputTrKey(), transitions).toString()
                + " | " + getTransitionNames(nextP.getOutputTrKey(), transitions).toString()
                + ")");
*/
        }
        //Case 3: next place contained in new subtree (new currentRoot needed)
        if ((nextP.getInputTrKey() == 0) || (nextP.getOutputTrKey() == 0)) {
            nextP = getNextRoot();
            if (nextP == null) {
                nextP = new MyPlace(0, 0);
            }
        }
/*			System.out.println("Case 3: " + nextP.toBinaryString()
                + " = (" + getTransitionNames(nextP.getInputTrKey(), transitions).toString()
                + " | " + getTransitionNames(nextP.getOutputTrKey(), transitions).toString()
                + ")");
*/
        //Case 4: no more places to be found
        if ((nextP.getInputTrKey() == 0) || (nextP.getOutputTrKey() == 0)) {
/*			System.out.println("Case 4: " + nextP.toBinaryString()
                + " = (" + getTransitionNames(nextP.getInputTrKey(), transitions).toString()
                + " | " + getTransitionNames(nextP.getOutputTrKey(), transitions).toString()
                + ")");
*/
            return null;
        }
        //return new place
        return nextP;
    }

    //try: find child of the last candidate node (deepen current branch), outchild first (NO backtracking)
    // possibly cut off uninteresting branches
    //possibly stop due to depth limit
    private MyPlace findNextChild(final MyPlace lastP, final MyPlaceStatus fitness) {
        int nextInKey = 0;
        int nextOutKey = 0;
        nextOutKey = findNextOutKey(lastP.getInputTrKey(), lastP.getOutputTrKey(), fitness); //adds a new out-transitions to current candidate, returns 0 if impossible
        if (nextOutKey != 0) {
            nextInKey = lastP.getInputTrKey();
        } else {
            nextInKey = findNextInKey(lastP.getInputTrKey(), lastP.getOutputTrKey(), fitness); //add new in-transition to current candidate (only possible if outkey has exactly one transition), returns 0 if impossible
            if (nextInKey != 0) {
                nextOutKey = lastP.getOutputTrKey(); //should contain exactly one transition, otherwise inkey should be 0
            }
        }
        return new MyPlace(nextInKey, nextOutKey);
    }


    //adds a new out transition to current candidate (NO BACKTRACKING), possibly cuts of current branch, returns 0 on fail
    //current implementation: increase inkey from left to right, outkey from right to left
    private int findNextOutKey(final int lastInKey, final int lastOutKey, final MyPlaceStatus fitness) {
        int nextOutKey = 0;
        //if path is not at end AND last place was underfed --> cut this path
        if (((lastOutKey & getMask(largestOutIndex)) == 0) //last out transition is not added
                && ((fitness == MyPlaceStatus.UNDERFED) || (fitness == MyPlaceStatus.MALFED))) {//previous place was underfed
            //System.out.println("Cutting off underfed path!");
        }
        //if path is not cut, add output transition if possible
        // add only transitions larger than the largest current out transition
        else if ((lastOutKey & getMask(largestOutIndex)) == 0) {//ensure largest transition is not set
            int currentOutIndex = getLargestOutTrIndex(lastOutKey);//find largest outIndex
            if (largestOutIndex != getMappedTransitionIndex(currentOutIndex)) {//check whether there are still transitions to find
                int newOutTransition = getMappedTransitionIndex(currentOutIndex + 1);//the index of the new, unwired transition
                nextOutKey = (lastOutKey | getMask(newOutTransition));
            }
        }
        //cut of outpath if depth limit is reached:
        if (this.limitDepth && (Integer.bitCount(nextOutKey) > this.depthLimit)) {
//			System.out.println("Limit Depth of outpath at "+this.placeToString(new MyPlace(0, nextOutKey)));
            return 0;
        } else {
            return nextOutKey;
        }
    }


    //adds a new in transition to current candidate (NO BACKTRACKING), possibly cuts of current branch, returns 0 on fail
    private int findNextInKey(final int lastInKey, final int lastOutKey, final MyPlaceStatus fitness) {
        int nextInKey = 0;
        // in transitions can only be added to places with exactly one output transition
        if (Integer.bitCount(lastOutKey) == 1) {
            //path is not at end AND no outkeys can be added, so overfedness of previous place can be exploited to cut branch
            if (((lastInKey & getMask(largestInIndex)) == 0)
                    && (lastOutKey == getMask(largestOutIndex))
                    && ((fitness == MyPlaceStatus.OVERFED) || (fitness == MyPlaceStatus.MALFED))) {
                //	System.out.println("Cutting off overfed path!");
            }
            // case input can be added (add only transitions larger than the largest in transition)
            // only add transitions that are not yet wired!
            else if ((lastInKey & getMask(largestInIndex)) == 0) {//ensure largest transition isn't set
                int newInTransition = Integer.lowestOneBit(lastInKey);//finds the highest (rightmost) intransition
                //while the current transition can be increased search for feasable new transition
                if (newInTransition != getMask(largestInIndex)) {
                    newInTransition = newInTransition >> 1;
                    nextInKey = lastInKey | newInTransition;
                }
            }
        }
        //cut of inpath if depth limit is reached:
        if (this.limitDepth && (Integer.bitCount(nextInKey) > this.depthLimit)) {
//			System.out.println("Limit Depth of inpath at "+this.placeToString(new MyPlace(nextInKey, 0)));
            return 0;
        } else {
            return nextInKey;
        }
    }


    //assume the current candidate cannot have children
    //try: backtrack at most up to current root, to find a new branch of this subtree
    private MyPlace findNextBranch(final MyPlace lastP) {
        int lastOutKey = lastP.getOutputTrKey();
        int lastInKey = lastP.getInputTrKey();
        int nextInKey = 0;
        int nextOutKey = 0;
        nextOutKey = backtrackOutKey(lastInKey, lastOutKey);
        if (nextOutKey != 0) {//new outbranch could be found
            nextInKey = lastInKey;
        } else {//no outbranch could be found
            int rootOutTree = getMask(getMappedTransitionIndex(getLowestOutTrIndex(lastOutKey))); //ensure only the smallest out-transition is left ("root" of this outkey)
            if (lastOutKey != rootOutTree) {//check whether the root of this outtree admits a new inbranch before backtracking (if lastOutKey was root, this has already been checked)
                nextInKey = findNextInKey(lastInKey, rootOutTree, MyPlaceStatus.UNKNOWN);
            }
            if (nextInKey == 0) {//this outkey "root" doesn't admit any children
                nextInKey = backtrackInKey(lastInKey, getOutTreeRoot(lastOutKey));
            }
            if (nextInKey != 0) {//new inbranch could be found
                nextOutKey = rootOutTree;
            }
        }
        MyPlace nextP = new MyPlace(nextInKey, nextOutKey);
        if (isInCurrentSubtree(nextP)) {
            return nextP;
        } else {//no more places in current subtree
            return new MyPlace(0, 0);
        }
    }

    //returns true if the given place is a descendant of the currenRoot
    private boolean isInCurrentSubtree(MyPlace nextPlace) {
        return isSubset(currentRoot.getInputTrKey(), nextPlace.getInputTrKey())
                && isSubset(currentRoot.getOutputTrKey(), nextPlace.getOutputTrKey());
    }

    //returns true if the set of transitions encoded by subset are all contained in the set of transitions encoded by superset
    private boolean isSubset(int subset, int superset) {
        //subset is completely contained in superset (possibly equal)
        return (subset & superset) == subset;
    }

    //based on the last outkey, backtracks the outbranch until a new outbranch is found, returns 0 on fail
    //assume no children can be added to current candidate
    private int backtrackOutKey(int lastInKey, int lastOutKey) {
        int nextOutKey = 0;
        if (Integer.bitCount(lastOutKey) > 1) {//if there is only one out transition, backtracking is impossible
            int lastOutIndex = getLargestOutTrIndex(lastOutKey);//find the largest remaining out transition of the last candidate
            lastOutKey = getMask(getMappedTransitionIndex(lastOutIndex)) ^ lastOutKey;//remove this largest transition to get back to candidates' parent
            // Case 1 (getTransitionIndex(lastOutIndex) != largestOutIndex): the last branch was cut, this parent has another outbranch (last outbranch is always exactly one candidate and cannot be cut)
            if (getMappedTransitionIndex(lastOutIndex) != largestOutIndex) {
                return (lastOutKey | getMask(getMappedTransitionIndex(lastOutIndex + 1))); //return next suitable outbranch of the parent
            }
            //case 2: the last branch was at it's end, so this parent cannot have other out branches
            //--> try to find the grandparent (parent of this parent) and find a new out branch of this grandparent
            if ((nextOutKey == 0) && (Integer.bitCount(lastOutKey) > 1)) { //if this parent has only one out transition, further backtracking is impossible
                lastOutIndex = getLargestOutTrIndex(lastOutKey);//find the largest remaining out transition of the last candidate
                lastOutKey = getMask(getMappedTransitionIndex(lastOutIndex)) ^ lastOutKey;//remove this transition to get back to candidates' grandparent

                if (getMappedTransitionIndex(lastOutIndex) != largestOutIndex) {
                    return (lastOutKey | getMask(getMappedTransitionIndex(lastOutIndex + 1))); //return next suitable outbranch of the parent
                }
            }
        }
        return nextOutKey;
    }


    //based on the last inkey, backtracks the inbranch until new inbranch is found, returns 0 on fail
    //assume no children can be added to current candidate
    private int backtrackInKey(int lastInKey, int lastOutKey) {
        if (Integer.bitCount(lastOutKey) > 1) {
            System.out.println("Error: backtrackInKey expects outkey of size 1!");
        }
        int nextInKey = 0;
        if (Integer.bitCount(lastInKey) > 1) {//if there is only one intransition backtracking is impossible
            int lastTransition = Integer.lowestOneBit(lastInKey); //find the largest remaining in transition of the last candidate
            lastInKey = lastTransition ^ lastInKey;//remove this largest transition to get back to candidates' parent
            //Case 1: last inbranch was cut, find next inbranch of this parent (there is at least one)
            if (lastTransition != 1) {
                lastTransition = lastTransition >> 1;
                return (lastInKey | lastTransition); //add transition
            }
            //Case 2: this inbranch is at it's end, parent cannot have other inbranch
            //--> find parent of this parent (grandparent of this candidate) and its next inbranch
            if ((nextInKey == 0) && (Integer.bitCount(lastInKey) > 1)) { //if parent has only one tranistion further backtracking is impossible
                lastTransition = Integer.lowestOneBit(lastInKey); //find the largest remaining in transition of this parent
                lastInKey = lastTransition ^ lastInKey;//remove this largest transition to get back to candidates' grandparent
                if (lastTransition != 1) {
                    lastTransition = lastTransition >> 1;
                    return (lastInKey | lastTransition); //add transition
                }
            }
        }
        return nextInKey;
    }


    private int getOutTreeRoot(int lastOutKey) {
        return getMask(getMappedTransitionIndex(getLowestOutTrIndex(lastOutKey))); //ensure only the smallest out-transition is left ("root" of this outkey)
    }


}