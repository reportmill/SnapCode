package snapcode.views;
import javakit.parse.JClassDecl;
import javakit.parse.JExprId;
import javakit.parse.JMemberDecl;
import javakit.parse.JType;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.util.ArrayUtils;
import snap.view.ColView;
import snap.view.Label;
import snap.view.RowView;
import snap.view.View;
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
        setBlockType(BlockType.None);
    }

    /**
     * Override to return labels.
     */
    @Override
    protected View[] createRowViews()
    {
        // Create/add node for id
        JClassDecl cd = getJNode();
        JExprId id = cd.getId();
        JNodeView<?> idView = new ClassDeclIdView<>(id);
        List<View> rowViews = new ArrayList<>();
        rowViews.add(idView);

        // Add JNodeView for extnds type
        List<JType> exts = cd.getExtendsTypes();
        JType ext = exts.size() > 0 ? exts.get(0) : null;
        if (ext != null) {

            // Add separator label
            Label label = new Label(" extends ");
            label.setFont(Font.Arial14.copyForSize(16));
            label.setTextFill(Color.WHITE);
            rowViews.add(label);

            // Add TypeView
            JNodeView<?> typView = new ClassDeclTypeView<>(ext);
            rowViews.add(typView);
        }

        // Return array
        return rowViews.toArray(new View[0]);
    }

    /**
     * Override to return member views.
     */
    @Override
    protected JNodeView<?>[] createColViews()
    {
        JClassDecl classDecl = getJNode();
        JMemberDecl[] memberDecls = classDecl.getMemberDecls();
        return ArrayUtils.mapNonNull(memberDecls, mdecl -> JMemberDeclView.createView(mdecl), JNodeView.class);
    }

    /**
     * Override to customize row view.
     */
    @Override
    protected RowView createRowView()
    {
        RowView rowView = super.createRowView();
        rowView.setSpacing(12);
        return rowView;
    }

    /**
     * Override to customize.
     */
    @Override
    protected ColView createColView()
    {
        ColView colView = super.createColView();
        colView.setPadding(25, 10, 10, 0);
        colView.setSpacing(25);
        colView.setFillWidth(false);
        return colView;
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
            setMinWidth(240);
            setBlockType(BlockType.Plain);
            setColor(ClassDeclColor);
            _blockView.setBorder(ClassDeclColor.darker(), 2);
        }

        /**
         * Override to return label.
         */
        @Override
        protected View[] createRowViews()
        {
            JExprId id = getJNode();
            Label label = createLabel(id.getName());
            label.setFont(label.getFont().copyForSize(20));
            return new View[] { label };
        }

        /**
         * Returns a string describing the part.
         */
        public String getPartString()  { return "ClassId"; }
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
            setMinWidth(120);
            setBlockType(BlockType.Plain);
            setColor(ClassDeclColor);
        }

        /**
         * Override to return label.
         */
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
}