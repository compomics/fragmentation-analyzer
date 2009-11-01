package no.uib.fragmentation_analyzer.util;

import java.awt.Component;
import java.util.HashMap;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;

/**
 * ListCellRenderer with alignment and icon image functionality.
 *
 * @author Harald Barsnes
 */
public class AlignedListCellRenderer extends DefaultListCellRenderer {

    /**
     * One of the following constants defined in SwingConstants: LEFT, CENTER
     * (the default for image-only labels), RIGHT, LEADING (the default for
     * text-only labels) or TRAILING.
     */
    private int align;

    /**
     * A mapping of text to icon mappings used to add icons to the labels.
     */
    private static HashMap<String, String> iconMappings = new HashMap<String, String>();

    /**
     * Creates a new AlignedListCellRenderer
     *
     * @param align SwingConstant: LEFT, CENTER, RIGHT, LEADING or TRAILING.
     */
    public AlignedListCellRenderer(int align) {
        this.align = align;
        setUpIconMappings();
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        JLabel lbl = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

        // set the standard horizontal alignment
        lbl.setHorizontalAlignment(align);

        // add icon
        if(iconMappings.containsKey(lbl.getText())){
            lbl.setIcon(new javax.swing.ImageIcon(getClass().getResource(
                    "/no/uib/fragmentation_analyzer/icons/"
                    + iconMappings.get(lbl.getText()))));

            lbl.setHorizontalAlignment(SwingConstants.LEADING);
            lbl.setIconTextGap(15);
        }

        return lbl;
    }

    /**
     * Set up the text to icon mappings
     */
    private void setUpIconMappings(){
        iconMappings.put("List Individual Identifications", "list.GIF");
        iconMappings.put("Intensity Box Plot", "box_plot_small.GIF");
        iconMappings.put("Mass Error Scatter Plot", "scatter_plot.GIF");
        iconMappings.put("Mass Error Bubble Plot", "bubble_plot.GIF");
        iconMappings.put("Mass Error Box Plot", "box_plot_small.GIF");
        iconMappings.put("Fragment Ion Probability Plot", "line_plot.GIF");
        iconMappings.put("View Spectra", "spectrum.GIF");
    }
}
