package snapcode.views;
import javakit.parse.*;
import javakit.resolver.JavaClass;
import snap.view.*;
import snap.viewx.Explode;
import snapcode.javatext.JavaTextArea;
import snap.text.TextLine;

/**
 * The Pane that actually holds SnapPart pieces.
 */
public class SnapEditor extends StackView {

    // The JavaTextArea
    private JavaTextArea _javaTextArea;

    // The file view
    private JFileView _fileView;

    // The selected part
    private JNodeView<?> _selNodeView;

    // The mouse X/Y during mouse drag
    private double _mouseX, _mouseY;

    // The currently dragging node view
    private JBlockView<?> _dragNodeView;

    /**
     * Constructor.
     */
    public SnapEditor(JavaTextArea javaTextArea)
    {
        // Set JavaTextArea
        _javaTextArea = javaTextArea;

        // Create FilePart and add
        _fileView = new JFileView();
        _fileView._editor = this;
        _fileView.setGrowWidth(true);
        _fileView.setGrowHeight(true);
        addChild(_fileView);

        // Configure mouse handling
        setFocusable(true);
        setFocusWhenPressed(true);
        enableEvents(KeyPress, MousePress, MouseDrag, MouseRelease);
        rebuildUI();
    }

    /**
     * Returns the SnapEditorPane.
     */
    public SnapEditorPane getEditorPane()
    {
        return getOwner(SnapEditorPane.class);
    }

    /**
     * Returns the JavaTextArea.
     */
    public JavaTextArea getJavaTextArea()  { return _javaTextArea; }

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

        // Forward to editor
        SnapEditorPane editorPane = getEditorPane();
        if (editorPane != null)
            editorPane.updateSelectedPart(_selNodeView);
    }

    /**
     * Returns the FileView.
     */
    public JFileView getFileView()  { return _fileView; }

    /**
     * Returns the JFile JNode.
     */
    public JFile getJFile()  { return _javaTextArea.getJFile(); }

    /**
     * Returns the selected part's class.
     */
    public JavaClass getSelNodeEvalClass()
    {
        // Get selected NodeView
        JNodeView<?> selNodeView = getSelNodeView();
        if (selNodeView == null)
            selNodeView = getFileView();

        // Get first parent JNode that resolves to class
        JNode selNode = selNodeView.getJNode();
        while (selNode != null && selNode.getEvalClass() == null)
            selNode = selNode.getParent();

        // Return eval class
        return selNode != null ? selNode.getEvalClass() : null;
    }

    /**
     * Rebuilds the pieces.
     */
    protected void rebuildUI()
    {
        JFileView fileView = getFileView();
        JFile jfile = getJFile();
        fileView.setJNode(jfile);
        setSelectedPartFromTextArea();
    }

    /**
     * Sets the selected part from TextArea selection.
     */
    private void setSelectedPartFromTextArea()
    {
        // Get node view at text sel start
        int charIndex = getJavaTextArea().getSelStart();
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
        return getNodeViewForNodeAndCharIndex(fileView, charIndex);
    }

    /**
     * Returns the node view at given index.
     */
    private JNodeView<?> getNodeViewForNodeAndCharIndex(JBlockView<?> parentNodeView, int charIndex)
    {
        // Check children
        JNodeView<?>[] children = parentNodeView.getJNodeViews();
        for (JNodeView<?> child : children) {
            if (child instanceof JBlockView) {
                JNodeView<?> nodeView = getNodeViewForNodeAndCharIndex((JBlockView<?>) child, charIndex);
                if (nodeView != null)
                    return nodeView;
            }
        }

        // Check part
        JNode jnode = parentNodeView.getJNode();
        if (jnode.getStartCharIndex() <= charIndex && charIndex <= jnode.getEndCharIndex())
            return parentNodeView;

        // Return not found
        return null;
    }

    /**
     * Replaces a string.
     */
    protected void replaceChars(CharSequence theChars, int aStart, int anEnd)
    {
        _javaTextArea.replaceChars(theChars, null, aStart, anEnd, true);
        rebuildUI();
    }

    /**
     * Sets text selection.
     */
    protected void setTextSel(int aStart, int anEnd)  { _javaTextArea.setSel(aStart, anEnd); }

    /**
     * Insets a node.
     */
    public void insertNode(JNode baseNode, JNode insertNode, int aPos)
    {
        // If base node is file, complain and return
        if (baseNode instanceof JFile) {
            System.out.println("Can't add to file");
            return;
        }

        // If base node is statement expr
        if (baseNode instanceof JStmtExpr && insertNode instanceof JStmtExpr) {

            // Get baseNode class and selNode class
            JavaClass baseNodeClass = baseNode.getEvalClass();
            JavaClass selNodeClass = getSelNodeEvalClass();

            //
            if (baseNodeClass == selNodeClass && !baseNodeClass.getName().equals("void")) {

                // Get insert string and index
                String nodeStr = insertNode.getString();
                String insertStr = '.' + nodeStr;
                int insertCharIndex = baseNode.getEndCharIndex();

                // Replace chars and return
                replaceChars(insertStr, insertCharIndex - 1, insertCharIndex);
                setTextSel(insertCharIndex, insertCharIndex + nodeStr.length());
                return;
            }
        }

        // Get insert char index for base node
        int insertCharIndex = aPos < 0 ? getCharIndexBeforeNode(baseNode) : aPos > 0 ? getCharIndexAfterNode(baseNode) : getCharIndexInNode(baseNode);

        // Get string for insert node
        String indentStr = getIndentStringForNode(baseNode, aPos);
        String nodeStr = insertNode.getString().trim().replace("\n", "\n" + indentStr);
        String insertStr = indentStr + nodeStr + '\n';

        // Replace chars
        replaceChars(insertStr, insertCharIndex, insertCharIndex);
        setTextSel(insertCharIndex + indentStr.length(), insertCharIndex + indentStr.length() + nodeStr.trim().length());
    }

    /**
     * Replaces a JNode with string.
     */
    public void replaceJNodeWithString(JNode aNode, String aString)
    {
        replaceChars(aString, aNode.getStartCharIndex(), aNode.getEndCharIndex());
    }

    /**
     * Removes a node.
     */
    public void removeNode(JNode aNode)
    {
        int startCharIndex = getCharIndexBeforeNode(aNode);
        int endCharIndex = getCharIndexAfterNode(aNode);
        replaceChars(null, startCharIndex, endCharIndex);
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
        int gridW = (int) nodeView.getWidth() / 10;
        int gridH = Math.max((int) nodeView.getHeight() / 10, 10);
        Explode explode = new Explode(nodeView, gridW, gridH, null);
        explode.setHostView(this);
        explode.play();

        // Remove the node
        if (nodeView.isManaged()) {
            JNode node = nodeView.getJNode();
            removeNode(node);
        }
    }

    /**
     * Moves a node to shelf.
     */
    public void moveNodeToShelf(JNodeView<?> nodeView)
    {
        // Remove the node
        JNode node = nodeView.getJNode();
        removeNode(node);

        // Add to shelf
        JFileView fileView = getFileView();
        fileView.addNodeViewToShelf(nodeView);
    }

    /**
     * Returns char index before given node.
     */
    public int getCharIndexBeforeNode(JNode aNode)
    {
        int nodeStartCharIndex = aNode.getStartCharIndex();
        JExpr scopeExpr = aNode instanceof JExpr ? ((JExpr) aNode).getScopeExpr() : null;
        if (scopeExpr != null)
            return scopeExpr.getEndCharIndex();

        TextLine textLine = _javaTextArea.getLineForCharIndex(nodeStartCharIndex);
        return textLine.getStartCharIndex();
    }

    /**
     * Returns char index after given node.
     */
    public int getCharIndexAfterNode(JNode aNode)
    {
        int nodeEndCharIndex = aNode.getEndCharIndex();
        JNode nodeParent = aNode.getParent();
        JExprDot dotExpr = nodeParent instanceof JExprDot ? (JExprDot) nodeParent : null;
        if (dotExpr != null)
            return dotExpr.getExpr().getEndCharIndex();

        TextLine textLine = _javaTextArea.getLineForCharIndex(nodeEndCharIndex);
        return textLine.getEndCharIndex();
    }

    /**
     * Returns in the node.
     */
    public int getCharIndexInNode(JNode aNode)
    {
        int nodeStartCharIndex = aNode.getStartCharIndex();
        while (nodeStartCharIndex < _javaTextArea.length() && _javaTextArea.charAt(nodeStartCharIndex) != '{')
            nodeStartCharIndex++;

        TextLine textLine = _javaTextArea.getLineForCharIndex(nodeStartCharIndex);
        return textLine.getEndCharIndex();
    }

    /**
     * Returns the indent.
     */
    private String getIndentStringForNode(JNode aNode, int aPos)
    {
        int nodeStartCharIndex = aNode.getStartCharIndex();
        TextLine textLine = _javaTextArea.getLineForCharIndex(nodeStartCharIndex);
        String indentStr = textLine.getIndentString();
        if (aPos == 0)
            indentStr += "    ";
        return indentStr;
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
        JNodeView<?> selNodeView = getSelNodeView();
        int keyCode = anEvent.getKeyCode();

        switch (keyCode) {

            // Handle Backspace, delete
            case KeyCode.BACK_SPACE:
            case KeyCode.DELETE: removeNodeView(selNodeView); anEvent.consume(); break;
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
}