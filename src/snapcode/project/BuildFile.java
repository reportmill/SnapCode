package snapcode.project;
import snap.props.PropArchiverJS;
import snap.props.PropObject;
import snap.props.PropSet;
import snap.util.ArrayUtils;
import snap.util.Convert;
import snap.util.FilePathUtils;
import snap.util.JSObject;
import snap.web.WebFile;
import snap.web.WebSite;
import java.util.Arrays;
import java.util.Objects;

/**
 * This class manages project properties.
 */
public class BuildFile extends PropObject {

    // The project
    private Project  _proj;

    // The project source path
    private String  _srcPath;

    // The project build path
    private String  _buildPath;

    // The dependencies
    private BuildDependency[] _dependencies;

    // The actual build file
    private WebFile _buildFile;

    // Constants
    public static final String BUILD_FILE_PATH = "/build.snapcode";

    // Constants for BuildFile properties
    public static final String SourcePath_Prop = "SourcePath";
    public static final String BuildPath_Prop = "BuildPath";
    public static final String Dependencies_Prop = "Dependencies";

    // Constants for defaults
    private static final String DEFAULT_SOURCE_PATH = "src";
    private static final String DEFAULT_BUILD_PATH = "bin";

    /**
     * Constructor.
     */
    public BuildFile()
    {
        super();

        // Set defaults
        _srcPath = DEFAULT_SOURCE_PATH;
        _buildPath = DEFAULT_BUILD_PATH;
        _dependencies = new BuildDependency[0];
    }

    /**
     * Constructor for project.
     */
    public BuildFile(Project aProj)
    {
        this();
        _proj = aProj;

        // Read actual build file if exists
        WebFile buildFile = getBuildFile();
        if (buildFile.getExists())
            readFile();
    }

    /**
     * Returns the source path.
     */
    public String getSourcePath()  { return _srcPath; }

    /**
     * Sets the source path.
     */
    public void setSourcePath(String aPath)
    {
        // If already set, just return
        if (Objects.equals(aPath, _srcPath)) return;

        // Set, firePropChange
        String newPath = aPath != null ? ProjectUtils.getRelativePath(_proj, aPath) : null;
        firePropChange(SourcePath_Prop, _srcPath, _srcPath = newPath);
    }

    /**
     * Returns the build path.
     */
    public String getBuildPath()  { return _buildPath; }

    /**
     * Sets the build path.
     */
    public void setBuildPath(String aPath)
    {
        // If already set, just return
        if (Objects.equals(aPath, _buildPath)) return;

        // Set, firePropChange
        String newPath = aPath != null ? ProjectUtils.getRelativePath(_proj, aPath) : null;
        firePropChange(BuildPath_Prop, _buildPath, _buildPath = newPath);
    }

    /**
     * Returns the dependencies.
     */
    public BuildDependency[] getDependencies()  { return _dependencies; }

    /**
     * Sets the dependencies.
     */
    public void setDependencies(BuildDependency[] theDependencies)
    {
        if (Arrays.equals(theDependencies, _dependencies)) return;
        firePropChange(Dependencies_Prop, _dependencies, _dependencies = theDependencies);
    }

    /**
     * Adds a dependency.
     */
    public void addDependency(BuildDependency aDependency)
    {
        addDependency(aDependency, getDependencies().length);
    }

    /**
     * Adds a dependency at given index.
     */
    public void addDependency(BuildDependency aDependency, int anIndex)
    {
        if (ArrayUtils.contains(_dependencies, aDependency)) return;
        BuildDependency[] newDependencies = ArrayUtils.add(_dependencies, aDependency, anIndex);
        setDependencies(newDependencies);
    }

    /**
     * Removes a dependency at given index.
     */
    public void removeDependency(int anIndex)
    {
        BuildDependency[] newDependencies = ArrayUtils.remove(_dependencies, anIndex);
        setDependencies(newDependencies);
    }

    /**
     * Removes a given dependency.
     */
    public void removeDependency(BuildDependency aDependency)
    {
        int index = ArrayUtils.indexOf(_dependencies, aDependency);
        if (index >= 0)
            removeDependency(index);
    }

    /**
     * Adds a build dependency for given Jar file class or class dir.
     */
    public void addJarFileDependencyForPath(String aPath)
    {
        String libPath = ProjectUtils.getRelativePath(_proj, aPath);
        BuildDependency.JarFileDependency jarFileDependency = new BuildDependency.JarFileDependency();
        jarFileDependency.setId(libPath);
        addDependency(jarFileDependency);
    }

    /**
     * Returns the dependent project names.
     */
    public String[] getProjectDependenciesNames()
    {
        BuildDependency[] dependencies = getDependencies();
        BuildDependency[] projectDependencies = ArrayUtils.filter(dependencies, dependency -> dependency instanceof BuildDependency.ProjectDependency);
        String[] projectNames = ArrayUtils.map(projectDependencies, dependency -> dependency.getId(), String.class);
        return projectNames;
    }

    /**
     * Adds a project path.
     */
    public void addProjectDependencyForProjectPath(String aPath)
    {
        String projectName = FilePathUtils.getFilename(aPath);
        BuildDependency.ProjectDependency projectDependency = new BuildDependency.ProjectDependency();
        projectDependency.setId(projectName);
        addDependency(projectDependency);
    }

    /**
     * Adds a maven build dependency.
     */
    public void addMavenDependencyForGroupAndPackageAndVersion(String groupName, String packageName, String version)
    {
        BuildDependency.MavenDependency mavenDependency = new BuildDependency.MavenDependency();
        String idStr = groupName + ':' + packageName + ':' + version;
        mavenDependency.setId(idStr);
        addDependency(mavenDependency);
    }

    /**
     * Returns the build path as absolute path.
     */
    public String getBuildPathAbsolute()
    {
        String buildPath = getBuildPath();
        return ProjectUtils.getAbsolutePath(_proj, buildPath, true);
    }

    /**
     * Reads BuildFile properties from project build file.
     */
    public void readFile()
    {
        // Get config file and json string
        WebFile configFile = getBuildFile();
        String jsonStr = configFile.getText();

        // Read BuildFile properties from JSON
        PropArchiverJS archiver = createArchiver();
        archiver.setRootObject(this);
        archiver.readPropObjectFromJSONString(jsonStr);
    }

    /**
     * Saves BuildFile properties to the project build file.
     */
    public void writeFile()
    {
        // Get config file
        WebFile configFile = getBuildFile();

        // Get BuildFile properties archived to JSON bytes
        PropArchiverJS archiver = createArchiver();
        JSObject jsonObj = archiver.writePropObjectToJSON(this);
        String jsonStr = jsonObj.toString();
        byte[] jsonBytes = jsonStr.getBytes();

        // Set bytes and save
        configFile.setBytes(jsonBytes);
        configFile.save();
    }

    /**
     * Returns the build file.
     */
    public WebFile getBuildFile()
    {
        // If already set, just return
        if (_buildFile != null) return _buildFile;

        // Get file (create if missing)
        WebSite projSite = _proj.getSite();
        WebFile file = projSite.getFileForPath(BUILD_FILE_PATH);
        if (file == null) {
            file = projSite.createFileForPath(BUILD_FILE_PATH, false);
            if (!_proj.isReadOnly()) {
                _buildFile = file;
                writeFile();
            }
        }

        // Set/return
        return _buildFile = file;
    }

    /**
     * Initialize properties for this class.
     */
    @Override
    protected void initProps(PropSet aPropSet)
    {
        // Do normal version
        super.initProps(aPropSet);

        // SourcePath, BuildPath, Dependencies
        aPropSet.addPropNamed(SourcePath_Prop, String.class);
        aPropSet.addPropNamed(BuildPath_Prop, String.class);
        aPropSet.addPropNamed(Dependencies_Prop, BuildDependency[].class);
    }

    /**
     * Returns the prop value for given key.
     */
    @Override
    public Object getPropValue(String aPropName)
    {
        // Handle properties
        switch (aPropName) {

            // SourcePath, BuildPath, Dependencies
            case SourcePath_Prop: return getSourcePath();
            case BuildPath_Prop: return getBuildPath();
            case Dependencies_Prop: return getDependencies();

            // Handle super class properties (or unknown)
            default: System.err.println("BuildFile.getPropValue: Unknown prop: " + aPropName); return null;
        }
    }

    /**
     * Sets the prop value for given key.
     */
    @Override
    public void setPropValue(String aPropName, Object aValue)
    {
        // Handle properties
        switch (aPropName) {

            // SourcePath, BuildPath, Dependencies
            case SourcePath_Prop: setSourcePath(Convert.stringValue(aValue)); break;
            case BuildPath_Prop: setBuildPath(Convert.stringValue(aValue)); break;
            case Dependencies_Prop: setDependencies((BuildDependency[]) aValue); break;

            // Handle super class properties (or unknown)
            default: System.err.println("BuildFile.setPropValue: Unknown prop: " + aPropName);
        }
    }

    /**
     * Creates the archiver.
     */
    private PropArchiverJS createArchiver()
    {
        PropArchiverJS archiver = new PropArchiverJS();
        archiver.addClassMapClass(BuildDependency.JarFileDependency.class);
        archiver.addClassMapClass(BuildDependency.ProjectDependency.class);
        archiver.addClassMapClass(BuildDependency.MavenDependency.class);
        return archiver;
    }
}