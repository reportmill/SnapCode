package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JStmtWhile;
import snap.view.Label;
import snap.view.View;

/**
 * SnapPartStmt subclass for JStmtWhile.
 */
public class JStmtWhileView<JNODE extends JStmtWhile> extends JStmtView<JNODE> {

    /**
     * Constructor.
     */
    public JStmtWhileView()
    {
        super();
    }

    /**
     * Override to return views for while statement.
     */
    @Override
    protected View[] createRowViews()
    {
        // Add label for 'if'
        Label label = JNodeViewUtils.createLabel("while");

        // Add condition view
        JStmtWhile whileStmt = getJNode();
        JExpr condExpr = whileStmt.getConditional();
        JNodeView<?> exprView = JNodeViewUtils.createNodeViewForNode(condExpr);

        // Return
        return new View[] { label, exprView };
    }
}