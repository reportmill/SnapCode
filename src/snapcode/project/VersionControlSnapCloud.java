package snapcode.project;
import snap.util.ActivityMonitor;
import snap.util.ListUtils;
import snap.util.Prefs;
import snap.util.UserInfo;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.List;
import java.util.concurrent.*;

/**
 * This version control class uses drop box.
 */
public class VersionControlSnapCloud extends VersionControl {

    // Whether to autosave
    private boolean _autsave;

    // Whether repo is writeable
    private boolean _writable;

    // The SnapCloud root URL
    public static final String SNAPCLOUD_ROOT = "snapcloud:/"; // "dbox://dbox.com/";

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
     * Override to prefetch remote files.
     */
    @Override
    protected List<WebFile> getUpdateFilesForLocalFilesImpl(List<WebFile> localFiles)
    {
        prefetchRemoteFilesForLocalFiles(localFiles);
        return super.getUpdateFilesForLocalFilesImpl(localFiles);
    }

    /**
     * Override to preload remote files.
     */
    @Override
    protected boolean updateFilesImpl(List<WebFile> localFiles, ActivityMonitor activityMonitor) throws Exception
    {
        preloadRemoteFilesForLocalFiles(localFiles);
        return super.updateFilesImpl(localFiles, activityMonitor);
    }

    /**
     * Returns whether given URL is SnapCloud.
     */
    public static boolean isSnapCloudUrl(WebURL aURL)
    {
        String scheme = aURL.getScheme();
        return scheme.equals("snapcloud") || scheme.equals("sc") || scheme.equals("dbox") || scheme.equals("dropbox");
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

    /**
     * Prefetches remote files for given local files.
     */
    private void prefetchRemoteFilesForLocalFiles(List<WebFile> localFiles)
    {
        localFiles.forEach(file -> prefetchRemoteFileForLocalFile(file));
    }

    /**
     * Prefetches files in directory.
     */
    private void prefetchRemoteFileForLocalFile(WebFile aFile)
    {
        WebFile remoteFile = getRemoteSite().getFileForPath(aFile.getPath());
        CompletableFuture.runAsync(() -> prefetchRemoteFileForLocalFileImpl(remoteFile));
        Thread.yield();
    }

    /**
     * Prefetches given files.
     */
    private static void prefetchRemoteFileForLocalFileImpl(WebFile aFile)
    {
        if (aFile.isDir()) {
            List<WebFile> dirFiles = aFile.getFiles();
            dirFiles.parallelStream().forEach(file -> prefetchRemoteFileForLocalFileImpl(file));
            return;
        }

        // Prefetch header - probably not needed because dir getFiles already fetches
        if (!aFile.isVerified())
            aFile.getExists();
    }

    /**
     * Preloads remote files for given local files. Expects all files to be regular files.
     */
    private void preloadRemoteFilesForLocalFiles(List<WebFile> localFiles)
    {
        if (localFiles.size() <= 1)
            return;
        if (ListUtils.hasMatch(localFiles, file -> file.isDir())) {
            System.err.println("VersionControlSnapCloud.preloadRemoteFilesForLocalFiles: Unexpected dir provided");
            return;
        }

        // Preload files in executor
        WebSite remoteSite = getRemoteSite();
        ExecutorService executor = Executors.newCachedThreadPool();
        localFiles.forEach(file -> executor.submit(() -> preloadRemoteFileForLocalFile(file, remoteSite)));
        executor.shutdown();
    }

    /**
     * Preloads given file.
     */
    private static void preloadRemoteFileForLocalFile(WebFile localFile, WebSite remoteSite)
    {
        WebFile remoteFile = remoteSite.createFileForPath(localFile.getPath(), localFile.isDir());
        if (remoteFile.getExists())
            remoteFile.getBytes();
    }
}
