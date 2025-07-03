package snapcode.project;
import snap.util.ArrayUtils;
import snap.web.*;
import java.util.List;

/**
 * A WebSite implementation for GitDirIndex.
 */
public class GitIndexSite extends WebSite {

    // The GitDir
    private GitDir _gitDir;

    /**
     * Constructor.
     */
    public GitIndexSite(GitDir gitDir)
    {
        super();
        _gitDir = gitDir;

        // Get/set site URL
        String gitDirPath = _gitDir.getDir().getUrl().getString();
        String sitePath = gitDirPath + ".index";
        WebURL siteUrl = WebURL.getUrl(sitePath);
        setURL(siteUrl);
    }

    /**
     * Handles a get or head request.
     */
    @Override
    protected void doGetOrHead(WebRequest aReq, WebResponse aResp, boolean isHead)
    {
        // Get URL and path and create empty response
        WebURL fileUrl = aReq.getURL();
        String filePath = fileUrl.getPath();
        if (filePath == null)
            filePath = "/";

        // Get entry - if not found, set Response.Code to NOT_FOUND and return
        GitIndex.Entry entry = _gitDir.getIndex().getEntry(filePath);
        if (entry == null) {
            aResp.setCode(WebResponse.NOT_FOUND);
            return;
        }

        // Otherwise configure
        aResp.setCode(WebResponse.OK);
        aResp.setDir(entry.isDir());
        aResp.setLastModTime(entry.getLastModified());
        aResp.setSize(entry.getLength());

        // If Head, just return
        if (isHead)
            return;

        // Handle plain file
        if (aResp.isFile()) {
            byte[] bytes = entry.getBytes();
            aResp.setBytes(bytes);
        }

        // Handle directory: Walk RevTree and get files for children
        else {
            GitIndex.Entry[] gitIndexEntries = entry.getEntries();
            List<FileHeader> fileHeaders = ArrayUtils.mapToList(gitIndexEntries, indexEntry -> getFileHeader(indexEntry.getPath()));
            aResp.setFileHeaders(fileHeaders);
        }
    }

    /**
     * Get file from directory.
     */
    protected FileHeader getFileHeader(String aPath)
    {
        GitIndex.Entry entry = _gitDir.getIndex().getEntry(aPath);
        if (entry == null)
            return null;

        FileHeader fileHeader = new FileHeader(aPath, entry.isDir());
        fileHeader.setLastModTime(entry.getLastModified());
        fileHeader.setSize(entry.getLength());
        return fileHeader;
    }
}
