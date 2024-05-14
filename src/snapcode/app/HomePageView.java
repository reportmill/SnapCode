package snapcode.app;
import snap.util.ArrayUtils;
import snap.view.*;
import snap.web.RecentFiles;
import snap.web.WebURL;
import snapcode.util.MDNode;
import snapcode.util.MarkDownView;

import java.util.Arrays;

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
        // Handle CreateNew, OpenRecent
        if (isDirectiveSet("CreateNew"))
            return createViewForCreateNewList(listNode);
        if (isDirectiveSet("OpenRecent"))
            return createViewForOpenRecentList();

        // Do normal version
        return super.createViewForListNode(listNode);
    }

    /**
     * Override to remap CreateNew list.
     */
    @Override
    protected ChildView createViewForListItemNode(MDNode listItemNode)
    {
        // Handle CreateNew list
        if (isDirectiveSet("CreateNew"))
            return createViewForCreateNewListItem(listItemNode);

        // Do normal version
        return super.createViewForListItemNode(listItemNode);
    }

    /**
     * Creates a view for "Create New" list.
     */
    private ChildView createViewForCreateNewList(MDNode listNode)
    {
        // Do normal version
        ChildView listNodeView = super.createViewForListNode(listNode);
        setDirectiveValue("CreateNew", null);

        // Replace list view with row
        RowView newListView = new RowView();
        newListView.setMargin(listNodeView.getMargin());
        newListView.setChildren(listNodeView.getChildren());
        return newListView;
    }

    /**
     * Creates a view for "Create New" list item.
     */
    private ChildView createViewForCreateNewListItem(MDNode listItemNode)
    {
        // Get link node and children
        MDNode linkNode = listItemNode.getChildNodes()[0];
        MDNode[] linkNodeChildren = linkNode.getChildNodes();

        // Get image node and create image view
        MDNode imageNode = linkNodeChildren[0];
        View imageNodeView = createViewForImageNode(imageNode);
        imageNodeView.setMargin(0, 0, 0, 0);

        // Create text view
        MDNode textNode = linkNodeChildren[1];
        View textNodeView = createViewForTextNode(textNode);
        textNodeView.setMargin(0, 0, 0, 0);

        // Create container view
        ColView listItemView = new ColView();
        listItemView.setPropsString("Fill:#F8; Margin:20; Padding:20; Spacing:10; Align: TOP_CENTER; BorderRadius:8; MinWidth:140;");
        addLinkToLinkView(linkNode.getOtherText(), listItemView);
        listItemView.setChildren(imageNodeView, textNodeView);

        // Return
        return listItemView;
    }

    /**
     * Creates a view for "Open Recent" list.
     */
    private ChildView createViewForOpenRecentList()
    {
        // Clear directive
        setDirectiveValue("OpenRecent", null);

        // Get recent files and create views
        WebURL[] recentFileUrls = RecentFiles.getURLs();
        if (recentFileUrls.length > 8)
            recentFileUrls = Arrays.copyOfRange(recentFileUrls, 0, 8);
        View[] recentFileViews = ArrayUtils.map(recentFileUrls, this::createViewForRecentFileUrl, View.class);

        // Create ColView for list
        ColView openRecentListView = new ColView();
        openRecentListView.setPropsString("Margin:20,20,20,40;");
        openRecentListView.setChildren(recentFileViews);
        return openRecentListView;
    }

    /**
     * Creates a view for recent file url.
     */
    private ChildView createViewForRecentFileUrl(WebURL recentFileUrl)
    {
        // Create label for name
        Label nameLabel = new Label(recentFileUrl.getFilename());
        nameLabel.setPropsString("Font:Arial 13; MinWidth: 140");

        // Create separator
        Separator separator = new Separator();
        separator.setVertical(true);
        separator.setPrefSize(28, 14);

        // Create label for address
        Label addressLabel = new Label(recentFileUrl.getParent().getString());
        addressLabel.setPropsString("Font:Arial 11");

        // Create close box
        CloseBox closeBox = new CloseBox();
        closeBox.setMargin(5, 5, 5, 5);

        // Create recent file view and add children
        RowView recentFileView = new RowView();
        recentFileView.setPropsString("Fill:#F8; BorderRadius:5; Margin:5; Padding:5;");
        recentFileView.setMinWidth(500);
        recentFileView.setChildren(nameLabel, separator, addressLabel, closeBox);
        addLinkToLinkView("OpenRecent:" + recentFileUrl.getString(), recentFileView);

        // Return
        return recentFileView;
    }
}
