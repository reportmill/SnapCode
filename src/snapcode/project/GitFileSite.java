package snapcode.project;
import snap.util.ArrayUtils;
import snap.web.*;

/**
 * A WebSite implementation for a GitCommit.
 */
public class GitFileSite extends WebSite {

    // The GitDir
    private GitDir _gitDir;

    // The GitCommit
    private GitCommit _commit;

    /**
     * Constructor.
     */
    public GitFileSite(GitDir gitDir, GitCommit aGC)
    {
        super();
        _gitDir = gitDir;
        _commit = aGC;

        // Get URL for site and set
        String gitDirPath = _gitDir.getDir().getURL().getString();
        String sitePath = gitDirPath + "!/" + _commit._rev.getId().getName();
        WebURL siteUrl = WebURL.getURL(sitePath);
        setURL(siteUrl);
    }

    /**
     * Returns the tree for this site.
     */
    public GitFile<?> getTree()  { return _commit.getTree(); }

    /**
     * Handles a get or head request.
     */
    protected void doGetOrHead(WebRequest aReq, WebResponse aResp, boolean isHead)
    {
        // Get URL and path and create empty response
        WebURL fileUrl = aReq.getURL();
        String filePath = fileUrl.getPath();
        if (filePath == null)
            filePath = "/";

        // Get Head branch Commit Tree and look for file - f not found, set Response.Code to NOT_FOUND and return
        GitFile<?> gitFile = getTree().getFileForPath(filePath);
        if (gitFile == null) {
            aResp.setCode(WebResponse.NOT_FOUND);
            return;
        }

        // Otherwise configure
        aResp.setCode(WebResponse.OK);
        aResp.setDir(gitFile.isDir());
        aResp.setLastModTime(_commit.getCommitTime());

        // If Head, just return
        if (isHead)
            return;

        // Handle plain file
        if (aResp.isFile()) {
            byte[] bytes = gitFile.getBytes();
            aResp.setBytes(bytes);
        }

        // Handle directory: Walk RevTree and get files for children
        else {
            GitFile<?>[] dirFiles = gitFile.getFiles();
            FileHeader[] dirFileHeaders = ArrayUtils.map(dirFiles, file -> getFileHeader(file.getPath()), FileHeader.class);
            aResp.setFileHeaders(dirFileHeaders);
        }
    }

    /**
     * Get file from directory.
     */
    protected FileHeader getFileHeader(String aPath)
    {
        // Get Head branch Commit Tree and look for file
        GitFile<?> gitFile = getTree().getFileForPath(aPath);
        if (gitFile == null)
            return null;

        // Create file for path and commit time
        FileHeader fileHeader = new FileHeader(aPath, gitFile.isDir());
        fileHeader.setLastModTime(_commit.getCommitTime());
        return fileHeader;
    }
}
