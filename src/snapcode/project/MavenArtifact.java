package snapcode.project;
import snap.props.PropObject;
import snap.util.*;
import snap.web.*;
import snapcode.util.DownloadFile;
import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.util.*;

/**
 * This class represents a Maven artifact (a group-id + artifact-id).
 */
public class MavenArtifact extends PropObject {

    // The full artifact id string
    private final String _id;

    // The group id string
    private final String _groupId;

    // The artifact id string
    private final String _artifactId;

    // The versions
    private List<String> _versions;

    // The downloaded local file
    private WebFile _localFile;

    // Whether dependency is loaded
    private boolean _loaded;

    // Whether dependency is loading
    private boolean _loading;

    // The error string
    private String _error;

    // A map of all artifacts
    private static Map<String, MavenArtifact> _artifacts = new HashMap<>();

    // Constants for properties
    public static final String Loaded_Prop = "Loaded";
    public static final String Loading_Prop = "Loading";

    // Metadata filename
    static final String METADATA_FILE_NAME = "maven-metadata.xml";

    // Constant for Maven central URL
    //public static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";
    public static final String MAVEN_CENTRAL_URL = "https://maven-central.storage-download.googleapis.com/maven2";

    /**
     * Constructor with full artifact id.
     */
    private MavenArtifact(String artifactId)
    {
        super();
        _id = artifactId;
        String[] names = artifactId.split(":");
        _groupId = names[0];
        _artifactId = names[1];
    }

    /**
     * Returns id string.
     */
    public String getId()  { return _id; }

    /**
     * Returns the group id string.
     */
    public String getGroupId()  { return _groupId; }

    /**
     * Returns the artifact id string.
     */
    public String getArtifactId()  { return _artifactId; }

    /**
     * Returns the latest version.
     */
    public String getLatestVersion()
    {
        List<String> versions = getAllVersions();
        return !versions.isEmpty() ? versions.getLast() : null;
    }

    /**
     * Returns the latest version.
     */
    public List<String> getAllVersions()
    {
        if (_versions != null) return _versions;
        return _versions = getVersionsImpl();
    }

    private List<String> getVersionsImpl()
    {
        // Get <version> XML elements
        XMLElement xml = getLocalArtifactFileXML();
        XMLElement versioningXML = xml != null ? xml.getElement("versioning") : null;
        XMLElement versionsXML = versioningXML != null ? versioningXML.getElement("versions") : null;
        List<XMLElement> versionXMLs = versionsXML != null ? versionsXML.getElements("version") : null;
        if (versionXMLs == null || versionXMLs.isEmpty())
            return Collections.emptyList();

        // Get version strings and return
        return ListUtils.mapNonNull(versionXMLs, MavenArtifact::getVersionStringForVersionXml);
    }

    private static String getVersionStringForVersionXml(XMLElement xml)
    {
        String version = xml.getValue();
        return !version.isBlank() ? version.trim() : null;
    }

    /**
     * Returns the local artifact file XML.
     */
    private XMLElement getLocalArtifactFileXML()
    {
        String xmlString;
        try { xmlString = getLocalArtifactFile().getText(); }
        catch (IOException e) {
            System.err.println(getClass().getSimpleName() + ".getXML: Can't read artifact file: " + e.getMessage());
            return null;
        }

        // Read and return
        try { return XMLElement.readXmlFromString(xmlString); }
        catch (Exception e) {
            System.err.println(getClass().getSimpleName() + ".getXML: Error reading artifact file: " + e.getMessage());
            System.err.println(e.getMessage());
            return null;
        }
    }

    /**
     * Returns the local artifact file (downloads if missing).
     */
    public WebFile getLocalArtifactFile() throws IOException
    {
        if (_localFile != null) return _localFile;
        return _localFile = getLocalArtifactFileImpl();
    }

    private synchronized WebFile getLocalArtifactFileImpl() throws IOException
    {
        // If local file already exists, just return
        String localFilePath = getLocalArtifactFilePath();
        WebFile localFile = WebFile.getFileForPath(localFilePath);
        if (localFile != null)
            return localFile;

        // Download file
        String remoteFileUrlString = getRemoteArtifactFileUrlString();
        URL remoteFileUrl = URI.create(remoteFileUrlString).toURL();
        DownloadFile.downloadUrlToLocalPath(remoteFileUrl, Path.of(localFilePath));

        // Return file which should exist now
        return WebFile.getFileForPath(localFilePath);
    }

    /**
     * Deletes the local artifact file.
     */
    void deleteLocalArtifactFile()
    {
        String localFilePath = getLocalArtifactFilePath();
        WebFile localFile = WebFile.getFileForPath(localFilePath);
        if (localFile == null)
            return;

        try { localFile.delete(); }
        catch (Exception e) { System.err.println(getClass().getSimpleName() + ": Delete local file failed: " + e.getMessage()); }
        _localFile = null;
    }

    /**
     * Returns the local maven artifact directory file.
     */
    WebFile getLocalArtifactDir()
    {
        String localMavenDirPath = getLocalArtifactDirPath();
        return WebFile.createFileForPath(localMavenDirPath, true);
    }

    /**
     * Returns the local maven artifact directory path string.
     */
    String getLocalArtifactDirPath()
    {
        // Get local maven cache directory path
        String homeDir = System.getProperty("user.home");
        String MAVEN_REPO_PATH = SnapEnv.isWebVM ? "maven_cache" : ".m2/repository";
        String localMavenCacheDir = FilePathUtils.getChildPath(homeDir, MAVEN_REPO_PATH);

        // Build path with /<group-id-path>/<artifact-id> and return
        String relativeArtifactDirPath = getRelativeArtifactDirPath();
        return FilePathUtils.getChildPath(localMavenCacheDir, relativeArtifactDirPath);
    }

    /**
     * Returns the local file path string.
     */
    String getLocalArtifactFilePath()
    {
        String localArtifactDirPath = getLocalArtifactDirPath();
        return FilePathUtils.getChildPath(localArtifactDirPath, METADATA_FILE_NAME);
    }

    /**
     * Returns the remote artifact metadata file URL string.
     */
    String getRemoteArtifactFileUrlString()
    {
        String remoteRepositoryDirURL = getRemoteRepositoryDirUrlString();
        String relativeArtifactFilePath = getRelativeArtifactDirPath() + '/' + METADATA_FILE_NAME;
        return FilePathUtils.getChildPath(remoteRepositoryDirURL, relativeArtifactFilePath);
    }

    /**
     * Returns the relative artifact directory path.
     */
    String getRelativeArtifactDirPath()  { return '/' + _groupId.replace(".", "/") + '/' + _artifactId; }

    /**
     * Returns the repository URL or default.
     */
    public String getRemoteRepositoryDirUrlString()
    {
        if (_groupId.toLowerCase().contains("reportmill"))
            return "https://reportmill.com/maven";
        String artifactId = _artifactId.toLowerCase();
        if (artifactId.contains("reportmill") || artifactId.contains("snapkit") || artifactId.contains("snapcharts"))
            return "https://reportmill.com/maven";

        if (SnapEnv.isWebVM)
            return WebUtils.getCorsProxyAddress(MAVEN_CENTRAL_URL);
        return MAVEN_CENTRAL_URL;
    }

    /**
     * Returns whether maven package is loaded.
     */
    public boolean isLoaded()  { return _loaded; }

    /**
     * Sets whether maven package is loaded.
     */
    private synchronized void setLoaded(boolean aValue)
    {
        if (aValue == _loaded) return;
        firePropChange(Loaded_Prop, _loaded, _loaded = aValue);
    }

    /**
     * Returns whether maven package is loading.
     */
    public boolean isLoading()  { return _loading; }

    /**
     * Sets whether maven package is loading.
     */
    private void setLoading(boolean aValue)
    {
        if (aValue == _loading) return;
        firePropChange(Loading_Prop, _loading, _loading = aValue);
    }

    /**
     * Loads package files.
     */
    public synchronized void loadPackageFiles()
    {
        if (isLoaded())
            return;

        try {

            // Set loading
            setLoaded(false);
            setLoading(true);
            _error = null;

            // Fetch metadata file
            getLocalArtifactFile();

            setLoaded(true);
        }

        // Handle errors
        catch (Exception e) { _error = "Error: " + e.getMessage(); }

        // Reset Loading
        finally { setLoading(false); }
    }

    /**
     * Returns the error.
     */
    public String getError()
    {
        if (_error != null) return _error;
        return _error = getErrorImpl();
    }

    /**
     * Returns the error.
     */
    private String getErrorImpl()
    {
        if (_groupId == null || _groupId.isEmpty())
            return "Invalid group id";
        if (_artifactId == null || _artifactId.isEmpty())
            return "Invalid artifact id";
        return null;
    }

    @Override
    public String toString()  { return "MavenArtifact: " + getId(); }

    /**
     * Returns the package for given id.
     */
    public static MavenArtifact getMavenArtifactForId(String mavenId)
    {
        MavenArtifact mavenArtifact = _artifacts.get(mavenId);
        if (mavenArtifact != null)
            return mavenArtifact;

        String fullArtifactId = getFullArtifactId(mavenId);
        if (fullArtifactId == null)
            return null;
        if (!fullArtifactId.equals(mavenId)) {
            mavenArtifact = getMavenArtifactForId(fullArtifactId);
            _artifacts.put(mavenId, mavenArtifact);
            return mavenArtifact;
        }

        mavenArtifact = new MavenArtifact(fullArtifactId);
        _artifacts.put(fullArtifactId, mavenArtifact);
        return mavenArtifact;
    }

    private static String getFullArtifactId(String artifactId)
    {
        String[] names = artifactId.split(":");
        if (names.length < 2 || names[0].isBlank() || names[1].isBlank())
            return null;
        return names[0] + ":" + names[1];
    }
}
