package snapcode.apptools;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.util.Prefs;
import snap.util.UserInfo;
import snap.view.*;
import snap.view.Button;
import snap.view.Cursor;
import snap.view.Label;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.app.JavaPage;
import snapcode.app.SnapCodeUtils;
import snapcode.app.WorkspacePane;
import java.util.List;

/**
 * This class manages settings for current user.
 */
public class AccountTool extends ViewOwner {

    // The workspace pane
    private WorkspacePane _workspacePane;

    // The popup window
    private PopupWindow _popupWindow;

    // Constants
    private static final int PREF_WIDTH = 400;
    private static final int TOP_MARGIN = 48;
    private static final int RIGHT_MARGIN = 36;
    private static final int BOTTOM_MARGIN = 50;

    // Constants
    private static final String GITHUB_USER_KEY = "GithubUser";
    private static final String GITHUB_PAC_KEY = "GithubPAC";

    /**
     * Constructor.
     */
    public AccountTool(WorkspacePane workspacePane)
    {
        super();
        _workspacePane = workspacePane;
    }

    /**
     * Shows the account tool.
     */
    public void showAccountTool()
    {
        _popupWindow = new PopupWindow();
        _popupWindow.setFocusable(true);
        _popupWindow.setContent(getUI());
        View workspacePaneUI = _workspacePane.getUI();
        _popupWindow.setPrefSize(PREF_WIDTH, getUI().getPrefHeight() + 80);
        _popupWindow.show(workspacePaneUI, workspacePaneUI.getWidth() - PREF_WIDTH - RIGHT_MARGIN, TOP_MARGIN);
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        Button closeButton = getView("CloseButton", Button.class);
        closeButton.setManaged(false);
        closeButton.setTextColor(Color.GRAY);

        // Add Info box
        getUI(ColView.class).addChild(getInfoBox());
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Update EmailText
        setViewValue("EmailText", UserInfo.getUserEmail());

        // Update LightModeButton, DarkModeButton
        setViewValue("LightModeButton", ViewTheme.get() == ViewTheme.getLight());
        setViewValue("DarkModeButton", ViewTheme.get() == ViewTheme.getDark());

        // Update GithubUserText, GithubPacText
        setViewValue("GithubUserText", getGithubUser());
        setViewValue("GithubPacText", getGithubPac());
    }

    /**
     * Respond UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle CloseButton
            case "CloseButton" -> _popupWindow.hide();

            // Handle EmailText
            case "EmailText" -> UserInfo.setUserEmail(anEvent.getStringValue());

            // Handle LightModeButton, DarkModeButton
            case "LightModeButton" -> setTheme(ViewTheme.getLight());
            case "DarkModeButton" -> setTheme(ViewTheme.getDark());

            // Handle GithubUserText, GithubPacText
            case "GithubUserText" -> setGithubUser(anEvent.getStringValue());
            case "GithubPacText" -> setGithubPac(anEvent.getStringValue());
        }
    }

    /**
     * Sets the theme.
     */
    private void setTheme(ViewTheme theme)
    {
        ViewTheme.setTheme(theme);
        if (!(_workspacePane.getPagePane().getSelPage() instanceof JavaPage))
            _workspacePane.getFilesTool().revertSelPage();
    }

    /**
     * Creates an info box
     */
    private View getInfoBox()
    {
        // Separator
        Separator separator = new Separator();
        separator.setGrowWidth(true);
        separator.setMargin(10, 0, 20, 0);

        // Header label
        Label headerLabel = new Label("System Info");
        headerLabel.setFont(Font.Arial14.getBold());
        headerLabel.setMargin(0, 0, 12, 0);

        // Add info
        View jvmText = new Label("JVM: " + System.getProperty("java.runtime.version")
                .replace("internal-_", ""));
        View osVendor = new Label("Vendor: " + System.getProperty("java.vendor")
                .replace("Corporation", "Corp").replace("Technologies", "Tech"));
        View osText = new Label("OS: " + System.getProperty("os.name") + ", " + System.getProperty("os.arch"));
        View buildText = new Label("Build: " + SnapCodeUtils.getBuildInfo());

        // Add ReleaseNotes link
        Label releaseNotesLabel = new Label("Release notes");
        releaseNotesLabel.setTextStyleString("Underline:1;");
        releaseNotesLabel.setCursor(Cursor.HAND);
        releaseNotesLabel.addEventHandler(e -> showReleaseNotes(), MousePress);

        // Set label margins
        List.of(jvmText, osVendor, osText, buildText, releaseNotesLabel).forEach(label -> label.setMargin(0, 0, 0, 12));

        // Create InfoColView and add
        ColView infoColView = new ColView();
        infoColView.setPropsString("Font: Arial 12; Margin:10; Spacing:2;");
        infoColView.setChildren(separator, headerLabel, jvmText, osVendor, osText, buildText, releaseNotesLabel);
        infoColView.setSizeToBestSize();

        // Return
        return infoColView;
    }

    /**
     * Shows the release notes.
     */
    public void showReleaseNotes()
    {
        //WebURL releaseNotesURL = WebURL.getURL("/Users/jeff/Markdown/ReleaseNotes.md");
        WebURL releaseNotesURL = WebURL.getUrl("https://reportmill.com/SnapCode/ReleaseNotes.md");
        assert releaseNotesURL != null;
        WebFile releaseNotesFile = releaseNotesURL.getFile();
        _workspacePane.openFile(releaseNotesFile);
    }

    /**
     * Returns the github user name.
     */
    public static String getGithubUser()
    {
        return Prefs.getDefaultPrefs().getString(GITHUB_USER_KEY, "");
    }

    /**
     * Sets the github user name.
     */
    public static void setGithubUser(String aValue)
    {
        Prefs.getDefaultPrefs().setValue(GITHUB_USER_KEY, aValue);
    }

    /**
     * Returns the github personal access token.
     */
    public static String getGithubPac()
    {
        return Prefs.getDefaultPrefs().getString(GITHUB_PAC_KEY, "");
    }

    /**
     * Sets the github personal access token.
     */
    public static void setGithubPac(String aValue)
    {
        Prefs.getDefaultPrefs().setValue(GITHUB_PAC_KEY, aValue);
    }
}
