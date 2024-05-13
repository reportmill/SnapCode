package snapcode.app;
import snap.view.ChildView;
import snap.view.ColView;
import snap.view.RowView;
import snapcode.util.MDNode;
import snapcode.util.MarkDownView;

/**
 * The MarkDownView for HomePage.
 */
public class HomePageView extends MarkDownView {

    // The HomePage
    private HomePage _homePage;

    /**
     * Constructor.
     */
    public HomePageView(HomePage homePage)
    {
        super();
        _homePage = homePage;
    }

    /**
     * Override to forward to HomePage.
     */
    @Override
    protected void handleLinkClick(String urlAddr)  { _homePage.handleLinkClick(urlAddr); }

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
            newView.setPropsString("Fill:#FA; Margin:20; Padding:20; Spacing:10; Align: TOP_CENTER; BorderRadius:8; MinWidth:140;");
            newView.setChildren(listItemView.getChildren());
            listItemView = newView;
        }

        // Return
        return listItemView;
    }
}
