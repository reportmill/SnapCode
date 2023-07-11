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
        RowView rowView = getRowView();

        // Add label for method name
        JExprMethodCall mc = getJNode();
        Label label = createLabel(mc.getName());
        rowView.addChild(label);

        // Add child UIs
        for (JNodeView child : getJNodeViews())
            rowView.addChild(child);
    }

    /**
     * Override to create children for method args.
     */
    @Override
    protected List<JNodeView<?>> createJNodeViews()
    {
        JExprMethodCall methodCall = getJNode();
        List<JExpr> args = methodCall.getArgs();
        List<JNodeView<?>> children = new ArrayList<>();

        if (args != null) {
            for (JExpr arg : args) {
                JExprView<? super JExpr> exprView = new JExprEditor<>();
                exprView.setJNode(arg);
                children.add(exprView);
            }
        }

        // Return
        return children;
    }
}