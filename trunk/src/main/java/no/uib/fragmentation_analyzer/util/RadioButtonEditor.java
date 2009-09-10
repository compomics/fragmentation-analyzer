package no.uib.fragmentation_analyzer.util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A cell editor for radio buttons.
 * 
 * @author  Harald Barsnes
 */
public class RadioButtonEditor extends DefaultCellEditor implements ItemListener {

    private JRadioButton button;

    /**
     * Creates a RadioButtonEditor-object
     *
     * @param checkBox
     */
    public RadioButtonEditor(JCheckBox checkBox) {
        super(checkBox);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        if (value == null) {
            return null;
        }
        button = (JRadioButton) value;
        button.addItemListener(this);
        return (Component) value;
    }

    @Override
    public Object getCellEditorValue() {
        button.removeItemListener(this);
        return button;
    }

    /**
     * Method overridden from DefaultCellEditor
     *
     * @param e
     */
    public void itemStateChanged(ItemEvent e) {
        super.fireEditingStopped();
    }
}
