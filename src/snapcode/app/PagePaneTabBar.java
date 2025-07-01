package snapcode.app;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.view.*;
import snap.web.WebFile;
import java.util.List;

/**
 * The tab bar.
 */
public class PagePaneTabBar extends TabBar {

    // The PagePane
    private PagePane _pagePane;

    // The separator view between tab bar and page pane browser
    private RectView _separator;

    // Constant for file tab attributes
    private static Font TAB_BAR_FONT = new Font("Arial", 12);
    private static Color TAB_BAR_BORDER_COLOR = Color.GRAY8;

    /**
     * Constructor.
     */
    public PagePaneTabBar(PagePane aPagePane)
    {
        super();
        _pagePane = aPagePane;
        setFont(TAB_BAR_FONT);
        setGrowWidth(true);
        setTabCloseActionHandler((e,tab) -> handleTabCloseAction(tab));

        // Create separator
        _separator = new RectView();
        _separator.setFill(TAB_BAR_BORDER_COLOR);
        _separator.setPrefHeight(1);

        // Set PrefHeight so it will show when empty
        Tab sampleTab = new Tab();
        sampleTab.setTitle("XXX");
        addTab(sampleTab);
        setPrefHeight(getPrefHeight() + 2);
        removeTab(0);
    }

    /**
     * Returns the separator.
     */
    public View getSeparator()  { return _separator; }

    /**
     * Returns the open files.
     */
    public List<WebFile> getOpenFiles()  { return _pagePane.getOpenFiles(); }

    /**
     * ResetUI.
     */
    protected void resetUI()
    {
        // Update TabBar.SelIndex
        WebFile selFile = _pagePane.getSelFile();
        List<WebFile> openFiles = getOpenFiles();
        int selIndex = openFiles.indexOf(selFile);
        setSelIndex(selIndex);

        // Update TabBar Visible
        boolean showTabBar = !openFiles.isEmpty();
        if (showTabBar && openFiles.size() == 1 && selFile != null) {
            if ("jepl".equals(selFile.getFileType()) || selFile.getName().contains("JavaFiddle"))
                showTabBar = false;
        }
        setVisible(showTabBar);
        _separator.setVisible(showTabBar);
    }

    /**
     * Builds the file tabs.
     */
    protected void buildFileTabs()
    {
        // If not on event thread, come back on that
        if (!ViewUtils.isEventThread()) {
            runLater(() -> buildFileTabs());
            return;
        }

        // Remove tabs
        removeTabs();

        // Iterate over OpenFiles, create FileTabs, init and add
        for (WebFile file : getOpenFiles()) {

            // Create/config/add Tab
            Tab fileTab = new Tab();
            fileTab.setTitle(file.getName());
            fileTab.setClosable(true);
            addTab(fileTab);

            // Configure Tab.Button
            ToggleButton tabButton = fileTab.getButton();
            tabButton.addEventFilter(e -> handleTabButtonMousePress(e, file), MousePress);
        }

        // Reset UI
        _pagePane.resetLater();
    }
    /**
     * Respond UI.
     */
    protected void handleTabBarActionEvent(ViewEvent anEvent)
    {
        List<WebFile> openFiles = getOpenFiles();
        int selIndex = getSelIndex();
        WebFile openFile = selIndex >= 0 ? openFiles.get(selIndex) : null;
        _pagePane.setSelFile(openFile);
    }

    /**
     * Called when TabBar Tab button close box is triggered.
     */
    private void handleTabCloseAction(Tab aTab)
    {
        int index = getTabs().indexOf(aTab);
        if (index >= 0) {
            WebFile tabFile = getOpenFiles().get(index);
            _pagePane.closeFile(tabFile);
        }
    }

    /**
     * Called when tab button gets mouse press.
     */
    private void handleTabButtonMousePress(ViewEvent anEvent, WebFile aFile)
    {
        if (anEvent.isPopupTrigger()) {
            Menu contextMenu = createFileContextMenu(aFile);
            contextMenu.showMenuAtXY(anEvent.getView(), anEvent.getX(), anEvent.getY());
            anEvent.consume();
        }
    }

    /**
     * Creates popup menu for tab button.
     */
    private Menu createFileContextMenu(WebFile aFile)
    {
        ViewBuilder<MenuItem> mb = new ViewBuilder<>(MenuItem.class);
        mb.text("Revert File").save().addEventHandler(e -> _pagePane._workspacePane.getFilesTool().revertFile(aFile), Action);
        mb.text("Show file in Finder/Explorer").save().addEventHandler(e -> _pagePane.showFileInFinder(aFile), Action);
        mb.text("Show file in Text Editor").save().addEventHandler(e -> _pagePane.showFileInTextEditor(aFile), Action);
        return mb.buildMenu("ContextMenu", null);
    }

    /**
     * Returns whether a file is an "OpenFile" (whether it needs a file tab).
     */
    protected boolean shouldHaveFileTab(WebFile aFile)
    {
        // If directory, return false
        if (aFile.isDir()) return false;

        // Accept all Java files
        if (aFile.getFileType().equals("java"))
            return true;

        // Accept all project files
        return _pagePane.isProjectFile(aFile);
    }
}
