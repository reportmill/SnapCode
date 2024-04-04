package snapcode.views;
import javakit.parse.*;
import snap.view.Label;
import snap.view.View;

/**
 * A JNodeView for JStmt.
 */
public class JStmtView<JNODE extends JStmt> extends JNodeView<JNODE> {

    /**
     * Constructor.
     */
    public JStmtView()
    {
        super();
        setMinWidth(120);
    }

    /**
     * Override to update block type and color.
     */
    @Override
    public void setJNode(JNODE aJNode)
    {
        boolean isBlock = aJNode instanceof WithBlockStmt;
        setBlockType(isBlock ? BlockType.Box : BlockType.Piece);
        setColor(isBlock ? BlockStmtColor : PieceColor);

        // Do normal version
        super.setJNode(aJNode);
    }

    /**
     * Returns a string describing the part.
     */
    public String getPartString()
    {
        return getJNode().getClass().getSimpleName().substring(5) + " Statement";
    }

    /**
     * Drops a node.
     */
    protected void dropNode(JNode aNode, double anX, double aY)
    {
        // If not statement, bail
        if (!(aNode instanceof JStmt)) {
            System.out.println("JStmtView.dropNode: Can't drop " + aNode);
            return;
        }

        // If less than 11, insert node before statement
        if (aY < 11)
            getEditor().insertNode(getJNode(), aNode, -1);

            // If greater than Height-6 or simple statement, insert node after statement
        else if (aY > getHeight() - 6 || !isBlock())
            getEditor().insertNode(getJNode(), aNode, 1);

            // If block but no children, insert inside statement
        else if (getJNodeViewCount() == 0)
            getEditor().insertNode(getJNode(), aNode, 0);

            // If before first child statement, have first child dropNode, otherwise have last child dropNode
        else if (aY < getHeight() / 2)
            getJNodeView(0).dropNode(aNode, anX, 0);
        else getJNodeViewLast().dropNode(aNode, anX, getJNodeViewLast().getHeight());
    }

    /**
     * Creates a JStmtView for a JStmt.
     */
    public static JNodeView<?> createView(JNode aNode)
    {
        if (aNode instanceof JStmtExpr)
            return new JStmtExprView<>();
        if (aNode instanceof JStmtWhile)
            return new JStmtWhileView<>();
        if (aNode instanceof JStmtIf)
            return new JStmtIfView<>();
        if (aNode instanceof JStmtFor)
            return new JStmtForView<>();
        return new JStmtOtherView();
    }

    /**
     * A subclass of JStmtView for statements not yet implemented.
     */
    protected static class JStmtOtherView extends JStmtView<JStmt> {

        /**
         * Constructor.
         */
        public JStmtOtherView()
        {
            super();
        }

        @Override
        protected View[] createRowViews()
        {
            JStmt stmt = getJNode();
            Label label = createLabel(stmt.getString());
            return new View[] { label };
        }
    }
}