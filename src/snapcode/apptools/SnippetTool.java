package snapcode.apptools;
import snap.props.PropChangeListener;
import snap.view.ColView;
import snap.view.EventListener;
import snap.view.View;
import snap.view.ViewEvent;
import snapcode.app.JavaPage;
import snapcode.app.PagePane;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import snapcode.javatext.JavaTextArea;
import snapcode.webbrowser.WebPage;

/**
 * A class to provide snippets.
 */
public class SnippetTool extends WorkspaceTool {

    // The SnapTool
    private SnapTool _snapTool;

    // The CompleterTool
    private CompleterTool _completerTool;

    // The currently visible tool
    private ChildTool _visibleTool;

    // Listener for JavaTextArea prop change
    private PropChangeListener _textAreaPropChangeLsnr = pc -> javaTextAreaSelNodeChanged();

    // Listener for JavaTextArea drag events
    private EventListener _textAreaDragEventLsnr = e -> handleJavaTextAreaDragEvent(e);

    // The current PagePane JavaTextArea
    private JavaTextArea _javaTextArea;

    /**
     * Constructor.
     */
    public SnippetTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
        _snapTool = new SnapTool(workspacePane);
        _completerTool = new CompleterTool(workspacePane);
    }

    /**
     * Override to show default snippet tool.
     */
    @Override
    protected void initUI()
    {
        setVisibleTool(_snapTool);
    }

    /**
     * Init showing.
     */
    @Override
    protected void initShowing()
    {
        // Start listening to PagePane.SelFile prop change
        _pagePane.addPropChangeListener(pc -> pagePaneSelFileChanged(), PagePane.SelFile_Prop);
        pagePaneSelFileChanged();
    }

    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle PlainCheckBox
        if (anEvent.equals("PlainCheckBox")) {
            ChildTool newTool = anEvent.getBoolValue() ? _completerTool : _snapTool;
            setVisibleTool(newTool);
        }
    }

    /**
     * Sets the selected tool.
     */
    private void setVisibleTool(ChildTool workspaceTool)
    {
        if (workspaceTool == _visibleTool) return;

        // Remove old VisibleTool
        ColView colView = getUI(ColView.class);
        if (_visibleTool != null)
            colView.removeChild(_visibleTool.getUI());

        // Set
        _visibleTool = workspaceTool;

        // Add new VisibleTool
        View toolUI = _visibleTool.getUI();
        toolUI.setGrowHeight(true);
        colView.addChild(toolUI);

        // Update new visible tool for JavaTextArea
        _visibleTool._javaTextArea = _javaTextArea;
        javaTextAreaSelNodeChanged();
    }

    /**
     * Called when PagePane.SelFile property changes
     */
    private void pagePaneSelFileChanged()
    {
        WebPage selPage = _pagePane.getSelPage();
        JavaPage javaPage = selPage instanceof JavaPage ? (JavaPage) selPage : null;
        JavaTextArea javaTextArea = javaPage != null ? javaPage.getTextArea() : null;
        setJavaTextArea(javaTextArea);
    }

    /**
     * Sets the JavaTextArea associated with text pane.
     */
    private void setJavaTextArea(JavaTextArea javaTextArea)
    {
        if (javaTextArea == _javaTextArea) return;

        // Remove listeners from old JavaTextArea
        if (_javaTextArea != null) {
            _javaTextArea.removePropChangeListener(_textAreaPropChangeLsnr);
            _javaTextArea.removeEventHandler(_textAreaDragEventLsnr);
        }

        // Set
        _javaTextArea = javaTextArea;
        _visibleTool._javaTextArea = javaTextArea;

        // Start listening to JavaTextArea SelNode prop and drag events
        if (_javaTextArea != null) {
            _javaTextArea.addPropChangeListener(_textAreaPropChangeLsnr, JavaTextArea.SelNode_Prop);
            _javaTextArea.addEventHandler(_textAreaDragEventLsnr, View.DragEvents);
            javaTextAreaSelNodeChanged();
        }
    }

    /**
     * Called when JavaTextArea SelNode prop changes.
     */
    protected void javaTextAreaSelNodeChanged()
    {
        if (isShowing())
            _visibleTool.javaTextAreaSelNodeChanged();
    }

    /**
     * Called when JavaTextArea gets drag events.
     */
    protected void handleJavaTextAreaDragEvent(ViewEvent anEvent)
    {
        if (isShowing())
            _visibleTool.handleJavaTextAreaDragEvent(anEvent);
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Snippets"; }

    /**
     * A WorkspaceTool subclass to add some notify methods.
     */
    protected static class ChildTool extends WorkspaceTool {

        // The current JavaTextArea
        protected JavaTextArea _javaTextArea;

        /**
         * Constructor.
         */
        public ChildTool(WorkspacePane workspacePane)  { super(workspacePane); }

        /**
         * Called when JavaTextArea.SelNode changes.
         */
        protected void javaTextAreaSelNodeChanged()  { }

        /**
         * Called when JavaTextArea gets drag events.
         */
        protected void handleJavaTextAreaDragEvent(ViewEvent anEvent)  { }
    }
}
