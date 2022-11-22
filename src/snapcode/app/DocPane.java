/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import javakit.parse.JeplTextDoc;
import snap.gfx.Color;
import snap.props.PropChange;
import snap.props.Undoer;
import snap.util.SnapUtils;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.viewx.FilePanel;
import snap.viewx.RecentFiles;
import snap.web.WebFile;
import snap.web.WebURL;
import java.util.Objects;

/**
 * This class provides UI and editing for a JeplDoc.
 */
public class DocPane extends ViewOwner {

    // The JeplTextDoc
    protected JeplTextDoc  _jeplDoc;

    // The EditPane
    protected EditPane  _editPane;

    // The EvalPane
    private EvalPane  _evalPane;

    // The SplitView to hold EditPane and EvalPane
    private SplitView  _splitView;

    // Constants
    public static Color BACK_FILL = Color.WHITE;
    public static final String JAVA_FILE_EXT = "jepl";
    public static final String RECENT_FILES_ID = "RecentJeplDocs";

    /**
     * Constructor.
     */
    public DocPane()
    {
        super();

        // Create EditPane, EvalPane
        _editPane = new EditPane(this);
        _evalPane = new EvalPane(this);

        // Create/set JeplTextDoc
        _jeplDoc = DocPaneUtils.createJeplTextDoc();
    }

    /**
     * Returns the JeplTextDoc.
     */
    public JeplTextDoc getJeplDoc()  { return _jeplDoc; }

    /**
     * Sets the JeplTextDoc.
     */
    public void setJeplDoc(JeplTextDoc aJeplDoc)
    {
        _jeplDoc = aJeplDoc;
    }

    /**
     * Returns the edit pane.
     */
    public EditPane getEditPane()  { return _editPane; }

    /**
     * Returns the eval pane.
     */
    public EvalPane getEvalPane()  { return _evalPane; }

    /**
     * Reset Repl values.
     */
    public void resetEvalValues()
    {
        if (_evalPane.isAutoRun())
            _evalPane.resetEvalValues();
    }

    /**
     * Creates a new default DocPane.
     */
    public DocPane newDoc()
    {
        return this;
    }

    /**
     * Creates a new DocPane from an open panel.
     */
    public DocPane showOpenPanel(View aView)
    {
        // Get path from open panel for supported file extensions
        String[] extensions = { DocPane.JAVA_FILE_EXT };
        String path = FilePanel.showOpenPanel(aView, "Snap Java File", extensions);
        if (path == null) return null;

        // Create/return DocPane for path
        return openDocFromSource(path);
    }

    /**
     * Creates a new DocPane by opening the given doc.
     */
    public DocPane openDoc(JeplTextDoc aJeplDoc)
    {
        // Set new doc
        setJeplDoc(aJeplDoc);

        // If source is string, add to recent files menu
        WebURL url = aJeplDoc.getSourceURL();
        String urls = url != null ? url.getString() : null;
        if(urls != null)
            RecentFiles.addPath(RECENT_FILES_ID, urls, 10);

        // Return the editor
        return this;
    }

    /**
     * Creates a new DocPane by opening doc from given source.
     */
    public DocPane openDocFromSource(Object aSource)
    {
        // Get URL for source
        WebURL url = WebURL.getURL(aSource);

        // Get doc for URL
        JeplTextDoc jeplTextDoc = DocPaneUtils.createJeplTextDoc();
        jeplTextDoc.readFromSourceURL(url);

        // Return call to openDoc
        return openDoc(jeplTextDoc);
    }

    /**
     * Saves the current editor document, running the save panel.
     */
    public void saveAs()
    {
        // Run save panel, set Document.Source to path and re-save (or just return if cancelled)
        String[] exts = new String[] { JAVA_FILE_EXT };
        //String path = FilePanel.showSavePanel(getUI(), "Snap Charts File", exts); if (path==null) return;
        WebFile file = FilePanel.showSavePanelWeb(getUI(), "Snap Java file", exts);
        if (file == null)
            return;

        // Set JeplDoc.SourceURL and save
        JeplTextDoc jeplDoc = getJeplDoc();
        jeplDoc.setSourceURL(file.getURL());
        save();
    }

    /**
     * Saves the current editor document, running the save panel if needed.
     */
    public void save()
    {
        // If can't save to current source, do SaveAs instead
        JeplTextDoc jeplDoc = getJeplDoc();
        WebURL url = jeplDoc.getSourceURL();
        if (url == null) {
            saveAs();
            return;
        }

        // Do actual save - if exception, print stack trace and set error string
        try {
            jeplDoc.writeToSourceFile();
        }
        catch(Throwable e) {
            e.printStackTrace();
            String msg = "The file " + url.getPath() + " could not be saved (" + e + ").";
            DialogBox dbox = new DialogBox("Error on Save"); dbox.setErrorMessage(msg);
            dbox.showMessageDialog(getUI());
            return;
        }

        // Add URL.String to RecentFilesMenu,
        String urls = url.getString();
        RecentFiles.addPath(RECENT_FILES_ID, urls, 10);

        // Clear TextArea.Undoer and EditPane.TextModified
        EditPane editPane = getEditPane();
        TextArea textArea = editPane.getTextArea();
        Undoer undoer = textArea.getUndoer();
        undoer.reset();
        editPane.setTextModified(false);

        // Focus
        textArea.requestFocus();
    }

    /**
     * Reloads the current editor document from the last saved version.
     */
    public void revert()
    {
        // Get filename (just return if null)
        JeplTextDoc jeplDoc = getJeplDoc();
        WebURL sourceURL = jeplDoc.getSourceURL();
        if (sourceURL == null)
            return;

        // Run option panel for revert confirmation (just return if denied)
        String msg = "Revert to saved version of " + sourceURL.getPathName() + "?";
        DialogBox dbox = new DialogBox("Revert to Saved");
        dbox.setQuestionMessage(msg);
        if (!dbox.showConfirmDialog(getUI()))
            return;

        // Re-open filename
        sourceURL.getFile().reload();
        openDocFromSource(sourceURL);

        // Clear TextArea.Undoer and EditPane.TextModified
        EditPane editPane = getEditPane();
        TextArea textArea = editPane.getTextArea();
        Undoer undoer = textArea.getUndoer();
        undoer.reset();
        editPane.setTextModified(false);
    }

    /**
     * Closes this editor pane
     */
    public void closeWindow(ViewEvent anEvent)
    {
        getWindow().hide();
        anEvent.consume();
        windowClosed();
    }

    /**
     * Called when DocPane Window is closed.
     */
    protected void windowClosed()
    {
        // If another open editor is available focus on it
        DocPane docPane = WindowView.getOpenWindowOwner(DocPane.class);
        if (docPane != null)
            docPane.getEditPane().getTextArea().requestFocus();

        // If no other open editor, show WelcomePanel
        else if (docPane == null)
            WelcomePanel.getShared().showPanel();
    }

    /**
     * Returns the window title.
     */
    public String getWindowTitle()
    {
        // Get window title: Basic filename + optional "Doc edited asterisk + optional "Doc Scaled"
        JeplTextDoc jeplDoc = getJeplDoc();
        WebURL sourceURL = jeplDoc.getSourceURL();
        String title = sourceURL != null ? sourceURL.getPath() : null;
        if (title == null)
            title = "Untitled.jepl";

        // If has undos, add asterisk
        EditPane editPane = getEditPane();
        if (editPane.isTextModified())
            title = "* " + title;
        return "SnapCode  –  " + title;
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
        resetEvalValues();
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // If title has changed, update window title
        if(isWindowVisible()) {
            String title = getWindowTitle();
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
                _evalPane.resetEvalValues();
            else _evalPane.cancelRun();
        }
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
