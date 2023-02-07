/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.appjr;
import snap.gfx.Color;
import snap.view.*;
import snapcode.util.SamplesPane;
import snapcode.util.HelpPane;

/**
 * This class provides UI and editing for a JeplDoc.
 */
public class DocPaneX extends DocPane {

    // The MainSplitView
    private SplitView  _mainSplitView;

    // The DrawerView
    private DrawerView  _drawerView;

    // The HelpPane
    private HelpPane  _helpPane;

    /**
     * Constructor.
     */
    public DocPaneX()
    {
        super();
    }

    /**
     * Returns the HelpPane.
     */
    public HelpPane getHelpPane()
    {
        // If already set, just return
        if (_helpPane != null) return _helpPane;

        // Create, set, return
        HelpPane helpPane = new HelpPane(this);
        return _helpPane = helpPane;
    }

    /**
     * Shows the Drawer.
     */
    public void showDrawer()
    {
        _drawerView.show();
    }

    /**
     * Hides the Drawer.
     */
    public void hideDrawer()
    {
        _drawerView.hide();
    }

    /**
     * Shows samples.
     */
    public void showSamples()
    {
        stopSamplesButtonAnim();
        hideDrawer();
        new SamplesPane().showSamples(this);
    }

    /**
     * Create UI.
     */
    @Override
    protected View createUI()
    {
        // Create normal DocPane UI
        View docPaneUI = super.createUI();

        // Create DocPaneX UI from snp file
        ParentView docPaneXUI = (ParentView) createUIForClass(getClass());

        // Get SplitView and add superClass UI
        _mainSplitView = (SplitView) docPaneXUI.getChildForName("MainSplitView");
        _mainSplitView.addItem(docPaneUI, 0);

        // Return DocPaneX UI
        return docPaneXUI;
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Do normal version
        super.initUI();

        // Configure TopView
        ParentView docPaneUI = (ParentView) getUI();
        docPaneUI.setFill(BACK_FILL);

        // Configure TopView to send ShortCut events to MenuBar first
        MenuBar menuBar = getView("MenuBar", MenuBar.class);
        docPaneUI.addEventHandler(e -> {
            if (e.isShortcutDown())
                ViewUtils.processEvent(menuBar, e);
        }, KeyPress);

        // Get/configure SplitView
        _mainSplitView = getView("MainSplitView", SplitView.class);
        _mainSplitView.setDividerSpan(6);
        _mainSplitView.getDivider().setFill(Color.WHITE);
        _mainSplitView.getDivider().setBorder(null);
        _mainSplitView.setBorder(null);

        // Get/configure drawer
        _drawerView = new DrawerView();
        _drawerView.getDrawerLabel().setText("Help Pane");
        _drawerView.getTabLabel().setText("Help");
        _drawerView.showTabButton(docPaneUI);
    }

    /**
     * Called when DocPane is showing.
     */
    @Override
    protected void initShowing()
    {
        super.initShowing();

        // Load HelpPane in background and show
        runLater(() -> initDrawer());
    }

    /**
     * Special init to make sure drawer is right size.
     */
    private void initDrawer()
    {
        // Install/configure HelpPane for first time
        HelpPane helpPane = getHelpPane();
        View helpPaneUI = helpPane.getUI();
        if (helpPaneUI.getParent() == null) {
            int HELP_PANE_DEFAULT_WIDTH = 460;
            int HELP_PANE_DEFAULT_HEIGHT = 530;
            double docPaneW = getUI().getWidth();
            double docPaneH = getUI().getHeight();
            double maxW = Math.round(docPaneW * .4);
            double maxH = Math.round(docPaneH * .6);
            double helpW = Math.min(HELP_PANE_DEFAULT_WIDTH, maxW);
            double helpH = Math.min(HELP_PANE_DEFAULT_HEIGHT, maxH);
            helpPaneUI.setPrefSize(helpW, helpH);
            _drawerView.setContent(helpPaneUI);
        }
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Do normal version
        super.resetUI();
    }

    /**
     * Respond to UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Do normal version
        super.respondUI(anEvent);

        // Handle MenuBar and ToolBar
        respondUIForMenus(anEvent);

        // Handle ShowSamplesMenuItem
        if (anEvent.equals("ShowSamplesMenuItem"))
            showSamples();

        // Handle ShowHelpMenuItem
        else if (anEvent.equals("ShowHelpMenuItem")) {
            if (_helpPane != null && _helpPane.isShowing())
                hideDrawer();
            else showDrawer();
        }
    }

    /**
     * Handle resetUI for Menu events.
     */
    private void respondUIForMenus(ViewEvent anEvent)
    {
        // Handle SaveMenuItem, SaveButton, SaveAsMenuItem, RevertMenuItem
        if (anEvent.equals("SaveMenuItem") || anEvent.equals("SaveButton"))
            save();
        if (anEvent.equals("SaveAsMenuItem"))
            saveAs();
        if (anEvent.equals("RevertMenuItem"))
            revert();

        // Handle QuitMenuItem
        if (anEvent.equals("QuitMenuItem"))
            App.quitApp();

        // Handle Cut, Copy, Paste, SelectAll
        if (anEvent.equals("CutMenuItem"))
            _editPane.getTextArea().cut();
        if (anEvent.equals("CopyMenuItem"))
            _editPane.getTextArea().copy();
        if (anEvent.equals("PasteMenuItem"))
            _editPane.getTextArea().paste();
        if (anEvent.equals("SelectAllMenuItem"))
            _editPane.getTextArea().selectAll();

        // Handle Edit menu items
        //if (anEvent.equals("UndoMenuItem") || anEvent.equals("UndoButton")) editor.undo();
        //if (anEvent.equals("RedoMenuItem") || anEvent.equals("RedoButton")) editor.redo();
    }

    public void addHelpCode(String aString)
    {
        // Get JeplTextPane.TextArea
        TextArea textArea = _editPane.getTextArea();

        // If current line not empty, select end
        if (!textArea.getSel().isEmpty() || textArea.getSel().getStartLine().length() > 1)
            textArea.setSel(textArea.length(), textArea.length());

        // Add help
        textArea.replaceCharsWithContent(aString);

        // Submit entry
        runApp();
        textArea.requestFocus();
    }

    /**
     * Animate SampleButton.
     */
    protected void startSamplesButtonAnim()
    {
        // Get button
        View samplesButton = _evalPane.getView("SamplesButton");

        // Configure anim
        ViewAnim anim = samplesButton.getAnim(0);
        anim.getAnim(400).setScale(1.3).getAnim(800).setScale(1.1).getAnim(1200).setScale(1.3).getAnim(1600).setScale(1.0)
                .getAnim(2400).setRotate(360);
        anim.setLoopCount(3).play();
    }

    /**
     * Stops SampleButton animation.
     */
    private void stopSamplesButtonAnim()
    {
        View samplesButton = _evalPane.getView("SamplesButton");
        samplesButton.getAnim(0).finish();
    }
}
