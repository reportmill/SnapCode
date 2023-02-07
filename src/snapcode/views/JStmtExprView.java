package snapcode.views;

import javakit.parse.JExpr;
import javakit.parse.JStmtExpr;
import snap.view.RowView;

/**
 * JStmtView subclass for JStmtExpression.
 */
public class JStmtExprView<JNODE extends JStmtExpr> extends JStmtView<JNODE> {

    /**
     * Updates UI for HBox.
     */
    protected void updateUI()
    {
        // Do normal version
        super.updateUI();

        // Configure HBox
        RowView hbox = getHBox();
        hbox.setPadding(0, 0, 0, 0);

        // Create/Add expr view
        JStmtExpr stmt = getJNode();
        JExpr expr = stmt.getExpr();
        JExprView eview = JExprView.createView(expr);
        eview.setGrowWidth(true);
        hbox.addChild(eview);
    }
}