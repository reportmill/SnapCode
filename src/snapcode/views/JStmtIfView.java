package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JStmtIf;
import snap.view.Label;

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
     * Updates UI for top line.
     */
    @Override
    protected void updateUI()
    {
        // Do normal version
        super.updateUI();

        // Add label for 'if'
        Label label = createLabel("if");
        addChildToRowView(label);

        // Add condition view
        JStmtIf ifStmt = getJNode();
        JExpr condExpr = ifStmt.getConditional();
        JExprView<JExpr> exprView = new JExprEditor<>();
        exprView.setJNode(condExpr);
        addChildToRowView(exprView);
    }
}