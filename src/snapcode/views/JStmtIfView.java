package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JStmtIf;
import snap.view.Label;
import snap.view.RowView;

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

        // Configure HBox
        RowView rowView = getRowView();

        // Create label and condition views and add to box
        Label label = createLabel("if");
        JStmtIf ifStmt = getJNode();
        JExpr condExpr = ifStmt.getConditional();
        JExprView<JExpr> exprView = new JExprEditor<>();
        exprView.setJNode(condExpr);
        rowView.setChildren(label, exprView);
    }
}