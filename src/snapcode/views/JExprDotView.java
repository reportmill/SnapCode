package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JExprDot;
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
    protected View[] createRowViews()
    {
        JExprDot dotExpr = getJNode();
        List<JNodeView<?>> childViews = new ArrayList<>();

        JExpr prefixExpr = dotExpr.getPrefixExpr();
        JNodeView<?> prefixView = createNodeViewForNode(prefixExpr);
        prefixView.setGrowWidth(true);
        childViews.add(prefixView);

        // Iterate over expression chain children, create expression views and add to list
        JExpr expr = dotExpr.getExpr();
        if (expr != null) {
            JNodeView<?> exprView = createNodeViewForNode(expr);
            exprView.setGrowWidth(true);
            childViews.add(exprView);
        }

        // Return
        return childViews.toArray(new JNodeView[0]);
    }
}