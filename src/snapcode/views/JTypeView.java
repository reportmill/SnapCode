package snapcode.views;

import javakit.parse.JType;
import snap.view.Label;
import snap.view.RowView;

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
        setType(BlockView.Type.Piece);
        setSeg(BlockView.Seg.Middle);
        setColor(PieceColor);

        // Configure HBox
        RowView rowView = getRowView();
        rowView.setPadding(0, 0, 0, 8);

        // Create label for type and add
        JType typ = getJNode();
        Label label = createLabel(typ.getName());
        label.setFont(label.getFont().copyForSize(14));
        rowView.addChild(label);
    }

    /**
     * Returns a string describing the part.
     */
    public String getPartString()  { return "Type"; }
}