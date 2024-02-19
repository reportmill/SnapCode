package snapcode.util;
import snap.geom.Pos;
import snap.gfx.*;
import snap.util.TaskRunner;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.viewx.DialogSheet;
import snap.web.WebURL;
import java.util.function.Consumer;

/**
 * A class to show samples.
 */
public class SamplesPane extends ViewOwner {

    // A consumer for resulting URL
    private Consumer<WebURL>  _handler;

    // The selected doc
    private SampleDoc _selDoc;

    // The dialog box
    private DialogSheet  _dialogSheet;

    // The samples docs
    private static SampleDoc[] _sampleDocs = new SampleDoc[0];

    // Constants
    private static final String SAMPLES_LABEL = "Select a sample file:";
    private static final Color CONTENT_FILL = new Color(.98, .98, 1d);

    /**
     * Shows the samples pane.
     */
    public void showSamples(ViewOwner anOwner, Consumer<WebURL> aHandler)
    {
        View view = anOwner.getUI();
        _handler = aHandler;

        _dialogSheet = new DialogSheet();
        _dialogSheet.setContent(getUI());
        _dialogSheet.showConfirmDialog(view);
        _dialogSheet.addPropChangeListener(pc -> dialogBoxClosed(), DialogBox.Showing_Prop);
    }

    /**
     * Called when dialog box closed.
     */
    private void dialogBoxClosed()
    {
        // If cancelled, just return
        if (_dialogSheet.isCancelled()) return;

        // Get selected URL and send to handler
        SampleDoc selDoc = getSelDoc();
        WebURL url = selDoc.getURL();
        runLater(() -> _handler.accept(url));
    }

    /**
     * Returns the selected doc.
     */
    public SampleDoc getSelDoc()  { return _selDoc; }

    /**
     * Sets the selected doc.
     */
    public void setSelDoc(SampleDoc aDoc)
    {
        // If already set, just return
        if (aDoc == _selDoc) return;

        // If old selection, turn off selected
        if (_selDoc != null)
            _selDoc.setSelected(false);

        _selDoc = aDoc;

        // If new selection, turn on selected
        if (_selDoc != null)
            _selDoc.setSelected(true);
    }

    /**
     * Creates UI.
     */
    @Override
    protected View createUI()
    {
        // Create main ColView to hold RowViews for samples
        ColView colView = new ColView();
        colView.setName("ItemColView");
        colView.setSpacing(10);
        colView.setPadding(15, 15, 15, 15);
        colView.setAlign(Pos.TOP_CENTER);
        colView.setFillWidth(true);
        colView.setFill(CONTENT_FILL);
        colView.setBorder(Color.GRAY, 1);
        colView.setPrefWidth(640);

        // Add loading label
        Label loadLabel = new Label("Loading...");
        loadLabel.setFont(Font.Arial16.copyForSize(32).getBold());
        loadLabel.setTextFill(Color.GRAY);
        colView.addChild(loadLabel);

        // Create ScrollView
        ScrollView scrollView = new ScrollView(colView);
        scrollView.setPrefHeight(450);

        // Create "Select template" label
        Label selectLabel = new Label(SAMPLES_LABEL);
        selectLabel.setFont(Font.Arial16.copyForSize(20).getBold());

        // Create HeaderRow to hold SelectLabel
        RowView headerRow = new RowView();
        headerRow.addChild(selectLabel);

        // Create top level col view to hold HeaderRow and ColView
        ColView boxView = new ColView();
        boxView.setSpacing(8);
        boxView.setFillWidth(true);
        boxView.setChildren(headerRow, scrollView);

        // Return
        return boxView;
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        if (_sampleDocs.length == 0)
            loadSampleDocs();
        else buildUI();
    }

    /**
     * Returns the samples docs.
     */
    private void loadSampleDocs()
    {
        TaskRunner<SampleDoc[]> loadSampleDocsRunner = new TaskRunner<>(SampleDoc::getSampleDocs);
        loadSampleDocsRunner.setOnSuccess(sdocs -> { _sampleDocs = sdocs; buildUI(); });
        loadSampleDocsRunner.setOnFailure(this::loadSampleDocsFailed);
        loadSampleDocsRunner.start();
    }

    /**
     * Loads failure condition.
     */
    private void loadSampleDocsFailed(Exception anException)
    {
        // Get error string and TextArea
        String str = "Failed to load index file.\n" + "Exception: " + anException;
        TextArea textArea = new TextArea();
        textArea.setText(str);

        // Add to ColView
        ColView colView = getView("ItemColView", ColView.class);
        colView.setAlign(Pos.CENTER);
        colView.addChild(textArea);
    }

    /**
     * Loads samples.
     */
    private void buildUI()
    {
        // Get ItemColView and remove children
        ColView colView = getView("ItemColView", ColView.class);
        colView.removeChildren();

        // Keep running RowView to add doc boxes to rows instead of column
        RowView rowView = null;

        // Create RowViews
        for (int i = 0; i < _sampleDocs.length; i++) {

            // Create/add new RowView for every three samples
            if (i % 3 == 0) {
                rowView = new RowView();
                rowView.setAlign(Pos.CENTER);
                colView.addChild(rowView);
            }

            // Add doc view to row
            SampleDoc sampleDoc = _sampleDocs[i];
            ColView docView = sampleDoc.getDocView();
            rowView.addChild(docView);
            sampleDoc._samplesPane = this;
        }

        // Select first doc
        if (_sampleDocs.length > 0)
            setSelDoc(_sampleDocs[0]);

        // Make sure all row views and image boxes are owned by ui
        for (View child : colView.getChildren())
            child.setOwner(this);
    }

    /**
     * Called when template ItemBox is clicked.
     */
    protected void docBoxWasPressed(SampleDoc sampleDoc, ViewEvent anEvent)
    {
        setSelDoc(sampleDoc);

        // If double-click, confirm dialog box
        if (anEvent.getClickCount() > 1)
            _dialogSheet.confirm();
    }

    /**
     * Animate SampleButton.
     */
    public static void startSamplesButtonAnim(View samplesButton)
    {
        // Configure anim
        ViewAnim anim = samplesButton.getAnim(0);
        anim.getAnim(400).setScale(1.3).getAnim(800).setScale(1.1).getAnim(1200).setScale(1.3).getAnim(1600).setScale(1.0)
                .getAnim(2400).setRotate(360);
        anim.setLoopCount(3).play();

        // Preload Sample docs
        new Thread(() -> SampleDoc.getSampleDocs()).start();
    }

    /**
     * Stops SampleButton animation.
     */
    public static void stopSamplesButtonAnim(View samplesButton)
    {
        samplesButton.getAnim(0).finish();
    }
}