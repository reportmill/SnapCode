package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JExprMethodCall;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.view.Label;
import snap.view.View;
import java.util.List;

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
        setColor(PieceColor);
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
        View[] rowViews = new View[] { label };

        // Create views for args
        JExprMethodCall methodCall = getJNode();
        List<JExpr> args = methodCall.getArgs();
        if (args != null) {
            View[] argViews = ListUtils.mapNonNullToArray(args, arg -> new JExprEditor<>(arg), View.class);
            rowViews = ArrayUtils.add(argViews, label, 0);
        }

        // Return
        return rowViews;
    }
}