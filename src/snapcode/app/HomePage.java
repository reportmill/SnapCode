package snapcode.app;
import snap.view.ScrollView;
import snap.view.View;
import snap.web.WebURL;
import snapcode.util.MarkDownView;
import snapcode.webbrowser.WebPage;

/**
 * This page class is a useful hub for common project functions.
 */
public class HomePage extends WebPage {

    // The HomePageView
    private HomePageView _homePageView;

    // The shared instance
    private static HomePage _shared = new HomePage();

    /**
     * Constructor.
     */
    public HomePage()
    {
        super();

        WebURL homePageUrl = WebURL.getURL(getClass(), "HomePage.md"); assert (homePageUrl != null);
        setURL(homePageUrl);
    }

    /**
     * Create UI.
     */
    @Override
    protected View createUI()
    {
        _homePageView = new HomePageView();
        ScrollView scrollView = new ScrollView(_homePageView);
        scrollView.setFillWidth(true);
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
        _homePageView.setMarkDown(homePageText);
    }

    /**
     * Returns the shared instance.
     */
    public static HomePage getShared()  { return _shared; }

    /**
     * The MarkDownView for HomePage.
     */
    private class HomePageView extends MarkDownView {


    }
}
