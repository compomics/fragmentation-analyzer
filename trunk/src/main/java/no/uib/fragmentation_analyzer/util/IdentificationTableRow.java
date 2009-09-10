
package no.uib.fragmentation_analyzer.util;

/**
 * An object containing information about one row in the identifications table. 
 * Used for fast and easy access to the list of selected rows.
 *
 * @author Harald Barsnes
 */
public class IdentificationTableRow {

    private String sequence;
    private String modifiedSequence;
    private Integer countA;
    private Integer countB;

    /**
     * Create a new IdentificationTableRow object.
     *
     * @param sequence
     * @param modifiedSequence
     * @param countA
     * @param countB
     */
    public IdentificationTableRow(String sequence, String modifiedSequence, Integer countA, Integer countB) {

        this.sequence = sequence;
        this.modifiedSequence = modifiedSequence;
        this.countA = countA;
        this.countB = countB;
    }

    @Override
    public boolean equals(Object object) {

        boolean result = false;

        if (object instanceof IdentificationTableRow) {

            IdentificationTableRow temp = (IdentificationTableRow) object;

            if (this.getSequence().equalsIgnoreCase(temp.getSequence()) &&
                    this.getModifiedSequence().equalsIgnoreCase(temp.getModifiedSequence())) {
                result = true;
            }
        }

        return result;
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
     * @return the countA
     */
    public Integer getCountA() {
        return countA;
    }

    /**
     * @param countA the countA to set
     */
    public void setCountA(Integer countA) {
        this.countA = countA;
    }

    /**
     * @return the countB
     */
    public Integer getCountB() {
        return countB;
    }

    /**
     * @param countB the countB to set
     */
    public void setCountB(Integer countB) {
        this.countB = countB;
    }
}
