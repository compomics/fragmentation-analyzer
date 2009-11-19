
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
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
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
import org.jdesktop.swingx.decorator.SearchPredicate;

/**
 *
 * @author Harald Barsnes
 */
public class HeatMapJPanel extends javax.swing.JPanel {

    String[][] heatMapData;

    /** Creates new form HeatMapJPanel */
    public HeatMapJPanel(String[][] heatMapData) {
        initComponents();

        this.heatMapData = heatMapData;

        // set up the heat map color coding
        setUpHeatMapColorCoding();

        // insert data
        insertHeatMapData(heatMapData);

        // set the table properties
        setTableProperties();
    }

    /**
     *
     * @param heatMapData
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
     *
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

        // hide the table grid
        heatMapJXTable.setShowGrid(false, false);
    }

    /**
     *
     */
    private void setUpHeatMapColorCoding() {

        boolean showNumbers = false;

        // ToDo: increase the resolution, i.e., more color levels

        Color[] heatMapColors = {
            new Color(50, 255, 0), // 0.0
            new Color(50, 255, 0), // 0.0-0.1
            new Color(40, 205, 0), // 0.1-0.2
            new Color(30, 155, 0), // 0.2-0.3
            new Color(20, 55, 0), // 0.3-0.4
            new Color(0, 0, 0), // 0.4-0.5
            new Color(55, 0, 0), // 0.5-0.6
            new Color(105, 0, 0), // 0.7-0.8
            new Color(155, 0, 0), // 0.8-0.9
            new Color(205, 0, 0), // 0.9-1.0
            new Color(255, 0, 0), // 1.0
        };


        // add the heat map colors
        for (int i = 0; i < heatMapColors.length - 1; i++) {

            Color numberColor = Color.BLACK;

            if (!showNumbers) {
                numberColor = heatMapColors[i];
            }

            Pattern pattern = Pattern.compile("0\\." + i);
            Highlighter highlighter = new ColorHighlighter(
                    heatMapColors[i],
                    numberColor,
                    new SearchPredicate(pattern));
            heatMapJXTable.addHighlighter(highlighter);
        }


        // add the last heat map color
        Color numberColor = Color.BLACK;

        if (!showNumbers) {
            numberColor = heatMapColors[heatMapColors.length - 1];
        }

        Pattern pattern = Pattern.compile("1\\.0");
        Highlighter highlighter = new ColorHighlighter(
                heatMapColors[heatMapColors.length - 1],
                numberColor,
                new SearchPredicate(pattern));
        heatMapJXTable.addHighlighter(highlighter);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        savePopupMenu = new javax.swing.JPopupMenu();
        saveJMenuItem = new javax.swing.JMenuItem();
        heatMapJXTable = new org.jdesktop.swingx.JXTable();

        saveJMenuItem.setText("Save");
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
        heatMapJXTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        heatMapJXTable.setFillsViewportHeight(false);
        heatMapJXTable.setFont(heatMapJXTable.getFont().deriveFont(heatMapJXTable.getFont().getSize()-3f));
        heatMapJXTable.setGridColor(new java.awt.Color(255, 255, 255));
        heatMapJXTable.setShowGrid(false);
        heatMapJXTable.setSortable(false);
        heatMapJXTable.setTableHeader(null);
        heatMapJXTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                heatMapJXTablejPanelMouseClicked(evt);
            }
        });
        add(heatMapJXTable);
    }// </editor-fold>//GEN-END:initComponents

    /**
     *
     * @param evt
     */
    private void heatMapJXTablejPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_heatMapJXTablejPanelMouseClicked

        if (evt.getButton() == evt.BUTTON3) {
            savePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_heatMapJXTablejPanelMouseClicked

    /**
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        int width = this.getWidth();
        int height = this.getHeight();

        int padding = 15;

        if (width < height) {
            heatMapJXTable.setPreferredSize(new Dimension(width - padding, width - padding));
        } else {
            heatMapJXTable.setPreferredSize(new Dimension(height - padding, height - padding));
        }

        this.revalidate();
        repaint();


        for (int i = 1; i < heatMapJXTable.getRowCount(); i++) {
            heatMapJXTable.setRowHeight(i, heatMapJXTable.getColumn(1).getWidth());
        }


        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                repaint();

                for (int i = 1; i < heatMapJXTable.getRowCount(); i++) {
                    heatMapJXTable.setRowHeight(i, heatMapJXTable.getColumn(1).getWidth());
                }
            }
        });

    }//GEN-LAST:event_formComponentResized

    /**
     *
     * @param evt
     */
    private void saveJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveJMenuItemActionPerformed
        try {
            File saveFile = new File("C:\\Harald\\" + "heat_map.gif");
            Image img = createImage(getWidth(), getHeight());
            //Image img = createImage(heatMapJXTable.getWidth(), heatMapJXTable.getHeight());
            Graphics g = img.getGraphics();
            paint(g);
            ImageIO.write(toBufferedImage(img), "gif", saveFile);
            JOptionPane.showMessageDialog(null, "Image saved to " + saveFile.toString());
            g.dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
}//GEN-LAST:event_saveJMenuItemActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jdesktop.swingx.JXTable heatMapJXTable;
    private javax.swing.JMenuItem saveJMenuItem;
    private javax.swing.JPopupMenu savePopupMenu;
    // End of variables declaration//GEN-END:variables

    /**
     * (the following is taken from: http://www.daniweb.com/forums/thread136630.html#)
     *
     * This method returns a buffered image with the contents of an image
     *
     * @param image
     * @return
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
     * @param image
     * @return
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
