package snapcode.app;
import snap.geom.Side;
import snap.props.PropChangeListener;
import snap.util.ArrayUtils;
import snap.view.*;
import snapcode.apptools.BuildTool;
import java.util.List;

/**
 * A class to hold TabView to show WorkspaceTool instances (ProblemsTool, DebugTool, etc.).
 */
public class ToolTray extends ViewOwner {

    // The side
    private Side  _side;

    // The array of WorkspaceTool instances for this tray
    protected WorkspaceTool[]  _trayTools;

    // The tab view
    private TabView  _tabView;

    /**
     * Creates a new SupportTray for given ProjectPane.
     */
    public ToolTray(Side aSide, WorkspaceTool[] trayTools)
    {
        _side = aSide;
        _trayTools = trayTools;
    }

    /**
     * Returns the tool for given class.
     */
    public <T extends WorkspaceTool> T getToolForClass(Class<T> aToolClass)
    {
        return (T) ArrayUtils.findMatch(_trayTools, tool -> aToolClass.isInstance(tool));
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
    public WorkspaceTool getSelTool()
    {
        Tab selTab = _tabView.getSelItem();
        WorkspaceTool selTool = selTab != null ? (WorkspaceTool) selTab.getContentOwner() : null;
        return selTool;
    }

    /**
     * Sets the selected tool.
     */
    public void setSelTool(WorkspaceTool aTool)
    {
        int index = ArrayUtils.indexOfId(_trayTools, aTool);
        setSelIndex(index);
    }

    /**
     * Sets the selected index for given class.
     */
    public void setSelToolForClass(Class<? extends WorkspaceTool> aClass)
    {
        WorkspaceTool tool = getToolForClass(aClass);
        setSelTool(tool);
    }

    /**
     * Shows the problems tool.
     */
    public void showProblemsTool()  { setSelToolForClass(BuildTool.class); }

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
        _tabView.setAnimateTabChange(true);

        // Have to reset tabView.PrefSize when TabView selection changes, so
        View tabViewContentBox = _tabView.getContentBox();
        PropChangeListener tabViewContentBoxPrefSizeLsnr = pc -> tabViewContentBoxPrefSizeChanged();
        tabViewContentBox.addPropChangeListener(tabViewContentBoxPrefSizeLsnr, View.PrefWidth_Prop, View.PrefHeight_Prop);

        // Get TabBuilder
        Tab.Builder tabBuilder = new Tab.Builder(_tabView.getTabBar());

        // Iterate over tools and add tab for each
        for (WorkspaceTool tool : _trayTools) {
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

    /**
     * Called when TabView.ContentBox has PrefSize change (always due to animation) to reset SplitView.Items.PrefSize.
     */
    private void tabViewContentBoxPrefSizeChanged()
    {
        // Get SplitView + Items for SplitView holding TabView
        SplitView splitView = (SplitView) _tabView.getParent();
        List<View> splitViewItems = splitView.getItems();

        // Reset SplitView.Items (conditional to handle Left/RightTray special)
        for (View item : splitViewItems) {
            if (item == _tabView || !(item instanceof TabView))
                item.setPrefSize(-1, -1);
        }
    }
}