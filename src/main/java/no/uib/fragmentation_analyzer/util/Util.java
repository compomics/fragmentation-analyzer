package no.uib.fragmentation_analyzer.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JOptionPane;

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
}
