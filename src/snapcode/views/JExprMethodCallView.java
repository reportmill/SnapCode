package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JExprMethodCall;
import snap.util.ArrayUtils;
import snap.view.Label;
import snap.view.View;

/**
 * SnapPartExpr subclass for JMethodCall.
 */
public class JExprMethodCallView<JNODE extends JExprMethodCall> extends JExprView<JNODE> {

    /**
     * Constructor.
     */
    public JExprMethodCallView()
    {
        super();
    }

    /**
     * Override to return views for method name and args.
     */
    @Override
    protected void addChildExprViews()
    {
        // Create for name
        JExprMethodCall methodCallExpr = getJNode();
        String methodName = methodCallExpr.getName();
        Label methodNameLabel = createLabel(methodName);
        addChild(methodNameLabel);

        // Add open paren
        Label openParenLabel = createLabel("(");
        addChild(openParenLabel);

        // Create views for args
        JExprMethodCall methodCall = getJNode();
        JExpr[] args = methodCall.getArgs();
        View[] argViews = ArrayUtils.mapNonNull(args, arg -> JNodeView.createNodeViewForNode(arg), View.class);
        for (View argView : argViews)
            addChild(argView);

        // Add close paren
        Label closeParenLabel = createLabel(")");
        addChild(closeParenLabel);
    }
}