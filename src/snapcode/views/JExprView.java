package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JExprDot;
import javakit.parse.JExprMethodCall;
import javakit.parse.JNode;
import snap.view.RowView;

/**
 * A JNodeView subclass for JExpr.
 */
public abstract class JExprView<JNODE extends JExpr> extends JNodeView<JNODE> {

    /**
     * Constructor.
     */
    public JExprView()
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

        // Configure RowView
        RowView rowView = getRowView();
        rowView.setPadding(0, 2, 2, 8);
        setMinHeight(BlockView.DEFAULT_HEIGHT);
    }

    /**
     * Override to forward to parent.
     */
    @Override
    protected void dropNode(JNode aJNode, double anX, double aY)
    {
        JNodeView<?> parentView = getJNodeViewParent();
        parentView.dropNode(aJNode, anX, aY);
    }

    /**
     * Creates a JNodeView for a JNode.
     */
    public static JExprView<?> createView(JExpr anExpr)
    {
        JExprView<? extends JExpr> exprView;
        if (anExpr instanceof JExprMethodCall)
            exprView = new JExprMethodCallView<>();
        else if (anExpr instanceof JExprDot)
            exprView = new JExprDotView<>();
        else exprView = new JExprEditor<>();

        // Set node
        ((JExprView<JExpr>) exprView).setJNode(anExpr);

        // Return
        return exprView;
    }
}