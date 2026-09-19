package snapcode.project;
import snap.props.PropObject;
import snap.util.FilePathUtils;
import snap.util.SnapEnv;
import snap.web.WebFile;
import snap.web.WebUtils;
import java.util.*;

/**
 * This class represents a Maven artifact (a group-id + artifact-id).
 */
public class MavenArtifact extends PropObject {

    // The id string
    private String _id;

    // The group id string
    private String _groupId;

    // The artifact id string
    private String _artifactId;

    // The package metadata file (e.g.: /group/artifact/maven-metadata.xml)
    private MavenArtifactMetadata _metadataFile;

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

    // Constant for Maven central URL
    public static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";

    /**
     * Constructor with maven id.
     */
    private MavenArtifact(String mavenId)
    {
        super();
        setId(mavenId);
    }

    /**
     * Returns id string.
     */
    public String getId()  { return _id; }

    /**
     * Sets properties for given id string.
     */
    private void setId(String aValue)
    {
        if (Objects.equals(aValue, _id)) return;

        // Set Group, Name
        String[] names = aValue.split(":");
        _groupId = names.length > 0 ? names[0] : null;
        _artifactId = names.length > 1 ? names[1] : null;
        _id = _groupId + ":" + _artifactId;
    }

    /**
     * Returns the group id string.
     */
    public String getGroupId()  { return _groupId; }

    /**
     * Returns the artifact id string.
     */
    public String getArtifactId()  { return _artifactId; }

    /**
     * Returns the artifact metadata file (e.g.: /group/artifact/maven-metadata.xml).
     */
    public MavenArtifactMetadata getMetadataFile()
    {
        if (_metadataFile != null) return _metadataFile;
        return _metadataFile = new MavenArtifactMetadata(this);
    }

    /**
     * Returns the repository URL or default.
     */
    public String getRepositoryUrlOrDefault()
    {
        if (_artifactId != null) {
            String name = _artifactId.toLowerCase();
            if (name.contains("reportmill") || name.contains("snapkit") || name.contains("snapcharts"))
                return "https://reportmill.com/maven";
            String group = _groupId.toLowerCase();
            if (group.contains("reportmill"))
                return "https://reportmill.com/maven";
        }
        if (SnapEnv.isWebVM)
            return WebUtils.getCorsProxyAddress(MAVEN_CENTRAL_URL);
        return MAVEN_CENTRAL_URL;
    }

    /**
     * Returns the local maven directory file.
     */
    public WebFile getLocalMavenDir()
    {
        String localMavenDirPath = getLocalFilePathForFilename(null);
        return WebFile.createFileForPath(localMavenDirPath, true);
    }

    /**
     * Returns the remote file URL string.
     */
    public String getRemoteFileUrlStringForFilename(String filename)
    {
        String repositoryURL = getRepositoryUrlOrDefault();
        String relativeFilePath = getRelativeFilePathForFilename(filename);
        if (repositoryURL == null || relativeFilePath == null)
            return null;
        return FilePathUtils.getChildPath(repositoryURL, relativeFilePath);
    }

    /**
     * Returns the local file path string.
     */
    public String getLocalFilePathForFilename(String filename)
    {
        // Get local maven cache path
        String homeDir = System.getProperty("user.home");
        String MAVEN_REPO_PATH = SnapEnv.isWebVM ? "maven_cache" : ".m2/repository";
        String localMavenCachePath = FilePathUtils.getChildPath(homeDir, MAVEN_REPO_PATH);

        // Get relative file path
        String relativeFilePath = getRelativeFilePathForFilename(filename);
        if (relativeFilePath == null)
            return null;

        // Return combined path
        return FilePathUtils.getChildPath(localMavenCachePath, relativeFilePath);
    }

    /**
     * Returns the relative file path (from any maven root).
     */
    String getRelativeFilePathForFilename(String filename)
    {
        // Get parts - if any are null, return null
        String group = getGroupId();
        String packageName = getArtifactId();
        if (group == null || group.isEmpty() || packageName == null || packageName.isEmpty())
            return null;

        // Build relative package jar path and return
        String groupPath = '/' + group.replace(".", "/");
        String artifactPath = FilePathUtils.getChildPath(groupPath, packageName);
        if(filename == null)
            return artifactPath;

        // Return artifact path + filename
        return FilePathUtils.getChildPath(artifactPath, filename);
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

            // Load metadata file
            getMetadataFile().downloadFile();

            setLoaded(true);
        }

        // Handle errors
        catch (Exception e) { _error = "Error: " + e.getMessage(); }

        // Reset Loading
        finally { setLoading(false); }
    }

    /**
     * Pre-Loads files in background.
     */
    public void preloadPackageFiles()
    {
        // If already loading, just return
        if (isLoaded() || isLoading())
            return;

        // Set Loading true and start thread
        new Thread(this::loadPackageFiles).start();
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
