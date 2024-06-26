package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JExprAssign;
import snap.view.Label;

/**
 * JExprView subclass for JExprAssign.
 */
public class JExprAssignView<JNODE extends JExprAssign> extends JExprView<JNODE> {

    /**
     * Constructor.
     */
    public JExprAssignView()
    {
        super();
    }

    /**
     * Override to return views for assign parts.
     */
    @Override
    protected void addChildExprViews()
    {
        JExprAssign assignExpr = getJNode();

        // Add child for left side expr
        JExpr prefixExpr = assignExpr.getLeftSideExpr();
        JNodeView<?> prefixView = JNodeView.createNodeViewForNode(prefixExpr);
        prefixView.setGrowWidth(true);
        addChild(prefixView);

        // Create '=' label
        Label equalsLabel = JNodeViewUtils.createLabel("=");
        addChild(equalsLabel);

        // Add child for assign value expr
        JExpr expr = assignExpr.getValueExpr();
        if (expr != null) {
            JNodeView<?> exprView = JNodeView.createNodeViewForNode(expr);
            exprView.setGrowWidth(true);
            addChild(exprView);
        }
    }
}