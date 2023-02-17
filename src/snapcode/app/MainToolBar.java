package snapcode.app;
import javakit.project.Project;
import snap.gfx.*;
import snap.util.StringUtils;
import snap.view.*;
import snap.viewx.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.apptools.EvalTool;
import snapcode.apptools.SearchTool;
import snapcode.util.SamplesPane;

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
        searchText.getLabel().setImage(Image.get(TextPane.class, "Find.png"));
        TextField.setBackLabelAlignAnimatedOnFocused(searchText, true);
    }

    /**
     * Respond to UI changes.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle SearchComboBox
        if (anEvent.equals("SearchComboBox"))
            handleSearchComboBox(anEvent);

        // Handle SamplesButton
        if (anEvent.equals("SamplesButton"))
            showSamples();
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
                SearchTool searchTool = _workspaceTools.getSearchTool();
                searchTool.search(text);
                _workspaceTools.showToolForClass(SearchTool.class);
            }
        }

        // Clear SearchComboBox
        setViewText("SearchComboBox", null);
    }

    /**
     * Shows samples.
     */
    public void showSamples()
    {
        stopSamplesButtonAnim();
        new SamplesPane().showSamples(_workspacePane, url -> showSamplesDidReturnURL(url));
    }

    /**
     * Called when SamplesPane returns a URL.
     */
    public void showSamplesDidReturnURL(WebURL aURL)
    {
        _workspacePane.setWorkspaceForJeplFileSource(aURL);

        // Kick off run
        _workspaceTools = _workspacePane.getWorkspaceTools();
        EvalTool evalTool = _workspaceTools.getToolForClass(EvalTool.class);
        if (evalTool != null && !evalTool.isAutoRun())
            evalTool.runApp(false);
    }

    /**
     * Animate SampleButton.
     */
    public void startSamplesButtonAnim()
    {
        View samplesButton = getView("SamplesButton");
        samplesButton.setVisible(true);
        SamplesPane.startSamplesButtonAnim(samplesButton);
    }

    /**
     * Stops SampleButton animation.
     */
    private void stopSamplesButtonAnim()
    {
        View samplesButton = getView("SamplesButton");
        SamplesPane.stopSamplesButtonAnim(samplesButton);
    }

    /**
     * Returns a list of files for given prefix.
     */
    private List<WebFile> getFilesForPrefix(String aPrefix)
    {
        if (aPrefix.length() == 0) return Collections.EMPTY_LIST;
        List<WebFile> files = new ArrayList<>();

        for (WebSite site : _workspacePane.getSites())
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
        Project project = Project.getProjectForFile(aFile);
        ProjectPane projectPane = _workspacePane.getProjectPaneForProject(project);
        if (projectPane != null && projectPane.isHiddenFile(aFile))
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