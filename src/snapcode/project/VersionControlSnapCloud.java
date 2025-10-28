package snapcode.project;
import snap.web.WebSite;
import snap.web.WebURL;

/**
 * This version control class uses drop box.
 */
public class VersionControlSnapCloud extends VersionControl {

    /**
     * Constructor.
     */
    public VersionControlSnapCloud(WebSite projectSite, WebURL remoteUrl)
    {
        super(projectSite, remoteUrl);
    }

    /**
     * Override to allow.
     */
    @Override
    public boolean canCreateRemote()  { return true; }

    /**
     * Returns whether given URL is SnapCloud.
     */
    public static boolean isSnapCloudUrl(WebURL aURL)
    {
        String scheme = aURL.getScheme();
        return scheme.equals("dbox") || scheme.equals("dropbox");
    }
}
