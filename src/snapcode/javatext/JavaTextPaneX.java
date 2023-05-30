package snapcode.javatext;
import snap.props.PropChange;
import snap.view.*;

/**
 * This JavaTextPane subclass adds a few more features (CodeBuilder).
 */
public class JavaTextPaneX extends JavaTextPane {

    // The code builder
    private CodeBuilder  _codeBuilder;

    // The SplitView
    private SplitView  _splitView;

    /**
     * Constructor.
     */
    public JavaTextPaneX()
    {
        super();
    }

    /**
     * Returns the CodeBuilder.
     */
    public CodeBuilder getCodeBuilder()
    {
        // If already set, just return
        if (_codeBuilder != null) return _codeBuilder;

        // Get, set, return
        CodeBuilder codeBuilder = new CodeBuilder(this);
        return _codeBuilder = codeBuilder;
    }

    /**
     * Returns whether CodeBuilder is visible.
     */
    public boolean isCodeBuilderVisible()
    {
        return _splitView.getItemCount() > 1;
    }

    /**
     * Sets whether CodeBuilder is visible.
     */
    public void setCodeBuilderVisible(boolean aFlag)
    {
        // If already set, just return
        if (aFlag == isCodeBuilderVisible()) return;
        View codeBuildrPane = getCodeBuilder().getUI();

        // If showing CodeBuilder, add to SplitView (animated)
        if (aFlag) {
            _splitView.addItemWithAnim(codeBuildrPane, 260);
            getCodeBuilder().setCodeBlocks();
        }

        // If hiding CodeBuilder, remove from SplitView (animated)
        else if (_splitView.getItemCount() > 1)
            _splitView.removeItemWithAnim(codeBuildrPane);
    }

    /**
     * Initialize UI panel.
     */
    @Override
    protected void initUI()
    {
        // Do normal version
        super.initUI();

        // Get TextArea and start listening for events (KeyEvents, MouseReleased, DragOver/Exit/Drop)
        enableEvents(_textArea, DragOver, DragExit, DragDrop);

        // Get ScrollGroup
        ScrollGroup scrollGroup = (ScrollGroup) getUI(BorderView.class).getCenter();

        // Get SplitView and add ScrollView and CodeBuilder
        _splitView = new SplitView();
        _splitView.addItem(scrollGroup);
        getUI(BorderView.class).setCenter(_splitView);
    }

    /**
     * Reset UI.
     */
    protected void resetUI()
    {
        // Do normal version
        super.resetUI();

        JavaDoc javaDoc = getJavaDoc();
        setViewVisible("JavaDocButton", javaDoc != null);
        String javaDocButtonText = javaDoc != null ? (javaDoc.getSimpleName() + " Doc") : null;
        setViewText("JavaDocButton", javaDocButtonText);
    }

    /**
     * Respond to UI controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Do normal version
        super.respondUI(anEvent);

        // Handle TextArea key events
        if (anEvent.equals("TextArea")) {

            // Handle DragOver, DragExit, DragDrop
            if (anEvent.isDragEvent()) {
                CodeBuilder codeBuilder = getCodeBuilder();
                if (anEvent.isDragOver())
                    codeBuilder.dragOver(anEvent.getX(), anEvent.getY());
                else if (anEvent.isDragExit())
                    codeBuilder.dragExit();
                else if (anEvent.isDragDropEvent())
                    codeBuilder.drop(0, 0);
            }
        }

        // Handle JavaDocButton
        else if (anEvent.equals("JavaDocButton")) {
            JavaDoc javaDoc = getJavaDoc();
            if (javaDoc != null)
                javaDoc.openUrl();
        }

        // Handle CodeBuilderButton
        else if (anEvent.equals("CodeBuilderButton"))
            setCodeBuilderVisible(!isCodeBuilderVisible());
    }

    /**
     * Called when JavaTextArea changes.
     */
    @Override
    protected void textAreaDidPropChange(PropChange aPC)
    {
        // Do normal version
        super.textAreaDidPropChange(aPC);

        // Handle SelectedNode change: Update CodeBuilder
        String propName = aPC.getPropName();
        if (propName == JavaTextArea.SelectedNode_Prop) {
            if (_codeBuilder != null && _codeBuilder.isVisible())
                _codeBuilder.setCodeBlocks();
        }
    }
}
