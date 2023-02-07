/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.props.PropChange;
import snap.util.ArrayUtils;
import snap.view.*;
import snap.viewx.WebBrowser;
import snap.web.WebFile;

/**
 * This class manages the file tabs for PagePane open files.
 */
public class PagePaneTabsBox extends ViewOwner {

    // The PagePane
    private PagePane  _pagePane;

    // The TabBar
    private TabBar  _tabBar;

    // Constant for file tab attributes
    private static Color BOX_BORDER_COLOR = Color.GRAY7;
    private static Font TAB_FONT = new Font("Arial", 12);
    private static Color TAB_TEXT_COLOR = Color.GRAY2;

    /**
     * Constructor.
     */
    public PagePaneTabsBox(PagePane aPagePane)
    {
        super();
        _pagePane = aPagePane;
    }

    /**
     * Create UI.
     */
    @Override
    protected View createUI()
    {
        // Add FileTabsPane pane
        _tabBar = new TabBar();
        _tabBar.setBorder(BOX_BORDER_COLOR, 1);
        _tabBar.setFont(TAB_FONT);
        _tabBar.setGrowWidth(true);

        // Set PrefHeight so it will show when empty
        Tab sampleTab = new Tab();
        sampleTab.setTitle("XXX");
        _tabBar.addTab(sampleTab);
        _tabBar.setPrefHeight(_tabBar.getPrefHeight());
        _tabBar.removeTab(0);

        // Register to build tabs whenever PagePage changes
        _pagePane.addPropChangeListener(pc -> propPaneDidPropChange(pc), PagePane.OpenFiles_Prop, PagePane.SelFile_Prop);

        // Return
        return _tabBar;
    }

    /**
     * Respond to UI changes.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        if (anEvent.getView() == _tabBar) {
            int selIndex = _tabBar.getSelIndex();
            WebFile[] openFiles = _pagePane.getOpenFiles();
            WebFile openFile = selIndex >= 0 ? openFiles[selIndex] : null;
            _pagePane.getBrowser().setTransition(WebBrowser.Instant);
            _pagePane.setSelectedFile(openFile);
        }
    }

    /**
     * Builds the file tabs.
     */
    private void buildFileTabs()
    {
        // If not on event thread, come back on that
        if (!isEventThread()) {
            runLater(() -> buildFileTabs());
            return;
        }

        // Remove tabs
        _tabBar.removeTabs();

        // Iterate over OpenFiles, create FileTabs, init and add
        WebFile[] openFiles = _pagePane.getOpenFiles();
        for (WebFile file : openFiles) {

            // Create/config/add Tab
            Tab fileTab = new Tab();
            fileTab.setTitle(file.getName());
            fileTab.setClosable(true);
            _tabBar.addTab(fileTab);

            // Configure Tab.Button
            ToggleButton tabButton = fileTab.getButton();
            tabButton.setTextFill(TAB_TEXT_COLOR);
        }
    }

    /**
     * Called when PropPane does PropChange.
     */
    private void propPaneDidPropChange(PropChange aPC)
    {
        String propName = aPC.getPropName();

        // Handle OpenFiles
        if (propName == PagePane.OpenFiles_Prop)
            buildFileTabs();

        // Handle SelFile
        if (propName == PagePane.SelFile_Prop) {
            WebFile selFile = _pagePane.getSelectedFile();
            WebFile[] openFiles = _pagePane.getOpenFiles();
            int selIndex = ArrayUtils.indexOfId(openFiles, selFile);
            _tabBar.setSelIndex(selIndex);
        }
    }
}
