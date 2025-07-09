package snapcode.views;
import javakit.parse.*;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.view.*;
import snapcode.javatext.JavaTextAreaNodeHpr;

/**
 * A view that manages painting and editing of a JNode as puzzle piece.
 */
public class JNodeView<JNODE extends JNode> extends ChildView {

    // The JNode
    protected JNODE _jnode;

    // Whether part is selected
    private boolean _selected;

    // The SnapPart being dragged
    public static JNodeView<?> _dragNodeView;

    // Constants for colors
    public static Color PieceColor = Color.get("#4C67d6");
    public static Color BlockStmtColor = Color.get("#8f56e3");
    public static Color MemberDeclColor = Color.get("#f0a822");
    public static Color ClassDeclColor = MemberDeclColor; //Color.get("#27B31E");

    // Constants for fonts
    protected static final Font LABEL_FONT = new Font("Arial Bold", 13);
    protected static final Font TEXTFIELD_FONT = new Font("Arial", 12);

    /**
     * Constructor.
     */
    public JNodeView()
    {
        super();
    }

    /**
     * Constructor for given JNode.
     */
    public JNodeView(JNODE aJNode)
    {
        this();
        setJNode(aJNode);
    }

    /**
     * Returns the JNode.
     */
    public JNODE getJNode()  { return _jnode; }

    /**
     * Sets the JNode.
     */
    public void setJNode(JNODE aJNode)
    {
        _jnode = aJNode;
    }

    /**
     * Returns the SnapEditor.
     */
    public SnapEditor getEditor()
    {
        JNodeView<?> parentNodeView = getNodeViewParent();
        return parentNodeView != null ? parentNodeView.getEditor() : null;
    }

    /**
     * Returns the JavaTextArea.NodeHpr.
     */
    public JavaTextAreaNodeHpr getNodeHpr()
    {
        SnapEditor editor = getEditor();
        return editor != null ? editor.getNodeHpr() : null;
    }

    /**
     * Returns the parent NodeView.
     */
    public JNodeView<?> getNodeViewParent()
    {
        return getParent(JNodeView.class);
    }

    /**
     * Returns a string describing the node.
     */
    public String getNodeString()  { return _jnode.getNodeString(); }

    /**
     * Returns whether part is selected.
     */
    public boolean isSelected()  { return _selected; }

    /**
     * Sets whether part is selected.
     */
    public void setSelected(boolean aValue)
    {
        if (aValue == _selected) return;
        _selected = aValue;
        repaint();
    }

    /**
     * Drops a node.
     */
    protected void dropNode(JNode aNode, double anX, double aY)
    {
        System.out.println("Cannot add node to " + getClass().getSimpleName());
    }

    /**
     * Override.
     */
    @Override
    protected double getPrefWidthImpl(double aH)
    {
        return BoxView.getPrefWidth(this, getLastChild(), aH);
    }

    /**
     * Override.
     */
    @Override
    protected double getPrefHeightImpl(double aW)
    {
        return BoxView.getPrefHeight(this, getLastChild(), -1);
    }

    /**
     * Override to resize rects.
     */
    @Override
    protected void layoutImpl()
    {
        BoxView.layout(this, getLastChild(), true, true);
    }

    /**
     * Standard toString implementation.
     */
    @Override
    public String toString()
    {
        return _jnode.toString();
    }

    /**
     * Returns the NodeView for given view.
     */
    public static JNodeView<?> getNodeView(View aView)
    {
        if (aView instanceof JNodeView)
            return (JNodeView<?>) aView;
        return aView.getParent(JNodeView.class);
    }

    /**
     * Returns a new nodeView for given node.
     */
    public static JNodeView<?> createNodeViewForNode(JNode aNode)
    {
        // Get NodeView class for node
        Class<? extends JNodeView<?>> nodeViewClass = JNodeViewUtils.getNodeViewClassForNode(aNode);

        // Create instance, set node and return
        try {
            JNodeView<JNode> nodeView = (JNodeView<JNode>) nodeViewClass.getConstructor().newInstance();
            nodeView.setJNode(aNode);
            return nodeView;
        }

        // If exception, just re-throw
        catch (Exception e) { throw new RuntimeException(e); }
    }
}