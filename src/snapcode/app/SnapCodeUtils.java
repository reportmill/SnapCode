package snapcode.app;

import snap.util.FileUtils;
import snap.web.WebURL;

import java.io.File;

/**
 * Utilities for SnapCodePro.
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
    public static String getSnapCodeDirPath()
    {
        return getSnapCodeDirURL().getPath();
    }

}