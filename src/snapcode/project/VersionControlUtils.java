package snapcode.project;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.Objects;

/**
 * Utility methods for VersionControl.
 */
public class VersionControlUtils {

    /**
     * Returns the remote site URL for a given project site.
     */
    public static WebURL getRemoteSiteUrl(WebSite projectSite)
    {
        String urlAddr = getRemoteSiteUrlAddress(projectSite);
        return WebURL.getURL(urlAddr);
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
        WebSite sandboxSite = projectSite.getSandboxSite();
        WebFile remoteSettingsFile = sandboxSite.getFileForPath("/settings/remote");
        if (remoteSettingsFile == null)
            return null;

        // Get file text and return
        String remoteSettingsText = remoteSettingsFile.getText();
        String urlAddress = remoteSettingsText.trim();
        return urlAddress.length() > 0 ? urlAddress : null;
    }

    /**
     * Sets the remote site URL address string for a given project site.
     */
    public static void setRemoteSiteUrlAddress(WebSite projectSite, String remoteUrlAddress)
    {
        // If already set, just return
        if (Objects.equals(remoteUrlAddress, getRemoteSiteUrlAddress(projectSite))) return;

        // Get remote settings file
        WebSite sandboxSite = projectSite.getSandboxSite();
        WebFile file = sandboxSite.getFileForPath("/settings/remote");
        if (file == null)
            file = sandboxSite.createFileForPath("/settings/remote", false);

        // Save URL to remote settings file
        try {

            // If empty URL, delete file
            if (remoteUrlAddress == null || remoteUrlAddress.length() == 0) {
                if (file.getExists())
                    file.delete();
            }

            // Set file text and save
            else {
                file.setText(remoteUrlAddress);
                file.save();
            }
        }

        // Rethrow exceptions
        catch (Exception e) { throw new RuntimeException(e); }

        // Set VersionControl
        projectSite.setProp(VersionControl.class.getName(), null);
    }
}
