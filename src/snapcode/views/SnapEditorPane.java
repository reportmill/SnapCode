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
    SnapEditor _editor;

    // A class for editing code
    JavaTextPane _javaPane;

    // The node path
    RowView _nodePathBox;

    // The deepest part of current NodePath (which is SelectedPart, unless NodePath changed SelectedPart)
    JNodeView _deepPart;

    // Whether to rebuild CodeArea
    boolean _rebuild = true;

    /**
     * Creates a new SnapEditorPane for given JavaTextPane.
     */
    public SnapEditorPane(JavaTextPane aJTP)
    {
        _javaPane = aJTP;
    }

    /**
     * Returns the SnapEditor.
     */
    public SnapEditor getEditor()
    {
        return _editor;
    }

    /**
     * Returns the SnapJavaPane.
     */
    public JavaTextPane getJavaTextPane()
    {
        return _javaPane;
    }

    /**
     * Returns the JavaTextArea.
     */
    public JavaTextArea getJavaTextArea()
    {
        return _javaPane.getTextArea();
    }

    /**
     * Returns the selected part.
     */
    public JNodeView getSelectedPart()
    {
        return _editor.getSelectedPart();
    }

    /**
     * Sets the selected parts.
     */
    public void setSelectedPart(JNodeView aPart)
    {
        _editor.setSelectedPart(aPart);
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
        ScrollView scrollView = new ScrollView(_editor);
        scrollView.setGrowWidth(true);
        scrollView.setGrowHeight(true);

        // Create NodePath and add to bottom
        _nodePathBox = new RowView();
        _nodePathBox.setPadding(2, 2, 2, 2);

        // Create ColView with toolbar
        ColView colView = new ColView();
        colView.setFillWidth(true);
        colView.setChildren(toolBar, scrollView, _nodePathBox);

        // Return
        return colView;
    }

    /**
     * Initialize UI.
     */
    protected void initU()
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
     * ResetUI.
     */
    public void resetUI()
    {
        // If needs rebuild, do rebuild
        if (_rebuild) {
            _editor.rebuildUI();
            _rebuild = false;
        }

        // Update UndoButton, RedoButton
        JavaTextArea javaTextArea = getJavaTextArea();
        Undoer undoer = javaTextArea.getUndoer();
        setViewEnabled("UndoButton", undoer.hasUndos());
        setViewEnabled("RedoButton",  undoer.hasRedos());

        // Update JavaDocButton
        JavaDoc javaDoc = _javaPane.getJavaDoc();
        setViewVisible("JavaDocButton", javaDoc != null);
        String javaDocButtonText = javaDoc != null ? (javaDoc.getSimpleName() + " Doc") : null;
        setViewText("JavaDocButton", javaDocButtonText);

        // Update NodePath
        rebuildNodePath();
    }

    /**
     * Respond to UI changes.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle NodePathLabel
        if (anEvent.equals("NodePathLabel")) {
            Label label = anEvent.getView(Label.class);
            JNodeView part = (JNodeView) label.getProp("SnapPart"), dpart = _deepPart;
            setSelectedPart(part);
            _deepPart = dpart;
        }

        // Handle SaveButton
        if (anEvent.equals("SaveButton")) getJavaTextPane().saveChanges();

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
            JavaDoc javaDoc = _javaPane.getJavaDoc();
            if (javaDoc != null)
                javaDoc.openUrl();
        }
    }

    /**
     * Rebuilds the NodePathBox.
     */
    void rebuildNodePath()
    {
        // Clear path and get font
        _nodePathBox.removeChildren();

        // Iterate up from DeepPart and add parts
        for (JNodeView part = _deepPart, spart = getSelectedPart(); part != null; ) {
            Label label = new Label(part.getPartString());
            label.setFont(Font.Arial12);
            label.setName("NodePathLabel");
            label.setProp("SnapPart", part);
            if (part == spart) label.setFill(Color.LIGHTGRAY);
            _nodePathBox.addChild(label, 0);
            label.setOwner(this);
            enableEvents(label, MouseRelease);
            part = part.getJNodeViewParent();
            if (part == null) break;
            Label div = new Label(" \u2022 ");
            div.setFont(Font.Arial12);
            _nodePathBox.addChild(div, 0);
        }
    }

    /**
     * Rebuilds the CodeArea UI later.
     */
    protected void rebuildLater()
    {
        _rebuild = true;
        resetLater();
    }

    /**
     * Sets the selected parts.
     */
    public void updateSelectedPart(JNodeView aPart)
    {
        //_supportPane.rebuildUI();
        resetLater();
        _deepPart = aPart;
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
        if (!(getSelectedPart() instanceof JStmtView)) {
            JStmtView stmt = getSelectedPart().getParent(JStmtView.class);
            if (stmt == null) return;
            setSelectedPart(stmt);
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

        // Get SelectedPart and drop node
        JNodeView selPart = getSelectedPart();
        if (selPart != null && node != null)
            selPart.dropNode(node, selPart.getWidth() / 2, selPart.getHeight());
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
        JNodeView par = getSelectedPart().getJNodeViewParent();
        if (par != null) setSelectedPart(par);
    }
}