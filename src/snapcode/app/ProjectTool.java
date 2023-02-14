package snapcode.app;
import javakit.project.Project;
import snap.view.ViewOwner;

/**
 * This class is the base for support panes specific to individual projects.
 */
public class ProjectTool extends ViewOwner {

    // The WorkspacePane
    protected PodPane  _workspacePane;

    // The ProjectPane
    protected ProjectPane  _projPane;

    // The project
    protected Project  _proj;

    /**
     * Constructor.
     */
    public ProjectTool(ProjectPane projectPane)
    {
        super();
        _workspacePane = projectPane.getWorkSpacePane();
        _projPane = projectPane;
        _proj = projectPane.getProject();
    }
}
