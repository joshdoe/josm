// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.gui.conflict.ConflictColors;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is a {@link TableCellRenderer} for {@link MultiValueResolutionDecision}s.
 *
 */
public class MultiValueCellRenderer extends JLabel implements TableCellRenderer {

    private ImageIcon iconDecided;
    private ImageIcon iconUndecided;
    private DefaultComboBoxModel model;
    private JComboBox cbDecisionRenderer;

    public MultiValueCellRenderer() {
        setOpaque(true);
        iconDecided = ImageProvider.get("dialogs/conflict", "tagconflictresolved");
        iconUndecided = ImageProvider.get("dialogs/conflict", "tagconflictunresolved");
        cbDecisionRenderer = new JComboBox(model = new DefaultComboBoxModel());
    }

    protected void renderColors(MultiValueResolutionDecision decision, boolean selected) {
        if (selected) {
            setForeground(UIManager.getColor("Table.selectionForeground"));
            setBackground(UIManager.getColor("Table.selectionBackground"));
        } else{
            switch(decision.getDecisionType()) {
            case UNDECIDED:
                setForeground(UIManager.getColor("Table.foreground"));
                setBackground(ConflictColors.BGCOLOR_UNDECIDED.get());
                break;
            case KEEP_NONE:
                setForeground(UIManager.getColor("Panel.foreground"));
                setBackground(UIManager.getColor("Panel.background"));
                break;
            default:
                setForeground(UIManager.getColor("Table.foreground"));
                setBackground(UIManager.getColor("Table.background"));
                break;
            }
        }
    }

    protected void renderValue(MultiValueResolutionDecision decision) {
        model.removeAllElements();
        switch(decision.getDecisionType()) {
        case UNDECIDED:
            model.addElement(tr("Choose a value"));
            cbDecisionRenderer.setFont(getFont().deriveFont(Font.ITALIC));
            cbDecisionRenderer.setSelectedIndex(0);
            break;
        case KEEP_ONE:
            model.addElement(decision.getChosenValue());
            cbDecisionRenderer.setFont(getFont());
            cbDecisionRenderer.setSelectedIndex(0);
            break;
        case KEEP_NONE:
            model.addElement(tr("deleted"));
            cbDecisionRenderer.setFont(getFont().deriveFont(Font.ITALIC));
            cbDecisionRenderer.setSelectedIndex(0);
            break;
        case KEEP_ALL:
            model.addElement(decision.getChosenValue());
            cbDecisionRenderer.setFont(getFont());
            cbDecisionRenderer.setSelectedIndex(0);
            break;
        }
    }

    /**
     * Sets the text of the tooltip for both renderers, this (the JLabel) and the combobox renderer.
     */
    protected void renderToolTipText(MultiValueResolutionDecision decision) {
        switch(decision.getDecisionType()) {
        case UNDECIDED:
        {
            String toolTipText = tr("Please decide which values to keep");
            setToolTipText(toolTipText);
            cbDecisionRenderer.setToolTipText(toolTipText);
            break;
        }
        case KEEP_ONE:
        {
            String toolTipText = tr("Value ''{0}'' is going to be applied for key ''{1}''", decision.getChosenValue(), decision.getKey());
            setToolTipText(toolTipText);
            cbDecisionRenderer.setToolTipText(toolTipText);
            break;
        }
        case KEEP_NONE:
        {
            String toolTipText = tr("The key ''{0}'' and all its values are going to be removed", decision.getKey());
            setToolTipText(toolTipText);
            cbDecisionRenderer.setToolTipText(toolTipText);
            break;
        }
        case KEEP_ALL:
            String toolTipText = tr("All values joined as ''{0}'' are going to be applied for key ''{1}''", decision.getChosenValue(), decision.getKey());
            setToolTipText(toolTipText);
            cbDecisionRenderer.setToolTipText(toolTipText);
            break;
        }
    }

    protected void reset() {
        setFont(UIManager.getFont("Table.font"));
        setIcon(null);
        setText("");
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        reset();
        if (value == null)
            return this;

        MultiValueResolutionDecision decision = (MultiValueResolutionDecision)value;
        renderColors(decision,isSelected);
        renderToolTipText(decision);
        switch(column) {
        case 0:
            if (decision.isDecided()) {
                setIcon(iconDecided);
            } else {
                setIcon(iconUndecided);
            }
            return this;

        case 1:
            setText(decision.getKey());
            return this;

        case 2:
            renderValue(decision);
            return cbDecisionRenderer;
        }
        return this;
    }
}
