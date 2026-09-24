package snapcode.project;
import snap.props.PropObject;
import snap.web.WebFile;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * This class represents a Maven package.
 */
public class MavenPackage extends PropObject {

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

    // The artifact
    private MavenArtifact _mavenArtifact;

    // The properties
    private Map<String,String> _properties;

    // The dependencies
    private List<MavenDependency> _dependencies;

    // The parent package
    private MavenPackage _parentPackage;

    // The local jar file
    private WebFile _localJarFile;

    // The local pom file
    private WebFile _localPomFile;

    // The class path
    private String _classPath;

    // Whether dependency is loaded
    private boolean _loaded;

    // Whether dependency is loading
    private boolean _loading;

    // The error string
    private String _error;

    // Helper class
    MavenPackageHelper _helper;

    // A map of all packages
    private static Map<String, MavenPackage> _packages = new HashMap<>();

    // Constant for maven id validator pattern
    private static Pattern MAVEN_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_.\\-]+:[a-zA-Z0-9_.\\-]+:[a-zA-Z0-9_.\\-]+$");

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
        String[] names = mavenId.split(":");
        _groupId = names[0];
        _artifactId = names[1];
        _version = names[2];
        _classifier = names.length > 3 ? names[3] : null;
        _mavenArtifact = MavenArtifact.getMavenArtifactForId(_groupId + ':' + _artifactId);
        _helper = new MavenPackageHelper(this);
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
     * Returns the properties.
     */
    public Map<String,String> getProperties()
    {
        if (_properties != null) return _properties;
        return _properties = _helper.getProperties();
    }

    /**
     * Returns the property value for given key.
     */
    public String getPropertyValueForKey(String key)
    {
        // Check properties
        Map<String,String> props = getProperties();
        String value = props.get(key);
        if (value != null)
            return value;

        // Check parent
        MavenPackage parentPackage = getParentPackage();
        if (parentPackage != null)
            value = parentPackage.getPropertyValueForKey(key);
        return value;
    }

    /**
     * Returns the transitive dependencies.
     */
    public List<MavenDependency> getDependencies()
    {
        if (_dependencies != null) return _dependencies;
        return _dependencies = _helper.getDependencies();
    }

    /**
     * Returns the parent package, if available.
     */
    public MavenPackage getParentPackage()
    {
        if (_parentPackage != null) return _parentPackage;
        return _parentPackage = _helper.getParentPackage();
    }

    /**
     * Returns the local Jar file.
     */
    public WebFile getLocalJarFile() throws IOException
    {
        if (_localJarFile != null) return _localJarFile;
        return _localJarFile = _helper.getLocalJarFile();
    }

    /**
     * Returns the local pom file.
     */
    public WebFile getLocalPomFile() throws IOException
    {
        if (_localPomFile != null) return _localPomFile;
        return _localPomFile = _helper.getLocalPomFile();
    }

    /**
     * Returns the class path for this dependency.
     */
    public String getClassPath()
    {
        if (_classPath != null) return _classPath;
        return _classPath = _helper.getLocalFilePathForType("jar");
    }

    /**
     * Returns the local maven directory file.
     */
    public WebFile getLocalMavenDir()
    {
        String localMavenDirPath = _helper.getLocalFilePathForType(null);
        return WebFile.createFileForPath(localMavenDirPath, true);
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
            getLocalJarFile();
            getLocalPomFile();

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
        String localJarFilePath = _helper.getLocalJarFilePath();
        WebFile localJarFile = WebFile.getFileForPath(localJarFilePath);
        if (localJarFile != null)
            localJarFile.delete();

        String localPomFilePath = _helper.getLocalPomFilePath();
        WebFile localPomFile = WebFile.getFileForPath(localPomFilePath);
        if (localPomFile != null)
            localPomFile.delete();

        setLoaded(false);
    }

    /**
     * Returns the error.
     */
    public String getError()  { return _error; }

    /**
     * Returns the package for given id.
     */
    public static MavenPackage getMavenPackageForId(String mavenId)
    {
        MavenPackage mavenPackage = _packages.get(mavenId);
        if (mavenPackage != null)
            return mavenPackage;
        if (!MAVEN_ID_PATTERN.matcher(mavenId).matches())
            return null;
        mavenPackage = new MavenPackage(mavenId);
        _packages.put(mavenId, mavenPackage);
        return mavenPackage;
    }

    @Override
    public String toString()  { return "MavenPackage: " + getId(); }
}
