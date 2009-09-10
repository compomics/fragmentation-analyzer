package no.uib.fragmentation_analyzer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Contains information about the contents of one PKL file.
 *
 * @author Harald Barsnes
 */
public class PKLFile {

    private int precurorCharge;
    private double precursorMz;
    private double precursorIntensity;
    private double[] mzValues;
    private double[] intensityValues;
    private String fileName;
    private String spectrumFileId;

    /**
     * Parse a PKL file and store the details in the PKLFile object.
     *
     * @param pklFile the file to parse
     * @throws IOException
     */
    public PKLFile(File pklFile) throws IOException {

        FileReader f = new FileReader(pklFile);
        BufferedReader b = new BufferedReader(f);

        fileName = pklFile.getName();
        spectrumFileId = pklFile.getName().substring(0, pklFile.getName().length() - 4);

        // read precursor details
        String precursorLine = b.readLine();

        String[] precursorDetails = precursorLine.split("\t");

        precursorMz = new Double(precursorDetails[0]);
        precursorIntensity = new Double(precursorDetails[1]);
        precurorCharge = new Integer(precursorDetails[2]);

        HashMap<Double, Double> peaks = new HashMap<Double, Double>();

        String peakLine = b.readLine();

        while (peakLine != null) {
            String[] peakDetails = peakLine.split("\t");
            peaks.put(new Double(peakDetails[0]), new Double(peakDetails[1]));
            peakLine = b.readLine();
        }

        // sort the values in increasing order
        TreeSet treeSet = new TreeSet();
        treeSet.clear();
        treeSet.addAll(peaks.keySet());

        Iterator treeSetIterator = treeSet.iterator();

        Double tempMz;
        mzValues = new double[peaks.size()];
        intensityValues = new double[peaks.size()];

        int peakCounter = 0;

        while (treeSetIterator.hasNext()) {
            tempMz = (Double) treeSetIterator.next();
            mzValues[peakCounter] = tempMz;
            intensityValues[peakCounter++] = peaks.get(tempMz);
        }

        b.close();
        f.close();
    }

    /**
     * @return the precurorCharge
     */
    public int getPrecurorCharge() {
        return precurorCharge;
    }

    /**
     * @param precurorCharge the precurorCharge to set
     */
    public void setPrecurorCharge(int precurorCharge) {
        this.precurorCharge = precurorCharge;
    }

    /**
     * @return the precursorMz
     */
    public double getPrecursorMz() {
        return precursorMz;
    }

    /**
     * @param precursorMz the precursorMz to set
     */
    public void setPrecursorMz(double precursorMz) {
        this.precursorMz = precursorMz;
    }

    /**
     * @return the precursorIntensity
     */
    public double getPrecursorIntensity() {
        return precursorIntensity;
    }

    /**
     * @param precursorIntensity the precursorIntensity to set
     */
    public void setPrecursorIntensity(double precursorIntensity) {
        this.precursorIntensity = precursorIntensity;
    }

    /**
     * @return the mzValues
     */
    public double[] getMzValues() {
        return mzValues;
    }

    /**
     * @param mzValues the mzValues to set
     */
    public void setMzValues(double[] mzValues) {
        this.mzValues = mzValues;
    }

    /**
     * @return the intensityValues
     */
    public double[] getIntensityValues() {
        return intensityValues;
    }

    /**
     * @param intensityValues the intensityValues to set
     */
    public void setIntensityValues(double[] intensityValues) {
        this.intensityValues = intensityValues;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return the spectrumFileId
     */
    public String getSpectrumFileId() {
        return spectrumFileId;
    }

    /**
     * @param spectrumFileId the spectrumFileId to set
     */
    public void setSpectrumFileId(String spectrumFileId) {
        this.spectrumFileId = spectrumFileId;
    }
}
