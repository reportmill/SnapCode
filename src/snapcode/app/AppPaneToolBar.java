package snapcode.app;
import snap.gfx.*;
import snap.util.StringUtils;
import snap.view.*;
import snap.viewx.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.app.RunConfigsPage;
import snapcode.apptools.DebugTool;
import snapcode.apptools.SearchPane;
import java.util.*;

/**
 * ToolBar.
 */
public class AppPaneToolBar extends ProjectTool {

    // The AppPane
    private AppPane  _appPane;

    // A placeholder for fill from toolbar button under mouse
    private Paint  _tempFill;

    // RunConfigsPage
    private RunConfigsPage _runConfigsPage;

    // Shared images
    static Image SIDEBAR_EXPAND = Image.get(AppPane.class, "SideBar_Expand.png");
    static Image SIDEBAR_COLLAPSE = Image.get(AppPane.class, "SideBar_Collapse.png");

    /**
     * Creates a new AppPaneToolBar.
     */
    public AppPaneToolBar(AppPane anAppPane)
    {
        super(anAppPane);
        _appPane = anAppPane;
    }

    /**
     * Selects the search text.
     */
    public void selectSearchText()
    {
        runLater(() -> requestFocus("SearchComboBox"));
    }

    /**
     * Override to add menu button.
     */
    protected View createUI()
    {
        // Do normal version
        RowView superUI = (RowView) super.createUI();

        // Add MenuButton
        MenuButton menuButton = new MenuButton();
        menuButton.setName("RunMenuButton");
        menuButton.setPrefSize(15, 14);
        menuButton.setMargin(22, 0, 0, 0);
        menuButton.setItems(Arrays.asList(getRunMenuButtonItems()));
        menuButton.getGraphicAfter().setPadding(0, 0, 0, 0);
        superUI.addChild(menuButton, 5);

        // Add Expand button
        Button expandButton = new Button();
        expandButton.setName("ExpandButton");
        expandButton.setImage(SIDEBAR_EXPAND);
        expandButton.setShowArea(false);
        expandButton.setPrefSize(16, 16);
        superUI.addChild(expandButton);

        // Return
        return superUI;
    }

    /**
     * Override to set PickOnBounds.
     */
    protected void initUI()
    {
        // Get/configure SearchComboBox
        ComboBox<WebFile> searchComboBox = getView("SearchComboBox", ComboBox.class);
        searchComboBox.setItemTextFunction(item -> item.getName());
        searchComboBox.getListView().setItemTextFunction(item -> item.getName() + " - " + item.getParent().getPath());
        searchComboBox.setPrefixFunction(s -> getFilesForPrefix(s));

        // Get/configure SearchComboBox.PopupList
        PopupList<?> searchPopup = searchComboBox.getPopupList();
        searchPopup.setRowHeight(22);
        searchPopup.setPrefWidth(300);
        searchPopup.setMaxRowCount(15);
        searchPopup.setAltPaint(Color.get("#F8F8F8"));

        // Get/configure SearchText: radius, prompt, image, animation
        TextField searchText = searchComboBox.getTextField();
        searchText.setBorderRadius(8);
        searchText.setPromptText("Search");
        searchText.getLabel().setImage(Image.get(TextPane.class, "Find.png"));
        TextField.setBackLabelAlignAnimatedOnFocused(searchText, true);

        // Enable events on buttons
        String[] buttonNames = { "HomeButton", "BackButton", "NextButton", "RefreshButton", "RunButton" };
        for (String name : buttonNames)
            enableEvents(name, MouseRelease, MouseEnter, MouseExit);
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        Image img = _appPane.isShowSideBar() ? SIDEBAR_EXPAND : SIDEBAR_COLLAPSE;
        getView("ExpandButton", Button.class).setImage(img);
    }

    /**
     * Respond to UI changes.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Get AppPane and AppBrowser
        WebBrowser appBrowser = getBrowser();

        // Handle MouseEnter: Make buttons glow
        if (anEvent.isMouseEnter()) {
            View view = anEvent.getView();
            _tempFill = view.getFill();
            view.setFill(Color.WHITE);
            return;
        }

        // Handle MouseExit: Restore fill
        if (anEvent.isMouseExit()) {
            View view = anEvent.getView();
            view.setFill(_tempFill);
            return;
        }

        // Handle HomeButton
        if (anEvent.equals("HomeButton") && anEvent.isMouseRelease())
            _pagePane.showHomePage();

        // Handle LastButton, NextButton
        if (anEvent.equals("BackButton") && anEvent.isMouseRelease())
            appBrowser.trackBack();
        if (anEvent.equals("NextButton") && anEvent.isMouseRelease())
            appBrowser.trackForward();

        // Handle RefreshButton
        if (anEvent.equals("RefreshButton") && anEvent.isMouseRelease())
            appBrowser.reloadPage();

        // Handle RunButton
        if (anEvent.equals("RunButton") && anEvent.isMouseRelease()) {
            DebugTool debugTool = _projTools.getDebugTool();
            debugTool.runDefaultConfig(false);
        }

        // Handle RunConfigMenuItems
        if (anEvent.getName().endsWith("RunConfigMenuItem")) {
            String configName = anEvent.getName().replace("RunConfigMenuItem", "");
            DebugTool debugTool = _projTools.getDebugTool();
            debugTool.runConfigForName(configName, false);
            setRunMenuButtonItems();
        }

        // Handle RunConfigsMenuItem
        if (anEvent.equals("RunConfigsMenuItem"))
            appBrowser.setURL(getRunConfigsPageURL());

        // Show history
        if (anEvent.equals("ShowHistoryMenuItem"))
            _pagePane.showHistory();

        // Handle SearchComboBox
        if (anEvent.equals("SearchComboBox"))
            handleSearchComboBox(anEvent);

        // Handle ExpandButton
        if (anEvent.equals("ExpandButton")) {
            boolean showSideBar = !_appPane.isShowSideBar();
            _appPane.setShowSideBar(showSideBar);
        }
    }

    /**
     * Returns the RunConfigsPage.
     */
    public RunConfigsPage getRunConfigsPage()
    {
        if (_runConfigsPage != null) return _runConfigsPage;
        _runConfigsPage = new RunConfigsPage();
        _pagePane.setPageForURL(_runConfigsPage.getURL(), _runConfigsPage);
        return _runConfigsPage;
    }

    /**
     * Returns the RunConfigsPageURL.
     */
    public WebURL getRunConfigsPageURL()
    {
        return getRunConfigsPage().getURL();
    }

    /**
     * Handle SearchComboBox changes.
     */
    public void handleSearchComboBox(ViewEvent anEvent)
    {
        // Get selected file and/or text
        WebFile file = (WebFile) anEvent.getSelItem();
        String text = anEvent.getStringValue();

        // If file available, open file
        if (file != null)
            _pagePane.setBrowserFile(file);

            // If text available, either open URL or search for string
        else if (text != null && text.length() > 0) {
            int colon = text.indexOf(':');
            if (colon > 0 && colon < 6) {
                WebURL url = WebURL.getURL(text);
                _pagePane.setBrowserURL(url);
            }
            else {
                SearchPane searchTool = _projTools.getSearchTool();
                searchTool.search(text);
                _projTools.showToolForClass(SearchPane.class);
            }
        }

        // Clear SearchComboBox
        setViewText("SearchComboBox", null);
    }

    /**
     * Creates a pop-up menu for preview edit button (currently with look and feel options).
     */
    private MenuItem[] getRunMenuButtonItems()
    {
        ViewBuilder<MenuItem> mib = new ViewBuilder<>(MenuItem.class);

        // Add RunConfigs MenuItems
        List<RunConfig> runConfigs = RunConfigs.get(getRootSite()).getRunConfigs();
        for (RunConfig runConfig : runConfigs) {
            String name = runConfig.getName() + "RunConfigMenuItem";
            mib.name(name).text(name).save();
        }

        // Add separator
        if (runConfigs.size() > 0)
            mib.save();

        // Add RunConfigsMenuItem
        mib.name("RunConfigsMenuItem").text("Run Configurations...").save();
        mib.name("ShowHistoryMenuItem").text("Show History...").save();

        // Return MenuItems
        return mib.buildAll();
    }

    /**
     * Sets the RunMenuButton items.
     */
    public void setRunMenuButtonItems()
    {
        MenuButton rmb = getView("RunMenuButton", MenuButton.class);
        rmb.setItems(Arrays.asList(getRunMenuButtonItems()));
        for (MenuItem mi : rmb.getItems())
            mi.setOwner(this);
    }

    /**
     * Returns a list of files for given prefix.
     */
    private List<WebFile> getFilesForPrefix(String aPrefix)
    {
        if (aPrefix.length() == 0) return Collections.EMPTY_LIST;
        List<WebFile> files = new ArrayList<>();

        for (WebSite site : _projPane.getSites())
            getFilesForPrefix(aPrefix, site.getRootDir(), files);
        files.sort(_fileComparator);
        return files;
    }

    /**
     * Gets files for given name prefix.
     */
    private void getFilesForPrefix(String aPrefix, WebFile aFile, List<WebFile> theFiles)
    {
        // If hidden file, just return
        SitePane sitePane = SitePane.get(aFile.getSite());
        if (sitePane.isHiddenFile(aFile))
            return;

        // If directory, recurse
        if (aFile.isDir()) for (WebFile file : aFile.getFiles())
            getFilesForPrefix(aPrefix, file, theFiles);

            // If file that starts with prefix, add to files
        else if (StringUtils.startsWithIC(aFile.getName(), aPrefix))
            theFiles.add(aFile);
    }

    /**
     * Comparator for files.
     */
    Comparator<WebFile> _fileComparator = (o1, o2) -> {
        int c = o1.getSimpleName().compareToIgnoreCase(o2.getSimpleName());
        return c != 0 ? c : o1.getName().compareToIgnoreCase(o2.getName());
    };
}