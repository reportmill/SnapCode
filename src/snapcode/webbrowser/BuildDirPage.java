package snapcode.webbrowser;
import snapcode.project.Project;
import snap.view.BrowserView;
import snap.view.ChildView;
import snap.view.ViewEvent;
import snap.web.WebFile;
import snap.web.WebResponse;
import snapcode.util.ClassInfoPage;

/**
 * A UI pane to show and manage the build directory.
 */
public class BuildDirPage extends WebPage {

    // The project
    private Project _proj;

    // The FileBrowser
    private BrowserView<WebFile>  _fileBrowser;

    // The PageBrowser
    private WebBrowser  _pageBrowser;

    /**
     * Constructor.
     */
    public BuildDirPage()
    {
        super();
    }

    /**
     * Returns the project.
     */
    public Project getProject()
    {
        if (_proj != null) return _proj;
        WebFile file = getFile();
        Project proj = Project.getProjectForFile(file);
        return _proj = proj;
    }

    /**
     * Returns the build directory.
     */
    public WebFile getBuildDir()
    {
        return getFile();
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        // Get/configure FileBrowser
        _fileBrowser = getView("FileBrowser", BrowserView.class);
        _fileBrowser.setPrefColCount(3);
        _fileBrowser.setResolver(new DirFilePage.FileTreeResolver());
        _fileBrowser.setItems(new WebFile[] { getBuildDir() });

        // Get/configure PageBrowser
        _pageBrowser = new WebBrowser() {
            protected Class<? extends WebPage> getPageClass(WebResponse aResp)
            {
                String type = aResp.getPathType();
                if (type.equals("class")) return ClassInfoPage.class;
                return super.getPageClass(aResp);
            }
        };

        // Configure PageBrowser
        _pageBrowser.setName("PageBrowser");
        _pageBrowser.setGrowHeight(true);
        getUI(ChildView.class).addChild(_pageBrowser);
    }

    /**
     * Respond to UI changes.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle FileBrowser
        if (anEvent.equals("FileBrowser")) {
            WebFile file = _fileBrowser.getSelItem();
            if (file != null && !file.isDir())
                _pageBrowser.setSelUrl(file.getURL());
        }
    }
}