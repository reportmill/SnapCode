package snapcode.project;
import snap.util.ListUtils;
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
