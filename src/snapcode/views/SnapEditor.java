package snapcode.views;
import javakit.parse.*;
import snap.props.PropChangeListener;
import snap.text.TextModel;
import snap.view.*;
import snap.viewx.Explode;
import snapcode.javatext.JavaTextArea;
import snapcode.javatext.JavaTextAreaNodeHpr;

/**
 * The Pane that actually holds BlockView pieces.
 */
public class SnapEditor extends StackView {

    // The JavaTextArea
    private JavaTextArea _javaTextArea;

    // The JavaTextAreaNodeHpr
    private JavaTextAreaNodeHpr _nodeHpr;

    // The file view
    private JFileView _fileView;

    // The selected node view
    private JNodeView<?> _selNodeView;

    // The mouse X/Y during mouse drag
    private double _mouseX, _mouseY;

    // The currently dragging node view
    private JBlockView<?> _dragNodeView;

    // A run to rebuild all blocks after delay
    private Runnable _rebuildAllBlocksRun, REBUILD_ALL_BLOCKS_RUN = this::rebuildAllBlockViews;

    // A listener to handle JavaTextArea.TextModel.Chars changes
    private PropChangeListener _javaTextAreaTextModelCharsChangeLsnr = pc -> rebuildAllBlockViewsLater();

    /**
     * Constructor.
     */
    public SnapEditor(JavaTextArea javaTextArea)
    {
        // Basic config
        super();
        setFocusable(true);
        setFocusWhenPressed(true);
        enableEvents(KeyPress, MousePress, MouseDrag, MouseRelease);

        // Set JavaTextArea and start listening to changes
        _javaTextArea = javaTextArea;
        _javaTextArea.getTextModel().addPropChangeListener(_javaTextAreaTextModelCharsChangeLsnr, TextModel.Chars_Prop);
        _nodeHpr = _javaTextArea.getNodeHpr();

        // Create FileView and add to editor
        _fileView = new JFileView();
        _fileView._editor = this;
        _fileView.setGrowWidth(true);
        _fileView.setGrowHeight(true);
        addChild(_fileView);

        // Rebuild all blocks
        rebuildAllBlockViews();
    }

    /**
     * Called when done using editor to clean up (remove listeners).
     */
    protected void closeEditor()
    {
        _javaTextArea.getTextModel().removePropChangeListener(_javaTextAreaTextModelCharsChangeLsnr);
    }

    /**
     * Returns the JavaTextArea.
     */
    //public JavaTextArea getJavaTextArea()  { return _javaTextArea; }

    /**
     * Returns the JavaTextArea NodeHpr.
     */
    public JavaTextAreaNodeHpr getNodeHpr()  { return _nodeHpr; }

    /**
     * Returns the FileView.
     */
    public JFileView getFileView()  { return _fileView; }

    /**
     * Returns the JFile JNode.
     */
    public JFile getJFile()  { return _javaTextArea.getJFile(); }

    /**
     * Returns the selected node view.
     */
    public JNodeView<?> getSelNodeView()  { return _selNodeView; }

    /**
     * Sets the selected node view.
     */
    public void setSelNodeView(JNodeView<?> aNodeView)
    {
        if (_selNodeView != null)
            _selNodeView.setSelected(false);
        _selNodeView = aNodeView != null ? aNodeView : _fileView;
        _selNodeView.setSelected(true);

        // Update JavaTextArea selection
        JNode jnode = _selNodeView.getJNode();
        int nodeStartCharIndex = jnode.getStartCharIndex();
        int nodeEndCharIndex = jnode.getEndCharIndex();
        _javaTextArea.setSel(nodeStartCharIndex, nodeEndCharIndex);
    }

    /**
     * Rebuilds the pieces.
     */
    protected void rebuildAllBlockViews()
    {
        JFileView fileView = getFileView();
        JFile jfile = getJFile();
        fileView.setJNode(jfile);
        setSelNodeViewFromJavaTextArea();
        _rebuildAllBlocksRun = null;
    }

    /**
     * Rebuilds the pieces.
     */
    protected void rebuildAllBlockViewsLater()
    {
        if (_rebuildAllBlocksRun == null)
            ViewUtils.runLater(_rebuildAllBlocksRun = REBUILD_ALL_BLOCKS_RUN);
    }

    /**
     * Sets the selected node view from JavaTextArea selection.
     */
    private void setSelNodeViewFromJavaTextArea()
    {
        // Get node view at text sel start
        int charIndex = _javaTextArea.getSelStart();
        JNodeView<?> nodeView = getNodeViewForCharIndex(charIndex);

        // Select node view
        setSelNodeView(nodeView);
    }

    /**
     * Returns the node view at given index.
     */
    public JNodeView<?> getNodeViewForCharIndex(int charIndex)
    {
        JFileView fileView = getFileView();
        return JNodeViewUtils.getNodeViewForNodeAndCharIndex(fileView, charIndex);
    }

    /**
     * Removes given node view.
     */
    public void removeNodeView(JNodeView<?> nodeView)
    {
        // If not removable, just return
        if (nodeView == null || nodeView instanceof JFileView || nodeView instanceof JClassDeclView || nodeView.getNodeViewParent() instanceof JClassDeclView)
            return;

        // Start explode
        int gridW = (int) nodeView.getWidth() / 8;
        int gridH = Math.max((int) nodeView.getHeight() / 8, 8);
        Explode explode = new Explode(nodeView, gridW, gridH, () -> removeNodeViewAnimDone(nodeView));
        explode.setRunTime(300);
        explode.setHostView(this);
        explode.play();

        // Hide nodeView
        nodeView.setVisible(false);
    }

    /**
     * Called when anim is done.
     */
    private void removeNodeViewAnimDone(JNodeView<?> nodeView)
    {
        // Remove the node
        if (nodeView.isManaged()) {
            JNode node = nodeView.getJNode();
            _nodeHpr.removeNode(node);
        }

        // Or remove shelved node
        else {
            JFileView fileView = getFileView();
            fileView.removeChild(nodeView);
        }
    }

    /**
     * Moves a node to shelf.
     */
    public void moveNodeToShelf(JNodeView<?> nodeView)
    {
        // Remove the node
        JNode node = nodeView.getJNode();
        _nodeHpr.removeNode(node);

        // Add to shelf
        JFileView fileView = getFileView();
        fileView.addNodeViewToShelf(nodeView);
    }

    /**
     * Process events.
     */
    protected void processEvent(ViewEvent anEvent)
    {
        // Handle MousePressed
        switch (anEvent.getType()) {
            case KeyPress: keyPress(anEvent); break;
            case MousePress: mousePress(anEvent); break;
            case MouseDrag: mouseDragged(anEvent); break;
            case MouseRelease: mouseReleased(); break;
        }
    }

    /**
     * Handle key press.
     */
    private void keyPress(ViewEvent anEvent)
    {
        int keyCode = anEvent.getKeyCode();

        switch (keyCode) {

            // Handle Backspace, delete
            case KeyCode.BACK_SPACE:
            case KeyCode.DELETE: delete(); anEvent.consume(); break;
            case KeyCode.ESCAPE: escape(); anEvent.consume(); break;
        }
    }

    /**
     * Handle mouse press.
     */
    private void mousePress(ViewEvent anEvent)
    {
        // Get mouse point
        _mouseX = anEvent.getX();
        _mouseY = anEvent.getY();

        // Get NodeView at mouse point
        View deepestView = ViewUtils.getDeepestChildAt(this, _mouseX, _mouseY);
        _dragNodeView = JBlockView.getBlockView(deepestView);
        if (_dragNodeView == _fileView)
            _dragNodeView = null;

        // Select drag node
        setSelNodeView(_dragNodeView);
    }

    /**
     * Handle mouse dragged.
     */
    private void mouseDragged(ViewEvent anEvent)
    {
        // If no drag node view, just return
        if (_dragNodeView == null) return;

        // Move dragNodeView by mouse offset
        double offsetX = anEvent.getX() - _mouseX;
        double offsetY = anEvent.getY() - _mouseY;
        _dragNodeView.moveBy(offsetX, offsetY);
        _mouseX = anEvent.getX();
        _mouseY = anEvent.getY();
    }

    /**
     * Handle mouse released.
     */
    private void mouseReleased()
    {
        // If no drag node view, just return
        if (_dragNodeView == null) return;

        // If drag node is outside bounds, remove node
        if (_dragNodeView.isOutsideParent())
            moveNodeToShelf(_dragNodeView);

        // Reset translation
        _dragNodeView.setTransX(0);
        _dragNodeView.setTransY(0);
        _dragNodeView = null;
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
        JNodeView<?> selNodeView = getSelNodeView();
        if (!(selNodeView instanceof JStmtView)) {
            JStmtView<?> stmtView = selNodeView.getParent(JStmtView.class);
            if (stmtView == null)
                return;
            setSelNodeView(stmtView);
        }

        // Do copy
        _javaTextArea.copy();
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
     * Deletes current selection.
     */
    public void delete()
    {
        JNodeView<?> selNodeView = getSelNodeView();
        removeNodeView(selNodeView);
    }

    /**
     * Undo last change.
     */
    public void undo()  { _javaTextArea.undo(); }

    /**
     * Redo last undo.
     */
    public void redo()  { _javaTextArea.redo(); }

    /**
     * Escape.
     */
    public void escape()
    {
        JNodeView<?> selNodeView = getSelNodeView();
        JNodeView<?> parentNodeView = selNodeView.getNodeViewParent();
        if (parentNodeView != null)
            setSelNodeView(parentNodeView);
    }
}