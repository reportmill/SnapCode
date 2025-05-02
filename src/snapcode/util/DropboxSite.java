package snapcode.util;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import snap.util.*;
import snap.web.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * This website implementation works with Dropbox.
 */
public class DropboxSite extends WebSite {

    // Dropbox client
    private DbxClientV2 _dboxClient;

    // Shared instance
    private static DropboxSite _shared;

    // Shared sites for email and project
    private static Map<String, WebSite> _dropBoxSites = new HashMap<>();

    // Header value
    private static String ACCESS_TOKEN = DropboxSiteHelp.ACCESS_TOKEN;

    // Constants
    private static final String DROPBOX_ROOT = "dbox://dbox.com";

    /**
     * Constructor.
     */
    private DropboxSite()
    {
        super();

        // Create/set URL
        WebURL url = WebURL.getURL(DROPBOX_ROOT);
        setURL(url);
    }

    /**
     * Returns the dropbox client object.
     */
    private DbxClientV2 getClient()
    {
        if (_dboxClient != null) return _dboxClient;

        DbxRequestConfig config = DbxRequestConfig.newBuilder("SnapCode").build();
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);
        return _dboxClient = client;
    }

    /**
     * Handles getting file info, contents or directory files.
     */
    @Override
    protected void doGetOrHead(WebRequest aReq, WebResponse aResp, boolean isHead)
    {
        // Always do Head
        doHead(aReq, aResp);
        if (isHead)
            return;

        // If error, just return
        if (aResp.getCode() != WebResponse.OK)
            return;

        // If directory, get files
        FileHeader fileHeader = aResp.getFileHeader();
        if (fileHeader.isDir())
            doGetDir(aReq, aResp);

        // Otherwise, get contents
        else doGetFileContents(aReq, aResp);
    }

    /**
     * Get Head for request.
     */
    protected void doHead(WebRequest aReq, WebResponse aResp)
    {
        DbxClientV2 dropboxClient = getClient();
        DbxUserFilesRequests dropboxFiles = dropboxClient.files();
        String dropboxPath = getDropboxPathForURL(aReq.getURL());

        try {

            // Get metadata for path
            Metadata metadata = dropboxFiles.getMetadata(dropboxPath);

            // Get FileHeader for metadata
            FileHeader fileHeader = createFileHeaderForDropboxMetadata(metadata);
            aResp.setFileHeader(fileHeader);
        }

        // Catch exceptions: Set in response
        catch (DbxException e) { aResp.setException(e); }
    }

    /**
     * Get Directory listing for request.
     */
    protected void doGetDir(WebRequest aReq, WebResponse aResp)
    {
        DbxClientV2 dropboxClient = getClient();
        DbxUserFilesRequests dropboxFiles = dropboxClient.files();
        String dropboxPath = getDropboxPathForURL(aReq.getURL());

        try {

            // List folder contents
            ListFolderResult listFolderResult = dropboxFiles.listFolder(dropboxPath);
            List<Metadata> fileMetadatas = listFolderResult.getEntries();

            // Convert to FileHeaders and add to response
            List<FileHeader> fileHeaders = ListUtils.map(fileMetadatas, DropboxSite::createFileHeaderForDropboxMetadata);
            aResp.setFileHeaders(fileHeaders);
        }

        // Catch exceptions
        catch (Exception e) { aResp.setException(e); }
    }

    /**
     * Get file request.
     */
    protected void doGetFileContents(WebRequest aReq, WebResponse aResp)
    {
        DbxClientV2 dropboxClient = getClient();
        DbxUserFilesRequests dropboxFiles = dropboxClient.files();
        String dropboxPath = getDropboxPathForURL(aReq.getURL());

        try (ByteArrayOutputStream downloadFile = new ByteArrayOutputStream()) {

            // Fetch bytes
            FileMetadata metadata = dropboxFiles.downloadBuilder(dropboxPath).download(downloadFile);
            byte[] fileBytes = downloadFile.toByteArray();
            aResp.setBytes(fileBytes);
        }

        // Catch exceptions
        catch (IOException | DbxException e) { aResp.setException(e); }
    }

    /**
     * Handle a PUT request.
     */
    protected void doPut(WebRequest aReq, WebResponse aResp)
    {
        WebFile file = aReq.getFile();
        if (file.isFile())
            doPutFile(aReq, aResp);
        else doPutDir(aReq, aResp);
    }

    /**
     * Handle a PUT request.
     */
    protected void doPutFile(WebRequest aReq, WebResponse aResp)
    {
    }

    /**
     * Handle a PUT request.
     */
    protected void doPutDir(WebRequest aReq, WebResponse aResp)
    {
    }

    /**
     * Handle a DELETE request.
     */
    protected void doDelete(WebRequest aReq, WebResponse aResp)
    {
    }

    /**
     * Returns the dropbox path for URL.
     */
    private String getDropboxPathForURL(WebURL aURL)
    {
        return aURL.getPath();
    }

    /**
     * Returns a FileHeader for DropBox file/folder metadata.
     */
    private static FileHeader createFileHeaderForDropboxMetadata(Metadata metadata)
    {
        // Get attributes
        String filePath = metadata.getPathDisplay();
        boolean isDir = metadata instanceof FolderMetadata;

        // Create FileHeader
        FileHeader fileHeader = new FileHeader(filePath, isDir);

        // Get additional file attributes
        if (metadata instanceof FileMetadata) {

            // Get/set size
            FileMetadata fileMetadata = (FileMetadata) metadata;
            long size = fileMetadata.getSize();
            Date lastModDate = fileMetadata.getServerModified();
            fileHeader.setSize(size);
            fileHeader.setLastModTime(lastModDate.getTime());
        }

        // Return
        return fileHeader;
    }

    /**
     * Returns a path for email address and project name. For example: jack@abc.com = /abc.com/jack.
     */
    private static String getPathForEmailAddressAndProject(String emailAddress, String projectName)
    {
        // Get email name
        int domainIndex = emailAddress.indexOf('@');
        if (domainIndex < 0)
            return "unknown";
        String emailName = emailAddress.substring(0, domainIndex);

        // Get email domain parts
        String domainName = emailAddress.substring(domainIndex + 1);

        // Get email path - just return if no project name
        String emailPath = '/' + domainName + "/" + emailName;
        if (projectName == null)
            return emailPath;

        // Add project name and return
        return emailPath + '/' + projectName;
    }

    /**
     * Returns the shared DropboxSite.
     */
    public static DropboxSite getShared()
    {
        if (_shared != null) return _shared;
        return _shared = new DropboxSite();
    }

    /**
     * Returns shared instance.
     */
    public static WebSite getSiteForUrl(WebURL siteUrl)
    {
        // Get cached dropbox for email
        String siteUrlAddress = siteUrl.getString();
        WebSite projectSite = _dropBoxSites.get(siteUrlAddress);
        if (projectSite != null)
            return projectSite;

        // Get shared
        DropboxSite dropboxSite = getShared();

        // Get project dir path and project dir
        String projectDirPath = siteUrl.getPath();
        WebFile projectDir = dropboxSite.createFileForPath(projectDirPath, true);

        // Get dir as site
        WebURL projectDirUrl = projectDir.getURL();
        projectSite = projectDirUrl.getAsSite();

        // Add to cache and return
        _dropBoxSites.put(siteUrlAddress, projectSite);
        return projectSite;
    }

    /**
     * Returns shared instance.
     */
    public static WebSite getSiteForEmailAddressAndProjectName(String emailAddress, String projectName)
    {
        // Get full name
        String fullName = emailAddress;
        if (projectName != null)
            fullName += projectName;

        // Get cached dropbox for email
        WebSite projectSite = _dropBoxSites.get(fullName);
        if (projectSite != null)
            return projectSite;

        // Get shared
        DropboxSite dropboxSite = getShared();

        // Get project dir path and project dir
        String projectDirPath = getPathForEmailAddressAndProject(emailAddress, projectName);
        WebFile projectDir = dropboxSite.createFileForPath(projectDirPath, true);

        // Get dir as site
        WebURL projectDirUrl = projectDir.getURL();
        projectSite = projectDirUrl.getAsSite();

        // Create dir path
        _dropBoxSites.put(fullName, projectSite);
        return projectSite;
    }
}
