/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import snap.gfx.Color;
import snap.view.*;
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
     * Shows the HelpPane.
     */
    public void showHelpPane()
    {
        // Get HelpPane (just return if already showing)
        HelpPane helpPane = getHelpPane();
        if (helpPane.isShowing())
            return;

        // Show Drawer
        showDrawer();
    }

    /**
     * Shows the HelpPane after loaded.
     */
    private void showHelpPaneWhenLoaded()
    {
        Runnable run = () -> {
            HelpPane helpPane = getHelpPane();
            View helpPaneUI = helpPane.getUI();
            helpPaneUI.addPropChangeListener(pc -> resetLater(), View.Showing_Prop);
            ViewUtils.runLater(() -> showHelpPane());
        };
        new Thread(run).start();
    }

    /**
     * Shows the Drawer.
     */
    public void showDrawer()
    {
        // If DocPane not yet showing, come back
        if (!isShowing()) {
            runLater(() -> showDrawer()); return; }

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

        // Show Drawer
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
        ParentView topView = (ParentView) getUI();
        topView.setFill(BACK_FILL);

        // Configure TopView to send ShortCut events to MenuBar first
        MenuBar menuBar = getView("MenuBar", MenuBar.class);
        topView.addEventHandler(e -> {
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
        _drawerView.setFill(new Color(.98));
        _drawerView.showTabButton(topView);
    }

    /**
     * Called when DocPane is showing.
     */
    @Override
    protected void initShowing()
    {
        super.initShowing();

        // Load HelpPane in background and show
        showHelpPaneWhenLoaded();
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

        // Handle ShowHelpMenuItem
        if (anEvent.equals("ShowHelpMenuItem")) {
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
        resetEvalValues();
        textArea.requestFocus();
    }
}
