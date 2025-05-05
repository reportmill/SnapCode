package snapcode.project;
import snap.web.WebSite;
import snap.web.WebURL;

/**
 * This version control class uses drop box.
 */
public class VersionControlDropbox extends VersionControl {

    // Constants
    private static final String USER_EMAIL_KEY = "SnapUserEmail";

    /**
     * Constructor.
     */
    public VersionControlDropbox(WebSite projectSite, WebURL remoteUrl)
    {
        super(projectSite, remoteUrl);
    }

    /**
     * Override to allow.
     */
    @Override
    public boolean canCreateRemote()  { return true; }
}
