package snapcode.app;
import snapcode.apptools.BuildFileTool;

/**
 * This class manages the tools for a project.
 */
public class ProjectTools {

    // The ProjectPane
    protected ProjectPane _projectPane;

    // The BuildFileTools
    private BuildFileTool _buildFileTool;

    /**
     * Constructor.
     */
    public ProjectTools(ProjectPane projectPane)
    {
        super();
        _projectPane = projectPane;

        // Create tools
        _buildFileTool = new BuildFileTool(projectPane);
    }

    /**
     * Returns the BuildFileTool.
     */
    public BuildFileTool getBuildFileTool()  { return _buildFileTool; }
}
