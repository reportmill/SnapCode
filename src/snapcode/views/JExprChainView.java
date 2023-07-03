package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JExprDot;
import snap.view.RowView;
import java.util.ArrayList;
import java.util.List;

/**
 * SnapPartExpr subclass for JExprChain.
 */
public class JExprChainView<JNODE extends JExprDot> extends JExprView<JNODE> {

    /**
     * Updates UI.
     */
    protected void updateUI()
    {
        // Do normal version
        super.updateUI();

        // Get/configure HBox
        RowView hbox = getHBox();
        hbox.setPadding(0, 0, 0, 0);

        // Create/add views for child expressions
        for (JNodeView child : getJNodeViews())
            hbox.addChild(child);
        getJNodeView(0).setSeg(Seg.First);
        getJNodeViewLast().setSeg(Seg.Last);
    }

    /**
     * Override to create children.
     */
    protected List<JNodeView> createJNodeViews()
    {
        JExprDot dotExpr = getJNode();
        List<JNodeView> children = new ArrayList<>();

        JExpr prefixExpr = dotExpr.getPrefixExpr();
        JExprView<?> prefixView = createView(prefixExpr);
        prefixView.setGrowWidth(true);
        children.add(prefixView);

        // Iterate over expression chain children, create expression views and add to list
        JExpr expr = dotExpr.getExpr();
        while (expr != null) {
            JExprView<?> exprView = createView(expr);
            exprView.setGrowWidth(true);
            children.add(exprView);
            if (expr instanceof JExprDot)
                expr = ((JExprDot) expr).getExpr();
        }

        // Return expression views
        return children;
    }
}