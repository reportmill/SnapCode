package snapcode.views;

import javakit.parse.JExpr;
import javakit.parse.JExprMethodCall;
import snap.view.Label;
import snap.view.RowView;

import java.util.ArrayList;
import java.util.List;

/**
 * SnapPartExpr subclass for JMethodCall.
 */
public class JExprMethodCallView<JNODE extends JExprMethodCall> extends JExprView<JNODE> {

    /**
     * Override.
     */
    public void updateUI()
    {
        // Do normal version
        super.updateUI();
        setColor(PieceColor);

        // Configure HBox
        RowView hbox = getHBox();

        // Add label for method name
        JExprMethodCall mc = getJNode();
        Label label = createLabel(mc.getName());
        hbox.addChild(label);

        // Add child UIs
        for (JNodeView child : getJNodeViews())
            hbox.addChild(child);
    }

    /**
     * Override to create children for method args.
     */
    protected List<JNodeView> createJNodeViews()
    {
        JExprMethodCall mc = getJNode();
        List<JExpr> args = mc.getArgs();
        List children = new ArrayList();
        if (args != null) for (JExpr arg : args) {
            JExprView spe = new JExprEditor();
            spe.setJNode(arg);
            children.add(spe);
        }
        return children;
    }
}