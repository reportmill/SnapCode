package snapcode.apptools;
import snap.util.TaskMonitor;
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
     * Builds the app.
     */
    public boolean buildApp()
    {
        // Get JavaAgent
        WebFile selFile = getSelFile();
        JavaAgent javaAgent = selFile != null ? JavaAgent.getAgentForFile(selFile) : null;
        if (javaAgent == null)
            return false;

        // Get ProjectBuilder and add build file
        Project proj = getProject();
        ProjectBuilder projectBuilder = proj.getBuilder();
        projectBuilder.addBuildFile(selFile, true);

        // Build project
        TaskMonitor taskMonitor = new TaskMonitor.Text(System.out);
        boolean success = projectBuilder.buildProject(taskMonitor);

        // Build file - if failed, show errors
        if (!success)
            _workspaceTools.showTool(this);

        // Return
        return success;
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
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        setViewText("BuildStatusLabel", getBuildStatusText());

        setViewItems("ErrorsList", getIssues());
        setViewSelItem("ErrorsList", getSelIssue());
    }

    /**
     * Respond to UI controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle ErrorsList
        if (anEvent.equals("ErrorsList")) {

            // Set issue
            BuildIssue issue = (BuildIssue) anEvent.getSelItem();
            setSelIssue(issue);

            // Open File
            if (issue != null) {
                WebFile file = issue.getFile();
                WebURL fileURL = file.getURL();
                String urls = fileURL.getString() + "#LineNumber=" + issue.getLineNumber();
                getBrowser().setURLString(urls);
            }
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
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Build"; }
}