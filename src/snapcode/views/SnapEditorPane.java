package snapcode.views;
import snap.props.PropChangeListener;
import snapcode.javatext.JavaDoc;
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

    // The JavaTextArea to do real editing
    private JavaTextArea _javaTextArea;

    // The node path
    private RowView _nodePathBox;

    // The deepest node view of current NodePath (which is SelNodeView, unless NodePath changed SelNode)
    private JNodeView<?> _deepSelNodeView;

    // A prop change listener for JavaTextArea.SelNode changes
    private PropChangeListener _javaTextAreaSelNodeChanged = pc -> javaTextAreaSelNodeChanged();

    /**
     * Constructor.
     */
    public SnapEditorPane(JavaTextPane javaTextPane)
    {
        _javaTextPane = javaTextPane;
        _javaTextArea = javaTextPane.getTextArea();
    }

    /**
     * Called when done using pane to clean up (remove listeners).
     */
    protected void closeEditorPane()
    {
        _javaTextArea.removePropChangeListener(_javaTextAreaSelNodeChanged);
        if (_editor != null)
            _editor.closeEditor();
    }

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
        _editor = new SnapEditor(_javaTextArea);

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
        // Register to reset UI when JavaTextArea.SelNode changes
        _javaTextArea.addPropChangeListener(_javaTextAreaSelNodeChanged, JavaTextArea.SelNode_Prop);

        // Add action handlers
        addKeyActionHandler("CutButton", "Shortcut+X");
        addKeyActionHandler("CopyButton", "Shortcut+C");
        addKeyActionHandler("PasteButton", "Shortcut+V");
        addKeyActionHandler("UndoButton", "Shortcut+Z");
        addKeyActionHandler("RedoButton", "Shortcut+Shift+Z");
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Update UndoButton, RedoButton
        Undoer undoer = _javaTextArea.getUndoer();
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
        switch (anEvent.getName()) {

            // Handle SaveButton
            case "SaveButton": _javaTextPane.saveTextToFile(); break;

            // Handle CutButton, CopyButton, PasteButton, DeleteButton
            case "CutButton": _editor.cut(); break;
            case "CopyButton": _editor.copy(); break;
            case "PasteButton": _editor.paste(); break;
            case "DeleteButton": _editor.delete(); break;

            // Handle UndoButton, RedoButton
            case "UndoButton": _editor.undo(); break;
            case "RedoButton": _editor.redo(); break;

            // Handle JavaDocButton
            case "JavaDocButton":
                JavaDoc javaDoc = _javaTextPane.getJavaDoc();
                if (javaDoc != null)
                    javaDoc.openUrl();
                break;
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
     * Called when JavaTextArea.SelNode changes.
     */
    private void javaTextAreaSelNodeChanged()
    {
        _deepSelNodeView = _editor.getSelNodeView();
        resetLater();
    }
}