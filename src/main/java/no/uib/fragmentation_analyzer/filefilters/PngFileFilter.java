package no.uib.fragmentation_analyzer.filefilters;

import java.io.File;
import javax.swing.filechooser.*;

/**
 * File filter for *.png files.
 *
 * @author  Harald Barsnes
 */
public class PngFileFilter extends FileFilter {
    
    /**
     * Accept all directories, *.png files.
     *
     * @param f
     * @return boolean
     */
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        
        String extension = FileFilterUtils.getExtension(f);
        if (extension != null) {
            if (extension.equals(FileFilterUtils.png)
                    || extension.equals(FileFilterUtils.PNG)){
                return true;
            } 
            else {
                return false;
            }
        }        
        return false;
    }
    
    /**
     * The description of this filter
     *
     * @return String
     */
    public java.lang.String getDescription() {
        return "*.png";
    }
}