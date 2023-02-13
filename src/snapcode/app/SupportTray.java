package snapcode.app;
import snap.geom.Side;
import snap.util.ArrayUtils;
import snap.view.*;
import snapcode.apptools.*;

/**
 * A class to hold TabView for ProjectTools (ProblemsTool, DebugTool, etc.).
 */
public class SupportTray extends ViewOwner {

    // The side
    private Side  _side;

    // The array of ProjectTools
    private ProjectTool[]  _trayTools;

    // The tab view
    private TabView  _tabView;

    /**
     * Creates a new SupportTray for given ProjectPane.
     */
    public SupportTray(PodPane projPane, Side aSide, ProjectTool[] trayTools)
    {
        _side = aSide;
        _trayTools = trayTools;
    }

    /**
     * Returns the tool for given class.
     */
    public ProjectTool getToolForClass(Class<? extends ProjectTool> aToolClass)
    {
        return ArrayUtils.findMatch(_trayTools, tool -> aToolClass.isInstance(tool));
    }

    /**
     * Returns the selected index.
     */
    public int getSelIndex()  { return _tabView != null ? _tabView.getSelIndex() : -1; }

    /**
     * Sets the selected index.
     */
    public void setSelIndex(int anIndex)
    {
        _tabView.setSelIndex(anIndex);
    }

    /**
     * Returns the selected tool.
     */
    public ProjectTool getSelTool()
    {
        Tab selTab = _tabView.getSelItem();
        ProjectTool selTool = selTab != null ? (ProjectTool) selTab.getContentOwner() : null;
        return selTool;
    }

    /**
     * Sets the selected tool.
     */
    public void setSelTool(ProjectTool aTool)
    {
        int index = ArrayUtils.indexOfId(_trayTools, aTool);
        setSelIndex(index);
    }

    /**
     * Sets the selected index for given class.
     */
    public void setSelToolForClass(Class<? extends ProjectTool> aClass)
    {
        ProjectTool tool = getToolForClass(aClass);
        setSelTool(tool);
    }

    /**
     * Shows the problems tool.
     */
    public void showProblemsTool()  { setSelToolForClass(ProblemsTool.class); }

    /**
     * Shows the run tool.
     */
    public void showRunTool()  { setSelToolForClass(RunConsole.class); }

    /**
     * Sets selected index to debug.
     */
    public void showDebugTool()  { setSelToolForClass(DebugTool.class); }

    /**
     * Hides selected tool.
     */
    public void hideTools()  { setSelIndex(-1); }

    /**
     * Creates UI for SupportTray.
     */
    protected View createUI()
    {
        // Create/config TabView
        _tabView = new TabView();
        _tabView.setName("TabView");
        _tabView.setFont(_tabView.getFont().deriveFont(12));
        _tabView.setTabSide(_side);
        _tabView.getTabBar().setTabMinWidth(70);
        _tabView.getTabBar().setAllowEmptySelection(true);

        // Bogus: Manually set TabView PrefHeight
        _tabView.addPropChangeListener(pc -> {
            if (_side.isTopOrBottom()) {
                double prefH = _tabView.getContent() != null ? 280 : 30;
                _tabView.setPrefHeight(prefH);
            }
            else {
                double prefW = _tabView.getContent() != null ? 250 : 30;
                _tabView.setPrefWidth(prefW);
            }
        }, TabView.SelIndex_Prop);

        // Get TabBuilder
        Tab.Builder tabBuilder = new Tab.Builder(_tabView.getTabBar());

        // Iterate over tools and add tab for each
        for (ProjectTool tool : _trayTools) {
            String title = tool.getTitle();
            tabBuilder.title(title).contentOwner(tool).add();
        }

        // Return
        return _tabView;
    }

    /**
     * Override to reset selected tab.
     */
    protected void resetUI()
    {
        Tab selTab = _tabView.getSelItem();
        ViewOwner viewOwner = selTab != null ? selTab.getContentOwner() : null;
        if (viewOwner != null)
            viewOwner.resetLater();
    }
}