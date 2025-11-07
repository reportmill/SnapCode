package snapcode.project;
import snap.util.Prefs;
import snap.web.WebSite;
import snap.web.WebURL;

/**
 * This version control class uses drop box.
 */
public class VersionControlSnapCloud extends VersionControl {

    // Whether to autosave
    private boolean _autsave;

    /**
     * Constructor.
     */
    public VersionControlSnapCloud(WebSite projectSite, WebURL remoteUrl)
    {
        super(projectSite, remoteUrl);

        // Init Autosave (defaults to true)
        String autoSavePrefKey = "AutoSave_" + getLocalSite().getName();
        _autsave = Prefs.getDefaultPrefs().getBoolean(autoSavePrefKey, true);
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
