package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JStmtReturn;
import snap.util.ArrayUtils;
import snap.view.Label;
import snap.view.View;

/**
 * SnapPartStmt subclass for JStmtReturn.
 */
public class JStmtReturnView<JNODE extends JStmtReturn> extends JStmtView<JNODE> {

    /**
     * Constructor.
     */
    public JStmtReturnView()
    {
        super();
    }

    /**
     * Override to return views for if statement.
     */
    @Override
    protected View[] createRowViews()
    {
        // Add label for 'return'
        Label label = JNodeViewUtils.createLabel("return");
        View[] rowViews = new View[] { label };

        // Add return view
        JStmtReturn returnStmt = getJNode();
        JExpr returnExpr = returnStmt.getExpr();
        if (returnExpr != null) {
            JNodeView<?> exprView = JNodeViewUtils.createNodeViewForNode(returnExpr);
            rowViews = ArrayUtils.add(rowViews, exprView);
        }

        // Return
        return rowViews;
    }
}