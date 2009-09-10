
package no.uib.fragmentation_analyzer.util;

/**
 * An object that stores information about a fragment ion.
 *
 * @author Harald Barsnes
 */
public class FragmentIon {

    /**
     * The id of the fragment ion.
     */
    private Integer fragmentIonId;
    /**
     * The id of the identification using the fragment ions.
     */
    private Integer identificationId;
    /**
     * The fragment ion type, e.g., y3, b2-H2O.
     */
    private String fragmentIonType;
    /**
     * The theoretical m/z value of the fragment ion.
     */
    private Double fragmentIonMz;
    /**
     * The intensity of the fragment ion.
     */
    private Double fragmentIonIntensity;
    /**
     * The fragment ion number of the fragment ion. E.g, y3 has a fragment ion number of 3.
     */
    private Integer fragmentIonNumber;
    /**
     * The fragment ion mass error, i.e., the (absolute) distance between the theoretical and
     * the experimental mass of the fragment ion. Given as experimental mass - theoretical mass.
     */
    private Double fragmentIonMassError;

    /**
     * Creates a new FragmentIon object from one line in a text file.
     *
     * @param fragmentIonLine
     */
    public FragmentIon(String fragmentIonLine) {

        // example line
        // 1	1	y1	175.119495	11.12	1	0.008269000000012738

        String[] tokens = fragmentIonLine.split("\t");

        fragmentIonId = new Integer(tokens[0]);
        identificationId = new Integer(tokens[1]);
        fragmentIonType = tokens[2];
        fragmentIonMz = new Double(tokens[3]);
        fragmentIonIntensity = new Double(tokens[4]);
        fragmentIonNumber = new Integer(tokens[5]);
        fragmentIonMassError = new Double(tokens[6]);
    }

    /**
     * Creates a new FragmentIon object.
     *
     * @param aFragmentIonId id of the fragment ion
     * @param aIdentificationId id of the identification using the fragment ions
     * @param aFragmentIonType fragment ion type, e.g., y3, b2-H2O
     * @param aFragmentIonMz theoretical m/z value of the fragment ion
     * @param aFragmentIonIntensity intensity of the fragment ion
     * @param aFragmentIonNumber fragment ion number of the fragment ion. E.g, y3 has a fragment ion number of 3
     * @param aFragmentIonMassError fragment ion mass error as (experimental mass - theoretical mass).
     */
    public FragmentIon(Integer aFragmentIonId,
            Integer aIdentificationId,
            String aFragmentIonType,
            Double aFragmentIonMz,
            Double aFragmentIonIntensity,
            Integer aFragmentIonNumber,
            Double aFragmentIonMassError) {

        fragmentIonId = aFragmentIonId;
        identificationId = aIdentificationId;
        fragmentIonType = aFragmentIonType;
        fragmentIonMz = aFragmentIonMz;
        fragmentIonIntensity = aFragmentIonIntensity;
        fragmentIonNumber = aFragmentIonNumber;
        fragmentIonMassError = aFragmentIonMassError;
    }

    @Override
    public String toString(){
        return fragmentIonId.toString();
    }

    /**
     * @return the fragmentIonId
     */
    public Integer getFragmentIonId() {
        return fragmentIonId;
    }

    /**
     * @param fragmentIonId the fragmentIonId to set
     */
    public void setFragmentIonId(Integer fragmentIonId) {
        this.fragmentIonId = fragmentIonId;
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
     * @return the fragmentIonType
     */
    public String getFragmentIonType() {
        return fragmentIonType;
    }

    /**
     * @param fragmentIonType the fragmentIonType to set
     */
    public void setFragmentIonType(String fragmentIonType) {
        this.fragmentIonType = fragmentIonType;
    }

    /**
     * @return the fragmentIonMz
     */
    public Double getFragmenIonMz() {
        return fragmentIonMz;
    }

    /**
     * @param fragmentIonMz the fragmentIonMz to set
     */
    public void setFragmentIonMz(Double fragmentIonMz) {
        this.fragmentIonMz = fragmentIonMz;
    }

    /**
     * @return the fragmentIonIntensity
     */
    public Double getFragmentIonIntensity() {
        return fragmentIonIntensity;
    }

    /**
     * @param fragmentIonIntensity the fragmentIonIntensity to set
     */
    public void setFragmentIonIntensity(Double fragmentIonIntensity) {
        this.fragmentIonIntensity = fragmentIonIntensity;
    }

    /**
     * @return the fragmentIonNumber
     */
    public Integer getFragmentIonNumber() {
        return fragmentIonNumber;
    }

    /**
     * @param fragmentIonNumber the fragmentIonNumber to set
     */
    public void setFragmentIonNumber(Integer fragmentIonNumber) {
        this.fragmentIonNumber = fragmentIonNumber;
    }

    /**
     * @return the fragmentIonMassError
     */
    public Double getFragmentIonMassError() {
        return fragmentIonMassError;
    }

    /**
     * @param fragmentIonMassError the fragmentIonMassError to set
     */
    public void setFragmentMassError(Double fragmentIonMassError) {
        this.fragmentIonMassError = fragmentIonMassError;
    }
}
