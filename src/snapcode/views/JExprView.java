package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JExprChain;
import javakit.parse.JExprMethodCall;
import javakit.parse.JNode;
import snap.view.RowView;

/**
 * A JNodeView subclass for JExpr.
 */
public abstract class JExprView<JNODE extends JExpr> extends JNodeView<JNODE> {

    /**
     * Updates UI.
     */
    protected void updateUI()
    {
        // Do normal version
        super.updateUI();

        // Configure HBox
        RowView hbox = getHBox();
        hbox.setPadding(0, 2, 2, 8);
        setMinHeight(PieceHeight);
    }

    /**
     * Override to forward to parent.
     */
    protected void dropNode(JNode aJNode, double anX, double aY)
    {
        getJNodeViewParent().dropNode(aJNode, anX, aY);
    }

    /**
     * Creates a JNodeView for a JNode.
     */
    public static JExprView createView(JNode aNode)
    {
        JExprView sp;
        if (aNode instanceof JExprMethodCall) sp = new JExprMethodCallView();
        else if (aNode instanceof JExprChain) sp = new JExprChainView();
        else sp = new JExprEditor();
        sp.setJNode(aNode);
        return sp;
    }
}