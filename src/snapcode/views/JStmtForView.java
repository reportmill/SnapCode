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
     * Constructor.
     */
    public JStmtForView()
    {
        super();
    }

    /**
     * Updates UI for top line.
     */
    protected void updateUI()
    {
        // Do normal version
        super.updateUI();

        // Configure HBox
        RowView rowView = getRowView();

        // Create label and add to HBox
        JStmtFor forStmt = getJNode();
        Label label = createLabel("for");
        rowView.addChild(label);

        // Add init declaration text
        if (forStmt.getInitDecl() != null) {
            JStmtVarDecl ivd = forStmt.getInitDecl();
            String str = ivd.getString();
            TextField tfield = createTextField(str);
            tfield.setName("ExprText");
            tfield.setProp("Expr", ivd);
            tfield.addEventHandler(e -> handleTextEvent(e));
            rowView.addChild(tfield);
        }

        // Add conditional text
        if (forStmt.getConditional() != null) {
            JExpr cond = forStmt.getConditional();
            String str = cond.getString();
            TextField tfield = createTextField(str);
            tfield.setName("ExprText");
            tfield.setProp("Expr", cond);
            tfield.addEventHandler(e -> handleTextEvent(e));
            rowView.addChild(tfield);
        }

        // Add update statement text
        if (forStmt.getUpdateStmts() != null && forStmt.getUpdateStmts().size() > 0) {
            JStmtExpr se = forStmt.getUpdateStmts().get(0);
            String str = se.getString();
            TextField tfield = createTextField(str);
            tfield.setName("ExprText");
            tfield.setProp("Expr", se);
            tfield.addEventHandler(e -> handleTextEvent(e));
            rowView.addChild(tfield);
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