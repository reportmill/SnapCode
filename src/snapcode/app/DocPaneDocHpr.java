/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import javakit.ide.JavaTextUtils;
import javakit.parse.JavaTextDoc;
import javakit.parse.JeplTextDoc;
import javakit.project.Project;
import javakit.resolver.Resolver;
import snap.gfx.Font;
import snap.props.PropObject;
import snap.props.Undoer;
import snap.text.TextStyle;
import snap.util.SnapUtils;
import snap.view.TextArea;
import snap.view.View;
import snap.view.ViewEvent;
import snap.view.WindowView;
import snap.viewx.DialogBox;
import snap.viewx.FilePanel;
import snap.viewx.RecentFiles;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcharts.data.DoubleArray;

/**
 * This class is a helper for DocPane to handle document open/save/close/etc.
 */
public class DocPaneDocHpr {

    // The DocPane
    private DocPane  _docPane;

    // A temp file to be used if file hasn't been saved
    private WebFile  _tempFile;

    // Constants
    public static final String JAVA_FILE_EXT = "jepl";
    public static final String RECENT_FILES_ID = "RecentJeplDocs";

    /**
     * Constructor.
     */
    public DocPaneDocHpr(DocPane aDocPane)
    {
        super();
        _docPane = aDocPane;
    }

    /**
     * Creates a JavaTextDoc for given source URL.
     */
    public static JeplTextDoc createJeplTextDoc(WebURL aSourceURL)
    {
        // Create/config/set Doc
        JeplTextDoc jeplDoc = new JeplTextDoc();

        // Get template Java text string
        jeplDoc.setSuperClassName(ChartsREPL.class.getName());
        jeplDoc.addImport("snapcharts.data.*");
        jeplDoc.addImport("snapcode.app.*");

        // Create/set resolver
        Resolver resolver = createResolver();
        jeplDoc.setResolver(resolver);

        // Set code font
        Font codeFont = JavaTextUtils.getCodeFont();
        jeplDoc.setDefaultStyle(new TextStyle(codeFont));

        // Read from URL
        if (aSourceURL != null)
            jeplDoc.readFromSourceURL(aSourceURL);

        // Return
        return jeplDoc;
    }

    /**
     * Creates a new DocPane from an open panel.
     */
    public DocPane showOpenPanel(View aView)
    {
        // Get path from open panel for supported file extensions
        String[] extensions = { JAVA_FILE_EXT };
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
        _docPane.setJeplDoc(aJeplDoc);

        _docPane.resetLater();

        // If source is string, add to recent files menu
        WebURL url = aJeplDoc.getSourceURL();
        String urls = url != null ? url.getString() : null;
        if(urls != null)
            RecentFiles.addPath(RECENT_FILES_ID, urls, 10);

        // Return
        return _docPane;
    }

    /**
     * Creates a new DocPane by opening doc from given source.
     */
    public DocPane openDocFromSource(Object aSource)
    {
        // Get URL for source
        WebURL url = WebURL.getURL(aSource);

        // Get doc for URL
        JeplTextDoc jeplTextDoc = DocPaneDocHpr.createJeplTextDoc(url);

        // Forward to openDoc
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
        WebFile file = FilePanel.showSavePanelWeb(_docPane.getUI(), "Snap Java file", exts);
        if (file == null)
            return;

        // Set JeplDoc.SourceURL and save
        JeplTextDoc jeplDoc = _docPane.getJeplDoc();
        jeplDoc.setSourceURL(file.getURL());
        save();
    }

    /**
     * Saves the current editor document, running the save panel if needed.
     */
    public void save()
    {
        // If can't save to current source, do SaveAs instead
        JeplTextDoc jeplDoc = _docPane.getJeplDoc();
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
            dbox.showMessageDialog(_docPane.getUI());
            return;
        }

        // Add URL.String to RecentFilesMenu,
        String urls = url.getString();
        RecentFiles.addPath(RECENT_FILES_ID, urls, 10);

        // Clear TextArea.Undoer and EditPane.TextModified
        EditPane<?> editPane = _docPane.getEditPane();
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
        JeplTextDoc jeplDoc = _docPane.getJeplDoc();
        WebURL sourceURL = jeplDoc.getSourceURL();
        if (sourceURL == null)
            return;

        // Run option panel for revert confirmation (just return if denied)
        String msg = "Revert to saved version of " + sourceURL.getPathName() + "?";
        DialogBox dbox = new DialogBox("Revert to Saved");
        dbox.setQuestionMessage(msg);
        if (!dbox.showConfirmDialog(_docPane.getUI()))
            return;

        // Re-open filename
        sourceURL.getFile().reload();
        openDocFromSource(sourceURL);

        // Clear TextArea.Undoer and EditPane.TextModified
        EditPane<?> editPane = _docPane.getEditPane();
        TextArea textArea = editPane.getTextArea();
        Undoer undoer = textArea.getUndoer();
        undoer.reset();
        editPane.setTextModified(false);
    }

    /**
     * Returns the window title.
     */
    public String getWindowTitle()
    {
        // Get window title: Basic filename + optional "Doc edited asterisk + optional "Doc Scaled"
        JeplTextDoc jeplDoc = _docPane.getJeplDoc();
        WebURL sourceURL = jeplDoc.getSourceURL();
        String title = sourceURL != null ? sourceURL.getPath() : null;
        if (title == null)
            title = "Untitled.jepl";

        // If has undos, add asterisk
        EditPane<?> editPane = _docPane.getEditPane();
        if (editPane.isTextModified())
            title = "* " + title;
        return "SnapCode  –  " + title;
    }

    /**
     * Closes this editor pane
     */
    public void closeWindow(ViewEvent anEvent)
    {
        _docPane.getWindow().hide();
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
        else WelcomePanel.getShared().showPanel();
    }

    /**
     * Returns a temp file.
     */
    private WebFile getTempFile()
    {
        if (_tempFile != null) return _tempFile;

        // Get temp project and create temp file
        Project tempProj = Project.getTempProject();
        WebFile tempFile = tempProj.getSourceFile("Untitled.java", true, false);

        // Set, return
        return _tempFile = tempFile;
    }

    /**
     * Creates the Resolver.
     */
    private static Resolver createResolver()
    {
        // Create resolver
        Resolver resolver = Resolver.newResolverForClassLoader(JavaTextDoc.class.getClassLoader());

        // For TeaVM: Link up StaticResolver
        if (Resolver.isTeaVM) {
            javakit.resolver.StaticResolver.shared()._next = new StaticResolver();
        }

        // For Desktop: Add class paths for SnapKit, SnapCode and SnapCharts
        if (!SnapUtils.isTeaVM) {
            resolver.addClassPathForClass(PropObject.class);
            resolver.addClassPathForClass(QuickCharts.class);
            resolver.addClassPathForClass(DoubleArray.class);
        }

        // Add more common class names from SnapCode
        javakit.resolver.ClassTreeWeb.addCommonClassNames(MORE_COMMON_CLASS_NAMES);

        // Return
        return resolver;
    }

    /**
     * Common class names for browser.
     */
    private static String[] MORE_COMMON_CLASS_NAMES = {
            Quick3D.class.getName(),
            QuickCharts.class.getName(),
            QuickData.class.getName(),
            QuickDraw.class.getName(),
            QuickDrawPen.class.getName(),
            DoubleArray.class.getName()
    };
}
