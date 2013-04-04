package no.uib.fragmentation_analyzer.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import no.uib.fragmentation_analyzer.filefilters.PngFileFilter;
import no.uib.fragmentation_analyzer.util.UserProperties;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

/**
 * A panel that sets up a heat map from the provided data. The data has to be in
 * the [0-1] range. Any any headers should be included in the table itself.
 *
 * <br><br>
 *
 * Example: <table border="1"> <tr><th> <td>1<td>2<td>3<td>A
 * <tr><th>1<td>1.0<td>0.3<td>0.2<td>0.2 <tr><th>2<td>0.3<td>1.0<td>0.1<td>0.2
 * <tr><th>3<td>0.5<td>0.5<td>1.0<td>0.5 <tr><th>A<td>0.2<td>0.2<td>0.5<td>1.0
 * </table>
 *
 * @author Harald Barsnes
 */
public class HeatMapJPanel extends javax.swing.JPanel {

    private String[][] heatMapData;
    private UserProperties userProperties;

    /**
     * Creates a HeatMapJPanel and inserts the provided data.
     *
     * <br><br>
     *
     * Example: <table border="1"> <tr><th> <td>1<td>2<td>3<td>A
     * <tr><th>1<td>1.0<td>0.3<td>0.2<td>0.2
     * <tr><th>2<td>0.3<td>1.0<td>0.1<td>0.2
     * <tr><th>3<td>0.5<td>0.5<td>1.0<td>0.5
     * <tr><th>A<td>0.2<td>0.2<td>0.5<td>1.0 </table>
     *
     * @param userProperties
     * @param heatMapData the data to map
     */
    public HeatMapJPanel(UserProperties userProperties, String[][] heatMapData) {
        initComponents();

        this.userProperties = userProperties;
        this.heatMapData = heatMapData;

        // set up the heat map color coding
        setUpHeatMapColorCoding();

        // insert data
        insertHeatMapData(heatMapData);

        // set the table properties
        setTableProperties();
    }

    /**
     * Builds the heat map based in the provided data.
     *
     * @param heatMapData the data to map
     */
    private void insertHeatMapData(String[][] heatMapData) {

        ArrayList<String> columnIdentifiers = new ArrayList<String>();
        columnIdentifiers.add(" ");

        for (int i = 1; i < heatMapData.length; i++) {
            columnIdentifiers.add("" + i);
        }

        Object[] columnHeaders = columnIdentifiers.toArray();

        heatMapJXTable.setModel(new javax.swing.table.DefaultTableModel(
                heatMapData, columnHeaders) {
            @Override
            public Class getColumnClass(int columnIndex) {
                return java.lang.String.class;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        });
    }

    /**
     * Sets up the properties of the heat map table to make it look like a heat
     * map.
     */
    private void setTableProperties() {

        // set the vertical and horizontal cell alignment
        Enumeration<TableColumn> enumerator = heatMapJXTable.getColumnModel().getColumns();

        while (enumerator.hasMoreElements()) {
            enumerator.nextElement().setCellRenderer(new MyCellRenderer());
        }

        // hardcode the width of the first column
        heatMapJXTable.getColumn(0).setMaxWidth(25);
        heatMapJXTable.getColumn(0).setMinWidth(25);

        // make it possible to resize the height of the rows individually
        heatMapJXTable.setRowHeightEnabled(true);

        // disable all types of cell selection
        heatMapJXTable.setRowSelectionAllowed(false);
        heatMapJXTable.setColumnSelectionAllowed(false);
        heatMapJXTable.setCellSelectionEnabled(false);

        // set the column resize type
        heatMapJXTable.setAutoResizeMode(JXTable.AUTO_RESIZE_ALL_COLUMNS);
        //heatMapJXTable.setHorizontalScrollEnabled(true);

        // hide the table grid
        heatMapJXTable.setShowGrid(false, false);
    }

    /**
     * Sets up the color coding scheme used for the heat map.
     */
    private void setUpHeatMapColorCoding() {

        boolean showNumbers = false;

        int numberOfColorLevels = 50;
        double distanceBetweenCorrelationLevels = 1 / (((double) numberOfColorLevels) / 2);

        for (int i = 0; i < (numberOfColorLevels / 2); i++) {

            final Double lowerRange = new Double(-1.0 + (i * distanceBetweenCorrelationLevels));
            final Double upperRange = new Double(-1.0 + ((i + 1) * distanceBetweenCorrelationLevels));

            final Color backGroundColor = new Color(50 - (i * 2), 255 - (i * 10), 0);

            //System.out.println("lowerRange: " + lowerRange);
            //System.out.println("upperRange: " + upperRange);
            //System.out.println("backGroundColor: " + backGroundColor);

            HighlightPredicate highlightPredicate = new HighlightPredicate() {
                public boolean isHighlighted(Component component, ComponentAdapter adapter) {
                    return setRange(adapter.getValue(), adapter.row, adapter.column);
                }

                private boolean setRange(Object value, int row, int column) {

                    if (row > 0 && column > 0) {

                        try {
                            Double tempValue = new Double(value.toString());

                            if (tempValue >= lowerRange && tempValue < upperRange) {
                                return true;
                            }

                        } catch (NumberFormatException e) {
                            return false;
                        }
                    } else {
                        return false;
                    }

                    return false;
                }
            };

            Color numberColor = Color.BLACK;

            if (!showNumbers) {
                numberColor = backGroundColor;
            }

            Highlighter hl = new ColorHighlighter(backGroundColor, numberColor, highlightPredicate);
            heatMapJXTable.addHighlighter(hl);
        }


        //System.out.println();


        for (int i = 0; i < (numberOfColorLevels / 2); i++) {

            final Double lowerRange = new Double(0.0 + distanceBetweenCorrelationLevels * i);
            final Double upperRange = new Double(0.0 + distanceBetweenCorrelationLevels * (i + 1));

            final Color backGroundColor = new Color(15 + 10 * i, 0, 0);

            //System.out.println("lowerRange: " + lowerRange);
            //System.out.println("upperRange: " + upperRange);
            //System.out.println("backGroundColor: " + backGroundColor);

            HighlightPredicate highlightPredicate = new HighlightPredicate() {
                public boolean isHighlighted(Component component, ComponentAdapter adapter) {
                    return setRange(adapter.getValue(), adapter.row, adapter.column);
                }

                private boolean setRange(Object value, int row, int column) {

                    if (row > 0 && column > 0) {

                        try {
                            Double tempValue = new Double(value.toString());

                            if (tempValue >= lowerRange && tempValue < upperRange) {
                                return true;
                            }

                            if (upperRange.doubleValue() == 1.0 && tempValue.doubleValue() == upperRange.doubleValue()) {
                                return true;
                            }

                        } catch (NumberFormatException e) {
                            return false;
                        }
                    } else {
                        return false;
                    }

                    return false;
                }
            };

            Color numberColor = Color.BLACK;

            if (!showNumbers) {
                numberColor = backGroundColor;
            }

            Highlighter hl = new ColorHighlighter(backGroundColor, numberColor, highlightPredicate);
            heatMapJXTable.addHighlighter(hl);
        }
    }

    /**
     * Set the tool tip to use for the heat map
     *
     * @param toolTip
     */
    public void setHeatMapToolTip(String toolTip) {
        heatMapJXTable.setToolTipText(toolTip);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        savePopupMenu = new javax.swing.JPopupMenu();
        saveJMenuItem = new javax.swing.JMenuItem();
        heatMapJScrollPane = new javax.swing.JScrollPane();
        scrollPaneJPanel = new javax.swing.JPanel();
        heatMapJXTable = new org.jdesktop.swingx.JXTable();

        saveJMenuItem.setText("Save Heat Map As PNG");
        saveJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveJMenuItemActionPerformed(evt);
            }
        });
        savePopupMenu.add(saveJMenuItem);

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        heatMapJScrollPane.setBackground(new java.awt.Color(255, 255, 255));
        heatMapJScrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255), 0));
        heatMapJScrollPane.setOpaque(false);
        heatMapJScrollPane.setPreferredSize(new java.awt.Dimension(450, 108));

        scrollPaneJPanel.setBackground(new java.awt.Color(255, 255, 255));

        heatMapJXTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"", "S1", "S2", "S3", "S4", "Avg"},
                {"S1", "0.14", "0.45", "0.13", "0.15", "0.32"},
                {"S2", "0.26", "0.37", "0.24", "0.2", "0.44"},
                {"S3", "0.333", "0.28", "0.16", "0.37", "0.34"},
                {"S4", "0.10", "0.10", "0.17", "0.32", "0.33"},
                {"Avg", "0.42", "0.11", "0.28", "0.37", "0.31"}
            },
            new String [] {
                " ", "S1", "S2", "S3", "S4", "Avg"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        heatMapJXTable.setToolTipText("<html>\n<font color=\"red\">Red: Positive Linear Correlation</font><br>\n<font color=\"black\">Black: No Linear Correlation</font><br>\n<font color=\"green\">Green: Negative Linear Correlation</font>\n</html>");
        heatMapJXTable.setFillsViewportHeight(false);
        heatMapJXTable.setFont(heatMapJXTable.getFont().deriveFont(heatMapJXTable.getFont().getSize()-3f));
        heatMapJXTable.setGridColor(new java.awt.Color(255, 255, 255));
        heatMapJXTable.setHorizontalScrollEnabled(true);
        heatMapJXTable.setMinimumSize(new java.awt.Dimension(11, 11));
        heatMapJXTable.setPreferredScrollableViewportSize(new java.awt.Dimension(250, 360));
        heatMapJXTable.setPreferredSize(new java.awt.Dimension(250, 108));
        heatMapJXTable.setShowGrid(false);
        heatMapJXTable.setSortable(false);
        heatMapJXTable.setTableHeader(null);
        heatMapJXTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                heatMapJXTablejPanelMouseClicked(evt);
            }
        });
        scrollPaneJPanel.add(heatMapJXTable);

        heatMapJScrollPane.setViewportView(scrollPaneJPanel);

        add(heatMapJScrollPane);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Right clicking on the heat map opens a popup menu with the option of
     * saving the heat map as a GIF file.
     *
     * @param evt
     */
    private void heatMapJXTablejPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_heatMapJXTablejPanelMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON3) {
            savePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_heatMapJXTablejPanelMouseClicked

    /**
     * When the panel is resized the heat map is resized to make sure that it is
     * always completely visible and rectangular. (Unless the panel gets too
     * small, then scroll bars are shown).
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        // update the size of the scroll pane and the heat map relative to the available space

        int width = this.getWidth();
        int height = this.getHeight();

        int padding = 15;
        int minimumHeatMapColumnWidth = 18;

        int newScrollPaneSize;

        // this is the minimum size required by the heat map
        int newHeatMapSize = (heatMapJXTable.getColumnCount() - 1) * minimumHeatMapColumnWidth
                + heatMapJXTable.getColumn(0).getWidth();

        if (width < height) {
            newScrollPaneSize = width - padding;
        } else {
            newScrollPaneSize = height - padding;
        }

        // if the minimum size required by the heat map is smaller than the size of the
        // scroll pane, increase the size of the heat map to cover the whole scroll pane
        if (newHeatMapSize < newScrollPaneSize) {
            newHeatMapSize = newScrollPaneSize - 10; // some padding seems to be needed
        }

        // set the size of the scroll pane to the maximum square possible inside the panel
        heatMapJScrollPane.setPreferredSize(new Dimension(newScrollPaneSize, newScrollPaneSize));

        // set the size of the heat map so that scrollbars are shown if it is too big for the scrollpane
        heatMapJXTable.setPreferredSize(new Dimension(newHeatMapSize, newHeatMapSize));

        this.revalidate();
        repaint();


        // set the horizontal size of the rows
        // (the columns will be automatically taken care of)
        for (int i = 1; i < heatMapJXTable.getRowCount(); i++) {
            heatMapJXTable.setRowHeight(i, heatMapJXTable.getColumn(1).getWidth());
        }


        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                repaint();

                // set the column size again (seems to be needed)
                for (int i = 1; i < heatMapJXTable.getRowCount(); i++) {
                    heatMapJXTable.setRowHeight(i, heatMapJXTable.getColumn(1).getWidth());
                }
            }
        });
    }//GEN-LAST:event_formComponentResized

    /**
     * Saves the heat map as a PNG file.
     *
     * @param evt
     */
    private void saveJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveJMenuItemActionPerformed

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        JFileChooser chooser = new JFileChooser(userProperties.getLastUsedFolder());
        chooser.setFileFilter(new PngFileFilter());

        int returnVal = chooser.showSaveDialog(this);

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            String selectedFile = chooser.getSelectedFile().getPath();

            if (!selectedFile.endsWith(".png") && !selectedFile.endsWith(".PNG")) {
                selectedFile = selectedFile + ".png";
            }

            boolean saveFile = true;

            if (new File(selectedFile).exists()) {
                int option = JOptionPane.showConfirmDialog(this,
                        "The file " + selectedFile + " already exists. Overwrite?",
                        "Overwrite?", JOptionPane.YES_NO_CANCEL_OPTION);

                if (option != JOptionPane.YES_OPTION) {
                    saveFile = false;
                }
            }

            if (saveFile) {

                userProperties.setLastUsedFolder(selectedFile);
                userProperties.saveUserPropertiesToFile();

                try {
                    File fileName = new File(selectedFile);
                    Image img = createImage(heatMapJXTable.getSize().width, heatMapJXTable.getSize().height);
                    Graphics g = img.getGraphics();
                    heatMapJXTable.paint(g);
                    ImageIO.write(toBufferedImage(img), "png", fileName);
                    JOptionPane.showMessageDialog(this, "Heat map saved to " + fileName.toString(),
                            "Heat Map Saved", JOptionPane.INFORMATION_MESSAGE);
                    g.dispose();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
}//GEN-LAST:event_saveJMenuItemActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane heatMapJScrollPane;
    private org.jdesktop.swingx.JXTable heatMapJXTable;
    private javax.swing.JMenuItem saveJMenuItem;
    private javax.swing.JPopupMenu savePopupMenu;
    private javax.swing.JPanel scrollPaneJPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * Returns the heat map table.
     *
     * @return the heat map table
     */
    public JXTable getHeatMap() {
        return heatMapJXTable;
    }

    /**
     * This method returns a buffered image with the contents of an image.
     *
     * (taken from: http://www.daniweb.com/forums/thread136630.html#)
     *
     * @param image
     * @return a buffered image with the contents of an image
     */
    private BufferedImage toBufferedImage(Image image) {

        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();

        // Determine if the image has transparent pixels; for this method's
        // implementation, see e661 Determining If an Image Has Transparent Pixels
        boolean hasAlpha = hasAlpha(image);

        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;

            if (hasAlpha) {
                transparency = Transparency.BITMASK;
            }

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);

        } catch (HeadlessException e) {
            // The system does not have a screen
        }

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;

            if (hasAlpha) {
                type = BufferedImage.TYPE_INT_ARGB;
            }

            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        //g.drawImage(image, 0, 0, image.getWidth(null), image.getHeight(null), null);
        g.drawImage(image, heatMapJXTable.getX(), heatMapJXTable.getY(), 50, 50, null);
        g.dispose();

        return bimage;
    }

    /**
     * This method returns true if the specified image has transparent pixels
     *
     * (taken from: http://www.daniweb.com/forums/thread136630.html#)
     *
     * @param image
     * @return true if the image has transparent pixels
     */
    public static boolean hasAlpha(Image image) {

        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            BufferedImage bimage = (BufferedImage) image;
            return bimage.getColorModel().hasAlpha();
        }

        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);

        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
        }

        // Get the image's color model
        ColorModel cm = pg.getColorModel();
        return cm.hasAlpha();
    }

    /**
     * A simple cell renderer that centers the text both vertically and
     * horizontally.
     */
    class MyCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            JLabel renderedLabel = (JLabel) super.getTableCellRendererComponent(
                    table, value, false, false, row, column);
            renderedLabel.setHorizontalAlignment(SwingConstants.CENTER);
            renderedLabel.setVerticalAlignment(SwingConstants.CENTER);

            return renderedLabel;
        }
    }
}
