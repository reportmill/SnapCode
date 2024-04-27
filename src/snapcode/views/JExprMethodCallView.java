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
    protected View[] createRowViews()
    {
        // Create for name
        JExprMethodCall mc = getJNode();
        Label label = createLabel(mc.getName());
        View[] rowViews;

        // Create views for args
        JExprMethodCall methodCall = getJNode();
        JExpr[] args = methodCall.getArgs();
        View[] argViews = ArrayUtils.mapNonNull(args, arg -> new JExprEditor<>(arg), View.class);
        rowViews = ArrayUtils.add(argViews, label, 0);

        // Return
        return rowViews;
    }
}