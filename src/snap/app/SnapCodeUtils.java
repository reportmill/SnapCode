package snap.app;
import java.io.File;
import snap.util.FileUtils;
import snap.web.WebURL;

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
public static String getSnapCodeDirPath()  { return getSnapCodeDirURL().getPath(); }

}