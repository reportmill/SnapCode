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
        JeplUtils.configureJeplDocProject(jeplDoc);
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
     * Shows samples.
     */
    public void showSamples()
    {
        stopSamplesButtonAnim();
        new SamplesPane().showSamples(this, url -> showSamplesDidReturnURL(url));
    }

    /**
     * Called when SamplesPane returns a URL.
     */
    private void showSamplesDidReturnURL(WebURL aURL)
    {
        openDocFromSource(aURL);

        // Kick off run
        if (!getEvalPane().isAutoRun())
            getEvalPane().runApp(false);
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
    }

    /**
     * Called when DocPane is showing.
     */
    @Override
    protected void initShowing()
    {
        runApp();
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
     * Animate SampleButton.
     */
    protected void startSamplesButtonAnim()
    {
        View samplesButton = _evalPane.getView("SamplesButton");
        SamplesPane.startSamplesButtonAnim(samplesButton);
    }

    /**
     * Stops SampleButton animation.
     */
    private void stopSamplesButtonAnim()
    {
        View samplesButton = _evalPane.getView("SamplesButton");
        SamplesPane.stopSamplesButtonAnim(samplesButton);
    }
}
