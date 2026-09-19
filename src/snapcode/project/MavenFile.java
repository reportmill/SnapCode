package snapcode.project;
import snap.util.XMLElement;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.util.DownloadFile;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * This class reads a maven package file for a maven dependency.
 */
public class MavenFile {

    // The maven package
    private MavenPackage _mavenPackage;

    // The file type
    private String _fileType;

    // The download file
    private DownloadFile _downloadFile;

    /**
     * Constructor.
     */
    public MavenFile(MavenPackage mavenPackage, String fileType)
    {
        _mavenPackage = mavenPackage;
        _fileType = fileType;
    }

    /**
     * Returns the package.
     */
    public MavenPackage getPackage()  { return _mavenPackage; }

    /**
     * Returns the file URL in remote repository.
     */
    public WebURL getRemoteUrl()
    {
        String fileUrlString = _mavenPackage.getRemoteFileUrlStringForType(_fileType);
        return WebURL.getUrl(fileUrlString);
    }

    /**
     * Returns the local maven file, triggering load if missing.
     */
    public WebFile getLocalFile()
    {
        // Create local file
        String localFilePath = _mavenPackage.getLocalFilePathForType(_fileType);
        WebFile localFile = WebFile.createFileForPath(localFilePath, false);

        // If file doesn't exist, load it
        if (!localFile.getExists()) {
            try { downloadFile(); }
            catch (IOException e) { System.err.println("MavenFile: Failed to download file: " + e.getMessage()); }
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
            throw new IOException("Can't resolve maven path for: " + _mavenPackage.getId());
        downloadFile.getLocalPath();
    }

    /**
     * Returns the download file.
     */
    private synchronized DownloadFile getDownloadFile()
    {
        if (_downloadFile != null) return _downloadFile;
        WebURL remoteUrl = getRemoteUrl();
        String localFilePath = _mavenPackage.getLocalFilePathForType(_fileType);
        if (remoteUrl == null || localFilePath == null)
            return null;
        return _downloadFile = new DownloadFile(remoteUrl.getJavaUrl(), Paths.get(localFilePath));
    }

    /**
     * Returns the XML for this MavenFile.
     */
    XMLElement getLocalFileXml()
    {
        WebFile pomFile = getLocalFile();
        String xmlString = pomFile.getExists() ? pomFile.getText() : null;
        if (xmlString == null) {
            System.err.println(getClass().getSimpleName() + ".getLocalFileXml: Can't read pom file: " + pomFile);
            return null;
        }

        // Read and return
        try { return XMLElement.readXmlFromString(xmlString); }
        catch (Exception e) {
            System.err.println(getClass().getSimpleName() + ".getLocalFileXml: Error reading file: " + pomFile.getPath());
            System.err.println(e.getMessage());
            return null;
        }
    }

    @Override
    public String toString()  { return getClass().getSimpleName() + ": " + getRemoteUrl(); }
}
