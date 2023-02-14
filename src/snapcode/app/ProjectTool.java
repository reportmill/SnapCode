package snapcode.app;
import javakit.project.Project;
import javakit.project.Workspace;
import snap.view.ViewOwner;

/**
 * This class is the base for support panes specific to individual projects.
 */
public class ProjectTool extends ViewOwner {

    // The WorkspacePane
    protected WorkspacePane  _workspacePane;

    // The ProjectPane
    protected ProjectPane  _projPane;

    // The Workspace
    protected Workspace  _workspace;

    // The project
    protected Project  _proj;

    /**
     * Constructor.
     */
    public ProjectTool(ProjectPane projectPane)
    {
        super();
        _workspacePane = projectPane.getWorkspacePane();
        _projPane = projectPane;
        _workspace = projectPane.getWorkspace();
        _proj = projectPane.getProject();
    }
}
