package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JStmtExpr;
import snap.view.View;

/**
 * JStmtView subclass for JStmtExpression.
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
        JStmtExpr stmt = getJNode();
        JExpr expr = stmt.getExpr();
        JExprView exprView = JExprView.createView(expr);
        exprView.setGrowWidth(true);
        return new View[] { exprView };
    }
}