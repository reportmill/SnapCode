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
     * Updates UI.
     */
    protected void updateUI()
    {
        // Do normal version
        super.updateUI();

        // Configure HBox
        RowView hbox = getHBox();

        // Creeate label and expr parts and add to HBox
        Label label = createLabel("while");
        JStmtWhile wstmt = getJNode();
        JExpr cond = wstmt.getConditional();
        JExprView spart = new JExprEditor();
        spart.setJNode(cond);
        hbox.setChildren(label, spart);
    }
}