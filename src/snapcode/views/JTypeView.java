package snapcode.views;

import javakit.parse.JType;
import snap.view.Label;
import snap.view.RowView;

/**
 * A JNodeView subclass for JClassDecl.
 */
public class JTypeView<JNODE extends JType> extends JNodeView<JNODE> {

    /**
     * Creates a new JTypeView.
     */
    public JTypeView()
    {
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
    protected void updateUI()
    {
        // Do normal version and set type, color
        super.updateUI();
        setType(Type.Piece);
        setSeg(Seg.Middle);
        setColor(PieceColor);

        // Configure HBox
        RowView hbox = getHBox();
        hbox.setPadding(0, 0, 0, 8);

        // Create label for type and add
        JType typ = getJNode();
        Label label = createLabel(typ.getName());
        label.setFont(label.getFont().deriveFont(14));
        hbox.addChild(label);
    }

    /**
     * Returns a string describing the part.
     */
    public String getPartString()
    {
        return "Type";
    }
}