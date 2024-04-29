package snapcode.views;
import javakit.parse.*;
import snap.geom.Pos;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.view.*;
import java.util.HashMap;
import java.util.Map;

/**
 * A view that manages painting and editing of a JNode as puzzle piece.
 */
public class JNodeView<JNODE extends JNode> extends ChildView {

    // The JNode
    protected JNODE _jnode;

    // Whether part is selected
    private boolean _selected;

    // The child node views
    protected JNodeView<?>[] _jnodeViews;

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
        return getJNodeViewParent() != null ? getJNodeViewParent().getEditor() : null;
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
     * Returns the parent.
     */
    public JNodeView<?> getJNodeViewParent()
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
     * Creates a label for this block.
     */
    protected Label createLabel(String aString)
    {
        Label label = new Label(aString);
        label.setMargin(2, 4, 2, 4);
        label.setFont(LABEL_FONT);
        label.setTextFill(Color.WHITE);
        return label;
    }

    /**
     * Creates a textfield for this block.
     */
    protected TextField createTextField(String aString)
    {
        TextField textField = new TextField();
        textField.setPadding(2, 6, 2, 6);
        textField.setAlign(Pos.CENTER);
        textField.setColCount(0);
        textField.setFont(TEXTFIELD_FONT);
        textField.setText(aString);
        textField.setMinWidth(36);
        textField.setPrefHeight(18);
        return textField;
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
     * Returns the SnapPart of a node.
     */
    public static JNodeView<?> getJNodeView(View aView)
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
        Class<? extends JNodeView<?>> nodeViewClass = getNodeViewClassForNode(aNode);
        try {
            JNodeView<JNode> nodeView = (JNodeView<JNode>) nodeViewClass.getConstructor().newInstance();
            nodeView.setJNode(aNode);
            return nodeView;
        }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    // A cache of node classes to node view classes
    private static Map<Class<? extends JNode>,Class<JNodeView<?>>> _nodeViewClasses = new HashMap<>();

    /**
     * Returns the nodeView class for node.
     */
    private static Class<? extends JNodeView<?>> getNodeViewClassForNode(JNode aNode)
    {
        Class<? extends JNode> nodeClass = aNode.getClass();
        return getNodeViewClassForNodeClass(nodeClass);
    }

    /**
     * Returns the nodeView class for node.
     */
    private static Class<JNodeView<?>> getNodeViewClassForNodeClass(Class<? extends JNode> nodeClass)
    {
        // If class found in NodeViewClasses map, just return it
        Class<JNodeView<?>> nodeViewClass = _nodeViewClasses.get(nodeClass);
        if (nodeViewClass != null)
            return nodeViewClass;

        // Look for class - if found, add to cache map
        nodeViewClass = getNodeViewClassForNodeClassImpl(nodeClass);
        if (nodeViewClass != null)
            _nodeViewClasses.put(nodeClass, nodeViewClass);

        // Return
        return nodeViewClass;
    }

    /**
     * Returns the nodeView class for node.
     */
    private static Class<JNodeView<?>> getNodeViewClassForNodeClassImpl(Class<? extends JNode> nodeClass)
    {
        for (Class<?> cls = nodeClass; cls != null; cls = cls.getSuperclass()) {

            // Construct name from "snapcode.views.<node_class_name>View"
            String pkgName = JNodeView.class.getPackage().getName();
            String simpleName = cls.getSimpleName();
            String className = pkgName + '.' + simpleName + "View";
            try { return (Class<JNodeView<?>>) Class.forName(className); }
            catch (Exception ignore) { }
        }

        // Return not found
        return null;
    }
}