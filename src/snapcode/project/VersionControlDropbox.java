package snapcode.project;
import snap.util.Prefs;
import snap.web.DropBoxSite;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.util.DropboxSite;

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
     * Override to return project site in remote ZipFile.
     */
    @Override
    protected WebSite getRemoteSiteImpl()
    {
//        String userEmail = Prefs.getDefaultPrefs().getString(USER_EMAIL_KEY);
//        if (userEmail == null || userEmail.isEmpty())
//            return null;

//        WebSite dropboxSite = DropBoxSite.getSiteForEmail(userEmail);
//        WebURL remoteProjectUrl = dropboxSite.getUrlForPath('/' + getLocalSite().getName());
//        return remoteProjectUrl.getAsSite();

        WebURL remoteUrl = getRemoteSiteUrl();
        return DropBoxSite.getSiteForUrl(remoteUrl);
    }
}
