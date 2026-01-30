package snapcode.project;
import snap.props.PropObject;
import snap.util.FilePathUtils;
import snap.util.SnapEnv;
import snap.web.WebFile;
import snap.web.WebUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class represents a Maven dependency.
 */
public class MavenPackage extends PropObject {

    // The id string
    private String _id;

    // The group name
    private String _group;

    // The product name
    private String _name;

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

    // Cached artifact id
    private String _artifactId;

    // A map of all packages
    private static Map<String, MavenPackage> _packages = new HashMap<>();

    // Constants for properties
    public static final String Loaded_Prop = "Loaded";
    public static final String Loading_Prop = "Loading";

    // Constant for Maven central URL
    public static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";

    /**
     * Constructor with maven id.
     */
    private MavenPackage(String mavenId)
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
        _id = aValue;

        // Set Group, Name, Version
        String[] names = aValue.split(":");
        _group = names.length > 0 ? names[0] : null;
        _name = names.length > 1 ? names[1] : null;
        _version = names.length > 2 ? names[2] : null;
        _classifier = names.length > 3 ? names[3] : null;
    }

    /**
     * Returns the group name.
     */
    public String getGroup()  { return _group; }

    /**
     * Returns the product name.
     */
    public String getName()  { return _name; }

    /**
     * Returns the version name.
     */
    public String getVersion()  { return _version; }

    /**
     * Returns the classifier.
     */
    public String getClassifier()  { return _classifier; }

    /**
     * Returns the artifact id.
     */
    public String getArtifactId()
    {
        if (_artifactId != null) return _artifactId;
        return _artifactId = _group + ":" + _name;
    }

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
    public List<MavenPackage> getTransitiveDependencies()
    {
        MavenPomFile pomFile = getPomFile();
        return pomFile.getDependencies();
    }

    /**
     * Returns the repository URL or default.
     */
    public String getRepositoryUrlOrDefault()
    {
        if (_name != null) {
            String name = _name.toLowerCase();
            if (name.contains("reportmill") || name.contains("snapkit") || name.contains("snapcharts"))
                return "https://reportmill.com/maven";
            String group = _group.toLowerCase();
            if (group.contains("reportmill"))
                return "https://reportmill.com/maven";
        }
        if (SnapEnv.isWebVM)
            return WebUtils.getCorsProxyAddress(MAVEN_CENTRAL_URL);
        return MAVEN_CENTRAL_URL;
    }

    /**
     * Returns the class path for this dependency.
     */
    public String getClassPath()
    {
        if (_classPath != null) return _classPath;
        String localJarPath = getLocalFilePathForType("jar");
        if (localJarPath == null)
            return null;

        // Return
        return _classPath = localJarPath;
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
    public String getRemoteFileUrlStringForType(String fileType)
    {
        String repositoryURL = getRepositoryUrlOrDefault();
        String relativeFilePath = getRelativeFilePathForType(fileType);
        if (repositoryURL == null || relativeFilePath == null)
            return null;
        return FilePathUtils.getChildPath(repositoryURL, relativeFilePath);
    }

    /**
     * Returns the local file path string.
     */
    public String getLocalFilePathForType(String fileType)
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
        // Get parts - if any are null, return null
        String group = getGroup();
        String packageName = getName();
        String version = getVersion();
        if (group == null || group.isEmpty() || packageName == null || packageName.isEmpty() ||
                version == null || version.isEmpty())
            return null;

        // Build relative package jar path and return
        String groupPath = '/' + group.replace(".", "/");
        String packagePath = FilePathUtils.getChildPath(groupPath, packageName);
        String versionPath = FilePathUtils.getChildPath(packagePath, version);
        if (fileType == null)
            return versionPath;

        // Get filename
        String filenameSimple = packageName + '-' + version;
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
    protected synchronized void setLoaded(boolean aValue)
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
    protected void setLoading(boolean aValue)
    {
        if (aValue == _loading) return;
        firePropChange(Loading_Prop, _loading, _loading = aValue);
    }

    /**
     * Loads package files.
     */
    public synchronized void loadPackageFiles()
    {
        // If already loaded, just return
        if (isLoaded())
            return;

        try {

            // Set loading
            setLoaded(false);
            setLoading(true);
            _error = null;

            // Load jar file
            getJarFile().getLocalFile();

            // Load transitive dependencies
            List<MavenPackage> transitiveDependencies = getTransitiveDependencies();
            transitiveDependencies.forEach(MavenPackage::loadPackageFiles);

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
     * Reloads files.
     */
    public void reloadPackageFiles()
    {
        deletePackageFiles();
        preloadPackageFiles();
    }

    /**
     * Deletes package files.
     */
    public void deletePackageFiles()
    {
        List<MavenPackage> transitiveDependencies = getTransitiveDependencies();
        transitiveDependencies.forEach(MavenPackage::deletePackageFiles);
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
        if (_group == null || _group.isEmpty())
            return "Invalid group";
        if (_name == null || _name.isEmpty())
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
}
