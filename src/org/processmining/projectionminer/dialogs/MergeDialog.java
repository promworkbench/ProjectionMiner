package org.processmining.projectionminer.dialogs;

import com.fluxicon.slickerbox.factory.SlickerFactory;
import info.clearthought.layout.TableLayout;
import org.deckfour.uitopia.api.event.TaskListener;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.util.collection.AlphanumComparator;
import org.processmining.framework.util.ui.widgets.ProMList;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class MergeDialog extends JPanel {
    // All items in the user interface, that can be handled or read.
    public static final String TITLE = "Choose places to be merged:";
    // ProM concerning ID
    private static final long serialVersionUID = 410560016753951971L;
    private final String EMPTYMARKING = "<Empty List>";
    private final String type = "List";
    private ProMList placeList;
    private DefaultListModel placeListMdl;
    private ProMList mergePlaces;
    private DefaultListModel mergePlacesMdl;
    private JButton addPlacesBtn;
    private JButton removePlacesBtn;

    public ArrayList<Place> apply(UIPluginContext context, Petrinet net) {
        this.init(net);
        TaskListener.InteractionResult result = context.showWizard("Select mapping", true, true, this);
        if (result != TaskListener.InteractionResult.FINISHED) {
            return null;
        } else {
            ArrayList<Place> placeToBeMerged = new ArrayList<>();
            if (this.mergePlacesMdl.size() > 1) {
                Enumeration elements = this.mergePlacesMdl.elements();

                while (elements.hasMoreElements()) {
                    placeToBeMerged.add((Place) elements.nextElement());
                }
            } else if (!this.EMPTYMARKING.equals(this.mergePlacesMdl.elementAt(0))) {
                placeToBeMerged.add((Place) this.mergePlacesMdl.elements().nextElement());
            }

            return placeToBeMerged;
        }
    }

    private void init(PetrinetGraph net) {
        SlickerFactory factory = SlickerFactory.instance();
        this.placeListMdl = new DefaultListModel();
        Set<Place> places = new TreeSet(new Comparator<Place>() {
            private final AlphanumComparator comp = new AlphanumComparator();

            public int compare(Place o1, Place o2) {
                return this.comp.compare(o1.getLabel(), o2.getLabel());
            }
        });
        places.addAll(net.getPlaces());
        Iterator var4 = places.iterator();

        while (var4.hasNext()) {
            Place p = (Place) var4.next();
            this.placeListMdl.addElement(p);
        }

        this.placeList = new ProMList("List of Places", this.placeListMdl);
        this.placeList.setSelectionMode(2);
        this.mergePlacesMdl = new DefaultListModel();
        this.mergePlacesMdl.addElement(this.EMPTYMARKING);
        this.mergePlaces = new ProMList("Merge Places " + this.type, this.mergePlacesMdl);
        this.addPlacesBtn = factory.createButton("Add Place >>");
        this.addPlacesBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!MergeDialog.this.placeList.getSelectedValuesList().isEmpty()) {
                    if (MergeDialog.this.mergePlacesMdl.size() == 1 && MergeDialog.this.mergePlacesMdl.elementAt(0).equals(MergeDialog.this.EMPTYMARKING)) {
                        MergeDialog.this.mergePlacesMdl.removeAllElements();
                    }

                    for (Object o : MergeDialog.this.placeList.getSelectedValuesList()) {
                        if (!MergeDialog.this.mergePlacesMdl.contains(o)) {
                            MergeDialog.this.mergePlacesMdl.addElement(o);
                        }
                    }
                }

            }
        });
        this.removePlacesBtn = factory.createButton("<< Remove Place");
        this.removePlacesBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Iterator var2 = MergeDialog.this.mergePlaces.getSelectedValuesList().iterator();

                while (var2.hasNext()) {
                    Object obj = var2.next();
                    MergeDialog.this.mergePlacesMdl.removeElement(obj);
                    if (MergeDialog.this.mergePlacesMdl.size() == 0) {
                        MergeDialog.this.mergePlacesMdl.addElement(MergeDialog.this.EMPTYMARKING);
                    }
                }

            }
        });
        double[][] size = new double[][]{{250.0D, 10.0D, 200.0D, 10.0D, 250.0D}, {-1.0D, 30.0D, 5.0D, 30.0D, -1.0D}};
        TableLayout layout = new TableLayout(size);
        this.setLayout(layout);
        this.add(this.placeList, "0,0,0,4");
        this.add(this.addPlacesBtn, "2,1");
        this.add(this.removePlacesBtn, "2,3");
        this.add(this.mergePlaces, "4,0,4,4");
    }
}
