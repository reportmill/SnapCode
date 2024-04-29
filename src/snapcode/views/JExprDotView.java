package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JExprDot;
import snap.view.Label;
import snap.view.View;
import java.util.ArrayList;
import java.util.List;

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

        JExpr prefixExpr = dotExpr.getPrefixExpr();
        JNodeView<?> prefixView = createNodeViewForNode(prefixExpr);
        prefixView.setGrowWidth(true);
        addChild(prefixView);

        // Create dot label
        Label dotLabel = createLabel(".");
        addChild(dotLabel);

        // Iterate over expression chain children, create expression views and add to list
        JExpr expr = dotExpr.getExpr();
        if (expr != null) {
            JNodeView<?> exprView = createNodeViewForNode(expr);
            exprView.setGrowWidth(true);
            addChild(exprView);
        }
    }
}