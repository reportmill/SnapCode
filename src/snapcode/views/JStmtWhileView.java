package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JStmtWhile;
import snap.view.Label;

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
     * Updates UI.
     */
    protected void updateUI()
    {
        // Do normal version
        super.updateUI();

        // Add label for 'if'
        Label label = createLabel("while");
        addChildToRowView(label);

        // Add condition view
        JStmtWhile whileStmt = getJNode();
        JExpr condExpr = whileStmt.getConditional();
        JExprView<JExpr> exprView = new JExprEditor<>();
        exprView.setJNode(condExpr);
        addChildToRowView(exprView);
    }
}