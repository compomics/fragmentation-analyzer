
package no.uib.fragmentation_analyzer.util;

/**
 * An object containing information about one row in the spectra table.
 * Used for fast and easy access to the list of selected rows.
 *
 * @author Harald Barsnes
 */
public class SpectrumTableRow {

    private Integer identificationId;
    private Integer spectrumId;
    private String sequence;
    private String modifiedSequence;
    private String instrument;

    /**
     * Create a new SpectrumTableRow object.
     *
     * @param identificationId
     * @param spectrumId
     * @param sequence
     * @param modifiedSequence
     * @param instrument
     */
    public SpectrumTableRow(Integer identificationId, Integer spectrumId, String sequence, String modifiedSequence, String instrument) {

        this.identificationId = identificationId;
        this.spectrumId = spectrumId;
        this.sequence = sequence;
        this.modifiedSequence = modifiedSequence;
        this.instrument = instrument;
    }

    @Override
    public boolean equals(Object object) {

        boolean result = false;

        if (object instanceof SpectrumTableRow) {

            SpectrumTableRow temp = (SpectrumTableRow) object;

            if (this.getIdentificationId().intValue() ==  temp.getIdentificationId().intValue()) {
                result = true;
            }
        }

        return result;
    }

    /**
     * @return the identificationId
     */
    public Integer getIdentificationId() {
        return identificationId;
    }

    /**
     * @param identificationId the identificationId to set
     */
    public void setIdentificationId(Integer identificationId) {
        this.identificationId = identificationId;
    }

    /**
     * @return the spectrumId
     */
    public Integer getSpectrumId() {
        return spectrumId;
    }

    /**
     * @param spectrumId the spectrumId to set
     */
    public void setSpectrumId(Integer spectrumId) {
        this.spectrumId = spectrumId;
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
     * @return the instrument
     */
    public String getInstrument() {
        return instrument;
    }

    /**
     * @param instrument the instrument to set
     */
    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }
}
