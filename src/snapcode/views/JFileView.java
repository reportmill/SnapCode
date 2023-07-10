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
     * Returns the SnapCodeArea.
     */
    public SnapEditor getEditor()
    {
        return _editor;
    }

    /**
     * Sets the JNode.
     */
    public void setJNode(JFile aJNode)
    {
        super.setJNode(aJNode);

        // Reset children and their UI
        _jnodeViews = null;
        getVBox().removeChildren();
        for (JNodeView<?> child : getJNodeViews())
            getVBox().addChild(child);
    }

    /**
     * Updates UI.
     */
    public void updateUI()
    {
        // Do normal version
        super.updateUI();
        setType(Type.None);
        BlockTop = 0;
        setFill(BACK_FILL);
        setBorder(Color.LIGHTGRAY, 1); //Bevel

        // Get/configure HBox
        RowView hbox = getHBox();
        hbox.setMinHeight(-1);

        // Get/configure VBox
        ColView vbox = getVBox();
        vbox.setPadding(25, 10, 10, 10);
        vbox.setSpacing(25);
        vbox.setFillWidth(false);
    }

    /**
     * Override to return JFile child node owners.
     */
    protected List<JNodeView> createJNodeViews()
    {
        JFile jfile = getJNode();
        List<JNodeView> children = new ArrayList<>();
        JClassDecl cdecl = jfile.getClassDecl();
        if (cdecl == null) return children;
        JClassDeclView<?> classDeclView = new JClassDeclView<>(cdecl);
        children.add(classDeclView);
        return children;
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