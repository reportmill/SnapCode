package snapcode.project;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.util.DownloadFile;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * This class reads a maven package file for a maven dependency.
 */
public class MavenFile {

    // The maven dependency
    private MavenPackage _mavenDependency;

    // The file type
    private String _fileType;

    // The download file
    private DownloadFile _downloadFile;

    /**
     * Constructor.
     */
    public MavenFile(MavenPackage mavenDependency, String fileType)
    {
        _mavenDependency = mavenDependency;
        _fileType = fileType;
    }

    /**
     * Returns the file URL in remote repository.
     */
    public WebURL getRemoteUrl()
    {
        String fileUrlString = _mavenDependency.getRemoteFileUrlStringForType(_fileType);
        return WebURL.getUrl(fileUrlString);
    }

    /**
     * Returns the local maven file, triggering load if missing.
     */
    public WebFile getLocalFile()
    {
        // Create local file
        String localFilePath = _mavenDependency.getLocalFilePathForType(_fileType);
        WebFile localFile = WebFile.createFileForPath(localFilePath, false);

        // If file doesn't exist, load it
        if (localFile != null && !localFile.getExists()) {
            try { downloadFile(); }
            catch (IOException e) { return null; }
        }

        // Return
        return localFile;
    }

    /**
     * Deletes the local file.
     */
    public void deleteLocalFile()
    {
        DownloadFile downloadFile = getDownloadFile();
        if (downloadFile != null) {
            try { downloadFile.deleteLocalFile(); }
            catch (Exception e) { System.err.println("MavenFile: Delete local file failed: " + e.getMessage()); }
        }
        _downloadFile = null;
    }

    /**
     * Loads the file.
     */
    public void downloadFile() throws IOException
    {
        DownloadFile downloadFile = getDownloadFile();
        if (downloadFile == null)
            throw new IOException("Can't resolve maven path for: " + _mavenDependency.getId());
        downloadFile.getLocalPath();
    }

    /**
     * Returns the download file.
     */
    private synchronized DownloadFile getDownloadFile()
    {
        if (_downloadFile != null) return _downloadFile;
        WebURL remoteUrl = getRemoteUrl();
        String localFilePath = _mavenDependency.getLocalFilePathForType(_fileType);
        if (remoteUrl == null || localFilePath == null)
            return null;
        return _downloadFile = new DownloadFile(remoteUrl.getJavaUrl(), Paths.get(localFilePath));
    }
}
