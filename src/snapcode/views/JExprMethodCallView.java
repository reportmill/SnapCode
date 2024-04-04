package snapcode.views;
import javakit.parse.JExpr;
import javakit.parse.JExprMethodCall;
import snap.util.ListUtils;
import snap.view.Label;
import java.util.List;
import java.util.stream.Stream;

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

        // Add label for method name
        JExprMethodCall mc = getJNode();
        Label label = createLabel(mc.getName());
        addChildToRowView(label);

        // Add child UIs
        JNodeView<?>[] nodeViews = getJNodeViews();
        Stream.of(nodeViews).forEach(this::addChildToRowView);
    }

    /**
     * Override to create children for method args.
     */
    @Override
    protected JNodeView<?>[] createJNodeViews()
    {
        JExprMethodCall methodCall = getJNode();
        List<JExpr> args = methodCall.getArgs();
        return args != null ? ListUtils.mapNonNullToArray(args, arg -> new JExprEditor<>(arg), JNodeView.class) : new JNodeView[0];
    }
}