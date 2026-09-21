package snapcode.project;
import snap.util.ListUtils;
import snap.util.XMLElement;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.util.DownloadFile;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * This class reads a metadata-local.xml file for a maven dependency.
 */
public class MavenArtifactFile {

    // The maven artifact
    private MavenArtifact _mavenArtifact;

    // The versions
    private List<String> _versions;

    // The download file
    private DownloadFile _downloadFile;

    // The XML
    private XMLElement _xml;

    // Metadata filename
    private static final String METADATA_FILE_NAME = "maven-metadata.xml";

    /**
     * Constructor.
     */
    public MavenArtifactFile(MavenArtifact mavenArtifact)
    {
        _mavenArtifact = mavenArtifact;
    }

    /**
     * Returns the latest version.
     */
    public String getLatestVersion()
    {
        List<String> versions = getVersions();
        return !versions.isEmpty() ? versions.getLast() : null;
    }

    /**
     * Returns the versions.
     */
    public List<String> getVersions()
    {
        if (_versions != null) return _versions;
        return _versions = getVersionsImpl();
    }

    /**
     * Returns the versions.
     */
    private List<String> getVersionsImpl()
    {
        // Get <versionin> XML elements
        XMLElement xml = getXML();
        XMLElement versioningXML = xml != null ? xml.getElement("versioning") : null;
        XMLElement versionsXML = versioningXML != null ? versioningXML.getElement("versions") : null;
        List<XMLElement> versionXMLs = versionsXML != null ? versionsXML.getElements("version") : null;
        if (versionXMLs == null || versionXMLs.isEmpty())
            return Collections.emptyList();

        // Get version strings and return
        return ListUtils.mapNonNull(versionXMLs, MavenArtifactFile::getVersionStringForVersionXml);
    }

    /**
     * Returns the XML.
     */
    private XMLElement getXML()
    {
        if (_xml != null) return _xml;
        return _xml = getLocalFileXml();
    }

    /**
     * Returns the local maven file, triggering load if missing.
     */
    public WebFile getLocalFile()
    {
        // Create local file
        String localFilePath = _mavenArtifact.getLocalFilePathForFilename(METADATA_FILE_NAME);
        WebFile localFile = WebFile.createFileForPath(localFilePath, false);

        // If file doesn't exist, load it
        if (!localFile.getExists()) {
            try { downloadFile(); }
            catch (IOException e) { System.err.println(getClass().getSimpleName() + ": Failed to download file: " + e.getMessage()); }
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
            catch (Exception e) { System.err.println(getClass().getSimpleName() + ": Delete local file failed: " + e.getMessage()); }
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
            throw new IOException("Can't resolve maven path for: " + _mavenArtifact.getId());
        downloadFile.getLocalPath();
    }

    /**
     * Returns the download file.
     */
    private synchronized DownloadFile getDownloadFile()
    {
        if (_downloadFile != null) return _downloadFile;
        WebURL remoteUrl = getRemoteUrl();
        String localFilePath = _mavenArtifact.getLocalFilePathForFilename(METADATA_FILE_NAME);
        if (remoteUrl == null || localFilePath == null)
            return null;
        return _downloadFile = new DownloadFile(remoteUrl.getJavaUrl(), Paths.get(localFilePath));
    }

    /**
     * Returns the XML for this MavenFile.
     */
    XMLElement getLocalFileXml()
    {
        WebFile artifactFile = getLocalFile();
        String xmlString = artifactFile.getExists() ? artifactFile.getText() : null;
        if (xmlString == null) {
            System.err.println(getClass().getSimpleName() + ".getLocalFileXml: Can't read artifact file: " + artifactFile);
            return null;
        }

        // Read and return
        try { return XMLElement.readXmlFromString(xmlString); }
        catch (Exception e) {
            System.err.println(getClass().getSimpleName() + ".getLocalFileXml: Error reading artifact file: " + artifactFile.getPath());
            System.err.println(e.getMessage());
            return null;
        }
    }

    /**
     * Returns the file URL in remote repository.
     */
    private WebURL getRemoteUrl()
    {
        String fileUrlString = _mavenArtifact.getRemoteFileUrlStringForFilename(METADATA_FILE_NAME);
        return WebURL.getUrl(fileUrlString);
    }

    @Override
    public String toString()  { return getClass().getSimpleName() + ": " + getRemoteUrl(); }

    /**
     * Returns the version string for given version XML, e.g.: <version>2025.01.02</version>.
     */
    private static String getVersionStringForVersionXml(XMLElement xml)
    {
        String version = xml.getValue();
        return !version.isBlank() ? version.trim() : null;
    }
}
