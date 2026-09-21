package snapcode.project;
import snap.props.PropObject;
import snap.util.FilePathUtils;
import snap.util.SnapEnv;
import snap.web.WebFile;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * This class represents a Maven package.
 */
public class MavenPackage extends PropObject {

    // The artifact
    private MavenArtifact _mavenArtifact;

    // The package id string
    private String _id;

    // The group id string
    private String _groupId;

    // The artifact id string
    private String _artifactId;

    // The version name
    private String _version;

    // The classifier
    private String _classifier;

    // The Jar file
    private MavenFile _jarFile;

    // The POM file
    private MavenPomFile _pomFile;

    // The class path
    private String _classPath;

    // Whether dependency is loaded
    private boolean _loaded;

    // Whether dependency is loading
    private boolean _loading;

    // The error string
    private String _error;

    // A map of all packages
    private static Map<String, MavenPackage> _packages = new HashMap<>();

    // Constants for properties
    public static final String Loaded_Prop = "Loaded";
    public static final String Loading_Prop = "Loading";

    /**
     * Constructor with maven id.
     */
    private MavenPackage(String mavenId)
    {
        super();
        _id = mavenId;

        // Set Group, Name, Version
        String[] names = mavenId.split(":");
        _groupId = names.length > 0 ? names[0] : null;
        _artifactId = names.length > 1 ? names[1] : null;
        _version = names.length > 2 ? names[2] : null;
        _classifier = names.length > 3 ? names[3] : null;

        _mavenArtifact = MavenArtifact.getMavenArtifactForId(_groupId + ':' + _artifactId);
    }

    /**
     * Returns id string.
     */
    public String getId()  { return _id; }

    /**
     * Returns the group name.
     */
    public String getGroupId()  { return _groupId; }

    /**
     * Returns the product name.
     */
    public String getArtifactId()  { return _artifactId; }

    /**
     * Returns the version name.
     */
    public String getVersion()  { return _version; }

    /**
     * Returns the classifier.
     */
    public String getClassifier()  { return _classifier; }

    /**
     * Returns the artifact.
     */
    public MavenArtifact getMavenArtifact()  { return _mavenArtifact; }

    /**
     * Returns the Jar file.
     */
    public MavenFile getJarFile()
    {
        if (_jarFile != null) return _jarFile;
        return _jarFile = new MavenFile(this, "jar");
    }

    /**
     * Returns the POM file.
     */
    public MavenPomFile getPomFile()
    {
        if (_pomFile != null) return _pomFile;
        return _pomFile = new MavenPomFile(this);
    }

    /**
     * Returns the transitive dependencies.
     */
    public List<MavenDependency> getDependencies()
    {
        MavenPomFile pomFile = getPomFile();
        return pomFile.getDependencies();
    }

    /**
     * Returns the class path for this dependency.
     */
    public String getClassPath()
    {
        if (_classPath != null) return _classPath;
        return _classPath = getLocalFilePathForType("jar");
    }

    /**
     * Returns the local maven directory file.
     */
    public WebFile getLocalMavenDir()
    {
        String localMavenDirPath = getLocalFilePathForType(null);
        return WebFile.createFileForPath(localMavenDirPath, true);
    }

    /**
     * Returns the remote file URL string.
     */
    String getRemoteFileUrlStringForType(String fileType)
    {
        String repositoryURL = _mavenArtifact.getRepositoryUrlOrDefault();
        String relativeFilePath = getRelativeFilePathForType(fileType);
        if (repositoryURL == null || relativeFilePath == null)
            return null;
        return FilePathUtils.getChildPath(repositoryURL, relativeFilePath);
    }

    /**
     * Returns the local file path string.
     */
    String getLocalFilePathForType(String fileType)
    {
        // Get local maven cache path
        String homeDir = System.getProperty("user.home");
        String MAVEN_REPO_PATH = SnapEnv.isWebVM ? "maven_cache" : ".m2/repository";
        String localMavenCachePath = FilePathUtils.getChildPath(homeDir, MAVEN_REPO_PATH);

        // Get relative file path
        String relativeFilePath = getRelativeFilePathForType(fileType);
        if (relativeFilePath == null)
            return null;

        // Return combined path
        return FilePathUtils.getChildPath(localMavenCachePath, relativeFilePath);
    }

    /**
     * Returns the relative file path (from any maven root).
     */
    private String getRelativeFilePathForType(String fileType)
    {
        // Get artifact path
        String artifactPath = _mavenArtifact.getRelativeFilePathForFilename(null);
        if (artifactPath == null)
            return null;

        // Build relative package jar path and return
        String version = getVersion();
        String versionPath = version != null ? FilePathUtils.getChildPath(artifactPath, version) : null;
        if (fileType == null)
            return versionPath;

        // Get filename
        String filenameSimple = getArtifactId() + '-' + version;
        if (_classifier != null && !_classifier.isBlank() && fileType.equals("jar"))
            filenameSimple += '-' + _classifier;
        String filename = filenameSimple + '.' + fileType;

        // Return path
        return FilePathUtils.getChildPath(versionPath, filename);
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

            // Load jar file and pom file
            getJarFile().downloadFile();
            getPomFile().downloadFile();

            setLoaded(true);
        }

        // Handle errors
        catch (Exception e) { _error = "Error: " + e.getMessage(); }

        // Reset Loading
        finally { setLoading(false); }
    }

    /**
     * Reloads files.
     */
    public void reloadPackageFiles()
    {
        deletePackageFiles();
        if (!isLoaded())
            CompletableFuture.runAsync(this::loadPackageFiles);
    }

    /**
     * Deletes package files.
     */
    public void deletePackageFiles()
    {
        getJarFile().deleteLocalFile();
        getPomFile().deleteLocalFile();
        setLoaded(false);
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
            return "Invalid group";
        if (_artifactId == null || _artifactId.isEmpty())
            return "Invalid package name";
        if (_version == null || _version.isEmpty())
            return "Invalid version";
        return null;
    }

    /**
     * Returns the package for given id.
     */
    public static MavenPackage getMavenPackageForId(String mavenId)
    {
        MavenPackage mavenPackage = _packages.get(mavenId);
        if (mavenPackage != null)
            return mavenPackage;
        mavenPackage = new MavenPackage(mavenId);
        _packages.put(mavenId, mavenPackage);
        return mavenPackage;
    }

    @Override
    public String toString()  { return "MavenPackage: " + getId(); }
}
