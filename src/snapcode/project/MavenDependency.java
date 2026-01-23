package snapcode.project;
import snap.props.PropSet;
import snap.util.*;
import snap.web.WebFile;
import snap.web.WebResponse;
import snap.web.WebURL;
import snap.web.WebUtils;

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

    // The classifier
    private String _classifier;

    // The repository url
    private String _repositoryURL;

    // The id string
    private String _id;

    // Whether dependency is loaded
    private boolean _loaded;

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
    public WebURL getRemoteJarUrl()
    {
        String urlString = getRemoteJarUrlString();
        return WebURL.getUrl(urlString);
    }

    /**
     * Returns the local Jar file, triggering load if missing.
     */
    public WebFile getLocalJarFile()
    {
        // Create local jar file
        String localJarPath = getLocalJarPath();
        WebFile localJarFile = WebFile.createFileForPath(localJarPath, false);

        // If file doesn't exist, load it
        if (localJarFile != null) {
             if (!localJarFile.getExists())
                loadPackageFiles();
             else setLoaded(true);
        }

        // Return
        return localJarFile;
    }

    /**
     * Returns the pom file URL in remote repository.
     */
    public WebURL getRemotePomUrl()
    {
        String pomUrlString = getRemoteJarUrlString().replace(".jar", ".pom");
        if (_classifier != null && !_classifier.isBlank())
            pomUrlString = pomUrlString.replace('-' + _classifier, "");
        return WebURL.getUrl(pomUrlString);
    }

    /**
     * Returns the local pom file, triggering load if missing.
     */
    public WebFile getLocalPomFile()
    {
        // Create local jar file
        String localPomPath = getLocalJarPath().replace(".jar", ".pom");
        if (_classifier != null && !_classifier.isBlank())
            localPomPath = localPomPath.replace('-' + _classifier, "");
        WebFile localPomFile = WebFile.createFileForPath(localPomPath, false);

        // If file doesn't exist, load it
        if (localPomFile != null) {
             if (!localPomFile.getExists())
                loadPackageFiles();
             else setLoaded(true);
        }

        // Return
        return localPomFile;
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
        return FilePathUtils.getChildPath(repositoryURL, relativeJarPath);
    }

    /**
     * Returns the remote Jar URL string.
     */
    public String getLocalJarPath()
    {
        // Get local maven cache path
        String homeDir = System.getProperty("user.home");
        String MAVEN_REPO_PATH = SnapEnv.isWebVM ? "maven_cache" : ".m2/repository";
        String localMavenCachePath = FilePathUtils.getChildPath(homeDir, MAVEN_REPO_PATH);

        // Get relative jar path
        String relativeJarPath = getRelativeJarPath();
        if (relativeJarPath == null)
            return null;

        // Return combined path
        return FilePathUtils.getChildPath(localMavenCachePath, relativeJarPath);
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
        if (group == null || group.isEmpty() || packageName == null || packageName.isEmpty() ||
                version == null || version.isEmpty())
            return null;

        // Build relative package jar path and return
        String groupPath = '/' + group.replace(".", "/");
        String packagePath = FilePathUtils.getChildPath(groupPath, packageName);
        String versionPath = FilePathUtils.getChildPath(packagePath, version);

        // Get jar filename
        String jarName = packageName + '-' + version;
        if (_classifier != null && !_classifier.isBlank())
            jarName += '-' + _classifier;
        String jarFilename = jarName + ".jar";

        // Return jar path
        return FilePathUtils.getChildPath(versionPath, jarFilename);
    }

    /**
     * Loads file.
     */
    public void loadPackageFiles()
    {
        // If already loading, just return
        if (isLoading())
            return;

        // Set Loading true and start thread
        new Thread(() -> loadPackageFilesImpl()).start();
    }

    /**
     * Loads package files.
     */
    private synchronized void loadPackageFilesImpl()
    {
        try {

            // Set loading
            setLoaded(false);
            setLoading(true);
            _error = null;

            // Get remote and local jar file urls - if either is null, just return
            WebURL remoteJarURL = getRemoteJarUrl();
            WebFile localJarFile = getLocalJarFile();
            if (remoteJarURL == null || localJarFile == null)
                return;

            // Fetch file
            copyUrlToFile(remoteJarURL, localJarFile);
            setLoaded(true);

            // Get remote and local jar file urls - if either is null, just return
            WebURL remotePomUrl = getRemotePomUrl();
            WebFile localPomFile = getLocalPomFile();
            if (remotePomUrl != null && localPomFile != null)
                copyUrlToFile(remotePomUrl, localPomFile);
        }

        // Handle errors
        catch (Exception e) {
            //e.printStackTrace();
            _error = "Error: " + e.getMessage();
        }

        // Reset Loading and wake waitForLoad thread(s)
        finally {
            setLoading(false);
            notifyAll();
        }
    }

    /**
     * Waits for dependency to load.
     */
    public synchronized void waitForLoad()
    {
        if (!isLoaded() || isLoading()) {
            loadPackageFiles();
            try { wait(); }
            catch (Exception e) { System.out.println("MavenDependency.waitForLoad: Failure: " + e.getMessage()); }
        }
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

    /**
     * Copies a given source URL to given destination file.
     */
    private static void copyUrlToFile(WebURL sourceURL, WebFile destFile) throws IOException
    {
        // Get bytes from source url
        byte[] sourceBytes = sourceURL.getBytesOrThrow();
        if (sourceBytes == null || sourceBytes.length == 0)
            throw new RuntimeException("Couldn't download remote jar file: " + sourceURL.getString());

        // Get old size. Shouldn't need to delete existing, but seemed to be corruption problem on WebVM.
        long oldSize = 0;          // Maybe gone now that FileSite just does writeBytes()
        if (destFile.getExists()) {
            oldSize = destFile.getSize();
            destFile.delete();
        }

        // Set source bytes in destination file and save
        destFile.setBytes(sourceBytes);
        WebResponse resp = destFile.save();
        if (resp.getException() != null)
            throw new RuntimeException(resp.getException());

        // Log change
        System.out.println("MavenDependency: Updated " + destFile.getPath() + ", old-size: " + oldSize + ", new-size: " + sourceBytes.length);
    }
}
