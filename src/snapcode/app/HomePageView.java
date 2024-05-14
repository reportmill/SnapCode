package snapcode.app;
import snap.gfx.Color;
import snap.gfx.Effect;
import snap.gfx.Image;
import snap.gfx.ShadowEffect;
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

    // Constants
    private static final Effect LINK_HIGHLIGHT_EFFECT = new ShadowEffect(10, Color.get("#8888FF"), 0, 0);

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
        // Handle CreateNew, OpenRecent, OpenSamples
        if (isDirectiveSet("CreateNew"))
            return createViewForCreateNewList(listNode);
        if (isDirectiveSet("OpenRecent"))
            return createViewForOpenRecentList();
        if (isDirectiveSet("OpenSamples"))
            return createViewForOpenSamplesList(listNode);

        // Do normal version
        return super.createViewForListNode(listNode);
    }

    /**
     * Override to remap CreateNew list.
     */
    @Override
    protected ChildView createViewForListItemNode(MDNode listItemNode)
    {
        // Handle CreateNew, OpenSamples
        if (isDirectiveSet("CreateNew"))
            return createViewForCreateNewListItem(listItemNode);
        if (isDirectiveSet("OpenSamples"))
            return createViewForOpenSamplesListItem(listItemNode);

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
        newListView.setPadding(0, 0, 0, 20);
        newListView.setSpacing(20);
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
        listItemView.setPropsString("Fill:#F8; Padding:15; Spacing:6; Align: TOP_CENTER; BorderRadius:8; MinWidth:140;");
        addLinkToLinkView(listItemView, linkNode.getOtherText());
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
        openRecentListView.setPropsString("Margin:10,10,10,40;");
        openRecentListView.setChildren(recentFileViews);

        // If no files, add label
        if (recentFileUrls.length == 0) {
            Label noRecentFilesLabel = new Label("(No recent files)");
            noRecentFilesLabel.setPropsString("Font:Arial Italic 16; Margin:5");
            noRecentFilesLabel.setTextFill(Color.GRAY);
            openRecentListView.addChild(noRecentFilesLabel);
        }

        // Return
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
        closeBox.addEventHandler(e -> _homePage.removeRecentFileUrl(recentFileUrl), Action);

        // Create recent file view and add children
        RowView recentFileView = new RowView();
        recentFileView.setPropsString("Fill:#F8; BorderRadius:5; Margin:5; Padding:5;");
        recentFileView.setMinWidth(500);
        recentFileView.setChildren(nameLabel, separator, addressLabel, closeBox);
        addLinkToLinkView(recentFileView, "OpenRecent:" + recentFileUrl.getString());

        // Return
        return recentFileView;
    }

    /**
     * Creates a view for "Open Samples" list.
     */
    private ChildView createViewForOpenSamplesList(MDNode listNode)
    {
        // Do normal version
        ChildView listNodeView = super.createViewForListNode(listNode);

        // Clear directive
        setDirectiveValue("OpenSamples", null);

        // Return
        return listNodeView;
    }

    /**
     * Creates a view for "Open Samples" list item.
     */
    private ChildView createViewForOpenSamplesListItem(MDNode listItemNode)
    {
        // Get title node
        MDNode[] listNodeChildren = listItemNode.getChildNodes();
        MDNode titleNode = listNodeChildren[0];
        Label titleLabel = new Label(titleNode.getText());
        titleLabel.setPropsString("Font:Arial Bold 16;");

        // Get link node
        MDNode linkNode = listNodeChildren[1];
        String linkUrlAddr = linkNode.getOtherText();
        WebURL linkUrl = WebURL.getURL(linkUrlAddr);
        WebURL parentUrl = linkUrl.getParent();

        // Get image node and create image view
        WebURL imageUrl = parentUrl.getChild(parentUrl.getFilename() + ".png");
        Image image = Image.getImageForSource(imageUrl);
        ImageView imageView = new ImageView(image);
        imageView.setMargin(10, 10, 10, 10);
        imageView.setMaxSize(80, 80);

        // Create text view
        MDNode textNode = listNodeChildren[2];
        View textNodeView = createViewForTextNode(textNode);
        textNodeView.setPropsString("Margin:0,0,0,40; PrefWidth:500;");

        // Create row view for image and text
        RowView rowView = new RowView();
        rowView.setChildren(imageView, textNodeView);

        // Create container view
        ColView listItemView = new ColView();
        listItemView.setPropsString("Fill:#F8; Margin:0,20,10,20; Padding:10; BorderRadius:8; Align:TOP_LEFT;");
        addLinkToLinkView(listItemView, "Sample:" + linkUrlAddr);
        listItemView.setChildren(titleLabel, rowView);

        // Return
        return listItemView;
    }

    /**
     * Override to highlight link views under mouse.
     */
    @Override
    protected void addLinkToLinkView(View linkNodeView, String urlAddr)
    {
        super.addLinkToLinkView(linkNodeView, urlAddr);

        linkNodeView.addEventFilter(this::handleLinkViewMouseEnterAndExitEvents, MouseEnter, MouseExit);
    }

    private void handleLinkViewMouseEnterAndExitEvents(ViewEvent anEvent)
    {
        if (anEvent.isMouseEnter())
            anEvent.getView().setEffect(LINK_HIGHLIGHT_EFFECT);
        else if (anEvent.isMouseExit())
            anEvent.getView().setEffect(null);
    }
}
