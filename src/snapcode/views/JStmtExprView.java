package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JStmtExpr;
import snap.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JStmtView subclass for JStmtExpression.
 */
public class JStmtExprView<JNODE extends JStmtExpr> extends JStmtView<JNODE> {

    /**
     * Constructor.
     */
    public JStmtExprView()
    {
        super();
    }

    /**
     * Override to return view for expression.
     */
    @Override
    protected View[] createRowViews()
    {
        JStmtExpr stmt = getJNode();
        JExpr expr = stmt.getExpr();
        JExprView exprView = JExprView.createView(expr);

        //exprView.setGrowWidth(true);
        //return new View[] { exprView };

        return getExpressionViews(exprView);
    }

    /**
     * Returns the expression views.
     */
    private View[] getExpressionViews(JExprView exprView)
    {
        List<View> exprViews = new ArrayList<>();
        View[] views = exprView.createRowViews();

        for (View view : views) {
            if (view instanceof JExprView) {
                View[] views2 = getExpressionViews((JExprView) view);
                Collections.addAll(exprViews, views2);
            }
            else exprViews.add(view);
        }

        return exprViews.toArray(new View[0]);
    }
}