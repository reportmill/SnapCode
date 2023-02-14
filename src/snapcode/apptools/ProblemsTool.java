package snapcode.apptools;
import javakit.project.BuildIssue;
import javakit.ide.JavaTextUtils;
import javakit.project.BuildIssues;
import javakit.project.Project;
import snap.gfx.Image;
import snap.view.*;
import snap.web.WebFile;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;

/**
 * A pane/panel to show current build issues.
 */
public class ProblemsTool extends WorkspaceTool {

    // The selected issue
    private BuildIssue  _selIssue;

    // Constants
    private static Image ErrorImage = Image.get(JavaTextUtils.class, "ErrorMarker.png");
    private static Image WarningImage = Image.get(JavaTextUtils.class, "WarningMarker.png");

    /**
     * Constructor.
     */
    public ProblemsTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Returns the array of current build issues.
     */
    public BuildIssue[] getIssues()
    {
        Project proj = getProject();
        return proj.getBuildIssues().getIssues();
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
        Project proj = getProject();
        BuildIssues buildIssues = proj.getBuildIssues();
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
            BuildIssue issue = getSelIssue();
            if (issue != null) {
                WebFile file = issue.getFile();
                String urls = file.getURL().getString() + "#LineNumber=" + issue.getLineNumber();
                getBrowser().setURLString(urls);
            }
        }
    }

    /**
     * Configures an errors list cell.
     */
    protected void configureErrorsCell(ListCell<BuildIssue> aCell)
    {
        BuildIssue buildIssue = aCell.getItem();
        if (buildIssue == null) return;
        String text = String.format("%s (%s:%d)", buildIssue.getText(), buildIssue.getFile().getName(), buildIssue.getLine() + 1);
        text = text.replace('\n', ' ');
        aCell.setText(text);
        aCell.setImage(buildIssue.isError() ? ErrorImage : WarningImage);
        aCell.getGraphic().setPadding(2, 5, 2, 5);
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Problems"; }
}