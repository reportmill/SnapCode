package snapcode.apptools;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.text.TextModel;
import snap.text.TextStyle;
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
import java.util.List;

/**
 * A pane/panel to show current build issues.
 */
public class BuildTool extends WorkspaceTool {

    // The selected issue
    private BuildIssue  _selIssue;

    // The errors tree
    private TreeView<Object> _errorsTree;

    // The build log text
    private TextModel _buildLogTextModel;

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
    }

    /**
     * Returns the list of issue files.
     */
    public List<WebFile> getBuildIssueFiles()
    {
        BuildIssues buildIssuesMgr = _workspace.getBuildIssues();
        List<BuildIssue> buildIssues = buildIssuesMgr.getBuildIssues();
        return buildIssues.stream().map(issue -> issue.getFile()).distinct().toList();
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
        // Configure ErrorsTree
        _errorsTree = getView("ErrorsTree", TreeView.class);
        _errorsTree.setResolver(new ErrorTreeResolver());
        _errorsTree.setCellConfigure(this::configureErrorsTreeCell);
        _errorsTree.setRowHeight(28);

        // Get BuildLogTextBlock
        TextView buildLogTextView = getView("BuildLogTextView", TextView.class);
        _buildLogTextModel = buildLogTextView.getTextModel();

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

        // Update ErrorsTree
        _errorsTree.setItems((List<Object>) (List<?>) getBuildIssueFiles());
        _errorsTree.setSelItem(getSelIssue());
        _errorsTree.expandAll();

        // Update BuildLogTextBlock: If Workspace.Builder.BuildLogBuffer is longer, append to BuildLogTextView
        StringBuffer buildLogBuffer = _workspace.getBuilder().getBuildLogBuffer();
        if (buildLogBuffer.length() > _buildLogTextModel.length()) {
            CharSequence appendStr = buildLogBuffer.subSequence(_buildLogTextModel.length(), buildLogBuffer.length());
            _buildLogTextModel.addChars(appendStr);
        }
        else if (buildLogBuffer.length() < _buildLogTextModel.length()) {
            _buildLogTextModel.clear();
            _buildLogTextModel.addChars(buildLogBuffer);
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

            // Handle ErrorsTree
            case "ErrorsTree" -> handleErrorsTreeActionEvent(anEvent);

            // Do normal version
            default -> super.respondUI(anEvent);
        }
    }

    /**
     * Configures an errors tree cell.
     */
    protected void configureErrorsTreeCell(ListCell<?> aCell)
    {
        // Handle Error file (headers)
        if (aCell.getItem() instanceof WebFile errorFile) {
            aCell.setFill(ERROR_TREE_HEADER_COLOR);
            aCell.getTextArea().clear();
            aCell.getTextArea().setRichText(true);
            aCell.getTextArea().addCharsWithStyle(getErrorFileText(errorFile), ERROR_TREE_HEADER_STYLE1);
            aCell.getTextArea().addCharsWithStyle("   " + errorFile.getSite().getName() + errorFile.getParent().getPath(), ERROR_TREE_HEADER_STYLE2);
        }

        // Handle BuildIssue
        else if (aCell.getItem() instanceof BuildIssue buildIssue) {
            aCell.setText(getBuildIssueText(buildIssue));
            aCell.setFont(Font.Arial14);
            if (!aCell.isSelected())
                aCell.setFill(ERROR_TREE_CONTENT_COLOR);

            // Set cell image
            aCell.setImage(buildIssue.isError() ? ErrorImage : WarningImage);
            aCell.getGraphic().setPadding(2, 5, 2, 5);
        }

        // Handle empty cell
        else aCell.setFill(ERROR_TREE_CONTENT_COLOR);
    }

    // ErrorsTree cell fill colors
    private static final Color ERROR_TREE_HEADER_COLOR = Color.get("#F6");
    private static final Color ERROR_TREE_CONTENT_COLOR = Color.get("#FD");
    private static final TextStyle ERROR_TREE_HEADER_STYLE1 = new TextStyle().copyForStyleValues(Font.Arial16);
    private static final TextStyle ERROR_TREE_HEADER_STYLE2 = new TextStyle().copyForStyleValues(Font.Arial16, Color.get("#80"));

    /**
     * Called when ErrorList is clicked.
     */
    private void handleErrorsTreeActionEvent(ViewEvent anEvent)
    {
        // Set issue
        BuildIssue issue = getIssueForErrorTreeNode(anEvent.getSelItem());
        setSelIssue(issue);

        // Open File
        if (issue != null) {
            WebFile file = issue.getFile();
            WebURL fileURL = file.getUrl();
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
            ViewUtils.runLater(_buildLogTextModel::clear);
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
        _buildLogTextModel.setString("Check " + selFile.getName() + " for errors (" + FormatUtils.formatDate(new Date()) + ")\n");
        javaAgent.checkFileForErrors();
        List<BuildIssue> errorIssues = javaAgent.getBuildErrors();
        _buildLogTextModel.addChars("Found "  + errorIssues.size() + " error(s)\n");
        List<BuildIssue> buildIssues = javaAgent.getBuildIssues();
        buildIssues.forEach(bi -> _buildLogTextModel.addChars(bi.getText()));
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Build"; }

    /**
     * Returns an issue for given errors tree node.
     */
    private static BuildIssue getIssueForErrorTreeNode(Object errorTreeNode)
    {
        if (errorTreeNode instanceof BuildIssue)
            return (BuildIssue) errorTreeNode;
        WebFile errorFile = (WebFile) errorTreeNode;
        JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(errorFile);
        List<BuildIssue> buildIssues = javaAgent.getBuildIssues();
        return buildIssues.get(0);
    }

    /**
     * Returns the error file text.
     */
    private static String getErrorFileText(WebFile errorFile)
    {
        JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(errorFile);
        List<BuildIssue> buildErrors = javaAgent.getBuildErrors();
        return errorFile.getName() + " - " + buildErrors.size() + " errors";
    }

    /**
     * Returns the build issue text.
     */
    private static String getBuildIssueText(BuildIssue buildIssue)
    {
        String issueText = buildIssue.getText();
        String issueFilename = buildIssue.getFile().getName();
        int issueLineNum = buildIssue.getLine() + 1;
        String text = String.format("%s (%s:%d)", issueText, issueFilename, issueLineNum);
        return text.replace('\n', ' ');
    }

    /**
     * TreeResolver for ErrorsTree.
     */
    private static class ErrorTreeResolver extends TreeResolver<Object> {

        @Override
        public Object getParent(Object anItem)
        {
            if (anItem instanceof WebFile)
                return null;
            BuildIssue issue = (BuildIssue) anItem;
            return issue.getFile();
        }

        @Override
        public boolean isParent(Object anItem)
        {
            return anItem instanceof WebFile;
        }

        @Override
        public List<Object> getChildren(Object aParent)
        {
            WebFile errorFile = (WebFile) aParent;
            JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(errorFile);
            return (List<Object>) (List<?>) javaAgent.getBuildIssues();
        }

        @Override
        public String getText(Object anItem)
        {
            if (anItem instanceof WebFile)
                return getErrorFileText((WebFile) anItem);
            if (anItem instanceof BuildIssue)
                return getBuildIssueText((BuildIssue) anItem);
            return null;
        }
    }
}