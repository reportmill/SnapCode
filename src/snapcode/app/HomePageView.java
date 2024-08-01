package snapcode.app;
import snap.gfx.*;
import snap.util.ArrayUtils;
import snap.util.SnapUtils;
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
    private static final Color BUTTON_BACKGROUND_COLOR = Color.get("#F8F8FC");
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
     * Override to add header.
     */
    @Override
    public void setMarkDown(String markDown)
    {
        addHeader();
        super.setMarkDown(markDown);
    }

    /**
     * Adds a header.
     */
    protected void addHeader()
    {
        // Create header image for
        Image image = Image.getImageForClassResource(getClass(), "pkg.images/SnapCode.png");
        ImageView imageView = new ImageView(image);

        // Wrap image in row view and add
        RowView rowView = new RowView();
        rowView.addChild(imageView);
        addChild(rowView);

        // Add info
        View jvmText = new Label("JVM: " + System.getProperty("java.runtime.version")
                .replace("internal-_", ""));
        View osVendor = new Label(" Vendor: " + System.getProperty("java.vendor")
                .replace("Corporation", "Corp").replace("Technologies", "Tech"));
        View osText = new Label("OS: " + System.getProperty("os.name") + ", " + System.getProperty("os.arch"));
        View buildText = new Label("Build: " + SnapCodeUtils.getBuildInfo());
        ColView infoColView = new ColView();
        infoColView.setPropsString("Margin:10; Spacing:2; Align:TOP_RIGHT; LeanX:RIGHT; LeanY:TOP; Managed:false; Opacity:.5");
        infoColView.setChildren(jvmText, osVendor, osText, buildText);
        infoColView.setSizeToBestSize();
        addChild(infoColView);
    }

    /**
     * Override.
     */
    @Override
    protected View createViewForHeaderNode(MDNode headerNode)
    {
        switch (headerNode.getText()) {

            // Handle "Create New": Add ClearWorkspaceButton
            case "Create New:": {

                // Create normal headerView
                View headerView = super.createViewForHeaderNode(headerNode);

                // Create ClearWorkspaceButton
                Button clearWorkspaceButton = new Button("Clear Workspace");
                clearWorkspaceButton.setPropsString("Name:ClearWorkspaceButton; Font:Arial 13; PrefWidth:120; PrefHeight:24; Margin:0,0,0,20");
                clearWorkspaceButton.setVisible(_homePage._workspacePane.getProjects().length > 0);

                // Create row, add children and return
                RowView rowView = new RowView();
                rowView.setChildren(headerView, clearWorkspaceButton);
                return rowView;
            }

            // Handle "Open Recent": Add OpenButton
            case "Open Recent:": {

                // Create normal headerView
                View headerView = super.createViewForHeaderNode(headerNode);

                // Create OpenButton
                Button openButton = new Button("Open...");
                openButton.setPropsString("Name:OpenButton; PrefWidth:100; PrefHeight:24; Margin:0,0,0,20");
                if (SnapUtils.isWebVM) {
                    openButton.setText("Open Browser File...");
                    openButton.setPrefWidth(130);
                }

                // Create OpenDesktopFileButton
                Button openDesktopFileButton = new Button("Open Desktop File...");
                openDesktopFileButton.setPropsString("Name:OpenDesktopFileButton; PrefWidth:130; PrefHeight:24; Margin:0,0,0,12");
                openDesktopFileButton.setVisible(SnapUtils.isWebVM);

                // Create row, add children and return
                RowView rowView = new RowView();
                rowView.setChildren(headerView, openButton, openDesktopFileButton);
                return rowView;
            }

            // Handle "Open Samples": Add link to SamplesPage
            case "Open Samples:": {

                // Create normal headerView
                View headerView = super.createViewForHeaderNode(headerNode);

                // Create ShowSamplesPageText
                Label showSamplesText = new Label("Show all samples ...");
                showSamplesText.setPropsString("Font:Arial Italic 20; Margin:0,0,0,30; Padding:0,0,2,0;");
                showSamplesText.setTextStylePropsString("Color:#6666FF; Underline:1;");
                showSamplesText.setCursor(Cursor.HAND);
                showSamplesText.addEventHandler(e -> _homePage.showSamplesPage(), MousePress);

                // Create row, add children and return
                RowView rowView = new RowView();
                rowView.setChildren(headerView, showSamplesText);
                return rowView;
            }
        }

        // Do normal version
        return super.createViewForHeaderNode(headerNode);
    }

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
        Label addressLabel = new Label(recentFileUrl.getString());
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
        // Create views for list items
        MDNode[] listItemNodes = listNode.getChildNodes();
        View[] listItemViews = ArrayUtils.map(listItemNodes, node -> createViewForOpenSamplesListItem(node), View.class);

        // Calculate rows and create rowView array
        int colCount = 5;
        int rowCount = (int) Math.ceil(listItemViews.length / (double) colCount);
        RowView[] rowViews = new RowView[rowCount];

        // Add items to row views
        for (int i = 0; i < rowCount; i++) {

            // Get views in row
            int startIndex = i * colCount;
            int endIndex = Math.min(startIndex + colCount, listItemViews.length);
            View[] rowItemViews = Arrays.copyOfRange(listItemViews, startIndex, endIndex);

            // Create rowView and add row item views
            RowView rowView = rowViews[i] = new RowView();
            rowView.setChildren(rowItemViews);
        }

        // Create ColView and add rows
        ChildView listNodeView = new ColView();
        listNodeView.setMargin(0, 20, 0, 30);
        listNodeView.setChildren(rowViews);

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
        titleLabel.setPropsString("Font:Arial Bold 14; Margin:10,10,5,15;");

        // Get link node
        MDNode linkNode = listNodeChildren[1];
        String linkUrlAddr = linkNode.getOtherText();
        WebURL linkUrl = WebURL.getURL(linkUrlAddr); assert (linkUrl != null);
        WebURL parentUrl = linkUrl.getParent();

        // Get image node and create image view
        WebURL imageUrl = parentUrl.getChild(parentUrl.getFilename() + ".png");
        Image image = Image.getImageForSource(imageUrl);
        ImageView imageView = new ImageView(image);
        imageView.setMargin(5, 0, 5, 5);
        imageView.setMaxSize(100, 100);

        // Create row view for image and text
        ColView listItemView = new ColView();
        listItemView.setPropsString("Fill:#F8; Margin:5,15,5,15; Padding:5; BorderRadius:8; MinWidth:140; Align:TOP_CENTER;");
        listItemView.setChildren(titleLabel, imageView);
        addLinkToLinkView(listItemView, "Sample:" + linkUrlAddr);

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

        linkNodeView.setFill(BUTTON_BACKGROUND_COLOR);
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
