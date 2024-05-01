package snapcode.views;
import javakit.parse.JClassDecl;
import javakit.parse.JFile;
import javakit.parse.JNode;
import snap.geom.Point;
import snap.gfx.Paint;
import snap.gfx.Painter;
import snap.view.ColView;
import snap.view.RowView;
import snap.view.ViewUtils;

/**
 * A SnapPart for JFile.
 */
public class JFileView extends JBlockView<JFile> {

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
        ColView colView = getColView();
        colView.removeChildren();

        // Do normal version
        super.setJNode(aJNode);

        // Reset children and their UI
        _childBlockViews = null;
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
    protected JBlockView<?>[] createChildBlockViews()
    {
        JFile jfile = getJNode();
        JClassDecl classDecl = jfile.getClassDecl();
        return classDecl != null ? new JBlockView[] { new JClassDeclView<>(classDecl) } : new JBlockView[0];
    }

    /**
     * Adds a node view to shelf.
     */
    public void addNodeViewToShelf(JNodeView<?> nodeView)
    {
        // If node already on shelf, remove and return
        if (nodeView.getParent() == this) {
            removeChild(nodeView);
            return;
        }

        // Reset node XY for FileView and add child
        Point nodeViewXYLocal = nodeView.localToParent(0, 0, this);
        nodeView.setTransX(0);
        nodeView.setTransY(0);
        nodeView.setXY(nodeViewXYLocal.x, nodeViewXYLocal.y);
        nodeView.setManaged(false);
        addChild(nodeView);

        // Animate size down
        double newW = nodeView.getBestWidth(-1);
        double newX = nodeView.getX() + Math.round((nodeView.getWidth() - newW) / 2);
        nodeView.getAnim(400).setX(newX).setWidth(newW).play();
    }

    /**
     * Override to paint selected block.
     */
    @Override
    protected void paintAbove(Painter aPntr)
    {
        // Get selected block view - just return if not nested piece
        SnapEditor editor = getEditor();
        JNodeView<?> selNodeView = editor.getSelNodeView();
        JBlockView<?> selBlockView = JBlockView.getBlockView(selNodeView);
        if (selBlockView == null || selBlockView instanceof JFileView || selBlockView instanceof JClassDeclView)
            return;

        // Paint selected block view again so it will always be on top
        ViewUtils.paintViewInView(selBlockView, this, aPntr);
    }

    /**
     * Override to drop statement node.
     */
    @Override
    protected void dropNode(JNode aNode, double aX, double aY)
    {
        JBlockView<?> blockView = JBlockView.createBlockViewForNode(aNode); assert (blockView != null);
        blockView.setSize(blockView.getBestSize());
        blockView.setXY(aX - Math.round(blockView.getWidth() / 2), aY - Math.round(blockView.getHeight() / 2));
        addNodeViewToShelf(blockView);
        getEditor().setSelNodeView(blockView);
    }

    /**
     * Returns a string describing the part.
     */
    public String getPartString()  { return "Class"; }
}