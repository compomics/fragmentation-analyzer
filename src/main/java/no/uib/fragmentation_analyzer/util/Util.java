package no.uib.fragmentation_analyzer.util;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import no.uib.fragmentation_analyzer.filefilters.JpegFileFilter;
import no.uib.fragmentation_analyzer.filefilters.PdfFileFilter;
import no.uib.fragmentation_analyzer.filefilters.PngFileFilter;
import no.uib.fragmentation_analyzer.filefilters.SvgFileFilter;
import no.uib.fragmentation_analyzer.filefilters.TiffFileFilter;

/**
 * Includes help methods that are used by the other classes.
 * 
 * @author  Harald Barsnes
 */
public final class Util {

    /**
     * Makes sure that all writing to the ErrorLog has a uniform appearence.
     *
     * Writes the given String to the errorLog. 
     * Adds date and time of entry.
     *
     * @param logEntry
     */
    public static void writeToErrorLog(String logEntry) {
        System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + ": " + logEntry);
    }

    /**
     * Rounds of a double value to the wanted number of decimalplaces
     *
     * @param d the double to round of
     * @param places number of decimal places wanted
     * @return double - the new double
     */
    public static double roundDouble(double d, int places) {
        return Math.round(d * Math.pow(10, (double) places)) / Math.pow(10, (double) places);
    }

    
    /**
     * Deletes all files and subdirectories under dir. Returns true if all deletions were successful.
     * If a deletion fails, the method stops attempting to delete and returns false.
     *
     * @param dir
     * @return rue if all deletions were successful
     */
    public static boolean deleteDir(File dir) {

        boolean success = false;

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        } else {
            // file is NOT a directory
            return false;
        }

        return dir.delete();
    }

    /**
     * Copies the selected file to a new location.
     *
     * @param fromFile the file to copy
     * @param toFile the location of the new file
     */
    public static boolean copyFile(File fromFile, File toFile) {

        boolean error = false;

        try {
            BufferedReader br = new BufferedReader(new FileReader(fromFile));
            BufferedWriter bw = new BufferedWriter(new FileWriter(toFile));

            String s;

            while (br.ready()) {
                s = br.readLine();
                bw.write(s);
                bw.newLine();
            }

            br.close();
            bw.close();

        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(null,
                    "An error occured when trying to copy a file. " +
                    "See ../Properties/ErrorLog.txt for more details.",
                    "Error Copying File", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("DataSource: ");
            ex.printStackTrace();
            error = true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                    "An error occured when trying to copy a file. " +
                    "See ../Properties/ErrorLog.txt for more details.",
                    "Error Copying File", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("DataSource: ");
            ex.printStackTrace();
            error = true;
        }

        return error;
    }

    /**
     * Removes the occurence count from an item in the combobox, e.g., <Mox> (12234) becomes <Mox>.
     *
     * @param item the item to remove the count from
     * @return the item without the occurence count
     */
    public static String removeOccurenceCount(String item) {

        if (item.endsWith(")")) {
            item = item.substring(0, item.lastIndexOf("(") - 1);
        }

        return item;
    }

    /**
     * Returns true if the given identification has the selected charge.
     *
     * @param reducedModIdentification
     * @param charge
     * @return true of the given identification has the selected charge, false otherwise
     */
    public static boolean checkCharge(ReducedIdentification reducedModIdentification, Integer charge) {

        boolean identificationMatch;

        if (reducedModIdentification.getCharge().intValue() == charge.intValue()) {
            identificationMatch = true;
            //System.out.println("charge match");
        } else {
            identificationMatch = false;
            //System.out.println("charge does not match!");
        }

        return identificationMatch;
    }

    /**
     * Returns true if the given identification has been identified using one if the provided
     * instruments.
     *
     * @param reducedModIdentification
     * @param instrument1
     * @param instrument2
     * @param instrument3
     * @return true of the given identification has been identified using one if the provided
     *         instruments, false otherwise
     */
    public static boolean checkInstrument(ReducedIdentification reducedModIdentification, String instrument1,
            String instrument2, String instrument3) {

        boolean identificationMatch;

        if (instrument1.equalsIgnoreCase("Select All")) {
            identificationMatch = true;
        } else {
            if (reducedModIdentification.getInstrumentName().equalsIgnoreCase(instrument1) ||
                    reducedModIdentification.getInstrumentName().equalsIgnoreCase(instrument2) ||
                    reducedModIdentification.getInstrumentName().equalsIgnoreCase(instrument3)) {
                //System.out.println("instrument match");
                identificationMatch = true;
            } else {
                identificationMatch = false;
                //System.out.println("instrument does not match!");
            }
        }

        return identificationMatch;
    }

    /**
     * Returns true if the given identification has the selected terminals.
     *
     * @param reducedModIdentification
     * @param nTerminal
     * @param cTerminal
     * @return true of the given identification has the selected terminals, false otherwise
     */
    public static boolean checkTerminals(ReducedIdentification reducedModIdentification, String nTerminal, String cTerminal) {

        boolean identificationMatch;

        boolean nTermSelectAll = false;
        boolean cTermSelectAll = false;

        if (nTerminal.equalsIgnoreCase("Select All")) { // use all n terminals
            nTermSelectAll = true;
        }

        if (cTerminal.equalsIgnoreCase("Select All")) { // use all c terminals
            cTermSelectAll = true;
        }

        if ((reducedModIdentification.getNTerminal().equalsIgnoreCase(nTerminal) || nTermSelectAll) &&
                (reducedModIdentification.getCTerminal().equalsIgnoreCase(cTerminal) || cTermSelectAll)) {
            identificationMatch = true;
            //System.out.println("terminals match");
        } else {
            identificationMatch = false;
            //System.out.println("terminals does not match!");
        }

        return identificationMatch;
    }

    /**
     * Returns true if the given identification contains the selected modifications.
     *
     * @param reducedIdentification
     * @param modification1
     * @param modification2
     * @param modification3
     * @param pattern
     * @param oneModificationOnly
     * @return true if the given identification contains the selected modifications, false otherwise
     */
    public static boolean checkModifications(ReducedIdentification reducedIdentification,
            String modification1, String modification2, String modification3, boolean oneModificationOnly,
            Pattern pattern) {

        boolean identificationMatch = true;

        if (modification1.equalsIgnoreCase(" - Select - ")) { // no modifications selected
            identificationMatch = true;
            //System.out.println("mods match");
        } else {
            ArrayList<String> tempMods = reducedIdentification.getInternalModifications(pattern);

            if (oneModificationOnly) {
                identificationMatch = (tempMods.size() == 1);
            }

            if (identificationMatch &&
                    tempMods.contains(modification1) ||
                    tempMods.contains(modification2) ||
                    tempMods.contains(modification3)) {
                identificationMatch = true;
                //System.out.println("mods match");
            } else {
                identificationMatch = false;
                //System.out.println("mods does not match!");
            }
        }

        return identificationMatch;
    }

    /**
     * Returns the ppm value of the given mass error relative to its
     * theoretical m/z value.
     *
     * @param theoreticalMzValue the theoretical mass
     * @param massError the mass error
     * @return the mass error as a ppm value relative to the theoretical mass
     */
    public static double getPpmError(double theoreticalMzValue, double massError) {
        double ppmValue = (massError / theoreticalMzValue) * 1000000;
        return ppmValue;
    }

    /**
     * Returns the total number of data points in the given data set.
     *
     * @param data
     * @return the total number of data points in the given data set
     */
    public static int getTotalFragmentIonCount(HashMap<String, ArrayList<XYZDataPoint>> data) {

        int count = 0;

        Iterator<String> iterator = data.keySet().iterator();

        while (iterator.hasNext()) {
            count += data.get(iterator.next()).size();
        }

        return count;
    }

    /**
     * Parse the selected PKL file.
     *
     * @param aPklFile the file to parse
     * @return the parsed PKL file as an PKLFile object
     * @throws IOException
     */
    public static PKLFile parsePKLFile(File aPklFile) throws IOException {
        PKLFile pklFile = new PKLFile(aPklFile);
        return pklFile;
    }

    /**
     * Returns the peak color to be used for the given peak label. The
     * colors used are based on the color coding used in MascotDatfile.
     *
     * @param peakLabel
     * @return the peak color
     */
    public static Color determineColorOfPeak(String peakLabel) {

        Color currentColor = Color.GRAY;

        if (peakLabel.startsWith("a")) {

            // turquoise
            currentColor = new Color(153, 0, 0);

            if (peakLabel.lastIndexOf("H2O") != -1 || peakLabel.lastIndexOf("H20") != -1) {
                // light purple-blue
                currentColor = new Color(171, 161, 255);
            } else if (peakLabel.lastIndexOf("NH3") != -1) {
                // ugly purple pink
                currentColor = new Color(248, 151, 202);
            }

        } else if (peakLabel.startsWith("b")) {

            // dark blue
            currentColor = new Color(0, 0, 255);

            if (peakLabel.lastIndexOf("H2O") != -1 || peakLabel.lastIndexOf("H20") != -1) {
                // nice blue
                currentColor = new Color(0, 125, 200);
            } else if (peakLabel.lastIndexOf("NH3") != -1) {
                // another purple
                currentColor = new Color(153, 0, 255);
            }

        } else if (peakLabel.startsWith("c")) {

            // purple blue
            currentColor = new Color(188, 0, 255); // ToDo: no colors for H2O and NH3??

        } else if (peakLabel.startsWith("x")) {

            // green
            currentColor = new Color(78, 200, 0); // ToDo: no colors for H2O and NH3??

        } else if (peakLabel.startsWith("y")) {

            // black
            currentColor = new Color(0, 0, 0);

            if (peakLabel.lastIndexOf("H2O") != -1 || peakLabel.lastIndexOf("H20") != -1) {
                // navy blue
                currentColor = new Color(0, 70, 135);
            } else if (peakLabel.lastIndexOf("NH3") != -1) {
                // another purple
                currentColor = new Color(155, 0, 155);
            }

        } else if (peakLabel.startsWith("z")) {

            // dark green
            currentColor = new Color(64, 179, 0); // ToDo: no colors for H2O and NH3??

        } else if (peakLabel.startsWith("Prec")) { // precursor

            // red
            currentColor = Color.gray; // Color.red is used in MascotDatFile

        } else if (peakLabel.startsWith("i")) { // immonimum ion
            // grey
            currentColor = Color.gray;
        }

        return currentColor;
    }

    /**
     * Returns the color to use for the given fragment ion label.
     *
     * @param seriesLabel the series label
     * @return the fragment ion color
     */
    public static Color determineFragmentIonColor(String seriesLabel) {

        Color currentColor = Color.GRAY;

        if (seriesLabel.startsWith("a")) {

            // turquoise
            currentColor = new Color(153, 0, 0);

            if (seriesLabel.lastIndexOf("H2O") != -1 || seriesLabel.lastIndexOf("H20") != -1) {
                // light purple-blue
                currentColor = new Color(171, 161, 255);
            } else if (seriesLabel.lastIndexOf("NH3") != -1) {
                // ugly purple pink
                currentColor = new Color(248, 151, 202);
            }

            // change color slightly if a double charge is detected
            if(seriesLabel.lastIndexOf("++") != -1){
                currentColor = new Color(currentColor.getRed() - 100, currentColor.getGreen(), currentColor.getBlue());
            }

        } else if (seriesLabel.startsWith("b")) {

            // dark blue
            currentColor = new Color(0, 0, 255);

            // change color slightly if a neutral loss is detected
            if (seriesLabel.lastIndexOf("H2O") != -1 || seriesLabel.lastIndexOf("H20") != -1) {
                currentColor = new Color(0, 150, 255);
            } else if (seriesLabel.lastIndexOf("NH3") != -1 || seriesLabel.equalsIgnoreCase("b ions - mod.")) {
                currentColor = new Color(150, 0, 255);
            }

            // change color slightly if a double charge is detected
            if(seriesLabel.lastIndexOf("++") != -1){
                currentColor = new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue() - 100);
            }

        } else if (seriesLabel.startsWith("c")) {

            // purple blue
            currentColor = new Color(188, 0, 255);

            // change color slightly if a neutral loss is detected
            if (seriesLabel.lastIndexOf("H2O") != -1 || seriesLabel.lastIndexOf("H20") != -1) {
                currentColor = new Color(188, 150, 255);
            } else if (seriesLabel.lastIndexOf("NH3") != -1) {
                currentColor = new Color(255, 0, 255);
            }

            // change color slightly if a double charge is detected
            if(seriesLabel.lastIndexOf("++") != -1){
                currentColor = new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue() - 100);
            }

        } else if (seriesLabel.startsWith("x")) {

            // green
            currentColor = new Color(78, 200, 0);

            // change color slightly if a neutral loss is detected
            if (seriesLabel.lastIndexOf("H2O") != -1 || seriesLabel.lastIndexOf("H20") != -1) {
                currentColor = new Color(78, 200, 150);
            } else if (seriesLabel.lastIndexOf("NH3") != -1) {
                currentColor = new Color(255, 200, 255);
            }

            // change color slightly if a double charge is detected
            if(seriesLabel.lastIndexOf("++") != -1){
                currentColor = new Color(currentColor.getRed(), currentColor.getGreen() - 100, currentColor.getBlue());
            }

        } else if (seriesLabel.startsWith("y")) {

            // red
            currentColor = new Color(255, 0, 0);

             // change color slightly if a neutral loss is detected
            if (seriesLabel.lastIndexOf("H2O") != -1 || seriesLabel.lastIndexOf("H20") != -1) {
                currentColor = new Color(255, 150, 0);
            } else if (seriesLabel.lastIndexOf("NH3") != -1 || seriesLabel.equalsIgnoreCase("y ions - mod.")) {
                currentColor = new Color(255, 0, 150);
            }

            // change color slightly if a double charge is detected
            if(seriesLabel.lastIndexOf("++") != -1){
                currentColor = new Color(currentColor.getRed() - 100, currentColor.getGreen(), currentColor.getBlue());
            }

        } else if (seriesLabel.startsWith("z")) {

            // dark green
            currentColor = new Color(64, 179, 0);

             // change color slightly if a neutral loss is detected
            if (seriesLabel.lastIndexOf("H2O") != -1 || seriesLabel.lastIndexOf("H20") != -1) {
                currentColor = new Color(64, 179, 150);
            } else if (seriesLabel.lastIndexOf("NH3") != -1) {
                currentColor = new Color(255, 179, 150);
            }

            // change color slightly if a double charge is detected
            if(seriesLabel.lastIndexOf("++") != -1){
                currentColor = new Color(currentColor.getRed(), currentColor.getGreen() - 100, currentColor.getBlue());
            }

        }

        return currentColor;
    }

    /**
     * Extract the unmodified sequence from the modified sequence. E.g. 'ARMR' from 'NH2-ARTM<Mox>R-COOH'.
     *
     * @param modifiedSequence the modified sequence
     * @param extractSequenceProperties if true the terminals and modifications are stored
     * @param combineFixedAndVariableMods if true the fixed and variable modifications are combined into one modification
     * @return the extracted unmodified sequence
     */
    public static String extractUnmodifiedSequenceAndModifications(
            String modifiedSequence, boolean extractSequenceProperties, boolean combineFixedAndVariableMods,
            Properties properties) {

        // colapses fixed and variable modificattions into one modification
        // For example, <Mox> and <Mox*> becomes <Mox>

        String unmodifiedSequence = modifiedSequence;
        String currentModification;

        // n-term modification
        if (unmodifiedSequence.startsWith("#")) {
            currentModification = unmodifiedSequence.substring(0, unmodifiedSequence.indexOf("#", 1) + 2);
            unmodifiedSequence = unmodifiedSequence.substring(unmodifiedSequence.indexOf("#", 1) + 2);
        } else {
            currentModification = unmodifiedSequence.substring(0, unmodifiedSequence.indexOf("-") + 1);
            unmodifiedSequence = unmodifiedSequence.substring(unmodifiedSequence.indexOf("-") + 1);
        }

        if (extractSequenceProperties) {

            if (currentModification.endsWith("*-") && combineFixedAndVariableMods) {
                currentModification = currentModification.substring(0, currentModification.length() - 2) + "-";
            }

            if (!properties.getExtractedNTermModifications().containsKey(currentModification)) {
                properties.getExtractedNTermModifications().put(currentModification, 1);
            } else {
                properties.getExtractedNTermModifications().put(currentModification,
                        properties.getExtractedNTermModifications().get(currentModification) + 1);
            }
        }

        // c-term modification
        if (unmodifiedSequence.endsWith("#")) {
            String temp = unmodifiedSequence.substring(0, unmodifiedSequence.length() - 1);
            currentModification = unmodifiedSequence.substring(temp.lastIndexOf("#") - 1);
            unmodifiedSequence = unmodifiedSequence.substring(0, temp.lastIndexOf("#") - 1);
        } else {
            currentModification = unmodifiedSequence.substring(unmodifiedSequence.lastIndexOf("-"));
            unmodifiedSequence = unmodifiedSequence.substring(0, unmodifiedSequence.lastIndexOf("-"));
        }

        if (extractSequenceProperties) {

            if (currentModification.endsWith("*") && combineFixedAndVariableMods) {
                currentModification = currentModification.substring(0, currentModification.length() - 1);
            }

            if (!properties.getExtractedCTermModifications().containsKey(currentModification)) {
                properties.getExtractedCTermModifications().put(currentModification, 1);
            } else {
                properties.getExtractedCTermModifications().put(currentModification,
                        properties.getExtractedCTermModifications().get(currentModification) + 1);
            }
        }


        // internal modification

        Matcher matcher = properties.getPattern().matcher(unmodifiedSequence);

        while (matcher.find()) {

            currentModification = matcher.group();

            unmodifiedSequence =
                    unmodifiedSequence.substring(0, matcher.start()) +
                    unmodifiedSequence.substring(matcher.end());

            matcher = properties.getPattern().matcher(unmodifiedSequence);

            //remove '<' and '>'
//                currentModification =
//                        currentModification.substring(1,
//                        currentModification.length() - 1);

            if (extractSequenceProperties) {

                if (currentModification.endsWith("*>") && combineFixedAndVariableMods) {
                    currentModification = currentModification.substring(0, currentModification.length() - 2) + ">";
                }

                if (!properties.getExtractedInternalModifications().containsKey(currentModification)) {
                    properties.getExtractedInternalModifications().put(currentModification, 1);
                } else {
                    properties.getExtractedInternalModifications().put(currentModification,
                            properties.getExtractedInternalModifications().get(currentModification) + 1);
                }
            }
        }

        return unmodifiedSequence;
    }

    /**
     * Update the list of currently used charges.
     *
     * @param charge current charge
     */
    public static void storeCharge(String charge, Properties properties) {
        if (!properties.getExtractedCharges().containsKey(charge)) {
            properties.getExtractedCharges().put(charge, 1);
        } else {
            properties.getExtractedCharges().put(charge, properties.getExtractedCharges().get(charge).intValue() + 1);
        }
    }

    /**
     * Update the list of currently used instruments.
     *
     * @param instrument current instrument
     */
    public static void storeInstrument(String instrument, Properties properties) {
        if (!properties.getExtractedInstruments().containsKey(instrument)) {
            properties.getExtractedInstruments().put(instrument, 1);
        } else {
            properties.getExtractedInstruments().put(instrument,
                    properties.getExtractedInstruments().get(instrument).intValue() + 1);
        }
    }

    /**
     * Returns true if all selected rows in the spectra table have the same modified sequence, false otherwise.
     *
     * @return true if all selected rows in the spectra table have the same modified sequence, false otherwise
     */
    public static boolean verifyEqualModifiedSeqences(boolean displayMessage, Properties properties) {

        // verify that all selected rows have the same modified sequence
        String currentModifiedSequence = properties.getCurrentlySelectedRowsInSpectraTable().get(0).getModifiedSequence();

        boolean sameSequence = true;

        for (int i = 1; i < properties.getCurrentlySelectedRowsInSpectraTable().size() && sameSequence; i++) {
            String tempModifiedSequence =
                    properties.getCurrentlySelectedRowsInSpectraTable().get(i).getModifiedSequence();

            if (!currentModifiedSequence.equalsIgnoreCase(tempModifiedSequence)) {
                sameSequence = false;
            }
        }

        if (!sameSequence) {
            if (displayMessage) {
                JOptionPane.showMessageDialog(null,
                        "For this analysis type all selected sequences must be equal.",
                        "Sequences Differ", JOptionPane.INFORMATION_MESSAGE);
            }
        }

        return sameSequence;
    }

    /**
     * Sets the image type file filter when exporting plots.
     *
     * @param imageType
     */
    public static void setFileFilter(JFileChooser chooser, ImageType imageType){

        if(imageType == ImageType.SVG){
            chooser.setFileFilter(new SvgFileFilter());
        } else if(imageType == ImageType.PDF){
            chooser.setFileFilter(new PdfFileFilter());
        } else if(imageType == ImageType.JPEG){
            chooser.setFileFilter(new JpegFileFilter());
        } else if(imageType == ImageType.PNG){
            chooser.setFileFilter(new PngFileFilter());
        } else if(imageType == ImageType.TIFF){
            chooser.setFileFilter(new TiffFileFilter());
        }
    }

    /**
     * Check if a newer version of FragmentationAnalyzer is available.
     * @param properties
     * @param debug 
     */
    public static void checkForNewVersion(Properties properties, boolean debug) {

        try {
            boolean deprecatedOrDeleted = false;

            URL downloadPage = new URL(
                    "http://code.google.com/p/fragmentation-analyzer/downloads/detail?name=FragmentationAnalyzer-" +
                    properties.getVersion() + ".zip");
            int respons = ((java.net.HttpURLConnection) downloadPage.openConnection()).getResponseCode();

            // 404 means that the file no longer exists, which means that
            // the running version is no longer available for download,
            // which again means that a never version is available.
            if (respons == 404) {
                deprecatedOrDeleted = true;
            } else {

                // also need to check if the available running version has been
                // deprecated (but not deleted)
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(downloadPage.openStream()));

                String inputLine;

                while ((inputLine = in.readLine()) != null && !deprecatedOrDeleted) {
                    if (inputLine.lastIndexOf("Deprecated") != -1 &&
                            inputLine.lastIndexOf("Deprecated Downloads") == -1 &&
                            inputLine.lastIndexOf("Deprecated downloads") == -1) {
                        deprecatedOrDeleted = true;
                    }
                }

                in.close();
            }

            // informs the user about an updated version of the converter, unless the user
            // is running a beta version
            if (deprecatedOrDeleted && properties.getVersion().lastIndexOf("beta") == -1) {
                int option = JOptionPane.showConfirmDialog(null,
                        "A newer version of FragmentationAnalyzer is available.\n" +
                        "Do you want to upgrade?",
                        "Upgrade Available",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    BareBonesBrowserLaunch.openURL("http://fragmentation-analyzer.googlecode.com/");
                    System.exit(0);
                } else if (option == JOptionPane.CANCEL_OPTION) {
                    System.exit(0);
                }
            }
        } catch (MalformedURLException e) {
            Util.writeToErrorLog("FragmentationAnalyzer: Error when trying to look for update: " + e.toString());
            if (debug) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            Util.writeToErrorLog("FragmentationAnalyzer: Error when trying to look for update: " + e.toString());
            if (debug) {
                e.printStackTrace();
            }
        }
    }
}
