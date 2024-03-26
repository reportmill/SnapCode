package snapcode.apptools;
import snap.view.ColView;
import snap.view.View;
import snap.view.ViewEvent;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;

/**
 * A class to provide snippets.
 */
public class SnippetTool extends WorkspaceTool {

    // The SnapTool
    private SnapTool _snapTool;

    // The CompleterTool
    private CompleterTool _completerTool;

    // The currently visible tool
    private WorkspaceTool _visibleTool;

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

    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle PlainCheckBox
        if (anEvent.equals("PlainCheckBox")) {
            WorkspaceTool newTool = anEvent.getBoolValue() ? _completerTool : _snapTool;
            setVisibleTool(newTool);
        }
    }

    /**
     * Sets the selected tool.
     */
    private void setVisibleTool(WorkspaceTool workspaceTool)
    {
        if (workspaceTool == _visibleTool) return;

        ColView colView = getUI(ColView.class);

        if (_visibleTool != null)
            colView.removeChild(_visibleTool.getUI());
        _visibleTool = workspaceTool;
        View toolUI = _visibleTool.getUI();
        toolUI.setGrowHeight(true);
        colView.addChild(toolUI);
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Snippets"; }
}
