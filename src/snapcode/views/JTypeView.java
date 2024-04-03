package snapcode.views;
import javakit.parse.JType;
import snap.view.Label;

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
     * Creates a new JTypeView for given JType.
     */
    public JTypeView(JNODE aCD)
    {
        super(aCD);
    }

    /**
     * Override.
     */
    @Override
    protected void updateUI()
    {
        // Do normal version and set type, color
        super.updateUI();
        setBlockType(BlockType.Middle);
        setColor(PieceColor);

        // Create label for type and add
        JType typ = getJNode();
        Label label = createLabel(typ.getName());
        label.setFont(label.getFont().copyForSize(14));
        addChildToRowView(label);
    }

    /**
     * Returns a string describing the part.
     */
    public String getPartString()  { return "Type"; }
}