// License: GPL. Copyright 2012 by Josh Doe and others
package org.openstreetmap.josm.command;

import java.awt.geom.Area;
import java.util.Collection;
import java.util.HashSet;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.data.osm.visitor.MergeSourceBuildingVisitor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A command that merges objects from one layer to another.
 *
 * @author joshdoe
 */
public class MergeCommand extends Command {

    private DataSetMerger merger;
    private DataSet sourceDataSet;
    private DataSet targetDataSet;
    private OsmDataLayer targetLayer;
    private Collection<DataSource> addedDataSources;
    private String otherVersion;
    private Collection<Conflict> addedConflicts;

    /**
     * Create command to merge all or only currently selected objects from
     * sourceDataSet to targetLayer.
     *
     * @param targetLayer
     * @param sourceDataSet
     * @param onlySelected true to only merge objects selected in the
     * sourceDataSet
     */
    public MergeCommand(OsmDataLayer targetLayer, DataSet sourceDataSet, boolean onlySelected) {
        this(targetLayer, sourceDataSet, onlySelected ? sourceDataSet.getSelected() : null);
    }

    /**
     * Create command to merge the selection from the sourceDataSet to the
     * targetLayer.
     *
     * @param targetLayer
     * @param sourceDataSet
     * @param selection
     */
    public MergeCommand(OsmDataLayer targetLayer, DataSet sourceDataSet, Collection<OsmPrimitive> selection) {
        super(targetLayer);
        CheckParameterUtil.ensureParameterNotNull(targetLayer, "targetLayer");
        CheckParameterUtil.ensureParameterNotNull(sourceDataSet, "sourceDataSet");
        this.targetLayer = targetLayer;
        this.targetDataSet = targetLayer.data;

        // if selection present, create new dataset with just selected objects
        // and their "hull" (otherwise use entire dataset)
        if (selection != null && !selection.isEmpty()) {
            Collection<OsmPrimitive> origSelection = sourceDataSet.getSelected();
            sourceDataSet.setSelected(selection);
            MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(sourceDataSet);
            this.sourceDataSet = builder.build();
            sourceDataSet.setSelected(origSelection);
        } else {
            this.sourceDataSet = sourceDataSet;
        }
        

        addedConflicts = new HashSet<Conflict>();
        addedDataSources = new HashSet<DataSource>();
    }

    @Override
    public boolean executeCommand() {
        PleaseWaitProgressMonitor monitor = new PleaseWaitProgressMonitor(tr("Merging data"));
        monitor.setCancelable(false);
        if (merger == null) {
            //first time command is executed
            merger = new DataSetMerger(targetDataSet, sourceDataSet);
            try {
                merger.merge(monitor);
            } catch (DataIntegrityProblemException e) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        e.getHtmlMessage() != null ? e.getHtmlMessage() : e.getMessage(),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE);
                return false;

            }

            Area a = targetDataSet.getDataSourceArea();

            // copy the merged layer's data source info;
            // only add source rectangles if they are not contained in the
            // layer already.
            for (DataSource src : sourceDataSet.dataSources) {
                if (a == null || !a.contains(src.bounds.asRect())) {
                    targetDataSet.dataSources.add(src);
                    addedDataSources.add(src);
                }
            }

            otherVersion = targetDataSet.getVersion();
            // copy the merged layer's API version, downgrade if required
            if (targetDataSet.getVersion() == null) {
                targetDataSet.setVersion(sourceDataSet.getVersion());
            } else if ("0.5".equals(targetDataSet.getVersion()) ^ "0.5".equals(sourceDataSet.getVersion())) {
                System.err.println(tr("Warning: mixing 0.6 and 0.5 data results in version 0.5"));
                targetDataSet.setVersion("0.5");
            }


            // FIXME: allow conflicts to be retrieved rather than added to layer?
            if (targetLayer != null) {
                for (Conflict<?> c : merger.getConflicts()) {
                    if (!targetLayer.getConflicts().hasConflict(c)) {
                        targetLayer.getConflicts().add(c);
                        addedConflicts.add(c);
                    }
                }
            }
        } else {
            // command is being "redone"
            
            merger.remerge();
            targetDataSet.dataSources.addAll(addedDataSources);

            String version = otherVersion;
            otherVersion = targetDataSet.getVersion();
            targetDataSet.setVersion(version);

            for (Conflict c : addedConflicts) {
                targetLayer.getConflicts().add(c);
            }
        }
        
        if (addedConflicts.size() > 0) {
            targetLayer.warnNumNewConflicts(addedConflicts.size());
        }
        
        // repaint to make sure new data is displayed properly.
        Main.map.mapView.repaint();
        
        monitor.close();
        
        return true;
    }

    @Override
    public void undoCommand() {
        merger.unmerge();

        // restore data source area
        targetDataSet.dataSources.removeAll(addedDataSources);

        String version = otherVersion;
        otherVersion = targetDataSet.getVersion();
        targetDataSet.setVersion(version);

        for (Conflict c : addedConflicts) {
            targetLayer.getConflicts().remove(c);
        }

        Main.map.mapView.repaint();
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getDescriptionText() {
        return tr(marktr("Merged objects: {0} added, {1} modified"),
                merger.getAddedObjects().size(),
                merger.getChangedObjectsMap().size());
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("dialogs", "mergedown");
    }
    
    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        HashSet<OsmPrimitive> prims = new HashSet<OsmPrimitive>();
        prims.addAll(merger.getAddedObjects());
        prims.addAll(merger.getChangedObjectsMap().keySet());
        return prims;
    }
}