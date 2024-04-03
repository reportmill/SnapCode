package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JStmtExpr;

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
     * Updates UI for HBox.
     */
    @Override
    protected void updateUI()
    {
        // Do normal version
        super.updateUI();

        // Create/Add expr view
        JStmtExpr stmt = getJNode();
        JExpr expr = stmt.getExpr();
        JExprView exprView = JExprView.createView(expr);
        exprView.setGrowWidth(true);
        addChildToRowView(exprView);
    }
}