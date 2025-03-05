package snapcode.app;
import snap.util.FileUtils;
import snap.util.SnapUtils;
import snap.web.WebFile;
import snap.web.WebSite;
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
     * Returns a SnapCode project site for given path.
     */
    public static WebSite getSnapCodeProjectSiteForNameOrPath(String projectPath)
    {
        // Get parent site
        WebURL parentSiteURL = getSnapCodeDirURL();
        WebSite parentSite = parentSiteURL.getSite();

        // Get URL and site for project path
        WebURL projURL = parentSite.getUrlForPath(projectPath);
        return projURL.getAsSite();
    }

    /**
     * Returns the SnapCode directory path.
     */
    public static String getSnapCodeDirPath()
    {
        return getSnapCodeDirURL().getPath();
    }

    /**
     * Returns a build date string (eg, "Jan-26-03") as generated into BuildInfo.txt at build time.
     */
    public static String getBuildInfo()
    {
        try { return SnapUtils.getText(SnapUtils.class, "/snapcode/BuildInfo.txt").trim(); }
        catch (Exception e) { return "BuildInfo not found"; }
    }
}