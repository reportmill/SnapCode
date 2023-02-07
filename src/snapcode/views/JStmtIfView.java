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
     * Updates UI for top line.
     */
    protected void updateUI()
    {
        // Do normal version
        super.updateUI();

        // Configure HBox
        RowView hbox = getHBox();

        // Create label and condition views and add to box
        Label label = createLabel("if");
        JStmtIf istmt = getJNode();
        JExpr cond = istmt.getConditional();
        JExprView eview = new JExprEditor();
        eview.setJNode(cond);
        hbox.setChildren(label, eview);
    }
}