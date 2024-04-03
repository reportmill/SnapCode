package snapcode.views;
import javakit.parse.JClassDecl;
import javakit.parse.JFile;
import snap.gfx.Color;
import snap.gfx.Paint;
import snap.view.ColView;
import snap.view.RowView;
import snap.view.ViewUtils;
import java.util.ArrayList;
import java.util.List;

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
        getColView().removeChildren();
        for (JNodeView<?> child : getJNodeViews())
            getColView().addChild(child);
    }

    /**
     * Updates UI.
     */
    public void updateUI()
    {
        // Do normal version
        super.updateUI();
        setBlockType(BlockType.None);
        setFill(BACK_FILL);
        setBorder(Color.LIGHTGRAY, 1); //Bevel

        // Get/configure HBox
        RowView rowView = getRowView();
        rowView.setMinHeight(-1);

        // Get/configure VBox
        ColView colView = getColView();
        colView.setPadding(25, 10, 10, 10);
        colView.setSpacing(25);
        colView.setFillWidth(false);
    }

    /**
     * Override to return JFile child node owners.
     */
    @Override
    protected List<JNodeView<?>> createJNodeViews()
    {
        JFile jfile = getJNode();
        List<JNodeView<?>> childViews = new ArrayList<>();
        JClassDecl classDecl = jfile.getClassDecl();
        if (classDecl == null)
            return childViews;

        JClassDeclView<?> classDeclView = new JClassDeclView<>(classDecl);
        childViews.add(classDeclView);

        // Return
        return childViews;
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