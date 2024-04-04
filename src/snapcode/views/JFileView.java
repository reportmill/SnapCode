package snapcode.views;
import javakit.parse.JClassDecl;
import javakit.parse.JFile;
import snap.gfx.Paint;
import snap.view.ColView;
import snap.view.RowView;
import snap.view.ViewUtils;
import java.util.stream.Stream;

/**
 * A SnapPart for JFile.
 */
public class JFileView extends JNodeView<JFile> {

    // The SnapCodeArea
    SnapEditor _editor;

    // Background fill
    static final Paint BACK_FILL = ViewUtils.getBackFill();

    /**
     * Constructor.
     */
    public JFileView()
    {
        super();
        setFill(BACK_FILL); //setBorder(Color.LIGHTGRAY, 1);
        setBlockType(BlockType.None);
    }

    /**
     * Returns the SnapCodeArea.
     */
    public SnapEditor getEditor()  { return _editor; }

    /**
     * Sets the JNode.
     */
    @Override
    public void setJNode(JFile aJNode)
    {
        super.setJNode(aJNode);

        // Reset children and their UI
        _jnodeViews = null;
        ColView colView = getColView();
        colView.removeChildren();
        JNodeView<?>[] jnodeViews = getJNodeViews();
        Stream.of(jnodeViews).forEach(this::addChildToColView);
    }

    /**
     * Override to customize.
     */
    @Override
    protected RowView createRowView()
    {
        RowView rowView = super.createRowView();
        rowView.setMinHeight(-1);
        return rowView;
    }

    /**
     * Override to customize.
     */
    @Override
    protected ColView createColView()
    {
        ColView colView = super.createColView();
        colView.setPadding(25, 10, 10, 10);
        colView.setSpacing(25);
        colView.setFillWidth(false);
        return colView;
    }

    /**
     * Override to return JFile child node owners.
     */
    @Override
    protected JNodeView<?>[] createJNodeViews()
    {
        JFile jfile = getJNode();
        JClassDecl classDecl = jfile.getClassDecl();
        return classDecl != null ? new JNodeView[] { new JClassDeclView<>(classDecl) } : new JNodeView[0];
    }

    /**
     * Override to return false.
     */
    public boolean isBlock()  { return false; }

    /**
     * Returns a string describing the part.
     */
    public String getPartString()  { return "Class"; }
}