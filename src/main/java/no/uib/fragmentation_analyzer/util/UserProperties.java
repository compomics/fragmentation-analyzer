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
 * Takes care of saving and retrieving the user properites, i.e, the information
 * stored between each run of the tool.
 *
 * @author Harald Barsnes
 */
public class UserProperties implements ProgressDialogParent {

    // default user settings, used if the UserProperties file can not be read
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
    private boolean aPosterioriDetected = false;
    private boolean detectedNotScoring = true;
    private boolean detectedAndScoring = true;
    private boolean normalizeIntensites = true;
    private int numberOfPlotsPerRow = 2;
    private int numberOfPlotsPerColumn = 2;
    private boolean useSpearmansCorrelation = true;

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

            if (!file.exists()) {
                file = new File("./src/main/resources/PropertiesAndDataSets/Properties/UserProperties.prop");
            }

            FileReader f = new FileReader(file);
            BufferedReader b = new BufferedReader(f);
            String s = b.readLine(); // header
            String version = b.readLine(); // version

            // see if the userproperties file is the (empty) default one. if it is
            // then ask the user if he/she wants to import the old user settings
            if (version.endsWith("*")) {

                int option = JOptionPane.showConfirmDialog(null,
                        "Are you upgrading from an older version of FragmentationAnalyzer?",
                        "Upgrading FragmentationAnalyzer?", JOptionPane.YES_NO_OPTION);

                if (option == JOptionPane.YES_OPTION) {
                    option = JOptionPane.showConfirmDialog(null,
                            "Import the settings from the previous version?",
                            "Import Settings?", JOptionPane.YES_NO_OPTION);

                    if (option == JOptionPane.YES_OPTION) {
                        importOldProperties = true;
                    }
                }

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

            if (s != null) {
                aPosterioriDetected = new Boolean(s.substring(s.indexOf(": ") + 2));
            }

            s = b.readLine();

            if (s != null) {
                detectedNotScoring = new Boolean(s.substring(s.indexOf(": ") + 2));
            }

            s = b.readLine();

            if (s != null) {
                detectedAndScoring = new Boolean(s.substring(s.indexOf(": ") + 2));
            }

            // get the normalize intensity, requires v1.3 or newer
            s = b.readLine();

            if (s != null) {
                normalizeIntensites = new Boolean(s.substring(s.indexOf(": ") + 2));
            }

            // get the number of plots per row and column, requires v1.3.1 or newer
            s = b.readLine();

            if (s != null) {
                numberOfPlotsPerRow = new Integer(s.substring(s.indexOf(": ") + 2));
            }

            s = b.readLine();

            if (s != null) {
                numberOfPlotsPerColumn = new Integer(s.substring(s.indexOf(": ") + 2));
            }

            // get the correlation type selection, requires v1.3.3 or newer
            s = b.readLine();

            if (s != null) {
                useSpearmansCorrelation = new Boolean(s.substring(s.indexOf(": ") + 2));
            }

            b.close();
            f.close();

            // import old user settings
            if (importOldProperties) {
                importUserProperties();
            }

            if (settingsFile != null) {
                JOptionPane.showMessageDialog(null,
                        "The old settings has been successfully imported.\n"
                        + "(Changes to the memory settings requires a restart.)\n\n"
                        + "To import your old data sets simply copy the contents\n"
                        + "of the old DataSets folder.",
                        "Settings Imported", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(null,
                    "Error when reading the user properties. "
                    + "See ../Properties/ErrorLog.txt for more details.",
                    "File Not Found", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error when reading the UserProperties: ");
            ex.printStackTrace();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                    "Error when reading the user properties. "
                    + "See ../Properties/ErrorLog.txt for more details.",
                    "File Error", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error when reading the UserProperties: ");
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            JOptionPane.showMessageDialog(null,
                    "Error when reading the user properties. "
                    + "See ../Properties/ErrorLog.txt for more details.",
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
                oldUserPropertiesLocation.substring(0, oldUserPropertiesLocation.lastIndexOf("/")
                + 1) + "Properties/UserProperties.prop";
        oldUserPropertiesLocation = oldUserPropertiesLocation.replace("%20", " ");

        JFileChooser chooser = new JFileChooser();

        int option = JOptionPane.showConfirmDialog(null,
                "Please locate the old settings file 'UserProperties.prop'.\n"
                + "(It is in the Properties folder of the previous installation.)",
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

                while ((!selectedFile.getName().equalsIgnoreCase("UserProperties.prop")
                        || (new File(oldUserPropertiesLocation).equals(selectedFile))
                        || !firstLine.equalsIgnoreCase("FragmentationAnalyzer"))
                        && !cancel) {

                    if (!selectedFile.getName().equalsIgnoreCase("UserProperties.prop")) {
                        option = JOptionPane.showConfirmDialog(null,
                                "The selected file is not 'UserProperties.prop'.\n"
                                + "Please select the file named 'UserProperties.prop' in the Properties folder.",
                                "Locate Old Settings File", JOptionPane.OK_CANCEL_OPTION);
                    } else if (new File(oldUserPropertiesLocation).equals(selectedFile)) {
                        //trying to upgrade from downloaded UserProperties file
                        option = JOptionPane.showConfirmDialog(null,
                                "It seems like you are trying to upgrade from the wrong UserProperties file.\n"
                                + "Please select the file named 'UserProperties.prop' in the Properties folder \n"
                                + "of the previous installation of FragmentationAnalyzer.",
                                "Wrong UserProperties File", JOptionPane.OK_CANCEL_OPTION);
                    } else {
                        option = JOptionPane.showConfirmDialog(null,
                                "The selected file is not a FragmentationAnalyzer 'UserProperties.prop' file.\n"
                                + "Please select the file named 'UserProperties.prop' in the FragmentationAnalyzer\n"
                                + "Properties folder.",
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

                    // get the old properties folder
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


                    // note: importing data sets is currently disabled
//                    option = JOptionPane.showConfirmDialog(null, "Import data sets as well?",
//                            "Import Data Sets?", JOptionPane.YES_NO_OPTION);
//
//                    if (option == JOptionPane.YES_OPTION) {
//                        importDataSets(propertiesFolder, path);
//                    }

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

                    if (currentDataSet.isDirectory()) {

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

            // fix for when run from inside netbeans
            if (!file.exists()) {
                file = new File("./src/main/resources/PropertiesAndDataSets/Properties/UserProperties.prop");
            }

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
            f.write("APosterioriDetected: " + aPosterioriDetected + "\n");
            f.write("DetectedNotScoring: " + detectedNotScoring + "\n");
            f.write("DectedAndScoring: " + detectedAndScoring + "\n");
            f.write("NormalizeIntensites: " + normalizeIntensites + "\n");
            f.write("NumberOfPlotsPerRow: " + numberOfPlotsPerRow + "\n");
            f.write("NumberOfPlotsPerColumn: " + numberOfPlotsPerColumn + "\n");
            f.write("UseSpearmansCorrelation: " + useSpearmansCorrelation);

            f.close();

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                    "Error saving the user properties.\n"
                    + "See the ErrorLog for more details.",
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
     * Returns the name of server host
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
     * @return true if a posteriori detected fragment ions are to be included in
     * the analysis
     */
    public boolean includeAPosterioriDetected() {
        return aPosterioriDetected;
    }

    /**
     * @param aPosterioriDetected sets if the a posteriori fragment ions are to
     * be included or not
     */
    public void setIncludeAPosterioriDetected(boolean aPosterioriDetected) {
        this.aPosterioriDetected = aPosterioriDetected;
    }

    /**
     * @return the true if the detected yet not scoring fragment ions are to be
     * included in the analysis
     */
    public boolean includeDetectedNotScoring() {
        return detectedNotScoring;
    }

    /**
     * @param detectedNotScoring sets if the a detected yet not scoring fragment
     * ions are to be included or not
     */
    public void setDetectedNotScoring(boolean detectedNotScoring) {
        this.detectedNotScoring = detectedNotScoring;
    }

    /**
     * @return true if the detected and scoring fragment ions are to be included
     * in the analysis
     */
    public boolean includeDetectedAndScoring() {
        return detectedAndScoring;
    }

    /**
     * @param detectedAndScoring sets if the a detected and scoring fragment
     * ions are to be included or not
     */
    public void setDetectedAndScoring(boolean detectedAndScoring) {
        this.detectedAndScoring = detectedAndScoring;
    }

    /**
     * @return the normalizeIntensites
     */
    public boolean normalizeIntensites() {
        return normalizeIntensites;
    }

    /**
     * @param normalizeIntensites the normalizeIntensites to set
     */
    public void setNormalizeIntensites(boolean normalizeIntensites) {
        this.normalizeIntensites = normalizeIntensites;
    }

    /**
     * @return the numberOfPlotsPerRow
     */
    public int getNumberOfPlotsPerRow() {
        return numberOfPlotsPerRow;
    }

    /**
     * @param numberOfPlotsPerRow the numberOfPlotsPerRow to set
     */
    public void setNumberOfPlotsPerRow(int numberOfPlotsPerRow) {
        this.numberOfPlotsPerRow = numberOfPlotsPerRow;
    }

    /**
     * @return the numberOfPlotsPerColumn
     */
    public int getNumberOfPlotsPerColumn() {
        return numberOfPlotsPerColumn;
    }

    /**
     * @param numberOfPlotsPerColumn the numberOfPlotsPerColumn to set
     */
    public void setNumberOfPlotsPerColumn(int numberOfPlotsPerColumn) {
        this.numberOfPlotsPerColumn = numberOfPlotsPerColumn;
    }

    /**
     * @return the userSpearmansCorrelation
     */
    public boolean useSpearmansCorrelation() {
        return useSpearmansCorrelation;
    }

    /**
     * @param userSpearmansCorrelation the userSpearmansCorrelation to set
     */
    public void setUseSpearmansCorrelation(boolean userSpearmansCorrelation) {
        this.useSpearmansCorrelation = userSpearmansCorrelation;
    }

    /**
     * Returns true if the given scoring type is currently selected, false
     * otherwise.
     *
     * @param scoringType the scoring type (0, 1 or 2)
     * @return true if the given scoring type is currently selected, false
     * otherwise
     */
    public boolean isScoringTypeSelected(long scoringType) {

        boolean scoringTypeIsSelected = false;

        if (scoringType == 0) {
            scoringTypeIsSelected = aPosterioriDetected;
        } else if (scoringType == 1) {
            scoringTypeIsSelected = detectedNotScoring;
        } else if (scoringType == 2) {
            scoringTypeIsSelected = detectedAndScoring;
        }

        return scoringTypeIsSelected;
    }
}
