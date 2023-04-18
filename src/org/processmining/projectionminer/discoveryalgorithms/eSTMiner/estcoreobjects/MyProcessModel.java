package org.processmining.projectionminer.discoveryalgorithms.eSTMiner.estcoreobjects;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.ArrayList;
import java.util.HashMap;

public class MyProcessModel {
    private final String[] transitions;
    private ArrayList<MyPlace> places;
    private boolean[] transitionsLiveness;
    private boolean[] variantVector; //used to save which trace variants are fitting this process model
    private ArrayList<MyPlace> potentialPlaces;
    private ArrayList<MyPlace> discardedPlaces;

    public MyProcessModel(Petrinet petrinet, ArrayList<Transition> transitions, String[] transitionNames) {
        this.transitions = transitionNames;
        places = new ArrayList<>();

        HashMap<Transition, Integer> transitionToID = new HashMap<>();
        for (int i = 0; i < transitions.size(); i++) {
            transitionToID.put(transitions.get(i), i);
        }

        for (Place place : petrinet.getPlaces()) {
            int inputTransitionKey = 0;
            int outputTransitionKey = 0;

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : petrinet.getInEdges(place)) {
                inputTransitionKey = inputTransitionKey | getMask(transitionToID.get(inEdge.getSource()), this.transitions);
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : petrinet.getOutEdges(place)) {
                outputTransitionKey = outputTransitionKey | getMask(transitionToID.get(outEdge.getTarget()), this.transitions);
            }

            places.add(new MyPlace(inputTransitionKey, outputTransitionKey));
        }
    }

    public MyProcessModel(final ArrayList<MyPlace> places, final String[] transitions, int numVariants) {
        this.places = places;
        this.transitions = transitions;

        this.variantVector = new boolean[numVariants];
        for (int i = 0; i < numVariants; i++) {
            variantVector[i] = true; // in the beginning all traces are replayable
        }
        this.potentialPlaces = new ArrayList<MyPlace>();
        this.discardedPlaces = new ArrayList<MyPlace>();
        //initially all transitions are live
        this.transitionsLiveness = new boolean[transitions.length];
        for (int i = 0; i < transitions.length; i++) {
            this.transitionsLiveness[i] = true;
        }
    }


    //add the place to the PM and update the PM variant vector accordigly (intersection of PM and Place replayabke variants)
    public void addPlace(final MyPlace p) {
        places.add(p);
        boolean[] placeVariantVector = p.getVariantVector();
        for (int i = 0; i < placeVariantVector.length; i++) {
            if (!placeVariantVector[i]) {
                this.variantVector[i] = false;
            }
        }
    }

    //add the places to the PM and update the PM variant vector accordigly (intersection of PM and Places replayabke variants)
    public void addPlace(final ArrayList<MyPlace> placesToAdd) {
        for (MyPlace p : placesToAdd) {
            this.addPlace(p);
        }
    }


    public MyProcessModel clone() {//TODO something goes wrong while cloning (wrong variant vectors?)
        ArrayList<MyPlace> newPlaces = new ArrayList<MyPlace>(getPlaces());
        ArrayList<MyPlace> newPotentialPlaces = new ArrayList<MyPlace>(getPotentialPlaces());
        ArrayList<MyPlace> newDiscardedPlaces = new ArrayList<MyPlace>(getDiscardedPlaces());
        String[] newTransitions = new String[getTransitions().length];
        for (int i = 0; i < getTransitions().length; i++) {
            newTransitions[i] = getTransitions()[i];
        }
        boolean[] newVariantVector = new boolean[getVariantVector().length];
        for (int i = 0; i < this.getVariantVector().length; i++) {
            newVariantVector[i] = getVariantVector()[i];
        }
        MyProcessModel newPM = new MyProcessModel(newPlaces, newTransitions, this.getVariantVector().length);
        newPM.setVariantVector(newVariantVector);
        newPM.setDiscardedPlaces(newDiscardedPlaces);
        newPM.setPotentialPlaces(newPotentialPlaces);
        boolean[] newTransitionLiveness = new boolean[this.transitionsLiveness.length];
        System.arraycopy(transitionsLiveness, 0, newTransitionLiveness, 0, transitionsLiveness.length);
        newPM.setTransitionsLiveness(newTransitionLiveness);
        return newPM;
    }


    //recompute and set PM variant vector based on the PM places variant vectors
    private boolean[] recomputeVariantVector() {
        boolean[] newVariantVector = new boolean[this.getVariantVector().length];
        for (int i = 0; i < newVariantVector.length; i++) {
            newVariantVector[i] = true;
        }
        //compute new vector from places
        for (MyPlace place : this.getPlaces()) {
            for (int i = 0; i < newVariantVector.length; i++) {
                if (!place.getVariantVector()[i]) {
                    newVariantVector[i] = false; //set to false if any place cannot replay this
                }
            }
        }
        this.setVariantVector(newVariantVector);
        return newVariantVector;
    }


    // printing & debugging and stuff

    //updates PM status and then prints overview (comment out if not debugging)
    public void updateAndPrintStatus(MyLog log) {
        String debuggingString = "\n _________________________________________PM: Update Status_______________________________________________________________________";
        this.updateStatus(log);
        debuggingString = debuggingString + "\n" + "Currently Live Transitions: " + this.countLiveTransitions();
        debuggingString = debuggingString + "\n" + "Currently Replayable Variants: " + this.countLiveVariants() + " out of " + this.getVariantVector().length;
        int numliveTraces = log.countLiveTraces(variantVector);
        debuggingString = debuggingString + "\n" + "Currently Replayable Traces: " + numliveTraces + " out of " + log.getNumOfTraces() + " (" + 100 * numliveTraces / log.getNumOfTraces() + ")%";
        debuggingString = debuggingString + "\n" + "Current number of potential places: " + this.getPotentialPlaces().size();
        debuggingString = debuggingString + "\n" + "Current number of discarded places: " + this.getDiscardedPlaces().size();
//		System.out.println(debuggingString);
//		this.printPlaceSummary(log);
//		System.out.println("_________________________________________PM: Status upated_______________________________________________________________________ \n");

    }


    //updates PM Variant vector based on place variant vectors TODO: currently, does NOT allow for places to be re-added!
    //updates the places activated actitivities set
    //updates PM active transitions
    //compares to old notifies about issues
    public void updateStatus(MyLog log) {
        //variant vector
        boolean[] oldVariantVector = this.getVariantVector();
        boolean[] newVariantVector = this.recomputeVariantVector();
        for (int i = 0; i < oldVariantVector.length; i++) {
            if (!oldVariantVector[i] && newVariantVector[i]) {
                this.variantVector[i] = false;
//				System.out.println("NOTE: variant NOT re-added to PM variant vector! - "+ log.getTraceVariants().get(i)); //for debugging
            }
        }
        //dead transitions
        boolean[] oldTransitionsLiveness = this.getTransitionsLiveness();
        boolean[] newTransitionsLiveness = this.recomputeTransitionsLiveness(log); // TODO this uses the variant vector and thus should not cause errors!
        for (int i = 0; i < oldTransitionsLiveness.length; i++) {
            if (!oldTransitionsLiveness[i] && newTransitionsLiveness[i]) {
                System.out.println("ERROR: tranistion is trying to rise from the dead! - " + transitions[i]);
                newTransitionsLiveness[i] = false; //rising from dead currently not allowed TODO
            }
        }
        this.transitionsLiveness = newTransitionsLiveness;
        //place connections
        this.setActiveKeys(log);
        for (MyPlace place : this.getPlaces()) {
            if (Integer.bitCount(place.getActiveKey()) < 2) {
//				System.out.println("ERROR: place is lacking live connections! - "+place.toTransitionsString(transitions));
            }
        }
    }

    //recomputes the live transitions based on this PM variant vector
    private boolean[] recomputeTransitionsLiveness(MyLog log) {
        return log.getTransitionsLiveness(this.getVariantVector());
    }


    public void printPlaceSummary(MyLog log) {
        String result = "Current places in model (" + this.getPlaces().size() + "): \n depth \t #fitVarsP \t #activeTr \t transitions: ";
        for (MyPlace place : this.getPlaces()) {
            result = result + "\n" + place.getLevel() + "\t" + place.getLocalFitness() + "\t \t" + Integer.bitCount(place.getActiveKey()) + "\t \t" + place.toTransitionsString(transitions);
        }
        System.out.println(result);
    }

    public void printPotentialPlaceSummary() {
        String result = "Current potential places: \n level \t fitness \t transitions: ";
        for (MyPlace place : this.getPotentialPlaces()) {
            result = result + "\n" + place.getLevel() + "\t" + place.getLocalFitness() + "\t" + place.toTransitionsString(transitions);
        }
        System.out.println(result);
    }

    public void printDiscardedPlaceSummary() {
        String result = "Current discarded places (level, fitness, transitions): ";
        for (MyPlace place : this.getDiscardedPlaces()) {
            result = result + "\n" + place.getLevel() + "\t" + place.getLocalFitness() + "\t" + place.toTransitionsString(transitions);
        }
        System.out.println(result);
    }


    //for debugging. sets the PM places active keys based on the transitionLiveness
    private ArrayList<MyPlace> setActiveKeys(MyLog log) {
        boolean[] transitionsLiveness = this.getTransitionsLiveness();
        for (MyPlace place : places) {
            int placeActiveKey = 0;
            for (int i = 0; i < transitionsLiveness.length; i++) {
                if (((getMask(i, transitions) & place.getInputTrKey())) > 0
                        || ((getMask(i, transitions) & place.getOutputTrKey()) > 0)) {//if the activity is connected to the place
                    if (transitionsLiveness[i]) {//if the transition is live
                        placeActiveKey = (placeActiveKey | getMask(i, transitions));//add the activity to the ones activated by place
                    } else {
//						System.out.println("Active Key Analysis: Dead transition "+transitions[i]+ " connected to "+place.toTransitionsString(transitions));
                    }
                }
            }
            place.setActiveKey(placeActiveKey);
        }
        return places;
    }


    //returns number of stored live transitions
    private int countLiveTransitions() {
        int sum = 0;
        for (int i = 0; i < this.transitionsLiveness.length; i++) {
            if (transitionsLiveness[i]) {
                sum++;
            }
        }
        return sum;
    }

    //counts the 'true' entries in this variant vector
    public int countLiveVariants() {
        int result = 0;
        for (int i = 0; i < variantVector.length; i++) {
            if (variantVector[i]) {
                result++;
            }
        }
        return result;
    }

    //removes all dead transitions from the given key
    public int removeDeadTransitions(int transitionKey) {
        for (int i = 0; i < this.getTransitionsLiveness().length; i++) {
            if (!this.getTransitionsLiveness()[i]) {
                int mask = getMask(i, transitions);
                transitionKey = (transitionKey | mask) ^ mask;
            }
        }
        return transitionKey;
    }


    //merges places, that have the same set of ingoing and outgoing transitions, except for selfloops
    public MyProcessModel mergeSelfLoopPlaces(MyLog log) {
        this.updateStatus(log);
        ArrayList<MyPlace> result = new ArrayList<MyPlace>();
        ArrayList<MyPlace> placesToMerge = this.getPlaces();
        ArrayList<MyPlace> remainingPlaces = new ArrayList<MyPlace>();
        while (!placesToMerge.isEmpty()) {
            MyPlace place1 = placesToMerge.remove(0);
            while (!placesToMerge.isEmpty()) {
                MyPlace place2 = placesToMerge.remove(0);
                int place1NonLoopInMask = this.removeDeadTransitions(place1.getNonLoopsInMask()); //mask indictaing all non-self-loop in transitions
                int place1NonLoopOutMask = this.removeDeadTransitions(place1.getNonLoopsOutMask());
                int place2NonLoopInMask = this.removeDeadTransitions(place2.getNonLoopsInMask()); //mask indictaing all non-self-loop in transitions
                int place2NonLoopOutMask = this.removeDeadTransitions(place2.getNonLoopsOutMask());
                if ((place1NonLoopInMask == place2NonLoopInMask) && (place1NonLoopOutMask == place2NonLoopOutMask)) {//if the non-looping transitions are exactly the same
                    place1 = place1.mergePlaces(place2);//merge if possible
                } else {
                    remainingPlaces.add(place2);//if not mergeable, keep for next iteration
                }
            }
            placesToMerge = new ArrayList<MyPlace>(remainingPlaces);
            remainingPlaces.clear();
            result.add(place1); // add the (possibly merged) place1
        }
        this.setPlaces(result);
        return this;
    }


    //G&S
    public ArrayList<MyPlace> getPlaces() {
        return places;
    }

    public void setPlaces(final ArrayList<MyPlace> places) {
        this.places = places;
    }

    public String[] getTransitions() {
        return transitions;
    }

    public boolean[] getVariantVector() {
        return variantVector;
    }

    private void setVariantVector(boolean[] variantVector) {
        this.variantVector = variantVector;
    }

    public int getNumLiveTraces(MyLog log) {
        return log.countLiveTraces(this.variantVector);
    }

    public ArrayList<MyPlace> getPotentialPlaces() {
        return potentialPlaces;
    }

    public void setPotentialPlaces(ArrayList<MyPlace> potentialPlaces) {
        this.potentialPlaces = potentialPlaces;
    }

    public ArrayList<MyPlace> getDiscardedPlaces() {
        return discardedPlaces;
    }

    public void setDiscardedPlaces(ArrayList<MyPlace> discardedPlaces) {
        this.discardedPlaces = discardedPlaces;
    }

    public boolean[] getTransitionsLiveness() {
        return transitionsLiveness;
    }

    private void setTransitionsLiveness(boolean[] newTransitionLiveness) {
        this.transitionsLiveness = newTransitionLiveness;

    }

    public int getNumDeadTransitions() {
        return (this.getTransitions().length - this.countLiveTransitions());
    }

    protected int getMask(final int position, final String[] transitions) {
        return (1 << (transitions.length - 1 - position));
    }


}
