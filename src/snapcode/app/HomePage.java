package snapcode.app;
import snap.geom.Pos;
import snap.gfx.Color;
import snap.view.*;
import snap.web.WebURL;
import snapcode.util.MDNode;
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

        /**
         * Override to remap CreateNew list.
         */
        @Override
        protected ChildView createViewForListNode(MDNode listNode)
        {
            ChildView listNodeView = super.createViewForListNode(listNode);

            // Handle CreateNew list
            if ("true".equals(getDirectiveValue("CreateNew"))) {
                setDirectiveValue("CreateNew", null);
                RowView newView = new RowView();
                newView.setMargin(listNodeView.getMargin());
                newView.setChildren(listNodeView.getChildren());
                listNodeView = newView;
            }

            // Return
            return listNodeView;
        }

        /**
         * Override to remap CreateNew list.
         */
        @Override
        protected ChildView createViewForListItemNode(MDNode listItemNode)
        {
            ChildView listItemView = super.createViewForListItemNode(listItemNode);

            // Handle CreateNew list
            if ("true".equals(getDirectiveValue("CreateNew"))) {
                listItemView.removeChild(0);
                listItemView = (ChildView) listItemView.getChild(0);
                ColView newView = new ColView();
                newView.setFill(new Color(.98));
                newView.setBorderRadius(8);
                newView.setMargin(20, 20, 20, 20);
                newView.setPadding(20, 20, 20, 20);
                newView.setSpacing(10);
                newView.setAlign(Pos.TOP_CENTER);
                newView.setMinWidth(140);
                newView.setChildren(listItemView.getChildren());
                listItemView = newView;
            }

            // Return
            return listItemView;
        }
    }
}
