package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JExprDot;
import snap.view.Label;

/**
 * SnapPartExpr subclass for JExprDot.
 */
public class JExprDotView<JNODE extends JExprDot> extends JExprView<JNODE> {

    /**
     * Constructor.
     */
    public JExprDotView()
    {
        super();
    }

    /**
     * Override to return views for prefix and expression.
     */
    @Override
    protected void addChildExprViews()
    {
        JExprDot dotExpr = getJNode();

        JExpr scopeExpr = dotExpr.getScopeExpr();
        JNodeView<?> scopeView = JNodeView.createNodeViewForNode(scopeExpr);
        scopeView.setGrowWidth(true);
        addChild(scopeView);

        // Create dot label
        Label dotLabel = JNodeViewUtils.createLabel(".");
        addChild(dotLabel);

        // Iterate over expression chain children, create expression views and add to list
        JExpr expr = dotExpr.getExpr();
        if (expr != null) {
            JNodeView<?> exprView = JNodeView.createNodeViewForNode(expr);
            exprView.setGrowWidth(true);
            addChild(exprView);
        }
    }
}