package snapcode.views;
import javakit.parse.*;
import snap.view.Label;
import snap.view.View;

/**
 * A JNodeView for JStmt.
 */
public class JStmtView<JNODE extends JStmt> extends JBlockView<JNODE> {

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
        Label label = JNodeViewUtils.createLabel(stmt.getString());
        return new View[]{label};
    }

    /**
     * Returns a string describing the part.
     */
    public String getNodeString()
    {
        String nodeClassName = _jnode.getClass().getSimpleName();
        return nodeClassName.substring(5) + " Statement";
    }
}