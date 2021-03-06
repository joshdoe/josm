// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import java.util.Date;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.User;

/**
 * Represents an immutable OSM node in the context of a historical view on
 * OSM data.
 *
 */
public class HistoryNode extends HistoryOsmPrimitive {
    /** the coordinates */

    private LatLon coords;

    public HistoryNode(long id, long version, boolean visible, User user, long changesetId, Date timestamp, LatLon coords) {
        super(id, version, visible, user, changesetId, timestamp);
        setCoords(coords);
    }

    public HistoryNode(Node p) {
        super(p);
        setCoords(p.getCoor());
    }

    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.NODE;
    }

    public LatLon getCoords() {
        return coords;
    }

    public void setCoords(LatLon coords) {
        this.coords = coords;
    }

    @Override
    public String getDisplayName(HistoryNameFormatter formatter) {
        return formatter.format(this);
    }
}
