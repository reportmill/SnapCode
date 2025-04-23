package snapcode.apptools;
import snap.gfx.Color;
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
    private static final int TOP_MARGIN = 50;
    private static final int RIGHT_MARGIN = 60;
    private static final int BOTTOM_MARGIN = 50;

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
        // Update LightModeButton, DarkModeButton
        setViewValue("LightModeButton", ViewTheme.get() == ViewTheme.getLight());
        setViewValue("DarkModeButton", ViewTheme.get() == ViewTheme.getDark());
    }

    /**
     * Respond UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle CloseButton
            case "CloseButton": _popupWindow.hide(); break;

            // Handle EmailText
            case "EmailText": break;

            // Handle LightModeButton, DarkModeButton
            case "LightModeButton": setTheme(ViewTheme.getLight()); break;
            case "DarkModeButton": setTheme(ViewTheme.getDark()); break;
        }
    }

    /**
     * Sets the theme.
     */
    private void setTheme(ViewTheme theme)
    {
        ViewTheme.setTheme(theme);
        if (!(_workspacePane.getPagePane().getSelPage() instanceof JavaPage))
            _workspacePane.getPagePane().getBrowser().reloadPage();
    }
}
