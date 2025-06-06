package snapcode.apptools;
import snap.text.TextBlock;
import snap.util.ArrayUtils;
import snap.util.FormatUtils;
import snapcode.project.*;
import snapcode.javatext.JavaTextUtils;
import snap.gfx.Image;
import snap.view.*;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import java.util.Date;
import java.util.stream.Stream;

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
     * Cleans the workspace.
     */
    public void cleanWorkspace()
    {
        WorkspaceBuilder workspaceBuilder = _workspace.getBuilder();
        workspaceBuilder.cleanWorkspace();
        workspaceBuilder.addAllFilesToBuild();
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

        // Add mouse listener on BuildStatusLabel for hidden check errors feature (on double-click)
        addViewEventHandler("BuildStatusLabel", this::handleBuildStatusLabelMouseRelease, MouseRelease);
    }

    /**
     * Initialize when showing.
     */
    @Override
    protected void initShowing()
    {
        // Start listening to workspace prop changes
        _workspace.addPropChangeListener(pc -> handleWorkspaceBuildingChange(), Workspace.Building_Prop);
        _workspace.getTaskManager().addPropChangeListener(pc -> resetLater(), TaskManager.ActivityText_Prop);
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

        // Update ErrorsList
        setViewItems("ErrorsList", getIssues());
        setViewSelItem("ErrorsList", getSelIssue());

        // Update BuildLogTextBlock: If Workspace.Builder.BuildLogBuffer is longer, append to BuildLogTextView
        StringBuffer buildLogBuffer = _workspace.getBuilder().getBuildLogBuffer();
        if (buildLogBuffer.length() > _buildLogTextBlock.length()) {
            CharSequence appendStr = buildLogBuffer.subSequence(_buildLogTextBlock.length(), buildLogBuffer.length());
            _buildLogTextBlock.addChars(appendStr);
        }
        else if (buildLogBuffer.length() < _buildLogTextBlock.length()) {
            _buildLogTextBlock.clear();
            _buildLogTextBlock.addChars(buildLogBuffer);
        }
    }

    /**
     * Respond to UI controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle BuildButton, StopButton, CleanButton
            case "BuildButton" -> buildWorkspace();
            case "StopButton" -> _workspace.getBuilder().stopBuild();
            case "CleanButton" -> cleanWorkspace();

            // Handle ErrorsList
            case "ErrorsList" -> handleErrorListActionEvent(anEvent);

            // Do normal version
            default -> super.respondUI(anEvent);
        }
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
     * Called when ErrorList is clicked.
     */
    private void handleErrorListActionEvent(ViewEvent anEvent)
    {
        // Set issue
        BuildIssue issue = (BuildIssue) anEvent.getSelItem();
        setSelIssue(issue);

        // Open File
        if (issue != null) {
            WebFile file = issue.getFile();
            WebURL fileURL = file.getURL();
            String urls = fileURL.getString() + "#LineNumber=" + issue.getLineNumber();
            getBrowser().setSelUrlForUrlAddress(urls);
        }
    }

    /**
     * Called when Workspace Building property changes.
     */
    private void handleWorkspaceBuildingChange()
    {
        if (_workspace.isBuilding())
            ViewUtils.runLater(_buildLogTextBlock::clear);
        resetLater();
    }

    /**
     * Called when BuildStatusLabel gets MouseRelease event to trigger hidden check-syntax feature on double-click.
     */
    private void handleBuildStatusLabelMouseRelease(ViewEvent anEvent)
    {
        if (anEvent.isMouseClick() && anEvent.getClickCount() == 2)
            checkSelectedJavaFileForErrors();
    }

    /**
     * Checks the current file for errors.
     */
    private void checkSelectedJavaFileForErrors()
    {
        // Get selected java file and agent (just return if not java file)
        WebFile selFile = _pagePane.getSelFile();
        JavaAgent javaAgent = JavaAgent.getAgentForFile(selFile);
        if (javaAgent == null)
            return;

        // Check for errors and report
        _buildLogTextBlock.setString("Check " + selFile.getName() + " for errors (" + FormatUtils.formatDate(new Date()) + ")\n");
        javaAgent.checkFileForErrors();
        BuildIssue[] buildIssues = javaAgent.getBuildIssues();
        BuildIssue[] errorIssues = ArrayUtils.filter(buildIssues, bi -> bi.isError());
        _buildLogTextBlock.addChars("Found "  + errorIssues.length + " error(s)\n");
        Stream.of(buildIssues).forEach(bi -> _buildLogTextBlock.addChars(bi.getText()));
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Build"; }
}