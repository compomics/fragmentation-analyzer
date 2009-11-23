package no.uib.fragmentation_analyzer.util;

/**
 * Enumerator for supported export plot file types.
 *
 * @author Harald Barsnes
 */
public enum ImageType {

    JPEG(".jpg"), TIFF(".tiff"), PNG(".png"), PDF(".pdf"), SVG(".svg");
    public String extension;

    ImageType(String extension) {
        this.extension = extension;
    }

    /**
     * Returns the extension.
     *
     * @return the extension
     */
    public String getExtension() {
        return this.extension;
    }
}

