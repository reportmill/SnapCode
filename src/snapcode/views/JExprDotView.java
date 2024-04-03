package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JExprDot;
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
     * Updates UI.
     */
    protected void updateUI()
    {
        // Do normal version
        super.updateUI();

        // Create/add views for child expressions
        List<JNodeView<?>> nodeViews = getJNodeViews();
        nodeViews.forEach(this::addChildToRowView);

        // Change block types
        getJNodeView(0).setBlockType(BlockType.Left);
        getJNodeViewLast().setBlockType(BlockType.Right);
    }

    /**
     * Override to create children.
     */
    @Override
    protected List<JNodeView<?>> createJNodeViews()
    {
        JExprDot dotExpr = getJNode();
        List<JNodeView<?>> childViews = new ArrayList<>();

        JExpr prefixExpr = dotExpr.getPrefixExpr();
        JExprView<?> prefixView = createView(prefixExpr);
        prefixView.setGrowWidth(true);
        childViews.add(prefixView);

        // Iterate over expression chain children, create expression views and add to list
        JExpr expr = dotExpr.getExpr();
        if (expr != null) {
            JExprView<?> exprView = createView(expr);
            exprView.setGrowWidth(true);
            childViews.add(exprView);
        }

        // Return
        return childViews;
    }
}