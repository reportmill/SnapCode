package snapcode.views;
import javakit.parse.*;
import snap.util.ArrayUtils;
import snap.view.Label;
import snap.view.View;

/**
 * JStmtView subclass for JStmtExpr.
 */
public class JStmtExprView<JNODE extends JStmtExpr> extends JStmtView<JNODE> {

    /**
     * Constructor.
     */
    public JStmtExprView()
    {
        super();
    }

    /**
     * Override to return view for expression.
     */
    @Override
    protected View[] createRowViews()
    {
        // Get expression views
        JStmtExpr exprStmt = getJNode();
        JExpr expr = exprStmt.getExpr();
        JExprView<?> exprView = (JExprView<?>) JNodeView.createNodeViewForNode(expr);
        View[] exprViews = new View[] { exprView };

        // Add prefix for expression type ('call...', 'set...', 'declare...', etc.)
        String exprPrefix = getExpressionPrefix();
        if (exprPrefix != null) {
            Label label = JNodeViewUtils.createLabel(exprPrefix);
            exprViews = ArrayUtils.add(exprViews, label, 0);
        }

        // Return
        return exprViews;
    }

    /**
     * Returns the expression prefix.
     */
    private String getExpressionPrefix()
    {
        JStmtExpr exprStmt = getJNode();
        JExpr expr = exprStmt.getExpr();
        if (expr instanceof JExprMethodCall) return "call";
        if (expr instanceof JExprAssign) return "set";
        if (expr instanceof JExprVarDecl) return "declare";
        return null;
    }
}