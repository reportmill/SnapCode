package snapcode.views;
import javakit.parse.JavaParser;
import snapcode.javatext.JavaDoc;
import javakit.parse.JNode;
import snapcode.javatext.JavaTextArea;
import snapcode.javatext.JavaTextPane;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.props.Undoer;
import snap.view.*;

/**
 * This class manages a SnapEditor.
 */
public class SnapEditorPane extends ViewOwner {

    // The main editor UI
    private SnapEditor _editor;

    // The JavaTextPane to do real editing
    private JavaTextPane _javaTextPane;

    // The node path
    private RowView _nodePathBox;

    // The deepest node view of current NodePath (which is SelNodeView, unless NodePath changed SelNode)
    private JNodeView<?> _deepSelNodeView;

    // Whether to have editor rebuild all block views
    private boolean _rebuildAllBlockViews = true;

    /**
     * Creates a new SnapEditorPane for given JavaTextPane.
     */
    public SnapEditorPane(JavaTextPane aJTP)
    {
        _javaTextPane = aJTP;
    }

    /**
     * Returns the SnapEditor.
     */
    public SnapEditor getEditor()  { return _editor; }

    /**
     * Returns the SnapJavaPane.
     */
    public JavaTextPane getJavaTextPane()  { return _javaTextPane; }

    /**
     * Returns the JavaTextArea.
     */
    public JavaTextArea getJavaTextArea()  { return _javaTextPane.getTextArea(); }

    /**
     * Returns the selected node view.
     */
    public JNodeView<?> getSelNodeView()  { return _editor.getSelNodeView(); }

    /**
     * Sets the selected node view.
     */
    public void setSelNodeView(JNodeView<?> nodeView)
    {
        _editor.setSelNodeView(nodeView);
    }

    /**
     * Create UI.
     */
    protected View createUI()
    {
        // Get normal UI
        View toolBar = super.createUI(); //toolBar.setMaxHeight(28);

        // Create SnapEditor
        JavaTextArea javaTextArea = getJavaTextArea();
        _editor = new SnapEditor(javaTextArea);

        // Add to Editor.UI to ScrollView
        ScrollView editorScrollView = new ScrollView(_editor);
        editorScrollView.setGrowWidth(true);
        editorScrollView.setGrowHeight(true);

        // Create NodePath and add to bottom
        _nodePathBox = new RowView();
        _nodePathBox.setPadding(2, 2, 2, 2);

        // Create ColView with toolbar
        ColView colView = new ColView();
        colView.setFillWidth(true);
        colView.setChildren(toolBar, editorScrollView, _nodePathBox);

        // Return
        return colView;
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        addKeyActionHandler("CutButton", "Shortcut+X");
        addKeyActionHandler("CopyButton", "Shortcut+C");
        addKeyActionHandler("PasteButton", "Shortcut+V");
        addKeyActionHandler("DeleteButton", "DELETE");
        addKeyActionHandler("DeleteButton", "BACKSPACE");
        addKeyActionHandler("UndoButton", "Shortcut+Z");
        addKeyActionHandler("RedoButton", "Shortcut+Shift+Z");
        addKeyActionHandler("Escape", "ESC");
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // If needs rebuild, do rebuild
        if (_rebuildAllBlockViews) {
            _editor.rebuildAllBlockViews();
            _rebuildAllBlockViews = false;
        }

        // Update UndoButton, RedoButton
        JavaTextArea javaTextArea = getJavaTextArea();
        Undoer undoer = javaTextArea.getUndoer();
        setViewEnabled("UndoButton", undoer.hasUndos());
        setViewEnabled("RedoButton",  undoer.hasRedos());

        // Update JavaDocButton
        JavaDoc javaDoc = _javaTextPane.getJavaDoc();
        setViewVisible("JavaDocButton", javaDoc != null);
        String javaDocButtonText = javaDoc != null ? (javaDoc.getSimpleName() + " Doc") : null;
        setViewText("JavaDocButton", javaDocButtonText);

        // Update NodePath
        rebuildNodePath();
    }

    /**
     * Respond to UI changes.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle SaveButton
        if (anEvent.equals("SaveButton"))
            getJavaTextPane().saveChanges();

        // Handle CutButton, CopyButton, PasteButton, Escape
        if (anEvent.equals("CutButton")) cut();
        if (anEvent.equals("CopyButton")) copy();
        if (anEvent.equals("PasteButton")) paste();
        if (anEvent.equals("DeleteButton")) delete();
        if (anEvent.equals("Escape")) escape();

        // Handle UndoButton, RedoButton
        if (anEvent.equals("UndoButton")) undo();
        if (anEvent.equals("RedoButton")) redo();

            // Handle JavaDocButton
        else if (anEvent.equals("JavaDocButton")) {
            JavaDoc javaDoc = _javaTextPane.getJavaDoc();
            if (javaDoc != null)
                javaDoc.openUrl();
        }
    }

    /**
     * Rebuilds the NodePathBox.
     */
    private void rebuildNodePath()
    {
        // Clear path and get font
        _nodePathBox.removeChildren();

        // Iterate up from DeepNodeView and add all nodes
        for (JNodeView<?> nodeView = _deepSelNodeView, selNodeView = getSelNodeView(); nodeView != null; ) {

            // Create and configure label for node view
            Label nodeViewLabel = new Label(nodeView.getNodeString());
            nodeViewLabel.setFont(Font.Arial12);
            nodeViewLabel.setName("NodePathLabel");
            nodeViewLabel.setProp("SnapPart", nodeView);
            if (nodeView == selNodeView)
                nodeViewLabel.setFill(Color.LIGHTGRAY);
            nodeViewLabel.addEventHandler(this::handleNodePathLabelMouseRelease, MouseRelease);
            _nodePathBox.addChild(nodeViewLabel, 0);
            nodeViewLabel.setOwner(this);

            // Iterate
            nodeView = nodeView.getNodeViewParent();
            if (nodeView == null)
                break;

            // Add divider
            Label dividerLabel = new Label(" \u2022 ");
            dividerLabel.setFont(Font.Arial12);
            _nodePathBox.addChild(dividerLabel, 0);
        }
    }

    /**
     * Rebuilds the CodeArea UI later.
     */
    protected void rebuildLater()
    {
        _rebuildAllBlockViews = true;
        resetLater();
    }

    /**
     * Called when NodePathLabel has mouse release.
     */
    private void handleNodePathLabelMouseRelease(ViewEvent anEvent)
    {
        Label nodePathLabel = anEvent.getView(Label.class);
        JNodeView<?> labelNodeView = (JNodeView<?>) nodePathLabel.getProp("SnapPart");
        JNodeView<?> deepSelNodeView = _deepSelNodeView;
        setSelNodeView(labelNodeView);
        _deepSelNodeView = deepSelNodeView;
    }

    /**
     * Sets the selected node view.
     */
    protected void updateSelNodeView(JNodeView<?> nodeView)
    {
        _deepSelNodeView = nodeView;
        resetLater();
    }

    /**
     * Cut current selection to clipboard.
     */
    public void cut()
    {
        copy();
        delete();
    }

    /**
     * Copy current selection to clipboard.
     */
    public void copy()
    {
        // Make sure statement is selected
        if (!(getSelNodeView() instanceof JStmtView)) {
            JNodeView<?> selNodeView = getSelNodeView();
            JStmtView<?> stmtView = selNodeView instanceof JStmtView ? (JStmtView<?>) selNodeView : selNodeView.getParent(JStmtView.class);
            if (stmtView == null)
                return;
            setSelNodeView(stmtView);
        }

        // Do copy
        getJavaTextArea().copy();
    }

    /**
     * Paste ClipBoard contents.
     */
    public void paste()
    {
        // Get Clipboard String
        Clipboard clipboard = Clipboard.get();
        String str = clipboard.hasString() ? clipboard.getString() : null;
        if (str == null)
            return;

        // Parse for statement or expression
        JavaParser javaParser = JavaParser.getShared();
        JNode node = null;
        try { node = javaParser.parseStatement(str, 0); }
        catch (Exception ignore) { }
        if (node == null) {
            try { node = javaParser.parseExpression(str); }
            catch (Exception ignore) { }
        }

        // Get SelNodeView and drop node
        JNodeView<?> selNodeView = getSelNodeView();
        if (selNodeView != null && node != null)
            selNodeView.dropNode(node, selNodeView.getWidth() / 2, selNodeView.getHeight());
    }

    /**
     * Delete current selection.
     */
    public void delete()
    {
        getJavaTextArea().delete();
        rebuildLater();
    }

    /**
     * Undo last change.
     */
    public void undo()
    {
        getJavaTextArea().undo();
        rebuildLater();
    }

    /**
     * Redo last undo.
     */
    public void redo()
    {
        getJavaTextArea().redo();
        rebuildLater();
    }

    /**
     * Escape.
     */
    public void escape()
    {
        JNodeView par = getSelNodeView().getNodeViewParent();
        if (par != null) setSelNodeView(par);
    }
}