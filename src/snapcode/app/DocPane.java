/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import javakit.parse.JavaTextDoc;
import javakit.parse.JeplTextDoc;
import snap.gfx.Color;
import snap.props.PropChange;
import snap.util.SnapUtils;
import snap.view.*;
import snap.web.WebURL;
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
        JeplTextDoc jeplDoc = DocPaneDocHpr.createJeplTextDoc(null);
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
    public EditPane getEditPane()  { return _editPane; }

    /**
     * Returns the eval pane.
     */
    public EvalPane getEvalPane()  { return _evalPane; }

    /**
     * Runs the app.
     */
    public void runApp()
    {
        if (_evalPane.isAutoRun())
            _evalPane.runApp(false);
    }

    /**
     * Create UI.
     */
    @Override
    protected View createUI()
    {
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

        // Return SplitView
        return _splitView;
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Configure window
        WindowView win = getWindow();
        win.addEventHandler(e -> closeWindow(e), WinClose);

        // TeaVM: Make Window fill browser
        if (SnapUtils.isTeaVM)
            win.setMaximized(true);

        // Add RunAction
        addKeyActionHandler("RunAction", "Shortcut+R");
        addKeyActionHandler("AutoRunAction", "Shortcut+Shift+R");
    }

    /**
     * Called when first showing.
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
}
