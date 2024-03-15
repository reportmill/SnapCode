package snapcode.apptools;
import snap.text.TextBlock;
import snapcode.project.*;
import snapcode.javatext.JavaTextUtils;
import snap.gfx.Image;
import snap.view.*;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;

/**
 * A pane/panel to show current build issues.
 */
public class BuildTool extends WorkspaceTool {

    // The selected issue
    private BuildIssue  _selIssue;

    // The build log text
    private TextBlock _buildLogTextBlock;

    // Constants
    private static Image ErrorImage = Image.getImageForClassResource(JavaTextUtils.class, "ErrorMarker.png");
    private static Image WarningImage = Image.getImageForClassResource(JavaTextUtils.class, "WarningMarker.png");

    /**
     * Constructor.
     */
    public BuildTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Builds the workspace.
     */
    public void buildWorkspace()
    {
        if (ViewUtils.isAltDown())
            _workspace.getBuilder().addAllFilesToBuild();
        _workspace.getBuilder().buildWorkspaceLater();

        _workspaceTools.showTool(this);
    }

    /**
     * Returns the array of current build issues.
     */
    public BuildIssue[] getIssues()
    {
        BuildIssues buildIssues = _workspace.getBuildIssues();
        return buildIssues.getIssues();
    }

    /**
     * Returns the selected issue.
     */
    public BuildIssue getSelIssue()  { return _selIssue; }

    /**
     * Sets the selected issue.
     */
    public void setSelIssue(BuildIssue anIssue)
    {
        _selIssue = anIssue;
    }

    /**
     * Returns the string overview of the results of last build.
     */
    public String getBuildStatusText()
    {
        BuildIssues buildIssues = _workspace.getBuildIssues();
        int errorCount = buildIssues.getErrorCount();
        int warningCount = buildIssues.getWarningCount();
        String error = errorCount == 1 ? "error" : "errors";
        String warning = warningCount == 1 ? "warning" : "warnings";
        return String.format("%d %s, %d %s", errorCount, error, warningCount, warning);
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        // Configure ErrorsList
        ListView<BuildIssue> errorsList = getView("ErrorsList", ListView.class);
        errorsList.setCellConfigure(this::configureErrorsCell);
        errorsList.setRowHeight(24);

        // Get BuildLogTextBlock
        TextView buildLogTextView = getView("BuildLogTextView", TextView.class);
        _buildLogTextBlock = buildLogTextView.getTextBlock();
    }

    /**
     * Initialize when showing.
     */
    @Override
    protected void initShowing()
    {
        // Start listening to  workspace prop changes
        _workspace.addPropChangeListener(pc -> resetLater(), Workspace.Activity_Prop);
        _workspace.addPropChangeListener(pc -> workspaceBuildingChanged(), Workspace.Building_Prop);
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Update BuildButton, StopButton
        boolean isBuilding = _workspace.isBuilding();
        setViewEnabled("BuildButton", !isBuilding);
        setViewEnabled("StopButton", isBuilding);

        // Update BuildStatusLabel
        setViewText("BuildStatusLabel", getBuildStatusText());

        setViewItems("ErrorsList", getIssues());
        setViewSelItem("ErrorsList", getSelIssue());

        // Update BuildLogTextBlock: If Workspace.Builder.BuildLogBuffer is longer, append to BuildLogTextView
        StringBuffer buildLogBuffer = _workspace.getBuilder().getBuildLogBuffer();
        if (buildLogBuffer.length() > _buildLogTextBlock.length()) {
            CharSequence appendStr = buildLogBuffer.subSequence(_buildLogTextBlock.length(), buildLogBuffer.length());
            _buildLogTextBlock.addChars(appendStr);
        }
    }

    /**
     * Respond to UI controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle BuildButton, Handle StopButton
        if (anEvent.equals("BuildButton"))
            buildWorkspace();
        else if (anEvent.equals("StopButton"))
            _workspace.getBuilder().stopBuild();

        // Handle ErrorsList
        else if (anEvent.equals("ErrorsList")) {

            // Set issue
            BuildIssue issue = (BuildIssue) anEvent.getSelItem();
            setSelIssue(issue);

            // Open File
            if (issue != null) {
                WebFile file = issue.getFile();
                WebURL fileURL = file.getURL();
                String urls = fileURL.getString() + "#LineNumber=" + issue.getLineNumber();
                getBrowser().setSelUrlForUrlString(urls);
            }
        }

        // Handle CleanButton
        if (anEvent.equals("CleanButton")) {
            WorkspaceBuilder workspaceBuilder = _workspace.getBuilder();
            workspaceBuilder.cleanWorkspace();
            workspaceBuilder.addAllFilesToBuild();
        }

        // Do normal version
        else super.respondUI(anEvent);
    }

    /**
     * Configures an errors list cell.
     */
    protected void configureErrorsCell(ListCell<BuildIssue> aCell)
    {
        BuildIssue buildIssue = aCell.getItem();
        if (buildIssue == null)
            return;

        // Get/set cell text
        String issueText = buildIssue.getText();
        String issueFilename = buildIssue.getFile().getName();
        int issueLineNum = buildIssue.getLine() + 1;
        String text = String.format("%s (%s:%d)", issueText, issueFilename, issueLineNum);
        text = text.replace('\n', ' ');
        aCell.setText(text);

        // Set cell image
        aCell.setImage(buildIssue.isError() ? ErrorImage : WarningImage);
        aCell.getGraphic().setPadding(2, 5, 2, 5);
    }

    /**
     * Called when Workspace Building property changes.
     */
    private void workspaceBuildingChanged()
    {
        if (_workspace.isBuilding())
            ViewUtils.runLater(() -> _buildLogTextBlock.clear());
        resetLater();
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Build"; }
}