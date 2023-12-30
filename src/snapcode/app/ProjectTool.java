package snapcode.app;
import snapcode.project.Project;
import snapcode.project.Workspace;
import snap.view.ViewOwner;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.webbrowser.WebBrowser;

import java.util.Collections;
import java.util.List;

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

    /**
     * Returns the Project site.
     */
    public WebSite getProjectSite()  { return _proj.getSite(); }

    /**
     * Returns the WorkspacePane.
     */
    public WorkspacePane getWorkspacePane()  { return _workspacePane; }

    /**
     * Returns the WorkspaceTools.
     */
    public WorkspaceTools getWorkspaceTools()  { return _workspacePane.getWorkspaceTools(); }

    /**
     * Returns the selected file.
     */
    public WebFile getSelFile()  { return _workspacePane.getSelFile(); }

    /**
     * Sets the selected site file.
     */
    public void setSelFile(WebFile aFile)
    {
        _workspacePane.setSelFile(aFile);
    }

    /**
     * Returns the list of selected files.
     */
    public List<WebFile> getSelFiles()
    {
        WebFile selFile = getSelFile();
        return selFile == null ? Collections.EMPTY_LIST : Collections.singletonList(selFile);
    }

    /**
     * Returns the browser.
     */
    public WebBrowser getBrowser()  { return _workspacePane.getBrowser(); }
}
