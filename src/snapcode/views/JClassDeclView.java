package snapcode.views;
import javakit.parse.*;
import snap.geom.Insets;
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
public class JClassDeclView<JNODE extends JClassDecl> extends JBlockView<JNODE> {

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
        JClassDecl classDecl = getJNode();
        JExprId classDeclId = classDecl.getId();
        JNodeView<?> idView = new ClassDeclIdView<>(classDeclId);
        List<View> rowViews = new ArrayList<>();
        rowViews.add(idView);

        // Return array
        return rowViews.toArray(new View[0]);
    }

    /**
     * Override to return member views.
     */
    @Override
    protected JBlockView<?>[] createChildBlockViews()
    {
        JClassDecl classDecl = getJNode();
        JMemberDecl[] memberDecls = classDecl.getMemberDecls();
        return ArrayUtils.mapNonNull(memberDecls, mdecl -> JBlockView.createBlockViewForNode(mdecl), JBlockView.class);
    }

    /**
     * Override to customize row view.
     */
    @Override
    protected RowView createRowView()
    {
        RowView rowView = super.createRowView();
        rowView.setPadding(Insets.EMPTY);
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
     * Override to forward to file.
     */
    @Override
    protected void dropNode(JNode aNode, double aX, double aY)
    {
        JFileView fileView = getParent(JFileView.class);
        fileView.dropNode(aNode, aX + getX(), aY + getY());
    }

    /**
     * Returns a string describing the part.
     */
    public String getNodeString()  { return "Class Declaration"; }

    /**
     * A JNodeView subclass for JClassDecl id.
     */
    public static class ClassDeclIdView<JNODE extends JExprId> extends JBlockView<JNODE> {

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
            Label label = JNodeViewUtils.createLabel(id.getName());
            label.setMargin(2, 4, 8, 4);
            label.setFont(label.getFont().copyForSize(20));
            return new View[] { label };
        }

        /**
         * Returns a string describing the part.
         */
        public String getNodeString()  { return "ClassId"; }
    }
}