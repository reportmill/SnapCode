package snapcode.project;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.Objects;

/**
 * Utility methods for VersionControl.
 */
public class VersionControlUtils {

    // The path to remote file
    private static final String REMOTE_CONFIG_FILE = "/remote.config";

    /**
     * Returns the remote site URL for a given project site.
     */
    public static WebURL getRemoteSiteUrl(WebSite projectSite)
    {
        String urlAddr = getRemoteSiteUrlAddress(projectSite);
        return WebURL.getUrl(urlAddr);
    }

    /**
     * Sets the remote site URL address string for a given project site.
     */
    public static void setRemoteSiteUrl(WebSite projectSite, WebURL remoteUrl)
    {
        String remoteUrlAddr = remoteUrl != null ? remoteUrl.getString() : null;
        setRemoteSiteUrlAddress(projectSite, remoteUrlAddr);
    }

    /**
     * Returns the remote site URL address string for a given project site.
     */
    public static String getRemoteSiteUrlAddress(WebSite projectSite)
    {
        // Get remote settings file
        WebFile remoteSettingsFile = getRemoteConfigFile(projectSite);
        if (!remoteSettingsFile.getExists())
            return null;

        // Get file text and return
        String remoteSettingsText = remoteSettingsFile.getText();
        String urlAddress = remoteSettingsText.trim();
        return !urlAddress.isEmpty() ? urlAddress : null;
    }

    /**
     * Sets the remote site URL address string for a given project site.
     */
    public static void setRemoteSiteUrlAddress(WebSite projectSite, String remoteUrlAddress)
    {
        // If already set, just return
        if (Objects.equals(remoteUrlAddress, getRemoteSiteUrlAddress(projectSite))) return;

        // Get remote settings file
        WebFile remoteSettingsFile = getRemoteConfigFile(projectSite);

        // Save URL to remote settings file
        try {

            // If empty URL, delete file
            if (remoteUrlAddress == null || remoteUrlAddress.isEmpty()) {
                if (remoteSettingsFile.getExists())
                    remoteSettingsFile.delete();
            }

            // Set file text and save
            else {
                remoteSettingsFile.setText(remoteUrlAddress);
                remoteSettingsFile.save();
            }
        }

        // Rethrow exceptions
        catch (Exception e) { throw new RuntimeException(e); }

        // Clear VersionControl
        projectSite.setMetadataForKey(VersionControl.class.getName(), null);
    }

    /**
     * Returns the remote config file.
     */
    private static WebFile getRemoteConfigFile(WebSite projectSite)
    {
        WebFile sandboxDir = projectSite.getSandboxDir();
        return sandboxDir.createChildFileForPath(REMOTE_CONFIG_FILE, false);
    }
}
