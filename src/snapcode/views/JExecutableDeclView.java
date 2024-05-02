package snapcode.views;
import javakit.parse.JExecutableDecl;
import javakit.parse.JMethodDecl;
import snap.view.Label;
import snap.view.View;

/**
 * Subclass for JExecutableDecl.
 */
public class JExecutableDeclView<JNODE extends JExecutableDecl> extends JBlockView<JNODE> {

    /**
     * Constructor.
     */
    public JExecutableDeclView()
    {
        super();
        setMinWidth(120);
        setBlockType(BlockType.PlainBox);
        setColor(MemberDeclColor);
    }

    /**
     * Override to return label.
     */
    @Override
    protected View[] createRowViews()
    {
        JExecutableDecl md = getJNode();
        Label label = JNodeViewUtils.createLabel(md.getName());
        label.setFont(label.getFont().copyForSize(14));
        return new View[]{label};
    }

    /**
     * Returns a string describing the part.
     */
    @Override
    public String getNodeString()
    {
        return _jnode instanceof JMethodDecl ? "Method" : "Constructor";
    }
}
