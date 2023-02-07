package snapcode.views;
import javakit.parse.JClassDecl;
import javakit.parse.JExprId;
import javakit.parse.JMemberDecl;
import javakit.parse.JType;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.view.ColView;
import snap.view.Label;
import snap.view.RowView;
import java.util.ArrayList;
import java.util.List;

/**
 * A JNodeView subclass for JClassDecl.
 */
public class JClassDeclView<JNODE extends JClassDecl> extends JNodeView<JNODE> {

    /**
     * Creates a new JClassDeclView for given JClassDecl.
     */
    public JClassDeclView(JNODE aCD)
    {
        super(aCD);
    }

    /**
     * Override.
     */
    protected void updateUI()
    {
        // Do normal version and set Type to None
        super.updateUI();
        setType(Type.None);
        BlockLeft = 0;

        // Configure HBox
        RowView hbox = getHBox();
        hbox.setSpacing(12);

        // Create/add node for id
        JClassDecl cd = getJNode();
        JExprId id = cd.getId();
        JNodeView idView = new ClassDeclIdView(id);
        hbox.addChild(idView);

        // Add JNodeView for extnds type
        List<JType> exts = cd.getExtendsTypes();
        JType ext = exts.size() > 0 ? exts.get(0) : null;
        if (ext != null) {

            // Add separator label
            Label label = new Label(" extends ");
            label.setFont(Font.Arial14.deriveFont(16));
            label.setTextFill(Color.WHITE);
            hbox.addChild(label);

            // Add TypeView
            JNodeView typView = new ClassDeclTypeView(ext);
            hbox.addChild(typView);
        }

        // Configure VBox special for file
        ColView vbox = getVBox();
        vbox.setPadding(25, 10, 10, 0);
        vbox.setSpacing(25);
        vbox.setFillWidth(false);
    }

    /**
     * Override to return JFile child node owners.
     */
    protected List<JNodeView> createJNodeViews()
    {
        JClassDecl cdecl = getJNode();
        List<JNodeView> children = new ArrayList();
        for (JMemberDecl md : cdecl.getMemberDecls()) {
            JNodeView mdp = JMemberDeclView.createView(md);
            if (mdp == null) continue;
            children.add(mdp);
        }
        return children;
    }

    /**
     * Override to return false.
     */
    public boolean isBlock()
    {
        return true;
    }

    /**
     * Returns a string describing the part.
     */
    public String getPartString()
    {
        return "Class Declaration";
    }

    /**
     * A JNodeView subclass for JClassDecl id.
     */
    public static class ClassDeclIdView<JNODE extends JExprId> extends JNodeView<JNODE> {

        /**
         * Creates a new JTypeView for given JType.
         */
        public ClassDeclIdView(JNODE aCD)
        {
            super(aCD);
        }

        /**
         * Override.
         */
        protected void updateUI()
        {
            // Do normal version
            super.updateUI();
            setType(Type.Plain);
            setSeg(Seg.Middle);
            setColor(ClassDeclColor);
            _bg.setBorder(ClassDeclColor.darker(), 2);

            // Get configure HBox
            RowView hbox = getHBox();
            hbox.setPadding(2, 2, 2, 8);
            hbox.setMinSize(240, 35);

            // Create/add view for Class id
            JExprId id = getJNode();
            Label label = createLabel(id.getName());
            label.setFont(label.getFont().deriveFont(20));
            hbox.addChild(label);
        }

        /**
         * Returns a string describing the part.
         */
        public String getPartString()
        {
            return "ClassId";
        }
    }

    /**
     * A JNodeView subclass for JClassDecl extends type.
     */
    public static class ClassDeclTypeView<JNODE extends JType> extends JNodeView<JNODE> {

        /**
         * Creates a new JTypeView for given JType.
         */
        public ClassDeclTypeView(JNODE aCD)
        {
            super(aCD);
        }

        /**
         * Override.
         */
        protected void updateUI()
        {
            // Do normal version and basic config
            super.updateUI();
            setType(Type.Plain);
            setSeg(Seg.Middle);
            setColor(ClassDeclColor);

            // Get/configure HBox
            RowView hbox = getHBox();
            hbox.setPadding(2, 2, 2, 8);
            hbox.setMinSize(120, 25);

            // Create/add label for type
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
}