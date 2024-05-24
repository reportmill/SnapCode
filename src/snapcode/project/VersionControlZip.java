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

        // If remote size is Zip file with single dir with same name as Zip file, use dir
        WebSite zipSite = _remoteSiteUrl.getAsSite();
        String siteName = _remoteSiteUrl.getFilenameSimple();
        WebFile dirFile = zipSite.getFileForPath('/' + siteName);
        if (dirFile == null) // If downloading github zip
            dirFile = zipSite.getFileForPath('/' + siteName + "-master");
        if (dirFile != null && dirFile.isDir())
            _remoteSiteUrl = dirFile.getURL();
    }
}
