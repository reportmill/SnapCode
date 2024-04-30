package snapcode.views;
import javakit.parse.*;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.view.*;

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
     * Returns whether part is selected.
     */
    public boolean isSelected()  { return _selected; }

    /**
     * Sets whether part is selected.
     */
    public void setSelected(boolean aValue)
    {
        _selected = aValue;
        repaint(-2, -2, getWidth() + 4, getHeight() + 4);
    }

    /**
     * Override.
     */
    @Override
    protected double getPrefWidthImpl(double aH)
    {
        return BoxView.getPrefWidth(this, getChildLast(), aH);
    }

    /**
     * Override.
     */
    @Override
    protected double getPrefHeightImpl(double aW)
    {
        return BoxView.getPrefHeight(this, getChildLast(), -1);
    }

    /**
     * Override to resize rects.
     */
    @Override
    protected void layoutImpl()
    {
        BoxView.layout(this, getChildLast(), true, true);
    }

    /**
     * Returns the SnapEditor.
     */
    public SnapEditor getEditor()
    {
        return getNodeViewParent() != null ? getNodeViewParent().getEditor() : null;
    }

    /**
     * Returns whether NodePane is a block.
     */
    public boolean isBlock()
    {
        JNode jNode = getJNode();
        return jNode instanceof WithBlockStmt && ((WithBlockStmt) jNode).getBlock() != null;
    }

    /**
     * Returns the parent NodeView.
     */
    public JNodeView<?> getNodeViewParent()
    {
        return getParent(JNodeView.class);
    }

    /**
     * Returns a string describing the part.
     */
    public String getPartString()
    {
        return getJNode().getNodeString();
    }

    /**
     * Drops a node at center of part.
     */
    protected void dropNode(JNode aJNode)
    {
        dropNode(aJNode, getWidth() / 2, getHeight() / 2);
    }

    /**
     * Drops a node.
     */
    protected void dropNode(JNode aNode, double anX, double aY)
    {
        System.out.println("Cannot add node to " + getClass().getSimpleName());
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return getJNode().toString();
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
}