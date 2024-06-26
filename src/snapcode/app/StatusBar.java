package snapcode.app;
import snap.geom.Pos;
import snap.view.*;
import snapcode.webbrowser.WebBrowser;

/**
 * Manages status bar.
 */
public class StatusBar extends WorkspaceTool {

    /**
     * Constructor.
     */
    public StatusBar(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Add to view.
     */
    public void addToView(View aView)
    {
        // Add StatusBar to MainSplit
        View statusBar = getUI();
        statusBar.setPropsString("Margin: 0,30,0,0; LeanX: RIGHT; LeanY: BOTTOM; Managed:false;");
        statusBar.setSize(500, 30);
        ViewUtils.addChild((ParentView) aView, statusBar);
    }

    /**
     * Resets UI panel.
     */
    public void resetUI()
    {
        // Set ActivityText, StatusText
        WebBrowser browser = getBrowser();
        setViewText("ActivityText", browser.getActivity());
        setViewText("StatusText", browser.getStatus());

        // Update ProgressBar
        ProgressBar progressBar = getView("ProgressBar", ProgressBar.class);
        boolean loading = browser.isLoading();
        if (loading && !progressBar.isVisible()) {
            progressBar.setVisible(true);
            progressBar.setProgress(-1);
        }
        else if (!loading && progressBar.isVisible()) {
            progressBar.setProgress(0);
            progressBar.setVisible(false);
        }
    }
}
