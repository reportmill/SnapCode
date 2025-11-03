package snapcode.app;
import snap.util.FileUtils;
import snap.util.SnapUtils;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.project.VersionControlUtils;
import java.io.File;
import java.util.List;

/**
 * Utilities for SnapCode.
 */
public class SnapCodeUtils {

    // Build info, build version
    private static String _buildInfo, _buildVersion;

    /**
     * Returns the SnapCode directory URL.
     */
    public static WebURL getSnapCodeDirURL()
    {
        File file = FileUtils.getUserHomeDir("SnapCode", false);
        return WebURL.getUrl(file);
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
        WebURL projectDirUrl = snapCodeDirUrl.getChildUrlForPath(projectName);
        assert (projectDirUrl != null);
        return projectDirUrl.createFile(true);
    }

    /**
     * Returns a SnapCode project site for given name.
     */
    public static WebSite getSnapCodeProjectSiteForName(String projectName)
    {
        WebURL snapCodeDirURL = getSnapCodeDirURL();
        WebURL projectUrl = snapCodeDirURL.getChildUrlForPath(projectName);
        return projectUrl.getAsSite();
    }

    /**
     * Returns the source for a given URL.
     */
    public static String getProjectSourceAddressForUrl(WebURL projectUrl)
    {
        // Get remote
        WebSite projectSite = SnapCodeUtils.getSnapCodeProjectSiteForName(projectUrl.getFilenameSimple());
        String projectSourceAddr = VersionControlUtils.getRemoteSiteUrlAddress(projectSite);

        // If no remote address, return "Local" or string
        if (projectSourceAddr == null) {
            if (projectUrl.getScheme().equals("file"))
                return "Local";
            return projectUrl.getString();
        }

        // Handle sample
        if (projectSourceAddr.contains("SnapCode/Samples"))
            return "Sample:" + projectUrl.getFilename();

        // Handle SnapCloud
        if (projectSourceAddr.contains("dbox://"))
            return "SnapCloud";

        // Handle anything else
        return projectSourceAddr;
    }

    /**
     * Returns a build date string (eg, "Jan-26-03") as generated into BuildInfo.txt at build time.
     */
    public static String getBuildInfo()
    {
        if (_buildInfo != null) return _buildInfo;
        try { return _buildInfo = SnapUtils.getText(SnapUtils.class, "/snapcode/BuildInfo.txt").trim(); }
        catch (Exception e) { return "Jan-01-25 12:01 not found"; }
    }

    /**
     * Returns a build version string in format YYYY_MM (eg, "2025.08").
     */
    public static String getBuildVersion()
    {
        if (_buildVersion != null) return _buildVersion;
        String buildInfo = getBuildInfo();
        String buildMonth = buildInfo.substring(0, 3);
        int buildMonthNum = List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec").indexOf(buildMonth) + 1;
        String buildYear = "20" + buildInfo.substring(7, 9);
        return _buildVersion = String.format("%s.%02d", buildYear, buildMonthNum);
    }
}