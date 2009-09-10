package no.uib.fragmentation_analyzer;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.SkyKrupp;
import java.io.*;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * A wrapper class used to start the jar file with parameters. The parameters 
 * are read from the JavaOptions file in the Properties folder.
 * 
 * @author  Harald Barsnes
 */
public class FragmentationAnalyzerWrapper {

    /**
     * If set to true debug output will be written to the screen.
     */
    private boolean debug = false;
    /**
     * The name of the omssa parser jar file. Must be equal to the name 
     * given in the pom file.
     */
    private String jarFileName = "FragmentationAnalyzer-0.1.jar";

    /**
     * Starts the launcher by calling the launch method. Use this as the 
     * main class in the jar file.
     */
    public FragmentationAnalyzerWrapper() {
        try {
            PlasticLookAndFeel.setPlasticTheme(new SkyKrupp());
            UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            // ignore exception
        }

        try {
            launch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Launches the jar file with parameters to the jvm.
     * 
     * @throws java.lang.Exception
     */
    private void launch() throws Exception {

        String temp = "", cmdLine, path;

        path = this.getClass().getResource("FragmentationAnalyzerWrapper.class").getPath();
        path = path.substring(5, path.indexOf(jarFileName));
        path = path.replace("%20", " ");

        File javaOptions = new File(path + "Properties/JavaOptions.txt");

        String options = "", currentOption;

        if (javaOptions.exists()) {

            try {
                FileReader f = new FileReader(javaOptions);
                BufferedReader b = new BufferedReader(f);

                currentOption = b.readLine();

                while (currentOption != null) {
                    if (!currentOption.startsWith("#")) {
                        options += currentOption + " ";
                    }
                    currentOption = b.readLine();
                }

                b.close();
                f.close();

            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            options = "-Xms128M -Xmx768M";
        }

        File tempFile = new File(path);

        String javaHome = System.getProperty("java.home") + File.separator +
                "bin" + File.separator;

        cmdLine = javaHome + "java " + options + " -cp "
                + new File(tempFile, jarFileName).getAbsolutePath()
                + " no.uib.fragmentation_analyzer.gui.FragmentationAnalyzer";

        if (debug) {
            System.out.println(cmdLine);
        }

        try {
            Process p = Runtime.getRuntime().exec(cmdLine);

            InputStream stderr = p.getErrorStream();
            InputStreamReader isr = new InputStreamReader(stderr);
            BufferedReader br = new BufferedReader(isr);
            String line = null;

            temp += "<ERROR>\n\n";

            if (debug) {
                System.out.println("<ERROR>");
            }

            line = br.readLine();

            boolean error = false;

            while (line != null) {

                if (debug) {
                    System.out.println(line);
                }

                temp += line + "\n";
                line = br.readLine();
                error = true;
            }

            if (debug) {
                System.out.println("</ERROR>");
            }

            temp += "\nThe command line executed:\n";
            temp += cmdLine + "\n";
            temp += "\n</ERROR>\n";
            int exitVal = p.waitFor();

            if (debug) {
                System.out.println("Process exitValue: " + exitVal);
            }

            if (error) {

                javax.swing.JOptionPane.showMessageDialog(null,
                        "Failed to start FragmentationAnalyzer.\n\n" +
                        "Make sure that FragmentationAnalyzer is installed in a path not containing\n" +
                        "special characters. On Linux it has to be run from a path without spaces.\n\n" +
                        "The upper memory limit used may be too high for your computer to handle.\n" +
                        "Try reducing it and see if this helps.\n\n" +
                        "For more details see:\n" +
                        System.getProperty("user.home") +
                        File.separator + "fragmentation_analyzer.log\n\n",
                        //+ "Or see \'Troubleshooting\' at http://code.google.com/p/omssa-parser",
                        "FragmentationAnalyzer - Startup Failed", JOptionPane.OK_OPTION);

                File logFile = new File(System.getProperty("user.home") +
                        File.separator + "fragmentation_analyzer.log");

                FileWriter f = new FileWriter(logFile);
                f.write(temp);
                f.close();

                System.exit(0);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the
     * main class in the jar file.
     *
     * @param args
     */
    public static void main(String[] args) {
        new FragmentationAnalyzerWrapper();
    }
}
