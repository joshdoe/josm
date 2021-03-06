// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;

/**
 * This is a helper class for persisting the geometry of a JOSM window to the preference store
 * and for restoring it from the preference store.
 *
 */
public class WindowGeometry {

    /**
     * Replies a window geometry object for a window with a specific size which is
     * centered on screen, where main window is
     *
     * @param extent  the size
     * @return the geometry object
     */
    static public WindowGeometry centerOnScreen(Dimension extent) {
        return centerOnScreen(extent, "gui.geometry");
    }

    /**
     * Replies a window geometry object for a window with a specific size which is
     * centered on screen where the corresponding window is.
     *
     * @param extent  the size
     * @param preferenceKey the key to get window size and position from, null value format
     * for whole virtual screen
     * @return the geometry object
     */
    static public WindowGeometry centerOnScreen(Dimension extent, String preferenceKey) {
        Rectangle size = preferenceKey != null ? getScreenInfo(preferenceKey)
            : getFullScreenInfo();
        Point topLeft = new Point(
                size.x + Math.max(0, (size.width - extent.width) /2),
                size.y + Math.max(0, (size.height - extent.height) /2)
        );
        return new WindowGeometry(topLeft, extent);
    }

    /**
     * Replies a window geometry object for a window with a specific size which is centered
     * relative to the parent window of a reference component.
     *
     * @param reference the reference component.
     * @param extent the size
     * @return the geometry object
     */
    static public WindowGeometry centerInWindow(Component reference, Dimension extent) {
        Window parentWindow = null;
        while(reference != null && ! (reference instanceof Window) ) {
            reference = reference.getParent();
        }
        if (reference == null)
            return new WindowGeometry(new Point(0,0), extent);
        parentWindow = (Window)reference;
        Point topLeft = new Point(
                Math.max(0, (parentWindow.getSize().width - extent.width) /2),
                Math.max(0, (parentWindow.getSize().height - extent.height) /2)
        );
        topLeft.x += parentWindow.getLocation().x;
        topLeft.y += parentWindow.getLocation().y;
        return new WindowGeometry(topLeft, extent);
    }

    /**
     * Exception thrown by the WindowGeometry class if something goes wrong
     *
     */
    static public class WindowGeometryException extends Exception {
        public WindowGeometryException(String message, Throwable cause) {
            super(message, cause);
        }

        public WindowGeometryException(String message) {
            super(message);
        }
    }

    /** the top left point */
    private Point topLeft;
    /** the size */
    private Dimension extent;

    /**
     *
     * @param topLeft the top left point
     * @param extent the extent
     */
    public WindowGeometry(Point topLeft, Dimension extent) {
        this.topLeft = topLeft;
        this.extent = extent;
    }

    /**
     *
     * @param rect the position
     */
    public WindowGeometry(Rectangle rect) {
        this.topLeft = rect.getLocation();
        this.extent = rect.getSize();
    }

    /**
     * Creates a window geometry from the position and the size of a window.
     *
     * @param window the window
     */
    public WindowGeometry(Window window)  {
        this(window.getLocationOnScreen(), window.getSize());
    }

    protected int parseField(String preferenceKey, String preferenceValue, String field) throws WindowGeometryException {
        String v = "";
        try {
            Pattern p = Pattern.compile(field + "=(-?\\d+)",Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(preferenceValue);
            if (!m.find())
                throw new WindowGeometryException(tr("Preference with key ''{0}'' does not include ''{1}''. Cannot restore window geometry from preferences.", preferenceKey, field));
            v = m.group(1);
            return Integer.parseInt(v);
        } catch(WindowGeometryException e) {
            throw e;
        } catch(NumberFormatException e) {
            throw new WindowGeometryException(tr("Preference with key ''{0}'' does not provide an int value for ''{1}''. Got {2}. Cannot restore window geometry from preferences.", preferenceKey, field, v));
        } catch(Exception e) {
            throw new WindowGeometryException(tr("Failed to parse field ''{1}'' in preference with key ''{0}''. Exception was: {2}. Cannot restore window geometry from preferences.", preferenceKey, field, e.toString()), e);
        }
    }

    protected void initFromPreferences(String preferenceKey) throws WindowGeometryException {
        String value = Main.pref.get(preferenceKey);
        if (value == null || value.equals(""))
            throw new WindowGeometryException(tr("Preference with key ''{0}'' does not exist. Cannot restore window geometry from preferences.", preferenceKey));
        topLeft = new Point();
        extent = new Dimension();
        topLeft.x = parseField(preferenceKey, value, "x");
        topLeft.y = parseField(preferenceKey, value, "y");
        extent.width = parseField(preferenceKey, value, "width");
        extent.height = parseField(preferenceKey, value, "height");
    }

    protected void initFromWindowGeometry(WindowGeometry other) {
        this.topLeft = other.topLeft;
        this.extent = other.extent;
    }

    static public WindowGeometry mainWindow(String preferenceKey, String arg, boolean maximize) {
        Rectangle screenDimension = getScreenInfo("gui.geometry");
        if (arg != null) {
            final Matcher m = Pattern.compile("(\\d+)x(\\d+)(([+-])(\\d+)([+-])(\\d+))?").matcher(arg);
            if (m.matches()) {
                int w = Integer.valueOf(m.group(1));
                int h = Integer.valueOf(m.group(2));
                int x = screenDimension.x, y = screenDimension.y;
                if (m.group(3) != null) {
                    x = Integer.valueOf(m.group(5));
                    y = Integer.valueOf(m.group(7));
                    if (m.group(4).equals("-")) {
                        x = screenDimension.x + screenDimension.width - x - w;
                    }
                    if (m.group(6).equals("-")) {
                        y = screenDimension.y + screenDimension.height - y - h;
                    }
                }
                return new WindowGeometry(new Point(x,y), new Dimension(w,h));
            } else {
                Main.warn(tr("Ignoring malformed geometry: {0}", arg));
            }
        }
        WindowGeometry def;
        if(maximize) {
            def = new WindowGeometry(screenDimension);
        } else {
            Point p = screenDimension.getLocation();
            p.x += (screenDimension.width-1000)/2;
            p.y += (screenDimension.height-740)/2;
            def = new WindowGeometry(p, new Dimension(1000, 740));
        }
        return new WindowGeometry(preferenceKey, def);
    }

    /**
     * Creates a window geometry from the values kept in the preference store under the
     * key <code>preferenceKey</code>
     *
     * @param preferenceKey the preference key
     * @throws WindowGeometryException thrown if no such key exist or if the preference value has
     * an illegal format
     */
    public WindowGeometry(String preferenceKey) throws WindowGeometryException {
        initFromPreferences(preferenceKey);
    }

    /**
     * Creates a window geometry from the values kept in the preference store under the
     * key <code>preferenceKey</code>. Falls back to the <code>defaultGeometry</code> if
     * something goes wrong.
     *
     * @param preferenceKey the preference key
     * @param defaultGeometry the default geometry
     *
     */
    public WindowGeometry(String preferenceKey, WindowGeometry defaultGeometry) {
        try {
            initFromPreferences(preferenceKey);
        } catch(WindowGeometryException e) {
            initFromWindowGeometry(defaultGeometry);
        }
    }

    /**
     * Remembers a window geometry under a specific preference key
     *
     * @param preferenceKey the preference key
     */
    public void remember(String preferenceKey) {
        StringBuffer value = new StringBuffer();
        value.append("x=").append(topLeft.x).append(",")
        .append("y=").append(topLeft.y).append(",")
        .append("width=").append(extent.width).append(",")
        .append("height=").append(extent.height);
        Main.pref.put(preferenceKey, value.toString());
    }

    /**
     * Replies the top left point for the geometry
     *
     * @return  the top left point for the geometry
     */
    public Point getTopLeft() {
        return topLeft;
    }

    /**
     * Replies the size spezified by the geometry
     *
     * @return the size spezified by the geometry
     */
    public Dimension getSize() {
        return extent;
    }

    private Rectangle getRectangle() {
        return new Rectangle(topLeft, extent);
    }

    /**
     * Applies this geometry to a window. Makes sure that the window is not
     * placed outside of the coordinate range of all available screens.
     * 
     * @param window the window
     */
    public void applySafe(Window window) {
        Point p = new Point(topLeft);

        Rectangle virtualBounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (int j = 0; j < gs.length; j++) {
            GraphicsDevice gd = gs[j];
            GraphicsConfiguration[] gc = gd.getConfigurations();
            for (int i = 0; i < gc.length; i++) {
                virtualBounds = virtualBounds.union(gc[i].getBounds());
            }
        }

        if (p.x < virtualBounds.x) {
            p.x = virtualBounds.x;
        } else if (p.x > virtualBounds.x + virtualBounds.width - extent.width) {
            p.x = virtualBounds.x + virtualBounds.width - extent.width;
        }

        if (p.y < virtualBounds.y) {
            p.y = virtualBounds.y;
        } else if (p.y > virtualBounds.y + virtualBounds.height - extent.height) {
            p.y = virtualBounds.y + virtualBounds.height - extent.height;
        }

        window.setLocation(p);
        window.setSize(extent);
    }

    /**
     * Find the size and position of the screen for given coordinates. Use first screen,
     * when no coordinates are stored or null is passed.
     * 
     * @param preferenceKey the key to get size and position from
     */
    public static Rectangle getScreenInfo(String preferenceKey) {
        Rectangle g = new WindowGeometry(preferenceKey,
            /* default: something on screen 1 */
            new WindowGeometry(new Point(0,0), new Dimension(10,10))).getRectangle();
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        int intersect = 0;
        Rectangle bounds = null;
        for (int j = 0; j < gs.length; j++) {
            GraphicsDevice gd = gs[j];
            GraphicsConfiguration[] gc = gd.getConfigurations();
            for (int i = 0; i < gc.length; i++) {
                Rectangle b = gc[i].getBounds();
                if(b.width/b.height >= 3) /* multiscreen with wrong definition */
                {
                    b.width /= 2;
                    Rectangle is = b.intersection(g);
                    int s = is.width*is.height;
                    if(bounds == null || intersect < s) {
                        intersect = s;
                        bounds = b;
                    }
                    b = new Rectangle(b);
                    b.x += b.width;
                    is = b.intersection(g);
                    s = is.width*is.height;
                    if(bounds == null || intersect < s) {
                        intersect = s;
                        bounds = b;
                    }
                }
                else
                {
                    Rectangle is = b.intersection(g);
                    int s = is.width*is.height;
                    if(bounds == null || intersect < s) {
                        intersect = s;
                        bounds = b;
                    }
                }
            }
        }
        return bounds;
    }

    /**
     * Find the size of the full virtual screen.
     */
    public static Rectangle getFullScreenInfo() {
        return new Rectangle(new Point(0,0), Toolkit.getDefaultToolkit().getScreenSize());
    }

    public String toString() {
        return "WindowGeometry{topLeft="+topLeft+",extent="+extent+"}";
    }
}
