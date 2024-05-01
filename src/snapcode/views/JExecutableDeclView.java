package snapcode.views;
import javakit.parse.JExecutableDecl;
import javakit.parse.JMethodDecl;
import javakit.parse.JNode;
import snap.view.Label;
import snap.view.View;

/**
 * Subclass for JExecutableDecl.
 */
public class JExecutableDeclView<JNODE extends JExecutableDecl> extends JBlockView<JNODE> {

    /**
     * Constructor.
     */
    public JExecutableDeclView()
    {
        super();
        setMinWidth(120);
        setBlockType(BlockType.PlainBox);
        setColor(MemberDeclColor);
    }

    /**
     * Override to return label.
     */
    @Override
    protected View[] createRowViews()
    {
        JExecutableDecl md = getJNode();
        Label label = JNodeViewUtils.createLabel(md.getName());
        label.setFont(label.getFont().copyForSize(14));
        return new View[]{label};
    }

    /**
     * Drops a node.
     */
    protected void dropNode(JNode aNode, double anX, double aY)
    {
        // If no children, just insert node inside this node
        if (getChildBlockViewCount() == 0) {
            getEditor().insertNode(getJNode(), aNode, 0);
        }

        // If in header bar, forward to first child
        else if (aY < getHeight() / 2) {
            JNodeView<?> firstChildNodeView = getChildBlockView(0);
            firstChildNodeView.dropNode(aNode, anX, 0);
        }

        // If in footer bar, forward to last child
        else {
            JNodeView<?> lastNodeView = getLastChildBlockView();
            lastNodeView.dropNode(aNode, anX, lastNodeView.getHeight());
        }
    }

    /**
     * Returns a string describing the part.
     */
    @Override
    public String getPartString()
    {
        return _jnode instanceof JMethodDecl ? "Method" : "Constructor";
    }
}
