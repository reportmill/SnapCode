package snapcode.util;
import snap.gfx.Image;
import snap.view.ViewUtils;
import snap.web.WebFile;

/**
 * Utility class to provide file icons.
 */
public class FileIcons {

    // Image Constants
    private static Image RootFile, DirFile, ClassFile, JavaFile, TableFile, PlainFile;

    /**
     * Returns the image for a file.
     */
    public static Image getFileIconImage(WebFile aFile)
    {
        // If first time, load files
        if (RootFile == null)
            loadFileIconImages();

        // Handle File types
        if (aFile.isRoot()) return RootFile;
        if (aFile.isDir()) return DirFile;
        if (aFile.getType().equals("class")) return ClassFile;
        if (aFile.getType().equals("java") || aFile.getType().equals("jepl")) return JavaFile;
        if (aFile.getType().equals("table")) return TableFile;
        return PlainFile;
    }

    /**
     * Loads the file icon images.
     */
    private static void loadFileIconImages()
    {
        RootFile = Image.getImageForClassResource(ViewUtils.class, "RootFile.png");
        DirFile = Image.getImageForClassResource(ViewUtils.class, "DirFile.png");
        ClassFile = Image.getImageForClassResource(ViewUtils.class, "ClassFile.png");
        JavaFile = Image.getImageForClassResource(ViewUtils.class, "JavaFile.png");
        TableFile = Image.getImageForClassResource(ViewUtils.class, "TableFile.png");
        PlainFile = Image.getImageForClassResource(ViewUtils.class, "PlainFile.png");
    }
}
