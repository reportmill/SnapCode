package snapcode.views;
import javakit.parse.JType;
import snap.view.BoxViewLayout;
import snap.view.Label;
import snap.view.ViewLayout;

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

    /**
     * Override to add child for type.
     */
    @Override
    public void setJNode(JNODE aJNode)
    {
        // Do normal version
        super.setJNode(aJNode);

        // Add label for type
        JType typ = getJNode();
        Label typeLabel = JNodeViewUtils.createLabel(typ.getName());
        typeLabel.setFont(typeLabel.getFont().copyForSize(14));
        addChild(typeLabel);
    }

    /**
     * Returns a string describing the part.
     */
    public String getNodeString()  { return "Type"; }

    /**
     * Override to return box layout.
     */
    @Override
    protected ViewLayout<?> getViewLayoutImpl()
    {
        return new BoxViewLayout<>(this, getLastChild(), true, true);
    }
}