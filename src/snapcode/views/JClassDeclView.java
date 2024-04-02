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
    @Override
    protected void updateUI()
    {
        // Do normal version and set Type to None
        super.updateUI();
        setBlockType(BlockType.None);
        BlockLeft = 0;

        // Configure HBox
        RowView rowView = getRowView();
        rowView.setSpacing(12);

        // Create/add node for id
        JClassDecl cd = getJNode();
        JExprId id = cd.getId();
        JNodeView<?> idView = new ClassDeclIdView<>(id);
        rowView.addChild(idView);

        // Add JNodeView for extnds type
        List<JType> exts = cd.getExtendsTypes();
        JType ext = exts.size() > 0 ? exts.get(0) : null;
        if (ext != null) {

            // Add separator label
            Label label = new Label(" extends ");
            label.setFont(Font.Arial14.copyForSize(16));
            label.setTextFill(Color.WHITE);
            rowView.addChild(label);

            // Add TypeView
            JNodeView<?> typView = new ClassDeclTypeView<>(ext);
            rowView.addChild(typView);
        }

        // Configure ColView special for file
        ColView colView = getColView();
        colView.setPadding(25, 10, 10, 0);
        colView.setSpacing(25);
        colView.setFillWidth(false);
    }

    /**
     * Override to return JFile child node owners.
     */
    @Override
    protected List<JNodeView<?>> createJNodeViews()
    {
        JClassDecl classDecl = getJNode();
        List<JNodeView<?>> childViews = new ArrayList<>();

        for (JMemberDecl memberDecl : classDecl.getMemberDecls()) {
            JNodeView<?> memberDeclView = JMemberDeclView.createView(memberDecl);
            if (memberDeclView == null)
                continue;
            childViews.add(memberDeclView);
        }

        // Return
        return childViews;
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
    public String getPartString()  { return "Class Declaration"; }

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
            setBlockType(BlockType.Plain);
            setColor(ClassDeclColor);
            _blockView.setBorder(ClassDeclColor.darker(), 2);

            // Get configure HBox
            RowView rowView = getRowView();
            rowView.setPadding(2, 2, 2, 8);
            rowView.setMinSize(240, 35);

            // Create/add view for Class id
            JExprId id = getJNode();
            Label label = createLabel(id.getName());
            label.setFont(label.getFont().copyForSize(20));
            rowView.addChild(label);
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
            setBlockType(BlockType.Plain);
            setColor(ClassDeclColor);

            // Get/configure HBox
            RowView rowView = getRowView();
            rowView.setPadding(2, 2, 2, 8);
            rowView.setMinSize(120, 25);

            // Create/add label for type
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
}