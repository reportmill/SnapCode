package snapcode.project;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;

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

        // Look for nested top level directory
        String siteName = _remoteSiteUrl.getFilenameSimple();
        WebFile dirFile = zipSite.getFileForPath('/' + siteName);
        if (dirFile == null) // If downloading github zip
            dirFile = zipSite.getFileForPath('/' + siteName + "-master");

        // If nested top level directory found, use that nested dir site instead
        if (dirFile != null && dirFile.isDir()) {
            WebURL dirFileUrl = dirFile.getURL();
            return dirFileUrl.getAsSite();
        }

        // Return
        return zipSite;
    }
}
