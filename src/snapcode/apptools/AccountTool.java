package snapcode.apptools;
import snap.gfx.Color;
import snap.util.Prefs;
import snap.util.UserInfo;
import snap.view.*;
import snapcode.app.JavaPage;
import snapcode.app.WorkspacePane;

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
    private static final int RIGHT_MARGIN = 56;
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
        _popupWindow.setPrefSize(PREF_WIDTH, workspacePaneUI.getHeight() - TOP_MARGIN - BOTTOM_MARGIN);
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
        closeButton.setSizeToPrefSize();
        closeButton.setTextColor(Color.GRAY);
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
