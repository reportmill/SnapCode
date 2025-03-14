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
     * Override to check ZipFile for nested top level directory and use that site instead.
     */
    @Override
    protected WebSite getRemoteSiteImpl()
    {
        // Get normal ZipFile site
        WebSite zipSite = _remoteSiteUrl.getAsSite();
        WebFile rootDir = zipSite.getRootDir();
        List<WebFile> rootFiles = rootDir.getFiles();
        if (rootFiles.isEmpty())
            System.out.println("VersionControlZip: No root files found for zip: " + _remoteSiteUrl);
        else if (rootFiles.size() > 1)
            System.out.println("VersionControlZip: ZipFile has multiple files: " + _remoteSiteUrl + ", " + rootFiles.size());

        // Look for nested top level directory and use that nested dir site instead
        WebFile dirFile = ListUtils.findMatch(rootFiles, file -> file.isDir());
        if (dirFile != null) {
            WebURL dirFileUrl = dirFile.getURL();
            return dirFileUrl.getAsSite();
        }

        // Return
        return zipSite;
    }
}
