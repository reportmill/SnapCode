package snapcode.app;
import snap.geom.Insets;
import snapcode.apptools.BuildTool;
import snapcode.apptools.RunTool;
import snapcode.apptools.RunToolUtils;
import snapcode.project.Project;
import snap.gfx.*;
import snap.util.StringUtils;
import snap.view.*;
import snap.viewx.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.apptools.SearchTool;
import java.util.*;

/**
 * ToolBar.
 */
public class MainToolBar extends WorkspaceTool {

    /**
     * Constructor.
     */
    public MainToolBar(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Selects the search text.
     */
    public void selectSearchText()
    {
        runLater(() -> requestFocus("SearchComboBox"));
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
        searchText.getLabel().setImage(Image.getImageForClassResource(TextPane.class, "Find.png"));
        TextField.setBackLabelAlignAnimatedOnFocused(searchText, true);

        // Configure RunConfigMenuButton
        MenuButton runConfigMenuButton = getView("RunConfigMenuButton", MenuButton.class);
        runConfigMenuButton.getLabel().setPadding(new Insets(0, 0, 0, 5));
    }

    /**
     * ResetUI
     */
    @Override
    protected void resetUI()
    {
        // Update BackButton, ForwardButton
        setViewEnabled("BackButton", _pagePane.getBrowser().getLastURL() != null);
        setViewEnabled("ForwardButton", _pagePane.getBrowser().getNextURL() != null);

        // Update RunConfigMenuButton
        MenuButton runConfigMenuButton = getView("RunConfigMenuButton", MenuButton.class);
        runConfigMenuButton.setText("Run Config");
        MenuItem[] runConfigMenuItems = getRunConfigMenuItems();
        runConfigMenuButton.setText(runConfigMenuItems[0].getText());
        runConfigMenuButton.setItems(Arrays.asList(runConfigMenuItems));

        // Update TerminateButton
        boolean isRunning = _workspaceTools.getRunTool().isRunning();
        setViewEnabled("TerminateButton", isRunning);
    }

    /**
     * Respond to UI changes.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle HomeButton, BackButton, ForwardButton
            case "HomeButton": _pagePane.showHomePage(); break;
            case "BackButton": _pagePane.getBrowser().trackBack(); break;
            case "ForwardButton": _pagePane.getBrowser().trackForward(); break;

            // Handle RunButton, DebugButton, TerminateButton, BuildButton
            case "RunButton": _workspaceTools.getRunTool().runAppForSelFile(false); break;
            case "DebugButton":
                if (anEvent.isAltDown()) { String str = null; str.length(); } // Hidden trigger to test NPE
                _workspaceTools.getRunTool().runAppForSelFile(true); break;
            case "TerminateButton": _workspaceTools.getRunTool().cancelRun(); break;
            case "BuildButton": _workspaceTools.getToolForClass(BuildTool.class).buildWorkspace(); break;

            // Handle SearchComboBox
            case "SearchComboBox": handleSearchComboBox(anEvent); break;
        }
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
        else if (text != null && !text.isEmpty()) {
            int colon = text.indexOf(':');
            if (colon > 0 && colon < 6) {
                WebURL url = WebURL.getURL(text);
                _pagePane.setBrowserURL(url);
            }
            else {
                SearchTool searchTool = _workspaceTools.getToolForClass(SearchTool.class);
                searchTool.search(text);
                _workspaceTools.showToolForClass(SearchTool.class);
            }
        }

        // Clear SearchComboBox
        setViewText("SearchComboBox", null);
    }

    /**
     * Returns a list of files for given prefix.
     */
    private List<WebFile> getFilesForPrefix(String aPrefix)
    {
        if (aPrefix.isEmpty()) return Collections.EMPTY_LIST;
        List<WebFile> files = new ArrayList<>();

        for (WebSite site : _workspacePane.getSites())
            findFilesForPrefix(aPrefix, site.getRootDir(), files);
        files.sort(_fileComparator);
        return files;
    }

    /**
     * Finds files for given name prefix and adds to list.
     */
    private void findFilesForPrefix(String aPrefix, WebFile aFile, List<WebFile> theFiles)
    {
        // If hidden file, just return
        Project project = Project.getProjectForFile(aFile);
        ProjectPane projectPane = _workspacePane.getProjectPaneForProject(project);
        if (projectPane != null && projectPane.isHiddenFile(aFile))
            return;

        // If directory, recurse
        if (aFile.isDir()) for (WebFile file : aFile.getFiles())
            findFilesForPrefix(aPrefix, file, theFiles);

            // If file that starts with prefix, add to files
        else if (StringUtils.startsWithIC(aFile.getName(), aPrefix))
            theFiles.add(aFile);
    }

    /**
     * Returns the run config menu items.
     */
    private MenuItem[] getRunConfigMenuItems()
    {
        // Create menu item for current main class source file
        ViewBuilder<MenuItem> menuItemBuilder = new ViewBuilder<>(MenuItem.class);
        RunTool runTool = _workspaceTools.getRunTool();
        WebFile mainClassFile = RunToolUtils.getMainClassSourceFile(runTool);
        if (mainClassFile != null)
            menuItemBuilder.text(mainClassFile.getSimpleName()).name("RunConfigNameMenuItem").save();
        else menuItemBuilder.text("Current File").name("RunConfigNameMenuItem").save();

        // Add separator and EditRunConfigsMenuItem
        menuItemBuilder.save();
        menuItemBuilder.text("Edit Configurations").name("EditRunConfigsMenuItem").save();
        return menuItemBuilder.buildAll();
    }

    /**
     * Comparator for files.
     */
    Comparator<WebFile> _fileComparator = (o1, o2) -> {
        int c = o1.getSimpleName().compareToIgnoreCase(o2.getSimpleName());
        return c != 0 ? c : o1.getName().compareToIgnoreCase(o2.getName());
    };
}