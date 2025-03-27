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
     * Override to return whether local clone of zip file is present.
     */
    @Override
    public boolean isAvailable()
    {
        WebFile cloneFileCached = createCloneZipFile();
        return cloneFileCached.getExists();
    }

    /**
     * Override to return project site in remote ZipFile.
     */
    @Override
    protected WebSite getRemoteSiteImpl()
    {
        WebSite zipSite = _remoteSiteUrl.getAsSite();
        return getProjectSiteForZipFileSite(zipSite);
    }

    /**
     * Override to return project site in clone ZipFile.
     */
    @Override
    protected WebSite getCloneSiteImpl()
    {
        WebFile cloneZipFile = getCloneZipFile();
        WebSite cloneZipFileSite = cloneZipFile.getURL().getAsSite();
        return getProjectSiteForZipFileSite(cloneZipFileSite);
    }

    /**
     * Returns the file for clone of remote zip file.
     */
    private WebFile getCloneZipFile()
    {
        WebFile remoteFile = getRemoteSiteUrl().getFile();
        WebFile cloneZipFile = createCloneZipFile();

        // If clone file exists and is newer than remote file, just return
        if (cloneZipFile.getExists() && cloneZipFile.getLastModTime() >= remoteFile.getLastModTime())
            return cloneZipFile;

        // Update clone file
        cloneZipFile.setBytes(remoteFile.getBytes());
        cloneZipFile.save();

        // Return
        return cloneZipFile;
    }

    /**
     * Creates the file for clone of remote zip file.
     */
    private WebFile createCloneZipFile()
    {
        // Get sandbox site for remote zip file
        WebURL remoteZipFileUrl = getRemoteSiteUrl();
        WebSite remoteZipFileSite = remoteZipFileUrl.getAsSite();
        WebSite sandboxSite = remoteZipFileSite.getSandboxSite();

        // Create clone file for zip file in sandbox site
        String cloneZipFilePath = '/' + remoteZipFileUrl.getFilename();
        return sandboxSite.createFileForPath(cloneZipFilePath, false);
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

        // Get clone of remote zip file
        getCloneZipFile();

        // Do normal version
        return super.checkout(taskMonitor);
    }

    /**
     * Returns the site for project dir in zip site.
     */
    private static WebSite getProjectSiteForZipFileSite(WebSite zipFileSite)
    {
        WebFile rootDir = zipFileSite.getRootDir();
        List<WebFile> rootFiles = rootDir.getFiles();

        // Look for nested top level directory and use that nested dir site instead
        WebFile dirFile = ListUtils.findMatch(rootFiles, file -> ProjectUtils.isProjectDir(file));
        if (dirFile != null) {
            WebURL dirFileUrl = dirFile.getURL();
            return dirFileUrl.getAsSite();
        }

        // This can't be good
        System.err.println("VersionControlZip.getProjectSiteForZipFileSite: Couldn't find project in zip file: " + zipFileSite);
        return zipFileSite;
    }
}
