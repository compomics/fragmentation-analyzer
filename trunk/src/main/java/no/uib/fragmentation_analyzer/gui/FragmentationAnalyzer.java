package no.uib.fragmentation_analyzer.gui;

import com.compomics.mslims.db.accessors.Fragmention;
import com.compomics.mslims.db.accessors.Spectrum;
import com.compomics.mslims.db.accessors.Spectrum_file;
import com.compomics.mslims.util.fileio.MascotGenericFile;
import com.compomics.util.gui.events.RescalingEvent;
import com.compomics.util.gui.interfaces.SpectrumPanelListener;
import com.compomics.util.gui.spectrum.DefaultSpectrumAnnotation;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.Options;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.SkyKrupp;
import com.mysql.jdbc.Driver;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.table.DefaultTableModel;
import no.uib.fragmentation_analyzer.util.AlignedListCellRenderer;
import no.uib.fragmentation_analyzer.util.FragmentIon;
import no.uib.fragmentation_analyzer.util.IdentificationTableRow;
import no.uib.fragmentation_analyzer.util.ImageType;
import no.uib.fragmentation_analyzer.util.PKLFile;
import no.uib.fragmentation_analyzer.util.PlotUtil;
import no.uib.fragmentation_analyzer.util.Properties;
import no.uib.fragmentation_analyzer.util.ReducedIdentification;
import no.uib.fragmentation_analyzer.util.SpectrumTableRow;
import no.uib.fragmentation_analyzer.util.UserProperties;
import no.uib.fragmentation_analyzer.util.Util;
import no.uib.fragmentation_analyzer.util.XYZDataPoint;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import org.apache.batik.transcoder.TranscoderException;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.SortOrder;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleEdge;

/**
 * FragmentationAnalyzer is the main screen in the FragmentationAnalyzer tool.
 *
 * @author Harald Barsnes
 */
public class FragmentationAnalyzer extends javax.swing.JFrame implements ProgressDialogParent {

    private static boolean debug = false;
    //private static boolean writeToFile = false;
    private static boolean useErrorLog = true;
    private static Connection conn = null;
    private static String analyzerName = "FragmentationAnalyzer";
    private static UserProperties userProperties;
    private static Properties properties;
    private static ProgressDialog progressDialog;
    private Vector resultsColumnToolTips, spectraColumnToolTips;
    private static boolean dataLoaded = false;
    private static Dimension plotPaneCurrentPreferredSize;
    private static int plotPaneCurrentScrollValue = 0;
    private static boolean internalFrameBeingResized = false, internalFrameIsMaximized = false;
    private static boolean updateScrollValue = true;
    private static boolean currentDataSetIsFromMsLims;
    private int internalFrameUniqueIdCounter = 0;
    private boolean cancelProgress = false, searchEnabled = false;
    private String searchResultAnalysisButtonDisabledToolTip = "Select at least one row in the Search Results table";
    private String spectraAnalysisButtonDisabledToolTip = "Select at least one row in the Individual Spectra table";
    private boolean initialSizeHasBeenSet = false;

    /**
     * Creates a new FragmentationAnalyzer frame and makes it visible. Then opens
     * a DataSource dialog where the user can select the data set to analyze.
     */
    public FragmentationAnalyzer() {
        initComponents();

        this.setTitle(analyzerName + " " + properties.getVersion());

        // make the interal frame tool bars invisible
        spectrumPanelToolBarJInternalFrame.setVisible(false);
        boxPlotPanelToolBarJInternalFrame.setVisible(false);

        plotsAndAnalysesJScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {

            public void adjustmentValueChanged(AdjustmentEvent e) {

                // this code moved the toolbars relative to the scrollpane to make
                // sure that they are always visible
                if (!e.getValueIsAdjusting()) {
                    spectrumPanelToolBarJInternalFrame.setBounds(
                            (int) spectrumPanelToolBarJInternalFrame.getBounds().getX(),
                            40 + e.getValue(),
                            (int) spectrumPanelToolBarJInternalFrame.getBounds().getWidth(),
                            (int) spectrumPanelToolBarJInternalFrame.getBounds().getHeight());

                    boxPlotPanelToolBarJInternalFrame.setBounds(
                            (int) boxPlotPanelToolBarJInternalFrame.getBounds().getX(),
                            40 + e.getValue(),
                            (int) boxPlotPanelToolBarJInternalFrame.getBounds().getWidth(),
                            (int) boxPlotPanelToolBarJInternalFrame.getBounds().getHeight());
                }
            }
        });

        jMenuBar.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.SINGLE);
        jMenuBar.putClientProperty(Options.NO_ICONS_KEY, Boolean.TRUE);

        // central allignment for the text in all combo boxes
        centrallyAllignComboBoxes();

        // the following lines are needed to make sure that the frame looks ok when
        // the scrollbars are showing (which changes the frame somewhat)
        while (searchSettingsJScrollPane.getViewport().getViewRect().height <
                searchSettingsJXTaskPaneContainer.getMinimumSize().getHeight()) {
            this.setPreferredSize(new Dimension(this.getWidth(), this.getHeight() + 1));
            pack();
        }

        while (resultsJScrollPane.getViewport().getViewRect().height <
                resultsJXTaskPaneContainer.getMinimumSize().getHeight()) {
            this.setPreferredSize(new Dimension(this.getWidth(), this.getHeight() + 1));
            pack();
        }


        // make sure the main frame is not too wide for the screen
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        if (dim.getWidth() < this.getWidth()) {
            this.setPreferredSize(new Dimension(dim.width - 50, this.getPreferredSize().height));
            pack();
        }


        setColumnProperties();

        searchResultsJXTaskPane.setExpanded(false);
        plotsAnalysesJXTaskPane.setExpanded(false);
        spectraJXTaskPane.setExpanded(false);

        userProperties = new UserProperties(properties.getVersion());
        userProperties.readUserPropertiesFromFile(null);

        // sets the icon of the frame
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().
                getResource("/no/uib/fragmentation_analyzer/icons/box_plot_small.GIF")));

        setLocationRelativeTo(null);
        setExtendedState(Frame.MAXIMIZED_BOTH);
        setVisible(true);

        initialSizeHasBeenSet = true;

        plotPaneCurrentPreferredSize = plotsAndAnalysesJScrollPane.getMinimumSize();

        new DataSource(this, true);
    }

    /**
     * Centrally align the text in all the combo boxes.
     */
    private void centrallyAllignComboBoxes() {
        instrument1JComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        instrument2JComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        instrument3JComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        nTermJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        cTermJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        chargeJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        modification1JComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        modification2JComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        modification3JComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        searchResultsJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        spectraJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        daOrPpmSearchResultsJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        combineSearchResultsJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        daOrPpmSpectraJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        combineSpectraJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
    }

    /**
     * Sets the column size and creates the column tooltips.
     */
    private void setColumnProperties() {

        searchResultsJXTable.getTableHeader().setReorderingAllowed(false);
        spectraJXTable.getTableHeader().setReorderingAllowed(false);

        searchResultsJXTable.getColumn(" ").setMaxWidth(60);
        searchResultsJXTable.getColumn(" ").setMinWidth(60);
        searchResultsJXTable.getColumn("Length").setMaxWidth(80);
        searchResultsJXTable.getColumn("Length").setMinWidth(80);
        searchResultsJXTable.getColumn("#1").setMaxWidth(80);
        searchResultsJXTable.getColumn("#1").setMinWidth(80);
        searchResultsJXTable.getColumn("#2").setMaxWidth(80);
        searchResultsJXTable.getColumn("#2").setMinWidth(80);
        searchResultsJXTable.getColumn("  ").setMaxWidth(30);
        searchResultsJXTable.getColumn("  ").setMinWidth(30);

        spectraJXTable.getColumn(" ").setMaxWidth(60);
        spectraJXTable.getColumn(" ").setMinWidth(60);
        spectraJXTable.getColumn("ID").setMaxWidth(60);
        spectraJXTable.getColumn("ID").setMinWidth(60);
        spectraJXTable.getColumn("SID").setMaxWidth(60);
        spectraJXTable.getColumn("SID").setMinWidth(60);
        spectraJXTable.getColumn("Length").setMaxWidth(80);
        spectraJXTable.getColumn("Length").setMinWidth(80);
        spectraJXTable.getColumn("  ").setMaxWidth(30);
        spectraJXTable.getColumn("  ").setMinWidth(30);

        resultsColumnToolTips = new Vector();
        resultsColumnToolTips.add(null);
        resultsColumnToolTips.add("Identified Peptide Sequence");
        resultsColumnToolTips.add("Identified Peptide Sequence with Modifications and Terminals");
        resultsColumnToolTips.add("Peptide Length");
        resultsColumnToolTips.add("Number of Unmodified Matches");
        resultsColumnToolTips.add("Number of Modified Matches");
        resultsColumnToolTips.add(null);

        spectraColumnToolTips = new Vector();
        spectraColumnToolTips.add(null);
        spectraColumnToolTips.add("Identification Number of the Identification");
        spectraColumnToolTips.add("Identification Number of the Spectrum");
        spectraColumnToolTips.add("Identified Peptide Sequence");
        spectraColumnToolTips.add("Identified Peptide Sequence with Modifications and Terminals");
        spectraColumnToolTips.add("Peptide Length");
        spectraColumnToolTips.add("Instrument Used");
        spectraColumnToolTips.add(null);

        // cell renderers
        searchResultsJXTable.getColumn("Length").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, Color.gray));
        searchResultsJXTable.getColumn("#1").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, Color.gray));
        searchResultsJXTable.getColumn("#2").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, Color.gray));

        ((JSparklinesBarChartTableCellRenderer) searchResultsJXTable.getColumn("Length").getCellRenderer()).showNumberAndChart(true, 40);
        ((JSparklinesBarChartTableCellRenderer) searchResultsJXTable.getColumn("#1").getCellRenderer()).showNumberAndChart(true, 40);
        ((JSparklinesBarChartTableCellRenderer) searchResultsJXTable.getColumn("#2").getCellRenderer()).showNumberAndChart(true, 40);

        spectraJXTable.getColumn("Length").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, Color.gray));
        ((JSparklinesBarChartTableCellRenderer) spectraJXTable.getColumn("Length").getCellRenderer()).showNumberAndChart(true, 40);
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

        searchButtonGroup = new javax.swing.ButtonGroup();
        internalFramesJPopupMenu = new javax.swing.JPopupMenu();
        showSpectrumToolBarJMenuItem = new javax.swing.JMenuItem();
        showBoxPlotToolBarJMenuItem = new javax.swing.JMenuItem();
        showDataSeriesSelectionJMenuItem = new javax.swing.JMenuItem();
        setTitleJMenuItem = new javax.swing.JMenuItem();
        duplicatePlotJMenuItem = new javax.swing.JMenuItem();
        exportJMenu = new javax.swing.JMenu();
        exportAsSvgJMenuItem = new javax.swing.JMenuItem();
        exportAsPdfJMenuItem = new javax.swing.JMenuItem();
        exportAsTiffJMenuItem = new javax.swing.JMenuItem();
        exportAsJpgJMenuItem = new javax.swing.JMenuItem();
        exportAsPngJMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        showLegendsJMenuItem = new javax.swing.JMenuItem();
        showMarkersJMenuItem = new javax.swing.JMenuItem();
        showAverageJMenuItem = new javax.swing.JMenuItem();
        showMaxMinJMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        closeAllInternalFramesJMenuItem = new javax.swing.JMenuItem();
        selectIdentificationsJPopupMenu = new javax.swing.JPopupMenu();
        selectAllIdentificationsJMenuItem = new javax.swing.JMenuItem();
        invertSelectionIdentificationsJMenuItem = new javax.swing.JMenuItem();
        highlightIdentificationsJMenu = new javax.swing.JMenu();
        selectHighlightedIdentificationsJMenuItem = new javax.swing.JMenuItem();
        deselectHighlightedIdentificationsJMenuItem = new javax.swing.JMenuItem();
        peptideLengthJMenuItem = new javax.swing.JMenuItem();
        selectSpectraJPopupMenu = new javax.swing.JPopupMenu();
        selectAllSpectrtaJMenuItem = new javax.swing.JMenuItem();
        invertSelectionSpectraJMenuItem = new javax.swing.JMenuItem();
        highlightSelectionSpectraJMenu = new javax.swing.JMenu();
        selectHighlightedSpectraJMenuItem = new javax.swing.JMenuItem();
        deselectHighlightedSpectraJMenuItem = new javax.swing.JMenuItem();
        mainJSplitPane = new javax.swing.JSplitPane();
        searchSettingsJScrollPane = new javax.swing.JScrollPane();
        searchSettingsJXTaskPaneContainer = new org.jdesktop.swingx.JXTaskPaneContainer();
        instrumentJXTaskPane = new org.jdesktop.swingx.JXTaskPane();
        jXPanel7 = new org.jdesktop.swingx.JXPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        instrument1JComboBox = new javax.swing.JComboBox();
        instrument2JComboBox = new javax.swing.JComboBox();
        instrument3JComboBox = new javax.swing.JComboBox();
        terminalsAndChargeJXTaskPane = new org.jdesktop.swingx.JXTaskPane();
        jXPanel2 = new org.jdesktop.swingx.JXPanel();
        jLabel17 = new javax.swing.JLabel();
        nTermJComboBox = new javax.swing.JComboBox();
        jLabel22 = new javax.swing.JLabel();
        cTermJComboBox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        chargeJComboBox = new javax.swing.JComboBox();
        modificationsJXTaskPane = new org.jdesktop.swingx.JXTaskPane();
        jXPanel3 = new org.jdesktop.swingx.JXPanel();
        jLabel5 = new javax.swing.JLabel();
        modification1JComboBox = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        modification2JComboBox = new javax.swing.JComboBox();
        jLabel7 = new javax.swing.JLabel();
        modification3JComboBox = new javax.swing.JComboBox();
        searchTypeJXPanel = new org.jdesktop.swingx.JXPanel();
        generalSearchJRadioButton = new javax.swing.JRadioButton();
        modificationSearchJRadioButton = new javax.swing.JRadioButton();
        searchJButton = new javax.swing.JButton();
        resultsJScrollPane = new javax.swing.JScrollPane();
        resultsJXTaskPaneContainer = new org.jdesktop.swingx.JXTaskPaneContainer();
        searchResultsJXTaskPane = new org.jdesktop.swingx.JXTaskPane();
        searchResultJXPanel = new org.jdesktop.swingx.JXPanel();
        searchResultsJButton = new javax.swing.JButton();
        searchResultsJComboBox = new javax.swing.JComboBox();
        searchResultsJScrollPane = new javax.swing.JScrollPane();
        searchResultsJXTable = new JXTable() {
            protected JXTableHeader createDefaultTableHeader() {
                return new JXTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) resultsColumnToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        daOrPpmSearchResultsJComboBox = new javax.swing.JComboBox();
        combineSearchResultsJComboBox = new javax.swing.JComboBox();
        spectraJXTaskPane = new org.jdesktop.swingx.JXTaskPane();
        spectraJXPanel = new org.jdesktop.swingx.JXPanel();
        spectraJButton = new javax.swing.JButton();
        spectraJComboBox = new javax.swing.JComboBox();
        spectraJScrollPane = new javax.swing.JScrollPane();
        spectraJXTable = new JXTable() {
            protected JXTableHeader createDefaultTableHeader() {
                return new JXTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) spectraColumnToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        daOrPpmSpectraJComboBox = new javax.swing.JComboBox();
        combineSpectraJComboBox = new javax.swing.JComboBox();
        plotsAnalysesJXTaskPane = new org.jdesktop.swingx.JXTaskPane();
        plotsAndAnalysesJScrollPane = new javax.swing.JScrollPane();
        plotsAndAnalysesJDesktopPane = new javax.swing.JDesktopPane();
        spectrumPanelToolBarJInternalFrame = new javax.swing.JInternalFrame();
        jPanel5 = new javax.swing.JPanel();
        jSeparator3 = new javax.swing.JSeparator();
        yIonsJCheckBox = new javax.swing.JCheckBox();
        xIonsJCheckBox = new javax.swing.JCheckBox();
        zIonsJCheckBox = new javax.swing.JCheckBox();
        chargeOneJCheckBox = new javax.swing.JCheckBox();
        chargeTwoJCheckBox = new javax.swing.JCheckBox();
        chargeOverTwoJCheckBox = new javax.swing.JCheckBox();
        jSeparator5 = new javax.swing.JSeparator();
        cIonsJCheckBox = new javax.swing.JCheckBox();
        bIonsJCheckBox = new javax.swing.JCheckBox();
        aIonsJCheckBox = new javax.swing.JCheckBox();
        otherIonsJCheckBox = new javax.swing.JCheckBox();
        jSeparator6 = new javax.swing.JSeparator();
        H2OIonsJCheckBox = new javax.swing.JCheckBox();
        NH3IonsJCheckBox = new javax.swing.JCheckBox();
        boxPlotPanelToolBarJInternalFrame = new javax.swing.JInternalFrame();
        jPanel6 = new javax.swing.JPanel();
        jSeparator4 = new javax.swing.JSeparator();
        bIonsUnmodifiedJCheckBox = new javax.swing.JCheckBox();
        bIonsModifiedJCheckBox = new javax.swing.JCheckBox();
        yIonsUnmodifiedJCheckBox = new javax.swing.JCheckBox();
        yIonsModifiedJCheckBox = new javax.swing.JCheckBox();
        bIonsBoxPlotJCheckBox = new javax.swing.JCheckBox();
        yIonsBoxPlotJCheckBox = new javax.swing.JCheckBox();
        jSeparator7 = new javax.swing.JSeparator();
        internalFrameTipJLabel = new javax.swing.JLabel();
        jMenuBar = new javax.swing.JMenuBar();
        fileJMenu = new javax.swing.JMenu();
        opemJMenuItem = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JSeparator();
        exitJMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        preferencesJMenuItem = new javax.swing.JMenuItem();
        windowJMenu = new javax.swing.JMenu();
        showLeftPanelJMenuItem = new javax.swing.JMenuItem();
        helpJMenu = new javax.swing.JMenu();
        helpJMenuItem = new javax.swing.JMenuItem();
        aboutJMenuItem = new javax.swing.JMenuItem();

        showSpectrumToolBarJMenuItem.setText("Show Spectrum Tool Bar");
        showSpectrumToolBarJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showSpectrumToolBarJMenuItemActionPerformed(evt);
            }
        });
        internalFramesJPopupMenu.add(showSpectrumToolBarJMenuItem);

        showBoxPlotToolBarJMenuItem.setText("Show Box Plot Tool Bar");
        showBoxPlotToolBarJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showBoxPlotToolBarJMenuItemActionPerformed(evt);
            }
        });
        internalFramesJPopupMenu.add(showBoxPlotToolBarJMenuItem);

        showDataSeriesSelectionJMenuItem.setText("Data Series Selection");
        showDataSeriesSelectionJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showDataSeriesSelectionJMenuItemActionPerformed(evt);
            }
        });
        internalFramesJPopupMenu.add(showDataSeriesSelectionJMenuItem);

        setTitleJMenuItem.setText("Edit Plot Title");
        setTitleJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setTitleJMenuItemActionPerformed(evt);
            }
        });
        internalFramesJPopupMenu.add(setTitleJMenuItem);

        duplicatePlotJMenuItem.setText("Duplicate Plot");
        duplicatePlotJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                duplicatePlotJMenuItemActionPerformed(evt);
            }
        });
        internalFramesJPopupMenu.add(duplicatePlotJMenuItem);

        exportJMenu.setText("Export Plot As ...");

        exportAsSvgJMenuItem.setText("SVG");
        exportAsSvgJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportAsSvgJMenuItemActionPerformed(evt);
            }
        });
        exportJMenu.add(exportAsSvgJMenuItem);

        exportAsPdfJMenuItem.setText("PDF");
        exportAsPdfJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportAsPdfJMenuItemActionPerformed(evt);
            }
        });
        exportJMenu.add(exportAsPdfJMenuItem);

        exportAsTiffJMenuItem.setText("TIFF");
        exportAsTiffJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportAsTiffJMenuItemActionPerformed(evt);
            }
        });
        exportJMenu.add(exportAsTiffJMenuItem);

        exportAsJpgJMenuItem.setText("JPG");
        exportAsJpgJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportAsJpgJMenuItemActionPerformed(evt);
            }
        });
        exportJMenu.add(exportAsJpgJMenuItem);

        exportAsPngJMenuItem.setText("PNG");
        exportAsPngJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportAsPngJMenuItemActionPerformed(evt);
            }
        });
        exportJMenu.add(exportAsPngJMenuItem);

        internalFramesJPopupMenu.add(exportJMenu);
        internalFramesJPopupMenu.add(jSeparator2);

        showLegendsJMenuItem.setText("Hide Legend");
        showLegendsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showLegendsJMenuItemActionPerformed(evt);
            }
        });
        internalFramesJPopupMenu.add(showLegendsJMenuItem);

        showMarkersJMenuItem.setText("Show Markers");
        showMarkersJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showMarkersJMenuItemActionPerformed(evt);
            }
        });
        internalFramesJPopupMenu.add(showMarkersJMenuItem);

        showAverageJMenuItem.setText("Show Average Mass Errors");
        showAverageJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showAverageJMenuItemActionPerformed(evt);
            }
        });
        internalFramesJPopupMenu.add(showAverageJMenuItem);

        showMaxMinJMenuItem.setText("Show Max and Min Values");
        showMaxMinJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showMaxMinJMenuItemActionPerformed(evt);
            }
        });
        internalFramesJPopupMenu.add(showMaxMinJMenuItem);
        internalFramesJPopupMenu.add(jSeparator1);

        closeAllInternalFramesJMenuItem.setText("Close All");
        closeAllInternalFramesJMenuItem.setToolTipText("Close All Plots/Analyes");
        closeAllInternalFramesJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeAllInternalFramesJMenuItemActionPerformed(evt);
            }
        });
        internalFramesJPopupMenu.add(closeAllInternalFramesJMenuItem);

        selectAllIdentificationsJMenuItem.setText("Select All");
        selectAllIdentificationsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllIdentificationsJMenuItemActionPerformed(evt);
            }
        });
        selectIdentificationsJPopupMenu.add(selectAllIdentificationsJMenuItem);

        invertSelectionIdentificationsJMenuItem.setText("Invert Selection");
        invertSelectionIdentificationsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invertSelectionIdentificationsJMenuItemActionPerformed(evt);
            }
        });
        selectIdentificationsJPopupMenu.add(invertSelectionIdentificationsJMenuItem);

        highlightIdentificationsJMenu.setText("Highlight Selection");

        selectHighlightedIdentificationsJMenuItem.setText("Select Highlighted");
        selectHighlightedIdentificationsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectHighlightedIdentificationsJMenuItemActionPerformed(evt);
            }
        });
        highlightIdentificationsJMenu.add(selectHighlightedIdentificationsJMenuItem);

        deselectHighlightedIdentificationsJMenuItem.setText("Deselect Highlighted");
        deselectHighlightedIdentificationsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deselectHighlightedIdentificationsJMenuItemActionPerformed(evt);
            }
        });
        highlightIdentificationsJMenu.add(deselectHighlightedIdentificationsJMenuItem);

        selectIdentificationsJPopupMenu.add(highlightIdentificationsJMenu);

        peptideLengthJMenuItem.setText("Peptide Length Selection");
        peptideLengthJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideLengthJMenuItemActionPerformed(evt);
            }
        });
        selectIdentificationsJPopupMenu.add(peptideLengthJMenuItem);

        selectAllSpectrtaJMenuItem.setText("Select All");
        selectAllSpectrtaJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllSpectrtaJMenuItemActionPerformed(evt);
            }
        });
        selectSpectraJPopupMenu.add(selectAllSpectrtaJMenuItem);

        invertSelectionSpectraJMenuItem.setText("Invert Selection");
        invertSelectionSpectraJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invertSelectionSpectraJMenuItemActionPerformed(evt);
            }
        });
        selectSpectraJPopupMenu.add(invertSelectionSpectraJMenuItem);

        highlightSelectionSpectraJMenu.setText("Highlight Selection");

        selectHighlightedSpectraJMenuItem.setText("Select Highlighted");
        selectHighlightedSpectraJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectHighlightedSpectraJMenuItemActionPerformed(evt);
            }
        });
        highlightSelectionSpectraJMenu.add(selectHighlightedSpectraJMenuItem);

        deselectHighlightedSpectraJMenuItem.setText("Deselect Highlighted");
        deselectHighlightedSpectraJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deselectHighlightedSpectraJMenuItemActionPerformed(evt);
            }
        });
        highlightSelectionSpectraJMenu.add(deselectHighlightedSpectraJMenuItem);

        selectSpectraJPopupMenu.add(highlightSelectionSpectraJMenu);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("FragmentationAnalyzer");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        addWindowStateListener(new java.awt.event.WindowStateListener() {
            public void windowStateChanged(java.awt.event.WindowEvent evt) {
                formWindowStateChanged(evt);
            }
        });

        mainJSplitPane.setBorder(null);
        mainJSplitPane.setDividerLocation(384);
        mainJSplitPane.setDividerSize(0);

        searchSettingsJScrollPane.setBorder(null);
        searchSettingsJScrollPane.setPreferredSize(new java.awt.Dimension(373, 632));

        instrumentJXTaskPane.setTitle("Instruments");
        instrumentJXTaskPane.setAnimated(false);

        jXPanel7.setBackground(javax.swing.UIManager.getDefaults().getColor("tab_focus_fill_dark"));

        jLabel1.setText("Alt 1:");
        jLabel1.setMaximumSize(new java.awt.Dimension(39, 14));
        jLabel1.setMinimumSize(new java.awt.Dimension(39, 14));
        jLabel1.setPreferredSize(new java.awt.Dimension(39, 14));

        jLabel4.setText("Alt 3:");
        jLabel4.setMaximumSize(new java.awt.Dimension(39, 14));
        jLabel4.setMinimumSize(new java.awt.Dimension(39, 14));
        jLabel4.setPreferredSize(new java.awt.Dimension(39, 14));

        jLabel3.setText("Alt 2:");
        jLabel3.setMaximumSize(new java.awt.Dimension(39, 14));
        jLabel3.setMinimumSize(new java.awt.Dimension(39, 14));
        jLabel3.setPreferredSize(new java.awt.Dimension(39, 14));

        instrument1JComboBox.setMaximumRowCount(20);
        instrument1JComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "- Select -" }));
        instrument1JComboBox.setMinimumSize(new java.awt.Dimension(0, 0));
        instrument1JComboBox.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                instrument1JComboBoxPopupMenuWillBecomeVisible(evt);
            }
        });
        instrument1JComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                instrument1JComboBoxActionPerformed(evt);
            }
        });

        instrument2JComboBox.setMaximumRowCount(20);
        instrument2JComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "- Select -" }));
        instrument2JComboBox.setEnabled(false);
        instrument2JComboBox.setMinimumSize(new java.awt.Dimension(0, 0));
        instrument2JComboBox.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                instrument2JComboBoxPopupMenuWillBecomeVisible(evt);
            }
        });
        instrument2JComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                instrument2JComboBoxActionPerformed(evt);
            }
        });

        instrument3JComboBox.setMaximumRowCount(20);
        instrument3JComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "- Select -" }));
        instrument3JComboBox.setEnabled(false);
        instrument3JComboBox.setMinimumSize(new java.awt.Dimension(0, 0));
        instrument3JComboBox.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                instrument3JComboBoxPopupMenuWillBecomeVisible(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jXPanel7Layout = new org.jdesktop.layout.GroupLayout(jXPanel7);
        jXPanel7.setLayout(jXPanel7Layout);
        jXPanel7Layout.setHorizontalGroup(
            jXPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jXPanel7Layout.createSequentialGroup()
                .add(jXPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jXPanel7Layout.createSequentialGroup()
                        .add(jLabel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(instrument3JComboBox, 0, 253, Short.MAX_VALUE))
                    .add(jXPanel7Layout.createSequentialGroup()
                        .add(jXPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jXPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(instrument1JComboBox, 0, 253, Short.MAX_VALUE)
                            .add(instrument2JComboBox, 0, 253, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jXPanel7Layout.setVerticalGroup(
            jXPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jXPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .add(jXPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(instrument1JComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jXPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(instrument2JComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jXPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(instrument3JComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout instrumentJXTaskPaneLayout = new org.jdesktop.layout.GroupLayout(instrumentJXTaskPane.getContentPane());
        instrumentJXTaskPane.getContentPane().setLayout(instrumentJXTaskPaneLayout);
        instrumentJXTaskPaneLayout.setHorizontalGroup(
            instrumentJXTaskPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(instrumentJXTaskPaneLayout.createSequentialGroup()
                .addContainerGap()
                .add(jXPanel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        instrumentJXTaskPaneLayout.setVerticalGroup(
            instrumentJXTaskPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jXPanel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        searchSettingsJXTaskPaneContainer.add(instrumentJXTaskPane);

        terminalsAndChargeJXTaskPane.setTitle("Terminals & Charge");
        terminalsAndChargeJXTaskPane.setAnimated(false);

        jXPanel2.setBackground(javax.swing.UIManager.getDefaults().getColor("tab_focus_fill_dark"));

        jLabel17.setText("N-term:");

        nTermJComboBox.setMaximumRowCount(20);
        nTermJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "- Select -" }));
        nTermJComboBox.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                nTermJComboBoxPopupMenuWillBecomeVisible(evt);
            }
        });
        nTermJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nTermJComboBoxActionPerformed(evt);
            }
        });

        jLabel22.setText("C-term:");

        cTermJComboBox.setMaximumRowCount(20);
        cTermJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "- Select -" }));
        cTermJComboBox.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                cTermJComboBoxPopupMenuWillBecomeVisible(evt);
            }
        });
        cTermJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cTermJComboBoxActionPerformed(evt);
            }
        });

        jLabel2.setText("Charge:");

        chargeJComboBox.setMaximumRowCount(20);
        chargeJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "- Select -" }));
        chargeJComboBox.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                chargeJComboBoxPopupMenuWillBecomeVisible(evt);
            }
        });
        chargeJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chargeJComboBoxActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jXPanel2Layout = new org.jdesktop.layout.GroupLayout(jXPanel2);
        jXPanel2.setLayout(jXPanel2Layout);
        jXPanel2Layout.setHorizontalGroup(
            jXPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jXPanel2Layout.createSequentialGroup()
                .add(jXPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jXPanel2Layout.createSequentialGroup()
                        .add(jXPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel17)
                            .add(jLabel22))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jXPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(nTermJComboBox, 0, 253, Short.MAX_VALUE)
                            .add(cTermJComboBox, 0, 253, Short.MAX_VALUE)))
                    .add(jXPanel2Layout.createSequentialGroup()
                        .add(jLabel2)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(chargeJComboBox, 0, 253, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jXPanel2Layout.linkSize(new java.awt.Component[] {jLabel17, jLabel2, jLabel22}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jXPanel2Layout.setVerticalGroup(
            jXPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jXPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jXPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel17)
                    .add(nTermJComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jXPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel22)
                    .add(cTermJComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jXPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(chargeJComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout terminalsAndChargeJXTaskPaneLayout = new org.jdesktop.layout.GroupLayout(terminalsAndChargeJXTaskPane.getContentPane());
        terminalsAndChargeJXTaskPane.getContentPane().setLayout(terminalsAndChargeJXTaskPaneLayout);
        terminalsAndChargeJXTaskPaneLayout.setHorizontalGroup(
            terminalsAndChargeJXTaskPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(terminalsAndChargeJXTaskPaneLayout.createSequentialGroup()
                .addContainerGap()
                .add(jXPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        terminalsAndChargeJXTaskPaneLayout.setVerticalGroup(
            terminalsAndChargeJXTaskPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jXPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        searchSettingsJXTaskPaneContainer.add(terminalsAndChargeJXTaskPane);

        modificationsJXTaskPane.setTitle("Modifications");
        modificationsJXTaskPane.setAnimated(false);

        jXPanel3.setBackground(javax.swing.UIManager.getDefaults().getColor("tab_focus_fill_dark"));

        jLabel5.setText("Alt 1:");
        jLabel5.setMaximumSize(new java.awt.Dimension(39, 14));
        jLabel5.setMinimumSize(new java.awt.Dimension(39, 14));
        jLabel5.setPreferredSize(new java.awt.Dimension(39, 14));

        modification1JComboBox.setMaximumRowCount(20);
        modification1JComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "- Select -" }));
        modification1JComboBox.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                modification1JComboBoxPopupMenuWillBecomeVisible(evt);
            }
        });
        modification1JComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modification1JComboBoxActionPerformed(evt);
            }
        });

        jLabel6.setText("Alt 2:");
        jLabel6.setMaximumSize(new java.awt.Dimension(39, 14));
        jLabel6.setMinimumSize(new java.awt.Dimension(39, 14));
        jLabel6.setPreferredSize(new java.awt.Dimension(39, 14));

        modification2JComboBox.setMaximumRowCount(20);
        modification2JComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "- Select -" }));
        modification2JComboBox.setEnabled(false);
        modification2JComboBox.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                modification2JComboBoxPopupMenuWillBecomeVisible(evt);
            }
        });
        modification2JComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modification2JComboBoxActionPerformed(evt);
            }
        });

        jLabel7.setText("Alt 2:");
        jLabel7.setMaximumSize(new java.awt.Dimension(39, 14));
        jLabel7.setMinimumSize(new java.awt.Dimension(39, 14));
        jLabel7.setPreferredSize(new java.awt.Dimension(39, 14));

        modification3JComboBox.setMaximumRowCount(20);
        modification3JComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "- Select -" }));
        modification3JComboBox.setEnabled(false);
        modification3JComboBox.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                modification3JComboBoxPopupMenuWillBecomeVisible(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jXPanel3Layout = new org.jdesktop.layout.GroupLayout(jXPanel3);
        jXPanel3.setLayout(jXPanel3Layout);
        jXPanel3Layout.setHorizontalGroup(
            jXPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jXPanel3Layout.createSequentialGroup()
                .add(jXPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jXPanel3Layout.createSequentialGroup()
                        .add(jXPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jXPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(modification2JComboBox, 0, 253, Short.MAX_VALUE)
                            .add(modification1JComboBox, 0, 253, Short.MAX_VALUE)))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jXPanel3Layout.createSequentialGroup()
                        .add(jLabel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(modification3JComboBox, 0, 253, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jXPanel3Layout.setVerticalGroup(
            jXPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jXPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jXPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(modification1JComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jXPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(modification2JComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jXPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(modification3JComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout modificationsJXTaskPaneLayout = new org.jdesktop.layout.GroupLayout(modificationsJXTaskPane.getContentPane());
        modificationsJXTaskPane.getContentPane().setLayout(modificationsJXTaskPaneLayout);
        modificationsJXTaskPaneLayout.setHorizontalGroup(
            modificationsJXTaskPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(modificationsJXTaskPaneLayout.createSequentialGroup()
                .addContainerGap()
                .add(jXPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        modificationsJXTaskPaneLayout.setVerticalGroup(
            modificationsJXTaskPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jXPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        searchSettingsJXTaskPaneContainer.add(modificationsJXTaskPane);

        searchTypeJXPanel.setBackground(instrumentJXTaskPane.getBackground());

        generalSearchJRadioButton.setBackground(instrumentJXTaskPane.getBackground());
        searchButtonGroup.add(generalSearchJRadioButton);
        generalSearchJRadioButton.setSelected(true);
        generalSearchJRadioButton.setText("General Search");
        generalSearchJRadioButton.setToolTipText("<html>\nFind all identifications satisfying<br>\nthe attributes selected above.\n</html>");
        generalSearchJRadioButton.setIconTextGap(30);

        modificationSearchJRadioButton.setBackground(instrumentJXTaskPane.getBackground());
        searchButtonGroup.add(modificationSearchJRadioButton);
        modificationSearchJRadioButton.setText("Modification Search");
        modificationSearchJRadioButton.setToolTipText("<html>\nFind all pairs of identifications where one <br>\nis modified and the other contains one of <br>\nthe modifications selected above.\n</html>");
        modificationSearchJRadioButton.setIconTextGap(30);

        searchJButton.setMnemonic('S');
        searchJButton.setText("Search");
        searchJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchJButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout searchTypeJXPanelLayout = new org.jdesktop.layout.GroupLayout(searchTypeJXPanel);
        searchTypeJXPanel.setLayout(searchTypeJXPanelLayout);
        searchTypeJXPanelLayout.setHorizontalGroup(
            searchTypeJXPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(searchTypeJXPanelLayout.createSequentialGroup()
                .add(28, 28, 28)
                .add(searchJButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE)
                .add(35, 35, 35))
            .add(searchTypeJXPanelLayout.createSequentialGroup()
                .add(38, 38, 38)
                .add(searchTypeJXPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(generalSearchJRadioButton)
                    .add(modificationSearchJRadioButton))
                .addContainerGap(138, Short.MAX_VALUE))
        );
        searchTypeJXPanelLayout.setVerticalGroup(
            searchTypeJXPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(searchTypeJXPanelLayout.createSequentialGroup()
                .add(19, 19, 19)
                .add(generalSearchJRadioButton)
                .add(18, 18, 18)
                .add(modificationSearchJRadioButton)
                .add(29, 29, 29)
                .add(searchJButton)
                .add(13, 13, 13))
        );

        searchSettingsJXTaskPaneContainer.add(searchTypeJXPanel);

        searchSettingsJScrollPane.setViewportView(searchSettingsJXTaskPaneContainer);

        mainJSplitPane.setLeftComponent(searchSettingsJScrollPane);

        resultsJScrollPane.setBorder(null);

        resultsJXTaskPaneContainer.setMinimumSize(new java.awt.Dimension(805, 632));

        searchResultsJXTaskPane.setTitle("Search Results");
        searchResultsJXTaskPane.setAnimated(false);
        searchResultsJXTaskPane.getContentPane().setLayout(new javax.swing.BoxLayout(searchResultsJXTaskPane.getContentPane(), javax.swing.BoxLayout.LINE_AXIS));

        searchResultJXPanel.setBackground(javax.swing.UIManager.getDefaults().getColor("tab_focus_fill_dark"));
        searchResultJXPanel.setPreferredSize(new java.awt.Dimension(0, 233));

        searchResultsJButton.setText("Analyze / Plot");
        searchResultsJButton.setToolTipText(searchResultAnalysisButtonDisabledToolTip);
        searchResultsJButton.setEnabled(false);
        searchResultsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchResultsJButtonActionPerformed(evt);
            }
        });

        searchResultsJComboBox.setMaximumRowCount(12);
        searchResultsJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { " - Select Analysis Type - ", "List Individual Identifications", "Intensity Box Plot", "Mass Error Scatter Plot", "Mass Error Bubble Plot", "Mass Error Box Plot", "Fragment Ion Probability Plot", "Fragment Ion Heat Map", "Intensity Meta Plots", "Intensity Correlation" }));
        searchResultsJComboBox.setPreferredSize(new java.awt.Dimension(190, 20));
        searchResultsJComboBox.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                searchResultsJComboBoxPopupMenuWillBecomeVisible(evt);
            }
        });
        searchResultsJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchResultsJComboBoxActionPerformed(evt);
            }
        });

        searchResultsJXTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Sequence", "Modified Sequence", "Length", "#1", "#2", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        searchResultsJXTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                searchResultsJXTableMouseReleased(evt);
            }
        });
        searchResultsJScrollPane.setViewportView(searchResultsJXTable);

        daOrPpmSearchResultsJComboBox.setMaximumRowCount(12);
        daOrPpmSearchResultsJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Da", "ppm" }));
        daOrPpmSearchResultsJComboBox.setToolTipText("<html>\nThe distance measurement to use for the mass error.<br>\n(Absolute (Da) or relative (ppm)).\n</html>");
        daOrPpmSearchResultsJComboBox.setEnabled(false);

        combineSearchResultsJComboBox.setMaximumRowCount(12);
        combineSearchResultsJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Single", "Combine" }));
        combineSearchResultsJComboBox.setToolTipText("<html>\nChoose if the results should be combined into one plot.\n</html>");
        combineSearchResultsJComboBox.setEnabled(false);

        org.jdesktop.layout.GroupLayout searchResultJXPanelLayout = new org.jdesktop.layout.GroupLayout(searchResultJXPanel);
        searchResultJXPanel.setLayout(searchResultJXPanelLayout);
        searchResultJXPanelLayout.setHorizontalGroup(
            searchResultJXPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, searchResultJXPanelLayout.createSequentialGroup()
                .add(searchResultsJComboBox, 0, 283, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(daOrPpmSearchResultsJComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(combineSearchResultsJComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(46, 46, 46)
                .add(searchResultsJButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE))
            .add(org.jdesktop.layout.GroupLayout.TRAILING, searchResultsJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 783, Short.MAX_VALUE)
        );

        searchResultJXPanelLayout.linkSize(new java.awt.Component[] {combineSearchResultsJComboBox, daOrPpmSearchResultsJComboBox}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        searchResultJXPanelLayout.setVerticalGroup(
            searchResultJXPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, searchResultJXPanelLayout.createSequentialGroup()
                .add(searchResultsJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 201, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(searchResultJXPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(searchResultsJComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(searchResultsJButton)
                    .add(combineSearchResultsJComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(daOrPpmSearchResultsJComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
        );

        searchResultJXPanelLayout.linkSize(new java.awt.Component[] {combineSearchResultsJComboBox, daOrPpmSearchResultsJComboBox, searchResultsJButton, searchResultsJComboBox}, org.jdesktop.layout.GroupLayout.VERTICAL);

        searchResultsJXTaskPane.getContentPane().add(searchResultJXPanel);

        resultsJXTaskPaneContainer.add(searchResultsJXTaskPane);

        spectraJXTaskPane.setTitle("Individual Spectra");
        spectraJXTaskPane.setAnimated(false);
        spectraJXTaskPane.getContentPane().setLayout(new javax.swing.BoxLayout(spectraJXTaskPane.getContentPane(), javax.swing.BoxLayout.LINE_AXIS));

        spectraJXPanel.setBackground(javax.swing.UIManager.getDefaults().getColor("tab_focus_fill_dark"));
        spectraJXPanel.setPreferredSize(new java.awt.Dimension(0, 233));

        spectraJButton.setText("Analyze / Plot");
        spectraJButton.setToolTipText(spectraAnalysisButtonDisabledToolTip);
        spectraJButton.setEnabled(false);
        spectraJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spectraJButtonActionPerformed(evt);
            }
        });

        spectraJComboBox.setMaximumRowCount(12);
        spectraJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { " - Select Analysis Type - ", "View Spectra", "Intensity Box Plot", "Mass Error Scatter Plot", "Mass Error Bubble Plot", "Mass Error Box Plot", "Fragment Ion Probability Plot" }));
        spectraJComboBox.setPreferredSize(new java.awt.Dimension(190, 20));
        spectraJComboBox.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                spectraJComboBoxPopupMenuWillBecomeVisible(evt);
            }
        });
        spectraJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spectraJComboBoxActionPerformed(evt);
            }
        });

        spectraJXTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "ID", "SID", "Sequence", "Modified Sequence", "Length", "Instrument", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        spectraJXTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                spectraJXTableMouseReleased(evt);
            }
        });
        spectraJScrollPane.setViewportView(spectraJXTable);

        daOrPpmSpectraJComboBox.setMaximumRowCount(12);
        daOrPpmSpectraJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Da", "ppm" }));
        daOrPpmSpectraJComboBox.setToolTipText("<html>\nThe distance measurement to use for the mass error.<br>\n(Absolute (Da) or relative (ppm)).\n</html>");
        daOrPpmSpectraJComboBox.setEnabled(false);

        combineSpectraJComboBox.setMaximumRowCount(12);
        combineSpectraJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Single", "Combine" }));
        combineSpectraJComboBox.setToolTipText("<html>\nChoose if the results should be combined into one plot.\n</html>");
        combineSpectraJComboBox.setEnabled(false);

        org.jdesktop.layout.GroupLayout spectraJXPanelLayout = new org.jdesktop.layout.GroupLayout(spectraJXPanel);
        spectraJXPanel.setLayout(spectraJXPanelLayout);
        spectraJXPanelLayout.setHorizontalGroup(
            spectraJXPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(spectraJXPanelLayout.createSequentialGroup()
                .add(spectraJComboBox, 0, 286, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(daOrPpmSpectraJComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 65, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(combineSpectraJComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(50, 50, 50)
                .add(spectraJButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 291, Short.MAX_VALUE))
            .add(spectraJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 783, Short.MAX_VALUE)
        );
        spectraJXPanelLayout.setVerticalGroup(
            spectraJXPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, spectraJXPanelLayout.createSequentialGroup()
                .add(spectraJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 201, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(spectraJXPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(spectraJComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(spectraJButton)
                    .add(daOrPpmSpectraJComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(combineSpectraJComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
        );

        spectraJXPanelLayout.linkSize(new java.awt.Component[] {combineSpectraJComboBox, daOrPpmSpectraJComboBox, spectraJButton, spectraJComboBox}, org.jdesktop.layout.GroupLayout.VERTICAL);

        spectraJXTaskPane.getContentPane().add(spectraJXPanel);

        resultsJXTaskPaneContainer.add(spectraJXTaskPane);

        plotsAnalysesJXTaskPane.setTitle("Plots / Analyses");
        plotsAnalysesJXTaskPane.setAnimated(false);
        plotsAnalysesJXTaskPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                plotsAnalysesJXTaskPaneMouseClicked(evt);
            }
        });
        plotsAnalysesJXTaskPane.getContentPane().setLayout(new javax.swing.BoxLayout(plotsAnalysesJXTaskPane.getContentPane(), javax.swing.BoxLayout.LINE_AXIS));

        plotsAndAnalysesJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        plotsAndAnalysesJScrollPane.setOpaque(false);
        plotsAndAnalysesJScrollPane.setPreferredSize(new java.awt.Dimension(0, 487));

        plotsAndAnalysesJDesktopPane.setBackground(javax.swing.UIManager.getDefaults().getColor("tab_focus_fill_dark"));
        plotsAndAnalysesJDesktopPane.setDragMode(javax.swing.JDesktopPane.OUTLINE_DRAG_MODE);

        spectrumPanelToolBarJInternalFrame.setClosable(true);
        spectrumPanelToolBarJInternalFrame.setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
        spectrumPanelToolBarJInternalFrame.setFrameIcon(new javax.swing.ImageIcon(getClass().getResource("/no/uib/fragmentation_analyzer/icons/spectrum.GIF"))); // NOI18N
        spectrumPanelToolBarJInternalFrame.setVisible(true);

        jPanel5.setOpaque(false);
        jPanel5.setPreferredSize(new java.awt.Dimension(96, 463));

        yIonsJCheckBox.setSelected(true);
        yIonsJCheckBox.setText("y");
        yIonsJCheckBox.setToolTipText("Show y-ions");
        yIonsJCheckBox.setMaximumSize(new java.awt.Dimension(39, 23));
        yIonsJCheckBox.setMinimumSize(new java.awt.Dimension(39, 23));
        yIonsJCheckBox.setOpaque(false);
        yIonsJCheckBox.setPreferredSize(new java.awt.Dimension(39, 23));
        yIonsJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yIonsJCheckBoxActionPerformed(evt);
            }
        });

        xIonsJCheckBox.setText("x");
        xIonsJCheckBox.setToolTipText("Show x-ions");
        xIonsJCheckBox.setMaximumSize(new java.awt.Dimension(39, 23));
        xIonsJCheckBox.setMinimumSize(new java.awt.Dimension(39, 23));
        xIonsJCheckBox.setOpaque(false);
        xIonsJCheckBox.setPreferredSize(new java.awt.Dimension(39, 23));
        xIonsJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xIonsJCheckBoxActionPerformed(evt);
            }
        });

        zIonsJCheckBox.setText("z");
        zIonsJCheckBox.setToolTipText("Show z-ions");
        zIonsJCheckBox.setMaximumSize(new java.awt.Dimension(39, 23));
        zIonsJCheckBox.setMinimumSize(new java.awt.Dimension(39, 23));
        zIonsJCheckBox.setOpaque(false);
        zIonsJCheckBox.setPreferredSize(new java.awt.Dimension(39, 23));
        zIonsJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zIonsJCheckBoxActionPerformed(evt);
            }
        });

        chargeOneJCheckBox.setSelected(true);
        chargeOneJCheckBox.setText("+");
        chargeOneJCheckBox.setToolTipText("Show ions with a charge of +1");
        chargeOneJCheckBox.setMaximumSize(new java.awt.Dimension(39, 23));
        chargeOneJCheckBox.setMinimumSize(new java.awt.Dimension(39, 23));
        chargeOneJCheckBox.setOpaque(false);
        chargeOneJCheckBox.setPreferredSize(new java.awt.Dimension(39, 23));
        chargeOneJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chargeOneJCheckBoxActionPerformed(evt);
            }
        });

        chargeTwoJCheckBox.setText("++");
        chargeTwoJCheckBox.setToolTipText("Show ions with a charge of +2");
        chargeTwoJCheckBox.setMaximumSize(new java.awt.Dimension(39, 23));
        chargeTwoJCheckBox.setMinimumSize(new java.awt.Dimension(39, 23));
        chargeTwoJCheckBox.setOpaque(false);
        chargeTwoJCheckBox.setPreferredSize(new java.awt.Dimension(39, 23));
        chargeTwoJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chargeTwoJCheckBoxActionPerformed(evt);
            }
        });

        chargeOverTwoJCheckBox.setText(">2");
        chargeOverTwoJCheckBox.setToolTipText("Show ions with charge greater than 2");
        chargeOverTwoJCheckBox.setOpaque(false);
        chargeOverTwoJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chargeOverTwoJCheckBoxActionPerformed(evt);
            }
        });

        cIonsJCheckBox.setText("c");
        cIonsJCheckBox.setToolTipText("Show c-ions");
        cIonsJCheckBox.setMaximumSize(new java.awt.Dimension(39, 23));
        cIonsJCheckBox.setMinimumSize(new java.awt.Dimension(39, 23));
        cIonsJCheckBox.setOpaque(false);
        cIonsJCheckBox.setPreferredSize(new java.awt.Dimension(39, 23));
        cIonsJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cIonsJCheckBoxActionPerformed(evt);
            }
        });

        bIonsJCheckBox.setSelected(true);
        bIonsJCheckBox.setText("b");
        bIonsJCheckBox.setToolTipText("Show b-ions");
        bIonsJCheckBox.setMaximumSize(new java.awt.Dimension(39, 23));
        bIonsJCheckBox.setMinimumSize(new java.awt.Dimension(39, 23));
        bIonsJCheckBox.setOpaque(false);
        bIonsJCheckBox.setPreferredSize(new java.awt.Dimension(39, 23));
        bIonsJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bIonsJCheckBoxActionPerformed(evt);
            }
        });

        aIonsJCheckBox.setText("a");
        aIonsJCheckBox.setToolTipText("Show a-ions");
        aIonsJCheckBox.setMaximumSize(new java.awt.Dimension(39, 23));
        aIonsJCheckBox.setMinimumSize(new java.awt.Dimension(39, 23));
        aIonsJCheckBox.setOpaque(false);
        aIonsJCheckBox.setPreferredSize(new java.awt.Dimension(39, 23));
        aIonsJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aIonsJCheckBoxActionPerformed(evt);
            }
        });

        otherIonsJCheckBox.setText("Oth.");
        otherIonsJCheckBox.setToolTipText("Show precursor, immonimum ions etc");
        otherIonsJCheckBox.setMaximumSize(new java.awt.Dimension(39, 23));
        otherIonsJCheckBox.setMinimumSize(new java.awt.Dimension(39, 23));
        otherIonsJCheckBox.setOpaque(false);
        otherIonsJCheckBox.setPreferredSize(new java.awt.Dimension(39, 23));
        otherIonsJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                otherIonsJCheckBoxActionPerformed(evt);
            }
        });

        H2OIonsJCheckBox.setText("H2O");
        H2OIonsJCheckBox.setToolTipText("Show H2O neutral losses");
        H2OIonsJCheckBox.setMaximumSize(new java.awt.Dimension(39, 23));
        H2OIonsJCheckBox.setMinimumSize(new java.awt.Dimension(39, 23));
        H2OIonsJCheckBox.setOpaque(false);
        H2OIonsJCheckBox.setPreferredSize(new java.awt.Dimension(39, 23));
        H2OIonsJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                H2OIonsJCheckBoxActionPerformed(evt);
            }
        });

        NH3IonsJCheckBox.setText("NH3");
        NH3IonsJCheckBox.setToolTipText("Show NH3 neutral losses");
        NH3IonsJCheckBox.setMaximumSize(new java.awt.Dimension(39, 23));
        NH3IonsJCheckBox.setMinimumSize(new java.awt.Dimension(39, 23));
        NH3IonsJCheckBox.setOpaque(false);
        NH3IonsJCheckBox.setPreferredSize(new java.awt.Dimension(39, 23));
        NH3IonsJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NH3IonsJCheckBoxActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(NH3IonsJCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
                    .add(jSeparator3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 34, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(yIonsJCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(zIonsJCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(xIonsJCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jSeparator5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 34, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(aIonsJCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(bIonsJCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(cIonsJCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(otherIonsJCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
                    .add(H2OIonsJCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
                    .add(jSeparator6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 34, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(chargeTwoJCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
                    .add(chargeOverTwoJCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
                    .add(chargeOneJCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel5Layout.linkSize(new java.awt.Component[] {aIonsJCheckBox, bIonsJCheckBox, cIonsJCheckBox, xIonsJCheckBox, yIonsJCheckBox, zIonsJCheckBox}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .add(10, 10, 10)
                .add(aIonsJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(bIonsJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(cIonsJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(jSeparator3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(xIonsJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(yIonsJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(zIonsJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(jSeparator5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(otherIonsJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(H2OIonsJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(NH3IonsJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(jSeparator6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(chargeOneJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(chargeTwoJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(chargeOverTwoJCheckBox)
                .addContainerGap())
        );

        jPanel5Layout.linkSize(new java.awt.Component[] {H2OIonsJCheckBox, NH3IonsJCheckBox, aIonsJCheckBox, bIonsJCheckBox, cIonsJCheckBox, chargeOneJCheckBox, chargeOverTwoJCheckBox, chargeTwoJCheckBox, otherIonsJCheckBox, xIonsJCheckBox, yIonsJCheckBox, zIonsJCheckBox}, org.jdesktop.layout.GroupLayout.VERTICAL);

        org.jdesktop.layout.GroupLayout spectrumPanelToolBarJInternalFrameLayout = new org.jdesktop.layout.GroupLayout(spectrumPanelToolBarJInternalFrame.getContentPane());
        spectrumPanelToolBarJInternalFrame.getContentPane().setLayout(spectrumPanelToolBarJInternalFrameLayout);
        spectrumPanelToolBarJInternalFrameLayout.setHorizontalGroup(
            spectrumPanelToolBarJInternalFrameLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 74, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        );
        spectrumPanelToolBarJInternalFrameLayout.setVerticalGroup(
            spectrumPanelToolBarJInternalFrameLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 325, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        );

        spectrumPanelToolBarJInternalFrame.setBounds(670, 30, 80, 370);
        plotsAndAnalysesJDesktopPane.add(spectrumPanelToolBarJInternalFrame, javax.swing.JLayeredPane.POPUP_LAYER);

        boxPlotPanelToolBarJInternalFrame.setClosable(true);
        boxPlotPanelToolBarJInternalFrame.setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
        boxPlotPanelToolBarJInternalFrame.setFrameIcon(new javax.swing.ImageIcon(getClass().getResource("/no/uib/fragmentation_analyzer/icons/box_plot_small.GIF"))); // NOI18N
        boxPlotPanelToolBarJInternalFrame.setPreferredSize(new java.awt.Dimension(100, 350));
        boxPlotPanelToolBarJInternalFrame.setVisible(true);

        jPanel6.setOpaque(false);
        jPanel6.setPreferredSize(new java.awt.Dimension(96, 463));

        jSeparator4.setPreferredSize(new java.awt.Dimension(0, 40));

        bIonsUnmodifiedJCheckBox.setSelected(true);
        bIonsUnmodifiedJCheckBox.setText("<html>b ions<br> unmod.</html>");
        bIonsUnmodifiedJCheckBox.setToolTipText("Hide Box Plot Fragment Ion Series");
        bIonsUnmodifiedJCheckBox.setIconTextGap(15);
        bIonsUnmodifiedJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bIonsUnmodifiedJCheckBoxActionPerformed(evt);
            }
        });

        bIonsModifiedJCheckBox.setSelected(true);
        bIonsModifiedJCheckBox.setText("<html>b ions<br> mod.</html>");
        bIonsModifiedJCheckBox.setToolTipText("Hide Box Plot Fragment Ion Series");
        bIonsModifiedJCheckBox.setIconTextGap(15);
        bIonsModifiedJCheckBox.setPreferredSize(new java.awt.Dimension(73, 37));
        bIonsModifiedJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bIonsModifiedJCheckBoxActionPerformed(evt);
            }
        });

        yIonsUnmodifiedJCheckBox.setSelected(true);
        yIonsUnmodifiedJCheckBox.setText("<html>y ions<br> unmod.</html>");
        yIonsUnmodifiedJCheckBox.setToolTipText("Hide Box Plot Fragment Ion Series");
        yIonsUnmodifiedJCheckBox.setIconTextGap(15);
        yIonsUnmodifiedJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yIonsUnmodifiedJCheckBoxActionPerformed(evt);
            }
        });

        yIonsModifiedJCheckBox.setSelected(true);
        yIonsModifiedJCheckBox.setText("<html>y ions<br> mod.</html>");
        yIonsModifiedJCheckBox.setToolTipText("Hide Box Plot Fragment Ion Series");
        yIonsModifiedJCheckBox.setIconTextGap(15);
        yIonsModifiedJCheckBox.setPreferredSize(new java.awt.Dimension(73, 37));
        yIonsModifiedJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yIonsModifiedJCheckBoxActionPerformed(evt);
            }
        });

        bIonsBoxPlotJCheckBox.setSelected(true);
        bIonsBoxPlotJCheckBox.setText("<html>b ions<br> </html>");
        bIonsBoxPlotJCheckBox.setToolTipText("Hide Box Plot Fragment Ion Series");
        bIonsBoxPlotJCheckBox.setIconTextGap(15);
        bIonsBoxPlotJCheckBox.setPreferredSize(new java.awt.Dimension(73, 37));
        bIonsBoxPlotJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bIonsBoxPlotJCheckBoxActionPerformed(evt);
            }
        });

        yIonsBoxPlotJCheckBox.setSelected(true);
        yIonsBoxPlotJCheckBox.setText("<html>y ions</html>");
        yIonsBoxPlotJCheckBox.setToolTipText("Hide Box Plot Fragment Ion Series");
        yIonsBoxPlotJCheckBox.setIconTextGap(15);
        yIonsBoxPlotJCheckBox.setPreferredSize(new java.awt.Dimension(73, 37));
        yIonsBoxPlotJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yIonsBoxPlotJCheckBoxActionPerformed(evt);
            }
        });

        jSeparator7.setMaximumSize(new java.awt.Dimension(0, 40));
        jSeparator7.setMinimumSize(new java.awt.Dimension(0, 40));
        jSeparator7.setPreferredSize(new java.awt.Dimension(0, 40));

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(yIonsModifiedJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(yIonsUnmodifiedJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(bIonsModifiedJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(bIonsUnmodifiedJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(yIonsBoxPlotJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(bIonsBoxPlotJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jSeparator7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 70, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jSeparator4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 70, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel6Layout.linkSize(new java.awt.Component[] {bIonsBoxPlotJCheckBox, bIonsModifiedJCheckBox, bIonsUnmodifiedJCheckBox, yIonsBoxPlotJCheckBox, yIonsModifiedJCheckBox, yIonsUnmodifiedJCheckBox}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .add(10, 10, 10)
                .add(bIonsBoxPlotJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(yIonsBoxPlotJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(jSeparator7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(bIonsUnmodifiedJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(bIonsModifiedJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(jSeparator4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(yIonsUnmodifiedJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, 0)
                .add(yIonsModifiedJCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(19, Short.MAX_VALUE))
        );

        jPanel6Layout.linkSize(new java.awt.Component[] {bIonsModifiedJCheckBox, bIonsUnmodifiedJCheckBox, yIonsModifiedJCheckBox, yIonsUnmodifiedJCheckBox}, org.jdesktop.layout.GroupLayout.VERTICAL);

        jPanel6Layout.linkSize(new java.awt.Component[] {bIonsBoxPlotJCheckBox, yIonsBoxPlotJCheckBox}, org.jdesktop.layout.GroupLayout.VERTICAL);

        org.jdesktop.layout.GroupLayout boxPlotPanelToolBarJInternalFrameLayout = new org.jdesktop.layout.GroupLayout(boxPlotPanelToolBarJInternalFrame.getContentPane());
        boxPlotPanelToolBarJInternalFrame.getContentPane().setLayout(boxPlotPanelToolBarJInternalFrameLayout);
        boxPlotPanelToolBarJInternalFrameLayout.setHorizontalGroup(
            boxPlotPanelToolBarJInternalFrameLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(boxPlotPanelToolBarJInternalFrameLayout.createSequentialGroup()
                .add(jPanel6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 103, Short.MAX_VALUE)
                .addContainerGap())
        );
        boxPlotPanelToolBarJInternalFrameLayout.setVerticalGroup(
            boxPlotPanelToolBarJInternalFrameLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(boxPlotPanelToolBarJInternalFrameLayout.createSequentialGroup()
                .add(jPanel6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 243, Short.MAX_VALUE)
                .addContainerGap())
        );

        boxPlotPanelToolBarJInternalFrame.setBounds(630, 80, 100, 290);
        plotsAndAnalysesJDesktopPane.add(boxPlotPanelToolBarJInternalFrame, javax.swing.JLayeredPane.POPUP_LAYER);

        internalFrameTipJLabel.setFont(internalFrameTipJLabel.getFont().deriveFont((internalFrameTipJLabel.getFont().getStyle() | java.awt.Font.ITALIC), internalFrameTipJLabel.getFont().getSize()-3));
        internalFrameTipJLabel.setText("Right click in the plot or on the plot title bar for plot options");
        internalFrameTipJLabel.setBounds(30, 0, 470, 20);
        plotsAndAnalysesJDesktopPane.add(internalFrameTipJLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        plotsAndAnalysesJScrollPane.setViewportView(plotsAndAnalysesJDesktopPane);

        plotsAnalysesJXTaskPane.getContentPane().add(plotsAndAnalysesJScrollPane);

        resultsJXTaskPaneContainer.add(plotsAnalysesJXTaskPane);

        resultsJScrollPane.setViewportView(resultsJXTaskPaneContainer);

        mainJSplitPane.setRightComponent(resultsJScrollPane);

        fileJMenu.setMnemonic('F');
        fileJMenu.setText("File");

        opemJMenuItem.setMnemonic('O');
        opemJMenuItem.setText("Open");
        opemJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                opemJMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(opemJMenuItem);
        fileJMenu.add(jSeparator8);

        exitJMenuItem.setText("Exit");
        exitJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitJMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(exitJMenuItem);

        jMenuBar.add(fileJMenu);

        editMenu.setMnemonic('E');
        editMenu.setText("Edit");

        preferencesJMenuItem.setText("Preferences");
        preferencesJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                preferencesJMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(preferencesJMenuItem);

        jMenuBar.add(editMenu);

        windowJMenu.setMnemonic('W');
        windowJMenu.setText("Window");

        showLeftPanelJMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        showLeftPanelJMenuItem.setText("Hide Left Panel");
        showLeftPanelJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showLeftPanelJMenuItemActionPerformed(evt);
            }
        });
        windowJMenu.add(showLeftPanelJMenuItem);

        jMenuBar.add(windowJMenu);

        helpJMenu.setMnemonic('H');
        helpJMenu.setText("Help");

        helpJMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        helpJMenuItem.setText("Help");
        helpJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpJMenuItemActionPerformed(evt);
            }
        });
        helpJMenu.add(helpJMenuItem);

        aboutJMenuItem.setText("About");
        aboutJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutJMenuItemActionPerformed(evt);
            }
        });
        helpJMenu.add(aboutJMenuItem);

        jMenuBar.add(helpJMenu);

        setJMenuBar(jMenuBar);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainJSplitPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1230, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainJSplitPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 619, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @see #exitJMenuItemActionPerformed(java.awt.event.ActionEvent)
     */
    public void close() {
        exitJMenuItemActionPerformed(null);
    }

    /**
     * Closes the database connection. Then terminates the program.
     *
     * @param evt
     */
    private void exitJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitJMenuItemActionPerformed
        userProperties.saveUserPropertiesToFile();
        closeDatabaseConnection();
        System.exit(0);
    }//GEN-LAST:event_exitJMenuItemActionPerformed

    /**
     * @see #exitJMenuItemActionPerformed(java.awt.event.ActionEvent)
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        exitJMenuItemActionPerformed(null);
    }//GEN-LAST:event_formWindowClosing

    /**
     * Opens a new DataSource dialog.
     *
     * @param evt
     */
    private void opemJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_opemJMenuItemActionPerformed
        new DataSource(this, true);
    }//GEN-LAST:event_opemJMenuItemActionPerformed

    /**
     * Returns true if at least one modification is selected.
     *
     * @return true if at least one modification is selected, false otherwise
     */
    private boolean modificationSelected() {
        String modification1 = (String) modification1JComboBox.getSelectedItem();
        return (!modification1.equalsIgnoreCase(" - Select - "));
    }

    /**
     * Starts the selected search and inserts the result into the results table.
     *
     * @param evt
     */
    private void searchJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchJButtonActionPerformed

        cancelProgress = false;

        // get the search settings
        int tempSearchType = -1;

        if (generalSearchJRadioButton.isSelected()) {
            tempSearchType = Properties.GENERAL_SEARCH;
        } else if (modificationSearchJRadioButton.isSelected()) {
            tempSearchType = Properties.MODIFICATION_SEARCH;
        }

        // has to be done like this due to the variable being used inside the thread
        final int searchType = tempSearchType;

        // verify that all the required parameters have been selected
        if (!searchEnabled) {
            JOptionPane.showMessageDialog(null,
                    "At least one instrument, the terminals and the charge has to be selected.",
                    "Search Parameters", JOptionPane.INFORMATION_MESSAGE);
            cancelProgress = true;
        }


        // if modification search, verify that at least one modification has been selected
        if (searchType == Properties.MODIFICATION_SEARCH && !cancelProgress) {
            if (!modificationSelected()) {
                JOptionPane.showMessageDialog(null,
                        "For Modification Searches you have to select at least one modification.",
                        "Modification Search", JOptionPane.INFORMATION_MESSAGE);
                cancelProgress = true;
            } else {
                // get the minimum number of sequence pairs required for a pair to be included in the results
                new MinimumSequencePairs(this, true);
            }
        }


        if (!cancelProgress) {

            // has to be final to be used inside the thread
            final int minimumNumberOfModificationPairs = userProperties.getMinimumIdentificationPairCounter();


            // open or close the involved panes to make the search results visible
            resultsJScrollPane.getVerticalScrollBar().setValue(0);
            searchResultsJScrollPane.getVerticalScrollBar().setValue(0);
            resultsJScrollPane.repaint();
            searchResultsJXTaskPane.setExpanded(false);
            spectraJXTaskPane.setExpanded(false);
            plotsAnalysesJXTaskPane.setExpanded(false);

            progressDialog = new ProgressDialog(this, this, true);

            new Thread(new Runnable() {

                public void run() {
                    progressDialog.setIntermidiate(false);
                    progressDialog.setTitle("Searching. Please Wait...");
                    progressDialog.setVisible(true);
                }
            }, "ProgressDialog").start();

            new Thread("SearchThread") {

                @Override
                public void run() {

                    File identificationFile = new File(properties.getCurrentDataSetFolder() + "/identifications.txt");

                    if (identificationFile.exists()) {

                        // empty the tables
                        ((DefaultTableModel) searchResultsJXTable.getModel()).setRowCount(0);
                        ((DefaultTableModel) spectraJXTable.getModel()).setRowCount(0);

                        properties.setCurrentlySelectedRowsInSearchTable(new ArrayList<IdentificationTableRow>());
                        properties.setCurrentlySelectedRowsInSpectraTable(new ArrayList<SpectrumTableRow>());

                        searchResultsJButton.setEnabled(false);
                        searchResultsJButton.setToolTipText(searchResultAnalysisButtonDisabledToolTip);
                        spectraJButton.setEnabled(false);
                        spectraJButton.setToolTipText(spectraAnalysisButtonDisabledToolTip);

                        properties.setSelectAllIdentifications(true);
                        selectAllIdentificationsJMenuItem.setText("Select All");

                        Integer charge = new Integer(Util.removeOccurenceCount(((String) chargeJComboBox.getSelectedItem())));

                        String modification1 = Util.removeOccurenceCount(((String) modification1JComboBox.getSelectedItem()));
                        String modification2 = Util.removeOccurenceCount(((String) modification2JComboBox.getSelectedItem()));
                        String modification3 = Util.removeOccurenceCount(((String) modification3JComboBox.getSelectedItem()));

                        String nTerminal = Util.removeOccurenceCount(((String) nTermJComboBox.getSelectedItem()));
                        String cTerminal = Util.removeOccurenceCount(((String) cTermJComboBox.getSelectedItem()));

                        String instrument1 = Util.removeOccurenceCount(((String) instrument1JComboBox.getSelectedItem()));
                        String instrument2 = Util.removeOccurenceCount(((String) instrument2JComboBox.getSelectedItem()));
                        String instrument3 = Util.removeOccurenceCount(((String) instrument3JComboBox.getSelectedItem()));

                        try {
                            BufferedReader b = new BufferedReader(new FileReader(identificationFile));

                            int identificationCount = new Integer(b.readLine());

                            int progressCounter = 0;
                            progressDialog.setValue(0);
                            progressDialog.setMax(identificationCount);

                            String currentLine = b.readLine();

                            int matchCounter = 0;

                            properties.setIdentificationMap(new HashMap<String, ArrayList<ReducedIdentification>>());
                            properties.setAllIdentifications(new HashMap<Integer, ReducedIdentification>());

                            // force garbage collection
                            System.gc();

                            while (currentLine != null && !cancelProgress) {
                                ReducedIdentification currentIdentification = new ReducedIdentification(currentLine, true);

                                boolean identificationMatch = true;

                                // check the charge
                                if (identificationMatch) {
                                    identificationMatch = Util.checkCharge(currentIdentification, charge);
                                }

                                // check the instrument
                                if (identificationMatch) {
                                    identificationMatch = Util.checkInstrument(
                                            currentIdentification, instrument1, instrument2, instrument3);
                                }

                                // check the terminals
                                if (identificationMatch) {
                                    identificationMatch = Util.checkTerminals(currentIdentification, nTerminal, cTerminal);
                                }

                                // check the modifications
                                if (identificationMatch && (searchType != Properties.MODIFICATION_SEARCH)) {
                                    identificationMatch = Util.checkModifications(
                                            currentIdentification, modification1, modification2, modification3, false,
                                            properties.getPattern());
                                }

                                if (identificationMatch) {

                                    matchCounter++;

                                    if (searchType != Properties.MODIFICATION_SEARCH) {

                                        if (properties.getIdentificationMap().containsKey(currentIdentification.getModifiedSequence())) {
                                            ArrayList<ReducedIdentification> temp = properties.getIdentificationMap().get(
                                                    currentIdentification.getModifiedSequence());
                                            temp.add(currentIdentification);
                                            properties.getIdentificationMap().put(currentIdentification.getModifiedSequence(), temp);
                                            properties.getAllIdentifications().put(currentIdentification.getIdentificationId(), currentIdentification);
                                        } else {
                                            ArrayList<ReducedIdentification> temp = new ArrayList<ReducedIdentification>();
                                            temp.add(currentIdentification);
                                            properties.getIdentificationMap().put(currentIdentification.getModifiedSequence(), temp);
                                            properties.getAllIdentifications().put(currentIdentification.getIdentificationId(), currentIdentification);
                                        }
                                    } else {

                                        if (properties.getIdentificationMap().containsKey(currentIdentification.getSequence())) {
                                            ArrayList<ReducedIdentification> temp = properties.getIdentificationMap().get(currentIdentification.getSequence());
                                            temp.add(currentIdentification);
                                            properties.getIdentificationMap().put(currentIdentification.getSequence(), temp);
                                            properties.getAllIdentifications().put(currentIdentification.getIdentificationId(), currentIdentification);
                                        } else {
                                            ArrayList<ReducedIdentification> temp = new ArrayList<ReducedIdentification>();
                                            temp.add(currentIdentification);
                                            properties.getIdentificationMap().put(currentIdentification.getSequence(), temp);
                                            properties.getAllIdentifications().put(currentIdentification.getIdentificationId(), currentIdentification);
                                        }
                                    }
                                }

                                progressDialog.setValue(progressCounter++);
                                currentLine = b.readLine();
                            }

                            if (matchCounter == 0 && !cancelProgress) {

                                progressDialog.setVisible(false);
                                progressDialog.dispose();

                                JOptionPane.showMessageDialog(null,
                                        "The search returned no hits.",
                                        "Search Results", JOptionPane.INFORMATION_MESSAGE);
                            } else {

                                if (!cancelProgress) {

                                    boolean rowsInserted = false;

                                    // hide or show the second count column
                                    hideSecondCountColumn(searchType == Properties.GENERAL_SEARCH);

                                    searchResultsJXTable.setSortable(false);

                                    if (searchType == Properties.GENERAL_SEARCH) {

                                        progressDialog.setTitle("Updating Results Table. Please Wait...");
                                        progressDialog.setIntermidiate(true);

                                        rowsInserted = insertGeneralSearchResults(progressDialog);

                                    } else if (searchType == Properties.MODIFICATION_SEARCH) {

                                        progressDialog.setTitle("Finding Sequence Pairs. Please Wait...");
                                        progressDialog.setIntermidiate(false);

                                        rowsInserted = insertModificationSearchResults(
                                                progressDialog, modification1, modification2, modification3,
                                                minimumNumberOfModificationPairs);
                                    }

                                    // check if any matches were found
                                    if (!rowsInserted) {

                                        progressDialog.setVisible(false);
                                        progressDialog.dispose();

                                        JOptionPane.showMessageDialog(null,
                                                "The search returned no hits.",
                                                "Search Results", JOptionPane.INFORMATION_MESSAGE);
                                    } else {

                                        progressDialog.setTitle("Sorting Results Table. Please Wait...");

                                        // sort the results table on the count column
                                        sortResultTableOnCount();

                                        final int finalMatchCounter = matchCounter;

                                        progressDialog.setVisible(false);
                                        progressDialog.dispose();

                                        NumberFormat formatter =  new DecimalFormat("##,###,###");

                                        if (searchType == Properties.GENERAL_SEARCH) {

                                            JOptionPane.showMessageDialog(null,
                                                    "#Identifications: " + formatter.format(finalMatchCounter) + "\n" +
                                                    "#Unique Peptides: " + formatter.format(properties.getIdentificationMap().size()) + "\n",
                                                    "Search Results", JOptionPane.INFORMATION_MESSAGE);
                                        } else if (searchType == Properties.MODIFICATION_SEARCH) {

                                            JOptionPane.showMessageDialog(null,
                                                    "#Sequence Pairs: " + formatter.format(searchResultsJXTable.getRowCount()),
                                                    "Search Results", JOptionPane.INFORMATION_MESSAGE);
                                        }
                                    }
                                }
                            }

                            if (searchResultsJXTable.getRowCount() > 0) {
                                searchResultsJXTable.setRowSelectionInterval(0, 0);
                                searchResultsJXTaskPane.setExpanded(true);
                            }

                            b.close();

                        } catch (FileNotFoundException e) {
                            JOptionPane.showMessageDialog(null,
                                    "An error occured when trying to open the identifications.txt file.\n" +
                                    "See /Properties/ErrorLog.txt for more details.",
                                    "Error Opening Identifications File", JOptionPane.ERROR_MESSAGE);
                            Util.writeToErrorLog("Error opening identifications file: ");
                            e.printStackTrace();
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(null,
                                    "An error occured when trying to open the identifications.txt file.\n" +
                                    "See /Properties/ErrorLog.txt for more details.",
                                    "Error Opening Identifications File", JOptionPane.ERROR_MESSAGE);
                            Util.writeToErrorLog("Error opening identifications file: ");
                            e.printStackTrace();
                        }
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "An error occured when trying to open the data set.\n" +
                                "See /Properties/ErrorLog.txt for more details.",
                                "Error Opening Data Set", JOptionPane.ERROR_MESSAGE);
                        Util.writeToErrorLog("Error Opening Data Set: the identifications file "
                                + properties.getCurrentDataSetFolder() +
                                "/identifications.txt" + " does not exist!");
                    }

                    progressDialog.setVisible(false);
                    progressDialog.dispose();
                }
            }.start();
        }
    }//GEN-LAST:event_searchJButtonActionPerformed

    /**
     * Inserts the result of a modification search into the search table. Returns true if
     * at least one row was inserted.
     *
     * @param progressDialog
     * @param modification1
     * @param modification2
     * @param modification3
     * @param minimumNumberOfModificationPairs
     * @return true if at least one row was inserted, false otherwise
     */
    private boolean insertModificationSearchResults(ProgressDialog progressDialog,
            String modification1, String modification2, String modification3,
            int minimumNumberOfModificationPairs) {

        ArrayList<ReducedIdentification> reducedIdentifications;
        Iterator<String> iterator = properties.getIdentificationMap().keySet().iterator();
        progressDialog.setMax(properties.getIdentificationMap().keySet().size());

        int rowCounter = 0, progressCounter = 1;
        int maxSequenceLength = 0;
        int maxSequenceCount1 = 0;
        int maxSequenceCount2 = 0;

        HashMap<String, Integer> modifiedSequences = new HashMap<String, Integer>();

        while (iterator.hasNext() && !cancelProgress) {

            progressDialog.setValue(progressCounter++);

            reducedIdentifications = properties.getIdentificationMap().get(iterator.next());

            int unmodifiedCounter = 0;
            modifiedSequences.clear();
            modifiedSequences = new HashMap<String, Integer>();

            if (reducedIdentifications.size() > 1) {

                for (int i = 0; i < reducedIdentifications.size() && !cancelProgress; i++) {

                    ReducedIdentification currentIdentification = reducedIdentifications.get(i);

                    if (currentIdentification.isModified()) {

                        if (Util.checkModifications(
                                currentIdentification, modification1, modification2, modification3, true,
                                properties.getPattern())) {

                            // have to be able to handle multiple versions of modified peptides
                            if (modifiedSequences.containsKey(currentIdentification.getModifiedSequence())) {
                                Integer counter = modifiedSequences.get(currentIdentification.getModifiedSequence());
                                modifiedSequences.put(currentIdentification.getModifiedSequence(), ++counter);
                            } else {
                                modifiedSequences.put(currentIdentification.getModifiedSequence(), 1);
                            }
                        } else {
                            //System.out.println("Modification did not match");
                        }
                    } else {
                        unmodifiedCounter++;
                    }
                }

                if (unmodifiedCounter >= minimumNumberOfModificationPairs && !cancelProgress) {

                    Iterator<String> modificationIterator = modifiedSequences.keySet().iterator();

                    while (modificationIterator.hasNext()) {

                        String modifiedSequence = modificationIterator.next();

                        if (modifiedSequences.get(modifiedSequence).intValue() >= minimumNumberOfModificationPairs) {

                            ((DefaultTableModel) searchResultsJXTable.getModel()).insertRow(rowCounter,
                                    new Object[]{new Integer(++rowCounter),
                                        reducedIdentifications.get(0).getSequence(),
                                        modifiedSequence,
                                        reducedIdentifications.get(0).getSequence().length(),
                                        unmodifiedCounter,
                                        modifiedSequences.get(modifiedSequence),
                                        Boolean.valueOf(false)
                                    });

                            if (reducedIdentifications.get(0).getSequence().length() > maxSequenceLength) {
                                maxSequenceLength = reducedIdentifications.get(0).getSequence().length();
                            }

                            if (unmodifiedCounter > maxSequenceCount1) {
                                maxSequenceCount1 = unmodifiedCounter;
                            }

                            if (modifiedSequences.get(modifiedSequence) > maxSequenceCount2) {
                                maxSequenceCount2 = modifiedSequences.get(modifiedSequence);
                            }
                        }
                    }
                }
            }
        }

        ((JSparklinesBarChartTableCellRenderer) searchResultsJXTable.getColumn("Length").getCellRenderer()).setMaxValue(maxSequenceLength);
        ((JSparklinesBarChartTableCellRenderer) searchResultsJXTable.getColumn("#1").getCellRenderer()).setMaxValue(maxSequenceCount1);
        ((JSparklinesBarChartTableCellRenderer) searchResultsJXTable.getColumn("#2").getCellRenderer()).setMaxValue(maxSequenceCount2);

        return rowCounter > 0;
    }

    /**
     * Hides the second count column if search is not modification search.
     *
     * @param hide
     */
    private void hideSecondCountColumn(boolean hide) {

        resultsColumnToolTips = new Vector();
        resultsColumnToolTips.add(null);
        resultsColumnToolTips.add("Identified Peptide Sequence");
        resultsColumnToolTips.add("Identified Peptide Sequence with Modifications and Terminals");
        resultsColumnToolTips.add("Peptide Length");

        if (hide) {
            searchResultsJXTable.getColumnExt("#2").setVisible(false);
            resultsColumnToolTips.add("Number of Matches");
            resultsColumnToolTips.add(null);
        } else {
            searchResultsJXTable.getColumnExt("#2").setVisible(true);
            resultsColumnToolTips.add("Number of Unmodified Matches");
            resultsColumnToolTips.add("Number of Modified Matches");
        }
    }

    /**
     * Inserts the results of a general search in to the search table. Returns true if
     * at least one row was inserted.
     *
     * @return true if at least one row was inserted, false otherwise
     */
    private boolean insertGeneralSearchResults(ProgressDialog progressDialog) {

//        ArrayList<String> codetectedIdentifications = new ArrayList<String>();
//
//        if(writeToFile){
//            codetectedIdentifications = Temp.getSequences();
//        }

        int rowCounter = 0;

        Iterator<String> iterator = properties.getIdentificationMap().keySet().iterator();

//        progressDialog.setIntermidiate(false);
//        progressDialog.setMax(properties.getIdentificationMap().keySet().size());
//
//
//        try{
//
//            FileWriter fileBions = null, fileYions = null, fileBAndYions = null;
//            BufferedWriter writerBions = null, writerYions = null, writerBAndYions = null;
//
//            if(writeToFile){
//                // create the files to write the fragment ion data to
//                fileBions = new FileWriter("C:\\Harald\\b_ions.txt");
//                writerBions = new BufferedWriter(fileBions);
//
//                fileYions = new FileWriter("C:\\Harald\\y_ions.txt");
//                writerYions = new BufferedWriter(fileYions);
//
//                fileBAndYions = new FileWriter("C:\\Harald\\b_and_y_ions_all.txt");
//                writerBAndYions = new BufferedWriter(fileBAndYions);
//
//                // add headers to the files
//                writerBions.write("Sequence FragmentIon Q_TOF_Median ORBI_Median Q_TOF_Quartile_Distance ORBI_Quartile_Distance\n");
//                writerYions.write("Sequence FragmentIon Q_TOF_Median ORBI_Median Q_TOF_Quartile_Distance ORBI_Quartile_Distance\n");
//                writerBAndYions.write("instrument sequence identificationID fragmentIonID fragmentIonType fragmentIonNumber m/z delta_m/z intensity\n");
//            }

            int maxSequenceLength = 0;
            int maxSequenceCount1 = 0;

            while (iterator.hasNext()) {

//                progressDialog.setValue(rowCounter);

                String key = iterator.next();

                ArrayList<ReducedIdentification> tempList = properties.getIdentificationMap().get(key);

                ReducedIdentification temp = tempList.get(0);

                ((DefaultTableModel) searchResultsJXTable.getModel()).addRow(
                        new Object[]{new Integer(++rowCounter),
                            temp.getSequence(),
                            temp.getModifiedSequence(),
                            temp.getSequence().length(),
                            tempList.size(),
                            null,
                            Boolean.valueOf(false)
                        });

                if (temp.getSequence().length() > maxSequenceLength) {
                    maxSequenceLength = temp.getSequence().length();
                }

                if (tempList.size() > maxSequenceCount1) {
                    maxSequenceCount1 = temp.getSequence().length();
                }


//                if(codetectedIdentifications.contains(temp.getModifiedSequence()) && writeToFile){
//
//                    ArrayList<ReducedIdentification> qTofs = new ArrayList<ReducedIdentification>();
//                    ArrayList<ReducedIdentification> orbi = new ArrayList<ReducedIdentification>();
//
//                    for(int i=0; i<tempList.size(); i++){
//                        if(tempList.get(i).getInstrumentName().equalsIgnoreCase("Micromass Q-TOF") ||
//                                tempList.get(i).getInstrumentName().equalsIgnoreCase("Waters Q-TOF Premier")){
//                                qTofs.add(tempList.get(i));
//                        } else if(tempList.get(i).getInstrumentName().equalsIgnoreCase("Thermo-Finnigan orbitrap")){
//                            orbi.add(tempList.get(i));
//                        }
//                    }
//
//                    double[][] fragmentIonIntensitiesBIonsQTofs = new double[qTofs.size()][temp.getSequence().length() + 1];
//                    double[][] fragmentIonIntensitiesYIonsQTofs = new double[qTofs.size()][temp.getSequence().length() + 1];
//
//                    // q-tof
//                    for(int i=0; i<qTofs.size(); i++){
//
//                        ReducedIdentification currentIdentification = qTofs.get(i);
//
//                        if(currentIdentification != null){
//
//                            // get the fragment ions
//                            try{
//                                Vector<Fragmention> fragmentIons = (Vector<Fragmention>) Fragmention.getAllFragmentions(
//                                        getConnection(), (long) currentIdentification.getIdentificationId());
//
//                                for (int j = 0; j < fragmentIons.size(); j++) {
//
//                                    Fragmention tempFragmentIon = fragmentIons.get(j);
//
//                                    String fragmentIonType = tempFragmentIon.getIonname();
//
//                                    if(fragmentIonType.equalsIgnoreCase("b") || fragmentIonType.equalsIgnoreCase("y")){
//
//                                        double intensity = tempFragmentIon.getIntensity();
//
//                                        // normalize the intensity
//                                        intensity = intensity / currentIdentification.getTotalIntensity();
//
//                                        if(fragmentIonType.equalsIgnoreCase("b")){
//                                            fragmentIonIntensitiesBIonsQTofs[i][(int) tempFragmentIon.getFragmentionnumber()] = intensity;
//                                        } else { // has to be a y ion
//                                            fragmentIonIntensitiesYIonsQTofs[i][(int) tempFragmentIon.getFragmentionnumber()] = intensity;
//                                        }
//
//                                        // write to the combined file
//                                        writerBAndYions.write("Q-TOF " + currentIdentification.getModifiedSequence() + " "
//                                                + currentIdentification.getIdentificationId() + " " + tempFragmentIon.getFragmentionid() + " "
//                                                + tempFragmentIon.getIonname() + " " + tempFragmentIon.getFragmentionnumber() + " "
//                                                + tempFragmentIon.getMz() + " " + tempFragmentIon.getMassdelta() + " "
//                                                + intensity + "\n");
//                                    }
//                                }
//                            } catch (SQLException e) {
//                                System.out.println(e.toString());
//                                e.printStackTrace();
//                            }
//                        } else {
//                            System.out.println(temp.getSequence() + ": null");
//                        }
//                    }
//
//
//                    // same for the orbitrap
//                    double[][] fragmentIonIntensitiesBIonsOrbi = new double[orbi.size()][temp.getSequence().length() + 1];
//                    double[][] fragmentIonIntensitiesYIonsOrbi = new double[orbi.size()][temp.getSequence().length() + 1];
//
//                    for(int i=0; i<orbi.size(); i++){
//
//                        ReducedIdentification currentIdentification = orbi.get(i);
//
//                        if(currentIdentification != null){
//
//                            // get the fragment ions
//                            try{
//                                Vector<Fragmention> fragmentIons = (Vector<Fragmention>) Fragmention.getAllFragmentions(
//                                        getConnection(), (long) currentIdentification.getIdentificationId());
//
//                                for (int j = 0; j < fragmentIons.size(); j++) {
//
//                                    Fragmention tempFragmentIon = fragmentIons.get(j);
//
//                                    String fragmentIonType = tempFragmentIon.getIonname();
//
//                                    if(fragmentIonType.equalsIgnoreCase("b") || fragmentIonType.equalsIgnoreCase("y")){
//
//                                        double intensity = tempFragmentIon.getIntensity();
//
//                                        // normalize the intensity
//                                        intensity = intensity / currentIdentification.getTotalIntensity();
//
//                                        if(fragmentIonType.equalsIgnoreCase("b")){
//                                            fragmentIonIntensitiesBIonsOrbi[i][(int) tempFragmentIon.getFragmentionnumber()] = intensity;
//                                        } else{ // has to be a y ion
//                                            fragmentIonIntensitiesYIonsOrbi[i][(int) tempFragmentIon.getFragmentionnumber()] = intensity;
//                                        }
//
//                                        // write to the combined file
//                                        writerBAndYions.write("ORBI " + currentIdentification.getModifiedSequence() + " "
//                                                + currentIdentification.getIdentificationId() + " " + tempFragmentIon.getFragmentionid() + " "
//                                                + tempFragmentIon.getIonname() + " " + tempFragmentIon.getFragmentionnumber() + " "
//                                                + tempFragmentIon.getMz() + " " + tempFragmentIon.getMassdelta() + " "
//                                                + intensity + "\n");
//                                    }
//                                }
//                            } catch (SQLException e) {
//                                System.out.println(e.toString());
//                                e.printStackTrace();
//                            }
//                        } else {
//                            System.out.println(temp.getSequence() + ": null");
//                        }
//                    }
//
//
//                    // compare the set of values from the two instrument types
//                    for(int i=1; i<=temp.getSequence().length(); i++){
//
//                        // b ions
//                        double[] tempValues = new double[qTofs.size()];
//
//                        for(int j=0; j < qTofs.size(); j++){
//                            tempValues[j] = fragmentIonIntensitiesBIonsQTofs[j][i];
//                        }
//
//                        double medianQTof = StatUtils.percentile(tempValues, 50);
//                        double ratioQuartileDistancesQTof = StatUtils.percentile(tempValues, 75) - StatUtils.percentile(tempValues, 25);
//
//                        tempValues = new double[orbi.size()];
//
//                        for(int j=0; j<orbi.size(); j++){
//                            tempValues[j] = fragmentIonIntensitiesBIonsOrbi[j][i];
//                        }
//
//                        double medianOrbi = StatUtils.percentile(tempValues, 50);
//                        double ratioQuartileDistancesOrbi = StatUtils.percentile(tempValues, 75) - StatUtils.percentile(tempValues, 25);
//
//
//                        // print to file
//                        writerBions.write(temp.getModifiedSequence() + " b" + i + " "
//                                + medianQTof + " " + medianOrbi + " " + ratioQuartileDistancesQTof + " " + ratioQuartileDistancesOrbi + "\n");
//
//
//                        // y ions
//                        tempValues = new double[qTofs.size()];
//
//                        for(int j=0; j < qTofs.size(); j++){
//                            tempValues[j] = fragmentIonIntensitiesYIonsQTofs[j][i];
//                        }
//
//                        medianQTof = StatUtils.percentile(tempValues, 50);
//                        ratioQuartileDistancesQTof = StatUtils.percentile(tempValues, 75) - StatUtils.percentile(tempValues, 25);
//
//                        tempValues = new double[orbi.size()];
//
//                        for(int j=0; j<orbi.size(); j++){
//                            tempValues[j] = fragmentIonIntensitiesYIonsOrbi[j][i];
//                        }
//
//                        medianOrbi = StatUtils.percentile(tempValues, 50);
//                        ratioQuartileDistancesOrbi = StatUtils.percentile(tempValues, 75) - StatUtils.percentile(tempValues, 25);
//
//
//                        // print to file
//                        writerYions.write(temp.getModifiedSequence() + " y" + i + " "
//                                + medianQTof + " " + medianOrbi + " " + ratioQuartileDistancesQTof + " " + ratioQuartileDistancesOrbi + "\n");
//                    }
//                }
            }

            ((JSparklinesBarChartTableCellRenderer) searchResultsJXTable.getColumn("Length").getCellRenderer()).setMaxValue(maxSequenceLength);
            ((JSparklinesBarChartTableCellRenderer) searchResultsJXTable.getColumn("#1").getCellRenderer()).setMaxValue(maxSequenceCount1);
//
//            // close the file writers
//            writerBions.close();
//            fileBions.close();
//            writerYions.close();
//            fileYions.close();
//            writerBAndYions.close();
//            fileBAndYions.close();
//
//        } catch (Exception e){
//            System.out.println("Error writing to file...");
//            e.printStackTrace();
//        }

        return rowCounter > 0;
    }

    /**
     * Sorts the search result table on the first count column.
     */
    private void sortResultTableOnCount() {

        searchResultsJXTable.setSortable(true);

        // sort the column according to number of hits per sequence
        // a bit complicated due to wanting to keep the indices in the first column
        // ToDo: could perhaps be simplified??
        searchResultsJXTable.setSortOrder(4, SortOrder.DESCENDING);

        HashMap<Integer, Integer> indexOrder = new HashMap<Integer, Integer>();

        for (int i = 0; i < searchResultsJXTable.getRowCount(); i++) {
            indexOrder.put(((Integer) searchResultsJXTable.getValueAt(i, 0)).intValue() - 1, (i + 1));
        }

        searchResultsJXTable.setSortable(false);

        for (int i = 0; i < searchResultsJXTable.getRowCount(); i++) {
            ((DefaultTableModel) searchResultsJXTable.getModel()).setValueAt(indexOrder.get(i), i, 0);
        }

        searchResultsJXTable.setSortable(true);
        searchResultsJXTable.setSortOrder(4, SortOrder.DESCENDING);
    }

    /**
     * Enables or disables the search button based on the selection in the combo boxes.
     */
    private void enableSearchButton() {

        if (instrument1JComboBox.getSelectedIndex() != 0 &&
                nTermJComboBox.getSelectedIndex() != 0 &&
                cTermJComboBox.getSelectedIndex() != 0 &&
                chargeJComboBox.getSelectedIndex() != 0) {
            searchEnabled = dataLoaded;
        } else {
            searchEnabled = false;
        }
    }

    /**
     * Enables or disables the search button based on the selection in the instrument combo boxes.
     *
     * @param evt
     */
    private void instrument1JComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_instrument1JComboBoxActionPerformed
        if (instrument1JComboBox.getSelectedIndex() == 0) {
            instrument2JComboBox.setEnabled(false);
            instrument2JComboBox.setSelectedIndex(0);
            instrument3JComboBox.setEnabled(false);
            instrument3JComboBox.setSelectedIndex(0);
            searchEnabled = false;
        } else {

            if (instrument1JComboBox.getSelectedItem().toString().equalsIgnoreCase("Select All")) {
                instrument2JComboBox.setEnabled(false);
                instrument2JComboBox.setSelectedIndex(0);
                instrument3JComboBox.setEnabled(false);
                instrument3JComboBox.setSelectedIndex(0);
            } else {
                instrument2JComboBox.setEnabled(true);
                instrument2JComboBoxActionPerformed(null);
            }

            enableSearchButton();
        }
    }//GEN-LAST:event_instrument1JComboBoxActionPerformed

    /**
     * Enables or disables the search button based on the selection in the instrument combo boxes.
     *
     * @param evt
     */
    private void instrument2JComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_instrument2JComboBoxActionPerformed
        if (instrument2JComboBox.getSelectedIndex() == 0) {
            instrument3JComboBox.setEnabled(false);
            instrument3JComboBox.setSelectedIndex(0);
        } else {
            instrument3JComboBox.setEnabled(true);
        }
    }//GEN-LAST:event_instrument2JComboBoxActionPerformed

    /**
     * Enables or disables the search button based on the selection in the modification combo boxes.
     *
     * @param evt
     */
    private void modification1JComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modification1JComboBoxActionPerformed
        if (modification1JComboBox.getSelectedIndex() == 0) {
            modification2JComboBox.setEnabled(false);
            modification2JComboBox.setSelectedIndex(0);
            modification3JComboBox.setEnabled(false);
            modification3JComboBox.setSelectedIndex(0);
        } else {
            modification2JComboBox.setEnabled(true);
            modification2JComboBoxActionPerformed(null);
        }
    }//GEN-LAST:event_modification1JComboBoxActionPerformed

    /**
     * Enables or disables the search button based on the selection in the modification combo boxes.
     *
     * @param evt
     */
    private void modification2JComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modification2JComboBoxActionPerformed
        if (modification2JComboBox.getSelectedIndex() == 0) {
            modification3JComboBox.setEnabled(false);
            modification3JComboBox.setSelectedIndex(0);
        } else {
            modification3JComboBox.setEnabled(true);
        }
    }//GEN-LAST:event_modification2JComboBoxActionPerformed

    /**
     * @see #enableSearchButton()
     */
    private void nTermJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nTermJComboBoxActionPerformed
        enableSearchButton();
}//GEN-LAST:event_nTermJComboBoxActionPerformed

    /**
     * @see #enableSearchButton()
     */
    private void cTermJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cTermJComboBoxActionPerformed
        enableSearchButton();
}//GEN-LAST:event_cTermJComboBoxActionPerformed

    /**
     * @see #enableSearchButton()
     */
    private void chargeJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chargeJComboBoxActionPerformed
        enableSearchButton();
}//GEN-LAST:event_chargeJComboBoxActionPerformed

    /**
     * Enables or disables the search results button based on the selection in the search result combo box.
     *
     * @param evt
     */
    private void searchResultsJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchResultsJComboBoxActionPerformed
        if (searchResultsJComboBox.getSelectedIndex() != 0 &&
                properties.getCurrentlySelectedRowsInSearchTable().size() > 0) {
            searchResultsJButton.setEnabled(true);
            searchResultsJButton.setToolTipText(null);
        } else {
            searchResultsJButton.setEnabled(false);
            searchResultsJButton.setToolTipText(searchResultAnalysisButtonDisabledToolTip);
        }

        if (searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_MASS_ERROR_SCATTER_PLOT ||
                searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_MASS_ERROR_BUBBLE_PLOT ||
                searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_MASS_ERROR_BOX_PLOT) {
            daOrPpmSearchResultsJComboBox.setEnabled(true);
            combineSearchResultsJComboBox.setEnabled(true);
        } else {
            daOrPpmSearchResultsJComboBox.setEnabled(false);
            combineSearchResultsJComboBox.setEnabled(false);
        }

        if (searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_ION_PROBABILITY_PLOT) {
            daOrPpmSearchResultsJComboBox.setEnabled(false);
            combineSearchResultsJComboBox.setEnabled(true);
        }

        if (searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_INTENSITY_BOX_PLOT) {
            combineSearchResultsJComboBox.setSelectedItem("Single");
        }

        if (searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_ION_HEAT_MAP) {
            combineSearchResultsJComboBox.setSelectedItem("Combine");
        }
    }//GEN-LAST:event_searchResultsJComboBoxActionPerformed

    /**
     * Performs the analysis selected in the search result combo box.
     *
     * @param evt
     */
    private void searchResultsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchResultsJButtonActionPerformed

        // ToDo: a lot of the code below is repeated for each plotting type and ought
        //       to be combined to simplify the code

        cancelProgress = false;

        // if more than 10 internal frames are to be opened, first ask if the user wants to continue or not
        if(properties.getCurrentlySelectedRowsInSearchTable().size() > 10 &&
                combineSearchResultsJComboBox.getSelectedIndex() == Properties.SINGLE_PLOT &&
                searchResultsJComboBox.getSelectedIndex() != Properties.SEARCH_RESULTS_SHOW_INDIVIDUAL_SPECTRA &&
                searchResultsJComboBox.getSelectedIndex() != Properties.SEARCH_RESULTS_INTENSITY_CORRELATION &&
                searchResultsJComboBox.getSelectedIndex() != Properties.SEARCH_RESULTS_META_INTENSITY_PLOTS){
            int option = JOptionPane.showConfirmDialog(this, "This will open " +
                    properties.getCurrentlySelectedRowsInSearchTable().size()
                    + " plots/analysis frames.\nAre you sure you want to continue?", "Continue?",
                    JOptionPane.YES_NO_OPTION);

            if(option == JOptionPane.NO_OPTION){
                cancelProgress = true;
            }
        }

        // get the wanted plot label type from the user
        if (!cancelProgress &&
                searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_MASS_ERROR_SCATTER_PLOT ||
                searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_MASS_ERROR_BUBBLE_PLOT) {
            new PlotLabelSelection(this, true, currentDataSetIsFromMsLims);
        }

        if (!cancelProgress) {

            progressDialog = new ProgressDialog(this, this, true);

            new Thread(new Runnable() {

                public void run() {
                    progressDialog.setIntermidiate(false);
                    progressDialog.setValue(0);
                    progressDialog.setTitle("Running Analysis. Please Wait...");
                    progressDialog.setVisible(true);
                }
            }, "ProgressDialog").start();

            new Thread("IdentificationsThread") {

                @Override
                public void run() {

                    // ToDo: a lot of the code below is repeated for each plotting type and ought
                    //       to be combined to simplify the code

                    int rowCounter = 0;
                    boolean singleSearch = !searchResultsJXTable.getColumnExt("#2").isVisible();

                    progressDialog.setValue(0);
                    progressDialog.setMax(properties.getCurrentlySelectedRowsInSearchTable().size());

                    if (searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_SHOW_INDIVIDUAL_SPECTRA) {

                        spectraJXTaskPane.setExpanded(true);

                        // empty the spectra table
                        ((DefaultTableModel) spectraJXTable.getModel()).setRowCount(0);
                        spectraJScrollPane.getVerticalScrollBar().setValue(0);
                        spectraJXTable.resetSortOrder();
                        properties.setCurrentlySelectedRowsInSpectraTable(new ArrayList<SpectrumTableRow>());
                        properties.setSelectAllSpectra(true);
                        selectAllSpectrtaJMenuItem.setText("Select All");
                        spectraJComboBoxActionPerformed(null);

                        int maxSequenceLength = 0;

                        for (int i = 0; i < properties.getCurrentlySelectedRowsInSearchTable().size() && !cancelProgress; i++) {

                            IdentificationTableRow currentlySelectedRow =
                                    properties.getCurrentlySelectedRowsInSearchTable().get(i);
                            String currentSequence = currentlySelectedRow.getSequence();
                            String currentModifiedSequence = currentlySelectedRow.getModifiedSequence();

                            progressDialog.setString("" + (i + 1) + "/" +
                                    properties.getCurrentlySelectedRowsInSearchTable().size());

                            int localCounter = 0;

                            // check for search type. if count 2 exists there are more than one id per line
                            if (!singleSearch) {

                                progressDialog.setMax(currentlySelectedRow.getCountA() + currentlySelectedRow.getCountB());
                                progressDialog.setValue(localCounter);

                                ArrayList<ReducedIdentification> currentIdentifications =
                                        properties.getIdentificationMap().get(currentSequence);

                                // add the unmodified sequences
                                for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {

                                    ReducedIdentification currentId = currentIdentifications.get(j);

                                    if (!currentId.isModified()) {

                                        progressDialog.setValue(++localCounter);

                                        ((DefaultTableModel) spectraJXTable.getModel()).insertRow(rowCounter++,
                                                new Object[]{rowCounter,
                                                    currentId.getIdentificationId(),
                                                    currentId.getSpectrumFileId(),
                                                    currentId.getSequence(),
                                                    currentId.getModifiedSequence(),
                                                    currentId.getSequence().length(),
                                                    currentId.getInstrumentName(),
                                                    Boolean.valueOf(false)
                                                });

                                        if (currentId.getSequence().length() > maxSequenceLength) {
                                            maxSequenceLength = currentId.getSequence().length();
                                        }
                                    }
                                }

                                // add the modified sequences
                                for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {

                                    ReducedIdentification currentId = currentIdentifications.get(j);

                                    if (currentId.getModifiedSequence().equalsIgnoreCase(currentModifiedSequence)) {

                                        progressDialog.setValue(++localCounter);

                                        ((DefaultTableModel) spectraJXTable.getModel()).insertRow(rowCounter++,
                                                new Object[]{rowCounter,
                                                    currentId.getIdentificationId(),
                                                    currentId.getSpectrumFileId(),
                                                    currentId.getSequence(),
                                                    currentId.getModifiedSequence(),
                                                    currentId.getSequence().length(),
                                                    currentId.getInstrumentName(),
                                                    Boolean.valueOf(false)
                                                });

                                        if (currentId.getSequence().length() > maxSequenceLength) {
                                            maxSequenceLength = currentId.getSequence().length();
                                        }
                                    }
                                }
                            } else {
                                ArrayList<ReducedIdentification> currentIdentifications =
                                        properties.getIdentificationMap().get(currentModifiedSequence);

                                progressDialog.setMax(currentlySelectedRow.getCountA());
                                progressDialog.setValue(localCounter);

                                for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {

                                    ReducedIdentification currentId = currentIdentifications.get(j);

                                    progressDialog.setValue(++localCounter);

                                    ((DefaultTableModel) spectraJXTable.getModel()).insertRow(rowCounter++,
                                            new Object[]{rowCounter,
                                                currentId.getIdentificationId(),
                                                currentId.getSpectrumFileId(),
                                                currentId.getSequence(),
                                                currentId.getModifiedSequence(),
                                                currentId.getSequence().length(),
                                                currentId.getInstrumentName(),
                                                Boolean.valueOf(false)
                                            });

                                    if (currentId.getSequence().length() > maxSequenceLength) {
                                        maxSequenceLength = currentId.getSequence().length();
                                    }
                                }
                            }
                        }

                        ((JSparklinesBarChartTableCellRenderer) spectraJXTable.getColumn("Length").getCellRenderer()).setMaxValue(maxSequenceLength);

                        if (!cancelProgress) {
                            spectraJXTable.setRowSelectionInterval(0, 0);
                            resultsJScrollPane.getVerticalScrollBar().setValue(0);
                        } else {
                            ((DefaultTableModel) spectraJXTable.getModel()).setRowCount(0);
                            properties.setCurrentlySelectedRowsInSpectraTable(new ArrayList<SpectrumTableRow>());
                            properties.setSelectAllSpectra(true);
                            selectAllSpectrtaJMenuItem.setText("Select All");
                            spectraJComboBoxActionPerformed(null);
                        }
                    } else if (searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_INTENSITY_BOX_PLOT) {

                        plotsAnalysesJXTaskPane.setExpanded(true);
                        searchResultsJXTaskPane.setExpanded(false);
                        spectraJXTaskPane.setExpanded(false);

                        int totalSpectraCount = 0;
                        int totalFragmentIons = 0;

                        try {

                            for (int i = 0; i < properties.getCurrentlySelectedRowsInSearchTable().size() && !cancelProgress; i++) {

                                progressDialog.setIntermidiate(false);
                                progressDialog.setTitle("Running Analysis. Please Wait...");
                                progressDialog.setValue(i + 1);
                                progressDialog.setString("(" + (i + 1) + "/" +
                                        properties.getCurrentlySelectedRowsInSearchTable().size() + ")");

                                IdentificationTableRow currentlySelectedRow =
                                        properties.getCurrentlySelectedRowsInSearchTable().get(i);
                                String currentSequence = currentlySelectedRow.getSequence();
                                String currentModifiedSequence = currentlySelectedRow.getModifiedSequence();

                                DefaultBoxAndWhiskerCategoryDataset dataSet = new DefaultBoxAndWhiskerCategoryDataset();

                                int unmodifiedCounter = 0;
                                int modifiedCounter = 0;

                                // check for search type
                                if (!singleSearch) {

                                    ArrayList<ReducedIdentification> currentIdentifications =
                                            properties.getIdentificationMap().get(currentSequence);

                                    unmodifiedCounter = 0;
                                    modifiedCounter = 0;

                                    // find the number of unmodified sequence
                                    for (int j = 0; j < currentIdentifications.size(); j++) {
                                        ReducedIdentification currentId = currentIdentifications.get(j);

                                        if (!currentId.isModified()) {
                                            unmodifiedCounter++;
                                        }
                                    }

                                    // find the number of modified sequences
                                    for (int j = 0; j < currentIdentifications.size(); j++) {
                                        ReducedIdentification currentId = currentIdentifications.get(j);

                                        if (currentId.getModifiedSequence().equalsIgnoreCase(currentModifiedSequence)) {
                                            modifiedCounter++;
                                        }
                                    }

                                    double[][] bUnmodIntensities = new double[currentSequence.length()][unmodifiedCounter];
                                    double[][] bModIntensities = new double[currentSequence.length()][modifiedCounter];
                                    double[][] yUnmodIntensities = new double[currentSequence.length()][unmodifiedCounter];
                                    double[][] yModIntensities = new double[currentSequence.length()][modifiedCounter];

                                    progressDialog.setTitle("Extracting Fragment Ions - Unmodifed. Please Wait...");
                                    progressDialog.setMax(unmodifiedCounter);
                                    progressDialog.setValue(0);

                                    unmodifiedCounter = 0;

                                    // add the unmodified sequences
                                    for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {

                                        ReducedIdentification currentId = currentIdentifications.get(j);

                                        if (!currentId.isModified()) {

                                            progressDialog.setValue(unmodifiedCounter + 1);

                                            if (currentDataSetIsFromMsLims) {
                                                // get all unmodified b fragments
                                                totalFragmentIons += getAllFragmentsFromMsLims(currentId, bUnmodIntensities, unmodifiedCounter, Properties.B_ION);

                                                // get all unmodified y fragments
                                                totalFragmentIons += getAllFragmentsFromMsLims(currentId, yUnmodIntensities, unmodifiedCounter, Properties.Y_ION);
                                            } else {
                                                // get all unmodified b fragments
                                                totalFragmentIons += getAllFragmentsFromFragmentIonsFile(currentId, bUnmodIntensities, unmodifiedCounter, "b");

                                                // get all unmodified y fragments
                                                totalFragmentIons += getAllFragmentsFromFragmentIonsFile(currentId, yUnmodIntensities, unmodifiedCounter, "y");
                                            }

                                            unmodifiedCounter++;
                                        }
                                    }

                                    progressDialog.setTitle("Extracting Fragment Ions - Modified. Please Wait...");
                                    progressDialog.setMax(modifiedCounter);
                                    progressDialog.setValue(0);

                                    modifiedCounter = 0;

                                    // add the modified sequences
                                    for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {

                                        ReducedIdentification currentId = currentIdentifications.get(j);

                                        if (currentId.getModifiedSequence().equalsIgnoreCase(currentModifiedSequence)) {

                                            progressDialog.setValue(modifiedCounter + 1);

                                            if (currentDataSetIsFromMsLims) {
                                                // get all modified b fragments
                                                totalFragmentIons += getAllFragmentsFromMsLims(currentId, bModIntensities, modifiedCounter, Properties.B_ION);

                                                // get all modified y fragments
                                                totalFragmentIons += getAllFragmentsFromMsLims(currentId, yModIntensities, modifiedCounter, Properties.Y_ION);
                                            } else {
                                                // get all modified b fragments
                                                totalFragmentIons += getAllFragmentsFromFragmentIonsFile(currentId, bModIntensities, modifiedCounter, "b");

                                                // get all modified y fragments
                                                totalFragmentIons += getAllFragmentsFromFragmentIonsFile(currentId, yModIntensities, modifiedCounter, "y");
                                            }

                                            modifiedCounter++;
                                        }
                                    }

                                    progressDialog.setIntermidiate(true);
                                    progressDialog.setString("(" + (i + 1) + "/" +
                                            properties.getCurrentlySelectedRowsInSearchTable().size() + ")");
                                    progressDialog.setTitle("Creating Box Plot. Please Wait...");

                                    // lists of all non null b and y values
                                    ArrayList<Double> nonNullBUnmodValues = new ArrayList<Double>();
                                    ArrayList<Double> nonNullBModValues = new ArrayList<Double>();
                                    ArrayList<Double> nonNullYUnmodValues = new ArrayList<Double>();
                                    ArrayList<Double> nonNullYModValues = new ArrayList<Double>();

                                    for (int k = 0; k < yUnmodIntensities.length; k++) {

                                        nonNullBUnmodValues = new ArrayList<Double>();
                                        nonNullBModValues = new ArrayList<Double>();
                                        nonNullYUnmodValues = new ArrayList<Double>();
                                        nonNullYModValues = new ArrayList<Double>();

                                        // get the list of non null b and y values, and get the average values
                                        // NB: the y fragments are flipped in the returned lists to make it
                                        //     easier to compare b and y ions in the same plot, i.e. y 1 becomes y n,
                                        //     y 2 becomes y (n-1), etc.
                                        double averageBUnmodValue = PlotUtil.getNonNullBFragments(nonNullBUnmodValues, bUnmodIntensities, k);
                                        double averageBModValue = PlotUtil.getNonNullBFragments(nonNullBModValues, bModIntensities, k);
                                        double averageYUnmodValue = PlotUtil.getNonNullYFragments(nonNullYUnmodValues, yUnmodIntensities, k);
                                        double averageYModValue = PlotUtil.getNonNullYFragments(nonNullYModValues, yModIntensities, k);

                                        String currentCategory = "" + currentSequence.charAt(k) + (k + 1);

                                        // add the b ions to the box plot data set
                                        double[] bUnmodValues = PlotUtil.addValuesToBoxPlot(dataSet, nonNullBUnmodValues, "b ions - unmod.",
                                                currentCategory);
                                        double[] bModValues = PlotUtil.addValuesToBoxPlot(dataSet, nonNullBModValues, "b ions - mod.",
                                                currentCategory);

                                        // add the y ions to the box plot data set
                                        double[] yUnmodValues = PlotUtil.addValuesToBoxPlot(dataSet, nonNullYUnmodValues, "y ions - unmod.",
                                                currentCategory);
                                        double[] yModValues = PlotUtil.addValuesToBoxPlot(dataSet, nonNullYModValues, "y ions - mod.",
                                                currentCategory);
                                    }
                                } else {
                                    ArrayList<ReducedIdentification> currentIdentifications =
                                            properties.getIdentificationMap().get(currentModifiedSequence);

                                    double[][] bIntensities = new double[currentSequence.length()][currentIdentifications.size()];
                                    double[][] yIntensities = new double[currentSequence.length()][currentIdentifications.size()];

                                    progressDialog.setTitle("Extracting Fragment Ions. Please Wait...");
                                    progressDialog.setMax(currentIdentifications.size());
                                    progressDialog.setValue(0);

                                    totalSpectraCount = currentIdentifications.size();

                                    // get all b and y fragment ions and store them in
                                    // the bIntensities and yIntensities tables, one row per identification
                                    for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {

                                        progressDialog.setValue(j);

                                        if (currentDataSetIsFromMsLims) {
                                            // get all b fragments
                                            totalFragmentIons += getAllFragmentsFromMsLims(currentIdentifications.get(j), bIntensities, j, Properties.B_ION);

                                            // get all y fragments
                                            totalFragmentIons += getAllFragmentsFromMsLims(currentIdentifications.get(j), yIntensities, j, Properties.Y_ION);
                                        } else {
                                            // get all b fragments
                                            totalFragmentIons += getAllFragmentsFromFragmentIonsFile(currentIdentifications.get(j), bIntensities, j, "b");

                                            // get all y fragments
                                            totalFragmentIons += getAllFragmentsFromFragmentIonsFile(currentIdentifications.get(j), yIntensities, j, "y");
                                        }
                                    }

                                    progressDialog.setIntermidiate(true);
                                    progressDialog.setString("(" + (i + 1) + "/" +
                                            properties.getCurrentlySelectedRowsInSearchTable().size() + ")");
                                    progressDialog.setTitle("Creating Box Plot. Please Wait...");

                                    // lists of all non null B and Y values
                                    ArrayList<Double> nonNullBValues = new ArrayList<Double>();
                                    ArrayList<Double> nonNullYValues = new ArrayList<Double>();

                                    for (int k = 0; k < yIntensities.length; k++) {

                                        nonNullBValues = new ArrayList<Double>();
                                        nonNullYValues = new ArrayList<Double>();

                                        // get the list of non null b and y values, and get the average values
                                        // NB: the y fragments are flipped in the returned lists to make it
                                        //     easier to compare b and y ions in the same plot, i.e. y 1 becomes y n,
                                        //     y 2 becomes y (n-1), etc.
                                        double averageBValue = PlotUtil.getNonNullBFragments(nonNullBValues, bIntensities, k);
                                        double averageYValue = PlotUtil.getNonNullYFragments(nonNullYValues, yIntensities, k);
                                        
                                        // add the b ions to the box plot data set
                                        double[] bValues = PlotUtil.addValuesToBoxPlot(dataSet, nonNullBValues, "b ions",
                                                "" + currentSequence.charAt(k) + (k + 1));

                                        // add the y ions to the box plot data set
                                        double[] yValues = PlotUtil.addValuesToBoxPlot(dataSet, nonNullYValues, "y ions",
                                                "" + currentSequence.charAt(k) + (k + 1));
                                    }
                                }

                                CategoryPlot plot = PlotUtil.getCategoryPlot(dataSet, "Sequence", "Intensity");

                                // set the data series colors
                                for (int j = 0; j < plot.getDataset().getRowKeys().size(); j++) {
                                    plot.getRenderer().setSeriesPaint(j, Util.determineFragmentIonColor(
                                            plot.getDataset().getRowKey(j).toString()));
                                }

                                // add a category marker for the modified residue
                                if (!singleSearch) {
                                    PlotUtil.addModificationMarker(currentModifiedSequence, plot,
                                            properties.showMarkers(), properties);
                                }

                                if (!cancelProgress) {
                                    JFreeChart chart = new JFreeChart(
                                            null,
                                            new Font("SansSerif", Font.BOLD, 10),
                                            plot,
                                            true);

                                    chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));
                                    chart.getLegend().setPosition(RectangleEdge.BOTTOM);

                                    chart.setBackgroundPaint(PlotUtil.chartBackgroundColor);

                                    if (!properties.showLegend()) {
                                        chart.getLegend().setVisible(false);
                                    }

                                    ChartPanel chartPanel = new ChartPanel(chart);

                                    String internalFrameTitle = "" + currentModifiedSequence;
                                    String plotType = "BoxPlot";

                                    // if modification_search add number of unmodified and modifed spectra to title
                                    if (!singleSearch) {
                                        internalFrameTitle += " (" + unmodifiedCounter + " unmod. spectra/"
                                                + modifiedCounter + " mod. spectra, " +
                                                totalFragmentIons + " fragment ions)";
                                        plotType = "BoxPlot_modification";
                                    } else {
                                        internalFrameTitle += " (" + totalSpectraCount + " spectra, " +
                                                totalFragmentIons + " fragment ions)";
                                    }

                                    FragmentationAnalyzerJInternalFrame internalFrame = new FragmentationAnalyzerJInternalFrame(
                                            internalFrameTitle, true, true, true, chartPanel, plotType, internalFrameUniqueIdCounter);
                                    internalFrame.add(chartPanel);

                                    insertInternalFrame(internalFrame);
                                    properties.getAllChartFrames().put(internalFrameUniqueIdCounter, chart);
                                    internalFrameUniqueIdCounter++;

                                    // update the visible box plot fragment ion selection
                                    updateVisibleFragmentIonBoxPlotSelection();
                                }
                            }
                        } catch (SQLException e) {
                            JOptionPane.showMessageDialog(null,
                                    "An error occured when accessing the database.\n" +
                                    "See ../Properties/ErrorLog.txt for more details.",
                                    "Error Accessing Database", JOptionPane.ERROR_MESSAGE);
                            Util.writeToErrorLog("Error when extracing fragment ions: ");
                            e.printStackTrace();
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(null,
                                    "An error occured when building a box plot.\n" +
                                    "See ../Properties/ErrorLog.txt for more details.",
                                    "Error Building Box Plot", JOptionPane.ERROR_MESSAGE);
                            Util.writeToErrorLog("Error when building box plot: ");
                            e.printStackTrace();
                        }
                    } else if(searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_INTENSITY_CORRELATION){

                        if (!singleSearch) {
                            JOptionPane.showMessageDialog(null, "This analysis type is only supported for a General Search.",
                                    "Analysis Type Not Supported", JOptionPane.OK_OPTION);
                        } else {

                            plotsAnalysesJXTaskPane.setExpanded(true);
                            searchResultsJXTaskPane.setExpanded(false);
                            spectraJXTaskPane.setExpanded(false);

                            try {

                                DefaultXYDataset xyValues = new DefaultXYDataset();

                                ArrayList<Double> q3_to_q1_distancesBIons = new ArrayList<Double>();
                                ArrayList<Double> mediansBIons = new ArrayList<Double>();

                                ArrayList<Double> q3_to_q1_distancesYIons = new ArrayList<Double>();
                                ArrayList<Double> mediansYIons = new ArrayList<Double>();

                                for (int i = 0; i < properties.getCurrentlySelectedRowsInSearchTable().size() && !cancelProgress; i++) {

                                    progressDialog.setIntermidiate(false);
                                    progressDialog.setTitle("Running Analysis. Please Wait...");
                                    progressDialog.setValue(i + 1);
                                    progressDialog.setString("(" + (i + 1) + "/" +
                                            properties.getCurrentlySelectedRowsInSearchTable().size() + ")");

                                    IdentificationTableRow currentlySelectedRow =
                                            properties.getCurrentlySelectedRowsInSearchTable().get(i);
                                    String currentSequence = currentlySelectedRow.getSequence();
                                    String currentModifiedSequence = currentlySelectedRow.getModifiedSequence();

                                    DefaultBoxAndWhiskerCategoryDataset dataSet = new DefaultBoxAndWhiskerCategoryDataset();

                                    ArrayList<ReducedIdentification> currentIdentifications =
                                            properties.getIdentificationMap().get(currentModifiedSequence);

                                    double[][] bIntensities = new double[currentSequence.length()][currentIdentifications.size()];
                                    double[][] yIntensities = new double[currentSequence.length()][currentIdentifications.size()];

                                    progressDialog.setTitle("Extracting Fragment Ions. Please Wait...");
                                    progressDialog.setMax(currentIdentifications.size());
                                    progressDialog.setValue(0);


                                    // get all b and y fragment ions and store them in
                                    // the bIntensities and yIntensities tables, one row per identification
                                    for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {

                                        progressDialog.setValue(j);

                                        if (currentDataSetIsFromMsLims) {
                                            // get all b fragments
                                            getAllFragmentsFromMsLims(currentIdentifications.get(j), bIntensities, j, Properties.B_ION);

                                            // get all y fragments
                                            getAllFragmentsFromMsLims(currentIdentifications.get(j), yIntensities, j, Properties.Y_ION);
                                        } else {
                                            // get all b fragments
                                            getAllFragmentsFromFragmentIonsFile(currentIdentifications.get(j), bIntensities, j, "b");

                                            // get all y fragments
                                            getAllFragmentsFromFragmentIonsFile(currentIdentifications.get(j), yIntensities, j, "y");
                                        }
                                    }

                                    progressDialog.setIntermidiate(true);
                                    progressDialog.setString("(" + (i + 1) + "/" +
                                            properties.getCurrentlySelectedRowsInSearchTable().size() + ")");
                                    progressDialog.setTitle("Creating Box Plot. Please Wait...");

                                    // lists of all non null B and Y values
                                    ArrayList<Double> nonNullBValues = new ArrayList<Double>();
                                    ArrayList<Double> nonNullYValues = new ArrayList<Double>();


                                    for (int k = 0; k < yIntensities.length; k++) {

                                        nonNullBValues = new ArrayList<Double>();
                                        nonNullYValues = new ArrayList<Double>();

                                        // get the list of non null b and y values, and get the average values
                                        // NB: the y fragments are flipped in the returned lists to make it
                                        //     easier to compare b and y ions in the same plot, i.e. y 1 becomes y n,
                                        //     y 2 becomes y (n-1), etc.
                                        double averageBValue = PlotUtil.getNonNullBFragments(nonNullBValues, bIntensities, k);
                                        double averageYValue = PlotUtil.getNonNullYFragments(nonNullYValues, yIntensities, k);

                                        // add the b ions to the box plot data set
                                        double[] bValues = PlotUtil.addValuesToBoxPlot(dataSet, nonNullBValues, "b ions",
                                                "" + currentSequence.charAt(k) + (k + 1));

                                        // add the y ions to the box plot data set
                                        double[] yValues = PlotUtil.addValuesToBoxPlot(dataSet, nonNullYValues, "y ions",
                                                "" + currentSequence.charAt(k) + (k + 1));
                                    }


                                    // get the b-ions
                                    Iterator<String> columnIterator = dataSet.getColumnKeys().iterator();

                                    while(columnIterator.hasNext()){

                                        String columnKey = columnIterator.next();

                                        double q3_to_q1_distance = dataSet.getQ3Value("b ions", columnKey).doubleValue()
                                                - dataSet.getQ1Value("b ions", columnKey).doubleValue();
                                        double median = dataSet.getMedianValue("b ions", columnKey).doubleValue();

                                        q3_to_q1_distancesBIons.add(q3_to_q1_distance);
                                        mediansBIons.add(median);
                                    }

                                    // get the y-ions
                                    Iterator<String> columnIterator2 = dataSet.getColumnKeys().iterator();

                                    while(columnIterator2.hasNext()){

                                        String columnKey = columnIterator2.next();

                                        double q3_to_q1_distance = dataSet.getQ3Value("y ions", columnKey).doubleValue()
                                                - dataSet.getQ1Value("y ions", columnKey).doubleValue();
                                        double median = dataSet.getMedianValue("y ions", columnKey).doubleValue();

                                        q3_to_q1_distancesYIons.add(q3_to_q1_distance);
                                        mediansYIons.add(median);
                                    }
                                }

                                if (!cancelProgress) {

                                     // add the data to the plot

                                     // b ions
                                     double[][] tempXYData = new double[2][mediansBIons.size()];

                                     for(int j=0; j < mediansBIons.size(); j++){
                                         tempXYData[0][j] = mediansBIons.get(j);
                                         tempXYData[1][j] = q3_to_q1_distancesBIons.get(j);
                                     }

                                     xyValues.addSeries("b ions", tempXYData);

                                     // y ions
                                     tempXYData = new double[2][mediansYIons.size()];

                                     for(int j=0; j < mediansYIons.size(); j++){
                                         tempXYData[0][j] = mediansYIons.get(j);
                                         tempXYData[1][j] = q3_to_q1_distancesYIons.get(j);
                                     }

                                     xyValues.addSeries("y ions", tempXYData);


                                     // create the chart
                                    JFreeChart chart = PlotUtil.getScatterPlotChart(
                                            xyValues, "Median Intensity", "Q3 - Q1", true, properties);

                                    chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));
                                    chart.getLegend().setPosition(RectangleEdge.BOTTOM);

                                    chart.setBackgroundPaint(PlotUtil.chartBackgroundColor);

                                    if (!properties.showLegend()) {
                                        chart.getLegend().setVisible(false);
                                    }

                                    // set the data series colors
                                    for (int i = 0; i < ((XYPlot) chart.getPlot()).getDataset().getSeriesCount(); i++) {
                                        ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(
                                                i, Util.determineFragmentIonColor(
                                                    ((XYPlot) chart.getPlot()).getDataset().getSeriesKey(i).toString()));
                                    }

                                    ChartPanel chartPanel = new ChartPanel(chart);

                                    String internalFrameTitle = "Intensity Variability";
                                    String plotType = "ScatterPlot";

                                    FragmentationAnalyzerJInternalFrame internalFrame = new FragmentationAnalyzerJInternalFrame(
                                            internalFrameTitle, true, true, true, chartPanel, plotType, internalFrameUniqueIdCounter);
                                    internalFrame.add(chartPanel);

                                    insertInternalFrame(internalFrame);
                                    properties.getAllChartFrames().put(internalFrameUniqueIdCounter, chart);
                                    internalFrameUniqueIdCounter++;

                                    // update the visible box plot fragment ion selection
                                    updateVisibleFragmentIonBoxPlotSelection();
                                }
                            } catch (SQLException e) {
                                JOptionPane.showMessageDialog(null,
                                        "An error occured when accessing the database.\n" +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Accessing Database", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when extracing fragment ions: ");
                                e.printStackTrace();
                            } catch (IOException e) {
                                JOptionPane.showMessageDialog(null,
                                        "An error occured when building a box plot.\n" +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Building Box Plot", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when building box plot: ");
                                e.printStackTrace();
                            }
                        }
                    } else if(searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_META_INTENSITY_PLOTS){

                        if (!singleSearch) {
                            JOptionPane.showMessageDialog(null, "This analysis type is only supported for a General Search.",
                                    "Analysis Type Not Supported", JOptionPane.OK_OPTION);
                        } else {

                            plotsAnalysesJXTaskPane.setExpanded(true);
                            searchResultsJXTaskPane.setExpanded(false);
                            spectraJXTaskPane.setExpanded(false);

                            try {

                                DefaultBoxAndWhiskerCategoryDataset dataSetCombined = new DefaultBoxAndWhiskerCategoryDataset();

                                DefaultBoxAndWhiskerCategoryDataset dataSetCombinedMedians = new DefaultBoxAndWhiskerCategoryDataset();
                                DefaultBoxAndWhiskerCategoryDataset dataSetCombinedQuartileDistance = new DefaultBoxAndWhiskerCategoryDataset();

                                ArrayList<Double> mediansB = new ArrayList();
                                ArrayList<Double> mediansY = new ArrayList();
                                ArrayList<Double> quartileDistancesB = new ArrayList();
                                ArrayList<Double> quartileDistancesY = new ArrayList();

                                for (int i = 0; i < properties.getCurrentlySelectedRowsInSearchTable().size() && !cancelProgress; i++) {

                                    progressDialog.setIntermidiate(false);
                                    progressDialog.setTitle("Running Analysis. Please Wait...");
                                    progressDialog.setValue(i + 1);
                                    progressDialog.setString("(" + (i + 1) + "/" +
                                            properties.getCurrentlySelectedRowsInSearchTable().size() + ")");

                                    IdentificationTableRow currentlySelectedRow =
                                            properties.getCurrentlySelectedRowsInSearchTable().get(i);
                                    String currentSequence = currentlySelectedRow.getSequence();
                                    String currentModifiedSequence = currentlySelectedRow.getModifiedSequence();

                                    DefaultBoxAndWhiskerCategoryDataset dataSet = new DefaultBoxAndWhiskerCategoryDataset();

                                    ArrayList<ReducedIdentification> currentIdentifications =
                                            properties.getIdentificationMap().get(currentModifiedSequence);

                                    double[][] bIntensities = new double[currentSequence.length()][currentIdentifications.size()];
                                    double[][] yIntensities = new double[currentSequence.length()][currentIdentifications.size()];

                                    progressDialog.setTitle("Extracting Fragment Ions. Please Wait...");
                                    progressDialog.setMax(currentIdentifications.size());
                                    progressDialog.setValue(0);


                                    // get all b and y fragment ions and store them in
                                    // the bIntensities and yIntensities tables, one row per identification
                                    for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {

                                        progressDialog.setValue(j);

                                        if (currentDataSetIsFromMsLims) {
                                            // get all b fragments
                                            getAllFragmentsFromMsLims(currentIdentifications.get(j), bIntensities, j, Properties.B_ION);

                                            // get all y fragments
                                            getAllFragmentsFromMsLims(currentIdentifications.get(j), yIntensities, j, Properties.Y_ION);
                                        } else {
                                            // get all b fragments
                                            getAllFragmentsFromFragmentIonsFile(currentIdentifications.get(j), bIntensities, j, "b");

                                            // get all y fragments
                                            getAllFragmentsFromFragmentIonsFile(currentIdentifications.get(j), yIntensities, j, "y");
                                        }
                                    }

                                    progressDialog.setIntermidiate(true);
                                    progressDialog.setString("(" + (i + 1) + "/" +
                                            properties.getCurrentlySelectedRowsInSearchTable().size() + ")");
                                    progressDialog.setTitle("Creating Box Plot. Please Wait...");

                                    // lists of all non null B and Y values
                                    ArrayList<Double> nonNullBValues = new ArrayList<Double>();
                                    ArrayList<Double> nonNullYValues = new ArrayList<Double>();


                                    for (int k = 0; k < yIntensities.length; k++) {

                                        nonNullBValues = new ArrayList<Double>();
                                        nonNullYValues = new ArrayList<Double>();

                                        // get the list of non null b and y values, and get the average values
                                        // NB: the y fragments are flipped in the returned lists to make it
                                        //     easier to compare b and y ions in the same plot, i.e. y 1 becomes y n,
                                        //     y 2 becomes y (n-1), etc.
                                        double averageBValue = PlotUtil.getNonNullBFragments(nonNullBValues, bIntensities, k);
                                        double averageYValue = PlotUtil.getNonNullYFragments(nonNullYValues, yIntensities, k);

                                        // add the b ions to the box plot data set
                                        double[] bValues = PlotUtil.addValuesToBoxPlot(dataSet, nonNullBValues, "b ions",
                                                "" + currentSequence.charAt(k) + (k + 1));

                                        // add the y ions to the box plot data set
                                        double[] yValues = PlotUtil.addValuesToBoxPlot(dataSet, nonNullYValues, "y ions",
                                                "" + currentSequence.charAt(k) + (k + 1));
                                    }

                                     Iterator<String> rowIterator = dataSet.getRowKeys().iterator();

                                     while(rowIterator.hasNext()){

                                         String rowKey = rowIterator.next();

                                         Iterator<String> columnIterator = dataSet.getColumnKeys().iterator();

                                         ArrayList<Double> listValues = new ArrayList();

                                         while(columnIterator.hasNext()){

                                             String columnKey = columnIterator.next();

                                             double q3_to_q1_distance_divided_by_mean = (dataSet.getQ3Value(rowKey, columnKey).doubleValue()
                                                     - dataSet.getQ1Value(rowKey, columnKey).doubleValue())
                                                     / dataSet.getMedianValue(rowKey, columnKey).doubleValue();
                                             listValues.add(new Double(q3_to_q1_distance_divided_by_mean));
                                         }

                                         dataSetCombined.add(listValues, rowKey, new Integer(i+1).toString());

                                         if(rowKey.equalsIgnoreCase("b ions")){
                                            mediansB.add(dataSetCombined.getMeanValue(rowKey, new Integer(i+1).toString()).doubleValue());

                                            quartileDistancesB.add(dataSetCombined.getQ3Value(rowKey, new Integer(i+1).toString()).doubleValue()
                                                 - dataSetCombined.getQ1Value(rowKey, new Integer(i+1).toString()).doubleValue());
                                         } else if(rowKey.equalsIgnoreCase("y ions")){
                                             mediansY.add(dataSetCombined.getMeanValue(rowKey, new Integer(i+1).toString()).doubleValue());

                                            quartileDistancesY.add(dataSetCombined.getQ3Value(rowKey, new Integer(i+1).toString()).doubleValue()
                                                 - dataSetCombined.getQ1Value(rowKey, new Integer(i+1).toString()).doubleValue());
                                         }
                                     }
                                }

                                if (!cancelProgress) {

                                    CategoryPlot plot = PlotUtil.getCategoryPlot(dataSetCombined, "Sequences", "Q3 - Q1 / Median");

                                    // set the data series colors
                                    for (int j = 0; j < plot.getDataset().getRowKeys().size(); j++) {
                                        plot.getRenderer().setSeriesPaint(j, Util.determineFragmentIonColor(
                                                plot.getDataset().getRowKey(j).toString()));
                                    }

                                    JFreeChart chart = new JFreeChart(
                                            null,
                                            new Font("SansSerif", Font.BOLD, 10),
                                            plot,
                                            true);

                                    chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));
                                    chart.getLegend().setPosition(RectangleEdge.BOTTOM);

                                    chart.setBackgroundPaint(PlotUtil.chartBackgroundColor);

                                    if (!properties.showLegend()) {
                                        chart.getLegend().setVisible(false);
                                    }

                                    ChartPanel chartPanel = new ChartPanel(chart);

                                    String internalFrameTitle = "Intensity Variability";
                                    String plotType = "BoxPlot";

                                    FragmentationAnalyzerJInternalFrame internalFrame = new FragmentationAnalyzerJInternalFrame(
                                            internalFrameTitle, true, true, true, chartPanel, plotType, internalFrameUniqueIdCounter);
                                    internalFrame.add(chartPanel);

                                    insertInternalFrame(internalFrame);
                                    properties.getAllChartFrames().put(internalFrameUniqueIdCounter, chart);
                                    internalFrameUniqueIdCounter++;

                                    // update the visible box plot fragment ion selection
                                    updateVisibleFragmentIonBoxPlotSelection();


                                    // box plot of the quartile distances
                                    dataSetCombinedQuartileDistance.add(quartileDistancesB, "b ions", "1");
                                    dataSetCombinedQuartileDistance.add(quartileDistancesY, "y ions", "1");


                                    // print out the median and quartile distances
//                                    System.out.println("quartileDistancesB:");
//
//                                    for(int i=0; i<quartileDistancesB.size(); i++){
//                                        System.out.println(quartileDistancesB.get(i));
//                                    }
//
//                                    System.out.println("\nquartileDistancesY:");
//
//                                    for(int i=0; i<quartileDistancesY.size(); i++){
//                                        System.out.println(quartileDistancesY.get(i));
//                                    }
//
//
//                                    System.out.println("medianB:");
//
//                                    for(int i=0; i<mediansB.size(); i++){
//                                        System.out.println(mediansB.get(i));
//                                    }
//
//                                    System.out.println("\nmedianY:");
//
//                                    for(int i=0; i<mediansY.size(); i++){
//                                        System.out.println(mediansY.get(i));
//                                    }

                                    CategoryPlot plotQuartileDistances =
                                            PlotUtil.getCategoryPlot(dataSetCombinedQuartileDistance, "", "Q3 - Q1");

                                    plotQuartileDistances.getDomainAxis().setTickLabelsVisible(false);

                                    // set the data series colors
                                    for (int j = 0; j < plotQuartileDistances.getDataset().getRowKeys().size(); j++) {
                                        plotQuartileDistances.getRenderer().setSeriesPaint(j, Util.determineFragmentIonColor(
                                                plotQuartileDistances.getDataset().getRowKey(j).toString()));
                                    }

                                    JFreeChart chartQuartileDistances = new JFreeChart(
                                            null,
                                            new Font("SansSerif", Font.BOLD, 10),
                                            plotQuartileDistances,
                                            true);

                                    chartQuartileDistances.setBackgroundPaint(PlotUtil.chartBackgroundColor);

                                    chartQuartileDistances.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));
                                    chartQuartileDistances.getLegend().setPosition(RectangleEdge.BOTTOM);

                                    if (!properties.showLegend()) {
                                        chartQuartileDistances.getLegend().setVisible(false);
                                    }

                                    ChartPanel chartPanelQuartileDistances = new ChartPanel(chartQuartileDistances);

                                    internalFrameTitle = "Intensity Variability - (Q3 - Q1)";
                                    plotType = "BoxPlot";

                                    FragmentationAnalyzerJInternalFrame internalFrameQuartileDistances = new FragmentationAnalyzerJInternalFrame(
                                            internalFrameTitle, true, true, true, chartPanelQuartileDistances, plotType, internalFrameUniqueIdCounter);
                                    internalFrameQuartileDistances.add(chartPanelQuartileDistances);

                                    insertInternalFrame(internalFrameQuartileDistances);
                                    properties.getAllChartFrames().put(internalFrameUniqueIdCounter, chartQuartileDistances);
                                    internalFrameUniqueIdCounter++;


                                    // box plot of the medians
                                    dataSetCombinedMedians.add(mediansB, "b ions", "1");
                                    dataSetCombinedMedians.add(mediansY, "y ions", "1");

                                    CategoryPlot plotMedians = PlotUtil.getCategoryPlot(dataSetCombinedMedians, "", "Median");

                                    plotMedians.getDomainAxis().setTickLabelsVisible(false);

                                    // set the data series colors
                                    for (int j = 0; j < plotMedians.getDataset().getRowKeys().size(); j++) {
                                        plotMedians.getRenderer().setSeriesPaint(j, Util.determineFragmentIonColor(
                                                plotMedians.getDataset().getRowKey(j).toString()));
                                    }

                                    JFreeChart chartMedians = new JFreeChart(
                                            null,
                                            new Font("SansSerif", Font.BOLD, 10),
                                            plotMedians,
                                            true);

                                    chartMedians.setBackgroundPaint(PlotUtil.chartBackgroundColor);

                                    chartMedians.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));
                                    chartMedians.getLegend().setPosition(RectangleEdge.BOTTOM);

                                    if (!properties.showLegend()) {
                                        chartMedians.getLegend().setVisible(false);
                                    }

                                    ChartPanel chartPanelMedians = new ChartPanel(chartMedians);

                                    internalFrameTitle = "Intensity Variability - Median";
                                    plotType = "BoxPlot";

                                    FragmentationAnalyzerJInternalFrame internalFrameMedians = new FragmentationAnalyzerJInternalFrame(
                                            internalFrameTitle, true, true, true, chartPanelMedians, plotType, internalFrameUniqueIdCounter);
                                    internalFrameMedians.add(chartPanelMedians);

                                    insertInternalFrame(internalFrameMedians);
                                    properties.getAllChartFrames().put(internalFrameUniqueIdCounter, chartMedians);
                                    internalFrameUniqueIdCounter++;

                                    // update the visible box plot fragment ion selection
                                    updateVisibleFragmentIonBoxPlotSelection();



                                    // plot the normal distributions and the histograms
                                    if(mediansB.size() > 1){

                                        // b ion medians
                                        insertNormalPlot(mediansB,
                                                dataSetCombinedMedians.getQ1Value("b ions", "1").doubleValue(),
                                                dataSetCombinedMedians.getQ3Value("b ions", "1").doubleValue(),
                                                "Intensity Variability - normal - b ions median");

                                        // b ion quartile distances
                                        insertNormalPlot(quartileDistancesB,
                                                dataSetCombinedQuartileDistance.getQ1Value("b ions", "1").doubleValue(),
                                                dataSetCombinedQuartileDistance.getQ3Value("b ions", "1").doubleValue(),
                                                "Intensity Variability - normal - b ions quartile distances");

                                        // y ion medians
                                        insertNormalPlot(mediansY,
                                                dataSetCombinedMedians.getQ1Value("y ions", "1").doubleValue(),
                                                dataSetCombinedMedians.getQ3Value("y ions", "1").doubleValue(),
                                                "Intensity Variability - normal - y ions median");

                                        // y ion quartile distances
                                        insertNormalPlot(quartileDistancesY,
                                                dataSetCombinedQuartileDistance.getQ1Value("y ions", "1").doubleValue(),
                                                dataSetCombinedQuartileDistance.getQ3Value("y ions", "1").doubleValue(),
                                                "Intensity Variability - normal - y ions quartile distances");
                                    }
                                }
                            } catch (SQLException e) {
                                JOptionPane.showMessageDialog(null,
                                        "An error occured when accessing the database.\n" +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Accessing Database", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when extracing fragment ions: ");
                                e.printStackTrace();
                            } catch (IOException e) {
                                JOptionPane.showMessageDialog(null,
                                        "An error occured when building a box plot.\n" +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Building Box Plot", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when building box plot: ");
                                e.printStackTrace();
                            }
                        }
                    } else if (searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_MASS_ERROR_SCATTER_PLOT ||
                            searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_MASS_ERROR_BUBBLE_PLOT) {

                        boolean isBubblePlot = false;
                        boolean normalize = false;

                        if (searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_MASS_ERROR_BUBBLE_PLOT) {
                            isBubblePlot = true;
                            normalize = userProperties.normalizeIntensites();
                        }

                        int bubbleScaling = userProperties.getDefaultBubbleScaling();

                        if (daOrPpmSearchResultsJComboBox.getSelectedIndex() == 1 && isBubblePlot) {
                            bubbleScaling = userProperties.getPpmBubbleScaling();
                        }


                        plotsAnalysesJXTaskPane.setExpanded(true);
                        searchResultsJXTaskPane.setExpanded(false);
                        spectraJXTaskPane.setExpanded(false);

                        int totalNumberOfSpectra = 0;
                        int totalNumberOfFragmentIons = 0;

                        HashMap<String, ArrayList<XYZDataPoint>> data = new HashMap<String, ArrayList<XYZDataPoint>>();

                        for (int i = 0; i < properties.getCurrentlySelectedRowsInSearchTable().size() && !cancelProgress; i++) {

                            try {

                                progressDialog.setString("" + (i + 1) + "/" +
                                        properties.getCurrentlySelectedRowsInSearchTable().size());
                                int localCounter = 0;

                                IdentificationTableRow currentlySelectedRow =
                                        properties.getCurrentlySelectedRowsInSearchTable().get(i);
                                String currentSequence = currentlySelectedRow.getSequence();
                                String currentModifiedSequence = currentlySelectedRow.getModifiedSequence();

                                // check for search type. if count 2 exists there are more than one id per line
                                if (!singleSearch) {

                                    progressDialog.setMax(currentlySelectedRow.getCountA() + currentlySelectedRow.getCountB());
                                    progressDialog.setValue(localCounter);

                                    ArrayList<ReducedIdentification> currentIdentifications =
                                            properties.getIdentificationMap().get(currentSequence);

                                    // add the unmodified sequences
                                    for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {

                                        ReducedIdentification currentId = currentIdentifications.get(j);

                                        if (!currentId.isModified()) {
                                            totalNumberOfFragmentIons += addFragmentIonsToXYZPlotDataSeries(
                                                    data, currentId, normalize, bubbleScaling, isBubblePlot,
                                                    daOrPpmSearchResultsJComboBox.getSelectedIndex() == Properties.ACCURACY_PPM);
                                            totalNumberOfSpectra++;
                                            progressDialog.setValue(++localCounter);
                                        }
                                    }

                                    // add the modified sequences
                                    for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {

                                        ReducedIdentification currentId = currentIdentifications.get(j);

                                        if (currentId.getModifiedSequence().equalsIgnoreCase(currentModifiedSequence)) {
                                            totalNumberOfFragmentIons += addFragmentIonsToXYZPlotDataSeries(
                                                    data, currentId, normalize, bubbleScaling, isBubblePlot,
                                                    daOrPpmSearchResultsJComboBox.getSelectedIndex() == Properties.ACCURACY_PPM);
                                            totalNumberOfSpectra++;
                                            progressDialog.setValue(++localCounter);
                                        }
                                    }
                                } else {

                                    progressDialog.setMax(currentlySelectedRow.getCountA());
                                    progressDialog.setValue(localCounter);

                                    ArrayList<ReducedIdentification> currentIdentifications =
                                            properties.getIdentificationMap().get(currentModifiedSequence);

//                                    ReducedIdentification tempId = currentIdentifications.get(0);
//
//                                    System.out.println(tempId.getModifiedSequence());
//                                    System.out.println(tempId.getSequence().length());

                                    for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {
                                        ReducedIdentification currentId = currentIdentifications.get(j);
                                        totalNumberOfFragmentIons += addFragmentIonsToXYZPlotDataSeries(
                                                data, currentId, normalize, bubbleScaling, isBubblePlot,
                                                daOrPpmSearchResultsJComboBox.getSelectedIndex() == Properties.ACCURACY_PPM);
                                        totalNumberOfSpectra++;
                                        progressDialog.setValue(++localCounter);
                                    }

//                                    System.out.println("#");
                                }

                                // if single plot is selected create the plot now
                                if (combineSearchResultsJComboBox.getSelectedIndex() == Properties.SINGLE_PLOT) {

                                    String internalFrameTitle = "";

                                    int fragmentIonCount = Util.getTotalFragmentIonCount(data);

                                    if (singleSearch) {
                                        internalFrameTitle += currentModifiedSequence +
                                                " (" + currentlySelectedRow.getCountA() + " spectra"
                                                + "|" + fragmentIonCount + " fragment ions)";
                                    } else {
                                        internalFrameTitle += currentModifiedSequence +
                                                " (" + currentlySelectedRow.getCountA() + " unmod. spectra/" +
                                                currentlySelectedRow.getCountB() + " mod. spectra"
                                                + "|" + fragmentIonCount + " fragment ions)";
                                    }

                                    insertMassErrorPlot(isBubblePlot, data, internalFrameTitle,
                                            daOrPpmSearchResultsJComboBox.getSelectedIndex() == Properties.ACCURACY_PPM);

                                    data = new HashMap<String, ArrayList<XYZDataPoint>>();
                                    totalNumberOfSpectra = 0;
                                    totalNumberOfFragmentIons = 0;
                                }
                            } catch (IOException e) {
                                JOptionPane.showMessageDialog(null,
                                        "An error occured when trying to create a plot.\n" +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Creating Plot", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when creating plot: ");
                                e.printStackTrace();
                            } catch (SQLException e) {
                                JOptionPane.showMessageDialog(null,
                                        "An error occured when accessing the database.\n" +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Accessing Database", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when accessing database: ");
                                e.printStackTrace();
                            }
                        }

                        // if combine plot is selected, create the plot now
                        if (combineSearchResultsJComboBox.getSelectedIndex() == Properties.COMBINE_PLOT) {

                            String internalFrameTitle = "Mass Error Plot";

                            int fragmentIonCount = Util.getTotalFragmentIonCount(data);

                            if (properties.getCurrentlySelectedRowsInSearchTable().size() == 1) {

                                internalFrameTitle = properties.getCurrentlySelectedRowsInSearchTable().get(0).getModifiedSequence();

                                if (singleSearch) {
                                    internalFrameTitle += " (" + properties.getCurrentlySelectedRowsInSearchTable().get(0).getCountA()
                                            + " spectra|" + fragmentIonCount + " fragment ions)";
                                } else {
                                    internalFrameTitle += " (" + properties.getCurrentlySelectedRowsInSearchTable().get(0).getCountA()
                                            + " unmod. spectra/" + properties.getCurrentlySelectedRowsInSearchTable().get(0).getCountB()
                                            + " mod. spectra" + "|" + fragmentIonCount + " fragment ions)";
                                }
                            } else {
                                internalFrameTitle += " (" + totalNumberOfSpectra + " spectra|" +
                                        totalNumberOfFragmentIons + " fragment ions)";
                            }

                            insertMassErrorPlot(isBubblePlot, data, internalFrameTitle,
                                    daOrPpmSearchResultsJComboBox.getSelectedIndex() == Properties.ACCURACY_PPM);
                        }
                    } else if (searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_MASS_ERROR_BOX_PLOT) {

                        plotsAnalysesJXTaskPane.setExpanded(true);
                        searchResultsJXTaskPane.setExpanded(false);
                        spectraJXTaskPane.setExpanded(false);

                        int totalNumberOfSpectra = 0;
                        int totalNumberOfFragmenIons = 0;

                        progressDialog.setIntermidiate(false);
                        progressDialog.setTitle("Running Analysis. Please Wait...");

                        HashMap<String, ArrayList<Double>> data = new HashMap<String, ArrayList<Double>>();

                        for (int i = 0; i < properties.getCurrentlySelectedRowsInSearchTable().size() && !cancelProgress; i++) {

                            progressDialog.setValue(i + 1);
                            progressDialog.setString("(" + (i + 1) + "/" +
                                    properties.getCurrentlySelectedRowsInSearchTable().size() + ")");

                            try {

                                progressDialog.setString("" + (i + 1) + "/" +
                                        properties.getCurrentlySelectedRowsInSearchTable().size());
                                int localCounter = 0;

                                IdentificationTableRow currentlySelectedRow =
                                        properties.getCurrentlySelectedRowsInSearchTable().get(i);
                                String currentSequence = currentlySelectedRow.getSequence();
                                String currentModifiedSequence = currentlySelectedRow.getModifiedSequence();


                                // check for search type. if count 2 exists there are more than one id per line
                                if (!singleSearch) {

                                    progressDialog.setMax(currentlySelectedRow.getCountA() + currentlySelectedRow.getCountB());
                                    progressDialog.setValue(localCounter);

                                    ArrayList<ReducedIdentification> currentIdentifications =
                                            properties.getIdentificationMap().get(currentSequence);

                                    // add the unmodified sequences
                                    for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {

                                        ReducedIdentification currentId = currentIdentifications.get(j);

                                        if (!currentId.isModified()) {

                                            totalNumberOfFragmenIons += addFragmentIonsToMassErrorBoxPlot(data, currentId,
                                                    daOrPpmSearchResultsJComboBox.getSelectedIndex() == Properties.ACCURACY_PPM);
                                            totalNumberOfSpectra++;
                                            progressDialog.setValue(++localCounter);
                                        }
                                    }

                                    // add the modified sequences
                                    for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {

                                        ReducedIdentification currentId = currentIdentifications.get(j);

                                        if (currentId.getModifiedSequence().equalsIgnoreCase(currentModifiedSequence)) {
                                            totalNumberOfFragmenIons += addFragmentIonsToMassErrorBoxPlot(data, currentId,
                                                    daOrPpmSearchResultsJComboBox.getSelectedIndex() == Properties.ACCURACY_PPM);
                                            totalNumberOfSpectra++;
                                            progressDialog.setValue(++localCounter);
                                        }
                                    }
                                } else {

                                    progressDialog.setMax(currentlySelectedRow.getCountA());
                                    progressDialog.setValue(localCounter);

                                    ArrayList<ReducedIdentification> currentIdentifications =
                                            properties.getIdentificationMap().get(currentModifiedSequence);

                                    for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {
                                        ReducedIdentification currentId = currentIdentifications.get(j);
                                        totalNumberOfFragmenIons += addFragmentIonsToMassErrorBoxPlot(data, currentId,
                                                daOrPpmSearchResultsJComboBox.getSelectedIndex() == Properties.ACCURACY_PPM);
                                        totalNumberOfSpectra++;
                                        progressDialog.setValue(++localCounter);
                                    }
                                }

                                // if single plot is selected create the plot now
                                if (combineSearchResultsJComboBox.getSelectedIndex() == Properties.SINGLE_PLOT) {

                                    String internalFrameTitle = "";

                                    if (singleSearch) {
                                        internalFrameTitle += currentModifiedSequence +
                                                " (" + totalNumberOfSpectra + " spectra"
                                                + "|" + totalNumberOfFragmenIons + " fragment ions)";
                                    } else {
                                        internalFrameTitle += currentModifiedSequence +
                                                " (" + currentlySelectedRow.getCountA() + " unmod. spectra/" +
                                                currentlySelectedRow.getCountB() + " mod. spectra"
                                                + "|" + totalNumberOfFragmenIons + " fragment ions)";
                                    }

                                    insertMassErrorBoxPlot(data, internalFrameTitle,
                                            daOrPpmSearchResultsJComboBox.getSelectedIndex() == Properties.ACCURACY_PPM);

                                    data = new HashMap<String, ArrayList<Double>>();
                                    totalNumberOfSpectra = 0;
                                    totalNumberOfFragmenIons = 0;
                                }
                            } catch (IOException e) {
                                JOptionPane.showMessageDialog(null,
                                        "An error occured when trying to create a plot.\n" +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Creating Plot", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when creating plot: ");
                                e.printStackTrace();
                            } catch (SQLException e) {
                                JOptionPane.showMessageDialog(null,
                                        "An error occured when accessing the database.\n" +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Accessing Database", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when accessing database: ");
                                e.printStackTrace();
                            }
                        }

                        // if combine plot is selected, create the plot now
                        if (combineSearchResultsJComboBox.getSelectedIndex() == Properties.COMBINE_PLOT) {

                            String internalFrameTitle = "Mass Error Box Plot";

                            if (properties.getCurrentlySelectedRowsInSearchTable().size() == 1) {
                                internalFrameTitle = properties.getCurrentlySelectedRowsInSearchTable().get(0).getModifiedSequence();
                            }

                            internalFrameTitle += " (" + totalNumberOfSpectra + " spectra"
                                    + "| " + totalNumberOfFragmenIons + " fragment ions)";

                            insertMassErrorBoxPlot(data, internalFrameTitle,
                                    daOrPpmSearchResultsJComboBox.getSelectedIndex() == Properties.ACCURACY_PPM);
                        }
                    } else if (searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_ION_PROBABILITY_PLOT
                            || searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_ION_HEAT_MAP) {

                        // if combine is selected we need to find the longest peptide sequence selected
                        int longestPeptideSequenceLength =
                                properties.getCurrentlySelectedRowsInSearchTable().get(0).getSequence().length();

                        int shortestPeptideLength = longestPeptideSequenceLength;

                        if (combineSearchResultsJComboBox.getSelectedIndex() == Properties.COMBINE_PLOT) {
                            for (int i = 1; i < properties.getCurrentlySelectedRowsInSearchTable().size() && !cancelProgress; i++) {

                                int tempLength = properties.getCurrentlySelectedRowsInSearchTable().get(i).getSequence().length();

                                if (tempLength > longestPeptideSequenceLength) {
                                    longestPeptideSequenceLength = tempLength;
                                }

                                if (tempLength < shortestPeptideLength) {
                                    shortestPeptideLength = tempLength;
                                }
                            }
                        }

                        int fragmentIonLowerThreshold = 1;
                        int fragmentIonUpperThreshold = longestPeptideSequenceLength - 1;

                        // decide how much of the peptide sequence to compare
                        if(searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_ION_HEAT_MAP){

                            // check if all selected peptides have the same length
                            if(shortestPeptideLength != longestPeptideSequenceLength){
                                JOptionPane.showMessageDialog(
                                        null,
                                        "Peptides with different lengths (" + shortestPeptideLength + "-"
                                        + longestPeptideSequenceLength + ") are selected.\n" +
                                        "Only fragments ions from 1 to " + shortestPeptideLength + " will be used.",
                                        "Peptide Lengths", JOptionPane.INFORMATION_MESSAGE);

                                fragmentIonUpperThreshold = shortestPeptideLength - 1;
                            }

                            int option = JOptionPane.showConfirmDialog(null,
                                    "Select a subset of the available fragment ions?",
                                    "Fragment Ion Selection", JOptionPane.YES_NO_OPTION);

                            if(option == JOptionPane.YES_OPTION){
                                
                                // set the lower fragment ion boundary
                                boolean notAnInteger = true;

                                while(notAnInteger && !cancelProgress){

                                    String value = JOptionPane.showInputDialog(
                                            null,
                                            "Use fragment ions from fragment number (1-" + (fragmentIonUpperThreshold - 2) + "):",
                                            "Fragment Ion Selection - Lower", JOptionPane.INFORMATION_MESSAGE);

                                    if(value != null){
                                        try{
                                            int tempLowerRange = new Integer(value).intValue();
                                            notAnInteger = false;

                                            if(tempLowerRange > 0 &&
                                                    tempLowerRange <= fragmentIonUpperThreshold - 2){
                                                fragmentIonLowerThreshold = tempLowerRange;
                                            } else {
                                                // set the start value to the first fragment ion
                                                fragmentIonLowerThreshold = 1;
                                            }
                                        } catch (NumberFormatException e){
                                            JOptionPane.showMessageDialog(null, "Inserted value has to be an integer.",
                                                    "Not An Integer", JOptionPane.INFORMATION_MESSAGE);
                                        }
                                    } else {
                                        cancelProgress = true;
                                    }
                                }


                                notAnInteger = true;

                                // set the upper fragment ion boundary
                                while(notAnInteger && !cancelProgress){

                                    String value = JOptionPane.showInputDialog(
                                            null,
                                            "Use fragment ions up to fragment number (" + (fragmentIonLowerThreshold + 2) + "-"
                                                + fragmentIonUpperThreshold + "):",
                                            "Fragment Ion Selection - Upper", JOptionPane.INFORMATION_MESSAGE);

                                    if(value != null){
                                        try{
                                            int tempUpperRange = new Integer(value).intValue();
                                            notAnInteger = false;

                                            if(tempUpperRange >= fragmentIonLowerThreshold + 2 &&
                                                    tempUpperRange <= fragmentIonUpperThreshold){
                                                fragmentIonUpperThreshold = tempUpperRange;
                                            } else {
                                                // use the end value to the last fragment ion
                                            }
                                        } catch (NumberFormatException e){
                                            JOptionPane.showMessageDialog(null, "Inserted value has to be an integer.",
                                                    "Not An Integer", JOptionPane.INFORMATION_MESSAGE);
                                        }
                                    } else {
                                        cancelProgress = true;
                                    }
                                }

                                if(!cancelProgress){
                                    JOptionPane.showMessageDialog(null,
                                            "Fragment Ion Range Selected: " + fragmentIonLowerThreshold + "-" +
                                            fragmentIonUpperThreshold, "Fragment Ion Range", JOptionPane.INFORMATION_MESSAGE);
                                }
                            }
                        }

                        if(!cancelProgress){
                            plotsAnalysesJXTaskPane.setExpanded(true);
                            searchResultsJXTaskPane.setExpanded(false);
                            spectraJXTaskPane.setExpanded(false);

                            progressDialog.setIntermidiate(false);
                            progressDialog.setTitle("Running Analysis. Please Wait...");
                        }

                        HashMap<String, int[]> sequenceDependentFragmentIons = new HashMap<String, int[]>();
                        HashMap<String, Integer> sequenceIndependentFragmentIons = new HashMap<String, Integer>();

                        //HashMap<String, double[]> averageSequenceIndependentFragmentIons = new HashMap<String, double[]>();
                        HashMap<String, double[][]> averageSequenceDependentFragmentIons = new HashMap<String, double[][]>();

                        int totalNumberOfSpectra = 0;
                        int[] numberOfSpectraOfGivenLength = new int[longestPeptideSequenceLength + 1];
                        int[] totalNumberOfSpectraOfGivenLength = new int[longestPeptideSequenceLength + 1];
                        int totalNumberOfFragmentIons = 0;

                        for (int i = 0; i < properties.getCurrentlySelectedRowsInSearchTable().size() && !cancelProgress; i++) {

                            progressDialog.setValue(i + 1);
                            progressDialog.setString("(" + (i + 1) + "/" +
                                    properties.getCurrentlySelectedRowsInSearchTable().size() + ")");

                            try {
                                progressDialog.setString("" + (i + 1) + "/" +
                                        properties.getCurrentlySelectedRowsInSearchTable().size());
                                int localCounter = 0;

                                IdentificationTableRow currentlySelectedRow =
                                        properties.getCurrentlySelectedRowsInSearchTable().get(i);
                                String currentSequence = currentlySelectedRow.getSequence();
                                String currentModifiedSequence = currentlySelectedRow.getModifiedSequence();

                                // if the plots are not going to be combined, we can reset the peptide length
                                if (combineSearchResultsJComboBox.getSelectedIndex() == Properties.SINGLE_PLOT) {
                                    longestPeptideSequenceLength = currentSequence.length();
                                    numberOfSpectraOfGivenLength = new int[longestPeptideSequenceLength + 1];
                                }

                                // check for search type. if count 2 exists there are more than one id per line
                                if (!singleSearch) {

                                    progressDialog.setMax(currentlySelectedRow.getCountA() + currentlySelectedRow.getCountB());
                                    progressDialog.setValue(localCounter);

                                    ArrayList<ReducedIdentification> currentIdentifications =
                                            properties.getIdentificationMap().get(currentSequence);

                                    // add the unmodified sequences
                                    for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {

                                        ReducedIdentification currentId = currentIdentifications.get(j);

                                        if (!currentId.isModified()) {
                                            totalNumberOfFragmentIons += addFragmentIonsToIonProbabilityPlot(currentId, sequenceDependentFragmentIons,
                                                    sequenceIndependentFragmentIons, longestPeptideSequenceLength,
                                                    combineSearchResultsJComboBox.getSelectedIndex() == Properties.SINGLE_PLOT);
                                            totalNumberOfSpectra++;

                                            // update the totalNumberOfSpectraOfGivenLength array
                                            for(int k = 1; k < currentId.getSequence().length(); k++){
                                                numberOfSpectraOfGivenLength[k]++;
                                            }

                                            progressDialog.setValue(++localCounter);
                                        }
                                    }

                                    // add the modified sequences
                                    for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {

                                        ReducedIdentification currentId = currentIdentifications.get(j);

                                        if (currentId.getModifiedSequence().equalsIgnoreCase(currentModifiedSequence)) {
                                            totalNumberOfFragmentIons += addFragmentIonsToIonProbabilityPlot(currentId, sequenceDependentFragmentIons,
                                                    sequenceIndependentFragmentIons, longestPeptideSequenceLength,
                                                    combineSearchResultsJComboBox.getSelectedIndex() == Properties.SINGLE_PLOT);
                                            totalNumberOfSpectra++;

                                            // update the totalNumberOfSpectraOfGivenLength array
                                            for(int k = 1; k < currentId.getSequence().length(); k++){
                                                numberOfSpectraOfGivenLength[k]++;
                                            }

                                            progressDialog.setValue(++localCounter);
                                        }
                                    }
                                } else {

                                    progressDialog.setMax(currentlySelectedRow.getCountA());
                                    progressDialog.setValue(localCounter);

                                    ArrayList<ReducedIdentification> currentIdentifications =
                                            properties.getIdentificationMap().get(currentModifiedSequence);

                                    for (int j = 0; j < currentIdentifications.size() && !cancelProgress; j++) {
                                        ReducedIdentification currentId = currentIdentifications.get(j);
                                        totalNumberOfFragmentIons += addFragmentIonsToIonProbabilityPlot(currentId, sequenceDependentFragmentIons,
                                                sequenceIndependentFragmentIons, longestPeptideSequenceLength, 
                                                combineSearchResultsJComboBox.getSelectedIndex() == Properties.SINGLE_PLOT);
                                        totalNumberOfSpectra++;

                                        // update the totalNumberOfSpectraOfGivenLength array
                                        for(int k = 1; k < currentId.getSequence().length(); k++){
                                            numberOfSpectraOfGivenLength[k]++;
                                        }
                                        
                                        progressDialog.setValue(++localCounter);
                                    }
                                }

                                // if single plot is selected, create the plot now
                                if (combineSearchResultsJComboBox.getSelectedIndex() == Properties.SINGLE_PLOT) {

                                    // create the line plot for the sequence dependent fragment ions
                                    JFreeChart chart = PlotUtil.getLinePlot(sequenceDependentFragmentIons,
                                            numberOfSpectraOfGivenLength,
                                            "Residue Number", "Occurence (%)");

                                    if (!properties.showLegend()) {
                                        chart.getLegend().setVisible(false);
                                    }

                                    chart.setBackgroundPaint(PlotUtil.chartBackgroundColor);

                                    ChartPanel chartPanel = new ChartPanel(chart);

                                    String internalFrameTitle = "" + currentModifiedSequence
                                            + " (" + totalNumberOfSpectra + " spectra|"
                                            + totalNumberOfFragmentIons + " fragment ions)";
                                    String plotType = "FragmentIonProbabilityPlot";

                                    FragmentationAnalyzerJInternalFrame internalFrame = new FragmentationAnalyzerJInternalFrame(
                                            internalFrameTitle, true, true, true, chartPanel, plotType, internalFrameUniqueIdCounter);
                                    internalFrame.add(chartPanel);

                                    insertInternalFrame(internalFrame);
                                    properties.getAllChartFrames().put(internalFrameUniqueIdCounter, chart);
                                    internalFrameUniqueIdCounter++;


                                    // create the bar plot for the precusor and immonium ions
                                    JFreeChart barChart = PlotUtil.getBarPlot(sequenceIndependentFragmentIons,
                                            totalNumberOfSpectra,
                                            "Ion Type", "Occurence (%)");

                                    barChart.setBackgroundPaint(PlotUtil.chartBackgroundColor);

                                    ChartPanel barChartPanel = new ChartPanel(barChart);
                                    plotType = "BarPlot";

                                    FragmentationAnalyzerJInternalFrame internalFrameBarChart = new FragmentationAnalyzerJInternalFrame(
                                            internalFrameTitle, true, true, true, barChartPanel, plotType, internalFrameUniqueIdCounter);
                                    internalFrameBarChart.add(barChartPanel);

                                    insertInternalFrame(internalFrameBarChart);
                                    properties.getAllChartFrames().put(internalFrameUniqueIdCounter, barChart);
                                    internalFrameUniqueIdCounter++;

                                    totalNumberOfSpectra = 0;
                                    totalNumberOfFragmentIons = 0;
                                } else {

                                    // update the totalNumberOfSpectraOfGivenLength array
                                    for(int k = 1; k < currentSequence.length(); k++){
                                        totalNumberOfSpectraOfGivenLength[k]++;
                                    }

                                    // update the list of average sequence dependent fragment ions
                                    PlotUtil.updateAverageSequenceDependentFragmentIons(averageSequenceDependentFragmentIons,
                                            sequenceDependentFragmentIons, numberOfSpectraOfGivenLength,
                                            longestPeptideSequenceLength, i,
                                            properties.getCurrentlySelectedRowsInSearchTable().size());

                                    // update the list of average sequence independent fragment ions
                                    // ToDo: implement
                                }

                                sequenceDependentFragmentIons = new HashMap<String, int[]>();
                                sequenceIndependentFragmentIons = new HashMap<String, Integer>();
                                numberOfSpectraOfGivenLength = new int[longestPeptideSequenceLength + 1];

                            } catch (IOException e) {
                                JOptionPane.showMessageDialog(null,
                                        "An error occured when trying to create a plot.\n" +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Creating Plot", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when creating plot: ");
                                e.printStackTrace();
                            } catch (SQLException e) {
                                JOptionPane.showMessageDialog(null,
                                        "An error occured when accessing the database.\n" +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Accessing Database", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when accessing database: ");
                                e.printStackTrace();
                            }
                        }

                        // if combine plot is selected, create the plot now
                        if (combineSearchResultsJComboBox.getSelectedIndex() == Properties.COMBINE_PLOT
                                && !cancelProgress) {

                            if(searchResultsJComboBox.getSelectedIndex() == Properties.SEARCH_RESULTS_ION_HEAT_MAP){

                                // create the heat maps

                                // b ions all values
                                insertHeatMap(averageSequenceDependentFragmentIons, totalNumberOfSpectraOfGivenLength,
                                        fragmentIonLowerThreshold, fragmentIonUpperThreshold,
                                        "b", false, "Heat Map - B Ions");

                                // y ions all values
                                insertHeatMap(averageSequenceDependentFragmentIons, totalNumberOfSpectraOfGivenLength,
                                        fragmentIonLowerThreshold, fragmentIonUpperThreshold,
                                        "y", false, "Heat Map - Y Ions");

                                // b ions only significant
                                insertHeatMap(averageSequenceDependentFragmentIons, totalNumberOfSpectraOfGivenLength,
                                        fragmentIonLowerThreshold, fragmentIonUpperThreshold,
                                        "b", true, "Heat Map - B Ions Significant");

                                // y ions only significant
                                insertHeatMap(averageSequenceDependentFragmentIons, totalNumberOfSpectraOfGivenLength,
                                        fragmentIonLowerThreshold, fragmentIonUpperThreshold,
                                        "y", true, "Heat Map - Y Ions Significant");

                            } else {

                                // create the line plot for the sequence dependent fragment ions
                                JFreeChart chart = PlotUtil.getAverageLinePlot(averageSequenceDependentFragmentIons,
                                        totalNumberOfSpectraOfGivenLength,
                                        "Fragment Ion Number", "Occurence (%)", properties);

                                chart.setBackgroundPaint(PlotUtil.chartBackgroundColor);

                                if (!properties.showLegend()) {
                                    chart.getLegend().setVisible(false);
                                }

                                ChartPanel chartPanel = new ChartPanel(chart);

                                String internalFrameTitle = "Fragment Ion Probability"
                                        + " (" + totalNumberOfSpectra + " spectra|"
                                        + totalNumberOfFragmentIons + " fragment ions)";

                                if (properties.getCurrentlySelectedRowsInSearchTable().size() == 1) {
                                    internalFrameTitle = "" + properties.getCurrentlySelectedRowsInSearchTable().get(0).getModifiedSequence() +
                                            " (" + totalNumberOfSpectra + " spectra)";
                                }

                                String plotType = "FragmentIonProbabilityPlot";

                                FragmentationAnalyzerJInternalFrame internalFrame = new FragmentationAnalyzerJInternalFrame(
                                        internalFrameTitle, true, true, true, chartPanel, plotType, internalFrameUniqueIdCounter);
                                internalFrame.add(chartPanel);

                                insertInternalFrame(internalFrame);
                                properties.getAllChartFrames().put(internalFrameUniqueIdCounter, chart);
                                internalFrameUniqueIdCounter++;


                                // create the box plot for the spread
                                insertFragmentIonProbabilityBoxPlot(averageSequenceDependentFragmentIons,
                                        "Fragment Ion Type", "Occurence (%)", internalFrameTitle);


                                // ToDo: implement
                                // create the bar plot for the precusor and immonium ions
//                                JFreeChart barChart = PlotUtil.getBarPlot(averageSequenceIndependentFragmentIons,
//                                        totalNumberOfSpectra,
//                                        "Ion Type", "Occurence (%)");
//
//                                chart.setBackgroundPaint(PlotUtil.chartBackgroundColor);
//
//                                ChartPanel barChartPanel = new ChartPanel(barChart);
//                                plotType = "BarPlot";
//
//                                FragmentationAnalyzerJInternalFrame internalFrameBarChart = new FragmentationAnalyzerJInternalFrame(
//                                        internalFrameTitle, true, true, true, barChartPanel, plotType, internalFrameUniqueIdCounter);
//                                internalFrameBarChart.add(barChartPanel);
//
//                                insertInternalFrame(internalFrameBarChart);
//                                properties.getAllChartFrames().put(internalFrameUniqueIdCounter, barChart);
//                                internalFrameUniqueIdCounter++;
                            }
                        }
                    }

                    progressDialog.setVisible(false);
                    progressDialog.dispose();
                }
            }.start();
        }
}//GEN-LAST:event_searchResultsJButtonActionPerformed

    /**
     * Creates and inserts a heat map into the plot pane.
     *
     * @param averageSequenceDependentFragmentIons
     * @param totalNumberOfSpectraOfGivenLength
     * @param fragmentIonLowerThreshold
     * @param fragmentIonUpperThreshold
     * @param fragmentIonType
     * @param significanceColorCoding
     * @param title
     */
    private void insertHeatMap(
            HashMap<String, double[][]> averageSequenceDependentFragmentIons,
            int[] totalNumberOfSpectraOfGivenLength,
            int fragmentIonLowerThreshold,
            int fragmentIonUpperThreshold,
            String fragmentIonType,
            boolean significanceColorCoding,
            String title){

        String[][] heatMapData =
                PlotUtil.getHeatMapData(averageSequenceDependentFragmentIons,
                totalNumberOfSpectraOfGivenLength, fragmentIonType, userProperties,
                fragmentIonLowerThreshold, fragmentIonUpperThreshold, significanceColorCoding);

        FragmentationAnalyzerJInternalFrame internalFrameHeatMap = new FragmentationAnalyzerJInternalFrame(
                title + " (" + fragmentIonLowerThreshold + "-" + fragmentIonUpperThreshold + ")",
                true, true, true, null, "HeatMap", internalFrameUniqueIdCounter);

        HeatMapJPanel heatMapJPanel = new HeatMapJPanel(userProperties, heatMapData);

        if(significanceColorCoding){
            heatMapJPanel.setHeatMapToolTip(
                    "<html><font color=\"red\">Red: Significant Positive Linear Correlation</font><br>" +
                    "<font color=\"green\">Green: No Significant Positive Linear Correlation</font></html>");
        }

        internalFrameHeatMap.add(heatMapJPanel);

        insertInternalFrame(internalFrameHeatMap);
        internalFrameUniqueIdCounter++;
    }

    /**
     * Creates and inserts a fragment ion probability box plot.
     *
     * @param data the data to plot
     * @param xAxisLabel the x axis label
     * @param yAxisLabel the y axis label
     * @param title the title of the plot
     */
    private void insertFragmentIonProbabilityBoxPlot(HashMap<String, double[][]> data,
            String xAxisLabel, String yAxisLabel, String title) {

        JFreeChart chart = PlotUtil.createFragmentIonProbabilityBoxPlot(data, xAxisLabel,yAxisLabel, properties, title);
        ChartPanel chartPanel = new ChartPanel(chart);

        FragmentationAnalyzerJInternalFrame internalFrame = new FragmentationAnalyzerJInternalFrame(
                title, true, true, true, chartPanel, "FragmentIonProbabilityBoxPlot", internalFrameUniqueIdCounter);
        internalFrame.add(chartPanel);

        insertInternalFrame(internalFrame);
        properties.getAllChartFrames().put(internalFrameUniqueIdCounter, chart);
        internalFrameUniqueIdCounter++;
    }

    /**
     * Creates and inserts a mass error box plot.
     *
     * @param data the data to plot
     * @param title the title of the plot
     * @param usePpm true if ppm is used as the mass error, false otherwise
     */
    private void insertMassErrorBoxPlot(HashMap<String, ArrayList<Double>> data, String title, boolean usePpm) {

        JFreeChart chart = PlotUtil.createMassErrorBoxPlot(data, title, usePpm);
        ChartPanel chartPanel = new ChartPanel(chart);

        FragmentationAnalyzerJInternalFrame internalFrame = new FragmentationAnalyzerJInternalFrame(
                title, true, true, true, chartPanel, "MassErrorBoxPlot", internalFrameUniqueIdCounter);
        internalFrame.add(chartPanel);

        insertInternalFrame(internalFrame);
        properties.getAllChartFrames().put(internalFrameUniqueIdCounter, chart);
        internalFrameUniqueIdCounter++;
    }

    /**
     * Inserts an internal frame with a histogram and a normal distribution based on the provided dataset.
     *
     * @param values the values to use as input for the plots
     * @param q1 the first quartile of the dataset
     * @param q3 the thirds quartile of the dataset
     * @param internalFrameTitle the title of the inserted internal frame
     */
    private void insertNormalPlot(ArrayList<Double> values, double q1, double q3, String internalFrameTitle){

        XYPlot plotNormalMediansBions = PlotUtil.plotNormalDistribution(values, q1, q3, properties.showMarkers());

        JFreeChart chartNormal = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plotNormalMediansBions, true);

        chartNormal.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));
        chartNormal.getLegend().setPosition(RectangleEdge.BOTTOM);

        chartNormal.setBackgroundPaint(PlotUtil.chartBackgroundColor);

        if (!properties.showLegend()) {
            chartNormal.getLegend().setVisible(false);
        }

        ChartPanel chartPanelNormals = new ChartPanel(chartNormal);

        String plotType = "Normal";

        FragmentationAnalyzerJInternalFrame internalFrameNormals = new FragmentationAnalyzerJInternalFrame(
                internalFrameTitle, true, true, true, chartPanelNormals, plotType, internalFrameUniqueIdCounter);
        internalFrameNormals.add(chartPanelNormals);

        insertInternalFrame(internalFrameNormals);
        properties.getAllChartFrames().put(internalFrameUniqueIdCounter, chartNormal);
        internalFrameUniqueIdCounter++;

        // update the visible box plot fragment ion selection
        updateVisibleFragmentIonBoxPlotSelection();
    }

    /**
     * Inserts a mass error plot (bubble or scatter) in an internal frame in the plotting pane.
     *
     * @param isBubblePlot if true the plot will be a bubble plot
     * @param data the data to plot
     * @param internalFrameTitle the title of the internal frame
     * @param usePpm if true ppm is used for the mass error, otherwise Da is used
     */
    private void insertMassErrorPlot(boolean isBubblePlot, HashMap<String, ArrayList<XYZDataPoint>> data,
            String internalFrameTitle, boolean usePpm) {

        JFreeChart chart = PlotUtil.createMassErrorPlot(isBubblePlot, data, internalFrameTitle, usePpm, properties);
        ChartPanel chartPanel = new ChartPanel(chart);

        String plotType = "";

        if (isBubblePlot) {
            plotType = "MassErrorBubblePlot";
        } else {
            plotType = "MassErrorScatterPlot";
        }

        // create the interal frame and add the plot
        FragmentationAnalyzerJInternalFrame internalFrame = new FragmentationAnalyzerJInternalFrame(
                internalFrameTitle, true, true, true, chartPanel, plotType, internalFrameUniqueIdCounter);
        internalFrame.add(chartPanel);

        insertInternalFrame(internalFrame);

        properties.getAllChartFrames().put(internalFrameUniqueIdCounter, chart);
        internalFrameUniqueIdCounter++;
    }

    /**
     * Make sure the visible series are the same as the currently selected ones.
     */
    private void updateVisibleFragmentIonBoxPlotSelection() {
        bIonsBoxPlotJCheckBoxActionPerformed(null);
        yIonsBoxPlotJCheckBoxActionPerformed(null);
        bIonsUnmodifiedJCheckBoxActionPerformed(null);
        bIonsModifiedJCheckBoxActionPerformed(null);
        yIonsUnmodifiedJCheckBoxActionPerformed(null);
        yIonsModifiedJCheckBoxActionPerformed(null);
    }

    /**
     * Add the fragment ions to an XYZ plot data series.
     *
     * @param data the data to be added
     * @param currentIdentification the current identification
     * @param normalize if true the z values are normalized
     * @param bubbleScaling the scaling value for the bubble size
     * @param switchYandZAxis if true the Y and Z values provied in the data set are switched
     * @param usePpmForMassError if true ppm is calculated and used for the mass error value,
     *                           otherwise the given mass error is used
     * @param int the number of fragment ions added
     * @throws IOException
     * @throws SQLException
     */
    private int addFragmentIonsToXYZPlotDataSeries(HashMap<String, ArrayList<XYZDataPoint>> data,
            ReducedIdentification currentIdentification, boolean normalize, int bubbleScaling,
            boolean switchYandZAxis, boolean usePpmForMassError) throws IOException, SQLException {

        // ToDo: This method ought to be moved into the PlotUtil class

        int totalNumberOfFragmentIonsUsed = 0;

//        int sequenceLength = currentIdentification.getSequence().length();
//
//        double[] bIons = new double[sequenceLength+1];
//        double[] yIons = new double[sequenceLength+1];
//        double[] allIons = new double[(sequenceLength+1)*2];

        if (currentDataSetIsFromMsLims) {

            Vector<Fragmention> fragmentIons = (Vector<Fragmention>) Fragmention.getAllFragmentions(
                    getConnection(), (long) currentIdentification.getIdentificationId());

//            boolean wantedFragmentsFound = false;

            for (int j = 0; j < fragmentIons.size(); j++) {

                if (userProperties.isScoringTypeSelected(fragmentIons.get(j).getL_ionscoringid())) {

                    totalNumberOfFragmentIonsUsed++;

                    double mzValue = fragmentIons.get(j).getMz().doubleValue();
                    double intensity = fragmentIons.get(j).getIntensity();
                    double massError = fragmentIons.get(j).getMassdelta().doubleValue();

                    String fragmentIonType = fragmentIons.get(j).getIonname();
                    int fragmentIonNumber = (int) fragmentIons.get(j).getFragmentionnumber();
                    String fragmentIonNumberAsString = "" + fragmentIonNumber;

                    // add padding enable correct sorting
                    if (fragmentIonNumber < 10) {
                        fragmentIonNumberAsString = "0" + fragmentIonNumberAsString;
                    }

                    if (fragmentIonType.startsWith("a") || fragmentIonType.startsWith("b") || fragmentIonType.startsWith("c") ||
                            fragmentIonType.startsWith("x") || fragmentIonType.startsWith("y") || fragmentIonType.startsWith("z")) {
                        if (fragmentIonType.length() > 1) {
                            fragmentIonType = fragmentIonType.substring(0, 1) + "[" + fragmentIonNumberAsString + "]" + fragmentIonType.substring(1);
                        } else {
                            fragmentIonType = fragmentIonType.substring(0, 1) + fragmentIonNumberAsString;
                        }
                    }


//                    if(fragmentIonType.startsWith("b") && fragmentIonType.indexOf("[") == -1){
//                        bIons[fragmentIonNumber] = intensity;
//                        allIons[fragmentIonNumber] = intensity;
//                        wantedFragmentsFound = true;
//                    } else if(fragmentIonType.startsWith("y") && fragmentIonType.indexOf("[") == -1){
//                        yIons[fragmentIonNumber] = intensity;
//                        allIons[fragmentIonNumber + (sequenceLength + 1)] = intensity;
//                        wantedFragmentsFound = true;
//                    }


                    if (usePpmForMassError) {
                        massError = Util.getPpmError(mzValue, massError);
                    }

                    // normalize the intensity
                    if (normalize) {

                        double totalIntensity = 1.0;

                        if (currentIdentification.getTotalIntensity() != null) {
                            totalIntensity = currentIdentification.getTotalIntensity();
                        } else {
                            if (currentIdentification.getSpectrumFileId() != null) {
                                totalIntensity = calculateTotalIntensityForMsLimsSpectrum(currentIdentification.getSpectrumFileId());
                            }
                        }

                        intensity = intensity / totalIntensity;
                    }

                    if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_INSTRUMENT) {
                        PlotUtil.addXYZDataPoint(data, currentIdentification.getInstrumentName(),
                                switchYandZAxis, mzValue, massError, intensity, bubbleScaling);
                    } else if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_TYPE) {
                        PlotUtil.addXYZDataPoint(data, fragmentIonType, switchYandZAxis, mzValue, massError, intensity, bubbleScaling);
                    } else if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_IDENTIFICATION_ID) {
                        PlotUtil.addXYZDataPoint(data, currentIdentification.getIdentificationId().toString(), switchYandZAxis,
                                mzValue, massError, intensity, bubbleScaling);
                    } else if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_SCORING_TYPE) {

                        String dataSeriesLabel = "" + fragmentIons.get(j).getL_ionscoringid();

                        if (fragmentIons.get(j).getL_ionscoringid() == 0) {
                            dataSeriesLabel = "Not Significant, Not Used For Scoring";
                        } else if (fragmentIons.get(j).getL_ionscoringid() == 1) {
                            dataSeriesLabel = "Significant, Not Used For Scoring";
                        } else if (fragmentIons.get(j).getL_ionscoringid() == 2) {
                            dataSeriesLabel = "Significant, Used For Scoring";
                        }

                        PlotUtil.addXYZDataPoint(data, dataSeriesLabel, switchYandZAxis, mzValue, massError, intensity, bubbleScaling);
                    } else if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_THRESHOLD) {
                        PlotUtil.addXYZDataPoint(data, fragmentIons.get(j).getMasserrormargin().toString(),
                                switchYandZAxis, mzValue, massError, intensity, bubbleScaling);
                    }
                }
            }

//            if(wantedFragmentsFound){
//                double[] bIonsRanks = new double[bIons.length-1];
//                double[] yIonsRanks = new double[yIons.length-1];
//                double[] ranksAll = new double[allIons.length-1];
//
//    //            System.out.println("bIons:");
//    //
//    //            for(int i=1; i<bIons.length; i++){
//    //                System.out.print(bIons[i] + " ");
//    //            }
//    //
//    //
//    //            int rank = 1;
//    //            int index = 1;
//    //
//    //            while(index != -1){
//    //
//    //                index = findIndexOfLargestNumber(bIons);
//    //
//    //                if(index > 0){
//    //                    bIonsRanks[index-1] = rank++;
//    //                }
//    //            }
//    //
//    //            //System.out.println("\nbRanks:");
//    //
//    //            for(int i=0; i<bIonsRanks.length; i++){
//    //                System.out.print((int) bIonsRanks[i] + " ");
//    //            }
//    //
//    //            System.out.println();
//
//
//    //            System.out.println("\nyIons:");
//    //
//    //            for(int i=1; i<yIons.length; i++){
//    //                System.out.print(yIons[i] + " ");
//    //            }
//
//    //            int rank = 1;
//    //            int index = 1;
//    //
//    //            while(index != -1){
//    //
//    //                index = findIndexOfLargestNumber(yIons);
//    //
//    //                if(index > 0){
//    //                    yIonsRanks[index-1] = rank++;
//    //                }
//    //            }
//    //
//    //
//    //            //System.out.println("\nyRanks:");
//    //
//    //            for(int i=0; i<yIonsRanks.length; i++){
//    //                System.out.print((int) yIonsRanks[i] + " ");
//    //            }
//    //
//    //            System.out.println();
//
//
//
//    //            System.out.println("\nallIons:");
//    //
//    //            for(int i=1; i<allIons.length; i++){
//    //                System.out.print(allIons[i] + " ");
//    //            }
//    //
//    //
//                int rank = 1;
//                int index = 1;
//
//                while(index != -1){
//
//                    index = findIndexOfLargestNumber(allIons);
//
//                    if(index > 0){
//                        ranksAll[index-1] = rank++;
//                    }
//                }
//
//                //System.out.println("\nallRanks:");
//
//                for(int i=0; i<ranksAll.length; i++){
//                    System.out.print((int) ranksAll[i] + " ");
//                }
//
//                System.out.println();
//            }

        } else {
            // get the fragment ions
            ArrayList<FragmentIon> fragmentIons = getFragmentIons(currentIdentification.getSpectrumFileId(), null);

//            boolean wantedFragmentsFound = false;

            for (int k = 0; k < fragmentIons.size(); k++) {

                totalNumberOfFragmentIonsUsed++;

                double mzValue = fragmentIons.get(k).getFragmenIonMz().doubleValue();
                double intensity = fragmentIons.get(k).getFragmentIonIntensity().doubleValue();
                double massError = fragmentIons.get(k).getFragmentIonMassError().doubleValue();

                String fragmentIonType = fragmentIons.get(k).getFragmentIonType();

                // add padding enable correct sorting
                if (fragmentIons.get(k).getFragmentIonNumber() < 10) {
                    fragmentIonType = fragmentIonType.replaceFirst("" + fragmentIons.get(k).getFragmentIonNumber(),
                            "0" + fragmentIons.get(k).getFragmentIonNumber());
                }

//                if(fragmentIonType.startsWith("b") && fragmentIonType.indexOf("[") == -1){
//                    bIons[fragmentIons.get(k).getFragmentIonNumber()] = intensity;
//                    allIons[fragmentIons.get(k).getFragmentIonNumber()] = intensity;
//                    wantedFragmentsFound = true;
//                } else if(fragmentIonType.startsWith("y") && fragmentIonType.indexOf("[") == -1){
//                    yIons[fragmentIons.get(k).getFragmentIonNumber()] = intensity;
//                    allIons[fragmentIons.get(k).getFragmentIonNumber() + (sequenceLength + 1)] = intensity;
//                    wantedFragmentsFound = true;
//                }

                if (usePpmForMassError) {
                    massError = Util.getPpmError(mzValue, massError);
                }

                // normalize the intensity
                if (normalize) {
                    intensity = intensity / currentIdentification.getTotalIntensity();
                }

                if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_INSTRUMENT) {
                    PlotUtil.addXYZDataPoint(data, currentIdentification.getInstrumentName(), switchYandZAxis, mzValue,
                            massError, intensity, bubbleScaling);
                } else if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_FRAGMENT_ION_TYPE) {
                    PlotUtil.addXYZDataPoint(data, fragmentIonType, switchYandZAxis, mzValue, massError, intensity, bubbleScaling);
                } else if (properties.getCurrentLabelType() == Properties.PLOT_LABEL_TYPE_IDENTIFICATION_ID) {
                    PlotUtil.addXYZDataPoint(data, currentIdentification.getIdentificationId().toString(), switchYandZAxis,
                            mzValue, massError, intensity, bubbleScaling);
                }
            }


//            // calculate the ranks
//            if(wantedFragmentsFound){
//
//                double[] bIonsRanks = new double[bIons.length-1];
//                double[] yIonsRanks = new double[yIons.length-1];
//                double[] ranksAll = new double[allIons.length-1];
//
//    //            System.out.println("bIons:");
//    //
//    //            for(int i=1; i<bIons.length; i++){
//    //                System.out.print(bIons[i] + " ");
//    //            }
//    //
//    //
//    //            int rank = 1;
//    //            int index = 1;
//    //
//    //            while(index != -1){
//    //
//    //                index = findIndexOfLargestNumber(bIons);
//    //
//    //                if(index > 0){
//    //                    bIonsRanks[index-1] = rank++;
//    //                }
//    //            }
//    //
//    //            System.out.println("\nbRanks:");
//    //
//    //            for(int i=0; i<bIonsRanks.length; i++){
//    //                System.out.print((int) bIonsRanks[i] + " ");
//    //            }
//
//
//
//    //            System.out.println("\nyIons:");
//    //
//    //            for(int i=1; i<yIons.length; i++){
//    //                System.out.print(yIons[i] + " ");
//    //            }
//
//    //            int rank = 1;
//    //            int index = 1;
//    //
//    //            while(index != -1){
//    //
//    //                index = findIndexOfLargestNumber(yIons);
//    //
//    //                if(index > 0){
//    //                    yIonsRanks[index-1] = rank++;
//    //                }
//    //            }
//    //
//    //
//    //            //System.out.println("\nyRanks:");
//    //
//    //            for(int i=0; i<yIonsRanks.length; i++){
//    //                System.out.print((int) yIonsRanks[i] + " ");
//    //            }
//    //
//    //            System.out.println();
//
//
//
//    //            System.out.println("\nallIons:");
//    //
//    //            for(int i=1; i<allIons.length; i++){
//    //                System.out.print(allIons[i] + " ");
//    //            }
//    //
//    //
//                int rank = 1;
//                int index = 1;
//
//                while(index != -1){
//
//                    index = findIndexOfLargestNumber(allIons);
//
//                    if(index > 0){
//                        ranksAll[index-1] = rank++;
//                    }
//                }
//
//                //System.out.println("\nallRanks:");
//
//                for(int i=0; i<ranksAll.length; i++){
//                    System.out.print((int) ranksAll[i] + " ");
//                }
//
//                System.out.println();
//            }
        }

        return totalNumberOfFragmentIonsUsed;
    }


    /**
     * Returns the index of the largest value in an array. Also replaces the
     * largest value with 0.0.
     *
     * @param values
     * @return
     */
    private int findIndexOfLargestNumber(double[] values){

        int index = -1;
        double largestValue = Double.MIN_VALUE;

        for(int i=1; i<values.length; i++){
            if(values[i] > 0 && values[i] > largestValue){
                largestValue = values[i];
                index = i;
            }
        }

        if(index != -1){
            values[index] = 0.0;
        }
        
        return index;
    }


    /**
     * Add the fragment ions to an XYZ plot data series.
     *
     * @param data the data to be added
     * @param currentIdentification the current identification
     * @param normalize if true the z values are normalized
     * @param bubbleScaling the scaling value for the bubble size
     * @param switchYandZAxis if true the Y and Z values provied in the data set are switched
     * @param usePpmForMassError if true ppm is calculated and used for the mass error value,
     *                           otherwise the given mass error is used
     * @return int number of fragment ions added
     * @throws IOException
     * @throws SQLException
     */
    private int addFragmentIonsToMassErrorBoxPlot(HashMap<String, ArrayList<Double>> data,
            ReducedIdentification currentIdentification, boolean usePpmForMassError) throws IOException, SQLException {

        // ToDo: This method ought to be moved into the PlotUtil class

        int numberOfFragmentIonsUsed = 0;

        if (currentDataSetIsFromMsLims) {

            // get the fragment ions
            Vector<Fragmention> fragmentIons = (Vector<Fragmention>) Fragmention.getAllFragmentions(
                    getConnection(), (long) currentIdentification.getIdentificationId());

            for (int j = 0; j < fragmentIons.size(); j++) {

                if (userProperties.isScoringTypeSelected(fragmentIons.get(j).getL_ionscoringid())) {

                    numberOfFragmentIonsUsed++;

                    double mzValue = fragmentIons.get(j).getMz().doubleValue();
                    double massError = fragmentIons.get(j).getMassdelta().doubleValue();

                    String fragmentIonType = fragmentIons.get(j).getIonname();
                    int fragmentIonNumber = (int) fragmentIons.get(j).getFragmentionnumber();
                    String fragmentIonNumberAsString = "" + fragmentIonNumber;

                    // add padding enable correct sorting
                    if (fragmentIonNumber < 10) {
                        fragmentIonNumberAsString = "0" + fragmentIonNumberAsString;
                    }

                    if (fragmentIonType.startsWith("a") || fragmentIonType.startsWith("b") || fragmentIonType.startsWith("c") ||
                            fragmentIonType.startsWith("x") || fragmentIonType.startsWith("y") || fragmentIonType.startsWith("z")) {
                        if (fragmentIonType.length() > 1) {
                            fragmentIonType = fragmentIonType.substring(0, 1) + "[" + fragmentIonNumberAsString + "]" + fragmentIonType.substring(1);
                        } else {
                            fragmentIonType = fragmentIonType.substring(0, 1) + fragmentIonNumberAsString;
                        }
                    }

                    if (usePpmForMassError) {
                        massError = Util.getPpmError(mzValue, massError);
                    }

                    if (data.get(fragmentIonType) != null) {
                        data.get(fragmentIonType).add(massError);
                    } else {
                        ArrayList<Double> temp = new ArrayList<Double>();
                        temp.add(massError);
                        data.put(fragmentIonType, temp);
                    }
                }
            }
        } else {
            // get the fragment ions
            ArrayList<FragmentIon> fragmentIons = getFragmentIons(currentIdentification.getSpectrumFileId(), null);

            for (int k = 0; k < fragmentIons.size(); k++) {

                numberOfFragmentIonsUsed++;

                FragmentIon currentFragmentIon = fragmentIons.get(k);

                double mzValue = currentFragmentIon.getFragmenIonMz().doubleValue();
                double massError = currentFragmentIon.getFragmentIonMassError().doubleValue();

                String fragmentIonType = currentFragmentIon.getFragmentIonType();

                if (usePpmForMassError) {
                    massError = Util.getPpmError(mzValue, massError);
                }

                // add padding to enable correct sorting
                if (currentFragmentIon.getFragmentIonNumber() < 10) {
                    fragmentIonType = fragmentIonType.replaceFirst("" + currentFragmentIon.getFragmentIonNumber(),
                            "0" + currentFragmentIon.getFragmentIonNumber());
                }

                if (data.get(fragmentIonType) != null) {
                    data.get(fragmentIonType).add(massError);
                } else {
                    ArrayList<Double> temp = new ArrayList<Double>();
                    temp.add(massError);
                    data.put(fragmentIonType, temp);
                }
            }
        }

        return numberOfFragmentIonsUsed;
    }

    /**
     * Retrieves all the fragment ions for the given identification from ms_lims.
     *
     * @param currentId the identification to get the fragment ions for
     * @param intensities the list to store the intensities
     * @param index the index in the intensities list where the intensites will be stored
     * @param ionType the ion type to extract (see ms_lims for details)
     * @return int number of fragment ions extracted
     * @throws SQLException
     */
    private int getAllFragmentsFromMsLims(ReducedIdentification currentId, double[][] intensities, int index, long ionType)
            throws SQLException {

        // ToDo: This method ought to be moved into a separate class

        int numberOfFragmentIonsUsed = 0;

        Collection fragments =
                Fragmention.getAllFragmentions(getConnection(), currentId.getIdentificationId(), ionType);

        Iterator fragmentIterator = fragments.iterator();

        double totalIntensity = 1.0;

        if (userProperties.normalizeIntensites()) {
            if (currentId.getTotalIntensity() != null) {
                totalIntensity = currentId.getTotalIntensity();
            } else {
                if (currentId.getSpectrumFileId() != null) {
                    totalIntensity = calculateTotalIntensityForMsLimsSpectrum(currentId.getSpectrumFileId());
                }
            }
        }

        while (fragmentIterator.hasNext()) {

            Fragmention currentFragmentIon = (Fragmention) fragmentIterator.next();

            if (userProperties.isScoringTypeSelected(currentFragmentIon.getL_ionscoringid())) {

                numberOfFragmentIonsUsed++;

                if (userProperties.normalizeIntensites()) {
                    intensities[new Long(currentFragmentIon.getFragmentionnumber()).intValue() - 1][index] +=
                            new Long(currentFragmentIon.getIntensity()).doubleValue() / totalIntensity;
                } else {
                    intensities[new Long(currentFragmentIon.getFragmentionnumber()).intValue() - 1][index] +=
                            currentFragmentIon.getIntensity();
                }
            }
        }

        return numberOfFragmentIonsUsed;
    }

    /**
     * Retrieves all the fragment ions for the given identification from the fragmentIons text file.
     *
     * @param currentId the identification to get the fragment ions for
     * @param intensities the list to store the intensities
     * @param index the index in the intensities list where the intensites will be stored
     * @param ionType the ion type to extract
     * @return int number of fragment ions used
     * @throws SQLException
     * @throws IOException
     */
    private int getAllFragmentsFromFragmentIonsFile(ReducedIdentification currentId, double[][] intensities, int index, String ionType)
            throws SQLException, IOException {

        // ToDo: This method ought to be moved into a separate class

        int numberOfFragmentIonsUsed = 0;

        // get the wanted fragment ions from the fragmentIons.txt file
        ArrayList<FragmentIon> fragmentIons = getFragmentIons(currentId.getIdentificationId(), ionType);

        // iterate the extracted fragment ions and store the intensities
        for (int i = 0; i < fragmentIons.size(); i++) {

            numberOfFragmentIonsUsed++;

            FragmentIon currentFragmentIon = fragmentIons.get(i);

            if (userProperties.normalizeIntensites()) {
                intensities[new Long(currentFragmentIon.getFragmentIonNumber()).intValue() - 1][index] +=
                        currentFragmentIon.getFragmentIonIntensity() / currentId.getTotalIntensity();
            } else {
                intensities[new Long(currentFragmentIon.getFragmentIonNumber()).intValue() - 1][index] +=
                        currentFragmentIon.getFragmentIonIntensity();
            }
        }

        return numberOfFragmentIonsUsed;
    }

    /**
     * Returns the total intensity of the selected spectrum in the ms_lims database.
     *
     * @param spectrumFileId
     * @return the total intensity of the selected spectrum
     */
    public Double calculateTotalIntensityForMsLimsSpectrum(Integer spectrumFileId) {

        // ToDo: This method ought to be moved into a separate class

        Double totalIntensity = 0.0;

        try {
            Spectrum_file spectrumFile = Spectrum_file.findFromID(spectrumFileId, getConnection());
            String filename = Spectrum.findFromID(spectrumFileId, getConnection()).getFilename();
            String file = new String(spectrumFile.getUnzippedFile());
            MascotGenericFile lSpectrumFile = new MascotGenericFile(filename, file);

            TreeSet treeSet = new TreeSet();
            HashMap aPeakList = lSpectrumFile.getPeaks();
            treeSet.clear();
            treeSet.addAll(aPeakList.keySet());

            Iterator treeSetIterator = treeSet.iterator();

            Double mz, intensity;

            while (treeSetIterator.hasNext()) {

                mz = (Double) treeSetIterator.next();
                intensity = (Double) aPeakList.get(mz);

                totalIntensity += intensity;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Spectrumfile ID \'" + spectrumFileId + "\' not found in database.\n" +
                    "Spectrum/Identification not included in the plot/analysis.",
                    "Spectrumfile Not Found", JOptionPane.WARNING_MESSAGE);
            Util.writeToErrorLog("Spectrumfile ID \'" + spectrumFileId + "\' not found  in database.\n" +
                    "Spectrum not included in plot/analysis.");
            ex.printStackTrace();
        }

        return totalIntensity;
    }

    /**
     * Enables or disables the spectra analysis button based on the selected item in the
     * spectra analysis combo box and the number of selected rows in the table.
     *
     * @param evt
     */
    private void spectraJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectraJComboBoxActionPerformed
        if (spectraJComboBox.getSelectedIndex() != 0 && properties.getCurrentlySelectedRowsInSpectraTable().size() > 0) {
            spectraJButton.setEnabled(true);
            spectraJButton.setToolTipText(null);
        } else {
            spectraJButton.setEnabled(false);
            spectraJButton.setToolTipText(spectraAnalysisButtonDisabledToolTip);
        }

        if (spectraJComboBox.getSelectedIndex() != Properties.SPECTRA_VIEW_SPECTRUM
                && spectraJComboBox.getSelectedIndex() != Properties.SPECTRA_INTENSITY_BOX_PLOT
                && spectraJComboBox.getSelectedIndex() != 0) {
            daOrPpmSpectraJComboBox.setEnabled(true);
            combineSpectraJComboBox.setEnabled(true);
        } else {
            daOrPpmSpectraJComboBox.setEnabled(false);
            combineSpectraJComboBox.setEnabled(false);
        }

        if (spectraJComboBox.getSelectedIndex() == Properties.SPECTRA_ION_PROBABILITY_PLOT) {
            daOrPpmSpectraJComboBox.setEnabled(false);
            combineSpectraJComboBox.setEnabled(true);
        }

        if(spectraJComboBox.getSelectedIndex() == Properties.SPECTRA_INTENSITY_BOX_PLOT ||
                spectraJComboBox.getSelectedIndex() == Properties.SPECTRA_VIEW_SPECTRUM){
            combineSpectraJComboBox.setSelectedItem("Single");
        }
    }//GEN-LAST:event_spectraJComboBoxActionPerformed

    /**
     * Starts the analysis type selected in the spectra analysis combo box.
     * Inserts the result into the analysis/plot frame.
     *
     * @param evt
     */
    private void spectraJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectraJButtonActionPerformed

        // ToDo: a lot of the code below is repeated for each plotting type and ought
        //       to be combined to simplify the code

        cancelProgress = false;

        // if more than 10 internal frames are to be opened, first ask if the user wants to continue or not
        if(properties.getCurrentlySelectedRowsInSpectraTable().size() > 10 &&
                combineSpectraJComboBox.getSelectedIndex() == Properties.SINGLE_PLOT &&
                spectraJComboBox.getSelectedIndex() != Properties.SPECTRA_INTENSITY_BOX_PLOT){
            int option = JOptionPane.showConfirmDialog(this, "This will open " +
                    properties.getCurrentlySelectedRowsInSpectraTable().size()
                    + " plots/analysis frames.\nAre you sure you want to continue?", "Continue?",
                    JOptionPane.YES_NO_OPTION);

            if(option == JOptionPane.NO_OPTION){
                cancelProgress = true;
            }
        }

        // get the wanted label type from the user
        if (!cancelProgress &&
                (spectraJComboBox.getSelectedIndex() == Properties.SPECTRA_MASS_ERROR_BUBBLE_PLOT ||
                spectraJComboBox.getSelectedIndex() == Properties.SPECTRA_MASS_ERROR_SCATTER_PLOT)) {
            new PlotLabelSelection(this, true, currentDataSetIsFromMsLims);
        }

        // verify that all modified sequences are equal
        if (!cancelProgress && spectraJComboBox.getSelectedIndex() == Properties.SPECTRA_INTENSITY_BOX_PLOT) {
            // || spectraJComboBox.getSelectedIndex() == Properties.SPECTRA_ION_PROBABILITY_PLOT){
            cancelProgress = !Util.verifyEqualModifiedSeqences(true, properties);
        }

        if (!cancelProgress) {

            plotsAnalysesJXTaskPane.setExpanded(true);
            searchResultsJXTaskPane.setExpanded(false);
            spectraJXTaskPane.setExpanded(false);

            progressDialog = new ProgressDialog(this, this, true);

            new Thread(new Runnable() {

                public void run() {
                    progressDialog.setIntermidiate(false);
                    progressDialog.setValue(0);
                    progressDialog.setTitle("Running Analysis. Please Wait...");
                    progressDialog.setVisible(true);
                }
            }, "ProgressDialog").start();

            new Thread("SpectraThread") {

                @Override
                public void run() {

                    progressDialog.setMax(properties.getCurrentlySelectedRowsInSpectraTable().size());

                    resultsJScrollPane.getVerticalScrollBar().setValue(resultsJScrollPane.getVerticalScrollBar().getVisibleAmount());

                    if (spectraJComboBox.getSelectedIndex() == Properties.SPECTRA_VIEW_SPECTRUM) {

                        for (int i = 0; i < properties.getCurrentlySelectedRowsInSpectraTable().size() && !cancelProgress; i++) {

                            SpectrumTableRow currentRow = properties.getCurrentlySelectedRowsInSpectraTable().get(i);
                            Integer currentId = currentRow.getIdentificationId();
                            Integer currentSpectrumId = currentRow.getSpectrumId();

                            String internalFrameTitle = currentRow.getModifiedSequence() + " (SID: " + currentSpectrumId + ")";

                            progressDialog.setValue(i + 1);

                            if (currentDataSetIsFromMsLims) {

                                try {
                                    // TODO could be replaced by a select that only extracts the file
                                    Spectrum_file spectrumFile = Spectrum_file.findFromID((long) currentSpectrumId, getConnection());
                                    Vector<Fragmention> fragmentIons = (Vector<Fragmention>) Fragmention.getAllFragmentions(getConnection(), (long) currentId);

                                    FragmentationAnalyzerJInternalFrame internalFrame = new FragmentationAnalyzerJInternalFrame(
                                            internalFrameTitle, true, true, true, null, "SpectrumPanel", internalFrameUniqueIdCounter);
                                    internalFrame.add(getSpectrumPanel(spectrumFile, fragmentIons));

                                    insertInternalFrame(internalFrame);
                                    internalFrameUniqueIdCounter++;

                                    // update the fragment ions
                                    aIonsJCheckBoxActionPerformed(null);

                                } catch (SQLException e) {
                                    JOptionPane.showMessageDialog(
                                            null, "An error occured when accesing the database." +
                                            "See ../Properties/ErrorLog.txt for more details.",
                                            "Error Accessing Database", JOptionPane.ERROR_MESSAGE);
                                    Util.writeToErrorLog("Error when accessing database: ");
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    JOptionPane.showMessageDialog(
                                            null, "An error occured when trying to view a spectrum." +
                                            "See ../Properties/ErrorLog.txt for more details.",
                                            "Error Viewing Spectrum", JOptionPane.ERROR_MESSAGE);
                                    Util.writeToErrorLog("Error when trying to view a spectrum: ");
                                    e.printStackTrace();
                                }
                            } else {

                                try {
                                    File spectrumFile = new File(properties.getCurrentDataSetFolder() + "/spectra/" + currentSpectrumId + ".pkl");
                                    PKLFile pklFile = Util.parsePKLFile(spectrumFile);

                                    // get the fragment ions
                                    ArrayList<FragmentIon> fragmentIons = getFragmentIons(currentSpectrumId, null);

                                    FragmentationAnalyzerJInternalFrame internalFrame = new FragmentationAnalyzerJInternalFrame(
                                            internalFrameTitle, true, true, true, null, "SpectrumPanel", internalFrameUniqueIdCounter);
                                    internalFrame.add(getSpectrumPanel(pklFile, fragmentIons));

                                    insertInternalFrame(internalFrame);
                                    internalFrameUniqueIdCounter++;

                                    // update the fragment ions
                                    aIonsJCheckBoxActionPerformed(null);

                                } catch (IOException e) {
                                    JOptionPane.showMessageDialog(
                                            null, "An error occured when trying to view a spectrum." +
                                            "See ../Properties/ErrorLog.txt for more details.",
                                            "Error Viewing Spectrum", JOptionPane.ERROR_MESSAGE);
                                    Util.writeToErrorLog("Error when trying to view a spectrum: ");
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else if (spectraJComboBox.getSelectedIndex() == Properties.SPECTRA_MASS_ERROR_BUBBLE_PLOT ||
                            spectraJComboBox.getSelectedIndex() == Properties.SPECTRA_MASS_ERROR_SCATTER_PLOT) {

                        boolean isBubblePlot = false;
                        boolean normalize = false;

                        if (spectraJComboBox.getSelectedIndex() == Properties.SPECTRA_MASS_ERROR_BUBBLE_PLOT) {
                            isBubblePlot = true;
                            normalize = userProperties.normalizeIntensites();
                        }

                        int bubbleScaling = userProperties.getDefaultBubbleScaling();

                        if (daOrPpmSpectraJComboBox.getSelectedIndex() == 1 && isBubblePlot) {
                            bubbleScaling = userProperties.getPpmBubbleScaling();
                        }

                        HashMap<String, ArrayList<XYZDataPoint>> data = new HashMap<String, ArrayList<XYZDataPoint>>();

                        boolean allPlotsHaveSameSequence = true;

                        // check if all plots have the same sequence
                        if (combineSpectraJComboBox.getSelectedIndex() == Properties.COMBINE_PLOT) {
                            allPlotsHaveSameSequence = Util.verifyEqualModifiedSeqences(false, properties);
                        }

                        int totalNumberOfFragmentIons = 0;

                        for (int i = 0; i < properties.getCurrentlySelectedRowsInSpectraTable().size() && !cancelProgress; i++) {

                            try {
                                Integer currentId = properties.getCurrentlySelectedRowsInSpectraTable().get(i).getIdentificationId();
                                ReducedIdentification currentIdentification = properties.getAllIdentifications().get(currentId);

                                progressDialog.setValue(i + 1);

                                totalNumberOfFragmentIons += addFragmentIonsToXYZPlotDataSeries(
                                        data, currentIdentification, normalize, bubbleScaling, isBubblePlot,
                                        daOrPpmSpectraJComboBox.getSelectedIndex() == Properties.ACCURACY_PPM);


                                // if not combine, create plot
                                if (combineSpectraJComboBox.getSelectedIndex() == Properties.SINGLE_PLOT) {

                                    String internalFrameTitle = currentIdentification.getModifiedSequence() +
                                            " (SID: " + currentIdentification.getSpectrumFileId()
                                            + "|" + totalNumberOfFragmentIons + " fragment ions)";

                                    insertMassErrorPlot(isBubblePlot, data, internalFrameTitle,
                                            daOrPpmSpectraJComboBox.getSelectedIndex() == Properties.ACCURACY_PPM);

                                    data = new HashMap<String, ArrayList<XYZDataPoint>>();
                                    totalNumberOfFragmentIons = 0;
                                }
                            } catch (SQLException e) {
                                JOptionPane.showMessageDialog(
                                        null, "An error occured when accessing the database." +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Accessing Database", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when accessing the database: ");
                                e.printStackTrace();
                            } catch (IOException e) {
                                JOptionPane.showMessageDialog(
                                        null, "An error occured when trying to create a mass error plot." +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Creating Mass Error Plot", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when creating mass error plot: ");
                                e.printStackTrace();
                            }
                        }


                        // if combine, create plot here
                        if (combineSpectraJComboBox.getSelectedIndex() == Properties.COMBINE_PLOT) {

                            String internalFrameTitle = "Mass Error Plot";

                            if (properties.getCurrentlySelectedRowsInSpectraTable().size() == 1 || allPlotsHaveSameSequence) {
                                internalFrameTitle = properties.getCurrentlySelectedRowsInSpectraTable().get(0).getModifiedSequence();
                            } else {
                                internalFrameTitle += " (";
                            }

                            if(properties.getCurrentlySelectedRowsInSpectraTable().size() == 1){
                                internalFrameTitle += " (SID: " + properties.getCurrentlySelectedRowsInSpectraTable().get(0).getSpectrumId() + ", ";
                            } else {
                                internalFrameTitle += " (";
                            }

                            internalFrameTitle += properties.getCurrentlySelectedRowsInSpectraTable().size()
                                    + " spectra|" + totalNumberOfFragmentIons + " fragment ions)";

                            insertMassErrorPlot(isBubblePlot, data, internalFrameTitle,
                                    daOrPpmSpectraJComboBox.getSelectedIndex() == Properties.ACCURACY_PPM);
                        }
                    } else if (spectraJComboBox.getSelectedIndex() == Properties.SPECTRA_MASS_ERROR_BOX_PLOT) {

                        HashMap<String, ArrayList<Double>> data = new HashMap<String, ArrayList<Double>>();

                        int totalNumberOfFragmentIons = 0;

                        for (int i = 0; i < properties.getCurrentlySelectedRowsInSpectraTable().size() && !cancelProgress; i++) {

                            try {
                                Integer currentId = properties.getCurrentlySelectedRowsInSpectraTable().get(i).getIdentificationId();
                                ReducedIdentification currentIdentification =
                                        properties.getAllIdentifications().get(currentId);

                                progressDialog.setValue(i + 1);

                                totalNumberOfFragmentIons += addFragmentIonsToMassErrorBoxPlot(data, currentIdentification,
                                        daOrPpmSpectraJComboBox.getSelectedIndex() == Properties.ACCURACY_PPM);


                                // if not combine create plot
                                if (combineSpectraJComboBox.getSelectedIndex() == Properties.SINGLE_PLOT) {

                                    String internalFrameTitle = currentIdentification.getModifiedSequence() +
                                            " (SID: " + currentIdentification.getSpectrumFileId() + "|"
                                            + totalNumberOfFragmentIons + " fragment ions)";

                                    insertMassErrorBoxPlot(data, internalFrameTitle,
                                            daOrPpmSpectraJComboBox.getSelectedIndex() == Properties.ACCURACY_PPM);

                                    data = new HashMap<String, ArrayList<Double>>();
                                    totalNumberOfFragmentIons = 0;
                                }
                            } catch (SQLException e) {
                                JOptionPane.showMessageDialog(
                                        null, "An error occured when accessing the database." +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Accessing Database", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when accessing the database: ");
                                e.printStackTrace();
                            } catch (IOException e) {
                                JOptionPane.showMessageDialog(
                                        null, "An error occured when trying to create a mass error plot." +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Creating Mass Error Plot", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when creating mass error plot: ");
                                e.printStackTrace();
                            }
                        }


                        // if combine create plot here
                        if (combineSpectraJComboBox.getSelectedIndex() == Properties.COMBINE_PLOT) {

                            String internalFrameTitle = "";

                            if (Util.verifyEqualModifiedSeqences(false, properties) || properties.getCurrentlySelectedRowsInSpectraTable().size() == 1) {
                                internalFrameTitle += properties.getCurrentlySelectedRowsInSpectraTable().get(0).getModifiedSequence();
                            } else {
                                internalFrameTitle += "Mass Error Box Plot";
                            }

                            if(properties.getCurrentlySelectedRowsInSpectraTable().size() == 1){
                                internalFrameTitle += " (SID: " + properties.getCurrentlySelectedRowsInSpectraTable().get(0).getSpectrumId() + ", ";
                            } else {
                                internalFrameTitle += " (";
                            }

                            internalFrameTitle += properties.getCurrentlySelectedRowsInSpectraTable().size() + " spectra|"
                                    + totalNumberOfFragmentIons + " fragment ions)";

                            insertMassErrorBoxPlot(data, internalFrameTitle,
                                    daOrPpmSpectraJComboBox.getSelectedIndex() == Properties.ACCURACY_PPM);
                        }
                    } else if (spectraJComboBox.getSelectedIndex() == Properties.SPECTRA_INTENSITY_BOX_PLOT) {

                        // get the sequences
                        String currentModifiedSequence = properties.getCurrentlySelectedRowsInSpectraTable().get(0).getModifiedSequence();
                        String currentSequence = properties.getCurrentlySelectedRowsInSpectraTable().get(0).getSequence();

                        plotsAnalysesJXTaskPane.setExpanded(true);
                        searchResultsJXTaskPane.setExpanded(false);
                        spectraJXTaskPane.setExpanded(false);

                        DefaultBoxAndWhiskerCategoryDataset dataSet = new DefaultBoxAndWhiskerCategoryDataset();

                        double[][] bIntensities = new double[currentSequence.length()][properties.getCurrentlySelectedRowsInSpectraTable().size()];
                        double[][] yIntensities = new double[currentSequence.length()][properties.getCurrentlySelectedRowsInSpectraTable().size()];

                        progressDialog.setTitle("Extracting Fragment Ions. Please Wait...");
                        progressDialog.setIntermidiate(false);
                        progressDialog.setMax(properties.getCurrentlySelectedRowsInSpectraTable().size());
                        progressDialog.setValue(0);

                        int totalNumberOfFragmentIons = 0;

                        for (int i = 0; i < properties.getCurrentlySelectedRowsInSpectraTable().size() && !cancelProgress; i++) {

                            try {
                                Integer currentId = properties.getCurrentlySelectedRowsInSpectraTable().get(i).getIdentificationId();
                                ReducedIdentification currentIdentification = properties.getAllIdentifications().get(currentId);

                                progressDialog.setValue(i + 1);

                                // get all b and y fragment ions and store them in
                                // the bIntensities and yIntensities tables, one row per identification
                                if (currentDataSetIsFromMsLims) {
                                    // get all b fragments
                                    totalNumberOfFragmentIons += getAllFragmentsFromMsLims(currentIdentification, bIntensities, i, Properties.B_ION);

                                    // get all y fragments
                                    totalNumberOfFragmentIons += getAllFragmentsFromMsLims(currentIdentification, yIntensities, i, Properties.Y_ION);
                                } else {
                                    // get all b fragments
                                    totalNumberOfFragmentIons += getAllFragmentsFromFragmentIonsFile(currentIdentification, bIntensities, i, "b");

                                    // get all y fragments
                                    totalNumberOfFragmentIons += getAllFragmentsFromFragmentIonsFile(currentIdentification, yIntensities, i, "y");
                                }

                                // lists of all non null B and Y values
                                ArrayList<Double> nonNullBValues = new ArrayList<Double>();
                                ArrayList<Double> nonNullYValues = new ArrayList<Double>();

                                for (int k = 0; k < yIntensities.length; k++) {

                                    nonNullBValues = new ArrayList<Double>();
                                    nonNullYValues = new ArrayList<Double>();

                                    // get the list of non null b and y values, and get the average values
                                    // NB: the y fragments are flipped in the returned lists to make it
                                    //     easier to compare b and y ions in the same plot, i.e. y 1 becomes y n,
                                    //     y 2 becomes y (n-1), etc.
                                    double averageBValue = PlotUtil.getNonNullBFragments(nonNullBValues, bIntensities, k);
                                    double averageYValue = PlotUtil.getNonNullYFragments(nonNullYValues, yIntensities, k);

                                    // add the b ions to the box plot data set
                                    double[] bValues = PlotUtil.addValuesToBoxPlot(dataSet, nonNullBValues, "b ions",
                                            "" + currentSequence.charAt(k) + (k + 1));

                                    // add the y ions to the box plot data set
                                    double[] yValues = PlotUtil.addValuesToBoxPlot(dataSet, nonNullYValues, "y ions",
                                            "" + currentSequence.charAt(k) + (k + 1));
                                }
                            } catch (SQLException e) {
                                JOptionPane.showMessageDialog(
                                        null, "An error occured when accessing the database." +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Accessing Database", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when accessing the database: ");
                                e.printStackTrace();
                            } catch (IOException e) {
                                JOptionPane.showMessageDialog(
                                        null, "An error occured when trying to create an intensity box plot." +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Creating Mass Error Plot", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when creating intensity box plot: ");
                                e.printStackTrace();
                            }
                        }

                        progressDialog.setIntermidiate(true);
                        progressDialog.setTitle("Creating Box Plot. Please Wait...");

                        CategoryPlot plot = PlotUtil.getCategoryPlot(dataSet, "Sequence", "Intensity");

                        // set the data series colors
                        for (int j = 0; j < plot.getDataset().getRowKeys().size(); j++) {
                            plot.getRenderer().setSeriesPaint(j, Util.determineFragmentIonColor(
                                    plot.getDataset().getRowKey(j).toString()));
                        }

                        if (!cancelProgress) {
                            JFreeChart chart = new JFreeChart(
                                    null,
                                    new Font("SansSerif", Font.BOLD, 10),
                                    plot,
                                    true);

                            chart.setBackgroundPaint(PlotUtil.chartBackgroundColor);

                            chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));
                            chart.getLegend().setPosition(RectangleEdge.BOTTOM);

                            if (!properties.showLegend()) {
                                chart.getLegend().setVisible(false);
                            }

                            ChartPanel chartPanel = new ChartPanel(chart);

                            String internalFrameTitle = "" + currentModifiedSequence +
                                    " (" + properties.getCurrentlySelectedRowsInSpectraTable().size() + " spectra|"
                                    + totalNumberOfFragmentIons + " fragment ions)";
                            String plotType = "BoxPlot";

                            FragmentationAnalyzerJInternalFrame internalFrame = new FragmentationAnalyzerJInternalFrame(
                                    internalFrameTitle, true, true, true, chartPanel, plotType, internalFrameUniqueIdCounter);
                            internalFrame.add(chartPanel);

                            insertInternalFrame(internalFrame);
                            properties.getAllChartFrames().put(internalFrameUniqueIdCounter, chart);
                            internalFrameUniqueIdCounter++;

                            // update the visible box plot fragment ion selection
                            updateVisibleFragmentIonBoxPlotSelection();
                        }
                    } else if (spectraJComboBox.getSelectedIndex() == Properties.SPECTRA_ION_PROBABILITY_PLOT) {

                        // if combine is selected we need to find the longest peptide sequence selected
                        int longestPeptideSequenceLength =
                                properties.getCurrentlySelectedRowsInSpectraTable().get(0).getSequence().length();

                        if (combineSpectraJComboBox.getSelectedIndex() == Properties.COMBINE_PLOT) {
                            for (int i = 1; i < properties.getCurrentlySelectedRowsInSpectraTable().size() && !cancelProgress; i++) {

                                int tempLength = properties.getCurrentlySelectedRowsInSpectraTable().get(i).getSequence().length();

                                if (tempLength > longestPeptideSequenceLength) {
                                    longestPeptideSequenceLength = tempLength;
                                }
                            }
                        }

                        HashMap<String, int[]> sequenceDependentFragmentIons = new HashMap<String, int[]>();
                        HashMap<String, Integer> sequenceIndependentFragmentIons = new HashMap<String, Integer>();

                        HashMap<String, double[][]> averageSequenceDependentFragmentIons = new HashMap<String, double[][]>();

                        int totalNumberOfFragmentIons = 0;
                        int[] numberOfSpectraOfGivenLength = new int[longestPeptideSequenceLength + 1];
                        int[] totalNumberOfSpectraOfGivenLength = new int[longestPeptideSequenceLength + 1];

                        for (int i = 0; i < properties.getCurrentlySelectedRowsInSpectraTable().size() && !cancelProgress; i++) {

                            try {
                                Integer currentId = properties.getCurrentlySelectedRowsInSpectraTable().get(i).getIdentificationId();
                                ReducedIdentification currentIdentification = properties.getAllIdentifications().get(currentId);

                                // if the plots are not going to be combined, we can reset the peptide length
                                if (combineSpectraJComboBox.getSelectedIndex() == Properties.SINGLE_PLOT) {
                                    longestPeptideSequenceLength = currentIdentification.getSequence().length();
                                }

                                progressDialog.setValue(i + 1);

                                totalNumberOfFragmentIons += addFragmentIonsToIonProbabilityPlot(currentIdentification, 
                                        sequenceDependentFragmentIons, sequenceIndependentFragmentIons,
                                        longestPeptideSequenceLength, combineSpectraJComboBox.getSelectedIndex() == Properties.SINGLE_PLOT);

                                // if the plots are not going to be combined, we can reset the peptide length
                                if (combineSpectraJComboBox.getSelectedIndex() == Properties.SINGLE_PLOT) {
                                    longestPeptideSequenceLength = currentIdentification.getSequence().length();
                                    numberOfSpectraOfGivenLength = new int[currentIdentification.getSequence().length() + 1];
                                }

                                // update the totalNumberOfSpectraOfGivenLength array
                                for(int k = 1; k <= currentIdentification.getSequence().length(); k++){
                                    numberOfSpectraOfGivenLength[k]++;
                                }

                                // if not combine create plot
                                if (combineSpectraJComboBox.getSelectedIndex() == Properties.SINGLE_PLOT) {

                                    // create the line plot for the sequence dependent fragment ions
                                    JFreeChart lineChart = PlotUtil.getLinePlot(sequenceDependentFragmentIons,
                                            numberOfSpectraOfGivenLength,
                                            "Residue Number", "Occurence (%)");

                                    if (!properties.showLegend()) {
                                        lineChart.getLegend().setVisible(false);
                                    }

                                    ChartPanel chartPanel = new ChartPanel(lineChart);

                                    String internalFrameTitle = "" + currentIdentification.getModifiedSequence() 
                                            + " (SID: " + currentIdentification.getSpectrumFileId() + ", 1 spectrum|"
                                            + totalNumberOfFragmentIons + " fragment ions)";
                                    String plotType = "FragmentIonProbabilityPlot";

                                    FragmentationAnalyzerJInternalFrame internalFrame = new FragmentationAnalyzerJInternalFrame(
                                            internalFrameTitle, true, true, true, chartPanel, plotType, internalFrameUniqueIdCounter);
                                    internalFrame.add(chartPanel);

                                    insertInternalFrame(internalFrame);
                                    properties.getAllChartFrames().put(internalFrameUniqueIdCounter, lineChart);
                                    internalFrameUniqueIdCounter++;


                                    // create the bar plot for the precusor and immonium ions
                                    JFreeChart barChart = PlotUtil.getBarPlot(sequenceIndependentFragmentIons,
                                            1, "Ion Type", "Occurence (%)");


                                    if (!properties.showLegend()) {
                                        barChart.getLegend().setVisible(false);
                                    }

                                    ChartPanel barChartPanel = new ChartPanel(barChart);
                                    plotType = "BarPlot";

                                    FragmentationAnalyzerJInternalFrame internalFrameBarChart = new FragmentationAnalyzerJInternalFrame(
                                            internalFrameTitle, true, true, true, barChartPanel, plotType, internalFrameUniqueIdCounter);
                                    internalFrameBarChart.add(barChartPanel);

                                    insertInternalFrame(internalFrameBarChart);
                                    properties.getAllChartFrames().put(internalFrameUniqueIdCounter, barChart);
                                    internalFrameUniqueIdCounter++;

                                    totalNumberOfFragmentIons = 0;
                                } else {

                                    // update the totalNumberOfSpectraOfGivenLength array
                                    for(int k = 1; k < currentIdentification.getSequence().length(); k++){
                                        totalNumberOfSpectraOfGivenLength[k]++;
                                    }

                                    // update the list of average sequence dependent fragment ions
                                    PlotUtil.updateAverageSequenceDependentFragmentIons(averageSequenceDependentFragmentIons,
                                            sequenceDependentFragmentIons, numberOfSpectraOfGivenLength,
                                            longestPeptideSequenceLength, i,
                                            properties.getCurrentlySelectedRowsInSpectraTable().size());
                                }

                                sequenceDependentFragmentIons = new HashMap<String, int[]>();
                                sequenceIndependentFragmentIons = new HashMap<String, Integer>();
                                numberOfSpectraOfGivenLength = new int[longestPeptideSequenceLength + 1];

                            } catch (SQLException e) {
                                JOptionPane.showMessageDialog(
                                        null, "An error occured when accessing the database." +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Accessing Database", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when accessing the database: ");
                                e.printStackTrace();
                            } catch (IOException e) {
                                JOptionPane.showMessageDialog(
                                        null, "An error occured when trying to create an ion probability plot." +
                                        "See ../Properties/ErrorLog.txt for more details.",
                                        "Error Creating Mass Error Plot", JOptionPane.ERROR_MESSAGE);
                                Util.writeToErrorLog("Error when creating ion probability plot: ");
                                e.printStackTrace();
                            }
                        }

                        // if combine, create plot here
                        if (combineSpectraJComboBox.getSelectedIndex() == Properties.COMBINE_PLOT) {

                            // create the line plot for the sequence dependent fragment ions
                            JFreeChart lineChart = PlotUtil.getAverageLinePlot(averageSequenceDependentFragmentIons,
                                    totalNumberOfSpectraOfGivenLength,
                                    "Fragment Ion Number", "Occurence (%)", properties);

                            if (!properties.showLegend()) {
                                lineChart.getLegend().setVisible(false);
                            }

                            ChartPanel lineChartPanel = new ChartPanel(lineChart);

                            String internalFrameTitle = "";

                            if (Util.verifyEqualModifiedSeqences(false, properties) || properties.getCurrentlySelectedRowsInSpectraTable().size() == 1) {
                                internalFrameTitle = properties.getCurrentlySelectedRowsInSpectraTable().get(0).getModifiedSequence();
                            } else {
                                internalFrameTitle = "Fragment Ion Probability Plot";
                            }

                            if(properties.getCurrentlySelectedRowsInSpectraTable().size() == 1){
                                internalFrameTitle += " (SID: " + properties.getCurrentlySelectedRowsInSpectraTable().get(0).getSpectrumId() + ", ";
                            } else {
                                internalFrameTitle += " (";
                            }

                            internalFrameTitle += properties.getCurrentlySelectedRowsInSpectraTable().size() 
                                    + " spectra|" + totalNumberOfFragmentIons + " fragment ions)";

                            String plotType = "FragmentIonProbabilityPlot";

                            FragmentationAnalyzerJInternalFrame internalFrame = new FragmentationAnalyzerJInternalFrame(
                                    internalFrameTitle, true, true, true, lineChartPanel, plotType, internalFrameUniqueIdCounter);
                            internalFrame.add(lineChartPanel);

                            insertInternalFrame(internalFrame);
                            properties.getAllChartFrames().put(internalFrameUniqueIdCounter, lineChart);
                            internalFrameUniqueIdCounter++;


                            // create the box plot for the spread
//                            insertFragmentIonProbabilityBoxPlot(averageSequenceDependentFragmentIons,
//                                    "Fragment Ion Type", "Occurence (%)",
//                                    properties, internalFrameTitle);


                            // create the bar plot for the precusor and immonium ions
                            JFreeChart barChart = PlotUtil.getBarPlot(sequenceIndependentFragmentIons,
                                    properties.getCurrentlySelectedRowsInSpectraTable().size(),
                                    "Ion Type", "Occurence (%)");


                            if (!properties.showLegend()) {
                                barChart.getLegend().setVisible(false);
                            }

                            ChartPanel barChartPanel = new ChartPanel(barChart);
                            plotType = "BarPlot";

                            FragmentationAnalyzerJInternalFrame internalFrameBarChart = new FragmentationAnalyzerJInternalFrame(
                                    internalFrameTitle, true, true, true, barChartPanel, plotType, internalFrameUniqueIdCounter);
                            internalFrameBarChart.add(barChartPanel);

                            insertInternalFrame(internalFrameBarChart);
                            properties.getAllChartFrames().put(internalFrameUniqueIdCounter, barChart);
                            internalFrameUniqueIdCounter++;
                        }
                    }

                    progressDialog.setVisible(false);
                    progressDialog.dispose();
                }
            }.start();
        }
    }//GEN-LAST:event_spectraJButtonActionPerformed

    /**
     * Adds the fragment ions for the given identification to the data set.
     *
     * @param currentIdentification
     * @param data
     * @return int number of fragment ions added
     * @throws IOException
     * @throws SQLException
     */
    private int addFragmentIonsToIonProbabilityPlot(
            ReducedIdentification currentIdentification,
            HashMap<String, int[]> sequenceDependentFragmentIons,
            HashMap<String, Integer> sequenceIndependentFragmentIons,
            int peptideSequenceLength, boolean invertXYZ)
            throws IOException, SQLException {

        // ToDo: This method ought to be moved into the PlotUtil class

        int numberOfFragmentIonsUsed = 0;

        if (currentDataSetIsFromMsLims) {

            // get the fragment ions
            Vector<Fragmention> fragmentIons = (Vector<Fragmention>) Fragmention.getAllFragmentions(
                    getConnection(), (long) currentIdentification.getIdentificationId());

            for (int j = 0; j < fragmentIons.size(); j++) {

                if (userProperties.isScoringTypeSelected(fragmentIons.get(j).getL_ionscoringid())) {

                    numberOfFragmentIonsUsed++;

                    Fragmention currentFragmentIon = fragmentIons.get(j);

                    String fragmentIonType = currentFragmentIon.getIonname();
                    int fragmentIonNumber = (int) currentFragmentIon.getFragmentionnumber();
                    String fragmentIonNumberAsString = "" + fragmentIonNumber;

                    // add padding enable correct sorting
                    if (fragmentIonNumber < 10) {
                        fragmentIonNumberAsString = "0" + fragmentIonNumberAsString;
                    }

                    if (fragmentIonType.startsWith("a") || fragmentIonType.startsWith("b") || fragmentIonType.startsWith("c") ||
                            fragmentIonType.startsWith("x") || fragmentIonType.startsWith("y") || fragmentIonType.startsWith("z")) {
                        if (fragmentIonType.length() > 1) {
                            fragmentIonType = fragmentIonType.substring(0, 1) + "[" + fragmentIonNumberAsString + "]" + fragmentIonType.substring(1);
                        } else {
                            fragmentIonType = fragmentIonType.substring(0, 1) + fragmentIonNumberAsString;
                        }
                    }

                    // get the fragment ion label. y3 -> y, y[4]++-H2O -> y++-H2O
                    String currentFragmentIonLabel = FragmentIon.getFragmentIonLabel(fragmentIonType, fragmentIonNumber);

                    // invert the x, y and z ions
                    if (invertXYZ) {
                        if (fragmentIonType.startsWith("x") || fragmentIonType.startsWith("y") || fragmentIonType.startsWith("z")) {
                            fragmentIonNumber = peptideSequenceLength - fragmentIonNumber;
                        }
                    }

                    if (fragmentIonNumber != 0) {
                        if (sequenceDependentFragmentIons.get(currentFragmentIonLabel) != null) {
                            int[] tempArray = sequenceDependentFragmentIons.get(currentFragmentIonLabel);
                            tempArray[fragmentIonNumber]++;
                            //data.put(currentFragmentIonLabel, tempArray);
                        } else {
                            int[] tempArray = new int[peptideSequenceLength + 1];
                            tempArray[fragmentIonNumber]++;
                            sequenceDependentFragmentIons.put(currentFragmentIonLabel, tempArray);
                        }
                    } else {
                        if (sequenceIndependentFragmentIons.get(currentFragmentIonLabel) != null) {
                            sequenceIndependentFragmentIons.put(
                                    currentFragmentIonLabel, sequenceIndependentFragmentIons.get(currentFragmentIonLabel) + 1);
                        } else {
                            sequenceIndependentFragmentIons.put(currentFragmentIonLabel, 1);
                        }
                    }
                }
            }
        } else {

            // get the fragment ions
            ArrayList<FragmentIon> fragmentIons = getFragmentIons(currentIdentification.getSpectrumFileId(), null);

            for (int k = 0; k < fragmentIons.size(); k++) {

                numberOfFragmentIonsUsed++;

                FragmentIon currentFragmentIon = fragmentIons.get(k);

                String fragmentIonType = currentFragmentIon.getFragmentIonType();
                Integer fragmentIonNumber = currentFragmentIon.getFragmentIonNumber();

                // add padding to enable correct sorting
                if (fragmentIonNumber < 10) {
                    fragmentIonType = fragmentIonType.replaceFirst("" + fragmentIonNumber, "0" + fragmentIonNumber);
                }

                // get the fragment ion label. y3 -> y, y[4]++-H2O -> y++-H2O
                String currentFragmentIonLabel = currentFragmentIon.getFragmentIonLabel();

                // invert the x, y and z ions
                if (invertXYZ) {
                    if (fragmentIonType.startsWith("x") || fragmentIonType.startsWith("y") || fragmentIonType.startsWith("z")) {
                        fragmentIonNumber = peptideSequenceLength - fragmentIonNumber;
                    }
                }

                if (fragmentIonNumber != 0) {
                    if (sequenceDependentFragmentIons.get(currentFragmentIonLabel) != null) {
                        int[] tempArray = sequenceDependentFragmentIons.get(currentFragmentIonLabel);
                        tempArray[fragmentIonNumber]++;
                        //data.put(currentFragmentIonLabel, tempArray);
                    } else {
                        int[] tempArray = new int[peptideSequenceLength + 1];
                        tempArray[fragmentIonNumber]++;
                        sequenceDependentFragmentIons.put(currentFragmentIonLabel, tempArray);
                    }
                } else {
                    if (sequenceIndependentFragmentIons.get(currentFragmentIonLabel) != null) {
                        sequenceIndependentFragmentIons.put(
                                currentFragmentIonLabel, sequenceIndependentFragmentIons.get(currentFragmentIonLabel) + 1);
                    } else {
                        sequenceIndependentFragmentIons.put(currentFragmentIonLabel, 1);
                    }
                }
            }
        }

        return numberOfFragmentIonsUsed;
    }

    /**
     * Extract the fragment ion for the given spectrum id from the fragmentIons text file.
     *
     * @param spectrumId the spectrum id to extract
     * @param type the fragment ion type to extract (only singly charged and ions with no neutral losses are returned)
     * @return the list of extracted fragment ions
     * @throws IOException
     */
    private ArrayList<FragmentIon> getFragmentIons(int spectrumId, String type) throws IOException {

        // ToDo: This method ought to be moved into the PlotUtil class

        ArrayList<FragmentIon> currentFragmentIons = new ArrayList<FragmentIon>();

        FileReader r = new FileReader(properties.getCurrentDataSetFolder() + "/fragmentIons.txt");
        BufferedReader b = new BufferedReader(r);

        String currentLine = b.readLine();

        while (currentLine != null) {

            FragmentIon currentFragmentIon = new FragmentIon(currentLine);

            if (currentFragmentIon.getIdentificationId().intValue() == spectrumId) {
                if (type != null) {

                    String currentType = currentFragmentIon.getFragmentIonType();

                    if (currentType.startsWith(type)) {

                        // only keep singly charged ions with no neutral losses
                        if (currentType.lastIndexOf("-H2O") == -1 &&
                                currentType.lastIndexOf("-H20") == -1 &&
                                currentType.lastIndexOf("-NH3") == -1 &&
                                currentType.lastIndexOf("[") == -1) {
                            currentFragmentIons.add(currentFragmentIon);
                        }
                    }
                } else {
                    currentFragmentIons.add(currentFragmentIon);
                }
            }

            currentLine = b.readLine();
        }

        return currentFragmentIons;
    }

    /**
     * Removes all the currently opened internal frames and resets the size of the
     * plots and analysis panel.
     *
     * @param evt
     */
    private void closeAllInternalFramesJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeAllInternalFramesJMenuItemActionPerformed

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        Iterator<Integer> iterator = properties.getAllInternalFrames().keySet().iterator();

        // store the keys in a list first to escape a ConcurrentModificationException
        ArrayList<Integer> keys = new ArrayList<Integer>();

        while (iterator.hasNext()) {
            keys.add(iterator.next());
        }

        for (int i = 0; i < keys.size(); i++) {
            properties.getAllInternalFrames().get(keys.get(i)).setVisible(false);
            properties.getAllInternalFrames().get(keys.get(i)).dispose();
        }

        properties.setAllInternalFrames(new HashMap<Integer, FragmentationAnalyzerJInternalFrame>());
        properties.setLinkedSpectrumPanels(new HashMap<Integer, SpectrumPanel>());
        properties.setAllAnnotations(new HashMap<Integer, Vector<DefaultSpectrumAnnotation>>());
        properties.setAllChartFrames(new HashMap<Integer, JFreeChart>());

        spectrumPanelToolBarJInternalFrame.setVisible(false);
        boxPlotPanelToolBarJInternalFrame.setVisible(false);

        plotsAndAnalysesJDesktopPane.setPreferredSize(plotsAndAnalysesJScrollPane.getMinimumSize());
        plotPaneCurrentPreferredSize = plotsAndAnalysesJDesktopPane.getPreferredSize();
        plotPaneCurrentScrollValue = 0;

        internalFrameIsMaximized = false;

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_closeAllInternalFramesJMenuItemActionPerformed

    /**
     * Filters the annotations and returns the annotations matching the currently selected list.
     *
     * @param annotations the annotations to be filtered
     * @return the filtered annotations
     */
    private Vector<DefaultSpectrumAnnotation> filterAnnotations(Vector<DefaultSpectrumAnnotation> annotations) {

        // ToDo: This method could be moved into the PlotUtil class?

        Vector<DefaultSpectrumAnnotation> filteredAnnotations = new Vector();

        for (int i = 0; i < annotations.size(); i++) {
            String currentLabel = annotations.get(i).getLabel();

            boolean useAnnotation = true;

            // check ion type
            if (currentLabel.startsWith("a")) {
                if (!aIonsJCheckBox.isSelected()) {
                    useAnnotation = false;
                }
            } else if (currentLabel.startsWith("b")) {
                if (!bIonsJCheckBox.isSelected()) {
                    useAnnotation = false;
                }
            } else if (currentLabel.startsWith("c")) {
                if (!cIonsJCheckBox.isSelected()) {
                    useAnnotation = false;
                }
            } else if (currentLabel.startsWith("x")) {
                if (!xIonsJCheckBox.isSelected()) {
                    useAnnotation = false;
                }
            } else if (currentLabel.startsWith("y")) {
                if (!yIonsJCheckBox.isSelected()) {
                    useAnnotation = false;
                }
            } else if (currentLabel.startsWith("z")) {
                if (!zIonsJCheckBox.isSelected()) {
                    useAnnotation = false;
                }
            } else if (currentLabel.startsWith("z")) {
                if (!zIonsJCheckBox.isSelected()) {
                    useAnnotation = false;
                }
            } else {
                if (!otherIonsJCheckBox.isSelected()) {
                    useAnnotation = false;
                }
            }

            // check neutral losses
            if (useAnnotation) {
                if (currentLabel.lastIndexOf("-H2O") != -1 || currentLabel.lastIndexOf("-H20") != -1) {
                    if (!H2OIonsJCheckBox.isSelected()) {
                        useAnnotation = false;
                    }
                }

                if (currentLabel.lastIndexOf("-NH3") != -1) {
                    if (!NH3IonsJCheckBox.isSelected()) {
                        useAnnotation = false;
                    }
                }
            }


            // check ion charge
            if (useAnnotation) {
                if (currentLabel.lastIndexOf("+") == -1) {

                    // test needed to be able to show ions in the "other" group
                    if (currentLabel.startsWith("a") || currentLabel.startsWith("b") || currentLabel.startsWith("c")
                            || currentLabel.startsWith("x") || currentLabel.startsWith("y") || currentLabel.startsWith("z")) {
                        if (!chargeOneJCheckBox.isSelected()) {
                            useAnnotation = false;
                        }
                    }
                } else if (currentLabel.lastIndexOf("+++") != -1) {
                    if (!chargeOverTwoJCheckBox.isSelected()) {
                        useAnnotation = false;
                    }
                } else if (currentLabel.lastIndexOf("++") != -1) {
                    if (!chargeTwoJCheckBox.isSelected()) {
                        useAnnotation = false;
                    }
                }
            }

            if (useAnnotation) {
                filteredAnnotations.add(annotations.get(i));
            }
        }

        return filteredAnnotations;
    }

    /**
     * Updates all the spectrum panels with the currently selected list of spectrum annotations.
     *
     * @param evt
     */
    private void aIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aIonsJCheckBoxActionPerformed

        Iterator<Integer> iterator = properties.getLinkedSpectrumPanels().keySet().iterator();

        while (iterator.hasNext()) {

            Integer key = iterator.next();
            SpectrumPanel currentSpectrumPanel = properties.getLinkedSpectrumPanels().get(key);
            Vector<DefaultSpectrumAnnotation> currentAnnotations = properties.getAllAnnotations().get(key);

            // update the ion coverage annotations
            currentSpectrumPanel.setAnnotations(filterAnnotations(currentAnnotations));
            currentSpectrumPanel.validate();
            currentSpectrumPanel.repaint();
        }
}//GEN-LAST:event_aIonsJCheckBoxActionPerformed

    /**
     * @see #aIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent)
     */
    private void bIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bIonsJCheckBoxActionPerformed
        aIonsJCheckBoxActionPerformed(null);
}//GEN-LAST:event_bIonsJCheckBoxActionPerformed

    /**
     * @see #aIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent)
     */
    private void cIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cIonsJCheckBoxActionPerformed
        aIonsJCheckBoxActionPerformed(null);
}//GEN-LAST:event_cIonsJCheckBoxActionPerformed

    /**
     * @see #aIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent)
     */
    private void yIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yIonsJCheckBoxActionPerformed
        aIonsJCheckBoxActionPerformed(null);
}//GEN-LAST:event_yIonsJCheckBoxActionPerformed

    /**
     * @see #aIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent)
     */
    private void xIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xIonsJCheckBoxActionPerformed
        aIonsJCheckBoxActionPerformed(null);
}//GEN-LAST:event_xIonsJCheckBoxActionPerformed

    /**
     * @see #aIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent)
     */
    private void zIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zIonsJCheckBoxActionPerformed
        aIonsJCheckBoxActionPerformed(null);
}//GEN-LAST:event_zIonsJCheckBoxActionPerformed

    /**
     * @see #aIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent)
     */
    private void chargeOneJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chargeOneJCheckBoxActionPerformed
        aIonsJCheckBoxActionPerformed(null);
}//GEN-LAST:event_chargeOneJCheckBoxActionPerformed

    /**
     * @see #aIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent)
     */
    private void chargeTwoJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chargeTwoJCheckBoxActionPerformed
        aIonsJCheckBoxActionPerformed(null);
}//GEN-LAST:event_chargeTwoJCheckBoxActionPerformed

    /**
     * @see #aIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent)
     */
    private void chargeOverTwoJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chargeOverTwoJCheckBoxActionPerformed
        aIonsJCheckBoxActionPerformed(null);
}//GEN-LAST:event_chargeOverTwoJCheckBoxActionPerformed

    /**
     * Makes sure that only the currently selected internal frame tool bar is visible.
     *
     * @param evt
     */
    private void showSpectrumToolBarJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showSpectrumToolBarJMenuItemActionPerformed
        spectrumPanelToolBarJInternalFrame.setVisible(true);
        boxPlotPanelToolBarJInternalFrame.setVisible(false);
}//GEN-LAST:event_showSpectrumToolBarJMenuItemActionPerformed

    /**
     * @see #aIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent)
     */
    private void otherIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_otherIonsJCheckBoxActionPerformed
        aIonsJCheckBoxActionPerformed(null);
}//GEN-LAST:event_otherIonsJCheckBoxActionPerformed

    /**
     * @see #aIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent)
     */
    private void H2OIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_H2OIonsJCheckBoxActionPerformed
        aIonsJCheckBoxActionPerformed(null);
}//GEN-LAST:event_H2OIonsJCheckBoxActionPerformed

    /**
     * @see #aIonsJCheckBoxActionPerformed(java.awt.event.ActionEvent)
     */
    private void NH3IonsJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NH3IonsJCheckBoxActionPerformed
        aIonsJCheckBoxActionPerformed(null);
}//GEN-LAST:event_NH3IonsJCheckBoxActionPerformed

    /**
     * Selects/deselected all the identifications in the search results table.
     *
     * @param evt
     */
    private void selectAllIdentificationsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllIdentificationsJMenuItemActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        boolean columnWasSorted = false;
        int sortedTableColumn = -1;
        SortOrder sortOrder = null;

        properties.setCurrentlySelectedRowsInSearchTable(new ArrayList<IdentificationTableRow>());

        if (searchResultsJXTable.getSortedColumn() != null) {
            sortedTableColumn = searchResultsJXTable.getSortedColumn().getModelIndex();
            sortOrder = searchResultsJXTable.getSortOrder(sortedTableColumn);
            searchResultsJXTable.setSortable(false);
            columnWasSorted = true;
        }

        if (properties.selectAllIdentifications()) {
            for (int i = 0; i < searchResultsJXTable.getRowCount(); i++) {
                searchResultsJXTable.setValueAt(Boolean.valueOf(true), i, searchResultsJXTable.getColumnCount() - 1);

                Integer countB = null;

                if (searchResultsJXTable.getColumnCount(false) == 7) {
                    countB = (Integer) searchResultsJXTable.getValueAt(i, 5);
                }

                properties.getCurrentlySelectedRowsInSearchTable().add(
                        new IdentificationTableRow(
                        (String) searchResultsJXTable.getValueAt(i, 1),
                        (String) searchResultsJXTable.getValueAt(i, 2),
                        (Integer) searchResultsJXTable.getValueAt(i, 3),
                        (Integer) searchResultsJXTable.getValueAt(i, 4),
                        countB));
            }
        } else {
            for (int i = 0; i < searchResultsJXTable.getRowCount(); i++) {
                if ((((Boolean) searchResultsJXTable.getValueAt(i, searchResultsJXTable.getColumnCount() - 1)).booleanValue())) {
                    searchResultsJXTable.setValueAt(Boolean.valueOf(false), i, searchResultsJXTable.getColumnCount() - 1);
                }
            }
        }

        if (properties.getCurrentlySelectedRowsInSearchTable().size() > 0) {
            searchResultsJComboBoxActionPerformed(null);
        } else {
            searchResultsJButton.setEnabled(false);
            searchResultsJButton.setToolTipText(searchResultAnalysisButtonDisabledToolTip);
        }

        properties.setSelectAllIdentifications(!properties.selectAllIdentifications());

        if(properties.selectAllIdentifications()){
            selectAllIdentificationsJMenuItem.setText("Select All");
        } else {
            selectAllIdentificationsJMenuItem.setText("Deselect All");
        }

        if (columnWasSorted) {
            searchResultsJXTable.setSortable(true);
            searchResultsJXTable.setSortOrder(sortedTableColumn, sortOrder);
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_selectAllIdentificationsJMenuItemActionPerformed

    /**
     * Inverts the selection in the search selection table.
     *
     * @param evt
     */
    private void invertSelectionIdentificationsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_invertSelectionIdentificationsJMenuItemActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        properties.setSelectAllIdentifications(true);
        selectAllIdentificationsJMenuItem.setText("Select All");

        boolean columnWasSorted = false;
        int sortedTableColumn = -1;
        SortOrder sortOrder = null;

        properties.setCurrentlySelectedRowsInSearchTable(new ArrayList<IdentificationTableRow>());

        if (searchResultsJXTable.getSortedColumn() != null) {
            sortedTableColumn = searchResultsJXTable.getSortedColumn().getModelIndex();
            sortOrder = searchResultsJXTable.getSortOrder(sortedTableColumn);
            searchResultsJXTable.setSortable(false);
            columnWasSorted = true;
        }

        for (int i = 0; i < searchResultsJXTable.getRowCount(); i++) {
            searchResultsJXTable.setValueAt(
                    !((Boolean) searchResultsJXTable.getValueAt(
                    i, searchResultsJXTable.getColumnCount() - 1)),
                    i, searchResultsJXTable.getColumnCount() - 1);

            if (((Boolean) searchResultsJXTable.getValueAt(i, searchResultsJXTable.getColumnCount() - 1)).booleanValue()) {

                Integer countB = null;

                if (searchResultsJXTable.getColumnCount(false) == 7) {
                    countB = (Integer) searchResultsJXTable.getValueAt(i, 5);
                }

                properties.getCurrentlySelectedRowsInSearchTable().add(
                        new IdentificationTableRow(
                        (String) searchResultsJXTable.getValueAt(i, 1),
                        (String) searchResultsJXTable.getValueAt(i, 2),
                        (Integer) searchResultsJXTable.getValueAt(i, 3),
                        (Integer) searchResultsJXTable.getValueAt(i, 4),
                        countB));
            }
        }

        if (properties.getCurrentlySelectedRowsInSearchTable().size() > 0) {
            searchResultsJComboBoxActionPerformed(null);
        } else {
            searchResultsJButton.setEnabled(false);
            searchResultsJButton.setToolTipText(searchResultAnalysisButtonDisabledToolTip);
        }

        if (columnWasSorted) {
            searchResultsJXTable.setSortable(true);
            searchResultsJXTable.setSortOrder(sortedTableColumn, sortOrder);
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_invertSelectionIdentificationsJMenuItemActionPerformed

    /**
     * Select/deselect all rows in the spectra table.
     *
     * @param evt
     */
    private void selectAllSpectrtaJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllSpectrtaJMenuItemActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        boolean columnWasSorted = false;
        int sortedTableColumn = -1;
        SortOrder sortOrder = null;

        properties.setCurrentlySelectedRowsInSpectraTable(new ArrayList<SpectrumTableRow>());

        if (spectraJXTable.getSortedColumn() != null) {
            sortedTableColumn = spectraJXTable.getSortedColumn().getModelIndex();
            sortOrder = spectraJXTable.getSortOrder(sortedTableColumn);
            spectraJXTable.setSortable(false);
            columnWasSorted = true;
        }

        if (properties.selectAllSpectra()) {
            for (int i = 0; i < spectraJXTable.getRowCount(); i++) {
                spectraJXTable.setValueAt(Boolean.valueOf(true), i, spectraJXTable.getColumnCount() - 1);
                properties.getCurrentlySelectedRowsInSpectraTable().add(
                        new SpectrumTableRow(
                        (Integer) spectraJXTable.getValueAt(i, 1),
                        (Integer) spectraJXTable.getValueAt(i, 2),
                        (String) spectraJXTable.getValueAt(i, 3),
                        (String) spectraJXTable.getValueAt(i, 4),
                        (Integer) spectraJXTable.getValueAt(i, 5),
                        (String) spectraJXTable.getValueAt(i, 6)));
            }
        } else {
            for (int i = 0; i < spectraJXTable.getRowCount(); i++) {
                if ((((Boolean) spectraJXTable.getValueAt(i, spectraJXTable.getColumnCount() - 1)).booleanValue())) {
                    spectraJXTable.setValueAt(new Boolean(false), i, spectraJXTable.getColumnCount() - 1);
                }
            }
        }

        if (properties.getCurrentlySelectedRowsInSpectraTable().size() > 0) {
            spectraJComboBoxActionPerformed(null);
        } else {
            spectraJButton.setEnabled(false);
            spectraJButton.setToolTipText(spectraAnalysisButtonDisabledToolTip);
        }

        properties.setSelectAllSpectra(!properties.selectAllSpectra());

        if(properties.selectAllSpectra()){
            selectAllSpectrtaJMenuItem.setText("Select All");
        } else {
            selectAllSpectrtaJMenuItem.setText("Deselect All");
        }

        if (columnWasSorted) {
            spectraJXTable.setSortable(true);
            spectraJXTable.setSortOrder(sortedTableColumn, sortOrder);
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_selectAllSpectrtaJMenuItemActionPerformed

    /**
     * Inverts the selection in the spectra table.
     *
     * @param evt
     */
    private void invertSelectionSpectraJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_invertSelectionSpectraJMenuItemActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        properties.setSelectAllSpectra(true);
        selectAllSpectrtaJMenuItem.setText("Select All");

        boolean columnWasSorted = false;
        int sortedTableColumn = -1;
        SortOrder sortOrder = null;

        properties.setCurrentlySelectedRowsInSpectraTable(new ArrayList<SpectrumTableRow>());

        if (spectraJXTable.getSortedColumn() != null) {
            sortedTableColumn = spectraJXTable.getSortedColumn().getModelIndex();
            sortOrder = spectraJXTable.getSortOrder(sortedTableColumn);
            spectraJXTable.setSortable(false);
            columnWasSorted = true;
        }

        for (int i = 0; i < spectraJXTable.getRowCount(); i++) {
            spectraJXTable.setValueAt(
                    !((Boolean) spectraJXTable.getValueAt(i,
                    spectraJXTable.getColumnCount() - 1)), i, spectraJXTable.getColumnCount() - 1);

            if (((Boolean) spectraJXTable.getValueAt(i, spectraJXTable.getColumnCount() - 1)).booleanValue()) {
                properties.getCurrentlySelectedRowsInSpectraTable().add(
                        new SpectrumTableRow(
                        (Integer) spectraJXTable.getValueAt(i, 1),
                        (Integer) spectraJXTable.getValueAt(i, 2),
                        (String) spectraJXTable.getValueAt(i, 3),
                        (String) spectraJXTable.getValueAt(i, 4),
                        (Integer) spectraJXTable.getValueAt(i, 5),
                        (String) spectraJXTable.getValueAt(i, 6)));
            }
        }

        if (properties.getCurrentlySelectedRowsInSpectraTable().size() > 0) {
            spectraJComboBoxActionPerformed(null);
        } else {
            spectraJButton.setEnabled(false);
            spectraJButton.setToolTipText(spectraAnalysisButtonDisabledToolTip);
        }

        if (columnWasSorted) {
            spectraJXTable.setSortable(true);
            spectraJXTable.setSortOrder(sortedTableColumn, sortOrder);
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_invertSelectionSpectraJMenuItemActionPerformed

    /**
     * Updated the list of selected rows if the user clicks in the selection column.
     * Right clicking in the selection column opens the selection popup menu.
     *
     * @param evt
     */
    private void searchResultsJXTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchResultsJXTableMouseReleased

        int column = searchResultsJXTable.columnAtPoint(evt.getPoint());
        int row = searchResultsJXTable.rowAtPoint(evt.getPoint());

        if (column != -1 && row != -1) {

            if (evt.getButton() == MouseEvent.BUTTON1) {

                if (column == searchResultsJXTable.getColumnCount() - 1) {

                    Integer countB = null;

                    if (searchResultsJXTable.getColumnCount(false) == 7) {
                        countB = (Integer) searchResultsJXTable.getValueAt(row, 5);
                    }

                    IdentificationTableRow temp = new IdentificationTableRow(
                            (String) searchResultsJXTable.getValueAt(row, 1),
                            (String) searchResultsJXTable.getValueAt(row, 2),
                            (Integer) searchResultsJXTable.getValueAt(row, 3),
                            (Integer) searchResultsJXTable.getValueAt(row, 4),
                            countB);

                    if (((Boolean) searchResultsJXTable.getValueAt(row, column)).booleanValue()) {
                        if (!properties.getCurrentlySelectedRowsInSearchTable().contains(temp)) {
                            properties.getCurrentlySelectedRowsInSearchTable().add(temp);
                        }
                    } else {
                        properties.getCurrentlySelectedRowsInSearchTable().remove(temp);
                    }
                }

                if (properties.getCurrentlySelectedRowsInSearchTable().size() > 0) {
                    searchResultsJComboBoxActionPerformed(null);
                } else {
                    searchResultsJButton.setEnabled(false);
                    searchResultsJButton.setToolTipText(searchResultAnalysisButtonDisabledToolTip);
                }
            } else if (evt.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                selectIdentificationsJPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
            }
        }
    }//GEN-LAST:event_searchResultsJXTableMouseReleased

    /**
     * Updated the list of selected rows if the user clicks in the selection column.
     * Right clicking in the selection column opens the selection popup menu.
     *
     * @param evt
     */
    private void spectraJXTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectraJXTableMouseReleased

        int column = spectraJXTable.columnAtPoint(evt.getPoint());
        int row = spectraJXTable.rowAtPoint(evt.getPoint());

        if (column != -1 && row != -1) {

            if (evt.getButton() == MouseEvent.BUTTON1) {

                if (column == spectraJXTable.getColumnCount() - 1) {

                    SpectrumTableRow temp = new SpectrumTableRow(
                            (Integer) spectraJXTable.getValueAt(row, 1),
                            (Integer) spectraJXTable.getValueAt(row, 2),
                            (String) spectraJXTable.getValueAt(row, 3),
                            (String) spectraJXTable.getValueAt(row, 4),
                            (Integer) spectraJXTable.getValueAt(row, 5),
                            (String) spectraJXTable.getValueAt(row, 6));

                    if (((Boolean) spectraJXTable.getValueAt(row, column)).booleanValue()) {

                        if (!properties.getCurrentlySelectedRowsInSpectraTable().contains(temp)) {
                            properties.getCurrentlySelectedRowsInSpectraTable().add(temp);
                        }
                    } else {
                        properties.getCurrentlySelectedRowsInSpectraTable().remove(temp);
                    }
                }

                if (properties.getCurrentlySelectedRowsInSpectraTable().size() > 0) {
                    spectraJComboBoxActionPerformed(null);
                } else {
                    spectraJButton.setEnabled(false);
                    spectraJButton.setToolTipText(spectraAnalysisButtonDisabledToolTip);
                }
            } else if (evt.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                selectSpectraJPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
            }
        }
    }//GEN-LAST:event_spectraJXTableMouseReleased

    /**
     * @see #showSpectrumToolBarJMenuItemActionPerformed(java.awt.event.ActionEvent)
     */
    private void showBoxPlotToolBarJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showBoxPlotToolBarJMenuItemActionPerformed
        boxPlotPanelToolBarJInternalFrame.setVisible(true);
        spectrumPanelToolBarJInternalFrame.setVisible(false);
}//GEN-LAST:event_showBoxPlotToolBarJMenuItemActionPerformed

    /**
     * Updates the current list of selected fragment ion types in the box plots.
     *
     * @param evt
     */
    private void bIonsBoxPlotJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bIonsBoxPlotJCheckBoxActionPerformed
        setSeriesVisible(bIonsBoxPlotJCheckBox.isSelected(), 0, "BoxPlot");
    }//GEN-LAST:event_bIonsBoxPlotJCheckBoxActionPerformed

    /**
     * @see #bIonsBoxPlotJCheckBoxActionPerformed(java.awt.event.ActionEvent)
     */
    private void bIonsUnmodifiedJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bIonsUnmodifiedJCheckBoxActionPerformed
        setSeriesVisible(bIonsUnmodifiedJCheckBox.isSelected(), 0, "BoxPlot_modification");
    }//GEN-LAST:event_bIonsUnmodifiedJCheckBoxActionPerformed

    /**
     * @see #bIonsBoxPlotJCheckBoxActionPerformed(java.awt.event.ActionEvent)
     */
    private void yIonsBoxPlotJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yIonsBoxPlotJCheckBoxActionPerformed
        setSeriesVisible(yIonsBoxPlotJCheckBox.isSelected(), 1, "BoxPlot");
    }//GEN-LAST:event_yIonsBoxPlotJCheckBoxActionPerformed

    /**
     * @see #bIonsBoxPlotJCheckBoxActionPerformed(java.awt.event.ActionEvent)
     */
    private void bIonsModifiedJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bIonsModifiedJCheckBoxActionPerformed
        setSeriesVisible(bIonsModifiedJCheckBox.isSelected(), 1, "BoxPlot_modification");
    }//GEN-LAST:event_bIonsModifiedJCheckBoxActionPerformed

    /**
     * @see #bIonsBoxPlotJCheckBoxActionPerformed(java.awt.event.ActionEvent)
     */
    private void yIonsUnmodifiedJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yIonsUnmodifiedJCheckBoxActionPerformed
        setSeriesVisible(yIonsUnmodifiedJCheckBox.isSelected(), 2, "BoxPlot_modification");
    }//GEN-LAST:event_yIonsUnmodifiedJCheckBoxActionPerformed

    /**
     * @see #bIonsBoxPlotJCheckBoxActionPerformed(java.awt.event.ActionEvent)
     */
    private void yIonsModifiedJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yIonsModifiedJCheckBoxActionPerformed
        setSeriesVisible(yIonsModifiedJCheckBox.isSelected(), 3, "BoxPlot_modification");
    }//GEN-LAST:event_yIonsModifiedJCheckBoxActionPerformed

    /**
     * Opens a DataSeriesSelection dialog where the data series
     * to be displayed can be chosen.
     */
    private void showDataSeriesSelectionJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showDataSeriesSelectionJMenuItemActionPerformed
        boxPlotPanelToolBarJInternalFrame.setVisible(false);
        spectrumPanelToolBarJInternalFrame.setVisible(false);

        Iterator<Integer> iterator = properties.getAllInternalFrames().keySet().iterator();

        boolean selectedFrameFound = false;

        // find the currently selected internal frame
        while (iterator.hasNext() && !selectedFrameFound) {
            Integer key = iterator.next();

            FragmentationAnalyzerJInternalFrame currentFrame = properties.getAllInternalFrames().get(key);

            if (currentFrame.isSelected()) {
                selectedFrameFound = true;
                new DataSeriesSelection(this, true, currentFrame);
            }
        }
    }//GEN-LAST:event_showDataSeriesSelectionJMenuItemActionPerformed

    /**
     * Opens a dialog where the preferences can be set.
     * 
     * @param evt
     */
    private void preferencesJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_preferencesJMenuItemActionPerformed
        new Preferences(this, true);
    }//GEN-LAST:event_preferencesJMenuItemActionPerformed

    /**
     * Opens a frame containing the help manual for FragmentationAnalyzer.
     *
     * @param evt
     */
    private void helpJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpJMenuItemActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(this, getClass().getResource("/no/uib/fragmentation_analyzer/helpfiles/FragmentationAnalyzer.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_helpJMenuItemActionPerformed

    /**
     * Opens a frame containing the About FragmentationAnalyzer information.
     *
     * @param evt
     */
    private void aboutJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutJMenuItemActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(this, getClass().getResource("/no/uib/fragmentation_analyzer/helpfiles/AboutFragmentationAnalyzer.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutJMenuItemActionPerformed

    /**
     * Closes the search result and spectra task panes if the plots pane is expanded.
     *
     * @param evt
     */
    private void plotsAnalysesJXTaskPaneMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_plotsAnalysesJXTaskPaneMouseClicked
        if (plotsAnalysesJXTaskPane.isExpanded()) {
            searchResultsJXTaskPane.setExpanded(false);
            spectraJXTaskPane.setExpanded(false);
        }
    }//GEN-LAST:event_plotsAnalysesJXTaskPaneMouseClicked

    /**
     * Selects all the higlighted rows in the search results table.
     *
     * @param evt
     */
    private void selectHighlightedIdentificationsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectHighlightedIdentificationsJMenuItemActionPerformed
        selectHighlightedIdentifications(true);
    }//GEN-LAST:event_selectHighlightedIdentificationsJMenuItemActionPerformed

    /**
     * Selects or deselects all the higlighted rows in the search results table.
     * 
     * @param select if true the rows are selected, if false the rows are deselected
     */
    private void selectHighlightedIdentifications(boolean select) {
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        properties.setSelectAllIdentifications(true);
        selectAllIdentificationsJMenuItem.setText("Select All");

        boolean columnWasSorted = false;
        int sortedTableColumn = -1;
        SortOrder sortOrder = null;

        if (searchResultsJXTable.getSortedColumn() != null) {
            sortedTableColumn = searchResultsJXTable.getSortedColumn().getModelIndex();
            sortOrder = searchResultsJXTable.getSortOrder(sortedTableColumn);
            searchResultsJXTable.setSortable(false);
            columnWasSorted = true;
        }

        int column = searchResultsJXTable.getColumnCount() - 1;

        int[] selectedRows = searchResultsJXTable.getSelectedRows();

        for (int i = 0; i < selectedRows.length; i++) {

            int currentRow = selectedRows[i];

            Integer countB = null;

            if (searchResultsJXTable.getColumnCount(false) == 7) {
                countB = (Integer) searchResultsJXTable.getValueAt(currentRow, 5);
            }

            IdentificationTableRow temp = new IdentificationTableRow(
                    (String) searchResultsJXTable.getValueAt(currentRow, 1),
                    (String) searchResultsJXTable.getValueAt(currentRow, 2),
                    (Integer) searchResultsJXTable.getValueAt(currentRow, 3),
                    (Integer) searchResultsJXTable.getValueAt(currentRow, 4),
                    countB);

            // select the row
            searchResultsJXTable.setValueAt(select, currentRow, column);

            // add the row to the list of selected rows
            if (((Boolean) searchResultsJXTable.getValueAt(currentRow, column)).booleanValue()) {
                if (!properties.getCurrentlySelectedRowsInSearchTable().contains(temp)) {
                    properties.getCurrentlySelectedRowsInSearchTable().add(temp);
                }
            } else {
                properties.getCurrentlySelectedRowsInSearchTable().remove(temp);
            }
        }

        if (properties.getCurrentlySelectedRowsInSearchTable().size() > 0) {
            searchResultsJComboBoxActionPerformed(null);
        } else {
            searchResultsJButton.setEnabled(false);
            searchResultsJButton.setToolTipText(searchResultAnalysisButtonDisabledToolTip);
        }

        if (columnWasSorted) {
            searchResultsJXTable.setSortable(true);
            searchResultsJXTable.setSortOrder(sortedTableColumn, sortOrder);
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Selects all the higlighted rows in the spectra table.
     *
     * @param evt
     */
    private void selectHighlightedSpectraJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectHighlightedSpectraJMenuItemActionPerformed
        selectHighlightedSpectra(true);
    }//GEN-LAST:event_selectHighlightedSpectraJMenuItemActionPerformed

    /**
     * Selects or deselects all the higlighted rows in the spectra table.
     * 
     * @param select if true the rows are selected, if false the rows are deselected
     */
    private void selectHighlightedSpectra(boolean select) {
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        properties.setSelectAllSpectra(true);
        selectAllSpectrtaJMenuItem.setText("Select All");

        boolean columnWasSorted = false;
        int sortedTableColumn = -1;
        SortOrder sortOrder = null;

        if (spectraJXTable.getSortedColumn() != null) {
            sortedTableColumn = spectraJXTable.getSortedColumn().getModelIndex();
            sortOrder = spectraJXTable.getSortOrder(sortedTableColumn);
            spectraJXTable.setSortable(false);
            columnWasSorted = true;
        }

        int column = spectraJXTable.getColumnCount() - 1;

        int[] selectedRows = spectraJXTable.getSelectedRows();

        for (int i = 0; i < selectedRows.length; i++) {

            int currentRow = selectedRows[i];

            SpectrumTableRow temp = new SpectrumTableRow(
                    (Integer) spectraJXTable.getValueAt(currentRow, 1),
                    (Integer) spectraJXTable.getValueAt(currentRow, 2),
                    (String) spectraJXTable.getValueAt(currentRow, 3),
                    (String) spectraJXTable.getValueAt(currentRow, 4),
                    (Integer) spectraJXTable.getValueAt(currentRow, 5),
                    (String) spectraJXTable.getValueAt(currentRow, 6));

            // select the row
            spectraJXTable.setValueAt(select, currentRow, column);

            // add the row to the list of selected rows
            if (((Boolean) spectraJXTable.getValueAt(currentRow, column)).booleanValue()) {
                if (!properties.getCurrentlySelectedRowsInSpectraTable().contains(temp)) {
                    properties.getCurrentlySelectedRowsInSpectraTable().add(temp);
                }
            } else {
                properties.getCurrentlySelectedRowsInSpectraTable().remove(temp);
            }
        }

        if (properties.getCurrentlySelectedRowsInSpectraTable().size() > 0) {
            spectraJComboBoxActionPerformed(null);
        } else {
            spectraJButton.setEnabled(false);
            spectraJButton.setToolTipText(spectraAnalysisButtonDisabledToolTip);
        }

        if (columnWasSorted) {
            spectraJXTable.setSortable(true);
            spectraJXTable.setSortOrder(sortedTableColumn, sortOrder);
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Deselects all the higlighted rows in the search results table.
     *
     * @param evt
     */
    private void deselectHighlightedIdentificationsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deselectHighlightedIdentificationsJMenuItemActionPerformed
        selectHighlightedIdentifications(false);
    }//GEN-LAST:event_deselectHighlightedIdentificationsJMenuItemActionPerformed

    /**
     * Deselects all the higlighted rows in the spectra table.
     *
     * @param evt
     */
    private void deselectHighlightedSpectraJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deselectHighlightedSpectraJMenuItemActionPerformed
        selectHighlightedSpectra(false);
    }//GEN-LAST:event_deselectHighlightedSpectraJMenuItemActionPerformed

    /**
     * Turns on or off the displaying of the chart legends.
     * 
     * @param evt
     */
    private void showLegendsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showLegendsJMenuItemActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        Iterator<Integer> iterator = properties.getAllInternalFrames().keySet().iterator();

        // store the keys in a list first to escape a ConcurrentModificationException
        ArrayList<Integer> keys = new ArrayList<Integer>();

        while (iterator.hasNext()) {
            keys.add(iterator.next());
        }

        properties.setShowLegend(!properties.showLegend());

        for (int i = 0; i < keys.size(); i++) {

            ChartPanel tepmChartPanel = properties.getAllInternalFrames().get(keys.get(i)).getChartPanel();

            if (tepmChartPanel != null) {
                LegendTitle tempLegend = tepmChartPanel.getChart().getLegend();

                if (tempLegend != null) {
                    tempLegend.setVisible(properties.showLegend());
                }
            }
        }

        if (properties.showLegend()) {
            showLegendsJMenuItem.setText("Hide Legends");
        } else {
            showLegendsJMenuItem.setText("Show Legends");
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_showLegendsJMenuItemActionPerformed

    /**
     * Turns on or off the displaying of the markers.
     * 
     * @param evt
     */
    private void showMarkersJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showMarkersJMenuItemActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        Iterator<Integer> internalFramesIterator = properties.getAllInternalFrames().keySet().iterator();

        // store the keys in a list first to escape a ConcurrentModificationException
        ArrayList<Integer> keys = new ArrayList<Integer>();

        while (internalFramesIterator.hasNext()) {
            keys.add(internalFramesIterator.next());
        }

        properties.setShowMarkers(!properties.showMarkers());

        for (int i = 0; i < keys.size(); i++) {

            ChartPanel tempChartPanel = properties.getAllInternalFrames().get(keys.get(i)).getChartPanel();

            if (tempChartPanel != null) {

                Collection markers = null;

                if (tempChartPanel.getChart().getPlot() instanceof XYPlot) {
                    markers = ((XYPlot) tempChartPanel.getChart().getPlot()).getDomainMarkers(Layer.BACKGROUND);
                } else if (tempChartPanel.getChart().getPlot() instanceof CategoryPlot) {
                    markers = ((CategoryPlot) tempChartPanel.getChart().getPlot()).getDomainMarkers(Layer.BACKGROUND);
                }
            
                if (markers != null) {
                    Iterator markerIterator = markers.iterator();

                    HashMap<String, Integer> seriesKeyToSeriesNumber = new HashMap<String, Integer>();

                    if (tempChartPanel.getChart().getPlot() instanceof XYPlot) {
                        XYDataset xyDataSet = ((XYPlot) tempChartPanel.getChart().getPlot()).getDataset(0);

                        // get all the data series keys
                        for (int j = 0; j < xyDataSet.getSeriesCount(); j++) {
                            seriesKeyToSeriesNumber.put(xyDataSet.getSeriesKey(j).toString(), j);
                        }
                    }

                    while (markerIterator.hasNext()) {

                        Marker tempMarker = ((Marker) markerIterator.next());

                        if (properties.showMarkers()) {
                            if (tempChartPanel.getChart().getPlot() instanceof XYPlot) {
                                if(tempMarker.getLabel() == null){
                                    tempMarker.setAlpha(Properties.DEFAULT_VISIBLE_MARKER_ALPHA);
                                } else {
                                    if (((XYPlot) tempChartPanel.getChart().getPlot()).getRenderer(0).isSeriesVisible(
                                            seriesKeyToSeriesNumber.get(tempMarker.getLabel()))) {
                                        tempMarker.setAlpha(Properties.DEFAULT_VISIBLE_MARKER_ALPHA);
                                    } else {
                                        tempMarker.setAlpha(Properties.DEFAULT_NON_VISIBLE_MARKER_ALPHA);
                                    }
                                }
                            } else if (tempChartPanel.getChart().getPlot() instanceof CategoryPlot) {
                                tempMarker.setAlpha(Properties.DEFAULT_VISIBLE_MARKER_ALPHA);
                            }
                        } else {
                            tempMarker.setAlpha(Properties.DEFAULT_NON_VISIBLE_MARKER_ALPHA);
                        }
                    }
                }
            }
        }

        if (properties.showMarkers()) {
            showMarkersJMenuItem.setText("Hide Markers");
        } else {
            showMarkersJMenuItem.setText("Show Markers");
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_showMarkersJMenuItemActionPerformed

    /**
     * Show or hide the average mass error line.
     * 
     * @param evt
     */
    private void showAverageJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showAverageJMenuItemActionPerformed

        Iterator<Integer> iterator = properties.getAllChartFrames().keySet().iterator();

        properties.setShowAverageMassError(!properties.showAverageMassError());

        while (iterator.hasNext()) {

            Integer key = iterator.next();

            JFreeChart tempChart = properties.getAllChartFrames().get(key);
            String tempPlotType = properties.getAllInternalFrames().get(key).getInternalFrameType();

            if (tempPlotType.equalsIgnoreCase("MassErrorScatterPlot") ||
                    tempPlotType.equalsIgnoreCase("MassErrorBubblePlot")) {

                if (((XYPlot) tempChart.getPlot()).getRenderer(1) != null) {
                    ((XYPlot) tempChart.getPlot()).getRenderer(1).setSeriesVisible(0, properties.showAverageMassError());
                }
            }
        }

        if (properties.showAverageMassError()) {
            showAverageJMenuItem.setText("Hide Average Mass Errors");
        } else {
            showAverageJMenuItem.setText("Show Average Mass Errors");
        }
    }//GEN-LAST:event_showAverageJMenuItemActionPerformed

    /**
     * Resizes components in relation to the new size of the frame.
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        if (initialSizeHasBeenSet) {

            // make sure the main split pane splitter is correctly located
            if(showLeftPanelJMenuItem.getText().equalsIgnoreCase("Hide Left Panel")){
                mainJSplitPane.setDividerLocation(Properties.DEFAULT_MAIN_SPLITTER_LOCATION);
            } else {
                mainJSplitPane.setDividerLocation(0);
            }

            // set the size of the results and spectra table task panes
            int newHeight = (resultsJScrollPane.getHeight() / 2) - 85; // ToDo: remove hardcoding
            searchResultJXPanel.setPreferredSize(new Dimension(0, newHeight));
            spectraJXPanel.setPreferredSize(new Dimension(0, newHeight));

            // set the size of the plot task panes
            newHeight = resultsJScrollPane.getHeight() - 150; // ToDo: remove hardcoding
            plotsAndAnalysesJScrollPane.setPreferredSize(new Dimension(0, newHeight));


            // move the spectrum and box plot tool bars
            spectrumPanelToolBarJInternalFrame.setBounds(
                    (int) plotsAndAnalysesJDesktopPane.getBounds().getWidth() - 100,
                    (int) spectrumPanelToolBarJInternalFrame.getBounds().getY(),
                    (int) spectrumPanelToolBarJInternalFrame.getBounds().getWidth(),
                    (int) spectrumPanelToolBarJInternalFrame.getBounds().getHeight());

            boxPlotPanelToolBarJInternalFrame.setBounds(
                    (int) plotsAndAnalysesJDesktopPane.getBounds().getWidth() - 120,
                    (int) boxPlotPanelToolBarJInternalFrame.getBounds().getY(),
                    (int) boxPlotPanelToolBarJInternalFrame.getBounds().getWidth(),
                    (int) boxPlotPanelToolBarJInternalFrame.getBounds().getHeight());

            repaint();

            // invoke later to give time for components to update
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    orderInternalFrames();
                }
            });
        }
    }//GEN-LAST:event_formComponentResized

    /**
     * @see #formComponentResized(java.awt.event.ComponentEvent) 
     */
    private void formWindowStateChanged(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowStateChanged
        formComponentResized(null);
        setVisible(true);
    }//GEN-LAST:event_formWindowStateChanged

    /**
     * @see #setVariableComoboBoxPopupMenuWidth()
     */
    private void nTermJComboBoxPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_nTermJComboBoxPopupMenuWillBecomeVisible
        setVariableComoboBoxPopupMenuWidth(evt);
    }//GEN-LAST:event_nTermJComboBoxPopupMenuWillBecomeVisible

    /**
     * @see #setVariableComoboBoxPopupMenuWidth()
     */
    private void chargeJComboBoxPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_chargeJComboBoxPopupMenuWillBecomeVisible
        setVariableComoboBoxPopupMenuWidth(evt);
    }//GEN-LAST:event_chargeJComboBoxPopupMenuWillBecomeVisible

    /**
     * @see #setVariableComoboBoxPopupMenuWidth()
     */
    private void instrument1JComboBoxPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_instrument1JComboBoxPopupMenuWillBecomeVisible
        setVariableComoboBoxPopupMenuWidth(evt);
    }//GEN-LAST:event_instrument1JComboBoxPopupMenuWillBecomeVisible

    /**
     * @see #setVariableComoboBoxPopupMenuWidth()
     */
    private void instrument2JComboBoxPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_instrument2JComboBoxPopupMenuWillBecomeVisible
        setVariableComoboBoxPopupMenuWidth(evt);
    }//GEN-LAST:event_instrument2JComboBoxPopupMenuWillBecomeVisible

    /**
     * @see #setVariableComoboBoxPopupMenuWidth()
     */
    private void instrument3JComboBoxPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_instrument3JComboBoxPopupMenuWillBecomeVisible
        setVariableComoboBoxPopupMenuWidth(evt);
    }//GEN-LAST:event_instrument3JComboBoxPopupMenuWillBecomeVisible

    /**
     * @see #setVariableComoboBoxPopupMenuWidth()
     */
    private void cTermJComboBoxPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_cTermJComboBoxPopupMenuWillBecomeVisible
        setVariableComoboBoxPopupMenuWidth(evt);
    }//GEN-LAST:event_cTermJComboBoxPopupMenuWillBecomeVisible

    /**
     * @see #setVariableComoboBoxPopupMenuWidth()
     */
    private void modification1JComboBoxPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_modification1JComboBoxPopupMenuWillBecomeVisible
        setVariableComoboBoxPopupMenuWidth(evt);
    }//GEN-LAST:event_modification1JComboBoxPopupMenuWillBecomeVisible

    /**
     * @see #setVariableComoboBoxPopupMenuWidth()
     */
    private void modification2JComboBoxPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_modification2JComboBoxPopupMenuWillBecomeVisible
        setVariableComoboBoxPopupMenuWidth(evt);
    }//GEN-LAST:event_modification2JComboBoxPopupMenuWillBecomeVisible

    /**
     * @see #setVariableComoboBoxPopupMenuWidth()
     */
    private void modification3JComboBoxPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_modification3JComboBoxPopupMenuWillBecomeVisible
        setVariableComoboBoxPopupMenuWidth(evt);
    }//GEN-LAST:event_modification3JComboBoxPopupMenuWillBecomeVisible

    /**
     * @see #setVariableComoboBoxPopupMenuWidth()
     */
    private void searchResultsJComboBoxPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_searchResultsJComboBoxPopupMenuWillBecomeVisible
        setVariableComoboBoxPopupMenuWidth(evt);
    }//GEN-LAST:event_searchResultsJComboBoxPopupMenuWillBecomeVisible

    /**
     * @see #setVariableComoboBoxPopupMenuWidth()
     */
    private void spectraJComboBoxPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_spectraJComboBoxPopupMenuWillBecomeVisible
        setVariableComoboBoxPopupMenuWidth(evt);
    }//GEN-LAST:event_spectraJComboBoxPopupMenuWillBecomeVisible

    /**
     * Hides or shows the left part of the main frame (mainly the search parameters).
     *
     * @param evt
     */
    private void showLeftPanelJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showLeftPanelJMenuItemActionPerformed

        if(mainJSplitPane.getDividerLocation() == 0){
            mainJSplitPane.setDividerLocation(Properties.DEFAULT_MAIN_SPLITTER_LOCATION);
            showLeftPanelJMenuItem.setText("Hide Left Panel");
        } else {
            mainJSplitPane.setDividerLocation(0);
            showLeftPanelJMenuItem.setText("Show Left Panel");
        }

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                orderInternalFrames();
            }
        });
    }//GEN-LAST:event_showLeftPanelJMenuItemActionPerformed

    /**
     * Opens a dialog where the new title of the internal frame can be inserted.
     *
     * @param evt
     */
    private void setTitleJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setTitleJMenuItemActionPerformed
        Iterator<Integer> iterator = properties.getAllInternalFrames().keySet().iterator();

        boolean selectedFrameFound = false;

        FragmentationAnalyzerJInternalFrame currentFrame = null;

        // find the currently selected internal frame
        while (iterator.hasNext() && !selectedFrameFound) {
            Integer key = iterator.next();

            currentFrame = properties.getAllInternalFrames().get(key);

            if (currentFrame.isSelected()) {
                selectedFrameFound = true;
            }
        }

        if(selectedFrameFound){
            String newTitle = JOptionPane.showInputDialog(this, "New Title:", currentFrame.getTitle());

            if(newTitle != null){
                currentFrame.setTitle(newTitle);
            }
        }
    }//GEN-LAST:event_setTitleJMenuItemActionPerformed

    /**
     * Turns the max and min lines in the combined line plot on or off.
     * 
     * @param evt
     */
    private void showMaxMinJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showMaxMinJMenuItemActionPerformed

        Iterator<Integer> iterator = properties.getAllChartFrames().keySet().iterator();

        properties.setShowMaxMin(!properties.showMaxMin());

        while (iterator.hasNext()) {

            Integer key = iterator.next();

            JFreeChart tempChart = properties.getAllChartFrames().get(key);
            String tempPlotType = properties.getAllInternalFrames().get(key).getInternalFrameType();

            if (tempPlotType.equalsIgnoreCase("FragmentIonProbabilityPlot")) {
                if (((XYPlot) tempChart.getPlot()).getRenderer() != null) {
                    if(((XYPlot) tempChart.getPlot()).getRenderer() instanceof XYErrorRenderer){
                        ((XYErrorRenderer) ((XYPlot) tempChart.getPlot()).getRenderer()).setDrawYError(properties.showMaxMin());
                    }
                }
            }
        }


        if (properties.showMaxMin()) {
            showMaxMinJMenuItem.setText("Hide Max and Min Values");
        } else {
            showMaxMinJMenuItem.setText("Show Max and Min Values");
        }
    }//GEN-LAST:event_showMaxMinJMenuItemActionPerformed

    /**
     * Opens a dialog where its possible to select the top x peptides of a given length.
     * 
     * @param evt
     */
    private void peptideLengthJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideLengthJMenuItemActionPerformed
        new PeptideLength(this, true);
    }//GEN-LAST:event_peptideLengthJMenuItemActionPerformed

    /**
     * Tries to save the plot to an SVG file.
     *
     * @param evt
     */
    private void exportAsSvgJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportAsSvgJMenuItemActionPerformed
        exportPlot(ImageType.SVG);
    }//GEN-LAST:event_exportAsSvgJMenuItemActionPerformed

    /**
     * Tries to save the plot to an PDF file.
     *
     * @param evt
     */
    private void exportAsPdfJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportAsPdfJMenuItemActionPerformed
        exportPlot(ImageType.PDF);
    }//GEN-LAST:event_exportAsPdfJMenuItemActionPerformed

    /**
     * Tries to save the plot to an TIFF file.
     *
     * @param evt
     */
    private void exportAsTiffJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportAsTiffJMenuItemActionPerformed
        exportPlot(ImageType.TIFF);
    }//GEN-LAST:event_exportAsTiffJMenuItemActionPerformed

    /**
     * Tries to save the plot to an JPG file.
     *
     * @param evt
     */
    private void exportAsJpgJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportAsJpgJMenuItemActionPerformed
        exportPlot(ImageType.JPEG);
    }//GEN-LAST:event_exportAsJpgJMenuItemActionPerformed

    /**
     * Tries to save the plot to an PNG file.
     *
     * @param evt
     */
    private void exportAsPngJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportAsPngJMenuItemActionPerformed
        exportPlot(ImageType.PNG);
    }//GEN-LAST:event_exportAsPngJMenuItemActionPerformed

    /**
     * Creates a duplicate of the plot without having to remake the plot.
     *
     * @param evt
     */
    private void duplicatePlotJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_duplicatePlotJMenuItemActionPerformed

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        // first we need to locate the selected internal (plot) frame
        Iterator<Integer> iterator = properties.getAllInternalFrames().keySet().iterator();

        boolean selectedFrameFound = false;

        FragmentationAnalyzerJInternalFrame currentFrame = null;

        // find the currently selected internal frame
        while (iterator.hasNext() && !selectedFrameFound) {
            Integer key = iterator.next();

            currentFrame = properties.getAllInternalFrames().get(key);

            if (currentFrame.isSelected()) {
                selectedFrameFound = true;
            }
        }

        if(selectedFrameFound){

            // ToDo: a lot of the code below could perhaps be simplified...

            try{

                ChartPanel tempChartPanel;

                if (currentFrame.getChartPanel().getChart().getPlot() instanceof CategoryPlot) {
                    DefaultBoxAndWhiskerCategoryDataset tempData = (DefaultBoxAndWhiskerCategoryDataset)
                            ((DefaultBoxAndWhiskerCategoryDataset) ((CategoryPlot)
                                currentFrame.getChartPanel().getChart().getPlot()).getDataset()).clone();

                    CategoryPlot tempPlot = new CategoryPlot(tempData, 
                            (CategoryAxis) ((CategoryPlot) currentFrame.getChartPanel().getChart().getPlot()).getDomainAxis().clone(),
                            (ValueAxis) ((CategoryPlot)currentFrame.getChartPanel().getChart().getPlot()).getRangeAxis().clone(),
                            ((CategoryPlot) currentFrame.getChartPanel().getChart().getPlot()).getRenderer());

                    tempPlot.setOrientation(((CategoryPlot) currentFrame.getChartPanel().getChart().getPlot()).getOrientation());
                    
                    tempChartPanel = new ChartPanel(new JFreeChart(tempPlot));

                    if(currentFrame.getChartPanel().getChart().getLegend() == null){
                        tempChartPanel.getChart().removeLegend();
                    }
                    
                } else {

                    Plot tempPlot = (Plot) currentFrame.getChartPanel().getChart().getPlot().clone();

                    tempChartPanel = new ChartPanel(new JFreeChart(tempPlot));

                    // ToDo: duplicating the markers does not yet work...
//                    if(currentFrame.getChartPanel().getChart().getPlot() instanceof XYPlot){
//                        Collection domainMarkers = ((XYPlot) tempChartPanel.getChart().getPlot()).getDomainMarkers(Layer.BACKGROUND);
//                        Iterator markerIterator = domainMarkers.iterator();
//
//                        // store in a list first to escape a ConcurrentModificationException
//                        ArrayList<IntervalMarker> markers = new ArrayList<IntervalMarker>();
//
//                        while (markerIterator.hasNext()) {
//                            markers.add((IntervalMarker) markerIterator.next());
//                        }
//
//                        for(int i=0; i<markers.size(); i++){
//                            ((XYPlot) tempChartPanel.getChart().getPlot()).addDomainMarker(markers.get(i), Layer.BACKGROUND);
//                        }
//                    }

                    if(currentFrame.getChartPanel().getChart().getLegend() == null){
                        tempChartPanel.getChart().removeLegend();
                    }  
                }
                
                if(currentFrame.getChartPanel().getChart().getLegend() != null){
                    tempChartPanel.getChart().getLegend().setVisible(properties.showLegend());
                    tempChartPanel.getChart().getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));
                }


                FragmentationAnalyzerJInternalFrame internalFrame = new FragmentationAnalyzerJInternalFrame(
                                                currentFrame.getTitle(), true, true, true, tempChartPanel,
                                                currentFrame.getInternalFrameType(), internalFrameUniqueIdCounter);
                internalFrame.add(tempChartPanel);

                insertInternalFrame(internalFrame);
                properties.getAllChartFrames().put(internalFrameUniqueIdCounter, tempChartPanel.getChart());
                internalFrameUniqueIdCounter++;
                
            } catch(CloneNotSupportedException e){
                System.out.println("Duplication error:");
                e.printStackTrace();
            }
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_duplicatePlotJMenuItemActionPerformed

    /**
     * Tries to export the plot to the selected format.
     *
     * @param imageType
     */
    private void exportPlot(ImageType imageType){

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        // first we need to locate the selected internal (plot) frame
        Iterator<Integer> iterator = properties.getAllInternalFrames().keySet().iterator();

        boolean selectedFrameFound = false;

        FragmentationAnalyzerJInternalFrame currentFrame = null;

        // find the currently selected internal frame
        while (iterator.hasNext() && !selectedFrameFound) {
            Integer key = iterator.next();

            currentFrame = properties.getAllInternalFrames().get(key);

            if (currentFrame.isSelected()) {
                selectedFrameFound = true;
            }
        }

        if(selectedFrameFound){

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            currentFrame.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            JFileChooser chooser = new JFileChooser(userProperties.getLastUsedFolder());

            // propse a title equal to the title of the internal frame
            // (note that some characters needs to be replaced)
            String tempTitle = currentFrame.getTitle();
            tempTitle = tempTitle.replaceAll("\\|", ", ");
            tempTitle = tempTitle.replaceAll(":", " ");
            tempTitle = tempTitle.replaceAll("<", "(");
            tempTitle = tempTitle.replaceAll(">", ")");
            chooser.setSelectedFile(new File(tempTitle));

            // set the file filter
            Util.setFileFilter(chooser, imageType);

            int returnVal = chooser.showSaveDialog(this);

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            currentFrame.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

            if (returnVal == JFileChooser.APPROVE_OPTION) {

                String selectedFile = chooser.getSelectedFile().getPath();

                if (!selectedFile.toLowerCase().endsWith(imageType.getExtension())) {
                    selectedFile = selectedFile + imageType.getExtension();
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

                        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                        currentFrame.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

                        if(currentFrame.getChartPanel() != null){
                            PlotUtil.exportChart(currentFrame.getChartPanel().getChart(), currentFrame.getChartPanel().getBounds(),
                                new File(selectedFile), imageType);
                        } else {

                            if(currentFrame.getInternalFrameType().equalsIgnoreCase("HeatMap")){
                                JXTable tempTable = ((HeatMapJPanel) currentFrame.getContentPane().getComponent(0)).getHeatMap();

                                PlotUtil.exportJComponent(tempTable, tempTable.getBounds(),
                                    new File(selectedFile), imageType);

                            } else {
                                JPanel tempPanel = (JPanel) currentFrame.getContentPane().getComponent(0);

                                PlotUtil.exportJComponent(tempPanel, tempPanel.getBounds(),
                                    new File(selectedFile), imageType);
                            }
                        }

                        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                        currentFrame.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                        JOptionPane.showMessageDialog(this, "Plot saved to " + selectedFile,
                                "Plot Saved", JOptionPane.INFORMATION_MESSAGE);

                    } catch(IOException e){
                        JOptionPane.showMessageDialog(null,
                                "An error occured when exporting the plot. " +
                                "See ../Properties/ErrorLog.txt for more details.",
                                "Error Exporting Plot", JOptionPane.ERROR_MESSAGE);
                        Util.writeToErrorLog("Error exporting plot: ");
                        e.printStackTrace();
                    } catch(TranscoderException e){
                        JOptionPane.showMessageDialog(null,
                                "An error occured when exporting the plot. " +
                                "See ../Properties/ErrorLog.txt for more details.",
                                "Error Exporting Plot", JOptionPane.ERROR_MESSAGE);
                        Util.writeToErrorLog("Error exporting plot: ");
                        e.printStackTrace();
                    }
                }
            }
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        currentFrame.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Selects the given number of peptides of the given length.
     *
     * Note that if sorting is turned on for the given column the selection might
     * take some time, especially if the table contains many elements.
     *
     * @param counter
     * @param peptideLength
     */
    public void updateSearchResultsSelection(int counter, int peptideLength){

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        // deselect all rows
        properties.setSelectAllIdentifications(false);
        selectAllIdentificationsJMenuItemActionPerformed(null);

        int numberOfRowsSelected = 0;

        // find all rows with the wanted peptide length and select all occurences up to the wanted number
        for(int i=0; i < searchResultsJXTable.getRowCount() && numberOfRowsSelected < counter; i++){
            if(((Integer) searchResultsJXTable.getValueAt(i, 3)).intValue() == peptideLength){
                searchResultsJXTable.setValueAt(true, i, searchResultsJXTable.getColumnCount() - 1);
                numberOfRowsSelected++;

                // update the selection
                Integer countB = null;

                if (searchResultsJXTable.getColumnCount(false) == 7) {
                    countB = (Integer) searchResultsJXTable.getValueAt(i, 5);
                }

                IdentificationTableRow temp = new IdentificationTableRow(
                        (String) searchResultsJXTable.getValueAt(i, 1),
                        (String) searchResultsJXTable.getValueAt(i, 2),
                        (Integer) searchResultsJXTable.getValueAt(i, 3),
                        (Integer) searchResultsJXTable.getValueAt(i, 4),
                        countB);

                properties.getCurrentlySelectedRowsInSearchTable().add(temp);
            }
        }

        if(numberOfRowsSelected > 0){
            JOptionPane.showMessageDialog(this,
                "Selected " + numberOfRowsSelected + " peptides of length " + peptideLength + ".",
                "Peptides Selected", JOptionPane.INFORMATION_MESSAGE);

            // check if the search results button should be enabled
            searchResultsJComboBoxActionPerformed(null);

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        } else {
            JOptionPane.showMessageDialog(this,
                "No peptides of length " + peptideLength + " were found.",
                "No Peptides Found", JOptionPane.INFORMATION_MESSAGE);

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Makes sure that the combox is always wide enough to 
     * display the longest element.
     */
    private void setVariableComoboBoxPopupMenuWidth(javax.swing.event.PopupMenuEvent evt){
        JComboBox box = (JComboBox) evt.getSource();
        Object comp = box.getUI().getAccessibleChild(box, 0);

        if (!(comp instanceof JPopupMenu)) return;

        JPopupMenu popupMenu = (JPopupMenu) comp;

        JComponent scrollPane = (JComponent) popupMenu.getComponent(0);
        Dimension size = new Dimension();

        if(box.getPreferredSize().width > scrollPane.getPreferredSize().width){
            size.width = box.getPreferredSize().width;
            size.height = scrollPane.getPreferredSize().height;
            scrollPane.setPreferredSize(size);
            scrollPane.setMaximumSize(size);
        }
    }

    /**
     * Makes sure that only the selected data series are visible.
     *
     * @param selected
     * @param seriesIndex
     * @param currentPlotType
     */
    private void setSeriesVisible(boolean selected, int seriesIndex, String currentPlotType) {

        if(boxPlotPanelToolBarJInternalFrame.isVisible()){

            Iterator<Integer> iterator = properties.getAllChartFrames().keySet().iterator();

            while (iterator.hasNext()) {

                Integer key = iterator.next();

                JFreeChart tempChart = properties.getAllChartFrames().get(key);
                String tempPlotType = properties.getAllInternalFrames().get(key).getInternalFrameType();

                if (tempPlotType.equalsIgnoreCase(currentPlotType)) {

                    if (tempPlotType.equalsIgnoreCase("BoxPlot") ||
                            tempPlotType.equalsIgnoreCase("BoxPlot_modification")) {
                        ((CategoryPlot) tempChart.getPlot()).getRenderer(0).setSeriesVisible(seriesIndex, selected);

                        // update the total fragment ion counter in the title bar
                        // note: at the moment the fragment ion count is simply removed
                        String oldTitle = properties.getAllInternalFrames().get(key).getTitle();
                        if(oldTitle.lastIndexOf(",") != -1){
                            String newTitle = oldTitle.substring(0, oldTitle.lastIndexOf(",")) + ")";
                            properties.getAllInternalFrames().get(key).setTitle(newTitle);
                        }


                        // ToDo: find a way to do this
                        // code below does not work
    //                    String newTitle = oldTitle.substring(0, oldTitle.lastIndexOf(",") + 2);
    //
    //                    int oldFragmentIonCountAsString =
    //                            new Integer(oldTitle.substring(
    //                            oldTitle.lastIndexOf(",") + 2, oldTitle.lastIndexOf("fragment ions") - 1)).intValue();
    //
    //                    if(selected){
    //                        oldFragmentIonCountAsString += ((CategoryPlot) tempChart.getPlot()).getDataset().get;
    //                    } else {
    //                        oldFragmentIonCountAsString -= ;
    //                    }
                    }
                }
            }
        }
    }

    /**
     * Order the internal frames to remove any "holes" left by removed frames.
     */
    public void orderInternalFrames() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        if (properties.getAllInternalFrames().size() <= 
                userProperties.getNumberOfPlotsPerRow()*userProperties.getNumberOfPlotsPerColumn()) {
            plotsAndAnalysesJDesktopPane.setPreferredSize(plotsAndAnalysesJScrollPane.getMinimumSize());
            plotPaneCurrentPreferredSize = plotsAndAnalysesJDesktopPane.getPreferredSize();
            plotPaneCurrentScrollValue = 0;
        }
        

        // sort the internal frames in increasing order depending on the unique internal frame index
        Iterator<Integer> iterator = properties.getAllInternalFrames().keySet().iterator();

        ArrayList<Integer> sortedKeys = new ArrayList<Integer>();

        while (iterator.hasNext()) {
            Integer key = iterator.next();
            sortedKeys.add(key);
        }

        java.util.Collections.sort(sortedKeys);


        // update the location of the frames
        for(int i=0; i < sortedKeys.size(); i++) {
            setLocationOfInternalFrame(properties.getAllInternalFrames().get(sortedKeys.get(i)), i);
        }

        setVisible(true);

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Insert an internal frame into the plots and analysis frame.
     *
     * @param internalFrame
     */
    private void insertInternalFrame(FragmentationAnalyzerJInternalFrame internalFrame) {

        // set the internal frames icon
        if (internalFrame.getInternalFrameType().equalsIgnoreCase("SpectrumPanel")) {
            internalFrame.setFrameIcon(new javax.swing.ImageIcon(getClass().getResource(
                    "/no/uib/fragmentation_analyzer/icons/spectrum.GIF")));
        } else if (internalFrame.getInternalFrameType().equalsIgnoreCase("BoxPlot") ||
                internalFrame.getInternalFrameType().equalsIgnoreCase("BoxPlot_modification") ||
                internalFrame.getInternalFrameType().equalsIgnoreCase("MassErrorBoxPlot") ||
                internalFrame.getInternalFrameType().equalsIgnoreCase("FragmentIonProbabilityBoxPlot")) {
            internalFrame.setFrameIcon(new javax.swing.ImageIcon(getClass().getResource(
                    "/no/uib/fragmentation_analyzer/icons/box_plot_small.GIF")));
        } else if (internalFrame.getInternalFrameType().equalsIgnoreCase("MassErrorBubblePlot")) {
            internalFrame.setFrameIcon(new javax.swing.ImageIcon(getClass().getResource(
                    "/no/uib/fragmentation_analyzer/icons/bubble_plot.GIF")));
        } else if (internalFrame.getInternalFrameType().equalsIgnoreCase("MassErrorScatterPlot") ||
                internalFrame.getInternalFrameType().equalsIgnoreCase("ScatterPlot")) {
            internalFrame.setFrameIcon(new javax.swing.ImageIcon(getClass().getResource(
                    "/no/uib/fragmentation_analyzer/icons/scatter_plot.GIF")));
        } else if (internalFrame.getInternalFrameType().equalsIgnoreCase("FragmentIonProbabilityPlot")) {
            internalFrame.setFrameIcon(new javax.swing.ImageIcon(getClass().getResource(
                    "/no/uib/fragmentation_analyzer/icons/line_plot.GIF")));
        } else if (internalFrame.getInternalFrameType().equalsIgnoreCase("BarPlot")) {
            internalFrame.setFrameIcon(new javax.swing.ImageIcon(getClass().getResource(
                    "/no/uib/fragmentation_analyzer/icons/bar_plot.GIF")));
        } else if (internalFrame.getInternalFrameType().equalsIgnoreCase("HeatMap")) {
            internalFrame.setFrameIcon(new javax.swing.ImageIcon(getClass().getResource(
                    "/no/uib/fragmentation_analyzer/icons/heat_map.GIF")));
        } else if (internalFrame.getInternalFrameType().equalsIgnoreCase("Normal")) {
            internalFrame.setFrameIcon(new javax.swing.ImageIcon(getClass().getResource(
                    "/no/uib/fragmentation_analyzer/icons/bar_plot.GIF")));
        } else {
            // use default icon
        }

        // platform dependent??
        ((BasicInternalFrameUI) internalFrame.getUI()).getNorthPane().addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                internalFrameMouseClicked(evt);
            }
        });

        internalFrame.addInternalFrameListener(new InternalFrameAdapter() {

            @Override
            public void internalFrameActivated(InternalFrameEvent e) {

                FragmentationAnalyzerJInternalFrame temp = (FragmentationAnalyzerJInternalFrame) e.getInternalFrame();

                // make sure that only relevant popup menu items are enabled
                if (temp.getInternalFrameType().equalsIgnoreCase("SpectrumPanel")) {
                    showSpectrumToolBarJMenuItem.setEnabled(true);
                    showSpectrumToolBarJMenuItem.setVisible(true);
                    showBoxPlotToolBarJMenuItem.setEnabled(false);
                    showBoxPlotToolBarJMenuItem.setVisible(false);
                    showDataSeriesSelectionJMenuItem.setEnabled(false);
                    showDataSeriesSelectionJMenuItem.setVisible(false);
                    showMaxMinJMenuItem.setVisible(false);
                    showMaxMinJMenuItem.setEnabled(false);
                    showAverageJMenuItem.setVisible(false);
                    showAverageJMenuItem.setEnabled(false);
                    duplicatePlotJMenuItem.setVisible(false);
                    duplicatePlotJMenuItem.setEnabled(false);
                } else if (temp.getInternalFrameType().equalsIgnoreCase("BoxPlot") ||
                        temp.getInternalFrameType().equalsIgnoreCase("BoxPlot_modification")) {
                    showSpectrumToolBarJMenuItem.setEnabled(false);
                    showSpectrumToolBarJMenuItem.setVisible(false);
                    showBoxPlotToolBarJMenuItem.setEnabled(true);
                    showBoxPlotToolBarJMenuItem.setVisible(true);
                    showDataSeriesSelectionJMenuItem.setEnabled(false);
                    showDataSeriesSelectionJMenuItem.setVisible(false);
                    showMaxMinJMenuItem.setVisible(false);
                    showMaxMinJMenuItem.setEnabled(false);
                    showAverageJMenuItem.setVisible(false);
                    showAverageJMenuItem.setEnabled(false);
                    duplicatePlotJMenuItem.setVisible(false);
                    duplicatePlotJMenuItem.setEnabled(false);
                } else if (temp.getInternalFrameType().equalsIgnoreCase("MassErrorScatterPlot") ||
                        temp.getInternalFrameType().equalsIgnoreCase("MassErrorBubblePlot") ||
                        temp.getInternalFrameType().equalsIgnoreCase("MassErrorBoxPlot") ||
                        temp.getInternalFrameType().equalsIgnoreCase("FragmentIonProbabilityPlot") ||
                        temp.getInternalFrameType().equalsIgnoreCase("BarPlot") ||
                        temp.getInternalFrameType().equalsIgnoreCase("FragmentIonProbabilityBoxPlot")) {
                    showSpectrumToolBarJMenuItem.setEnabled(false);
                    showSpectrumToolBarJMenuItem.setVisible(false);
                    showBoxPlotToolBarJMenuItem.setEnabled(false);
                    showBoxPlotToolBarJMenuItem.setVisible(false);
                    showDataSeriesSelectionJMenuItem.setEnabled(true);
                    showDataSeriesSelectionJMenuItem.setVisible(true);

                    if(temp.getInternalFrameType().equalsIgnoreCase("BarPlot") ||
                            temp.getInternalFrameType().equalsIgnoreCase("MassErrorScatterPlot") ||
                        temp.getInternalFrameType().equalsIgnoreCase("MassErrorBubblePlot")){
                        duplicatePlotJMenuItem.setVisible(false);
                        duplicatePlotJMenuItem.setEnabled(false);
                    } else {
                        duplicatePlotJMenuItem.setVisible(true);
                        duplicatePlotJMenuItem.setEnabled(true);
                    }

                    if(temp.getInternalFrameType().equalsIgnoreCase("FragmentIonProbabilityPlot")){
                        showMaxMinJMenuItem.setVisible(true);
                        showMaxMinJMenuItem.setEnabled(true);
                    } else {
                        showMaxMinJMenuItem.setVisible(false);
                        showMaxMinJMenuItem.setEnabled(false);
                    }

                    if(temp.getInternalFrameType().equalsIgnoreCase("MassErrorScatterPlot") ||
                        temp.getInternalFrameType().equalsIgnoreCase("MassErrorBubblePlot")){
                        showAverageJMenuItem.setVisible(true);
                        showAverageJMenuItem.setEnabled(true);
                    } else {
                        showAverageJMenuItem.setVisible(false);
                        showAverageJMenuItem.setEnabled(false);
                    }

                } else {
                    showSpectrumToolBarJMenuItem.setEnabled(false);
                    showSpectrumToolBarJMenuItem.setVisible(false);
                    showBoxPlotToolBarJMenuItem.setEnabled(false);
                    showBoxPlotToolBarJMenuItem.setVisible(false);
                    showDataSeriesSelectionJMenuItem.setEnabled(false);
                    showDataSeriesSelectionJMenuItem.setVisible(false);
                    showMaxMinJMenuItem.setVisible(false);
                    showMaxMinJMenuItem.setEnabled(false);
                    duplicatePlotJMenuItem.setVisible(false);
                    duplicatePlotJMenuItem.setEnabled(false);
                    showAverageJMenuItem.setVisible(false);
                    showAverageJMenuItem.setEnabled(false);
                }
            }

            @Override
            public void internalFrameClosed(InternalFrameEvent e) {

                FragmentationAnalyzerJInternalFrame temp = (FragmentationAnalyzerJInternalFrame) e.getInternalFrame();

                if (temp.getInternalFrameType().equalsIgnoreCase("SpectrumPanel")) {
                    properties.getLinkedSpectrumPanels().remove(temp.getUniqueId());
                    properties.getAllAnnotations().remove(temp.getUniqueId());
                }

                internalFrameIsMaximized = false;

                properties.getAllChartFrames().remove(temp.getUniqueId());
                properties.getAllInternalFrames().remove(temp.getUniqueId());
                orderInternalFrames();
            }
        });

        internalFrame.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {

                if (!internalFrameBeingResized) {

                    FragmentationAnalyzerJInternalFrame tempInternalFrame = (FragmentationAnalyzerJInternalFrame) e.getComponent();

                    if (tempInternalFrame.isMaximum()) {

                        internalFrameIsMaximized = true;
                        internalFrameBeingResized = true;
                        tempInternalFrame.setResizable(false);

                        if (updateScrollValue) {
                            plotPaneCurrentScrollValue = plotsAndAnalysesJScrollPane.getVerticalScrollBar().getValue();
                        }

                        updateScrollValue = false;

                        plotsAndAnalysesJDesktopPane.setPreferredSize(plotsAndAnalysesJScrollPane.getMinimumSize());
                        plotsAndAnalysesJDesktopPane.setSize(plotsAndAnalysesJScrollPane.getMinimumSize());
                        plotsAndAnalysesJDesktopPane.setMaximumSize(plotsAndAnalysesJScrollPane.getMinimumSize());
                        plotsAndAnalysesJDesktopPane.setMinimumSize(plotsAndAnalysesJScrollPane.getMinimumSize());
                        plotsAndAnalysesJDesktopPane.repaint();
                        plotsAndAnalysesJScrollPane.repaint();

                        tempInternalFrame.setResizable(true);
                        internalFrameBeingResized = false;

                    } else {

                        updateScrollValue = true;

                        if (!internalFrameIsMaximized) {
                            plotPaneCurrentScrollValue = plotsAndAnalysesJScrollPane.getVerticalScrollBar().getValue();
                        }

                        int temp = plotPaneCurrentScrollValue;

                        internalFrameBeingResized = true;

                        plotsAndAnalysesJDesktopPane.setPreferredSize(plotPaneCurrentPreferredSize);
                        plotsAndAnalysesJDesktopPane.setPreferredSize(plotPaneCurrentPreferredSize);
                        plotsAndAnalysesJDesktopPane.setSize(plotPaneCurrentPreferredSize);
                        plotsAndAnalysesJDesktopPane.setMaximumSize(plotPaneCurrentPreferredSize);
                        plotsAndAnalysesJDesktopPane.setMinimumSize(plotPaneCurrentPreferredSize);
                        plotsAndAnalysesJDesktopPane.repaint();
                        plotsAndAnalysesJScrollPane.repaint();
                        plotsAndAnalysesJScrollPane.getVerticalScrollBar().setValue(temp);

                        // invoke later to give time for plots to render.
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                plotsAndAnalysesJScrollPane.getVerticalScrollBar().setValue(plotPaneCurrentScrollValue);
                            }
                        });

                        internalFrameBeingResized = false;
                        internalFrameIsMaximized = false;
                    }
                }
            }
        });

        setLocationOfInternalFrame(internalFrame, properties.getAllInternalFrames().size());
        properties.getAllInternalFrames().put(internalFrameUniqueIdCounter, internalFrame);

        internalFrame.setVisible(true);
        internalFrame.validate();
        internalFrame.repaint();

        plotsAndAnalysesJDesktopPane.add(internalFrame, javax.swing.JLayeredPane.DEFAULT_LAYER);
        plotsAndAnalysesJScrollPane.repaint();

        // make sure that the internal frame tip is always in the back
        plotsAndAnalysesJDesktopPane.moveToBack(internalFrameTipJLabel);

        // invoke later to give time for frame to be updated
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                plotsAndAnalysesJScrollPane.getVerticalScrollBar().setValue(
                        plotsAndAnalysesJScrollPane.getVerticalScrollBar().getMaximum());
                plotPaneCurrentScrollValue = plotsAndAnalysesJScrollPane.getVerticalScrollBar().getValue();
            }
        });
    }

    /**
     * Opens the internal frame popup menu if the user right clicks on the frame title bar.
     *
     * @param evt
     */
    private void internalFrameMouseClicked(java.awt.event.MouseEvent evt) {
        if (evt.getButton() == java.awt.event.MouseEvent.BUTTON3) {
            internalFramesJPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }

    /**
     * Sets the location of an internal frame based on the index of the frame.
     *
     * @param internalFrame
     * @param index
     */
    private void setLocationOfInternalFrame(FragmentationAnalyzerJInternalFrame internalFrame, int index) {

        int numberPerRow = userProperties.getNumberOfPlotsPerRow();
        int numberPerColumn = userProperties.getNumberOfPlotsPerColumn();
        int spaceBetween = 10;

        int temp = index / numberPerRow;

        int width = (plotsAndAnalysesJScrollPane.getWidth() - spaceBetween * (numberPerRow + 4)) / numberPerRow;
        int height = (plotsAndAnalysesJScrollPane.getHeight() - spaceBetween * (numberPerColumn + 3)) / numberPerColumn;

        int xCorr = spaceBetween + (spaceBetween * (index % numberPerRow + 1)) + (index % numberPerRow * width);
        int yCorr = spaceBetween + (height * temp) + (spaceBetween * (temp + 1));

        internalFrame.setBounds(xCorr, yCorr, width, height);

        while ((yCorr + height + spaceBetween) > plotsAndAnalysesJDesktopPane.getPreferredSize().getHeight()) {
            plotsAndAnalysesJDesktopPane.setPreferredSize(new Dimension(
                    (int) plotsAndAnalysesJDesktopPane.getPreferredSize().getWidth(),
                    (int) plotsAndAnalysesJDesktopPane.getPreferredSize().getHeight() + 1));
        }

        plotPaneCurrentPreferredSize = plotsAndAnalysesJDesktopPane.getPreferredSize();
    }

    /**
     * Returns a spectrum panel containing the provided data.
     *
     * @param spectrumFile the spectrum to display
     * @param fragmentIons the fragment ions to annotate
     * @return the created spectrum panel
     * @throws IOException
     */
    private SpectrumPanel getSpectrumPanel(Spectrum_file spectrumFile, Vector<Fragmention> fragmentIons) 
            throws IOException, SQLException {

        // ToDo: This method ought to be moved into the PlotUtil class

        String filename = Spectrum.findFromID(spectrumFile.getL_spectrumid(), getConnection()).getFilename();
        String file = new String(spectrumFile.getUnzippedFile());
        MascotGenericFile lSpectrumFile = new MascotGenericFile(filename, file);

        TreeSet treeSet = new TreeSet();
        HashMap aPeakList = lSpectrumFile.getPeaks();
        treeSet.clear();
        treeSet.addAll(aPeakList.keySet());

        Iterator treeSetIterator = treeSet.iterator();

        Double tempMz;
        double[] mzValues = new double[aPeakList.size()];
        double[] intValues = new double[aPeakList.size()];

        int peakCounter = 0;

        while (treeSetIterator.hasNext()) {
            tempMz = (Double) treeSetIterator.next();
            mzValues[peakCounter] = tempMz;
            intValues[peakCounter++] = (Double) aPeakList.get(tempMz);
        }

        SpectrumPanel spectrumPanel = new SpectrumPanel(
                mzValues, intValues,
                lSpectrumFile.getPrecursorMZ(), "" + lSpectrumFile.getCharge(),
                "" + spectrumFile.getL_spectrumid(),
                60, false);

        spectrumPanel.addSpectrumPanelListener(new SpectrumPanelListener() {

            public void rescaled(RescalingEvent rescalingEvent) {
                SpectrumPanel source = (SpectrumPanel) rescalingEvent.getSource();
                double minMass = rescalingEvent.getMinMass();
                double maxMass = rescalingEvent.getMaxMass();

                Iterator<Integer> iterator = properties.getLinkedSpectrumPanels().keySet().iterator();

                while (iterator.hasNext()) {
                    SpectrumPanel currentSpectrumPanel = properties.getLinkedSpectrumPanels().get(iterator.next());
                    if (currentSpectrumPanel != source) {
                        currentSpectrumPanel.rescale(minMass, maxMass, false);
                        currentSpectrumPanel.repaint();
                    }
                }
            }
        });

        // add the fragment ion annotations
        Vector<DefaultSpectrumAnnotation> currentAnnotations = new Vector();

        for (int i = 0; i < fragmentIons.size(); i++) {

            Fragmention currentFragmentIon = fragmentIons.get(i);

            if (userProperties.isScoringTypeSelected(currentFragmentIon.getL_ionscoringid())) {

                int fragmentIonNumber = (int) currentFragmentIon.getFragmentionnumber();
                String ionName = currentFragmentIon.getIonname();

                Color currentColor = Util.determineColorOfPeak(ionName);

                if (ionName.startsWith("a") || ionName.startsWith("b") || ionName.startsWith("c") ||
                        ionName.startsWith("x") || ionName.startsWith("y") || ionName.startsWith("z")) {
                    if (ionName.length() > 1) {
                        ionName = ionName.substring(0, 1) + "[" + fragmentIonNumber + "]" + ionName.substring(1);
                    } else {
                        ionName = ionName.substring(0, 1) + fragmentIonNumber;
                    }
                }

                currentAnnotations.add(new DefaultSpectrumAnnotation(
                        currentFragmentIon.getMz().doubleValue(),
                        currentFragmentIon.getMasserrormargin().doubleValue(),
                        currentColor,
                        ionName));
            }
        }

        properties.getAllAnnotations().put(internalFrameUniqueIdCounter, currentAnnotations);
        spectrumPanel.setAnnotations(currentAnnotations);
        properties.getLinkedSpectrumPanels().put(new Integer(internalFrameUniqueIdCounter), spectrumPanel);

        return spectrumPanel;
    }

    /**
     * Returns a spectrum panel containing the provided data.
     *
     * @param pklFile the pkl file containing the spectrum
     * @param fragmentIons the fragment ions to annotate
     * @return the created spectrum panel
     * @throws IOException
     */
    private SpectrumPanel getSpectrumPanel(PKLFile pklFile, ArrayList<FragmentIon> fragmentIons) throws IOException {

        // ToDo: This method ought to be moved into the PlotUtil class

        SpectrumPanel spectrumPanel = new SpectrumPanel(
                pklFile.getMzValues(), pklFile.getIntensityValues(),
                pklFile.getPrecursorMz(), "" + pklFile.getPrecurorCharge(),
                "" + pklFile.getFileName(),
                60, false);

        spectrumPanel.addSpectrumPanelListener(new SpectrumPanelListener() {

            public void rescaled(RescalingEvent rescalingEvent) {
                SpectrumPanel source = (SpectrumPanel) rescalingEvent.getSource();
                double minMass = rescalingEvent.getMinMass();
                double maxMass = rescalingEvent.getMaxMass();

                Iterator<Integer> iterator = properties.getLinkedSpectrumPanels().keySet().iterator();

                while (iterator.hasNext()) {
                    SpectrumPanel currentSpectrumPanel = properties.getLinkedSpectrumPanels().get(iterator.next());
                    if (currentSpectrumPanel != source) {
                        currentSpectrumPanel.rescale(minMass, maxMass, false);
                        currentSpectrumPanel.repaint();
                    }
                }
            }
        });

        // add the fragment ion annotations
        Vector<DefaultSpectrumAnnotation> currentAnnotations = new Vector();

        for (int i = 0; i < fragmentIons.size(); i++) {

            FragmentIon currentFragmentIon = fragmentIons.get(i);

            String ionName = currentFragmentIon.getFragmentIonType();

            Color currentColor = Util.determineColorOfPeak(ionName);

            currentAnnotations.add(new DefaultSpectrumAnnotation(
                    currentFragmentIon.getFragmenIonMz().doubleValue(),
                    currentFragmentIon.getFragmentIonMassError().doubleValue(),
                    currentColor,
                    ionName));
        }

        properties.getAllAnnotations().put(internalFrameUniqueIdCounter, currentAnnotations);
        spectrumPanel.setAnnotations(currentAnnotations);
        properties.getLinkedSpectrumPanels().put(new Integer(internalFrameUniqueIdCounter), spectrumPanel);

        return spectrumPanel;
    }

    /**
     * Starts the tool by first setting the look and feel and creating the error log if
     * not already created. The opens the main frame.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // create the properties object
        properties = new Properties();

        // makes sure that '.' is used as the decimal sign
        Locale.setDefault(Locale.US);

        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {

                try {
                    PlasticLookAndFeel.setPlasticTheme(new SkyKrupp());
                    UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
                    //UIManager.setLookAndFeel(new WindowsLookAndFeel());
                } catch (UnsupportedLookAndFeelException e) {
                    //e.printStackTrace();
                    // ignore error
                }

                // check if a newer version of FragmentationAnalyzer is available
                Util.checkForNewVersion(properties, debug);

                if (useErrorLog) {
                    try {
                        String path = "" + this.getClass().getProtectionDomain().getCodeSource().getLocation();
                        path = path.substring(5, path.lastIndexOf("/"));
                        path = path + "/Properties/ErrorLog.txt";
                        path = path.replace("%20", " ");

                        File file = new File(path);
                        System.setOut(new java.io.PrintStream(new FileOutputStream(file, true)));
                        System.setErr(new java.io.PrintStream(new FileOutputStream(file, true)));

                        // creates a new error log file if it does not exist
                        if (!file.exists()) {
                            file.createNewFile();

                            FileWriter w = new FileWriter(file);
                            BufferedWriter bw = new BufferedWriter(w);

                            bw.close();
                            w.close();
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                                null, "An error occured when trying to create the ErrorLog." +
                                "See ../Properties/ErrorLog.txt for more details.",
                                "Error Creating ErrorLog", JOptionPane.ERROR_MESSAGE);
                        Util.writeToErrorLog("Error when creating ErrorLog: ");
                        e.printStackTrace();
                    }
                }

                new FragmentationAnalyzer();
            }
        });
    }

    /**
     * Tries to load the currently selected data set.
     *
     * @param ms_lims_dataSet set to true of the data set to import is from ms_lims, false otherwise
     */
    public void loadDataSet(boolean ms_lims_dataSet) {

        cancelProgress = false;

        this.setTitle(analyzerName + " " + properties.getVersion());

        // close the two panes with now old data
        searchResultsJXTaskPane.setExpanded(false);
        spectraJXTaskPane.setExpanded(false);
        plotsAnalysesJXTaskPane.setExpanded(false);
        spectraJButton.setEnabled(false);
        spectraJButton.setToolTipText(spectraAnalysisButtonDisabledToolTip);
        searchResultsJButton.setEnabled(false);
        searchResultsJButton.setToolTipText(searchResultAnalysisButtonDisabledToolTip);
        searchEnabled = false;

        // empty the tables
        ((DefaultTableModel) searchResultsJXTable.getModel()).setRowCount(0);
        ((DefaultTableModel) spectraJXTable.getModel()).setRowCount(0);

        properties.setCurrentlySelectedRowsInSearchTable(new ArrayList<IdentificationTableRow>());

        currentDataSetIsFromMsLims = ms_lims_dataSet;

        progressDialog = new ProgressDialog(this, this, true);

        new Thread(new Runnable() {

            public void run() {
                progressDialog.setIntermidiate(false);
                progressDialog.setTitle("Loading Identifications. Please Wait...");
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        new Thread("LoadThread") {

            @Override
            public void run() {

                dataLoaded = false;

                File identificationsFile = new File(properties.getCurrentDataSetFolder() + "/identifications.txt");

                if (identificationsFile.exists()) {

                    try {
                        BufferedReader b = new BufferedReader(new FileReader(identificationsFile));

                        int identificationCount = new Integer(b.readLine());

                        int progressCounter = 0;
                        progressDialog.setValue(0);
                        progressDialog.setMax(identificationCount);

                        properties.setExtractedInternalModifications(new HashMap<String, Integer>());
                        properties.setExtractedNTermModifications(new HashMap<String, Integer>());
                        properties.setExtractedCTermModifications(new HashMap<String, Integer>());
                        properties.setExtractedCharges(new HashMap<String, Integer>());
                        properties.setExtractedInstruments(new HashMap<String, Integer>());

                        instrument2JComboBox.setEnabled(false);
                        instrument3JComboBox.setEnabled(false);
                        modification2JComboBox.setEnabled(false);
                        modification3JComboBox.setEnabled(false);

                        String currentLine = b.readLine();

                        while (currentLine != null && !cancelProgress) {
                            ReducedIdentification currentIdentification = new ReducedIdentification(currentLine, true);

                            // store a list of all found charges, instruments, terminals and modifications
                            Util.storeCharge(currentIdentification.getCharge().toString(), properties);
                            Util.storeInstrument(currentIdentification.getInstrumentName(), properties);
                            Util.extractUnmodifiedSequenceAndModifications(
                                    currentIdentification.getModifiedSequence(), true, false, properties);

                            //identifications.put(currentIdentification.getIdentificationId(), currentIdentification);
                            progressDialog.setValue(progressCounter++);
                            currentLine = b.readLine();
                        }

                        b.close();

                        if (!cancelProgress) {

                            // update the comboboxes
                            updateComboBox(instrument1JComboBox, properties.getExtractedInstruments(), true);
                            updateComboBox(instrument2JComboBox, properties.getExtractedInstruments(), false);
                            updateComboBox(instrument3JComboBox, properties.getExtractedInstruments(), false);

                            updateComboBox(nTermJComboBox, properties.getExtractedNTermModifications(), true);
                            updateComboBox(cTermJComboBox, properties.getExtractedCTermModifications(), true);

                            updateComboBox(chargeJComboBox, properties.getExtractedCharges(), false);

                            updateComboBox(modification1JComboBox, properties.getExtractedInternalModifications(), false);
                            updateComboBox(modification2JComboBox, properties.getExtractedInternalModifications(), false);
                            updateComboBox(modification3JComboBox, properties.getExtractedInternalModifications(), false);

                            dataLoaded = true;

                            progressDialog.setVisible(false);
                            progressDialog.dispose();

                            // make sure the left panel is showing
                            mainJSplitPane.setDividerLocation(Properties.DEFAULT_MAIN_SPLITTER_LOCATION);
                            showLeftPanelJMenuItem.setText("Hide Left Panel");

                            JOptionPane.showMessageDialog(null,
                                    "Select the search parameters to the left and\n" +
                                    "click on Search to start analyzing the data.\n",
                                    "Data Set Loaded", JOptionPane.INFORMATION_MESSAGE);

                            repaint();

                            // invoke later to give time for components to update
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    orderInternalFrames();
                                }
                            });

                            setTitle(analyzerName + " " + properties.getVersion() + " - " + new File(properties.getCurrentDataSetFolder()).getName());
                        } else {
                            // cancel loading of the data set
                            closeDatabaseConnection();
                        }
                    } catch (FileNotFoundException e) {
                        JOptionPane.showMessageDialog(null,
                                "An error occured when trying to open the identifications.txt file.\n" +
                                "See /Properties/ErrorLog.txt for more details.",
                                "Error Opening Identifications File", JOptionPane.ERROR_MESSAGE);
                        Util.writeToErrorLog("Error opening identifications file:");
                        e.printStackTrace();
                        closeDatabaseConnection();
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(null,
                                "An error occured when trying to open the identifications.txt file.\n" +
                                "See /Properties/ErrorLog.txt for more details.",
                                "Error Opening Identifications File", JOptionPane.ERROR_MESSAGE);
                        Util.writeToErrorLog("Error opening identifications file:");
                        e.printStackTrace();
                        closeDatabaseConnection();
                    }
                } else {
                    JOptionPane.showMessageDialog(null,
                            "An error occured when trying to open the data set.\n" +
                            "See /Properties/ErrorLog.txt for more details.",
                            "Error Opening Data Set", JOptionPane.ERROR_MESSAGE);
                    Util.writeToErrorLog("Error Opening Data Set: the identifications file "
                            + properties.getCurrentDataSetFolder() + "/identifications.txt" + " does not exist!");
                    closeDatabaseConnection();
                }

                progressDialog.setVisible(false);
                progressDialog.dispose();
            }
        }.start();
    }

    /**
     * Update the values in the combobox. Note that the values are sorted alphabetically.
     *
     * @param comboBox the combobox to update
     * @param values the new values
     * @param addAllOption
     */
    private void updateComboBox(JComboBox comboBox, HashMap<String, Integer> values, boolean addAllOption) {

        Vector<String> tempInstruments = new Vector<String>();
        tempInstruments.add(" - Select - ");

        Iterator<String> iteratorSequence = values.keySet().iterator();

        NumberFormat formatter =  new DecimalFormat("##,###,###");

        while (iteratorSequence.hasNext()) {
            String key = iteratorSequence.next();

            // format the occurence value
            String occurenceAsString = formatter.format(values.get(key));

            tempInstruments.add(key + " (" + occurenceAsString + ")");
        }

        java.util.Collections.sort(tempInstruments);

        if (addAllOption) {
            tempInstruments.add("Select All");
        }

        comboBox.setModel(new DefaultComboBoxModel(tempInstruments));
    }

    /**
     * Tries to connect to the ms_lims database. Returns true if connection was successfull.
     * Note: only works for ms_lims 7 and newer.
     *
     * @return true if connection was successfull
     */
    public static boolean connectToDatabase() {

        // ToDo: This method could be moved into the Util class?
        
        boolean connectionSuccessfull = false;

        try {
            // DB driver loading.
            Driver driver = (Driver) Class.forName("com.mysql.jdbc.Driver").newInstance();

            // DB user and password setting.
            java.util.Properties props = new java.util.Properties();
            props.put("user", userProperties.getUserName());
            props.put("password", properties.getPassWord());
            conn = driver.connect("jdbc:mysql://" +
                    userProperties.getServerHost() + "/" +
                    userProperties.getSchema(), props);
            connectionSuccessfull = true;

            //test to check if the supported version of ms_lims is used
            try {
                Spectrum_file.findFromID(0, conn); // test for ms_lims 7.3
                //Protocol.getAllProtocols(conn); // test for ms_lims 7
                //Identification.getIdentification(conn, ""); // test for ms_lims 6
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null,
                        "Database connection not established:\n" +
                        "Verify that you are using a supported version of ms_lims,\n" +
                        "and upgrade if necessary: http://genesis.ugent.be/ms_lims.",
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                connectionSuccessfull = false;
                closeDatabaseConnection();
            }
        } catch (Exception e) {
            if (e.getMessage().lastIndexOf("Communications link failure") != -1) {
                // this is the most likely option as far as I can see
                JOptionPane.showMessageDialog(null, "Database connection not established:" +
                        "\n" + "Verify server host.", "Database Error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Database connection not established:" +
                        "\n" + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        return connectionSuccessfull;
    }

    /**
     * Closes the database connection.
     */
    public static void closeDatabaseConnection() {

        // ToDo: This method could be moved into the Util class?

        // Close DB connection.
        if (conn != null) {
            try {
                conn.close();
                conn = null;
            } catch (SQLException sqle) {
                // Nothing to be done.
                JOptionPane.showMessageDialog(
                        null, "An error occured when attempting to close the DB connection." +
                        "See ../Properties/ErrorLog.txt for more details.",
                        "DB Connection Error", JOptionPane.ERROR_MESSAGE);
                sqle.printStackTrace();
            }
        }
    }

    /**
     * Returns a UsertProperties object with all the user settings.
     *
     * @return the user settings
     */
    public static UserProperties getUserProperties() {
        return userProperties;
    }

    /**
     * Returns a Properties object with all the settings.
     *
     * @return the user settings
     */
    public static Properties getProperties() {
        return properties;
    }

    /**
     * Returns the ms_lims database connection or null if no connection.
     *
     * @return the ms_lims database connection
     */
    public Connection getConnection() {

        if (conn != null) {

            int value = JOptionPane.OK_OPTION;

            while (value != JOptionPane.CANCEL_OPTION) {

                // verify that the connection is still alive. if not, open a new connection
                try {
                    Statement s = conn.createStatement();
                    s.execute("show columns in spectrum_file");

                    // get out of the while loop
                    value = JOptionPane.CANCEL_OPTION;
                } catch (SQLException e) {

                    conn = null;

                    value = JOptionPane.showConfirmDialog(this, "Database connection is no longer available. Please reconnect.",
                            "Database Connection Failure", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);

                    if (value == JOptionPane.OK_OPTION) {
                        new DatabaseDialog(this, true);
                    }
                }
            }
        }

        return conn;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox H2OIonsJCheckBox;
    private javax.swing.JCheckBox NH3IonsJCheckBox;
    private javax.swing.JCheckBox aIonsJCheckBox;
    private javax.swing.JMenuItem aboutJMenuItem;
    private javax.swing.JCheckBox bIonsBoxPlotJCheckBox;
    private javax.swing.JCheckBox bIonsJCheckBox;
    private javax.swing.JCheckBox bIonsModifiedJCheckBox;
    private javax.swing.JCheckBox bIonsUnmodifiedJCheckBox;
    private javax.swing.JInternalFrame boxPlotPanelToolBarJInternalFrame;
    private javax.swing.JCheckBox cIonsJCheckBox;
    private javax.swing.JComboBox cTermJComboBox;
    private javax.swing.JComboBox chargeJComboBox;
    private javax.swing.JCheckBox chargeOneJCheckBox;
    private javax.swing.JCheckBox chargeOverTwoJCheckBox;
    private javax.swing.JCheckBox chargeTwoJCheckBox;
    private javax.swing.JMenuItem closeAllInternalFramesJMenuItem;
    private javax.swing.JComboBox combineSearchResultsJComboBox;
    private javax.swing.JComboBox combineSpectraJComboBox;
    private javax.swing.JComboBox daOrPpmSearchResultsJComboBox;
    private javax.swing.JComboBox daOrPpmSpectraJComboBox;
    private javax.swing.JMenuItem deselectHighlightedIdentificationsJMenuItem;
    private javax.swing.JMenuItem deselectHighlightedSpectraJMenuItem;
    private javax.swing.JMenuItem duplicatePlotJMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitJMenuItem;
    private javax.swing.JMenuItem exportAsJpgJMenuItem;
    private javax.swing.JMenuItem exportAsPdfJMenuItem;
    private javax.swing.JMenuItem exportAsPngJMenuItem;
    private javax.swing.JMenuItem exportAsSvgJMenuItem;
    private javax.swing.JMenuItem exportAsTiffJMenuItem;
    private javax.swing.JMenu exportJMenu;
    private javax.swing.JMenu fileJMenu;
    private javax.swing.JRadioButton generalSearchJRadioButton;
    private javax.swing.JMenu helpJMenu;
    private javax.swing.JMenuItem helpJMenuItem;
    private javax.swing.JMenu highlightIdentificationsJMenu;
    private javax.swing.JMenu highlightSelectionSpectraJMenu;
    private javax.swing.JComboBox instrument1JComboBox;
    private javax.swing.JComboBox instrument2JComboBox;
    private javax.swing.JComboBox instrument3JComboBox;
    private org.jdesktop.swingx.JXTaskPane instrumentJXTaskPane;
    private javax.swing.JLabel internalFrameTipJLabel;
    private javax.swing.JPopupMenu internalFramesJPopupMenu;
    private javax.swing.JMenuItem invertSelectionIdentificationsJMenuItem;
    private javax.swing.JMenuItem invertSelectionSpectraJMenuItem;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JMenuBar jMenuBar;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private org.jdesktop.swingx.JXPanel jXPanel2;
    private org.jdesktop.swingx.JXPanel jXPanel3;
    private org.jdesktop.swingx.JXPanel jXPanel7;
    private javax.swing.JSplitPane mainJSplitPane;
    private javax.swing.JComboBox modification1JComboBox;
    private javax.swing.JComboBox modification2JComboBox;
    private javax.swing.JComboBox modification3JComboBox;
    private javax.swing.JRadioButton modificationSearchJRadioButton;
    private org.jdesktop.swingx.JXTaskPane modificationsJXTaskPane;
    private javax.swing.JComboBox nTermJComboBox;
    private javax.swing.JMenuItem opemJMenuItem;
    private javax.swing.JCheckBox otherIonsJCheckBox;
    private javax.swing.JMenuItem peptideLengthJMenuItem;
    private org.jdesktop.swingx.JXTaskPane plotsAnalysesJXTaskPane;
    private javax.swing.JDesktopPane plotsAndAnalysesJDesktopPane;
    private javax.swing.JScrollPane plotsAndAnalysesJScrollPane;
    private javax.swing.JMenuItem preferencesJMenuItem;
    private javax.swing.JScrollPane resultsJScrollPane;
    private org.jdesktop.swingx.JXTaskPaneContainer resultsJXTaskPaneContainer;
    private javax.swing.ButtonGroup searchButtonGroup;
    private javax.swing.JButton searchJButton;
    private org.jdesktop.swingx.JXPanel searchResultJXPanel;
    private javax.swing.JButton searchResultsJButton;
    private javax.swing.JComboBox searchResultsJComboBox;
    private javax.swing.JScrollPane searchResultsJScrollPane;
    private org.jdesktop.swingx.JXTable searchResultsJXTable;
    private org.jdesktop.swingx.JXTaskPane searchResultsJXTaskPane;
    private javax.swing.JScrollPane searchSettingsJScrollPane;
    private org.jdesktop.swingx.JXTaskPaneContainer searchSettingsJXTaskPaneContainer;
    private org.jdesktop.swingx.JXPanel searchTypeJXPanel;
    private javax.swing.JMenuItem selectAllIdentificationsJMenuItem;
    private javax.swing.JMenuItem selectAllSpectrtaJMenuItem;
    private javax.swing.JMenuItem selectHighlightedIdentificationsJMenuItem;
    private javax.swing.JMenuItem selectHighlightedSpectraJMenuItem;
    private javax.swing.JPopupMenu selectIdentificationsJPopupMenu;
    private javax.swing.JPopupMenu selectSpectraJPopupMenu;
    private javax.swing.JMenuItem setTitleJMenuItem;
    private javax.swing.JMenuItem showAverageJMenuItem;
    private javax.swing.JMenuItem showBoxPlotToolBarJMenuItem;
    private javax.swing.JMenuItem showDataSeriesSelectionJMenuItem;
    private javax.swing.JMenuItem showLeftPanelJMenuItem;
    private javax.swing.JMenuItem showLegendsJMenuItem;
    private javax.swing.JMenuItem showMarkersJMenuItem;
    private javax.swing.JMenuItem showMaxMinJMenuItem;
    private javax.swing.JMenuItem showSpectrumToolBarJMenuItem;
    private javax.swing.JButton spectraJButton;
    private javax.swing.JComboBox spectraJComboBox;
    private javax.swing.JScrollPane spectraJScrollPane;
    private org.jdesktop.swingx.JXPanel spectraJXPanel;
    private org.jdesktop.swingx.JXTable spectraJXTable;
    private org.jdesktop.swingx.JXTaskPane spectraJXTaskPane;
    private javax.swing.JInternalFrame spectrumPanelToolBarJInternalFrame;
    private org.jdesktop.swingx.JXTaskPane terminalsAndChargeJXTaskPane;
    private javax.swing.JMenu windowJMenu;
    private javax.swing.JCheckBox xIonsJCheckBox;
    private javax.swing.JCheckBox yIonsBoxPlotJCheckBox;
    private javax.swing.JCheckBox yIonsJCheckBox;
    private javax.swing.JCheckBox yIonsModifiedJCheckBox;
    private javax.swing.JCheckBox yIonsUnmodifiedJCheckBox;
    private javax.swing.JCheckBox zIonsJCheckBox;
    // End of variables declaration//GEN-END:variables
}
