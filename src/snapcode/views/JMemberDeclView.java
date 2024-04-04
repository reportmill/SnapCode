package snapcode.views;
import javakit.parse.*;
import snap.view.Label;
import snap.view.View;

/**
 * A SnapPart subclass for JMethodDecl.
 */
public class JMemberDeclView<JNODE extends JMemberDecl> extends JNodeView<JNODE> {

    /**
     * Constructor.
     */
    public JMemberDeclView()
    {
        super();
    }

    /**
     * Creates a SnapPart for a JNode.
     */
    public static JNodeView<?> createView(JNode aNode)
    {
        JNodeView nodeView;
        if (aNode instanceof JConstrDecl)
            nodeView = new ConstructorDecl();
        else if (aNode instanceof JMethodDecl)
            nodeView = new MethodDecl();
        else return null;
        nodeView.setJNode(aNode);
        return nodeView;
    }

    /**
     * Subclass for JExecutableDecl.
     */
    public static class ExecutableDecl<JNODE extends JExecutableDecl> extends JMemberDeclView<JNODE> {

        /**
         * Constructor.
         */
        public ExecutableDecl()
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
            Label label = createLabel(md.getName());
            label.setFont(label.getFont().copyForSize(14));
            return new View[] { label };
        }

        /**
         * Drops a node.
         */
        protected void dropNode(JNode aNode, double anX, double aY)
        {
            if (getJNodeViewCount() == 0) {
                getEditor().insertNode(getJNode(), aNode, 0);
            }
            else if (aY < getHeight() / 2) {
                getJNodeView(0).dropNode(aNode, anX, 0);
            }
            else {
                JNodeView<?> lastNodeView = getJNodeViewLast();
                lastNodeView.dropNode(aNode, anX, lastNodeView.getHeight());
            }
        }
    }

    /**
     * Subclass for JMethodDecl.
     */
    public static class MethodDecl<JNODE extends JMethodDecl> extends ExecutableDecl<JNODE> {

        /**
         * Returns a string describing the part.
         */
        public String getPartString()
        {
            return "Method";
        }
    }

    /**
     * Subclass for JConstrDecl.
     */
    public static class ConstructorDecl<JNODE extends JConstrDecl> extends ExecutableDecl<JNODE> {

        /**
         * Returns a string describing the part.
         */
        public String getPartString()
        {
            return "Constructor";
        }
    }
}