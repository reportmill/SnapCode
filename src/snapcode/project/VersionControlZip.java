package snapcode.project;
import snap.util.ArrayUtils;
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
        WebFile rootDir = zipSite.getRootDir();
        WebFile[] rootFiles = rootDir.getFiles();
        if (rootFiles.length == 0)
            System.out.println("VersionControlZip: No root files found for zip: " + _remoteSiteUrl);
        else if (rootFiles.length > 1)
            System.out.println("VersionControlZip: ZipFile has multiple files: " + _remoteSiteUrl + ", " + rootFiles.length);

        // Look for nested top level directory and use that nested dir site instead
        WebFile dirFile = ArrayUtils.findMatch(rootFiles, file -> file.isDir());
        if (dirFile != null) {
            WebURL dirFileUrl = dirFile.getURL();
            return dirFileUrl.getAsSite();
        }

        // Return
        return zipSite;
    }
}
