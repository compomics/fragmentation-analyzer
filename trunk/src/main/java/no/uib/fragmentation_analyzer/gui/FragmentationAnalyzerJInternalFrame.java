package no.uib.fragmentation_analyzer.gui;

import javax.swing.JInternalFrame;

/**
 * An extension of a JInternalFrame that adds two parameters: frame type and frame id. 
 * These can be used to identify a specific frame and its type, e.g., spectrum panel,
 * box plot etc.
 *
 * @author Harald Barsnes
 */
public class FragmentationAnalyzerJInternalFrame extends JInternalFrame {

    /**
     * Frame type e.g., spectrum panel, box plot, etc.
     */
    private String internalFrameType;
    /**
     * Unique frame id. (Note that the uniqueness is not checked on input.)
     */
    private int uniqueId;

    /**
     * Creates a new FragmentationAnalyzerJInternalFrame.
     *
     * @param title Frame title
     * @param resizable
     * @param closable
     * @param maximizable
     * @param internalFrameType Frame type e.g., spectrum panel, box plot, etc.
     * @param uniqueId Unique frame id
     */
    public FragmentationAnalyzerJInternalFrame(String title, boolean resizable, boolean closable,
            boolean maximizable, String internalFrameType, int uniqueId) {
        super(title, resizable, closable, maximizable);

        this.internalFrameType = internalFrameType;
        this.uniqueId = uniqueId;
    }

    /**
     * @return the internalFrameType
     */
    public String getInternalFrameType() {
        return internalFrameType;
    }

    /**
     * @param internalFrameType the internalFrameType to set
     */
    public void setInternalFrameType(String internalFrameType) {
        this.internalFrameType = internalFrameType;
    }

    /**
     * @return the uniqueId
     */
    public int getUniqueId() {
        return uniqueId;
    }

    /**
     * @param uniqueId the uniqueId to set
     */
    public void setUniqueId(int uniqueId) {
        this.uniqueId = uniqueId;
    }
}
