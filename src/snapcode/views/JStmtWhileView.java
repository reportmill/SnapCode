package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JStmtWhile;
import snap.view.Label;
import snap.view.RowView;

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

        // Configure HBox
        RowView rowView = getRowView();

        // Creeate label and expr parts and add to HBox
        Label label = createLabel("while");
        JStmtWhile whileStmt = getJNode();
        JExpr condExpr = whileStmt.getConditional();
        JExprView<JExpr> exprView = new JExprEditor<>();
        exprView.setJNode(condExpr);
        rowView.setChildren(label, exprView);
    }
}