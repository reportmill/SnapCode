package snapcode.views;
import javakit.parse.*;
import snap.view.Label;

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
    public static JNodeView createView(JNode aNode)
    {
        JNodeView np = null;
        if (aNode instanceof JConstrDecl) np = new ConstructorDecl();
        else if (aNode instanceof JMethodDecl) np = new MethodDecl();
        else return null;
        np.setJNode(aNode);
        return np;
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
        }

        /**
         * Override.
         */
        protected void updateUI()
        {
            // Do normal version and set type to MemberDecl
            super.updateUI();
            setBlockType(BlockType.PlainBox);
            setColor(MemberDeclColor);

            // Add label for method name
            JExecutableDecl md = getJNode();
            Label label = createLabel(md.getName());
            label.setFont(label.getFont().copyForSize(14));
            addChildToRowView(label);
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
                JNodeView lastNodeView = getJNodeViewLast();
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