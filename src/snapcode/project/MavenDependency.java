package snapcode.project;
import snap.props.PropSet;
import snap.util.Convert;
import snap.util.FilePathUtils;
import snap.util.SnapUtils;
import snap.web.WebFile;
import snap.web.WebResponse;
import snap.web.WebSite;
import snap.web.WebURL;
import java.io.IOException;
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

    // The repository url
    private String _repositoryURL;

    // The id string
    private String _id;

    // Whether dependency is loading
    private boolean _loading;

    // The error string
    private String _error;

    // Constants for properties
    public static final String Group_Prop = "Group";
    public static final String Name_Prop = "Name";
    public static final String Version_Prop = "Version";
    public static final String RepositoryURL_Prop = "RepositoryURL";
    public static final String Loading_Prop = "Loading";

    // Constant for Maven central URL
    public static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";

    // Constant for proxy server for CORS access found by googling for 'free cors proxy server'
    private static final String CORS_PROXY_SERVER = "https://corsproxy.io/?";

    /**
     * Constructor.
     */
    public MavenDependency()
    {
        super();
    }

    /**
     * Constructor with maven id.
     */
    public MavenDependency(String mavenId)
    {
        super();
        setId(mavenId);
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
        if (_group == null || _name == null || _version == null)
            return null;
        if (_group.length() == 0 || _name.length() == 0 || _version.length() == 0)
            return null;

        // Create id string and return
        return _id = _group + ":" + _name + ":" + _version;
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
        _classPaths = null;
        _id = null;
        _error = null;
        firePropChange(Name_Prop, _name, _name = aValue);
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
        _classPaths = null;
        _id = null;
        _error = null;
        firePropChange(Version_Prop, _version, _version = aValue);
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
    }

    /**
     * Returns the status.
     */
    public String getStatus()
    {
        WebFile localJarFile = getLocalJarFile();
        if (localJarFile != null)
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
        if (_group == null || _group.length() == 0)
            return "Invalid group";
        if (_name == null || _name.length() == 0)
            return "Invalid package name";
        if (_version == null || _version.length() == 0)
            return "Invalid version";
        return null;
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
        if (SnapUtils.isWebVM)
            return CORS_PROXY_SERVER + MAVEN_CENTRAL_URL;
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
        // Get local jar file (just return if it doesn't exist)
        WebFile localJarFile = getLocalJarFile();
        if (localJarFile == null)
            return null;

        // Return
        String localJarPath = getLocalJarPath();
        return new String[]{localJarPath};
    }

    /**
     * Returns the jar URL in remote repository.
     */
    public WebURL getRemoteJarURL()
    {
        String urlString = getRemoteJarUrlString();
        return WebURL.getURL(urlString);
    }

    /**
     * Returns the jar URL for local cache directory file.
     */
    public WebURL getLocalJarURL()
    {
        String urlString = getLocalJarPath();
        return WebURL.getURL(urlString);
    }

    /**
     * Returns the local Jar file, fetching it if missing.
     */
    public WebFile getLocalJarFile()
    {
        // If loading, return null
        if (isLoading() || getError() != null)
            return null;

        // Get local Jar URL (just return if that can't be created)
        WebURL localJarURL = getLocalJarURL();
        if (localJarURL == null)
            return null;

        // Get local Jar file (just return if local file already exists)
        WebFile localJarFile = localJarURL.getFile();
        if (localJarFile != null)
            return localJarFile;

        // Copy maven package to local cache dir
        loadPackageFiles();

        // Return
        return localJarURL.getFile();
    }

    /**
     * Returns the remote Jar URL string.
     */
    public String getRemoteJarUrlString()
    {
        String repositoryURL = getRepositoryUrlOrDefault();
        String relativeJarPath = getRelativeJarPath();
        if (repositoryURL == null || relativeJarPath == null)
            return null;
        return FilePathUtils.getChild(repositoryURL, relativeJarPath);
    }

    /**
     * Returns the remote Jar URL string.
     */
    public String getLocalJarPath()
    {
        // Get local maven cache path
        String homeDir = System.getProperty("user.home");
        String MAVEN_REPO_PATH = "maven_cache";
        String localMavenCachePath = FilePathUtils.getChild(homeDir, MAVEN_REPO_PATH);

        // Get relative jar path
        String relativeJarPath = getRelativeJarPath();
        if (relativeJarPath == null)
            return null;

        // Return combined path
        return FilePathUtils.getChild(localMavenCachePath, relativeJarPath);
    }

    /**
     * Returns the relative Jar path (from any maven root).
     */
    private String getRelativeJarPath()
    {
        // Get parts - if any are null, return null
        String group = getGroup();
        String packageName = getName();
        String version = getVersion();
        if (group == null || group.length() == 0 ||
                packageName == null || packageName.length() == 0 ||
                version == null || version.length() == 0)
            return null;

        // Build relative package jar path and return
        String groupPath = '/' + group.replace(".", "/");
        String packagePath = FilePathUtils.getChild(groupPath, packageName);
        String versionPath = FilePathUtils.getChild(packagePath, version);
        String jarName = packageName + '-' + version + ".jar";
        return FilePathUtils.getChild(versionPath, jarName);
    }

    /**
     * Loads file.
     */
    public void loadPackageFiles()
    {
        // If already loading, just return
        if (isLoading())
            return;

        // Set loading
        setLoading(true);

        // Set Loading true and start thread
        new Thread(() -> loadPackageFilesImpl()).start();
    }

    /**
     * Loads package files.
     */
    private void loadPackageFilesImpl()
    {
        // Get remote and local jar file urls - if either is null, just return
        WebURL remoteJarURL = getRemoteJarURL();
        WebURL localJarURL = getLocalJarURL();
        if (remoteJarURL == null || localJarURL == null)
            return;

        // Fetch file
        try { copyFileForURLs(remoteJarURL, localJarURL); }
        catch (Exception e) {
            e.printStackTrace();
            _error = e.getMessage();
        }

        // Reset loading
        setLoading(false);
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
        aPropSet.addPropNamed(RepositoryURL_Prop, String.class);
    }

    /**
     * Override to support props for this class.
     */
    @Override
    public Object getPropValue(String aPropName)
    {
        switch (aPropName) {

            // Group, Name, Version, RepositoryURL
            case Group_Prop: return getGroup();
            case Name_Prop: return getName();
            case Version_Prop: return getVersion();
            case RepositoryURL_Prop: return getRepositoryURL();

            // Do normal version
            default: return super.getPropValue(aPropName);
        }
    }

    /**
     * Override to support props for this class.
     */
    @Override
    public void setPropValue(String aPropName, Object aValue)
    {
        switch (aPropName) {

            // Group, Name, Version, RepositoryURL
            case Group_Prop: setGroup(Convert.stringValue(aValue)); break;
            case Name_Prop: setName(Convert.stringValue(aValue)); break;
            case Version_Prop: setVersion(Convert.stringValue(aValue)); break;
            case RepositoryURL_Prop: setRepositoryURL(Convert.stringValue(aValue)); break;

            // Do normal version
            default: super.setPropValue(aPropName, aValue);
        }
    }

    /**
     * Copies a given source URL to given destination url.
     */
    private static void copyFileForURLs(WebURL sourceURL, WebURL destURL) throws IOException
    {
        // Get bytes from source url
        byte[] sourceBytes = sourceURL.getBytesOrThrow();
        if (sourceBytes == null || sourceBytes.length == 0)
            throw new RuntimeException("Couldn't download remote jar file: " + sourceURL.getString());

        // Create destination file
        WebSite destSite = destURL.getSite();
        String destFilePath = destURL.getPath();
        WebFile destFile = destSite.createFileForPath(destFilePath, false);

        // Set source bytes in destination file and save
        destFile.setBytes(sourceBytes);
        WebResponse resp = destFile.save();
        if (resp.getException() != null)
            throw new RuntimeException(resp.getException());
        System.out.println("MavenDependency: Updated " + destFilePath + ", size: " + sourceBytes.length);
    }
}
