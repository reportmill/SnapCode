package snapcode.project;
import snap.props.PropSet;
import snap.util.*;
import snap.web.WebFile;
import snap.web.WebUtils;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a Maven dependency.
 */
public class MavenDependency extends BuildDependency {

    // The group name
    private String _group;

    // The product name
    private String _name;

    // The version name
    private String _version;

    // The classifier
    private String _classifier;

    // The repository url
    private String _repositoryURL;

    // The id string
    private String _id;

    // Whether dependency is loaded
    private boolean _loaded;

    // The Jar file
    private MavenFile _jarFile;

    // The POM file
    private MavenPomFile _pomFile;

    // Load listeners
    private Runnable[] _loadLsnrs = new Runnable[0];

    // Whether dependency is loading
    private boolean _loading;

    // The error string
    private String _error;

    // Constants for properties
    public static final String Group_Prop = "Group";
    public static final String Name_Prop = "Name";
    public static final String Version_Prop = "Version";
    public static final String Classifier_Prop = "Classifier";
    public static final String RepositoryURL_Prop = "RepositoryURL";
    public static final String Loaded_Prop = "Loaded";
    public static final String Loading_Prop = "Loading";

    // Constant for Maven central URL
    public static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";

    /**
     * Constructor.
     */
    public MavenDependency()
    {
        super();
        _jarFile = new MavenFile(this, "jar");
        _pomFile = new MavenPomFile(this);
    }

    /**
     * Constructor with maven id.
     */
    public MavenDependency(String mavenId)
    {
        super();
        setId(mavenId);
        _jarFile = new MavenFile(this, "jar");
        _pomFile = new MavenPomFile(this);
    }

    /**
     * Returns the type.
     */
    public Type getType()  { return Type.Maven; }

    /**
     * Returns id string.
     */
    @Override
    public String getId()
    {
        if (_id != null) return _id;

        // If any part is invalid, just return
        if (_group == null || _group.isBlank() || _name == null || _name.isBlank() || _version == null || _version.isBlank())
            return null;

        // Create id string and return
        _id = _group + ":" + _name + ":" + _version;
        if (_classifier != null && !_classifier.isBlank())
            _id += ':' + _classifier;
        return _id;
    }

    /**
     * Sets properties for given id string.
     */
    public void setId(String aValue)
    {
        if (Objects.equals(aValue, _id)) return;
        _id = aValue;

        // Set Group, Name, Version
        String[] names = aValue.split(":");
        setGroup(names.length > 0 ? names[0] : null);
        setName(names.length > 1 ? names[1] : null);
        setVersion(names.length > 2 ? names[2] : null);
        setClassifier(names.length > 3 ? names[3] : null);
        setLoaded(false);
    }

    /**
     * Returns the group name.
     */
    public String getGroup()  { return _group; }

    /**
     * Sets the group name.
     */
    public void setGroup(String aValue)
    {
        if (Objects.equals(aValue, _group)) return;
        _classPaths = null;
        _id = null;
        _error = null;
        firePropChange(Group_Prop, _group, _group = aValue);
        setLoaded(false);
    }

    /**
     * Returns the product name.
     */
    public String getName()  { return _name; }

    /**
     * Sets the product name.
     */
    public void setName(String aValue)
    {
        if (Objects.equals(aValue, _name)) return;
        _classPaths = null; _id = null; _error = null;
        firePropChange(Name_Prop, _name, _name = aValue);
        setLoaded(false);
    }

    /**
     * Returns the version name.
     */
    public String getVersion()  { return _version; }

    /**
     * Sets the version name.
     */
    public void setVersion(String aValue)
    {
        if (Objects.equals(aValue, _version)) return;
        _classPaths = null; _id = null; _error = null;
        firePropChange(Version_Prop, _version, _version = aValue);
        setLoaded(false);
    }

    /**
     * Returns the classifier.
     */
    public String getClassifier()  { return _classifier; }

    /**
     * Sets the classifier.
     */
    public void setClassifier(String aValue)
    {
        if (Objects.equals(aValue, _classifier)) return;
        _classPaths = null; _id = null; _error = null;
        firePropChange(Classifier_Prop, _classifier, _classifier = aValue);
        setLoaded(false);
    }

    /**
     * Returns the repository name.
     */
    public String getRepositoryURL()  { return _repositoryURL; }

    /**
     * Sets the repository name.
     */
    public void setRepositoryURL(String aValue)
    {
        if (Objects.equals(aValue, _repositoryURL)) return;
        _classPaths = null;
        _error = null;
        firePropChange(RepositoryURL_Prop, _repositoryURL, _repositoryURL = aValue);
        setLoaded(false);
    }

    /**
     * Returns the status.
     */
    public String getStatus()
    {
        if (isLoaded())
            return "Loaded";
        if (isLoading())
            return "Loading";
        return "Error";
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

        // Handle true: Fire load listeners
        if (aValue) {

            // Fire load listeners
            for (Runnable loadLsnr : _loadLsnrs)
                loadLsnr.run();
            _loadLsnrs = new Runnable[0];
        }
    }

    /**
     * Returns the Jar file.
     */
    public MavenFile getJarFile()  { return _jarFile; }

    /**
     * Returns the POM file.
     */
    public MavenPomFile getPomFile()  { return _pomFile; }

    /**
     * Returns the transitive dependencies.
     */
    public List<MavenDependency> getTransitiveDependencies()
    {
        MavenPomFile pomFile = getPomFile();
        return pomFile.getDependencies();
    }

    /**
     * Adds a load listener.
     */
    public synchronized void addLoadListener(Runnable aRunnable)
    {
        if (isLoaded())
            aRunnable.run();
        else _loadLsnrs = ArrayUtils.add(_loadLsnrs, aRunnable);
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
     * Returns the repository URL or default.
     */
    public String getRepositoryUrlOrDefault()
    {
        if (_repositoryURL != null)
            return _repositoryURL;
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
     * Returns the name of the default repository.
     */
    public String getRepositoryDefaultName()
    {
        if (getRepositoryUrlOrDefault().contains("reportmill"))
            return "ReportMill";
        return "Maven Central";
    }

    /**
     * Override to get class paths for project.
     */
    @Override
    protected String[] getClassPathsImpl()
    {
        String localJarPath = getLocalFilePathForType("jar");
        if (localJarPath == null)
            return null;

        // Return
        return new String[] { localJarPath };
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
            List<MavenDependency> transitiveDependencies = getTransitiveDependencies();
            transitiveDependencies.forEach(MavenDependency::loadPackageFiles);

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
        preloadPackageFiles();
    }

    /**
     * Deletes package files.
     */
    public void deletePackageFiles()
    {
        List<MavenDependency> transitiveDependencies = getTransitiveDependencies();
        transitiveDependencies.forEach(MavenDependency::deletePackageFiles);
        getJarFile().deleteLocalFile();
        getPomFile().deleteLocalFile();
        setLoaded(false);
    }

    /**
     * Override to support props for this class.
     */
    @Override
    protected void initProps(PropSet aPropSet)
    {
        super.initProps(aPropSet);
        aPropSet.addPropNamed(Group_Prop, String.class);
        aPropSet.addPropNamed(Name_Prop, String.class);
        aPropSet.addPropNamed(Version_Prop, String.class);
        aPropSet.addPropNamed(Classifier_Prop, String.class);
        aPropSet.addPropNamed(RepositoryURL_Prop, String.class);
    }

    /**
     * Override to support props for this class.
     */
    @Override
    public Object getPropValue(String aPropName)
    {
        return switch (aPropName) {

            // Group, Name, Version, Classifier, RepositoryURL
            case Group_Prop -> getGroup();
            case Name_Prop -> getName();
            case Version_Prop -> getVersion();
            case Classifier_Prop -> getClassifier();
            case RepositoryURL_Prop -> getRepositoryURL();

            // Do normal version
            default -> super.getPropValue(aPropName);
        };
    }

    /**
     * Override to support props for this class.
     */
    @Override
    public void setPropValue(String aPropName, Object aValue)
    {
        switch (aPropName) {

            // Group, Name, Version, Classifier, RepositoryURL
            case Group_Prop -> setGroup(Convert.stringValue(aValue));
            case Name_Prop -> setName(Convert.stringValue(aValue));
            case Version_Prop -> setVersion(Convert.stringValue(aValue));
            case Classifier_Prop -> setClassifier(Convert.stringValue(aValue));
            case RepositoryURL_Prop -> setRepositoryURL(Convert.stringValue(aValue));

            // Do normal version
            default -> super.setPropValue(aPropName, aValue);
        }
    }
}
