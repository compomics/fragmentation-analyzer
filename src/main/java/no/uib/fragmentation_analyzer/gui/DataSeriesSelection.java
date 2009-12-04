package no.uib.fragmentation_analyzer.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.swing.JOptionPane;
import org.jdesktop.swingx.decorator.SortOrder;
import javax.swing.table.DefaultTableModel;
import no.uib.fragmentation_analyzer.util.Properties;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.Layer;

/**
 * A dialog for selecting the currently visible data series in a mass error plot.
 *
 * @author Harald Barsnes
 */
public class DataSeriesSelection extends javax.swing.JDialog {

    private ChartPanel chartPanel;
    private HashMap<String, Integer> seriesKeyToSeriesNumber;
    private boolean selectAllSeries = true;
    private FragmentationAnalyzer fragmentationAnalyzer;
    private FragmentationAnalyzerJInternalFrame currentFrame;

    /**
     * Creates a new DataSeriesSelection dialog and makes it visible
     *
     * @param fragmentationAnalyzer
     * @param currentFrame
     * @param modal
     */
    public DataSeriesSelection(FragmentationAnalyzer fragmentationAnalyzer, boolean modal, 
            FragmentationAnalyzerJInternalFrame currentFrame) {
        super(fragmentationAnalyzer, modal);
        initComponents();

        this.fragmentationAnalyzer = fragmentationAnalyzer;
        this.currentFrame = currentFrame;
        chartPanel = currentFrame.getChartPanel();

        insertDataSeries();

        // set the column properties
        dataSeriesJXTable.getColumn(" ").setMinWidth(30);
        dataSeriesJXTable.getColumn(" ").setMaxWidth(30);
        dataSeriesJXTable.getColumn("  ").setMaxWidth(30);
        dataSeriesJXTable.getColumn("  ").setMinWidth(30);

        setLocationRelativeTo(fragmentationAnalyzer);
        setVisible(true);
    }

    /**
     * Inserts the data about the data series into the table.
     */
    private void insertDataSeries() {

        if (chartPanel.getChart().getPlot() instanceof XYPlot) {
            XYDataset xyDataSet = ((XYPlot) chartPanel.getChart().getPlot()).getDataset(0);

            ArrayList<String> seriesKeys = new ArrayList<String>();
            seriesKeyToSeriesNumber = new HashMap<String, Integer>();

            // get all the data series keys
            for (int i = 0; i < xyDataSet.getSeriesCount(); i++) {
                seriesKeys.add(xyDataSet.getSeriesKey(i).toString());
                seriesKeyToSeriesNumber.put(xyDataSet.getSeriesKey(i).toString(), i);
            }

            // sort the series keys in acending order
            java.util.Collections.sort(seriesKeys);

            // add the series keys to the table
            for (int i = 0; i < seriesKeys.size(); i++) {
                ((DefaultTableModel) dataSeriesJXTable.getModel()).addRow(new Object[]{
                            new Integer((i + 1)), seriesKeys.get(i),
                            ((XYPlot) chartPanel.getChart().getPlot()).getRenderer(0).isSeriesVisible(
                            seriesKeyToSeriesNumber.get(seriesKeys.get(i)))
                        });
            }
        } else if (chartPanel.getChart().getPlot() instanceof CategoryPlot) {

            CategoryDataset categoryDataset = ((CategoryPlot) chartPanel.getChart().getPlot()).getDataset(0);

            List columnKeys = categoryDataset.getColumnKeys();
            seriesKeyToSeriesNumber = new HashMap<String, Integer>();

            // add the series keys to the table
            for (int i = 0; i < columnKeys.size(); i++) {

                ((DefaultTableModel) dataSeriesJXTable.getModel()).addRow(new Object[]{
                            new Integer((i + 1)), columnKeys.get(i),
                            true
                        });
                seriesKeyToSeriesNumber.put(columnKeys.get(i).toString(), i);
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        selectSeriesJPopupMenu = new javax.swing.JPopupMenu();
        selectAllSeriesJMenuItem = new javax.swing.JMenuItem();
        invertSelectionJMenuItem = new javax.swing.JMenuItem();
        highlightSelectionJMenu = new javax.swing.JMenu();
        selectHighlightedJMenuItem = new javax.swing.JMenuItem();
        deselectHighlightedJMenuItem = new javax.swing.JMenuItem();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        dataSeriesJXTable = new org.jdesktop.swingx.JXTable();
        okJButton = new javax.swing.JButton();
        cancelJButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();

        selectAllSeriesJMenuItem.setText("Select All");
        selectAllSeriesJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllSeriesJMenuItemActionPerformed(evt);
            }
        });
        selectSeriesJPopupMenu.add(selectAllSeriesJMenuItem);

        invertSelectionJMenuItem.setText("Invert Selection");
        invertSelectionJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invertSelectionJMenuItemActionPerformed(evt);
            }
        });
        selectSeriesJPopupMenu.add(invertSelectionJMenuItem);

        highlightSelectionJMenu.setText("Highlight Selection");

        selectHighlightedJMenuItem.setText("Select Highlighted");
        selectHighlightedJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectHighlightedJMenuItemActionPerformed(evt);
            }
        });
        highlightSelectionJMenu.add(selectHighlightedJMenuItem);

        deselectHighlightedJMenuItem.setText("Deselect Highlighted");
        deselectHighlightedJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deselectHighlightedJMenuItemActionPerformed(evt);
            }
        });
        highlightSelectionJMenu.add(deselectHighlightedJMenuItem);

        selectSeriesJPopupMenu.add(highlightSelectionJMenu);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Data Series Selection");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Data Series", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 11), new java.awt.Color(0, 0, 0))); // NOI18N

        dataSeriesJXTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Series Name", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        dataSeriesJXTable.setOpaque(false);
        dataSeriesJXTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                dataSeriesJXTableMouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(dataSeriesJXTable);

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 228, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE)
                .addContainerGap())
        );

        okJButton.setText("OK");
        okJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okJButtonActionPerformed(evt);
            }
        });

        cancelJButton.setText("Cancel");
        cancelJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeDialogJButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(okJButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cancelJButton))
                    .add(jSeparator1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(new java.awt.Component[] {cancelJButton, okJButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(18, 18, 18)
                .add(jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(cancelJButton)
                    .add(okJButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Updated the visible series selection in the plot and then
     * closes the dialog.
     *
     * @param evt
     */
    private void okJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okJButtonActionPerformed

        Collection tempMarkers = null;

        // get the markers
        if(chartPanel.getChart().getPlot() instanceof XYPlot){
            tempMarkers = ((XYPlot) chartPanel.getChart().getPlot()).getDomainMarkers(Layer.BACKGROUND);
        } else if(chartPanel.getChart().getPlot() instanceof CategoryPlot){
            tempMarkers = ((CategoryPlot) chartPanel.getChart().getPlot()).getDomainMarkers(Layer.BACKGROUND);
        }

        HashMap<String, Marker> markers = new HashMap<String, Marker>();

        if (tempMarkers != null) {
            Iterator markerIterator = tempMarkers.iterator();

            while (markerIterator.hasNext()) {
                Marker currentMarker = ((Marker) markerIterator.next());
                markers.put(currentMarker.getLabel(), currentMarker);
            }
        }

        int totalNumberOfFragmentIons = 0;
        boolean removalWarningGiven = false;
        boolean cancel = false;

        // read the contents of the table and update the data series selection
        for (int i = 0; i < dataSeriesJXTable.getRowCount() && !cancel; i++) {

            String currentSeriesKey = (String) dataSeriesJXTable.getValueAt(i, 1);
            boolean isCurrentlySelected = ((Boolean) dataSeriesJXTable.getValueAt(i, 2)).booleanValue();

            // update the data series selection
            if (chartPanel.getChart().getPlot() instanceof XYPlot) {

                // set the series visible or not visible
                ((XYPlot) chartPanel.getChart().getPlot()).getRenderer(0).setSeriesVisible(
                        seriesKeyToSeriesNumber.get(currentSeriesKey), isCurrentlySelected);

                // update the fragment ion number
                if(((XYPlot) chartPanel.getChart().getPlot()).getRenderer(0).isSeriesVisible(
                        seriesKeyToSeriesNumber.get(currentSeriesKey))){

                    // update the fragment ion number
                    // ToDo: find a way of doing this
//                    totalNumberOfFragmentIons += ((XYPlot) chartPanel.getChart().getPlot()).getDataset().getItemCount(
//                            seriesKeyToSeriesNumber.get(currentSeriesKey));
                }

            } else if (chartPanel.getChart().getPlot() instanceof CategoryPlot) {

                // update the data series selection
                // NB: for category plots this is a non-reverable process
                if(!isCurrentlySelected){

                    if(((CategoryPlot) chartPanel.getChart().getPlot()).getDataset() instanceof DefaultCategoryDataset){

                        if(!removalWarningGiven){
                            int value = JOptionPane.showConfirmDialog(this,
                                    "Removing data for this plotting type is irreversable. Continue?", "" +
                                    "Remove Category?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                            removalWarningGiven = true;

                            if(value != JOptionPane.YES_OPTION){
                                cancel = true;
                            }
                        }

                        if(!cancel){
                            ((DefaultCategoryDataset) ((CategoryPlot)
                                chartPanel.getChart().getPlot()).getDataset()).removeColumn(currentSeriesKey);
                        }
                    } else if(((CategoryPlot) chartPanel.getChart().getPlot()).getDataset() instanceof DefaultBoxAndWhiskerCategoryDataset){

                        if(!removalWarningGiven){
                            int value = JOptionPane.showConfirmDialog(this,
                                    "Removing data for this plotting type is irreversable. Continue?", "" +
                                    "Remove Category?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                            removalWarningGiven = true;

                            if(value != JOptionPane.YES_OPTION){
                                cancel = true;
                            }
                        }

                        if(!cancel){
                            ((DefaultBoxAndWhiskerCategoryDataset) ((CategoryPlot)
                                chartPanel.getChart().getPlot()).getDataset()).removeColumn(currentSeriesKey);
                        }
                    }
                } else {

                    // update the fragment ion number
                    // ToDo: find a way of doing this
//                    if(((XYPlot) chartPanel.getChart().getPlot()).getDataset() instanceof DefaultCategoryDataset){
//                        totalNumberOfFragmentIons += ((DefaultCategoryDataset) ((CategoryPlot)
//                            chartPanel.getChart().getPlot()).getDataset()).getRowCount();
//                    } else if(((XYPlot) chartPanel.getChart().getPlot()).getDataset() instanceof DefaultBoxAndWhiskerCategoryDataset){
//                        totalNumberOfFragmentIons += ((DefaultBoxAndWhiskerCategoryDataset) ((CategoryPlot)
//                            chartPanel.getChart().getPlot()).getDataset()).getRowCount();
//                    }
                }
            }

            // update the marker
            if (markers.get(currentSeriesKey) != null && fragmentationAnalyzer.getProperties().showMarkers()) {
                if (isCurrentlySelected) {
                    markers.get(currentSeriesKey).setAlpha(Properties.DEFAULT_VISIBLE_MARKER_ALPHA);
                } else {
                    markers.get(currentSeriesKey).setAlpha(Properties.DEFAULT_NON_VISIBLE_MARKER_ALPHA);
                }
            }
        }

        if(!cancel){

            chartPanel.getChart().fireChartChanged();

            String oldTitle = currentFrame.getTitle();

            if(totalNumberOfFragmentIons > 0){
                if(oldTitle.indexOf("|") != -1){
                    currentFrame.setTitle(oldTitle.substring(0, oldTitle.indexOf("|") + 2)
                            + totalNumberOfFragmentIons + " fragment ions"
                            + oldTitle.substring(oldTitle.indexOf(" fragment ions") + " fragment ions".length()));
                } else {
//                    currentFrame.setTitle(oldTitle.substring(0, oldTitle.length() - 1) + ", " + totalNumberOfFragmentIons +
//                    " fragment ions)");
                }
            } else {
                if(oldTitle.indexOf("|") != -1){
                    currentFrame.setTitle(oldTitle.substring(0, oldTitle.indexOf("|"))
                            + oldTitle.substring(oldTitle.indexOf(" fragment ions") + " fragment ions".length()));
                }
            }
        }
        
        closeDialogJButtonActionPerformed(null);

    }//GEN-LAST:event_okJButtonActionPerformed

    /**
     * Closes the dialog.
     *
     * @param evt
     */
    private void closeDialogJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeDialogJButtonActionPerformed
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_closeDialogJButtonActionPerformed

    /**
     * Opens the data series selection popup menu if a right click in the
     * table is detected.
     *
     * @param evt
     */
    private void dataSeriesJXTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_dataSeriesJXTableMouseReleased

        if (evt.getButton() == java.awt.event.MouseEvent.BUTTON3) {
            selectSeriesJPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_dataSeriesJXTableMouseReleased

    /**
     * Selects or deselects all the data series.
     *
     * @param evt
     */
    private void selectAllSeriesJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllSeriesJMenuItemActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        if (selectAllSeries) {
            for (int i = 0; i < dataSeriesJXTable.getRowCount(); i++) {
                dataSeriesJXTable.setValueAt(new Boolean(true), i, dataSeriesJXTable.getColumnCount() - 1);
            }
        } else {
            for (int i = 0; i < dataSeriesJXTable.getRowCount(); i++) {
                if ((((Boolean) dataSeriesJXTable.getValueAt(i, dataSeriesJXTable.getColumnCount() - 1)).booleanValue())) {
                    dataSeriesJXTable.setValueAt(new Boolean(false), i, dataSeriesJXTable.getColumnCount() - 1);
                }
            }
        }

        selectAllSeries = !selectAllSeries;
        
        if(selectAllSeries){
            selectAllSeriesJMenuItem.setText("Select All");
        } else {
            selectAllSeriesJMenuItem.setText("Deselect All");
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_selectAllSeriesJMenuItemActionPerformed

    /**
     * Inverts the current data series selection.
     *
     * @param evt
     */
    private void invertSelectionJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_invertSelectionJMenuItemActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        selectAllSeries = true;
        selectAllSeriesJMenuItem.setText("Select All");

        for (int i = 0; i < dataSeriesJXTable.getRowCount(); i++) {
            dataSeriesJXTable.setValueAt(
                    new Boolean(!((Boolean) dataSeriesJXTable.getValueAt(
                    i, dataSeriesJXTable.getColumnCount() - 1)).booleanValue()),
                    i, dataSeriesJXTable.getColumnCount() - 1);
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_invertSelectionJMenuItemActionPerformed

    /**
     * Selects all the higlighted rows in the search results table.
     *
     * @param evt
     */
    private void selectHighlightedJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectHighlightedJMenuItemActionPerformed
        selectHighlighted(true);
}//GEN-LAST:event_selectHighlightedJMenuItemActionPerformed

    /**
     * Deselects all the higlighted rows in the search results table.
     *
     * @param evt
     */
    private void deselectHighlightedJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deselectHighlightedJMenuItemActionPerformed
        selectHighlighted(false);
}//GEN-LAST:event_deselectHighlightedJMenuItemActionPerformed

    /**
     * Selects or deselects all the higlighted rows.
     *
     * @param select if true the rows are selected, if false the rows are deselected
     */
    private void selectHighlighted(boolean select) {
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        selectAllSeries = true;
        selectAllSeriesJMenuItem.setText("Select All");

        boolean columnWasSorted = false;
        int sortedTableColumn = -1;
        SortOrder sortOrder = null;

        if (dataSeriesJXTable.getSortedColumn() != null) {
            sortedTableColumn = dataSeriesJXTable.getSortedColumn().getModelIndex();
            sortOrder = dataSeriesJXTable.getSortOrder(sortedTableColumn);
            dataSeriesJXTable.setSortable(false);
            columnWasSorted = true;
        }

        int column = dataSeriesJXTable.getColumnCount() - 1;

        int[] selectedRows = dataSeriesJXTable.getSelectedRows();

        for (int i = 0; i < selectedRows.length; i++) {

            int currentRow = selectedRows[i];

            // select the row
            dataSeriesJXTable.setValueAt(new Boolean(select), currentRow, column);
        }

        if (columnWasSorted) {
            dataSeriesJXTable.setSortable(true);
            dataSeriesJXTable.setSortOrder(sortedTableColumn, sortOrder);
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelJButton;
    private org.jdesktop.swingx.JXTable dataSeriesJXTable;
    private javax.swing.JMenuItem deselectHighlightedJMenuItem;
    private javax.swing.JMenu highlightSelectionJMenu;
    private javax.swing.JMenuItem invertSelectionJMenuItem;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JButton okJButton;
    private javax.swing.JMenuItem selectAllSeriesJMenuItem;
    private javax.swing.JMenuItem selectHighlightedJMenuItem;
    private javax.swing.JPopupMenu selectSeriesJPopupMenu;
    // End of variables declaration//GEN-END:variables
}
