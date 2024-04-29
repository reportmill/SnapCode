package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JStmtIf;
import snap.view.Label;
import snap.view.View;

/**
 * SnapPartStmt subclass for JStmtIf.
 */
public class JStmtIfView<JNODE extends JStmtIf> extends JStmtView<JNODE> {

    /**
     * Constructor.
     */
    public JStmtIfView()
    {
        super();
    }

    /**
     * Override to return views for if statement.
     */
    @Override
    protected View[] createRowViews()
    {
        // Add label for 'if'
        Label label = createLabel("if");

        // Add condition view
        JStmtIf ifStmt = getJNode();
        JExpr condExpr = ifStmt.getConditional();
        JNodeView<?> exprView = JNodeView.createNodeViewForNode(condExpr);

        // Return
        return new View[] { label, exprView };
    }
}