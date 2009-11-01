package no.uib.fragmentation_analyzer.util;

import be.proteomics.util.gui.spectrum.DefaultSpectrumAnnotation;
import be.proteomics.util.gui.spectrum.SpectrumPanel;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Pattern;
import no.uib.fragmentation_analyzer.gui.FragmentationAnalyzerJInternalFrame;
import org.jfree.chart.JFreeChart;

/**
 * This class contains many of the properties that are used during the 
 * use of the tool, but that are not stored in the UserProperties.prop 
 * file between each run of the program.
 *
 * @author  Harald Barsnes
 */
public class Properties {

    private String passwordDatabase;
    public static final int GENERAL_SEARCH = 1, MODIFICATION_SEARCH = 2;
    public static final int SEARCH_RESULTS_SHOW_INDIVIDUAL_SPECTRA = 1, SEARCH_RESULTS_INTENSITY_BOX_PLOT = 2,
            SEARCH_RESULTS_MASS_ERROR_SCATTER_PLOT = 3, SEARCH_RESULTS_MASS_ERROR_BUBBLE_PLOT = 4,
            SEARCH_RESULTS_MASS_ERROR_BOX_PLOT = 5, SEARCH_RESULTS_ION_PROBABILITY_PLOT = 6;
    public static final int SPECTRA_VIEW_SPECTRUM = 1, SPECTRA_INTENSITY_BOX_PLOT = 2, 
            SPECTRA_MASS_ERROR_SCATTER_PLOT = 3, SPECTRA_MASS_ERROR_BUBBLE_PLOT = 4,
            SPECTRA_MASS_ERROR_BOX_PLOT = 5, SPECTRA_ION_PROBABILITY_PLOT = 6;
    public static final int SINGLE_PLOT = 0, COMBINE_PLOT = 1;
    public static final int ACCURACY_DA = 0, ACCURACY_PPM = 1;
    private Color defaultMarkerColor = new Color(0, 0, 255, 25); // light blue
    private Color bFragmentIonColor = new Color(0, 0, 255, 25); // light  blue
    private Color yFragmentIonColor = new Color(0, 255, 0, 25); // light green
    private Color otherFragmentIonColor = new Color(255, 0, 0, 25); // light red
    public static final float DEFAULT_VISIBLE_MARKER_ALPHA = 1.0f;
    public static final float DEFAULT_NON_VISIBLE_MARKER_ALPHA = 0.0f;
    public static final int PLOT_LABEL_TYPE_INSTRUMENT = 0, PLOT_LABEL_TYPE_FRAGMENT_ION_TYPE = 1,
            PLOT_LABEL_TYPE_IDENTIFICATION_ID = 2, PLOT_LABEL_TYPE_FRAGMENT_ION_SCORING_TYPE = 3,
            PLOT_LABEL_TYPE_FRAGMENT_ION_THRESHOLD = 4;
    public static final long Y_ION = 7, B_ION = 1;
    private String currentDataSetFolder = null, currentDataSetName = null;
    private HashMap<String, Integer> extractedInternalModifications, extractedNTermModifications,
            extractedCTermModifications, extractedCharges, extractedInstruments;
    private Pattern pattern;
    private HashMap<String, ArrayList<ReducedIdentification>> identificationMap = new HashMap<String, ArrayList<ReducedIdentification>>();
    private HashMap<Integer, ReducedIdentification> allIdentifications = new HashMap<Integer, ReducedIdentification>();
    private HashMap<Integer, SpectrumPanel> linkedSpectrumPanels = new HashMap<Integer, SpectrumPanel>();
    private HashMap<Integer, JFreeChart> allChartFrames = new HashMap<Integer, JFreeChart>();
    private HashMap<Integer, Vector<DefaultSpectrumAnnotation>> allAnnotations = new HashMap<Integer, Vector<DefaultSpectrumAnnotation>>();
    private HashMap<Integer, FragmentationAnalyzerJInternalFrame> allInternalFrames = new HashMap<Integer, FragmentationAnalyzerJInternalFrame>();
    private ArrayList<IdentificationTableRow> currentlySelectedRowsInSearchTable = new ArrayList<IdentificationTableRow>();
    private ArrayList<SpectrumTableRow> currentlySelectedRowsInSpectraTable = new ArrayList<SpectrumTableRow>();
    private int currentLabelType = 0; // no type selected
    private String modificationPattern = "[<][^<]*[>]";
    private boolean showLegend = true, showMarkers = false, showAverageMassError = false;
        
    /**
     * Creates a new empty Properties object.
     */
    public Properties() {
        setPattern(Pattern.compile(modificationPattern));
    }

    /**
     * Retrieves the version number set in the pom file.
     *
     * @return the version number of the fragmentation analyzer
     */
    public String getVersion() {

        java.util.Properties p = new java.util.Properties();

        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("fragmentation-analyzer.properties");
            p.load( is );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return p.getProperty("fragmentation-analyzer.version");
    }

    /**
     * Returns the current ms_lims database password.
     * 
     * @return the current ms_lims database password
     */
    public String getPassWord() {
        return passwordDatabase;
    }

    /**
     * Set the current ms_lims database password
     * 
     * @param passWord
     */
    public void setPassWord(String passWord) {
        this.passwordDatabase = passWord;
    }

        /**
     * @return the pattern
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * @param aPattern the pattern to set
     */
    public void setPattern(Pattern aPattern) {
        pattern = aPattern;
    }

    /**
     * @return the currentDataSetFolder
     */
    public String getCurrentDataSetFolder() {
        return currentDataSetFolder;
    }

    /**
     * @param currentDataSetFolder the currentDataSetFolder to set
     */
    public void setCurrentDataSetFolder(String currentDataSetFolder) {
        this.currentDataSetFolder = currentDataSetFolder;
    }

    /**
     * @return the currentDataSetName
     */
    public String getCurrentDataSetName() {
        return currentDataSetName;
    }

    /**
     * @param currentDataSetName the currentDataSetName to set
     */
    public void setCurrentDataSetName(String currentDataSetName) {
        this.currentDataSetName = currentDataSetName;
    }

    /**
     * @return the extractedInternalModifications
     */
    public HashMap<String, Integer> getExtractedInternalModifications() {
        return extractedInternalModifications;
    }

    /**
     * @param aExtractedInternalModifications the extractedInternalModifications to set
     */
    public void setExtractedInternalModifications(HashMap<String, Integer> aExtractedInternalModifications) {
        extractedInternalModifications = aExtractedInternalModifications;
    }

    /**
     * @return the extractedNTermModifications
     */
    public HashMap<String, Integer> getExtractedNTermModifications() {
        return extractedNTermModifications;
    }

    /**
     * @param aExtractedNTermModifications the extractedNTermModifications to set
     */
    public void setExtractedNTermModifications(HashMap<String, Integer> aExtractedNTermModifications) {
        extractedNTermModifications = aExtractedNTermModifications;
    }

    /**
     * @return the extractedCTermModifications
     */
    public HashMap<String, Integer> getExtractedCTermModifications() {
        return extractedCTermModifications;
    }

    /**
     * @param aExtractedCTermModifications the extractedCTermModifications to set
     */
    public void setExtractedCTermModifications(HashMap<String, Integer> aExtractedCTermModifications) {
        extractedCTermModifications = aExtractedCTermModifications;
    }

    /**
     * @return the extractedCharges
     */
    public HashMap<String, Integer> getExtractedCharges() {
        return extractedCharges;
    }

    /**
     * @param aExtractedCharges the extractedCharges to set
     */
    public void setExtractedCharges(HashMap<String, Integer> aExtractedCharges) {
        extractedCharges = aExtractedCharges;
    }

    /**
     * @return the extractedInstruments
     */
    public HashMap<String, Integer> getExtractedInstruments() {
        return extractedInstruments;
    }

    /**
     * @param aExtractedInstruments the extractedInstruments to set
     */
    public void setExtractedInstruments(HashMap<String, Integer> aExtractedInstruments) {
        extractedInstruments = aExtractedInstruments;
    }

    /**
     * @return the identificationMap
     */
    public HashMap<String, ArrayList<ReducedIdentification>> getIdentificationMap() {
        return identificationMap;
    }

    /**
     * @param identificationMap the identificationMap to set
     */
    public void setIdentificationMap(HashMap<String, ArrayList<ReducedIdentification>> identificationMap) {
        this.identificationMap = identificationMap;
    }

    /**
     * @return the allIdentifications
     */
    public HashMap<Integer, ReducedIdentification> getAllIdentifications() {
        return allIdentifications;
    }

    /**
     * @param allIdentifications the allIdentifications to set
     */
    public void setAllIdentifications(HashMap<Integer, ReducedIdentification> allIdentifications) {
        this.allIdentifications = allIdentifications;
    }

    /**
     * @return the linkedSpectrumPanels
     */
    public HashMap<Integer, SpectrumPanel> getLinkedSpectrumPanels() {
        return linkedSpectrumPanels;
    }

    /**
     * @param linkedSpectrumPanels the linkedSpectrumPanels to set
     */
    public void setLinkedSpectrumPanels(HashMap<Integer, SpectrumPanel> linkedSpectrumPanels) {
        this.linkedSpectrumPanels = linkedSpectrumPanels;
    }

    /**
     * @return the allChartFrames
     */
    public HashMap<Integer, JFreeChart> getAllChartFrames() {
        return allChartFrames;
    }

    /**
     * @param allChartFrames the allChartFrames to set
     */
    public void setAllChartFrames(HashMap<Integer, JFreeChart> allChartFrames) {
        this.allChartFrames = allChartFrames;
    }

    /**
     * @return the allAnnotations
     */
    public HashMap<Integer, Vector<DefaultSpectrumAnnotation>> getAllAnnotations() {
        return allAnnotations;
    }

    /**
     * @param allAnnotations the allAnnotations to set
     */
    public void setAllAnnotations(HashMap<Integer, Vector<DefaultSpectrumAnnotation>> allAnnotations) {
        this.allAnnotations = allAnnotations;
    }

    /**
     * @return the allInternalFrames
     */
    public HashMap<Integer, FragmentationAnalyzerJInternalFrame> getAllInternalFrames() {
        return allInternalFrames;
    }

    /**
     * @param allInternalFrames the allInternalFrames to set
     */
    public void setAllInternalFrames(HashMap<Integer, FragmentationAnalyzerJInternalFrame> allInternalFrames) {
        this.allInternalFrames = allInternalFrames;
    }

    /**
     * @return the defaultMarkerColor
     */
    public Color getDefaultMarkerColor() {
        return defaultMarkerColor;
    }

    /**
     * @param defaultMarkerColor the defaultMarkerColor to set
     */
    public void setDefaultMarkerColor(Color defaultMarkerColor) {
        this.defaultMarkerColor = defaultMarkerColor;
    }

    /**
     * @return the bFragmentIonColor
     */
    public Color getbFragmentIonColor() {
        return bFragmentIonColor;
    }

    /**
     * @param bFragmentIonColor the bFragmentIonColor to set
     */
    public void setbFragmentIonColor(Color bFragmentIonColor) {
        this.bFragmentIonColor = bFragmentIonColor;
    }

    /**
     * @return the yFragmentIonColor
     */
    public Color getyFragmentIonColor() {
        return yFragmentIonColor;
    }

    /**
     * @param yFragmentIonColor the yFragmentIonColor to set
     */
    public void setyFragmentIonColor(Color yFragmentIonColor) {
        this.yFragmentIonColor = yFragmentIonColor;
    }

    /**
     * @return the otherFragmentIonColor
     */
    public Color getOtherFragmentIonColor() {
        return otherFragmentIonColor;
    }

    /**
     * @param otherFragmentIonColor the otherFragmentIonColor to set
     */
    public void setOtherFragmentIonColor(Color otherFragmentIonColor) {
        this.otherFragmentIonColor = otherFragmentIonColor;
    }

    /**
     * @return the currentlySelectedRowsInSearchTable
     */
    public ArrayList<IdentificationTableRow> getCurrentlySelectedRowsInSearchTable() {
        return currentlySelectedRowsInSearchTable;
    }

    /**
     * @param currentlySelectedRowsInSearchTable the currentlySelectedRowsInSearchTable to set
     */
    public void setCurrentlySelectedRowsInSearchTable(ArrayList<IdentificationTableRow> currentlySelectedRowsInSearchTable) {
        this.currentlySelectedRowsInSearchTable = currentlySelectedRowsInSearchTable;
    }

    /**
     * @return the currentlySelectedRowsInSpectraTable
     */
    public ArrayList<SpectrumTableRow> getCurrentlySelectedRowsInSpectraTable() {
        return currentlySelectedRowsInSpectraTable;
    }

    /**
     * @param currentlySelectedRowsInSpectraTable the currentlySelectedRowsInSpectraTable to set
     */
    public void setCurrentlySelectedRowsInSpectraTable(ArrayList<SpectrumTableRow> currentlySelectedRowsInSpectraTable) {
        this.currentlySelectedRowsInSpectraTable = currentlySelectedRowsInSpectraTable;
    }

    /**
     * Sets the label type to use in the scatter and bubble plots.
     *
     * @param labelType of the following: PLOT_LABEL_TYPE_INSTRUMENT, PLOT_LABEL_TYPE_FRAGMENT_ION_TYPE,
     *        PLOT_LABEL_TYPE_FRAGMENT_ION_SCORING_TYPE or PLOT_LABEL_TYPE_FRAGMENT_ION_THRESHOLD
     */
    public void setCurrentLabelType(int labelType) {
        currentLabelType = labelType;
    }

    /**
     * Returns the currently used/last used label type. One of the following:
     * PLOT_LABEL_TYPE_INSTRUMENT, PLOT_LABEL_TYPE_FRAGMENT_ION_TYPE, PLOT_LABEL_TYPE_FRAGMENT_ION_SCORING_TYPE
     * or PLOT_LABEL_TYPE_FRAGMENT_ION_THRESHOLD
     *
     * @return the current label type, one of the following:
     *         PLOT_LABEL_TYPE_INSTRUMENT, PLOT_LABEL_TYPE_FRAGMENT_ION_TYPE, PLOT_LABEL_TYPE_FRAGMENT_ION_SCORING_TYPE
     *         or PLOT_LABEL_TYPE_FRAGMENT_ION_THRESHOLD
     */
    public int getCurrentLabelType() {
        return currentLabelType;
    }

    /**
     * @return the showLegend
     */
    public boolean showLegend() {
        return showLegend;
    }

    /**
     * @param showLegend the showLegend to set
     */
    public void setShowLegend(boolean showLegend) {
        this.showLegend = showLegend;
    }

    /**
     * @return the showMarkers
     */
    public boolean showMarkers() {
        return showMarkers;
    }

    /**
     * @param showMarkers the showMarkers to set
     */
    public void setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
    }

    /**
     * @return the showAverageMassError
     */
    public boolean showAverageMassError() {
        return showAverageMassError;
    }

    /**
     * @param showAverageMassError the showAverageMassError to set
     */
    public void setShowAverageMassError(boolean showAverageMassError) {
        this.showAverageMassError = showAverageMassError;
    }
}
