package snapcode.app;
import snap.gfx.*;
import snap.util.StringUtils;
import snap.view.*;
import snap.viewx.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.apptools.DebugTool;
import snapcode.apptools.SearchPane;
import java.util.*;

/**
 * ToolBar.
 */
public class AppPaneToolBar extends ProjectTool {

    // A placeholder for fill from toolbar button under mouse
    private Paint  _tempFill;

    /**
     * Creates a new AppPaneToolBar.
     */
    public AppPaneToolBar(AppPane anAppPane)
    {
        super(anAppPane);
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
        searchText.getLabel().setImage(Image.get(TextPane.class, "Find.png"));
        TextField.setBackLabelAlignAnimatedOnFocused(searchText, true);

        // Enable events on buttons
        String[] buttonNames = { "HomeButton", "BackButton", "NextButton", "RefreshButton", "RunButton" };
        for (String name : buttonNames)
            enableEvents(name, MouseRelease, MouseEnter, MouseExit);
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

        // Handle SearchComboBox
        if (anEvent.equals("SearchComboBox"))
            handleSearchComboBox(anEvent);
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