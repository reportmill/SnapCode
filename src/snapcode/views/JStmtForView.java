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

        // Get for statement parts
        JExpr varDeclExpr = forStmt.getVarDeclExpr();
        JExpr condExpr = forStmt.isForEach() ? forStmt.getIterableExpr() : forStmt.getConditional();
        JExpr[] updateExprs = forStmt.getUpdateExprs();
        JExpr updateExpr = updateExprs.length > 0 ? updateExprs[0] : null;

        // Add init declaration text
        if (varDeclExpr != null) {
            String str = varDeclExpr.getString();
            TextField tfield = createTextField(str);
            tfield.setName("ExprText");
            tfield.setProp("Expr", varDeclExpr);
            tfield.addEventHandler(e -> handleTextEvent(e));
            rowView.addChild(tfield);
        }

        // Add conditional / iterable text
        if (condExpr != null) {
            String str = condExpr.getString();
            TextField tfield = createTextField(str);
            tfield.setName("ExprText");
            tfield.setProp("Expr", condExpr);
            tfield.addEventHandler(e -> handleTextEvent(e));
            rowView.addChild(tfield);
        }

        // Add update statement text
        if (updateExpr != null) {
            String str = updateExpr.getString();
            TextField tfield = createTextField(str);
            tfield.setName("ExprText");
            tfield.setProp("Expr", updateExpr);
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