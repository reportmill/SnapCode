package snapcode.app;
import snapcode.apptools.DebugTool;
import snapcode.apptools.SearchPane;

/**
 * This class manages all of the ProjectTools for a ProjectPane.
 */
public class ProjectTools {

    // The ProjectPane
    private ProjectPane  _projPane;

    // The DebugTool
    private DebugTool  _debugTool;

    // The SearchTool
    private SearchPane  _searchTool;

    /**
     * Constructor.
     */
    public ProjectTools(ProjectPane projectPane)
    {
        super();
        _projPane = projectPane;
    }

    /**
     * Create tools.
     */
    protected void createTools()
    {
        _debugTool = new DebugTool(_projPane);
        _searchTool = new SearchPane(_projPane);
    }

    /**
     * Returns the debug tool.
     */
    public DebugTool getDebugTool()  { return _debugTool; }

    /**
     * Returns the search tool.
     */
    public SearchPane getSearchTool()  { return _searchTool; }

    /**
     * Sets the selected index for given class.
     */
    public void showToolForClass(Class<? extends ProjectTool> aClass)
    {
        SupportTray supportTray = ((AppPane) _projPane).getSupportTray();
        supportTray.setSelToolForClass(aClass);
    }


}
