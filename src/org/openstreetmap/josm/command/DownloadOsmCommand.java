// License: GPL. Copyright 2012 by Josh Doe and others
package org.openstreetmap.josm.command;

import java.util.ArrayList;
import java.util.Collection;
import javax.swing.Icon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A command that merges a downloaded dataset to a layer.
 * 
 * @author joshdoe
 */
public class DownloadOsmCommand extends Command {
    private OsmDataLayer targetLayer;
    private DataSet sourceDataSet;
    private MergeCommand mergeCommand;
    private boolean requiresSaveToFile;
    private boolean requiresUploadToServer;
    private String commandSummary;

    public DownloadOsmCommand(String commandSummary, OsmDataLayer targetLayer, DataSet sourceDataSet) {
        super(targetLayer);
        this.commandSummary = commandSummary;
        this.targetLayer = targetLayer;
        this.sourceDataSet = sourceDataSet;
        mergeCommand = new MergeCommand(targetLayer, sourceDataSet, false);
    }

    @Override
    public boolean executeCommand() {
        if (!mergeCommand.executeCommand()) {
            return false;
        }
        requiresSaveToFile = targetLayer.requiresSaveToFile();
        requiresUploadToServer = targetLayer.requiresUploadToServer();
        targetLayer.setRequiresSaveToFile(true);
        targetLayer.setRequiresUploadToServer(sourceDataSet.isModified());
        return true;
    }

    @Override
    public void undoCommand() {
        mergeCommand.undoCommand();
        boolean oldValue;
        oldValue = targetLayer.requiresSaveToFile();
        targetLayer.setRequiresSaveToFile(requiresSaveToFile);
        requiresSaveToFile = oldValue;
        oldValue = targetLayer.requiresUploadToServer();
        targetLayer.setRequiresUploadToServer(requiresUploadToServer);
        requiresUploadToServer = oldValue;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getDescriptionText() {
        return commandSummary;
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("dialogs", "down");
    }
    
    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        return sourceDataSet.allPrimitives();
    }
    
    private class DownloadPseudoCommand extends PseudoCommand {

        @Override
        public String getDescriptionText() {
            return tr(marktr("Download {0} nodes, {1} ways, {2} relations"),
                    sourceDataSet.getNodes().size(),
                    sourceDataSet.getWays().size(),
                    sourceDataSet.getRelations().size());
        }

        @Override
        public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
            return sourceDataSet.allPrimitives();
        }
        
        @Override
        public Icon getDescriptionIcon() {
            return ImageProvider.get("dialogs", "down");
        }
        
    }
    @Override
    public Collection<PseudoCommand> getChildren() {
        ArrayList<PseudoCommand> children = new ArrayList<PseudoCommand>();
        children.add(new DownloadPseudoCommand());
        children.add(mergeCommand);
        return children;
    }
    
}
