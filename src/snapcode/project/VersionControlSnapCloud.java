package snapcode.project;
import snap.util.Prefs;
import snap.util.UserInfo;
import snap.web.WebSite;
import snap.web.WebURL;

/**
 * This version control class uses drop box.
 */
public class VersionControlSnapCloud extends VersionControl {

    // Whether to autosave
    private boolean _autsave;

    // Whether repo is writeable
    private boolean _writable;

    // The SnapCloud root URL
    public static final String SNAPCLOUD_ROOT = "dbox://dbox.com/";

    /**
     * Constructor.
     */
    public VersionControlSnapCloud(WebSite projectSite, WebURL remoteUrl)
    {
        super(projectSite, remoteUrl);

        // Get whether current user is owner
        _writable = isSnapCloudUrlWritable(remoteUrl);

        // Init Autosave (defaults to true)
        String autoSavePrefKey = "AutoSave_" + getLocalSite().getName();
        _autsave = _writable && Prefs.getDefaultPrefs().getBoolean(autoSavePrefKey, true);
    }

    /**
     * Returns whether files are autosaved to cloud.
     */
    public boolean isAutoSave()  { return _autsave; }

    /**
     * Sets whether files are autosaved to cloud.
     */
    public void setAutoSave(boolean aValue)
    {
        if (aValue == _autsave) return;
        _autsave = aValue;

        // Save key to prefs if false
        String autoSavePrefKey = "AutoSave_" + getLocalSite().getName();
        if (_autsave)
            Prefs.getDefaultPrefs().remove(autoSavePrefKey);
        else Prefs.getDefaultPrefs().setValue(autoSavePrefKey, false);
    }

    /**
     * Returns whether remote site is writeable.
     */
    @Override
    public boolean isRemoteWritable()  { return _writable; }

    /**
     * Override to allow.
     */
    @Override
    public boolean canCreateRemote()  { return _writable; }

    /**
     * Returns whether given URL is SnapCloud.
     */
    public static boolean isSnapCloudUrl(WebURL aURL)
    {
        String scheme = aURL.getScheme();
        return scheme.equals("dbox") || scheme.equals("dropbox");
    }

    /**
     * Returns whether given SnapCloud URL is writeable.
     */
    public static boolean isSnapCloudUrlWritable(WebURL snapCloudUrl)
    {
        WebURL userUrl = getSnapCloudUserUrl();
        if (userUrl == null)
            return false;

        return snapCloudUrl.getPath().startsWith(userUrl.getPath());
    }

    /**
     * Returns the snap cloud URL for account user email.
     */
    public static WebSite getSnapCloudUserSite()
    {
        WebURL snapCloudUserUrl = getSnapCloudUserUrl();
        if (snapCloudUserUrl == null)
            return null;
        return snapCloudUserUrl.getAsSite();
    }

    /**
     * Returns the snap cloud URL for account user email.
     */
    public static WebURL getSnapCloudUserUrl()
    {
        String userEmail = UserInfo.getUserEmail();
        if (userEmail == null || userEmail.isBlank())
            return null;

        int sepIndex = userEmail.indexOf('@');
        if (sepIndex <= 0)
            return null;

        // Get username and domain
        String userName = userEmail.substring(0, sepIndex);
        String domain = userEmail.substring(sepIndex + 1);

        // Return
        String snapCloudUrlAddress = SNAPCLOUD_ROOT + domain + "/" + userName;
        return WebURL.getUrl(snapCloudUrlAddress);
    }
}
