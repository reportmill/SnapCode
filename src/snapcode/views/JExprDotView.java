package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JExprDot;
import snap.view.RowView;
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

        // Get/configure HBox
        RowView rowView = getRowView();
        rowView.setPadding(0, 0, 0, 0);

        // Create/add views for child expressions
        for (JNodeView child : getJNodeViews())
            rowView.addChild(child);
        getJNodeView(0).setSeg(Seg.First);
        getJNodeViewLast().setSeg(Seg.Last);
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