package snapcode.views;

import javakit.parse.*;
import snap.view.Label;
import snap.view.RowView;
import snap.view.TextField;
import snap.view.ViewEvent;

/**
 * SnapPartStmt subclass for JStmtFor.
 */
public class JStmtForView<JNODE extends JStmtFor> extends JStmtView<JNODE> {

    /**
     * Updates UI for top line.
     */
    protected void updateUI()
    {
        // Do normal version
        super.updateUI();

        // Configure HBox
        RowView hbox = getHBox();

        // Create label and add to HBox
        JStmtFor fs = getJNode();
        Label label = createLabel("for");
        hbox.addChild(label);

        // Add init declaration text
        if (fs.getInitDecl() != null) {
            JStmtVarDecl ivd = fs.getInitDecl();
            String str = ivd.getString();
            TextField tfield = createTextField(str);
            tfield.setName("ExprText");
            tfield.setProp("Expr", ivd);
            tfield.addEventHandler(e -> handleTextEvent(e));
            hbox.addChild(tfield);
        }

        // Add conditional text
        if (fs.getConditional() != null) {
            JExpr cond = fs.getConditional();
            String str = cond.getString();
            TextField tfield = createTextField(str);
            tfield.setName("ExprText");
            tfield.setProp("Expr", cond);
            tfield.addEventHandler(e -> handleTextEvent(e));
            hbox.addChild(tfield);
        }

        // Add update statement text
        if (fs.getUpdateStmts() != null && fs.getUpdateStmts().size() > 0) {
            JStmtExpr se = fs.getUpdateStmts().get(0);
            String str = se.getString();
            TextField tfield = createTextField(str);
            tfield.setName("ExprText");
            tfield.setProp("Expr", se);
            tfield.addEventHandler(e -> handleTextEvent(e));
            hbox.addChild(tfield);
        }
    }

    /**
     * Responds to UI.
     */
    protected void handleTextEvent(ViewEvent anEvent)
    {
        TextField tfield = anEvent.getView(TextField.class);
        JNode jnode = (JNode) tfield.getProp("Expr");
        getEditor().replaceJNode(jnode, anEvent.getStringValue());
    }
}