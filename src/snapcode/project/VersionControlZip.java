package snapcode.project;
import snap.util.ListUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.List;

/**
 * This class is a VersionControl implementation that uses a Zip file as remote repository.
 */
public class VersionControlZip extends VersionControl {

    /**
     * Constructor.
     */
    public VersionControlZip(WebSite localSite, WebURL remoteSiteUrl)
    {
        super(localSite, remoteSiteUrl);
    }

    /**
     * Override to check ZipFile for nested top level directory and use that site instead.
     */
    @Override
    protected WebSite getRemoteSiteImpl()
    {
        // Get normal ZipFile site
        WebSite zipSite = _remoteSiteUrl.getAsSite();
        WebFile rootDir = zipSite.getRootDir();
        List<WebFile> rootFiles = rootDir.getFiles();

        // Look for nested top level directory and use that nested dir site instead
        WebFile dirFile = ListUtils.findMatch(rootFiles, file -> ProjectUtils.isProjectDir(file));
        if (dirFile != null) {
            WebURL dirFileUrl = dirFile.getURL();
            return dirFileUrl.getAsSite();
        }

        // Return
        return zipSite;
    }

    /**
     * Override to return clone file cached ZipFileSite.
     */
    @Override
    protected WebSite getCloneSiteImpl()
    {
        WebFile cloneFileCached = getCloneFileCached();
        WebURL cloneFileUrl = cloneFileCached.getURL();
        return cloneFileUrl.getAsSite();
    }

    /**
     * Returns a local file for given file (with option to cache for future use).
     */
    public WebFile getCloneFileCached()
    {
        WebFile remoteFile = getRemoteSiteUrl().getFile();
        WebFile cloneFileCached = getCloneFile();

        // If clone file exists and is newer than remote file, just return
        if (cloneFileCached.getExists() && cloneFileCached.getLastModTime() >= remoteFile.getLastModTime())
            return cloneFileCached;

        // Update clone file
        cloneFileCached.setBytes(remoteFile.getBytes());
        cloneFileCached.save();

        // Return
        return cloneFileCached;
    }

    /**
     * Returns a cache file for path.
     */
    private WebFile getCloneFile()
    {
        // Get RemoteSite sandbox site and clone file path
        WebURL remoteSiteUrl = getRemoteSiteUrl();
        WebSite remoteSite = remoteSiteUrl.getAsSite();
        WebSite sandboxSite = remoteSite.getSandboxSite();
        String cloneFilePath = '/' + remoteSiteUrl.getFilename();

        // Get or create clone file
        WebFile sandboxCloneFile = sandboxSite.getFileForPath(cloneFilePath);
        if (sandboxCloneFile == null)
            sandboxCloneFile = sandboxSite.createFileForPath(cloneFilePath, false);

        // Return
        return sandboxCloneFile;
    }

    /**
     * Returns whether existing VCS artifacts are detected for project.
     */
    @Override
    public boolean isAvailable()
    {
        // If clone file exists and is newer than remote file, just return
        WebFile cloneFileCached = getCloneFile();
        if (cloneFileCached.getExists())
            return true;
        return false;
    }

    /**
     * Load all remote files into project directory.
     */
    @Override
    public boolean checkout(TaskMonitor taskMonitor) throws Exception
    {
        // Make sure local site exists
        WebSite localSite = getLocalSite();
        if (!localSite.getExists()) {
            WebFile rootDir = localSite.getRootDir();
            rootDir.save();
        }

        // Do normal version
        return super.checkout(taskMonitor);
    }

    /**
     * Override to do fetch first.
     */
    @Override
    public List<WebFile> getUpdateFilesForRootFiles(List<WebFile> theFiles)
    {
        // Make sure latest version is in clone site
        getCloneFileCached();

        // Do normal version
        return super.getUpdateFilesForRootFiles(theFiles);
    }
}
