package snapcode.app;
import javakit.project.Project;
import snap.view.ViewOwner;
import snap.viewx.WebBrowser;
import snap.web.WebFile;
import snap.web.WebSite;
import java.util.Collections;
import java.util.List;

/**
 * This is the base class for pod tools.
 */
public class PodTool extends ViewOwner {

    // The PodPane
    protected PodPane  _podPane;

    // The PodTools
    protected PodTools  _podTools;

    // The PagePane
    protected PagePane  _pagePane;

    /**
     * Constructor.
     */
    public PodTool(PodPane podPane)
    {
        super();
        _podPane = podPane;
        _podTools = podPane.getPodTools();
        _pagePane = podPane.getPagePane();
    }

    /**
     * Returns the PodPane.
     */
    public PodPane getPodPane()  { return _podPane; }

    /**
     * Returns the top level site.
     */
    public WebSite getRootSite()  { return _podPane.getRootSite(); }

    /**
     * Returns the selected project.
     */
    public Project getProject()  { return _podPane.getRootProject(); }

    /**
     * Returns the selected file.
     */
    public WebFile getSelFile()  { return _podPane.getSelFile(); }

    /**
     * Sets the selected site file.
     */
    public void setSelFile(WebFile aFile)
    {
        _podPane.setSelFile(aFile);
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
    public WebSite getSelSite()  { return _podPane.getSelSite(); }

    /**
     * Returns the browser.
     */
    public WebBrowser getBrowser()  { return _podPane.getBrowser(); }

    /**
     * Returns the title.
     */
    public String getTitle()  { return "Unknown"; }
}
