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
     * Override to return project site in remote ZipFile.
     */
    @Override
    protected WebSite getRemoteSiteImpl()
    {
        WebSite zipSite = _remoteSiteUrl.getAsSite();
        return getProjectSiteForZipFileSite(zipSite);
    }

    /**
     * Override to return project site in local ZipFile.
     */
    @Override
    protected WebSite getCloneSiteImpl()
    {
        WebFile localZipFile = getLocalZipFile();
        WebSite localZipFileSite = localZipFile.getURL().getAsSite();
        return getProjectSiteForZipFileSite(localZipFileSite);
    }

    /**
     * Returns the local copy of zip file.
     */
    public WebFile getLocalZipFile()
    {
        WebFile remoteFile = getRemoteSiteUrl().getFile();
        WebFile localZipFile = createLocalZipFile();

        // If clone file exists and is newer than remote file, just return
        if (localZipFile.getExists() && localZipFile.getLastModTime() >= remoteFile.getLastModTime())
            return localZipFile;

        // Update clone file
        localZipFile.setBytes(remoteFile.getBytes());
        localZipFile.save();

        // Return
        return localZipFile;
    }

    /**
     * Creates the file for local copy of zip file.
     */
    private WebFile createLocalZipFile()
    {
        // Get sandbox site for remote zip file
        WebURL remoteZipFileUrl = getRemoteSiteUrl();
        WebSite remoteZipFileSite = remoteZipFileUrl.getAsSite();
        WebSite sandboxSite = remoteZipFileSite.getSandboxSite();

        // Create local file for zip file in sandbox site
        String localZipFilePath = '/' + remoteZipFileUrl.getFilename();
        return sandboxSite.createFileForPath(localZipFilePath, false);
    }

    /**
     * Returns whether existing VCS artifacts are detected for project.
     */
    @Override
    public boolean isAvailable()
    {
        // If clone file exists and is newer than remote file, just return
        WebFile cloneFileCached = createLocalZipFile();
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
