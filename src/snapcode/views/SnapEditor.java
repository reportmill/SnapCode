package snapcode.views;
import javakit.parse.*;
import javakit.resolver.JavaClass;
import snapcode.javatext.JavaTextArea;
import snap.text.TextLine;
import snap.view.StackView;
import snap.view.View;
import snap.view.ViewEvent;
import snap.view.ViewUtils;
import java.util.List;

/**
 * The Pane that actually holds SnapPart pieces.
 */
public class SnapEditor extends StackView {

    // The JavaTextArea
    private JavaTextArea  _jtextArea;

    // The scripts pane
    private JFileView  _filePart;

    // The selected part
    private JNodeView<?> _selPart;

    // The mouse node and X/Y during mouse drag
    private View  _mnode;
    private JNodeView<?>  _mpart;
    private double  _mx, _my;

    /**
     * Creates a new SnapCodeArea.
     */
    public SnapEditor(JavaTextArea aJTA)
    {
        // Set JavaTextArea
        _jtextArea = aJTA;

        // Create FilePart and add
        _filePart = new JFileView();
        _filePart._editor = this;
        _filePart.setGrowWidth(true);
        _filePart.setGrowHeight(true);
        addChild(_filePart);

        // Configure mouse handling
        enableEvents(MousePress, MouseDrag, MouseRelease);
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
    public JavaTextArea getJavaTextArea()  { return _jtextArea; }

    /**
     * Returns the selected part.
     */
    public JNodeView<?> getSelectedPart()
    {
        return _selPart;
    }

    /**
     * Sets the selected parts.
     */
    public void setSelectedPart(JNodeView<?> aPart)
    {
        if (_selPart != null) _selPart.setSelected(false);
        _selPart = aPart != null ? aPart : _filePart;
        _selPart.setSelected(true);

        // Update JavaTextArea selection
        JNode jnode = _selPart.getJNode();
        int ss = jnode.getStartCharIndex(), se = jnode.getEndCharIndex();
        getJavaTextArea().setSel(ss, se);

        // Forward to editor
        SnapEditorPane ep = getEditorPane();
        if (ep != null) ep.updateSelectedPart(_selPart);
    }

    /**
     * Returns the FilePart.
     */
    public JFileView getFilePart()  { return _filePart; }

    /**
     * Returns the JFile JNode.
     */
    public JFile getJFile()
    {
        return getJavaTextArea().getJFile();
    }

    /**
     * Returns the selected part's class.
     */
    public Class<?> getSelectedPartClass()
    {
        // Get selected NodeView
        JNodeView<?> selNodeView = getSelectedPart();
        if (selNodeView == null)
            selNodeView = getFilePart();

        // Get first parent JNode that resolves to class
        JNode selNode = selNodeView.getJNode();
        while (selNode != null && selNode.getEvalClass() == null)
            selNode = selNode.getParent();

        // Return class
        JavaClass javaClass = selNode != null ? selNode.getEvalClass() : null;
        Class<?> realClass = javaClass != null ? javaClass.getRealClass() : null;
        return realClass;
    }

    /**
     * Returns the selected part's class or the enclosing class, if void.class.
     */
    public Class<?> getSelectedPartEnclClass()
    {
        // Get selected NodeView
        JNodeView<?> selNodeView = getSelectedPart();
        if (selNodeView == null)
            selNodeView = getFilePart();

        // Get first parent JNode that resolves to class
        JNode selNode = selNodeView.getJNode();
        while (selNode != null && (selNode.getEvalClass() == null || selNode.getEvalClass().isPrimitive()))
            selNode = selNode.getParent();

        // Return class
        JavaClass javaClass = selNode != null ? selNode.getEvalClass() : null;
        Class<?> realClass = javaClass != null ? javaClass.getRealClass() : null;
        return realClass;
    }

    /**
     * Rebuilds the pieces.
     */
    protected void rebuildUI()
    {
        JFileView fileView = getFilePart();
        JFile jfile = getJFile();
        fileView.setJNode(jfile);
        setSelectedPartFromTextArea();
    }

    /**
     * Sets the selected part from TextArea selection.
     */
    void setSelectedPartFromTextArea()
    {
        int index = getJavaTextArea().getSelStart();
        JNodeView<?> selPart = getSnapPartAt(getFilePart(), index);
        setSelectedPart(selPart);
    }

    /**
     * Returns the snap part at given index.
     */
    public JNodeView<?> getSnapPartAt(JNodeView aPart, int anIndex)
    {
        // Check children
        List<JNodeView<?>> children = aPart.getJNodeViews();
        for (JNodeView<?> child : children) {
            JNodeView<?> part = getSnapPartAt(child, anIndex);
            if (part != null)
                return part;
        }

        // Check part
        JNode jnode = aPart.getJNode();
        return jnode.getStartCharIndex() <= anIndex && anIndex <= jnode.getEndCharIndex() ? aPart : null;
    }

    /**
     * Replaces a string.
     */
    protected void replaceText(String aString, int aStart, int anEnd)
    {
        JavaTextArea tview = getJavaTextArea();
        tview.replaceChars(aString, null, aStart, anEnd, true);
        rebuildUI();
    }

    /**
     * Sets text selection.
     */
    protected void setTextSelection(int aStart, int anEnd)
    {
        JavaTextArea tview = getJavaTextArea();
        tview.setSel(aStart, anEnd);
    }

    /**
     * Insets a node.
     */
    public void insertNode(JNode aBaseNode, JNode aNewNode, int aPos)
    {
        if (aBaseNode instanceof JFile) {
            System.out.println("Can't add to file");
            return;
        }

        if (aBaseNode instanceof JStmtExpr && aNewNode instanceof JStmtExpr) {
            JavaClass baseNodeJavaClass = aBaseNode.getEvalClass();
            Class<?> baseNodeClass = baseNodeJavaClass != null ? baseNodeJavaClass.getRealClass() : null;
            Class<?> selPartClass = getSelectedPartClass();
            if (baseNodeClass == selPartClass && baseNodeClass != void.class) {
                int index = aBaseNode.getEndCharIndex();
                String nodeStr = aNewNode.getString(), str = '.' + nodeStr;
                replaceText(str, index - 1, index);
                setTextSelection(index, index + nodeStr.length());
                return;
            }
        }

        //
        int index = aPos < 0 ? getBeforeNode(aBaseNode) : aPos > 0 ? getAfterNode(aBaseNode) : getInNode(aBaseNode);
        String indent = getIndent(aBaseNode, aPos);
        String nodeStr = aNewNode.getString().trim().replace("\n", "\n" + indent);
        String str = indent + nodeStr + '\n';
        replaceText(str, index, index);
        setTextSelection(index + indent.length(), index + indent.length() + nodeStr.trim().length());
    }

    /**
     * Replaces a JNode with string.
     */
    public void replaceJNode(JNode aNode, String aString)
    {
        replaceText(aString, aNode.getStartCharIndex(), aNode.getEndCharIndex());
    }

    /**
     * Removes a node.
     */
    public void removeNode(JNode aNode)
    {
        int start = getBeforeNode(aNode);
        int end = getAfterNode(aNode);
        replaceText(null, start, end);
    }

    /**
     * Returns after node.
     */
    public int getBeforeNode(JNode aNode)
    {
        int nodeStartCharIndex = aNode.getStartCharIndex();
        JExpr scopeExpr = aNode instanceof JExpr ? ((JExpr) aNode).getScopeExpr() : null;
        if (scopeExpr != null)
            return scopeExpr.getEndCharIndex();

        JavaTextArea javaTextArea = getJavaTextArea();
        TextLine textLine = javaTextArea.getLineForCharIndex(nodeStartCharIndex);
        return textLine.getStartCharIndex();
    }

    /**
     * Returns after node.
     */
    public int getAfterNode(JNode aNode)
    {
        int index = aNode.getEndCharIndex();
        JNode nodeParent = aNode.getParent();
        JExprDot dotExpr = nodeParent instanceof JExprDot ? (JExprDot) nodeParent : null;
        if (dotExpr != null)
            return dotExpr.getExpr().getEndCharIndex();

        JavaTextArea javaTextArea = getJavaTextArea();
        TextLine tline = javaTextArea.getLineForCharIndex(index);
        return tline.getEndCharIndex();
    }

    /**
     * Returns in the node.
     */
    public int getInNode(JNode aNode)
    {
        JavaTextArea tview = getJavaTextArea();
        int index = aNode.getStartCharIndex();
        while (index < tview.length() && tview.charAt(index) != '{') index++;
        TextLine tline = tview.getLineForCharIndex(index);
        return tline.getEndCharIndex();
    }

    /**
     * Returns the indent.
     */
    String getIndent(JNode aNode, int aPos)
    {
        int index = aNode.getStartCharIndex();
        TextLine textLine = getJavaTextArea().getLineForCharIndex(index);
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
        if (anEvent.isMousePress()) {
            _mx = anEvent.getX();
            _my = anEvent.getY();
            _mnode = ViewUtils.getDeepestChildAt(this, _mx, _my);
            _mpart = JNodeView.getJNodeView(_mnode);
            if (_mpart == null) _mnode = null;
            else _mnode = _mpart;
            if (_mpart == _filePart) {
                setSelectedPart(null);
                _mpart = null;
            }
            setSelectedPart(_mpart);
        }

        // Handle MouseDragged
        else if (anEvent.isMouseDrag()) {
            if (_mpart == null) return;
            double mx = anEvent.getX(), my = anEvent.getY();
            _mnode.setTransX(_mnode.getTransX() + mx - _mx);
            _mx = mx;
            _mnode.setTransY(_mnode.getTransY() + my - _my);
            _my = my;
        }

        // Handle MouseReleased
        else if (anEvent.isMouseRelease()) {
            if (_mpart == null) return;
            if (_mnode.getTransX() > 150 && _mpart.getJNodeViewParent() != null) removeNode(_mpart.getJNode());
            _mnode.setTransX(0);
            _mnode.setTransY(0);
            _mnode = null;
        }
    }
}