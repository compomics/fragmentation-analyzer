package no.uib.fragmentation_analyzer.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains information about a peptide identification.
 *
 * @author Harald Barsnes
 */
public class ReducedIdentification implements Comparable<ReducedIdentification> {

    /**
     * The id of the identification.
     */
    private Integer identificationid;
    /**
     * The identified peptide sequence.
     */
    private String sequence;
    /**
     * The identified peptide sequence with terminals and modifications, e.g., NH2-ARTM<Mox>HHR-COOH.
     */
    private String modifiedSequence;
    /**
     * The charge of the identified peptide.
     */
    private Integer charge;
    /**
     * The instrument used to identify the peptide.
     */
    private String instrumentName;
    /**
     * The total intensity of the spectrum used to identify the peptide.
     */
    private Double totalIntensity;
    /**
     * The name of the spectrum file used to identify the peptide.
     */
    private String spectrumFileName;
    /**
     * The file id of the spectrum used to identify the peptide
     */
    private Integer spectrumFileId;
    /**
     * The original file name of the spectrum file.
     */
    private String originalFileName;

    /**
     * Create a new ReducedIdentification object.
     *
     * @param aIdentificationid id of the identification
     * @param aSequence identified peptide sequence
     * @param aModifiedSequene identified peptide sequence with terminals and modifications, e.g., NH2-ARTM<Mox>HHR-COOH
     * @param aCharge charge of the identified peptide
     * @param aInstrumentName instrument used to identify the peptide
     * @param aSpectrumFileName file name of the spectrum used to identify the peptide
     * @param aSpectrumFileId file id of the spectrum used to identify the peptide
     * @param aTotalIntensity total intensity of the spectrum used to identify the peptide
     * @param aOriginalFileName original file name of the spectrum file
     */
    public ReducedIdentification(Integer aIdentificationid, String aSequence, String aModifiedSequene,
            Integer aCharge, String aInstrumentName, String aSpectrumFileName, Integer aSpectrumFileId,
            Double aTotalIntensity, String aOriginalFileName) {

        identificationid = aIdentificationid;
        sequence = aSequence;
        modifiedSequence = aModifiedSequene;
        charge = aCharge;
        instrumentName = aInstrumentName;
        spectrumFileName = aSpectrumFileName;
        spectrumFileId = aSpectrumFileId;
        totalIntensity = aTotalIntensity;
        originalFileName = aOriginalFileName;
    }

    /**
     * Creates a new ReducedIdentification object from a line in a text file.
     *
     * @param lineFromIdentificationsFile the line to parse
     * @param combineFixedAndVariableMods if true variable and fixed modifications, e.g., <Mox> and <Mox*> are
     *                                    combined into one modification type.
     */
    public ReducedIdentification(String lineFromIdentificationsFile, boolean combineFixedAndVariableMods) {

        String[] tokens = lineFromIdentificationsFile.split("\t");

        // Example (tab separated):
        // 197	TLSDYNIQK	NH2-TLSDYNIQK-COOH	2	Micromass Q-TOF	null	[4358 12235]

        identificationid = new Integer(tokens[0]);
        sequence = tokens[1];
        modifiedSequence = tokens[2];

        if (combineFixedAndVariableMods) {
            combineFixedAndVariableModificationAnnotations();
        }

        charge = new Integer(tokens[3]);
        instrumentName = tokens[4];
        spectrumFileName = tokens[5];

        if (spectrumFileName.equalsIgnoreCase("null")) {
            spectrumFileName = null;
        }

        if (tokens.length > 6) {
            if (tokens[6].equalsIgnoreCase("null")) {
                spectrumFileId = null;
            } else {
                spectrumFileId = new Integer(tokens[6]);
            }
        } else {
            spectrumFileId = null;
        }

        if (tokens.length > 7) {
            totalIntensity = new Double(tokens[7]);
        } else {
            totalIntensity = null; // no normalization
        }

        if (tokens.length > 8) {
            originalFileName = tokens[8];
        } else {
            originalFileName = null;
        }
    }

    /**
     * Combines variable and fixed modifications, e.g., <Mox> and <Mox*> are
     * combined into one modification type <Mox>.
     */
    private void combineFixedAndVariableModificationAnnotations() {

        StringBuffer modifiedSequenceBuffer = new StringBuffer(modifiedSequence.length());

        for (int i = 0; i < modifiedSequence.length(); i++) {
            if (modifiedSequence.charAt(i) != '*') {
                modifiedSequenceBuffer.append(modifiedSequence.charAt(i));
            }
        }

        modifiedSequence = modifiedSequenceBuffer.toString();
    }

    /**
     * Returns true of the sequence is modified, false otherwise.
     *
     * @return true of the sequence is modified, false otherwise
     */
    public boolean isModified() {

        String unmodifiedModifiedSequence = modifiedSequence;

        // remove n terminal
        if (unmodifiedModifiedSequence.startsWith("#")) {
            unmodifiedModifiedSequence = unmodifiedModifiedSequence.substring(unmodifiedModifiedSequence.indexOf("#", 1) + 2);
        } else {
            unmodifiedModifiedSequence = unmodifiedModifiedSequence.substring(unmodifiedModifiedSequence.indexOf("-") + 1);
        }

        // remove c terminal
        if (unmodifiedModifiedSequence.endsWith("#")) {
            String temp = unmodifiedModifiedSequence.substring(0, unmodifiedModifiedSequence.length() - 1);
            unmodifiedModifiedSequence = unmodifiedModifiedSequence.substring(0, temp.lastIndexOf("#") - 1);
        } else {
            unmodifiedModifiedSequence = unmodifiedModifiedSequence.substring(0, unmodifiedModifiedSequence.lastIndexOf("-"));
        }

        return !unmodifiedModifiedSequence.equalsIgnoreCase(sequence);
    }

    /**
     * Returns the n terminal.
     *
     * @return the n terminal
     */
    public String getNTerminal() {

        String tempNTerminal;

        if (modifiedSequence.startsWith("#")) {
            tempNTerminal = modifiedSequence.substring(0, modifiedSequence.indexOf("#", 1) + 2);
        } else {
            tempNTerminal = modifiedSequence.substring(0, modifiedSequence.indexOf("-") + 1);
        }

        return tempNTerminal;
    }

    /**
     * Returns the c terminal.
     *
     * @return the c terminal
     */
    public String getCTerminal() {

        String tempCTerminal;

        if (modifiedSequence.endsWith("#")) {
            String temp = modifiedSequence.substring(0, modifiedSequence.length() - 1);
            tempCTerminal = modifiedSequence.substring(temp.lastIndexOf("#") - 1);
        } else {
            tempCTerminal = modifiedSequence.substring(modifiedSequence.lastIndexOf("-"));
        }

        return tempCTerminal;
    }

    /**
     * Returns the internal modifications.
     *
     * @param pattern the compiled modification pattern
     * @return the internal modifications
     */
    public ArrayList<String> getInternalModifications(Pattern pattern) {

        ArrayList<String> tempInternalModifications = new ArrayList<String>();

        String tempSequence = modifiedSequence;

        Matcher matcher = pattern.matcher(tempSequence);

        while (matcher.find()) {

            String currentModification = matcher.group();

            //remove '<' and '>'
//                currentModification =
//                        currentModification.substring(1,
//                        currentModification.length() - 1);

            tempInternalModifications.add(currentModification);

            tempSequence = tempSequence.substring(0, matcher.start()) +
                    tempSequence.substring(matcher.end());

            matcher = pattern.matcher(tempSequence);
        }

        return tempInternalModifications;
    }

    public int compareTo(ReducedIdentification o) {
        return modifiedSequence.compareTo(o.getModifiedSequence());
    }

    @Override
    public String toString() {
        return identificationid + ": " + sequence + " " + modifiedSequence + " " + charge + " " + instrumentName;
    }

    /**
     * @return the identificationid
     */
    public Integer getIdentificationId() {
        return identificationid;
    }

    /**
     * @param identificationid the identificationid to set
     */
    public void setIdentificationid(Integer identificationid) {
        this.identificationid = identificationid;
    }

    /**
     * @return the sequence
     */
    public String getSequence() {
        return sequence;
    }

    /**
     * @param sequence the sequence to set
     */
    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    /**
     * @return the modifiedSequence
     */
    public String getModifiedSequence() {
        return modifiedSequence;
    }

    /**
     * @param modifiedSequence the modifiedSequence to set
     */
    public void setModifiedSequence(String modifiedSequence) {
        this.modifiedSequence = modifiedSequence;
    }

    /**
     * @return the charge
     */
    public Integer getCharge() {
        return charge;
    }

    /**
     * @param charge the charge to set
     */
    public void setCharge(Integer charge) {
        this.charge = charge;
    }

    /**
     * @return the instrumentName
     */
    public String getInstrumentName() {
        return instrumentName;
    }

    /**
     * @param instrumentName the instrumentName to set
     */
    public void setInstrumentName(String instrumentName) {
        this.instrumentName = instrumentName;
    }

    /**
     * @return the totalIntensity
     */
    public Double getTotalIntensity() {
        return totalIntensity;
    }

    /**
     * @param totalIntensity the totalIntensity to set
     */
    public void setTotalIntensity(Double totalIntensity) {
        this.totalIntensity = totalIntensity;
    }

    /**
     * @return the spectrumFileName
     */
    public String getSpectrumFileName() {
        return spectrumFileName;
    }

    /**
     * @param spectrumFileName the spectrumFileName to set
     */
    public void setSpectrumFileName(String spectrumFileName) {
        this.spectrumFileName = spectrumFileName;
    }

    /**
     * @return the spectrumFileId
     */
    public Integer getSpectrumFileId() {
        return spectrumFileId;
    }

    /**
     * @param spectrumFileId the spectrumFileId to set
     */
    public void setSpectrumFileId(Integer spectrumFileId) {
        this.spectrumFileId = spectrumFileId;
    }

    /**
     * @return the originalFileName
     */
    public String getOriginalFileName() {
        return originalFileName;
    }

    /**
     * @param originalFileName the originalFileName to set
     */
    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }
}
