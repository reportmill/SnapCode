package snapcode.app;
import javakit.project.Project;
import snap.view.ViewOwner;
import snap.viewx.WebBrowser;
import snap.web.WebFile;
import snap.web.WebSite;
import java.util.Collections;
import java.util.List;

/**
 * This is the base class for project tools.
 */
public class ProjectTool extends ViewOwner {

    // The ProjectPane
    protected ProjectPane  _projPane;

    // The ProjectTools
    protected ProjectTools _projTools;

    // The PagePane
    protected PagePane  _pagePane;

    /**
     * Constructor.
     */
    public ProjectTool(ProjectPane projPane)
    {
        super();
        _projPane = projPane;
        _projTools = projPane.getProjectTools();
        _pagePane = projPane.getPagePane();
    }

    /**
     * Returns the ProjectPane.
     */
    public ProjectPane getProjectPane()  { return _projPane; }

    /**
     * Returns the top level site.
     */
    public WebSite getRootSite()  { return _projPane.getRootSite(); }

    /**
     * Returns the selected project.
     */
    public Project getProject()  { return _projPane.getProject(); }

    /**
     * Returns the selected file.
     */
    public WebFile getSelFile()  { return _projPane.getSelFile(); }

    /**
     * Sets the selected site file.
     */
    public void setSelFile(WebFile aFile)
    {
        _projPane.setSelFile(aFile);
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
     * Returns the selected site.
     */
    public WebSite getSelSite()  { return _projPane.getSelSite(); }

    /**
     * Returns the browser.
     */
    public WebBrowser getBrowser()  { return _projPane.getBrowser(); }

    /**
     * Returns the title.
     */
    public String getTitle()  { return "Unknown"; }
}
