package snapcode.views;
import javakit.parse.*;
import snap.view.Label;
import snap.view.RowView;

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
    }

    /**
     * Override to configure.
     */
    protected void updateUI()
    {
        // Do normal version and set type, color
        super.updateUI();
        BlockTop = BlockBottom = 2;
        setType(isBlock() ? Type.BlockStmt : Type.Piece);
        setColor(isBlock() ? BlockStmtColor : PieceColor);

        // Configure HBox
        RowView rowView = getRowView();
        rowView.setPadding(0, 2, 0, 8);
        rowView.setMinSize(120, PieceHeight);
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
            System.out.println("SnapPartStmt.dropNode: Can't drop " + aNode);
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

        /**
         * Override to configure.
         */
        protected void updateUI()
        {
            // Do normal version and set type, color
            super.updateUI();

            // Create label for statement and add to HBox
            JStmt stmt = getJNode();
            Label label = createLabel(stmt.getString());
            getRowView().addChild(label);
        }
    }
}