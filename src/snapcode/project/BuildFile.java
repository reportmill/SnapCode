package snapcode.project;
import snap.props.*;
import snap.util.*;
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

    // The compile release version
    private int _compileRelease;

    // The dependencies
    private BuildDependency[] _dependencies;

    // The main class name
    private String _mainClassName;

    // Whether to include the built-in SnapKit runtime as dependency
    private boolean _includeSnapKitRuntime;

    // The actual build file
    private WebFile _buildFile;

    // A listener to propagate dependency changes to build file listeners
    private PropChangeListener _dependencyDidChangeLsnr = this::dependencyDidChange;

    // Constants
    public static final String BUILD_FILE_PATH = "/build.snapcode";

    // Constants for BuildFile properties
    public static final String SourcePath_Prop = "SourcePath";
    public static final String BuildPath_Prop = "BuildPath";
    public static final String CompileRelease_Prop = "CompileRelease";
    public static final String Dependency_Prop = "Dependency";
    public static final String Dependencies_Prop = "Dependencies";
    public static final String MainClassName_Prop = "MainClassName";
    public static final String IncludeSnapKitRuntime_Prop = "IncludeSnapKitRuntime";

    // Constants for defaults
    private static final String DEFAULT_SOURCE_PATH = "src";
    private static final String DEFAULT_BUILD_PATH = "bin";
    private static final int DEFAULT_JAVA_VERSION = SnapUtils.getJavaVersionInt();

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
        _compileRelease = DEFAULT_JAVA_VERSION;
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
     * Returns the project that owns this build file.
     */
    public Project getProject()  { return _proj; }

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
        String path = aPath != null ? aPath : "";
        if (Objects.equals(path, _srcPath)) return;

        // Set, firePropChange
        String newPath = ProjectUtils.getRelativePath(_proj, aPath);
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
        String path = aPath != null ? aPath : "";
        if (Objects.equals(path, _buildPath)) return;

        // Set, firePropChange
        String newPath = ProjectUtils.getRelativePath(_proj, path);
        firePropChange(BuildPath_Prop, _buildPath, _buildPath = newPath);
    }

    /**
     * Returns the compile release version.
     */
    public int getCompileRelease()  { return Math.min(_compileRelease, DEFAULT_JAVA_VERSION); }

    /**
     * Sets the compile release version.
     */
    public void setCompileRelease(int aValue)
    {
        if (aValue == _compileRelease) return;
        firePropChange(CompileRelease_Prop, _compileRelease, _compileRelease = aValue);
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

        // Reset old
        BuildDependency[] oldDependencies = _dependencies;
        for (BuildDependency dependency : oldDependencies )
            dependency.removePropChangeListener(_dependencyDidChangeLsnr);

        // Set new
        _dependencies = theDependencies;
        for (BuildDependency dependency : theDependencies) {
            dependency._buildFile = this;
            dependency.addPropChangeListener(_dependencyDidChangeLsnr);
        }

        // Fire prop change
        firePropChange(Dependencies_Prop, oldDependencies, _dependencies);
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
        firePropChange(Dependency_Prop, null, aDependency);
    }

    /**
     * Removes a dependency at given index.
     */
    public void removeDependency(int anIndex)
    {
        BuildDependency dependency = _dependencies[anIndex];
        BuildDependency[] newDependencies = ArrayUtils.remove(_dependencies, anIndex);
        setDependencies(newDependencies);
        firePropChange(Dependency_Prop, dependency, null);
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
     * Returns the main class name.
     */
    public String getMainClassName()  { return _mainClassName; }

    /**
     * Sets the main class name.
     */
    public void setMainClassName(String aName)
    {
        // If already set, just return
        if (Objects.equals(aName, _mainClassName)) return;

        // Set, firePropChange
        firePropChange(SourcePath_Prop, _mainClassName, _mainClassName = aName);
    }

    /**
     * Returns whether to include the built-in SnapKit runtime as dependency.
     */
    public boolean isIncludeSnapKitRuntime()  { return _includeSnapKitRuntime; }

    /**
     * Sets whether to include the built-in SnapKit runtime as dependency.
     */
    public void setIncludeSnapKitRuntime(boolean aValue)
    {
        if (aValue == _includeSnapKitRuntime) return;
        firePropChange(IncludeSnapKitRuntime_Prop, _includeSnapKitRuntime, _includeSnapKitRuntime = aValue);
    }

    /**
     * Returns whether to include the built-in SnapCharts runtime as dependency.
     */
    public boolean isIncludeSnapChartsRuntime()
    {
        BuildDependency[] dependencies = getDependencies();
        return ArrayUtils.hasMatch(dependencies, dep -> dep.getId() != null && dep.getId().contains("snapcharts"));
    }

    /**
     * Sets whether to include the built-in SnapCharts runtime as dependency.
     */
    public void setIncludeSnapChartsRuntime(boolean aValue)
    {
        if (aValue == isIncludeSnapChartsRuntime()) return;

        // Handle Add SnapCharts
        if (aValue)
            addDependency(new MavenDependency("com.reportmill:snapcharts:2025.04"));

        // Handle Remove SnapCharts
        else {
            BuildDependency[] dependencies = getDependencies();
            BuildDependency dependency = ArrayUtils.findMatch(dependencies, dep -> dep.getId() != null && dep.getId().contains("snapcharts"));
            removeDependency(dependency);
        }
    }

    /**
     * Returns whether to include built-in greenfoot runtime.
     */
    public boolean isIncludeGreenfootRuntime()
    {
        if (_includeGreenfoot != null) return _includeGreenfoot;
        WebFile greenfootFile = _proj.getFileForPath("/src/project.greenfoot");
        return _includeGreenfoot = greenfootFile != null;
    }
    private Boolean _includeGreenfoot;

    /**
     * Sets whether to include built-in greenfoot runtime.
     */
    public void setIncludeGreenfootRuntime(boolean aValue)
    {
        if (aValue == _includeGreenfoot) return;
        //buildFile.addDependency(new MavenDependency("com.reportmill:greenfoot:2024.11"));
        _includeGreenfoot = aValue;
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
        projectDependency.setProjectName(projectName);
        addDependency(projectDependency);
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

        // Upgrade greenfoot
        MavenDependency gf = (MavenDependency) ArrayUtils.findMatch(getDependencies(),
                dep -> dep.getId() != null && dep.getId().contains("com.reportmill:greenfoot:"));
        if (gf != null) // && !gf.getId().equals("com.reportmill:greenfoot:2024.11")) gf.setId("com.reportmill:greenfoot:2024.11");
            removeDependency(gf);
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
     * Called when a dependency changes to forward to build file listeners.
     */
    private void dependencyDidChange(PropChange aPC)
    {
        firePropChange(aPC);
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
        if (file == null)
            file = projSite.createFileForPath(BUILD_FILE_PATH, false);

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

        // SourcePath, BuildPath, CompileRelease
        aPropSet.addPropNamed(SourcePath_Prop, String.class);
        aPropSet.addPropNamed(BuildPath_Prop, String.class);
        aPropSet.addPropNamed(CompileRelease_Prop, int.class);

        // Dependencies, MainClassName, IncludeSnapKitRuntime, IncludeSnapChartsRuntime
        aPropSet.addPropNamed(Dependencies_Prop, BuildDependency[].class);
        aPropSet.addPropNamed(MainClassName_Prop, String.class);
        aPropSet.addPropNamed(IncludeSnapKitRuntime_Prop, boolean.class);
        //aPropSet.addPropNamed(IncludeSnapChartsRuntime_Prop, boolean.class);
    }

    /**
     * Returns the prop value for given key.
     */
    @Override
    public Object getPropValue(String aPropName)
    {
        // Handle properties
        switch (aPropName) {

            // SourcePath, BuildPath, CompileRelease
            case SourcePath_Prop: return getSourcePath();
            case BuildPath_Prop: return getBuildPath();
            case CompileRelease_Prop: return getCompileRelease();

            // Dependencies, MainClassName, IncludeSnapKitRuntime, IncludeSnapChartsRuntime
            case Dependencies_Prop: return getDependencies();
            case MainClassName_Prop: return getMainClassName();
            case IncludeSnapKitRuntime_Prop: return isIncludeSnapKitRuntime();
            //case IncludeSnapChartsRuntime_Prop: return isIncludeSnapChartsRuntime();

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

            // SourcePath, BuildPath, CompileRelease
            case SourcePath_Prop: setSourcePath(Convert.stringValue(aValue)); break;
            case BuildPath_Prop: setBuildPath(Convert.stringValue(aValue)); break;
            case CompileRelease_Prop: setCompileRelease(Convert.intValue(aValue)); break;

            // Dependencies, MainClassName, IncludeSnapKitRuntime, IncludeSnapChartsRuntime
            case Dependencies_Prop: setDependencies((BuildDependency[]) aValue); break;
            case MainClassName_Prop: setMainClassName(Convert.stringValue(aValue)); break;
            case IncludeSnapKitRuntime_Prop: setIncludeSnapKitRuntime(Convert.boolValue(aValue)); break;
            //case IncludeSnapChartsRuntime_Prop: setIncludeSnapChartsRuntime(Convert.boolValue(aValue)); break;

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
        archiver.addClassMapClass(MavenDependency.class);
        return archiver;
    }
}