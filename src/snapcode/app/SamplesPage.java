package snapcode.app;
import snap.view.ScrollView;
import snap.view.View;
import snap.web.WebURL;
import snapcode.webbrowser.WebPage;

/**
 * This page class shows samples.
 */
public class SamplesPage extends WebPage {

    // The WorkspacePane
    protected WorkspacePane _workspacePane;

    // The SamplesPageView
    private SamplesPageView _samplesPageView;

    /**
     * Constructor.
     */
    public SamplesPage(WorkspacePane workspacePane)
    {
        super();
        _workspacePane = workspacePane;

        WebURL samplesPageUrl = WebURL.getResourceUrl(getClass(), "SamplesPage.md"); assert (samplesPageUrl != null);
        setURL(samplesPageUrl);
    }

    /**
     * Called to resolve links.
     */
    protected void handleLinkClick(String urlAddr)
    {
        // Handle any link with "Sample:..."
        if (urlAddr.startsWith("Sample:")) {
            String sampleUrlAddr = urlAddr.substring("Sample:".length());
            WebURL sampleUrl = WebURL.getUrl(sampleUrlAddr);
            WorkspacePaneUtils.openSamplesUrl(_workspacePane, sampleUrl);
        }
    }

    /**
     * Create UI.
     */
    @Override
    protected View createUI()
    {
        _samplesPageView = new SamplesPageView(this);
        ScrollView scrollView = new ScrollView(_samplesPageView);
        scrollView.setBorder(null);
        return scrollView;
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        WebURL homePageUrl = getURL();
        String homePageText = homePageUrl.getText();
        _samplesPageView.setMarkDown(homePageText);
    }
}
