/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.appjr;
import javakit.parse.JavaTextDoc;
import javakit.parse.JeplTextDoc;
import snap.gfx.Color;
import snap.props.PropChange;
import snap.util.SnapUtils;
import snap.view.*;
import snap.web.WebURL;
import snapcode.util.HelpPane;
import snapcode.util.SamplesPane;

import java.util.Objects;

/**
 * This class provides UI and editing for a JeplDoc.
 */
public class DocPane extends ViewOwner {

    // A helper class for doc IO
    private DocPaneDocHpr _docHpr;

    // The EditPane
    protected EditPane<JavaTextDoc>  _editPane;

    // The EvalPane
    protected EvalPane  _evalPane;

    // The SplitView to hold EditPane and EvalPane
    private SplitView  _splitView;

    // The MainSplitView
    private SplitView  _mainSplitView;

    // The DrawerView
    private DrawerView  _drawerView;

    // The HelpPane
    private HelpPane _helpPane;

    // Constants
    public static Color BACK_FILL = Color.WHITE;

    /**
     * Constructor.
     */
    public DocPane()
    {
        super();

        // Create/set doc helper
        _docHpr = new DocPaneDocHpr(this);

        // Create EditPane, EvalPane
        _editPane = new EditPane<>(this);
        _evalPane = new EvalPane(this);

        // Install the JeplDoc
        JeplTextDoc jeplDoc = JeplTextDoc.getJeplTextDocForSourceURL(null);
        setJeplDoc(jeplDoc);
    }

    /**
     * Returns the JeplTextDoc.
     */
    public JeplTextDoc getJeplDoc()
    {
        JavaTextDoc javaTextDoc = _editPane.getTextDoc();
        return javaTextDoc instanceof JeplTextDoc ? (JeplTextDoc) javaTextDoc : null;
    }

    /**
     * Sets the JeplTextDoc.
     */
    public void setJeplDoc(JeplTextDoc aJeplDoc)
    {
        // JeplTextDoc Project should reference
        DocPaneDocHpr.configureJeplDocProject(aJeplDoc);

        // Forward to EditPane
        _editPane.setTextDoc(aJeplDoc);
    }

    /**
     * Creates a new DocPane from an open panel.
     */
    public DocPane showOpenPanel(View aView)
    {
        return _docHpr.showOpenPanel(aView);
    }

    /**
     * Creates a new DocPane by opening doc from given source.
     */
    public DocPane openDocFromSource(Object aSource)
    {
        return _docHpr.openDocFromSource(aSource);
    }

    /**
     * Saves the current editor document, running the save panel.
     */
    public void saveAs()  { _docHpr.saveAs(); }

    /**
     * Saves the current editor document, running the save panel if needed.
     */
    public void save()  { _docHpr.save(); }

    /**
     * Reloads the current editor document from the last saved version.
     */
    public void revert()  { _docHpr.revert(); }

    /**
     * Closes this editor pane
     */
    public void closeWindow(ViewEvent anEvent)  { _docHpr.closeWindow(anEvent); }

    /**
     * Returns the edit pane.
     */
    public EditPane<?> getEditPane()  { return _editPane; }

    /**
     * Returns the eval pane.
     */
    public EvalPane getEvalPane()  { return _evalPane; }

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
     * Runs the app.
     */
    public void runApp()
    {
        if (_evalPane.isAutoRun())
            _evalPane.runApp(false);
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Get/configure SplitView
        _mainSplitView = getView("MainSplitView", SplitView.class);
        _mainSplitView.setDividerSpan(6);
        _mainSplitView.getDivider().setFill(Color.WHITE);
        _mainSplitView.getDivider().setBorder(null);
        _mainSplitView.setBorder(null);

        // Create EditPane
        _editPane.addPropChangeListener(pc -> editPaneDidPropChange(pc));
        View editPaneUI = _editPane.getUI();

        // Create EvalPane
        View evalPaneUI = _evalPane.getUI();

        // Create SplitView for EditPane and EvalPane
        _splitView = new SplitView();
        _splitView.setGrowHeight(true);
        _splitView.setVertical(false);
        _splitView.setDividerSpan(6);
        _splitView.getDivider().setFill(Color.WHITE);
        _splitView.getDivider().setBorder(Color.GRAY9, 1);
        _splitView.setBorder(null);
        _splitView.addItem(editPaneUI);
        _splitView.addItem(evalPaneUI);
        _mainSplitView.addItem(_splitView, 0);

        // Configure window
        WindowView win = getWindow();
        win.addEventHandler(e -> closeWindow(e), WinClose);

        // TeaVM: Make Window fill browser
        if (SnapUtils.isTeaVM)
            win.setMaximized(true);

        // Add RunAction
        addKeyActionHandler("RunAction", "Shortcut+R");
        addKeyActionHandler("AutoRunAction", "Shortcut+Shift+R");

        // Configure TopView
        ParentView docPaneUI = (ParentView) getUI();
        docPaneUI.setFill(BACK_FILL);

        // Configure TopView to send ShortCut events to MenuBar first
        MenuBar menuBar = getView("MenuBar", MenuBar.class);
        docPaneUI.addEventHandler(e -> {
            if (e.isShortcutDown())
                ViewUtils.processEvent(menuBar, e);
        }, KeyPress);

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
        // Run app
        runApp();

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
        // If title has changed, update window title
        if(isWindowVisible()) {
            String title = _docHpr.getWindowTitle();
            WindowView win = getWindow();
            if(!Objects.equals(title, win.getTitle())) {
                win.setTitle(title);
                JeplTextDoc jeplDoc = getJeplDoc();
                WebURL sourceURL = jeplDoc.getSourceURL();
                win.setDocURL(sourceURL);
            }
        }
    }

    /**
     * Respond to UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle RunAction, AutoRunAction
        if (anEvent.equals("RunAction")) {
            if (!_evalPane.isRunning())
                _evalPane.runApp(false);
            else _evalPane.cancelRun();
        }

        // Handle AutoRunAction
        if (anEvent.equals("AutoRunAction")) {
            _evalPane.setAutoRun(!_evalPane.isAutoRun());
            _evalPane.resetLater();
        }

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

    /**
     * Called when edit pane has prop change.
     */
    private void editPaneDidPropChange(PropChange aPC)
    {
        String propName = aPC.getPropName();
        if (propName == EditPane.TextModified_Prop)
            resetLater();
    }

    /**
     * Add HelpCode.
     */
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
