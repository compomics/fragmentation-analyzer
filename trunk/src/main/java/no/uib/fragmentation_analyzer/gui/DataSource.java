package no.uib.fragmentation_analyzer.gui;

import be.proteomics.lims.db.accessors.Instrument;
import be.proteomics.mascotdatfile.util.interfaces.MascotDatfileInf;
import be.proteomics.mascotdatfile.util.interfaces.QueryToPeptideMapInf;
import be.proteomics.mascotdatfile.util.mascot.Peak;
import be.proteomics.mascotdatfile.util.mascot.PeptideHit;
import be.proteomics.mascotdatfile.util.mascot.PeptideHitAnnotation;
import be.proteomics.mascotdatfile.util.mascot.Query;
import be.proteomics.mascotdatfile.util.mascot.enumeration.MascotDatfileType;
import be.proteomics.mascotdatfile.util.mascot.factory.MascotDatfileFactory;
import be.proteomics.mascotdatfile.util.mascot.iterator.QueryEnumerator;
import de.proteinms.omxparser.OmssaOmxFile;
import de.proteinms.omxparser.util.MSHitSet;
import de.proteinms.omxparser.util.MSHits;
import de.proteinms.omxparser.util.MSMZHit;
import de.proteinms.omxparser.util.MSModHit;
import de.proteinms.omxparser.util.MSSpectrum;
import de.proteinms.omxparser.util.OmssaModification;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import no.uib.fragmentation_analyzer.filefilters.DatFileFilter;
import no.uib.fragmentation_analyzer.filefilters.OmxFileFilter;
import no.uib.fragmentation_analyzer.util.RadioButtonEditor;
import no.uib.fragmentation_analyzer.util.RadioButtonRenderer;
import no.uib.fragmentation_analyzer.util.Util;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;

/**
 * A dialog where the user can import new data sets and load existing ones into the tool.
 *
 * @author Harald Barsnes
 */
public class DataSource extends javax.swing.JDialog implements ProgressDialogParent {

    private static JXTable dataSetsJXTable;
    private DefaultTableModel dataSetsModel = null;
    private FragmentationAnalyzer fragmentationAnalyzer;
    private static ProgressDialog progressDialog;
    private PreparedStatement ps;
    private ResultSet rs;
    private int progressCounter = 0;
    private static HashMap<Long, String> allInstruments;
    private static ArrayList<Long> allIdentificationIds;
    private static ArrayList<Long> spectrumfileids;
    private static HashMap<Long, String> spectraInstrumentMapping;
    private static HashMap<Long, Double> spectraTotalIntensityMapping;
    private final int NUMBER_OF_BYTES_PER_MEGABYTE = 1048576;
    private final double MAX_MASCOT_DAT_FILESIZE_BEFORE_INDEXING = 40; //in megabytes
    private ArrayList<File> selectedDataFiles;
    private static boolean cancelProgress = false;

    /**
     * Create a new DataSource and make it visible.
     *
     * @param fragmentationAnalyzer
     * @param modal
     */
    public DataSource(FragmentationAnalyzer fragmentationAnalyzer, boolean modal) {
        super(fragmentationAnalyzer, modal);

        this.fragmentationAnalyzer = fragmentationAnalyzer;

        initComponents();

        dataSetsJXTable = new JXTable(new DefaultTableModel()) {

            @Override
            public void tableChanged(TableModelEvent e) {
                super.tableChanged(e);
                repaint();
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return (column == 2);
            }

            @Override
            public Class getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return java.lang.Integer.class;
                }
                if (columnIndex == 1) {
                    return java.lang.String.class;
                } else {
                    return java.lang.Boolean.class;
                }
            }
        };

        dataSetsJXTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        dataSetsJXTable.addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {

                if (evt.getButton() == MouseEvent.BUTTON1) {
                    int column = dataSetsJXTable.columnAtPoint(evt.getPoint());
                    int row = dataSetsJXTable.rowAtPoint(evt.getPoint());

                    if (column == 2 && row != -1) {
                        openDataSetJButton.setEnabled(true);
                    }
                }
            }
        });

        dataSetsModel = new DefaultTableModel();
        dataSetsModel.setColumnIdentifiers(new Object[]{" ", "Data Set", "  "});
        dataSetsJXTable.setModel(dataSetsModel);

        ((JXTableHeader) dataSetsJXTable.getTableHeader()).setReorderingAllowed(false);

        dataSetsJXTable.getColumn("  ").setCellRenderer(new RadioButtonRenderer());
        dataSetsJXTable.getColumn("  ").setCellEditor(new RadioButtonEditor(new JCheckBox()));

        dataSetsJXTable.getColumn(" ").setMaxWidth(40);
        dataSetsJXTable.getColumn(" ").setMinWidth(40);
        dataSetsJXTable.getColumn("  ").setMaxWidth(30);
        dataSetsJXTable.getColumn("  ").setMinWidth(30);

        datasSetsJScrollPane.setViewportView(dataSetsJXTable);

        setLocationRelativeTo(fragmentationAnalyzer);
        insertAvailableDataSets(null);
        setVisible(true);
    }

    /**
     * Returns a reference to the main FragmentationAnalyzer frame that
     * dialogs can access.
     *
     * @return a reference to the main FragmentationAnalyzer frame
     */
    public FragmentationAnalyzer getFragmentationAnalyzer() {
        return fragmentationAnalyzer;
    }

    /**
     * Insert the available data sets into the data set table.
     */
    private void insertAvailableDataSets(String currentDataSet) {

        openDataSetJButton.setEnabled(false);

        while (dataSetsJXTable.getRowCount() > 0) {
            ((DefaultTableModel) dataSetsJXTable.getModel()).removeRow(0);
        }

        String path = "" + this.getClass().getProtectionDomain().getCodeSource().getLocation();
        path = path.substring(5, path.lastIndexOf("/"));
        path = path + "/DataSets";
        path = path.replace("%20", " ");

        File dataFolder = new File(path);

        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }

        // get all the files
        File[] tempDataSets = dataFolder.listFiles();

        // add the files to an array list and sort them
        ArrayList<File> dataSets = new ArrayList<File>();

        for (int i = 0; i < tempDataSets.length; i++) {
            dataSets.add(tempDataSets[i]);
        }

        java.util.Collections.sort(dataSets);

        int counter = 0;

        buttonGroup = new ButtonGroup();

        int selectedRow = 0;

        boolean selectDataSet = false;

        // iterate the files and add them to the table
        for (int i = 0; i < dataSets.size(); i++) {
            if (dataSets.get(i).isDirectory()) {
                File[] dataFiles = dataSets.get(i).listFiles();

                boolean identificationFileFound = false;
                boolean fragmentIonsFileOrMsLimsPropFileFound = false;

                for (int j = 0; j < dataFiles.length &&
                        !(identificationFileFound && fragmentIonsFileOrMsLimsPropFileFound); j++) {

                    String currentFileName = dataFiles[j].getName();

                    if (currentFileName.equalsIgnoreCase("identifications.txt")) {
                        identificationFileFound = true;
                    } else if (currentFileName.equalsIgnoreCase("fragmentIons.txt")) {
                        fragmentIonsFileOrMsLimsPropFileFound = true;
                    } else if (currentFileName.equalsIgnoreCase("ms_lims.prop")) {
                        fragmentIonsFileOrMsLimsPropFileFound = true;
                    }
                }

                if (identificationFileFound && fragmentIonsFileOrMsLimsPropFileFound) {

                    if (currentDataSet != null) {
                        if (currentDataSet.equalsIgnoreCase(dataSets.get(i).getName())) {
                            selectDataSet = true;
                            selectedRow = dataSetsModel.getRowCount();
                        }
                    }

                    JRadioButton tempRadioButton = new JRadioButton();
                    tempRadioButton.setSelected(selectDataSet);
                    tempRadioButton.setOpaque(true);

                    dataSetsModel.addRow(new Object[]{
                                ++counter,
                                dataSets.get(i).getName(),
                                tempRadioButton
                            });

                    buttonGroup.add((JRadioButton) dataSetsModel.getValueAt((counter - 1), 2));
                }
            }
        }

        dataSetsJXTable.setModel(dataSetsModel);

        if (selectDataSet) {
            openDataSetJButton.setEnabled(true);
        }

        // make sure the selected data set (if any) is visible in the table
        final int tempSelectedRow = selectedRow;
        final boolean tempSelectDataSet = selectDataSet;

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                dataSetsJXTable.scrollCellToVisible(tempSelectedRow, 0);

                if (tempSelectDataSet) {
                    dataSetsJXTable.setRowSelectionInterval(tempSelectedRow, tempSelectedRow);

                    int value = JOptionPane.showConfirmDialog(
                            null, "Data imported successfully.\nOpen data set?", "Open Data Set?",
                            JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

                    if (value == JOptionPane.YES_OPTION) {
                        openDataSetJButtonActionPerformed(null);
                    }
                }
            }
        });
    }

    public void cancelProgress() {
        cancelProgress = true;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup = new javax.swing.ButtonGroup();
        jXTaskPaneContainer1 = new org.jdesktop.swingx.JXTaskPaneContainer();
        jXTaskPane1 = new org.jdesktop.swingx.JXTaskPane();
        jXPanel1 = new org.jdesktop.swingx.JXPanel();
        ms_limsJRadioButton = new javax.swing.JRadioButton();
        mascotDatFilesJRadioButton = new javax.swing.JRadioButton();
        omssaJRadioButton = new javax.swing.JRadioButton();
        importJButton = new javax.swing.JButton();
        jXTaskPane2 = new org.jdesktop.swingx.JXTaskPane();
        jXPanel2 = new org.jdesktop.swingx.JXPanel();
        datasSetsJScrollPane = new javax.swing.JScrollPane();
        openDataSetJButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Select Data Set");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jXTaskPane1.setTitle("Import New Data Set");

        jXPanel1.setBackground(javax.swing.UIManager.getDefaults().getColor("tab_focus_fill_dark"));

        ms_limsJRadioButton.setBackground(javax.swing.UIManager.getDefaults().getColor("tab_focus_fill_dark"));
        buttonGroup.add(ms_limsJRadioButton);
        ms_limsJRadioButton.setText("Extract Data From ms_lims 7");
        ms_limsJRadioButton.setIconTextGap(30);
        ms_limsJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ms_limsJRadioButtonActionPerformed(evt);
            }
        });

        mascotDatFilesJRadioButton.setBackground(javax.swing.UIManager.getDefaults().getColor("tab_focus_fill_dark"));
        buttonGroup.add(mascotDatFilesJRadioButton);
        mascotDatFilesJRadioButton.setText("Extract Data From Mascot DAT Files");
        mascotDatFilesJRadioButton.setIconTextGap(30);
        mascotDatFilesJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mascotDatFilesJRadioButtonActionPerformed(evt);
            }
        });

        omssaJRadioButton.setBackground(javax.swing.UIManager.getDefaults().getColor("tab_focus_fill_dark"));
        buttonGroup.add(omssaJRadioButton);
        omssaJRadioButton.setText("Extract Data From OMSSA OMX Files");
        omssaJRadioButton.setIconTextGap(30);
        omssaJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                omssaJRadioButtonActionPerformed(evt);
            }
        });

        importJButton.setText("Import");
        importJButton.setEnabled(false);
        importJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importJButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jXPanel1Layout = new org.jdesktop.layout.GroupLayout(jXPanel1);
        jXPanel1.setLayout(jXPanel1Layout);
        jXPanel1Layout.setHorizontalGroup(
            jXPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jXPanel1Layout.createSequentialGroup()
                .add(jXPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jXPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(importJButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 379, Short.MAX_VALUE))
                    .add(jXPanel1Layout.createSequentialGroup()
                        .add(33, 33, 33)
                        .add(jXPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(ms_limsJRadioButton)
                            .add(mascotDatFilesJRadioButton)
                            .add(omssaJRadioButton))))
                .addContainerGap())
        );
        jXPanel1Layout.setVerticalGroup(
            jXPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jXPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(ms_limsJRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(mascotDatFilesJRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(omssaJRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 18, Short.MAX_VALUE)
                .add(importJButton))
        );

        jXTaskPane1.getContentPane().add(jXPanel1);

        jXTaskPaneContainer1.add(jXTaskPane1);

        jXTaskPane2.setTitle("Available Data Sets");

        jXPanel2.setBackground(javax.swing.UIManager.getDefaults().getColor("tab_focus_fill_dark"));

        openDataSetJButton.setText("Open Data Set");
        openDataSetJButton.setEnabled(false);
        openDataSetJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openDataSetJButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jXPanel2Layout = new org.jdesktop.layout.GroupLayout(jXPanel2);
        jXPanel2.setLayout(jXPanel2Layout);
        jXPanel2Layout.setHorizontalGroup(
            jXPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jXPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jXPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, datasSetsJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 379, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, openDataSetJButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 379, Short.MAX_VALUE))
                .addContainerGap())
        );
        jXPanel2Layout.setVerticalGroup(
            jXPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jXPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(datasSetsJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 134, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(openDataSetJButton))
        );

        jXTaskPane2.getContentPane().add(jXPanel2);

        jXTaskPaneContainer1.add(jXTaskPane2);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jXTaskPaneContainer1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 441, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jXTaskPaneContainer1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Enables or disables the import button.
     *
     * @param evt
     */
    private void ms_limsJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ms_limsJRadioButtonActionPerformed
        importJButton.setEnabled(ms_limsJRadioButton.isSelected() || mascotDatFilesJRadioButton.isSelected() || omssaJRadioButton.isSelected());
    }//GEN-LAST:event_ms_limsJRadioButtonActionPerformed

    /**
     * @see #ms_limsJRadioButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void mascotDatFilesJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mascotDatFilesJRadioButtonActionPerformed
        ms_limsJRadioButtonActionPerformed(null);
    }//GEN-LAST:event_mascotDatFilesJRadioButtonActionPerformed

    /**
     * @see #ms_limsJRadioButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void omssaJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_omssaJRadioButtonActionPerformed
        ms_limsJRadioButtonActionPerformed(null);
    }//GEN-LAST:event_omssaJRadioButtonActionPerformed

    /**
     * Closes the database connection (if any), and then closes the dialog.
     *
     * @param evt
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        fragmentationAnalyzer.closeDatabaseConnection();
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_formWindowClosing

    /**
     * Tries to import data of the selected type. The user provides information, like
     * the files to import etc.
     *
     * @param evt
     */
    private void importJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importJButtonActionPerformed

        cancelProgress = false;

        String path = "" + this.getClass().getProtectionDomain().getCodeSource().getLocation();
        path = path.substring(5, path.lastIndexOf("/"));
        path = path + "/DataSets";
        path = path.replace("%20", " ");


        new DataSetNameDialog(this, true);

        String currentDatasetName = fragmentationAnalyzer.getProperties().getCurrentDataSetName();

        if (currentDatasetName != null) {

            String newName;

            newName = path + "/" + currentDatasetName;

            while (currentDatasetName != null && new File(newName).exists()) {
                JOptionPane.showMessageDialog(this,
                        "The name is already in use.",
                        "Data Set Name", JOptionPane.ERROR_MESSAGE);
                new DataSetNameDialog(this, true);

                currentDatasetName = fragmentationAnalyzer.getProperties().getCurrentDataSetName();
                newName = path + "/" + currentDatasetName;
            }

            if (currentDatasetName != null) {

                String currentDatasetFolder = newName;

                fragmentationAnalyzer.getProperties().setCurrentDataSetFolder(currentDatasetFolder);
                fragmentationAnalyzer.getProperties().setCurrentDataSetName(currentDatasetName);

                // import data from ms_lims
                if (ms_limsJRadioButton.isSelected()) {
                    new DatabaseDialog(this, fragmentationAnalyzer, true, true);
                } else {

                    boolean folderCreated = new File(
                            fragmentationAnalyzer.getProperties().getCurrentDataSetFolder()).mkdir();

                    if (!folderCreated) {
                        JOptionPane.showMessageDialog(this,
                                "An error occured while creating the data set folder.\n" +
                                "See ../Properties/ErrorLog.txt for more details.",
                                "Error Creating Data Set Folder",
                                JOptionPane.ERROR_MESSAGE);
                        Util.writeToErrorLog("Creating Data Set Folder: Error while creating data set folder!");
                    } else {

                        selectedDataFiles = new ArrayList<File>();

                        JOptionPane.showMessageDialog(this,
                                "Select the file(s) to import.",
                                "File Selection", JOptionPane.INFORMATION_MESSAGE);

                        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

                        // get the selected files
                        JFileChooser chooser = new JFileChooser(
                                fragmentationAnalyzer.getUserProperties().getLastUsedFolder());
                        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                        chooser.setMultiSelectionEnabled(true);

                        if (mascotDatFilesJRadioButton.isSelected()) {
                            chooser.setFileFilter(new DatFileFilter());
                        } else if (omssaJRadioButton.isSelected()) {
                            chooser.setFileFilter(new OmxFileFilter());
                        }

                        int returnVal = chooser.showOpenDialog(this);

                        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                        if (returnVal == JFileChooser.APPROVE_OPTION) {

                            fragmentationAnalyzer.getUserProperties().setLastUsedFolder(
                                    chooser.getSelectedFile().getPath());

                            File[] selectedFiles = chooser.getSelectedFiles();

                            for (int j = 0; j < selectedFiles.length; j++) {

                                File currentFile = selectedFiles[j];

                                if (currentFile.isDirectory()) {

                                    File[] files = currentFile.listFiles();
                                    File tempFile;

                                    for (int i = 0; i < files.length; i++) {
                                        tempFile = files[i];

                                        if (chooser.accept(tempFile)) {
                                            selectedDataFiles.add(tempFile);
                                        }
                                    }
                                } else {
                                    selectedDataFiles.add(currentFile);
                                }
                            }


                            //boolean error = false;

                            // select the Mascot confidence level
                            // or get the OMSSA modification files
                            if (mascotDatFilesJRadioButton.isSelected()) {
                                new MascotConfidenceLevel(this, true);
                            } else if (omssaJRadioButton.isSelected()) {
                                cancelProgress = getOmssaModificationFiles();
                            }

                            // check if an incorrect confidence level was provided
                            // or the OMSSA modification files were not found
                            if (!cancelProgress) {

                                progressDialog = new ProgressDialog(this, this, true);

                                new Thread(new Runnable() {

                                    public void run() {
                                        progressDialog.setIntermidiate(true);
                                        progressDialog.setTitle("Importing Data. Please Wait...");
                                        progressDialog.setVisible(true);
                                    }
                                }, "ProgressDialog").start();

                                // Wait until progress dialog is visible.
                                //
                                // The following is not needed in Java 1.6, but seemed to be needed in 1.5.
                                //
                                // Not including the lines _used to_ result in a crash on Windows, but not anymore.
                                // Including the lines results in a crash on Linux and Mac.
                                if (System.getProperty("os.name").toLowerCase().lastIndexOf("windows") != -1) {
                                    while (!progressDialog.isVisible()) {
                                    }
                                }

                                new Thread("ImportThread") {

                                    @Override
                                    public void run() {

                                        try {
                                            FileWriter identificationWriter = new FileWriter(
                                                    fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/identifications.temp");
                                            BufferedWriter identificationsBufferedWriter =
                                                    new BufferedWriter(identificationWriter);

                                            FileWriter fragmentIonsWriter = new FileWriter(
                                                    fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/fragmentIons.txt");
                                            BufferedWriter fragmentIonsBufferedWriter =
                                                    new BufferedWriter(fragmentIonsWriter);

                                            int identificationsCounter = 0;
                                            int fragmentIonCounter = 0;

                                            // read the selected files, extract the required information
                                            // and write it to the identifications.txt file
                                            for (int i = 0; i < selectedDataFiles.size() && !cancelProgress; i++) {

                                                File currentFile = selectedDataFiles.get(i);

                                                progressDialog.setTitle("Reading File. Please Wait...");
                                                progressDialog.setValue(0);
                                                progressDialog.setIntermidiate(true);
                                                progressDialog.setString(currentFile.getName() + " (" + (i + 1) + "/" + selectedDataFiles.size() + ")");

                                                if (mascotDatFilesJRadioButton.isSelected()) {
                                                    identificationsCounter = parseMascotDatFile(
                                                            currentFile, identificationsCounter, fragmentIonCounter,
                                                            identificationsBufferedWriter, fragmentIonsBufferedWriter);
                                                } else if (omssaJRadioButton.isSelected()) {
                                                    identificationsCounter = parseOmssaOmxFile(
                                                            currentFile, identificationsCounter, fragmentIonCounter,
                                                            identificationsBufferedWriter, fragmentIonsBufferedWriter);
                                                }
                                            }

                                            // close the writers
                                            fragmentIonsBufferedWriter.close();
                                            fragmentIonsWriter.close();
                                            identificationsBufferedWriter.close();
                                            identificationWriter.close();

                                            progressDialog.setIntermidiate(true);
                                            progressDialog.setString("Adding Identification Counter. Please Wait...");

                                            // add the identification counter
                                            addIdentificationCounter(identificationsCounter);

                                            // delete the temp identifications file
                                            new File(fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/identifications.temp").delete();

                                        } catch (OutOfMemoryError error) {
                                            progressDialog.setVisible(false);
                                            progressDialog.dispose();
                                            Runtime.getRuntime().gc();
                                            JOptionPane.showMessageDialog(null,
                                                    "The task used up all the available memory and had to be stopped.\n" +
                                                    "Memory boundaries are set in ../Properties/JavaOptions.txt.",
                                                    "Out of Memory Error",
                                                    JOptionPane.ERROR_MESSAGE);
                                            Util.writeToErrorLog("FragmentationAnalyzer: Ran out of memory!");
                                            error.printStackTrace();
                                            System.exit(0);
                                        } catch (IOException e) {
                                            JOptionPane.showMessageDialog(null,
                                                    "Error when trying to import data from MS files. " +
                                                    "See ../Properties/ErrorLog.txt for more details.",
                                                    "Error Importing Files", JOptionPane.ERROR_MESSAGE);
                                            Util.writeToErrorLog("DataSource: ");
                                            e.printStackTrace();
                                        }

                                        if (!cancelProgress) {
                                            insertAvailableDataSets(fragmentationAnalyzer.getProperties().getCurrentDataSetName());
                                        } else {
                                            // delete the created project folder and close any open database connections
                                            Util.deleteDir(new File(fragmentationAnalyzer.getProperties().getCurrentDataSetFolder()));
                                            fragmentationAnalyzer.closeDatabaseConnection();
                                        }

                                        progressDialog.setVisible(false);
                                        progressDialog.dispose();
                                    }
                                }.start();
                            } else {
                                // delete the created project folder and close any open database connections
                                Util.deleteDir(new File(fragmentationAnalyzer.getProperties().getCurrentDataSetFolder()));
                                fragmentationAnalyzer.closeDatabaseConnection();
                            }
                        } else {
                            // delete the created project folder and close any open database connections
                            Util.deleteDir(new File(fragmentationAnalyzer.getProperties().getCurrentDataSetFolder()));
                            fragmentationAnalyzer.closeDatabaseConnection();
                        }
                    }
                }
            }
        }
    }//GEN-LAST:event_importJButtonActionPerformed

    /**
     * Extracts the OMSSA modification file details.
     *
     * Currently not in use.
     *
     * @return
     */
    private boolean getOmssaModificationFiles() {

        boolean error = false;

        JOptionPane.showMessageDialog(this,
                "Provide the OMSSA installation folder containing the details about \n" +
                "the peptide modifications (the mods.xml and the usermods.xml files).",
                "OMSSA Installation Folder", JOptionPane.INFORMATION_MESSAGE);

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        JFileChooser chooser = new JFileChooser(fragmentationAnalyzer.getUserProperties().getLastUsedFolder());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Locate The OMSSA Installation Folder");

        String path;

        int returnVal = chooser.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            path = (chooser.getSelectedFile().getAbsoluteFile().getPath());

            if (!path.endsWith(File.separator)) {
                path += File.separator;
            }

            if (!new File(path + "mods.xml").exists() || !new File(path + "usermods.xml").exists()) {

                JOptionPane.showMessageDialog(this,
                        "The selected folder does not contain \'mods.xml\' and \'usermods.xml\'.\n" +
                        "Extracting data from omx files can not be completed without these files.\n\n" +
                        "Please select the correct OMSSA installation folder.",
                        "Incorrect OMSSA Installation Folder",
                        JOptionPane.YES_NO_OPTION);

                fragmentationAnalyzer.getUserProperties().setLastUsedFolder(path);
                getOmssaModificationFiles();

            } else {
                error = Util.copyFile(new File(path + "mods.xml"), new File(
                        fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/mods.xml"));
                error = Util.copyFile(new File(path + "usermods.xml"), new File(
                        fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/usermods.xml"));
            }
        } else {
            error = true;
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        return error;
    }

    /**
     * Adds the total identification counter to the top of the identification file.
     * (Note that if the files become very big this method should perhaps be rewritten
     * to make it more efficient.)
     *
     * @param identificationsCounter
     * @throws IOException
     */
    private void addIdentificationCounter(int identificationsCounter) throws IOException {

        FileWriter idFileWriter = new FileWriter(
                fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/identifications.txt");
        BufferedWriter idBufferedWriter = new BufferedWriter(idFileWriter);

        FileReader r = new FileReader(
                fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/identifications.temp");
        BufferedReader br = new BufferedReader(r);

        idBufferedWriter.write(identificationsCounter + "\n");

        String currentLine = br.readLine();

        while (currentLine != null) {
            idBufferedWriter.write(currentLine + "\n");
            currentLine = br.readLine();
        }

        idBufferedWriter.close();
        idFileWriter.close();

        br.close();
        r.close();
    }

    /**
     * Parses an OMMSA file and writes the result to the provided buffered writers.
     *
     * @param currentOmssaOmxFile
     * @param identificationsCounter
     * @param fragmentIonCounter
     * @param identificationsBufferedWriter
     * @param fragmentIonsBufferedWriter
     * @return the number of identificatins in the file
     * @throws IOException
     * @throws OutOfMemoryError
     */
    private int parseOmssaOmxFile(File currentOmssaOmxFile, int identificationsCounter, int fragmentIonCounter,
            BufferedWriter identificationsBufferedWriter, BufferedWriter fragmentIonsBufferedWriter)
            throws IOException, OutOfMemoryError {

        progressDialog.setTitle("Parsing OMX File. Please Wait...");
        progressDialog.setIntermidiate(true);

        // parses the file
        OmssaOmxFile omssaOmxFile = new OmssaOmxFile(currentOmssaOmxFile.getPath(),
                fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/mods.xml",
                fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/usermods.xml");

        if (!cancelProgress) {

            // extract MSSearchSettings_msmstol
            double ionCoverageErrorMargin =
                    omssaOmxFile.getParserResult().MSSearch_request.MSRequest.get(0).MSRequest_settings.MSSearchSettings.MSSearchSettings_msmstol;

            // extract MSResponse_scale
            int omssaResponseScale =
                    omssaOmxFile.getParserResult().MSSearch_response.MSResponse.get(0).MSResponse_scale;

            // extract the ion types used
            List<Integer> usedIonTypes =
                    omssaOmxFile.getParserResult().MSSearch_request.MSRequest.get(0).MSRequest_settings.MSSearchSettings.MSSearchSettings_ionstosearch.MSIonType;


            // not in omx file has to be provided by the user
            String instrumentName = "(unknown)";

            String value = JOptionPane.showInputDialog(this,
                    "Provide the name of the instrument used for the file: \n" + currentOmssaOmxFile.getName());

            if (value != null) {
                instrumentName = value;
            }


            HashMap<MSSpectrum, MSHitSet> results = omssaOmxFile.getSpectrumToHitSetMap();
            Iterator<MSSpectrum> iterator = results.keySet().iterator();

            progressDialog.setIntermidiate(false);
            progressDialog.setMax(results.keySet().size());
            progressDialog.setValue(0);

            while (iterator.hasNext() && !cancelProgress) {

                progressDialog.setValue(progressCounter++);

                MSSpectrum currentSpectrum = iterator.next();


                // check if spectrum is identified
                if (results.get(currentSpectrum).MSHitSet_hits.MSHits.size() > 0) {

                    identificationsCounter++;

                    // find (and select) the MSHit with the lowest e-value
                    List<MSHits> allMSHits = results.get(currentSpectrum).MSHitSet_hits.MSHits;
                    Iterator<MSHits> msHitIterator = allMSHits.iterator();
                    double lowestEValue = Double.MAX_VALUE;
                    MSHits currentMSHit = null;

                    while (msHitIterator.hasNext()) {

                        MSHits tempMSHit = msHitIterator.next();

                        if (tempMSHit.MSHits_evalue < lowestEValue) {
                            lowestEValue = tempMSHit.MSHits_evalue;
                            currentMSHit = tempMSHit;
                        }
                    }


                    // extract the identification details
                    String peptideSequence = currentMSHit.MSHits_pepstring;
                    String modifiedSequence = getModifiedOmssaSequence(peptideSequence, omssaOmxFile, currentMSHit);
                    String precursorCharge = "" + currentSpectrum.MSSpectrum_charge.MSSpectrum_charge_E.get(0);

                    double precursorMz = ((double) currentSpectrum.MSSpectrum_precursormz) / omssaResponseScale;
                    double precursorIntensity = 0; // not provided


                    // extract and store as pkl file using identificationsCounter as name
                    File spectrumFolder = new File(
                            fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/spectra/");

                    if (!spectrumFolder.exists()) {
                        spectrumFolder.mkdir();
                    }

                    File spectrumFile = new File(
                            fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() +
                            "/spectra/" + identificationsCounter + ".pkl");

                    FileWriter spectrumFileWriter = new FileWriter(spectrumFile);
                    BufferedWriter spectrumFileBufferedWriter = new BufferedWriter(spectrumFileWriter);

                    spectrumFileBufferedWriter.write(precursorMz + "\t" +
                            precursorIntensity + "\t" + precursorCharge + "\n");

                    double totalIntensity = 0.0;

                    List<Integer> mzValues = currentSpectrum.MSSpectrum_mz.MSSpectrum_mz_E;
                    List<Integer> intensityValues = currentSpectrum.MSSpectrum_abundance.MSSpectrum_abundance_E;

                    for (int j = 0; j < mzValues.size() && !cancelProgress; j++) {
                        spectrumFileBufferedWriter.write(
                                (mzValues.get(j).doubleValue() / omssaResponseScale) + "\t" +
                                (intensityValues.get(j).doubleValue() / omssaResponseScale) + "\n");

                        totalIntensity += (intensityValues.get(j).doubleValue() / omssaResponseScale);
                    }

                    spectrumFileBufferedWriter.close();
                    spectrumFileWriter.close();


                    String spectrumFileName = "";

                    // TODO: check: possible with more than one spectrum file name..?
                    // note that spectrum file name is not mandatory
                    if (currentSpectrum.MSSpectrum_ids.MSSpectrum_ids_E.size() > 0) {
                        spectrumFileName = currentSpectrum.MSSpectrum_ids.MSSpectrum_ids_E.get(0) + "_";
                    }


                    // write the identification details to file
                    identificationsBufferedWriter.write(identificationsCounter + "\t" +
                            peptideSequence + "\t" +
                            modifiedSequence + "\t" +
                            precursorCharge + "\t" +
                            instrumentName + "\t" +
                            identificationsCounter + ".pkl\t" + // new file name
                            identificationsCounter + "\t" +
                            totalIntensity + "\t" +
                            currentOmssaOmxFile.getName() + "_" + // reference to the original spectrum
                            spectrumFileName +
                            currentSpectrum.MSSpectrum_number + "\n");


                    // get and store the fragment ions
                    Iterator<MSMZHit> mzHits = currentMSHit.MSHits_mzhits.MSMZHit.iterator();

                    while (mzHits.hasNext() && !cancelProgress) {
                        MSMZHit currentFragmentIon = mzHits.next();

                        int ionType = currentFragmentIon.MSMZHit_ion.MSIonType;

                        int msIonNeutralLossType = currentFragmentIon.MSMZHit_moreion.MSIon.MSIon_neutralloss.MSIonNeutralLoss;

                        String neturalLossTag = "";
                        String immoniumTag = "";

                        // -1 means no neutral loss reported
                        if (msIonNeutralLossType == -1) {
                            // check for immonium ions
                            // note: assumes that an immonium ion can not have a neutral loss
                            if (currentFragmentIon.MSMZHit_moreion.MSIon.MSIon_immonium.MSImmonium.MSImmonium_parent != null) {
                                immoniumTag = "i" + currentFragmentIon.MSMZHit_moreion.MSIon.MSIon_immonium.MSImmonium.MSImmonium_parent;
                            }
                        } else {
                            if (msIonNeutralLossType == 0) {
                                // water neutral loss
                                neturalLossTag = " -H2O";
                            } else if (msIonNeutralLossType == 1) {
                                // ammonia neutral loss
                                neturalLossTag = " -NH3";
                            }
                        }

                        int charge = currentFragmentIon.MSMZHit_charge;
                        int ionNumber = currentFragmentIon.MSMZHit_number + 1;

                        String chargeAsString = "";

                        // add the charge to the label if higher than 1
                        if (charge > 1) {
                            for (int i = 0; i < charge; i++) {
                                chargeAsString += "+";
                            }
                        }

                        String unusedIon = "";

//                    if (!usedIonTypes.contains(new Integer(ionType))) {
//                        unusedIon = "#";
//                    }

                        String ionNumberAsString = "" + ionNumber;

                        if (charge > 1) {
                            ionNumberAsString = "[" + ionNumber + "]";
                        }

                        String ionName = "";

                        if (ionType == 0) {
                            ionName = unusedIon + "a" + ionNumberAsString + chargeAsString + neturalLossTag;
                        } else if (ionType == 1) {
                            ionName = unusedIon + "b" + ionNumberAsString + chargeAsString + neturalLossTag;
                        } else if (ionType == 2) {
                            ionName = unusedIon + "c" + ionNumberAsString + chargeAsString + neturalLossTag;
                        } else if (ionType == 3) {
                            ionName = unusedIon + "x" + ionNumberAsString + chargeAsString + neturalLossTag;
                        } else if (ionType == 4) {
                            ionName = unusedIon + "y" + ionNumberAsString + chargeAsString + neturalLossTag;
                        } else if (ionType == 5) {
                            ionName = unusedIon + "z" + ionNumberAsString + chargeAsString + neturalLossTag;
                        } else if (ionType == 6) {
                            ionName = unusedIon + "Prec" + chargeAsString + neturalLossTag;
                        } else if (ionType == 7) {
                            ionName = unusedIon + "internal" + chargeAsString + neturalLossTag;
                        } else if (ionType == 8) {
                            ionName = unusedIon + immoniumTag /* + chargeAsString  + neturalLossTag*/;
                        } else if (ionType == 9) {
                            ionName = unusedIon + "unknown" + chargeAsString + neturalLossTag;
                        }


                        double fragmentIonIntensityScaled = -1;
                        double observedPeakMzValue = -1;
                        double fragmentIonMassError = -1;

//                    boolean error = extractFragmentIonIntensityFromPeak(tempMzHit, currentSpectrum,
//                            ionCoverageErrorMargin, omssaResponseScale,
//                            fragmentIonIntensityScaled, observedPeakMzValue, fragmentIonMassError);

                        boolean error = false;

                        // Now we have to map the reported fragment ion to its corresponding peak.
                        // Note that the values given in the OMSSA file are scaled.
                        int fragmentIonMzValueUnscaled = currentFragmentIon.MSMZHit_mz;

                        //System.out.println("fragmentIonMzValueUnscaled: " + fragmentIonMzValueUnscaled);

                        double currentIntensityScale = currentSpectrum.MSSpectrum_iscale;

                        // Iterate the peaks and find the values within the fragment ion error range.
                        // If more than one match, use the most intense.
                        for (int j = 0; j < mzValues.size() && !cancelProgress; j++) {

                            // check if the fragment ion is within the mass error range
                            if (Math.abs(mzValues.get(j) - fragmentIonMzValueUnscaled) <=
                                    (ionCoverageErrorMargin * omssaResponseScale)) {

                                // select this peak if it's the most intense peak within range
                                if ((intensityValues.get(j).doubleValue() / currentIntensityScale) > fragmentIonIntensityScaled) {
                                    fragmentIonIntensityScaled = intensityValues.get(j).doubleValue() / currentIntensityScale;

                                    // calculate the fragmet ion mass
                                    fragmentIonMassError = (mzValues.get(j).doubleValue() - fragmentIonMzValueUnscaled) / omssaResponseScale; // @TODO: or the other way around?? The order decides the sign.
                                    observedPeakMzValue = mzValues.get(j).doubleValue() / omssaResponseScale;
                                }
                            }
                        }

                        // check if any peaks in the spectrum matched the fragment ion
                        if (fragmentIonIntensityScaled == -1) {

                            JOptionPane.showMessageDialog(this,
                                    "Unable to map the fragment ion \'" +
                                    currentFragmentIon.MSMZHit_ion.MSIonType + " " + currentFragmentIon.MSMZHit_number + "\'. Ion not included in annotation.", "Unable To Map Fragment Ion",
                                    JOptionPane.INFORMATION_MESSAGE);
                            error = true;
                        }


                        if (!error) {
                            fragmentIonsBufferedWriter.write(
                                    ++fragmentIonCounter + "\t" +
                                    identificationsCounter + "\t" +
                                    ionName + "\t" +
                                    observedPeakMzValue + "\t" +
                                    fragmentIonIntensityScaled + "\t" +
                                    ionNumber + "\t" +
                                    fragmentIonMassError + "\n");
                        }
                    }
                }
            }
        }

        return identificationsCounter;
    }

    /**
     * Returns the modified OMSSA sequence, e.g., NH2-ARTM<Mox>HR-COOH, for the given identification.
     *
     * @param peptideSequence
     * @param omssaOmxFile
     * @param currentMSHit
     * @return the peptide sequence containing terminals and modifications.
     */
    private String getModifiedOmssaSequence(String peptideSequence, OmssaOmxFile omssaOmxFile, MSHits currentMSHit) {

        String[] modifications = new String[peptideSequence.length()];

        for (int i = 0; i < modifications.length; i++) {
            modifications[i] = "";
        }

        String modifiedSequence = "";
        String nTerminal = "";
        String cTerminal = "";

        // get the list of fixed modifications
        List<Integer> fixedModifications =
                omssaOmxFile.getParserResult().MSSearch_request.MSRequest.get(0).MSRequest_settings.MSSearchSettings.MSSearchSettings_fixed.MSMod;

        // handle modifications
        if (omssaOmxFile.getModifications().size() > 0) {

            if (fixedModifications.size() > 0) {

                for (int i = 0; i < fixedModifications.size(); i++) {

                    Vector<String> modifiedResidues =
                            omssaOmxFile.getModifications().get(fixedModifications.get(i)).getModResidues();

                    for (int j = 0; j < modifiedResidues.size(); j++) {

                        int index = peptideSequence.indexOf(modifiedResidues.get(j));

                        while (index != -1) {

                            modifications[index] +=
                                    "<" + omssaOmxFile.getModifications().get(fixedModifications.get(i)).getModNumber() + ">";

                            index = peptideSequence.indexOf(modifiedResidues.get(j), index + 1);
                        }
                    }
                }
            }

            // variable modifications
            Iterator<MSModHit> modsIterator = currentMSHit.MSHits_mods.MSModHit.iterator();

            while (modsIterator.hasNext()) {

                MSModHit currentMSModHit = modsIterator.next();

                modifications[currentMSModHit.MSModHit_site] +=
                        "<" + currentMSModHit.MSModHit_modtype.MSMod + ">";
            }

            // cycle through all the modifications and extract the modification type if possible
            for (int i = 0; i < modifications.length; i++) {

                // add the amino acid itself to the sequence
                modifiedSequence += peptideSequence.substring(i, i + 1);

                if (!modifications[i].equalsIgnoreCase("")) {

                    // have to check for multiple modifications on one residue
                    String[] residueMods = modifications[i].split(">");

                    for (int j = 0; j < residueMods.length; j++) {

                        String currentMod = residueMods[j] + ">";

                        OmssaModification tempOmssaModification = omssaOmxFile.getModifications().get(
                                new Integer(residueMods[j].substring(1)));

                        if (tempOmssaModification != null) {

                            if (tempOmssaModification.getModType() == OmssaModification.MODAA) {

                                // "normal" modification
                                modifiedSequence += currentMod;
                            } else if (tempOmssaModification.getModType() == OmssaModification.MODN ||
                                    tempOmssaModification.getModType() == OmssaModification.MODNAA ||
                                    tempOmssaModification.getModType() == OmssaModification.MODNP ||
                                    tempOmssaModification.getModType() == OmssaModification.MODNPAA) {

                                // n-terminal modification
                                nTerminal += currentMod;
                            } else if (tempOmssaModification.getModType() == OmssaModification.MODC ||
                                    tempOmssaModification.getModType() == OmssaModification.MODCAA ||
                                    tempOmssaModification.getModType() == OmssaModification.MODCP ||
                                    tempOmssaModification.getModType() == OmssaModification.MODCPAA) {

                                // c-terminal modification
                                cTerminal += currentMod;
                            }
                        } else {
                            modifiedSequence += currentMod;
                        }
                    }
                }
            }


            // set the n-terminal
            if (nTerminal.length() == 0) {
                nTerminal = "NH2-"; // no terminal (or terminal modification) given
            } else {
                nTerminal += "-"; // add the "-" at the end, i.e. "NH2-"
            }

            // set the c-terminal
            if (cTerminal.length() == 0) {
                cTerminal = "-COOH"; // no terminal (or terminal modification) given
            } else {
                cTerminal = "-" + cTerminal; // add the "-" at the beginning, i.e. "-COOH"
            }

            modifiedSequence = nTerminal + modifiedSequence + cTerminal;
        }

        return modifiedSequence;
    }

    /**
     * Parses a Mascot dat file and writes the result to the provided buffered writers.
     *
     * @param currentMascotDatFile
     * @param identificationsCounter
     * @param fragmentIonCounter
     * @param identificationsBufferedWriter
     * @param fragmentIonsBufferedWriter
     * @return the total number of identification in the file.
     * @throws IOException
     */
    private int parseMascotDatFile(File currentMascotDatFile, int identificationsCounter, int fragmentIonCounter,
            BufferedWriter identificationsBufferedWriter, BufferedWriter fragmentIonsBufferedWriter)
            throws IOException {

        MascotDatfileInf tempMascotDatfile;

        double size = (double) currentMascotDatFile.length() / NUMBER_OF_BYTES_PER_MEGABYTE;

        if (size > MAX_MASCOT_DAT_FILESIZE_BEFORE_INDEXING) {
            //if file is large
            tempMascotDatfile = MascotDatfileFactory.create(currentMascotDatFile.getPath(),
                    MascotDatfileType.INDEX);
        } else {
            tempMascotDatfile = MascotDatfileFactory.create(currentMascotDatFile.getPath(),
                    MascotDatfileType.MEMORY);
        }

        if (!cancelProgress) {

            String instrumentName = tempMascotDatfile.getParametersSection().getInstrument();

            QueryToPeptideMapInf queryToPeptideMap = tempMascotDatfile.getQueryToPeptideMap();
            QueryEnumerator queries = tempMascotDatfile.getQueryEnumerator();

            progressDialog.setTitle("Importing Data. Please Wait...");
            progressDialog.setIntermidiate(false);
            progressDialog.setValue(0);
            progressDialog.setMax(tempMascotDatfile.getNumberOfQueries());
            progressCounter = 1;

            while (queries.hasMoreElements() && !cancelProgress) {
                Query currentQuery = queries.nextElement();

                progressDialog.setValue(progressCounter++);

                PeptideHit tempPeptideHit = queryToPeptideMap.getPeptideHitOfOneQuery(currentQuery.getQueryNumber());

                if (tempPeptideHit != null) {

                    if (tempPeptideHit.scoresAboveIdentityThreshold(1 -
                            FragmentationAnalyzer.getUserProperties().getMascotConfidenceLevel())) {

                        // the spectrum is identified above the threshold
                        identificationsCounter++;

                        // extract the identification details
                        String peptideSequence = tempPeptideHit.getSequence();
                        String modifiedSequence = tempPeptideHit.getModifiedSequence();

                        String precursorCharge = currentQuery.getChargeString();
                        precursorCharge = precursorCharge.replaceFirst("\\+", "");
                        double precursorMz = currentQuery.getPrecursorMZ();
                        double precursorIntensity = currentQuery.getPrecursorIntensity();

                        // extract and store as pkl file using identificationsCounter as name
                        Peak[] peakList = currentQuery.getPeakList();

                        File spectrumFolder = new File(
                                fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/spectra/");

                        if (!spectrumFolder.exists()) {
                            spectrumFolder.mkdir();
                        }

                        File spectrumFile = new File(
                                fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() +
                                "/spectra/" + identificationsCounter + ".pkl");

                        FileWriter spectrumFileWriter = new FileWriter(spectrumFile);
                        BufferedWriter spectrumFileBufferedWriter = new BufferedWriter(spectrumFileWriter);

                        spectrumFileBufferedWriter.write(precursorMz + "\t" +
                                precursorIntensity + "\t" + precursorCharge + "\n");

                        double totalIntensity = 0.0;

                        for (int j = 0; j < peakList.length && !cancelProgress; j++) {
                            spectrumFileBufferedWriter.write(
                                    peakList[j].getMZ() + "\t" +
                                    peakList[j].getIntensity() + "\n");

                            totalIntensity += peakList[j].getIntensity();
                        }

                        spectrumFileBufferedWriter.close();
                        spectrumFileWriter.close();


                        // write the identification details to file
                        identificationsBufferedWriter.write(identificationsCounter + "\t" +
                                peptideSequence + "\t" +
                                modifiedSequence + "\t" +
                                precursorCharge + "\t" +
                                instrumentName + "\t" +
                                identificationsCounter + ".pkl\t" + // new file name
                                identificationsCounter + "\t" +
                                totalIntensity + "\t" +
                                currentMascotDatFile.getName() + "_" + // reference to the original spectrum
                                currentQuery.getFilename() + "_" +
                                currentQuery.getQueryNumber() + "\n");


                        // get and store the fragment ions

                        // get the peptide annotations from the file
                        PeptideHitAnnotation peptideHitAnnotations =
                                tempPeptideHit.getPeptideHitAnnotation(
                                tempMascotDatfile.getMasses(), tempMascotDatfile.getParametersSection(),
                                currentQuery.getPrecursorMZ(), currentQuery.getChargeString());

                        // get the fragment ions
                        Vector currentFragmentIons = peptideHitAnnotations.getFusedMatchedIons(
                                currentQuery.getPeakList(), tempPeptideHit.getPeaksUsedFromIons1(),
                                currentQuery.getMaxIntensity(), 0.05D);

                        // iterate the fragment ions
                        for (Object currentFragmentIon1 : currentFragmentIons) {

                            // Note: 'FragmentIon' is included in several projetcs so the complete path is required
                            be.proteomics.mascotdatfile.util.mascot.fragmentions.FragmentIonImpl currentFragmentIon =
                                    (be.proteomics.mascotdatfile.util.mascot.fragmentions.FragmentIonImpl) currentFragmentIon1;


                            // remove # or & in front of label
                            //
                            // From Mascot Dat File:
                            // Not Significant, Not Scoring has a '&' in front.
                            // Significant, Not Scoring has a '#' in front
                            String tempLabel = currentFragmentIon.getLabel();

                            if (tempLabel.startsWith("#") || tempLabel.startsWith("&")) {
                                tempLabel = tempLabel.substring(1);
                            }

                            fragmentIonsBufferedWriter.write(++fragmentIonCounter + "\t" +
                                    identificationsCounter + "\t" +
                                    tempLabel + "\t" +
                                    currentFragmentIon.getMZ() + "\t" +
                                    currentFragmentIon.getIntensity() + "\t" +
                                    currentFragmentIon.getNumber() + "\t" +
                                    currentFragmentIon.getTheoreticalExperimantalMassError() + "\n");
                        }
                    }
                }
            }
        }

        return identificationsCounter;
    }

    /**
     * Tries to load the selected data set into the tool. After loading the information in
     * the combo boxes (the instruments, the terminals etc) is updated.
     *
     * @param evt
     */
    private void openDataSetJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openDataSetJButtonActionPerformed

        cancelProgress = false;

        int selectedIndex = -1;

        for (int i = 0; i < dataSetsJXTable.getRowCount(); i++) {
            if (((JRadioButton) dataSetsJXTable.getValueAt(i, 2)).isSelected()) {
                selectedIndex = i;
            }
        }

        String currentDatasetName = (String) dataSetsJXTable.getValueAt(selectedIndex, 1);

        fragmentationAnalyzer.getProperties().setCurrentDataSetName(currentDatasetName);

        String path = "" + this.getClass().getProtectionDomain().getCodeSource().getLocation();
        path = path.substring(5, path.lastIndexOf("/"));
        path = path + "/DataSets";
        path = path.replace("%20", " ");

        String currentDatasetFolder = path + "/" + currentDatasetName;

        fragmentationAnalyzer.getProperties().setCurrentDataSetFolder(currentDatasetFolder);

        File dataSetFolder = new File(currentDatasetFolder);

        if (!dataSetFolder.exists()) {
            JOptionPane.showMessageDialog(this,
                    "An error occured when trying to open the data set.\n" +
                    "See /Properties/ErrorLog.txt for more details.",
                    "Error Opening Dataset", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error Opening Dataset: the folder " + currentDatasetFolder + " does not exist!");
        } else {

            // check if the data set is an ms_lims data set
            // if yes, establish an ms_lims connection
            if (new File(currentDatasetFolder + "/ms_lims.prop").exists()) {

                // read the contents of the ms_lims.prop file
                boolean noError = readMsLimsPropFile(currentDatasetFolder);

                if (noError) {
                    fragmentationAnalyzer.getProperties().setCurrentDataSetFolder(currentDatasetFolder);
                    this.setVisible(false);
                    fragmentationAnalyzer.loadDataSet(true);
                    this.dispose();
                }

            } else if (new File(currentDatasetFolder + "/mods.xml").exists()) {

                fragmentationAnalyzer.closeDatabaseConnection();

                // extracted from OMSSA
                fragmentationAnalyzer.getProperties().setCurrentDataSetFolder(currentDatasetFolder);
                this.setVisible(false);
                //massSpectrometryFragmentationAnalyzer.loadOmssaModificationFiles();
                fragmentationAnalyzer.loadDataSet(false);
                this.dispose();
            } else {

                fragmentationAnalyzer.closeDatabaseConnection();

                // extracted from Mascot Dat Files or created from scratch
                fragmentationAnalyzer.getProperties().setCurrentDataSetFolder(currentDatasetFolder);
                this.setVisible(false);
                this.dispose();
                fragmentationAnalyzer.loadDataSet(false);
            }
        }
    }//GEN-LAST:event_openDataSetJButtonActionPerformed

    /**
     * Reads the contents of the ms_lims properties file, then tells the user to log on
     * to the used database before continuing, and provided a dialog for doing this.
     *
     * @param currentDatasetFolder
     * @return false if an error occured, true otherwise
     */
    private boolean readMsLimsPropFile(String currentDatasetFolder) {

        boolean noError = true;

        try {
            FileReader f = new FileReader(currentDatasetFolder + "/ms_lims.prop");
            BufferedReader b = new BufferedReader(f);

            String s = b.readLine();

            String userName = s.substring(s.indexOf(": ") + 2);
            s = b.readLine();
            String serverHost = s.substring(s.indexOf(": ") + 2);
            s = b.readLine();
            String schema = s.substring(s.indexOf(": ") + 2);
            s = b.readLine();
            String date = s.substring(s.indexOf(": ") + 2);

            if (fragmentationAnalyzer.getUserProperties().getSchema().equalsIgnoreCase(schema) &&
                    fragmentationAnalyzer.getUserProperties().getServerHost().equalsIgnoreCase(serverHost) &&
                    fragmentationAnalyzer.getConnection() != null) {
                // do nothing, allready connected
            } else {

                fragmentationAnalyzer.closeDatabaseConnection();

                fragmentationAnalyzer.getUserProperties().setUserName(userName);
                fragmentationAnalyzer.getUserProperties().setServerHost(serverHost);
                fragmentationAnalyzer.getUserProperties().setSchema(schema);
                fragmentationAnalyzer.getUserProperties().saveUserPropertiesToFile();

                new DatabaseDialog(this, fragmentationAnalyzer, true, false);

                // check if connection was made
                if (fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() == null) {
                    noError = false;
                }
            }

            b.close();
            f.close();

        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this,
                    "An error occured when trying to open the ms_lims.prop file.\n" +
                    "See /Properties/ErrorLog.txt for more details.",
                    "Error Opening Dataset", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error opening ms_lims.prop file:");
            e.printStackTrace();
            noError = false;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "An error occured when trying to open the ms_lims.prop file.\n" +
                    "See /Properties/ErrorLog.txt for more details.",
                    "Error Opening Dataset", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error opening ms_lims.prop file:");
            e.printStackTrace();
            noError = false;
        }

        return noError;
    }

    /**
     * Tries to extract all the identifications from the ms_lims database.
     */
    public void extractIdentificationsFromDatabase() {

        progressDialog = new ProgressDialog(this, this, true);

        new Thread(new Runnable() {

            public void run() {
                progressDialog.setIntermidiate(false);
                progressDialog.setTitle("Importing Data. Please Wait...");
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        // Wait until progress dialog is visible.
        //
        // The following is not needed in Java 1.6, but seemed to be needed in 1.5.
        //
        // Not including the lines _used to_ result in a crash on Windows, but not anymore.
        // Including the lines results in a crash on Linux and Mac.
        if (System.getProperty("os.name").toLowerCase().lastIndexOf("windows") != -1) {
            while (!progressDialog.isVisible()) {
            }
        }

        new Thread("ExtractIdsThread") {

            @Override
            public void run() {

                //long start = System.currentTimeMillis();

                try {

                    // create the ms_lims properties file and store the ms_lims details
                    createMsLimsPropertiesFile();

                    // get the instrument mappings (instrumentid <-> instrument name)
                    allInstruments = new HashMap<Long, String>(20);
                    if (!cancelProgress) {
                        getInstrumentMappings();
                    }

                    //long temp = System.currentTimeMillis();
                    //System.out.println("Instrument Mapping: Milliseconds: " + (temp - start) + "\n");

                    // get the identifications (count: 1 542 998 per 18.06.09)
                    if (!cancelProgress) {
                        allIdentificationIds = new ArrayList<Long>(getIdentificationCount());
                        spectrumfileids = new ArrayList<Long>(getIdentificationCount());
                    }

                    int incorrectModifiedSequenceCounter =
                            getAllIdentifications(getMaxIdentificationId(), getIdentificationCount());

                    //System.out.println("incorrectModifiedSequenceCounter: " + incorrectModifiedSequenceCounter +
                    //        " correct: " + allIdentificationIds.size());

                    //long temp2 = System.currentTimeMillis();
                    //System.out.println("Identifications Extracted: Milliseconds: " + (temp2 - temp) + "\n");

                    // get the spectrum-instrument mappings and the total intensity (if available)
                    spectraInstrumentMapping = new HashMap<Long, String>(spectrumfileids.size());
                    spectraTotalIntensityMapping = new HashMap<Long, Double>();

                    if (!cancelProgress) {
                        getSpectrumInstrumentMappingsAndTotalIntensity();
                    }

                    //long temp3 = System.currentTimeMillis();
                    //System.out.println("Spectrum-Instrument Mapping Extracted: Milliseconds: " + (temp3 - temp2) + "\n");

                    // add the instrument and the total intensity (if available)
                    if (!cancelProgress) {
                        addInstrumentsAndTotalIntensityToIdentificationsFile();
                    }

                    //long temp4 = System.currentTimeMillis();
                    //System.out.println("Spectrum-Instrument Mapping Added: Milliseconds: " + (temp4 - temp3) + "\n");

                    // fragment ions (count: 41 122 567 per 18.06.09)
                    //extractFragmentIons();
                    //long temp5 = System.currentTimeMillis();
                    //System.out.println("Fragment Ions Extracted: Milliseconds: " + (temp5 - temp4) + "\n");

                    // print out the terminals, the modifications and the charges
                    //printTerminalsModsInstrumentsAndCharges();

                    if (!cancelProgress) {
                        insertAvailableDataSets(fragmentationAnalyzer.getProperties().getCurrentDataSetName());
                    } else {
                        // delete the created project folder and close any open database connections
                        Util.deleteDir(new File(fragmentationAnalyzer.getProperties().getCurrentDataSetFolder()));
                        fragmentationAnalyzer.closeDatabaseConnection();
                    }

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null,
                            "An error occured when extracting data from the database\n" +
                            "See the Properties/ErrorLog.txt file for more details.",
                            "Error Extracting Data", JOptionPane.ERROR_MESSAGE);
                    Util.writeToErrorLog("Error extracting data from database: ");
                    e.printStackTrace();

                    // delete the created project folder and close any open database connections
                    Util.deleteDir(new File(fragmentationAnalyzer.getProperties().getCurrentDataSetFolder()));
                    fragmentationAnalyzer.closeDatabaseConnection();
                }

                //long end = System.currentTimeMillis();
                //System.out.println("From Start to End: Milliseconds: " + (end - start) + "\n");

                progressDialog.setVisible(false);
                progressDialog.dispose();
            }
        }.start();
    }

    /**
     * Creates the ms_lims properties file.
     *
     * @throws IOException
     */
    private void createMsLimsPropertiesFile() throws IOException {

        FileWriter f = new FileWriter(fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/ms_lims.prop");
        BufferedWriter b = new BufferedWriter(f);

        b.write("Username: " + fragmentationAnalyzer.getUserProperties().getUserName() + "\n");
        b.write("ServerHost: " + fragmentationAnalyzer.getUserProperties().getServerHost() + "\n");
        b.write("Schema: " + fragmentationAnalyzer.getUserProperties().getSchema() + "\n");

        String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);

        b.write("Date: " + sdf.format(cal.getTime()));

        b.close();
        f.close();
    }

    /**
     * Returns the largest identification id in the database.
     *
     * @return the largest identification id in the database
     * @throws SQLException
     */
    private int getMaxIdentificationId() throws SQLException {

        ps = fragmentationAnalyzer.getConnection().prepareStatement(
                //"select count(*) from identification");
                "select max(identificationid) from identification;");
        rs = ps.executeQuery();
        rs.next();

        int highestIdentificationId = rs.getInt(1);
        //int highestIdentificationId = 10000;//rs.getInt(1);

        return highestIdentificationId;
    }

    /**
     * Returns the total number of identifications.
     *
     * @return the total number of identifications
     * @throws SQLException
     */
    private int getIdentificationCount() throws SQLException {

        ps = fragmentationAnalyzer.getConnection().prepareStatement(
                "select count(*) from identification");
        rs = ps.executeQuery();
        rs.next();

        int totalNumberOfIdentifications = rs.getInt(1);
        //int totalNumberOfIdentifications = 10000;

        return totalNumberOfIdentifications;
    }

    /**
     * Returns the total number of spectrum files.
     *
     * @return the total number of spectrum files
     * @throws SQLException
     */
    private int getSpectrumFileCount() throws SQLException {

        ps = fragmentationAnalyzer.getConnection().prepareStatement(
                "select count(*) from spectrumfile");
        rs = ps.executeQuery();
        rs.next();

        int totalNumberOfSpectrumFiles = rs.getInt(1);

        return totalNumberOfSpectrumFiles;
    }

    /**
     * Retrieve the instrument details, id vs name.
     *
     * @throws SQLException
     */
    private void getInstrumentMappings()
            throws SQLException {

        progressDialog.setTitle("Retrieving Instrument Details. Please Wait...");
        progressDialog.setIntermidiate(true);

        Instrument[] tempAllInstruments = Instrument.getAllInstruments(fragmentationAnalyzer.getConnection());

        for (int i = 0; i < tempAllInstruments.length; i++) {
            allInstruments.put(tempAllInstruments[i].getInstrumentid(), tempAllInstruments[i].getName());
        }
    }

    /**
     * Extract all the identifications from the database.
     *
     * @param highestIdentificationId
     * @param totalNumberOfIdentifications
     * @return the number of sequences where the modified sequence in the database is incorrect
     * @throws SQLException
     * @throws IOException
     */
    private int getAllIdentifications(int highestIdentificationId, int totalNumberOfIdentifications)
            throws SQLException, IOException {

        progressDialog.setIntermidiate(false);
        progressDialog.setValue(0);
        progressDialog.setMax(totalNumberOfIdentifications);
        progressDialog.setTitle("Retrieving Identifications. Please Wait...");
        progressCounter = 0;

        FileWriter f = new FileWriter(
                fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/identifications.temp");
        BufferedWriter b = new BufferedWriter(f);

        String modifiedSequence, undmodifiedSequence;

        int incorrectModifiedSequenceCounter = 0;

        ps = fragmentationAnalyzer.getConnection().prepareStatement(
                "select identificationid, l_spectrumfileid, " +
                "modified_sequence, " +
                "charge, sequence from identification " +
                "where (identificationid >= ? AND identificationid < ?)");

        int querrySize = 20000;

        for (int i = 1; i <= highestIdentificationId; i++) {

            ps.setInt(1, i);
            ps.setInt(2, (i + querrySize));
            rs = ps.executeQuery();
            ps.clearParameters();

            while (rs.next()) {

                progressDialog.setValue(progressCounter++);

                if (rs.getLong(2) != 0 && rs.getString(3).length() > 0) {

                    modifiedSequence = rs.getString(3);
                    undmodifiedSequence =
                            Util.extractUnmodifiedSequenceAndModifications(modifiedSequence, false, false,
                            fragmentationAnalyzer.getProperties());

                    if (rs.getString(5).equalsIgnoreCase(undmodifiedSequence)) {

                        b.write(rs.getLong(1) + "\t" + undmodifiedSequence + "\t" +
                                modifiedSequence + "\t" + rs.getInt(4) + "\t" + rs.getLong(2) + "\n");

                        allIdentificationIds.add(rs.getLong(1));
                        spectrumfileids.add(rs.getLong(2));
                    } else {
                        incorrectModifiedSequenceCounter++;
                    }
                }
            }

            i += (querrySize - 1);
        }

        b.close();
        f.close();

        return incorrectModifiedSequenceCounter;
    }

    /**
     * Get the spectrum vs instrument mappings. And the total intensity if available in database.
     *
     * @throws SQLException
     */
    private void getSpectrumInstrumentMappingsAndTotalIntensity() throws SQLException {

        progressDialog.setMax(spectrumfileids.size() * 2);
        progressDialog.setValue(0);
        progressDialog.setIntermidiate(false);
        progressDialog.setTitle("Retrieving Spectrum-Instrument Mappings. Please Wait...");
        progressCounter = 0;

        StringBuffer inClause;

        // can't use a prepared statment as the in clause is too long, and seems to
        // get cut by the insertString method
        Statement s = fragmentationAnalyzer.getConnection().createStatement();

        // verify if the database contains the total_spectrum_intensity column
        s.execute("show columns in spectrumfile where Field = 'total_spectrum_intensity'");
        rs = s.getResultSet();

        boolean totalIntensityColumnExists = false;

        if(rs.next()){
            totalIntensityColumnExists = true;
        }

        int querrySize = 10000;

        for (int i = 0; i < spectrumfileids.size(); i++) {

            inClause = new StringBuffer(spectrumfileids.size() * 8); // leaves room for average ids of length 7

            //progressDialog.setTitle("SI: Building in clause. Please Wait...");

            inClause.append(spectrumfileids.get(i));

            for (int j = (i + 1); j < (i + querrySize) && j < spectrumfileids.size(); j++) {
                progressDialog.setValue(progressCounter++);
                inClause.append("," + spectrumfileids.get(j));
            }

            //progressDialog.setTitle("SI: Executing query. Please Wait...");
            if(totalIntensityColumnExists){
                s.execute("select spectrumfileid, l_instrumentid, total_spectrum_intensity from spectrumfile where " +
                    "spectrumfileid in (" + inClause + ")");
            } else{
                s.execute("select spectrumfileid, l_instrumentid from spectrumfile where " +
                    "spectrumfileid in (" + inClause + ")");
            }
            

            rs = s.getResultSet();

            i += (querrySize - 1);

            //progressDialog.setTitle("Parsing result. Please Wait...");

            while (rs.next()) {
                progressDialog.setValue(progressCounter++);

                long spectrumfileid = rs.getLong(1);

                spectraInstrumentMapping.put(spectrumfileid, allInstruments.get(rs.getLong(2)));

                if(totalIntensityColumnExists){
                    spectraTotalIntensityMapping.put(spectrumfileid, rs.getDouble(3));
                }
            }
        }
    }

    /**
     * Add the instrument details and the total intensity (if available) to the identification file.
     *
     * @throws IOException
     * @throws SQLException
     */
    private void addInstrumentsAndTotalIntensityToIdentificationsFile() throws IOException, SQLException {

        progressDialog.setMax(allIdentificationIds.size());
        progressDialog.setValue(0);
        progressDialog.setIntermidiate(false);
        progressDialog.setTitle("Adding Instrument Details. Please Wait...");

        FileWriter w = new FileWriter(
                fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/identifications.txt");
        BufferedWriter bw = new BufferedWriter(w);

        FileReader r = new FileReader(
                fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/identifications.temp");
        BufferedReader b = new BufferedReader(r);

        String line = b.readLine();

        progressCounter = 0;

        String[] tokens;
        String instrumentName = "";

        // add the total identifications count
        bw.write(getIdentificationCount() + "\n");

        while (line != null) {

            progressCounter++;
            progressDialog.setValue(progressCounter);

            tokens = line.split("\t");

            // get the instrument name
            Long currentSpectrumId = new Long(tokens[4]);
            instrumentName = spectraInstrumentMapping.get(currentSpectrumId);

            // get the total intensity
            Double totalIntensity = spectraTotalIntensityMapping.get(currentSpectrumId);

            // write the extended identification to the file
            bw.write(tokens[0] + "\t" + tokens[1] + "\t" + tokens[2] + "\t" + tokens[3] + "\t" 
                    + instrumentName + "\t" + null + "\t" + tokens[4]);

            if(totalIntensity != null){
                bw.write("\t" + totalIntensity);
            }

            bw.write("\n");

            line = b.readLine();
        }

        r.close();
        b.close();
        bw.close();
        w.close();

        new File(fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/identifications.temp").delete();
    }

    /**
     * Extract the fragment ions from the ms_lims database.
     *
     * Note: currently not in use due to the massive number of rows in the table.
     *
     * @throws SQLException
     * @throws IOException
     */
    private void extractFragmentIons() throws SQLException, IOException {

        // NB: not complete, and should not be used due to it takes waaaay to long to extract 45 mill rows...

        progressDialog.setMax(allIdentificationIds.size());
        progressDialog.setValue(0);
        progressDialog.setIntermidiate(false);
        progressDialog.setTitle("Retrieving Fragment Ions. Please Wait...");

        progressCounter = 0;

        StringBuffer inClause = new StringBuffer(spectrumfileids.size() * 10);
        String inClauseAsString = "";

        FileWriter f = new FileWriter(
                fragmentationAnalyzer.getProperties().getCurrentDataSetFolder() + "/fragment_ions.txt");
        BufferedWriter b = new BufferedWriter(f);

        ps = fragmentationAnalyzer.getConnection().prepareStatement(
                "select fragmentionid, l_identificationid, mz, intensity, ionname, fragmentionnumber, massdelta " +
                "from fragmention where " +
                "l_identificationid in (?)");

        for (int i = 0; i < allIdentificationIds.size(); i++) {

            for (int j = i; j < (i + 10000) && j < allIdentificationIds.size(); j++) {
                progressDialog.setValue(progressCounter++);
                inClause.append(allIdentificationIds.get(j) + ",");
            }

            inClauseAsString = inClause.toString().substring(0, inClause.length() - 1);

            //progressDialog.setTitle("FI: Executing query. Please Wait...");

            ps.setString(1, inClauseAsString);
            rs = ps.executeQuery();
            ps.clearParameters();

            i += 9999;

            //progressDialog.setTitle("FI: Parsing result. Please Wait...");

            while (rs.next()) {
                b.write(rs.getLong(1) + "\t" + rs.getLong(2) + "\n");
                //progressDialog.setValue(progressCounter++);
            }
        }

        b.close();
        f.close();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup;
    private javax.swing.JScrollPane datasSetsJScrollPane;
    private javax.swing.JButton importJButton;
    private org.jdesktop.swingx.JXPanel jXPanel1;
    private org.jdesktop.swingx.JXPanel jXPanel2;
    private org.jdesktop.swingx.JXTaskPane jXTaskPane1;
    private org.jdesktop.swingx.JXTaskPane jXTaskPane2;
    private org.jdesktop.swingx.JXTaskPaneContainer jXTaskPaneContainer1;
    private javax.swing.JRadioButton mascotDatFilesJRadioButton;
    private javax.swing.JRadioButton ms_limsJRadioButton;
    private javax.swing.JRadioButton omssaJRadioButton;
    private javax.swing.JButton openDataSetJButton;
    // End of variables declaration//GEN-END:variables
}
