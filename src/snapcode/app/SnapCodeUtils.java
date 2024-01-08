package snapcode.app;
import snap.util.FileUtils;
import snap.web.WebFile;
import snap.web.WebURL;
import java.io.File;

/**
 * Utilities for SnapCode.
 */
public class SnapCodeUtils {

    /**
     * Returns the SnapCode directory URL.
     */
    public static WebURL getSnapCodeDirURL()
    {
        File file = FileUtils.getUserHomeDir("SnapCode", false);
        return WebURL.getURL(file);
    }

    /**
     * Returns the SnapCode directory path.
     */
    public static WebFile getSnapCodeDir()
    {
        WebURL snapCodeDirUrl = getSnapCodeDirURL();
        assert (snapCodeDirUrl != null);
        return snapCodeDirUrl.createFile(true);
    }

    /**
     * Returns the SnapCode directory path.
     */
    public static WebFile getSnapCodeProjectDirForName(String projectName)
    {
        WebURL snapCodeDirUrl = getSnapCodeDirURL();
        assert (snapCodeDirUrl != null);
        WebURL projectDirUrl = snapCodeDirUrl.getChild(projectName);
        assert (projectDirUrl != null);
        return projectDirUrl.createFile(true);
    }

    /**
     * Returns the SnapCode directory path.
     */
    public static String getSnapCodeDirPath()
    {
        return getSnapCodeDirURL().getPath();
    }

}