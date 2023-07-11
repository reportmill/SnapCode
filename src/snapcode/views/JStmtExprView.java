package snapcode.views;

import javakit.parse.JExpr;
import javakit.parse.JStmtExpr;
import snap.view.RowView;

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

        // Configure HBox
        RowView rowView = getRowView();
        rowView.setPadding(0, 0, 0, 0);

        // Create/Add expr view
        JStmtExpr stmt = getJNode();
        JExpr expr = stmt.getExpr();
        JExprView exprView = JExprView.createView(expr);
        exprView.setGrowWidth(true);
        rowView.addChild(exprView);
    }
}