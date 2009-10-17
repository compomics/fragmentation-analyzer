package no.uib.fragmentation_analyzer.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import no.uib.fragmentation_analyzer.gui.ProgressDialog;
import no.uib.fragmentation_analyzer.gui.ProgressDialogParent;

/**
 * Takes care of saving and retrieving the user properites, i.e, the information stored between
 * each run of the tool.
 *
 * @author  Harald Barsnes
 */
public class UserProperties implements ProgressDialogParent {

    // defaults user settings, used if the UserProperties file can not be read
    private String userName = ""; //database user name
    private String serverHost = ""; //database serverhost
    private String schema = ""; //database schema
    private String versionNumber;
    private int minimumSequencePairCounter = 30;
    private String lastUsedFolder = "";
    private double mascotConfidenceLevel = 0.95;
    private int ppmBubbleScaling = 1000;
    private int defaultBubbleScaling = 1;
    private boolean cancelProgress = false;
    private ProgressDialog progressDialog;
    private boolean notSignificantNotScoringFragmentIon = false;
    private boolean significantNotScoringFragmentIon = true;
    private boolean significantScoringFragmentIon = true;

    /**
     * Creates a new UserProperties object
     * 
     * @param versionNumber
     */
    public UserProperties(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    /**
     * Tries to read the user properties from file.
     */
    public void readUserPropertiesFromFile(File settingsFile) {

        boolean importOldProperties = false;

        try {
            String path = "" + this.getClass().getProtectionDomain().getCodeSource().getLocation();
            path = path.substring(5, path.lastIndexOf("/"));
            path = path + "/Properties/UserProperties.prop";
            path = path.replace("%20", " ");

            File file;

            // use the default settings file
            if (settingsFile == null) {
                file = new File(path);
            } else {
                file = settingsFile;
            }

            FileReader f = new FileReader(file);
            BufferedReader b = new BufferedReader(f);
            String s = b.readLine(); // header
            String version = b.readLine(); // version

            // see if the userproperties file is the (empty) default one. if it is
            // then ask the user if he/she wants to import the old user settings
            if (version.endsWith("*")) {

                // note: importing old user settings is currently disabled

//                int option = JOptionPane.showConfirmDialog(null,
//                        "Are you upgrading from an older version of FragmentationAnalyzer?",
//                        "Upgrading FragmentationAnalyzer?", JOptionPane.YES_NO_OPTION);
//
//                if (option == JOptionPane.YES_OPTION) {
//                    option = JOptionPane.showConfirmDialog(null,
//                            "Import the settings from the previous version?",
//                            "Import Settings?", JOptionPane.YES_NO_OPTION);
//
//                    if (option == JOptionPane.YES_OPTION) {
//                        importOldProperties = true;
//                    }
//                }

                // Removes the '*' at the end of the version number
                // The '*' is used as a marker showing that the user as
                // not been asked to import old user setting.
                version = version.substring(0, version.length() - 1);
            }

            s = b.readLine();
            userName = s.substring(s.indexOf(": ") + 2);
            s = b.readLine();
            serverHost = s.substring(s.indexOf(": ") + 2);
            s = b.readLine();
            schema = s.substring(s.indexOf(": ") + 2);
            s = b.readLine();
            minimumSequencePairCounter = new Integer(s.substring(s.indexOf(": ") + 2));
            s = b.readLine();
            lastUsedFolder = s.substring(s.indexOf(": ") + 2);
            s = b.readLine();
            mascotConfidenceLevel = new Double(s.substring(s.indexOf(": ") + 2));
            s = b.readLine();
            ppmBubbleScaling = new Integer(s.substring(s.indexOf(": ") + 2));
            s = b.readLine();
            defaultBubbleScaling = new Integer(s.substring(s.indexOf(": ") + 2));

            // get the fragment ion scoring types, requires v1.1 or newer
            s = b.readLine();

            if(s != null){
                notSignificantNotScoringFragmentIon = new Boolean(s.substring(s.indexOf(": ") + 2));
            }

            s = b.readLine();

            if(s != null){
                significantNotScoringFragmentIon = new Boolean(s.substring(s.indexOf(": ") + 2));
            }

            s = b.readLine();

            if(s != null){
                significantScoringFragmentIon = new Boolean(s.substring(s.indexOf(": ") + 2));
            }

            b.close();
            f.close();

            // import old user settings
            if (importOldProperties) {
                //importUserProperties(); // not currently used
            }

            if (settingsFile != null) {
                JOptionPane.showMessageDialog(null,
                        "The old settings has been successfully imported.\n" +
                        "(Changes to the memory settings requires a restart.)",
                        "Settings Imported", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(null,
                    "Error when reading the user properties. " +
                    "See ../Properties/ErrorLog.txt for more details.",
                    "File Not Found", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error when reading the UserProperties: ");
            ex.printStackTrace();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                    "Error when reading the user properties. " +
                    "See ../Properties/ErrorLog.txt for more details.",
                    "File Error", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error when reading the UserProperties: ");
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            JOptionPane.showMessageDialog(null,
                    "Error when reading the user properties. " +
                    "See ../Properties/ErrorLog.txt for more details.",
                    "File Error", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error when reading the UserProperties: ");
            ex.printStackTrace();
        }
    }

    /**
     * Tries to import user properties.
     */
    private void importUserProperties() throws FileNotFoundException, IOException {

        String path = "" + this.getClass().getProtectionDomain().getCodeSource().getLocation();
        path = path.substring(5, path.lastIndexOf("/"));
        path = path.replace("%20", " ");

        String oldUserPropertiesLocation =
                "" + this.getClass().getProtectionDomain().getCodeSource().getLocation();
        oldUserPropertiesLocation =
                oldUserPropertiesLocation.substring(5, oldUserPropertiesLocation.lastIndexOf("/"));
        oldUserPropertiesLocation =
                oldUserPropertiesLocation.substring(0, oldUserPropertiesLocation.lastIndexOf("/") +
                1) + "Properties/UserProperties.prop";
        oldUserPropertiesLocation = oldUserPropertiesLocation.replace("%20", " ");

        JFileChooser chooser = new JFileChooser();

        int option = JOptionPane.showConfirmDialog(null,
                "Please locate the old settings file 'UserProperties.prop'.\n" +
                "(It is in the Properties folder of the previous installation.)",
                "Locate Old Settings File", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {

            chooser.setDialogTitle("Select Old Settings File 'UserProperties.prop'");

            int returnVal = chooser.showOpenDialog(null);

            if (returnVal == JFileChooser.APPROVE_OPTION) {

                File selectedFile = chooser.getSelectedFile();
                FileReader fr = new FileReader(selectedFile);
                BufferedReader br = new BufferedReader(fr);
                String firstLine = br.readLine();
                br.close();
                fr.close();

                boolean cancel = false;

                while ((!selectedFile.getName().equalsIgnoreCase("UserProperties.prop") ||
                        (new File(oldUserPropertiesLocation).equals(selectedFile)) ||
                        !firstLine.equalsIgnoreCase("FragmentationAnalyzer")) &&
                        !cancel) {

                    if (!selectedFile.getName().equalsIgnoreCase("UserProperties.prop")) {
                        option = JOptionPane.showConfirmDialog(null,
                                "The selected file is not 'UserProperties.prop'.\n" +
                                "Please select the file named 'UserProperties.prop' in the Properties folder.",
                                "Locate Old Settings File", JOptionPane.OK_CANCEL_OPTION);
                    } else if (new File(oldUserPropertiesLocation).equals(selectedFile)) {
                        //trying to upgrade from downloaded UserProperties file
                        option = JOptionPane.showConfirmDialog(null,
                                "It seems like you are trying to upgrade from the wrong UserProperties file.\n" +
                                "Please select the file named 'UserProperties.prop' in the Properties folder \n" +
                                "of the previous installation of FragmentationAnalyzer.",
                                "Wrong UserProperties File", JOptionPane.OK_CANCEL_OPTION);
                    } else {
                        option = JOptionPane.showConfirmDialog(null,
                                "The selected file is not a FragmentationAnalyzer 'UserProperties.prop' file.\n" +
                                "Please select the file named 'UserProperties.prop' in the FragmentationAnalyzer\n" +
                                "Properties folder.",
                                "Locate Old Settings File", JOptionPane.OK_CANCEL_OPTION);
                    }

                    if (option == JOptionPane.CANCEL_OPTION) {
                        cancel = true;
                    } else {

                        returnVal = chooser.showOpenDialog(null);

                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            selectedFile = chooser.getSelectedFile();
                            fr = new FileReader(selectedFile);
                            br = new BufferedReader(fr);
                            firstLine = br.readLine();
                            br.close();
                            fr.close();
                        } else {
                            cancel = true;
                        }
                    }
                }

                if (!cancel) {

                    // copy the instrumens, contacts, protocols and samples
                    File propertiesFolder = selectedFile.getParentFile();

                    // copy the JavaOptions file
                    if (new File(propertiesFolder + "/JavaOptions.txt").exists()) {
                        Util.copyFile(new File(propertiesFolder + "/JavaOptions.txt"),
                                new File(path + "/Properties/JavaOptions.txt"));
                    }

                    // add the java stack size options if not already included
                    File newJavaOptionsFile = new File(path + "/Properties/JavaOptions.txt");
                    FileReader fileReader = new FileReader(newJavaOptionsFile);
                    BufferedReader bufferedReader = new BufferedReader(fileReader);

                    boolean insertJavaStackSizeOptions = true;

                    String temp = bufferedReader.readLine();

                    while (temp != null) {
                        if (temp.lastIndexOf("-Xss") != -1) {
                            insertJavaStackSizeOptions = false;
                        } else if (temp.lastIndexOf("-Xoss") != -1) {
                            insertJavaStackSizeOptions = false;
                        }

                        temp = bufferedReader.readLine();
                    }

                    if (insertJavaStackSizeOptions) {
                        FileWriter fw = new FileWriter(newJavaOptionsFile, true);
                        BufferedWriter bw = new BufferedWriter(fw);
                        bw.write("-Xss1M\n-Xoss1M");
                        bw.close();
                        fw.close();
                    }


                    option = JOptionPane.showConfirmDialog(null, "Import data sets as well?",
                            "Import Data Sets?", JOptionPane.YES_NO_OPTION);

                    if (option == JOptionPane.YES_OPTION) {
                        importDataSets(propertiesFolder, path);
                    }

                    //copy the old UserProperties.prop file
                    readUserPropertiesFromFile(selectedFile);
                }
            }
        }
    }

    /**
     * Tries to import the existing data sets.
     */
    private void importDataSets(File aPropertiesFolder, String aPath) {

        final File propertiesFolder = aPropertiesFolder;
        final String path = aPath;

        progressDialog = new ProgressDialog(this, true);

        new Thread(new Runnable() {

            public void run() {
                progressDialog.setTitle("Importing Data Sets. Please Wait...");
                progressDialog.setIntermidiate(true);
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();


        new Thread("LoadThread") {

            @Override
            public void run() {

                File dataSetFolder = new File(propertiesFolder.getParentFile() + "/DataSets/");

                // if the data set folder is not included we have to create it
                if (!new File(path + "/DataSets/").exists()) {
                    new File(path + "/DataSets/").mkdir();
                }

                File[] dataSets = dataSetFolder.listFiles();

                for (int i = 0; i < dataSets.length && !cancelProgress; i++) {

                    File currentDataSet = dataSets[i];
                    String dataSetName = currentDataSet.getName();
                    File[] files = currentDataSet.listFiles();
                    new File(path + "/DataSets/" + dataSetName + "/").mkdir();

                    progressDialog.setString("(" + (i + 1) + "/" + dataSets.length + ")");

                    for (int j = 0; j < files.length && !cancelProgress; j++) {

                        File dataFile = files[j];

                        if (dataFile.isDirectory()) {

                            progressDialog.setTitle("Importing Spectra. Please Wait...");
                            progressDialog.setIntermidiate(false);

                            File[] spectra = dataFile.listFiles();

                            progressDialog.setMax(spectra.length);
                            progressDialog.setValue(0);

                            new File(path + "/DataSets/" + dataSetName + "/spectra/").mkdir();

                            // the spectra folder
                            for (int k = 0; k < spectra.length && !cancelProgress; k++) {
                                progressDialog.setValue(k);
                                File spectrum = spectra[k];
                                Util.copyFile(spectrum, new File(path + "/DataSets/" + dataSetName + "/spectra/" + spectrum.getName()));
                            }
                        } else {

                            if (dataFile.getName().equalsIgnoreCase("identifications.txt")) {
                                progressDialog.setTitle("Importing Identifications. Please Wait...");
                            } else {
                                progressDialog.setTitle("Importing Fragment Ions. Please Wait...");
                            }

                            progressDialog.setIntermidiate(true);
                            progressDialog.setString("(" + (i + 1) + "/" + dataSets.length + ")");

                            // the identification and fragment ion files
                            Util.copyFile(dataFile, new File(path + "/DataSets/" + dataSetName + "/" + dataFile.getName()));
                        }
                    }
                }

                progressDialog.setVisible(false);
                progressDialog.dispose();

                if (!cancelProgress) {
                    JOptionPane.showMessageDialog(null,
                            "The data sets have been successfully imported.",
                            "Data Sets Imported", JOptionPane.INFORMATION_MESSAGE);
                }

            }
        }.start();
    }

    /**
     * Tries to save the user properties to file.
     */
    public void saveUserPropertiesToFile() {

        try {
            String path = "" + this.getClass().getProtectionDomain().getCodeSource().getLocation();
            path = path.substring(5, path.lastIndexOf("/"));
            path = path + "/Properties/UserProperties.prop";
            path = path.replace("%20", " ");

            File file = new File(path);
            file.getAbsolutePath();
            FileWriter f = new FileWriter(file);

            f.write("FragmentationAnalyzer\n");
            f.write(versionNumber + "\n");
            f.write("UserName: " + userName + "\n");
            f.write("ServerHost: " + serverHost + "\n");
            f.write("Schema: " + schema + "\n");
            f.write("ModificationPairMinimum: " + minimumSequencePairCounter + "\n");
            f.write("LastUsedFolder: " + lastUsedFolder + "\n");
            f.write("MascotConfidenceLevel: " + mascotConfidenceLevel + "\n");
            f.write("ppmBubbleScaling: " + ppmBubbleScaling + "\n");
            f.write("DefaultBubbleScaling: " + defaultBubbleScaling + "\n");
            f.write("NotSignificantNotScoringFragmentIon: " + notSignificantNotScoringFragmentIon + "\n");
            f.write("SignificantNotScoringFragmentIon: " + significantNotScoringFragmentIon + "\n");
            f.write("SignificantScoringFragmentIon: " + significantScoringFragmentIon + "\n");

            f.close();

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                    "Error saving the user properties.\n" +
                    "See the ErrorLog for more details.",
                    "File Error", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("UserProperties: ");
            ex.printStackTrace();
        }
    }

    /**
     * Set the user name
     * 
     * @param userName
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Returns the user name 
     * 
     * @return the user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Set the server host
     * 
     * @param serverHost
     */
    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    /**
     * Returne the name of server host
     * 
     * @return the name of server host
     */
    public String getServerHost() {
        return serverHost;
    }

    /**
     * Set the schema
     * 
     * @param schema
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Returns the name of the schema
     * 
     * @return the name of the schema
     */
    public String getSchema() {
        return schema;
    }

    /**
     * @return the minimumSequencePairCounter
     */
    public int getMinimumIdentificationPairCounter() {
        return minimumSequencePairCounter;
    }

    /**
     * @param minimumSequencePairCounter the minimumSequencePairCounter to set
     */
    public void setMinimumSequencePairCounter(int minimumSequencePairCounter) {
        this.minimumSequencePairCounter = minimumSequencePairCounter;
    }

    /**
     * @return the lastUsedFolder
     */
    public String getLastUsedFolder() {
        return lastUsedFolder;
    }

    /**
     * @param lastUsedFolder the lastUsedFolder to set
     */
    public void setLastUsedFolder(String lastUsedFolder) {
        this.lastUsedFolder = lastUsedFolder;
    }

    /**
     * @return the mascotConfidenceLevel
     */
    public double getMascotConfidenceLevel() {
        return mascotConfidenceLevel;
    }

    /**
     * @param mascotConfidenceLevel the mascotConfidenceLevel to set
     */
    public void setMascotConfidenceLevel(double mascotConfidenceLevel) {
        this.mascotConfidenceLevel = mascotConfidenceLevel;
    }

    /**
     * @return the ppmBubbleScaling
     */
    public int getPpmBubbleScaling() {
        return ppmBubbleScaling;
    }

    /**
     * @param ppmBubbleScaling the ppmBubbleScaling to set
     */
    public void setPpmBubbleScaling(int ppmBubbleScaling) {
        this.ppmBubbleScaling = ppmBubbleScaling;
    }

    /**
     * @return the defaultBubbleScaling
     */
    public int getDefaultBubbleScaling() {
        return defaultBubbleScaling;
    }

    /**
     * @param defaultBubbleScaling the defaultBubbleScaling to set
     */
    public void setDefaultBubbleScaling(int defaultBubbleScaling) {
        this.defaultBubbleScaling = defaultBubbleScaling;
    }

    public void cancelProgress() {
        cancelProgress = true;
    }

        /**
     * @return the notSignificantNotScoringFragmentIon
     */
    public boolean isNotSignificantNotScoringFragmentIon() {
        return notSignificantNotScoringFragmentIon;
    }

    /**
     * @param notSignificantNotScoringFragmentIon the notSignificantNotScoringFragmentIon to set
     */
    public void setNotSignificantNotScoringFragmentIon(boolean notSignificantNotScoringFragmentIon) {
        this.notSignificantNotScoringFragmentIon = notSignificantNotScoringFragmentIon;
    }

    /**
     * @return the significantNotScoringFragmentIon
     */
    public boolean isSignificantNotScoringFragmentIon() {
        return significantNotScoringFragmentIon;
    }

    /**
     * @param significantNotScoringFragmentIon the significantNotScoringFragmentIon to set
     */
    public void setSignificantNotScoringFragmentIon(boolean significantNotScoringFragmentIon) {
        this.significantNotScoringFragmentIon = significantNotScoringFragmentIon;
    }

    /**
     * @return the significantScoringFragmentIon
     */
    public boolean isSignificantScoringFragmentIon() {
        return significantScoringFragmentIon;
    }

    /**
     * @param significantScoringFragmentIon the significantScoringFragmentIon to set
     */
    public void setSignificantScoringFragmentIon(boolean significantScoringFragmentIon) {
        this.significantScoringFragmentIon = significantScoringFragmentIon;
    }
}
