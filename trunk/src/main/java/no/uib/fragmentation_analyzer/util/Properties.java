package no.uib.fragmentation_analyzer.util;

/**
 * This class contains many of the properties that are used during the 
 * use of the tool, but that are not stored in the UserProperties.prop 
 * file between each run of the program.
 *
 * @author  Harald Barsnes
 */
public class Properties {
    
    private String passwordDatabase;
   
    /**
     * Creates a new empty Properties object.
     */
    public Properties() {

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
}
