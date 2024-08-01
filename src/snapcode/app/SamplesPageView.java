package snapcode.app;
import snap.gfx.*;
import snap.util.ArrayUtils;
import snap.view.*;
import snap.web.WebURL;
import snapcode.util.MDNode;
import snapcode.util.MarkDownView;
import java.util.Arrays;

/**
 * The MarkDownView for SamplesPage.
 */
public class SamplesPageView extends MarkDownView {

    // The SamplesPage
    private SamplesPage _samplesPage;

    // Constants
    private static final Color BUTTON_BACKGROUND_COLOR = Color.get("#F8F8FC");
    private static final Effect LINK_HIGHLIGHT_EFFECT = new ShadowEffect(10, Color.get("#8888FF"), 0, 0);

    /**
     * Constructor.
     */
    public SamplesPageView(SamplesPage homePage)
    {
        super();
        _samplesPage = homePage;
    }

    /**
     * Override to forward to HomePage.
     */
    @Override
    protected void handleLinkClick(String urlAddr)  { _samplesPage.handleLinkClick(urlAddr); }

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
        // Create label for heading
        Label samplesLabel = new Label("Samples");
        samplesLabel.setPropsString("Font:Arial Bold 64;");
        samplesLabel.setTextStylePropsString("Color:#CD5652; Border:WHITE 2; CharSpacing:2;");
        samplesLabel.setEffect(new ShadowEffect(12, Color.BLACK, 0, 0));

        // Wrap image in row view and add
        RowView rowView = new RowView();
        rowView.addChild(samplesLabel);
        addChild(rowView);
    }

    /**
     * Override.
     */
    @Override
    protected View createViewForHeaderNode(MDNode headerNode)
    {
        if (headerNode.getText().equals("enCode/Create")) {

            // Create normal headerView
            View headerView = super.createViewForHeaderNode(headerNode);

            // Create learn to code label
            Label learnToCodeLabel = new Label("Learn to Code! ...");
            learnToCodeLabel.setPropsString("Font:Arial Italic 20; Margin:0,0,0,40; Padding:0,0,2,0;");
            learnToCodeLabel.setTextStylePropsString("Color:#6666FF; Underline:1;");
            learnToCodeLabel.setCursor(Cursor.HAND);
            learnToCodeLabel.addEventHandler(e -> GFXEnv.getEnv().openURL("https://www.encodecreate.com"), MousePress);

            // Create row, add children and return
            RowView rowView = new RowView();
            rowView.setChildren(headerView, learnToCodeLabel);
            return rowView;
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
        // Handle OpenSamples
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
        // Handle OpenSamples
        if (isDirectiveSet("OpenSamples"))
            return createViewForOpenSamplesListItem(listItemNode);

        // Do normal version
        return super.createViewForListItemNode(listItemNode);
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
