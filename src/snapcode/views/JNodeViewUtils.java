package snapcode.views;
import javakit.parse.JNode;
import snap.geom.Pos;
import snap.gfx.Color;
import snap.view.Label;
import snap.view.TextField;
import java.util.HashMap;
import java.util.Map;

/**
 * Some utility methods.
 */
public class JNodeViewUtils {

    // A cache of node classes to node view classes
    private static Map<Class<? extends JNode>,Class<JNodeView<?>>> _nodeViewClasses = new HashMap<>();

    /**
     * Returns the node view at given index.
     */
    public static JNodeView<?> getNodeViewForNodeAndCharIndex(JBlockView<?> parentNodeView, int charIndex)
    {
        // Check children
        JNodeView<?>[] children = parentNodeView.getChildBlockViews();
        for (JNodeView<?> child : children) {
            if (child instanceof JBlockView) {
                JNodeView<?> nodeView = getNodeViewForNodeAndCharIndex((JBlockView<?>) child, charIndex);
                if (nodeView != null)
                    return nodeView;
            }
        }

        // If char index within node char range, return node view
        JNode jnode = parentNodeView.getJNode();
        if (jnode.getStartCharIndex() <= charIndex && charIndex <= jnode.getEndCharIndex())
            return parentNodeView;

        // Return not found
        return null;
    }

    /**
     * Creates a label for this block.
     */
    public static Label createLabel(String aString)
    {
        Label label = new Label(aString);
        label.setMargin(2, 4, 2, 4);
        label.setFont(JNodeView.LABEL_FONT);
        label.setTextFill(Color.WHITE);
        return label;
    }

    /**
     * Creates a textfield for this block.
     */
    public static TextField createTextField(String aString)
    {
        TextField textField = new TextField();
        textField.setBorderRadius(5);
        textField.setPadding(2, 6, 2, 6);
        textField.setAlign(Pos.CENTER);
        textField.setColCount(0);
        textField.setFont(JNodeView.TEXTFIELD_FONT);
        textField.setText(aString);
        textField.setMinWidth(36);
        textField.setPrefHeight(18);
        return textField;
    }

    /**
     * Returns the nodeView class for node.
     */
    public static Class<? extends JNodeView<?>> getNodeViewClassForNode(JNode aNode)
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
