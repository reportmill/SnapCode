package snapcode.views;
import javakit.parse.*;
import snap.view.Label;
import snap.view.View;

/**
 * A JNodeView for JStmt.
 */
public class JStmtView<JNODE extends JStmt> extends JNodeBlockView<JNODE> {

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
     * Override to return statement as label.
     */
    @Override
    protected View[] createRowViews()
    {
        JStmt stmt = getJNode();
        Label label = createLabel(stmt.getString());
        return new View[]{label};
    }

    /**
     * Returns a string describing the part.
     */
    public String getPartString()
    {
        String nodeClassName = _jnode.getClass().getSimpleName();
        return nodeClassName.substring(5) + " Statement";
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
            getEditor().insertNode(_jnode, aNode, -1);

        // If greater than Height-6 or simple statement, insert node after statement
        else if (aY > getHeight() - 6 || !isBlock())
            getEditor().insertNode(_jnode, aNode, 1);

        // If block but no children, insert inside statement
        else if (getJNodeViewCount() == 0)
            getEditor().insertNode(_jnode, aNode, 0);

        // If before first child statement, have first child dropNode, otherwise have last child dropNode
        else if (aY < getHeight() / 2)
            getJNodeView(0).dropNode(aNode, anX, 0);
        else getJNodeViewLast().dropNode(aNode, anX, getJNodeViewLast().getHeight());
    }

}