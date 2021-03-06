//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStream;

import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public class OsmServerLocationReader extends OsmServerReader {

    String url;

    public OsmServerLocationReader(String url) {
        this.url = url;
    }

    /**
     * Method to download OSM files from somewhere
     */
    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        InputStream in = null;
        progressMonitor.beginTask(tr("Contacting Server...", 10));
        try {
            in = getInputStreamRaw(url, progressMonitor.createSubTaskMonitor(9, false));
            if (in == null)
                return null;
            progressMonitor.subTask(tr("Downloading OSM data..."));
            return OsmReader.parseDataSet(in, progressMonitor.createSubTaskMonitor(1, false));
        } catch(OsmTransferException e) {
            throw e;
        } catch (Exception e) {
            if (cancel)
                return null;
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
            try {
                activeConnection = null;
                if (in != null) {
                    in.close();
                }
            } catch(Exception e) {/* ignore it */}
        }
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.josm.io.OsmServerReader#parseOsmChange(org.openstreetmap.josm.gui.progress.ProgressMonitor)
     */
    @Override
    public DataSet parseOsmChange(ProgressMonitor progressMonitor)
            throws OsmTransferException {
        InputStream in = null;
        progressMonitor.beginTask(tr("Contacting Server...", 10));
        try {
            in = getInputStreamRaw(url, progressMonitor.createSubTaskMonitor(9, false));
            if (in == null)
                return null;
            progressMonitor.subTask(tr("Downloading OSM data..."));
            return OsmChangeReader.parseDataSet(in, progressMonitor.createSubTaskMonitor(1, false));
        } catch(OsmTransferException e) {
            throw e;
        } catch (Exception e) {
            if (cancel)
                return null;
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
            try {
                activeConnection = null;
                if (in != null) {
                    in.close();
                }
            } catch(Exception e) {/* ignore it */}
        }
    }

    @Override
    public GpxData parseRawGps(ProgressMonitor progressMonitor) throws OsmTransferException {
        InputStream in = null;
        progressMonitor.beginTask(tr("Contacting Server...", 10));
        try {
            in = getInputStreamRaw(url, progressMonitor.createSubTaskMonitor(1, true));
            if (in == null)
                return null;
            progressMonitor.subTask(tr("Downloading OSM data..."));
            GpxReader reader = new GpxReader(in);
            reader.parse(false);
            GpxData result = reader.data;
            result.fromServer = true;
            return result;
        } catch(OsmTransferException e) {
            throw e;
        } catch (Exception e) {
            if (cancel)
                return null;
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
            try {
                activeConnection = null;
                if (in != null) {
                    in.close();
                }
            } catch(Exception e) {/* ignore it */}
        }
    }
}
