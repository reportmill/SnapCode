package snapcode.views;
import javakit.parse.*;
import snap.view.Label;
import snap.view.TextField;
import snap.view.View;
import snap.view.ViewEvent;

import java.util.ArrayList;
import java.util.List;

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
     * Override to return views for statement.
     */
    @Override
    protected View[] createRowViews()
    {
        // Create label and add to HBox
        JStmtFor forStmt = getJNode();
        Label label = JNodeViewUtils.createLabel("for  ");
        List<View> rowViews = new ArrayList<>();
        rowViews.add(label);

        // Get for statement parts
        JExpr varDeclExpr = forStmt.getVarDeclExpr();
        JExpr condExpr = forStmt.isForEach() ? forStmt.getIterableExpr() : forStmt.getConditional();
        JExpr[] updateExprs = forStmt.getUpdateExprs();
        JExpr updateExpr = updateExprs.length > 0 ? updateExprs[0] : null;

        // Add init declaration text
        if (varDeclExpr != null) {
            String str = varDeclExpr.getString();
            TextField tfield = JNodeViewUtils.createTextField(str);
            tfield.setName("ExprText");
            tfield.setProp("Expr", varDeclExpr);
            tfield.addEventHandler(e -> handleTextEvent(e));
            rowViews.add(tfield);
        }

        // Add conditional / iterable text
        if (condExpr != null) {
            String str = condExpr.getString();
            TextField tfield = JNodeViewUtils.createTextField(str);
            tfield.setName("ExprText");
            tfield.setProp("Expr", condExpr);
            tfield.addEventHandler(e -> handleTextEvent(e));
            rowViews.add(tfield);
        }

        // Add update statement text
        if (updateExpr != null) {
            String str = updateExpr.getString();
            TextField tfield = JNodeViewUtils.createTextField(str);
            tfield.setName("ExprText");
            tfield.setProp("Expr", updateExpr);
            tfield.addEventHandler(e -> handleTextEvent(e));
            rowViews.add(tfield);
        }

        // Return
        return rowViews.toArray(new View[0]);
    }

    /**
     * Responds to UI.
     */
    protected void handleTextEvent(ViewEvent anEvent)
    {
        TextField tfield = anEvent.getView(TextField.class);
        JNode jnode = (JNode) tfield.getProp("Expr");
        getEditor().replaceNodeWithString(jnode, anEvent.getStringValue());
    }
}