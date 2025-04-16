package snapcode.util;
import snap.gfx.Image;
import snap.web.WebFile;

/**
 * Utility class to provide file icons.
 */
public class FileIcons {

    // Image Constants
    private static Image RootFile, DirFile, ClassFile, JavaFile, TableFile, PlainFile;

    /**
     * Returns the Dir image.
     */
    public static Image getDirImage()
    {
        if (DirFile == null) loadFileIconImages();
        return DirFile;
    }

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
        if (aFile.getFileType().equals("class")) return ClassFile;
        if (aFile.getFileType().equals("java") || aFile.getFileType().equals("jepl")) return JavaFile;
        if (aFile.getFileType().equals("table")) return TableFile;
        return PlainFile;
    }

    /**
     * Loads the file icon images.
     */
    private static void loadFileIconImages()
    {
        RootFile = Image.getImageForClassResource(FileIcons.class, "RootFile.png");
        DirFile = Image.getImageForClassResource(FileIcons.class, "DirFile.png");
        ClassFile = Image.getImageForClassResource(FileIcons.class, "ClassFile.png");
        JavaFile = Image.getImageForClassResource(FileIcons.class, "JavaFile.png");
        TableFile = Image.getImageForClassResource(FileIcons.class, "TableFile.png");
        PlainFile = Image.getImageForClassResource(FileIcons.class, "PlainFile.png");
    }
}
