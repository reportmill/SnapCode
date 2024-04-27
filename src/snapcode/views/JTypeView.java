package snapcode.views;
import javakit.parse.JType;
import snap.view.Label;
import snap.view.View;

/**
 * A JNodeView subclass for JClassDecl.
 */
public class JTypeView<JNODE extends JType> extends JNodeView<JNODE> {

    /**
     * Constructor.
     */
    public JTypeView()
    {
        super();
    }

    @Override
    protected View[] createRowViews()
    {
        JType typ = getJNode();
        Label label = createLabel(typ.getName());
        label.setFont(label.getFont().copyForSize(14));
        return new View[] { label };
    }

    /**
     * Returns a string describing the part.
     */
    public String getPartString()  { return "Type"; }
}