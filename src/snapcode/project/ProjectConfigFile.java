package snapcode.project;
import javakit.project.Project;
import javakit.project.ProjectConfig;
import snap.props.Prop;
import snap.props.PropArchiverJS;
import snap.util.JSObject;
import snap.web.WebFile;
import snap.web.WebSite;

/**
 * This class manages read/write of ProjectConfig.
 */
public class ProjectConfigFile {

    // The Project
    private Project _proj;

    // The ProjectConfig
    private ProjectConfig _projConfig;

    // The web file
    private WebFile _file;

    // Constants
    public static final String BUILD_FILE_PATH = "/build.snapcode";

    /**
     * Constructor.
     */
    public ProjectConfigFile(Project aProject)
    {
        _proj = aProject;

        // Create ProjectConfig
        _projConfig = new ProjectConfig(aProject);

        // Read config file if exists
        WebFile configFile = getConfigFile();
        if (configFile.getExists())
            readFile();

        // Legacy: Otherwise, look for Eclipse file
        else {
            new EclipseBuildFile(aProject, _projConfig);
            writeFile();
        }

        // Start listening to changes
        _projConfig.addPropChangeListener(pc -> propConfigDidPropChange());
    }

    /**
     * Returns the ProjectConfig.
     */
    public ProjectConfig getProjectConfig()  { return _projConfig; }

    /**
     * Reads ProjectConfig properties from project config file.
     */
    public void readFile()
    {
        // Get config file and json string
        WebFile configFile = getConfigFile();
        String jsonStr = configFile.getText();

        // Read ProjectConfig from JSON
        PropArchiverJS archiver = new PropArchiverJS();
        ProjectConfig projectConfig = (ProjectConfig) archiver.readPropObjectFromJSONString(jsonStr);

        // Copy to existing ProjConfig
        Prop[] props = projectConfig.getPropsForArchival();
        for (Prop prop : props) {
            String propName = prop.getName();
            Object newValue = projectConfig.getPropValue(propName);
            _projConfig.setPropValue(propName, newValue);
        }
    }

    /**
     * Saves the ProjectConfig properties to the project config file.
     */
    public void writeFile()
    {
        // Get config file
        WebFile configFile = getConfigFile();

        // Get ProjectConfig archived to JSON bytes
        PropArchiverJS archiver = new PropArchiverJS();
        JSObject jsonObj = archiver.writePropObjectToJSON(_projConfig);
        String jsonStr = jsonObj.toString();
        byte[] jsonBytes = jsonStr.getBytes();

        // Set bytes and save
        configFile.setBytes(jsonBytes);
        configFile.save();
    }

    /**
     * Called when PropConfig does PropChange.
     */
    private void propConfigDidPropChange()
    {
        try { writeFile(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Returns the file.
     */
    public WebFile getConfigFile()
    {
        // If already set, just return
        if (_file != null) return _file;

        // Get file (create if missing)
        WebSite projSite = _proj.getSite();
        WebFile file = projSite.getFileForPath(BUILD_FILE_PATH);
        if (file == null)
            file = projSite.createFileForPath(BUILD_FILE_PATH, false);

        // Set/return
        return _file = file;
    }
}
