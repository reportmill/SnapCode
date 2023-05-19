package snapcode.app;
import javakit.ide.NodeMatcher;
import javakit.parse.JFile;
import javakit.parse.JNode;
import javakit.project.*;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.viewx.TaskMonitorPanel;
import snapcode.webbrowser.WebPage;
import snap.web.WebFile;
import snap.web.WebSite;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class to manage UI aspects of a Project.
 */
public class ProjectPane extends WebPage {

    // The WorkspacePane that owns this ProjectPane
    private WorkspacePane  _workspacePane;

    // The Project
    private Project  _project;

    // The ProjectTools
    private ProjectTools _projectTools;

    // A PropChangeListener for Site file changes
    private PropChangeListener  _siteFileLsnr = pc -> siteFileChanged(pc);

    /**
     * Constructor.
     */
    protected ProjectPane(WorkspacePane workspacePane, Project aProject)
    {
        super();
        _workspacePane = workspacePane;
        _project = aProject;

        // Create ProjectTools
        _projectTools = new ProjectTools(this);

        // Set this ProjectPane as Site prop
        WebSite projSite = aProject.getSite();
        projSite.addFileChangeListener(_siteFileLsnr);
        projSite.setProp(ProjectPane.class.getName(), this);
    }

    /**
     * Returns the WorkspacePane.
     */
    public WorkspacePane getWorkspacePane()  { return _workspacePane; }

    /**
     * Returns the Workspace.
     */
    public Workspace getWorkspace()  { return _workspacePane.getWorkspace(); }

    /**
     * Returns the project.
     */
    public Project getProject()  { return _project; }

    /**
     * Returns the project tools.
     */
    public ProjectTools getProjectTools()  { return _projectTools; }

    /**
     * Returns the site.
     */
    public WebSite getSite()  { return _project.getSite(); }

    /**
     * Opens the Site.
     */
    public void openSite()
    {
        // Do AutoBuild
        Workspace workspace = getWorkspace();
        WorkspaceBuilder builder = workspace.getBuilder();
        if (builder.isAutoBuildEnabled())
            builder.buildWorkspaceLater(true);
    }

    /**
     * Closes the site.
     */
    public void closeSite()
    {
        WebSite projSite = _project.getSite();
        projSite.removeFileChangeListener(_siteFileLsnr);
        projSite.setProp(ProjectPane.class.getName(), null);
        _workspacePane = null;
    }

    /**
     * Deletes a site.
     */
    public void deleteSite(View aView)
    {
        // Disable AutoBuild
        Workspace workspace = getWorkspace();
        WorkspaceBuilder builder = workspace.getBuilder();
        builder.setAutoBuild(false);

        // Delete project
        Project project = getProject();
        TaskMonitorPanel taskMonitorPanel = new TaskMonitorPanel(aView, "Delete Project");
        try { project.deleteProject(taskMonitorPanel); }
        catch (Exception e) { DialogBox.showExceptionDialog(aView, "Delete Project Failed", e); }
    }

    /**
     * Returns whether given file is a hidden file.
     */
    public boolean isHiddenFile(WebFile aFile)
    {
        if (aFile == getProject().getBuildDir())
            return true;
        return aFile.getPath().startsWith("/.git");
    }

    /**
     * Called when a site file changes.
     */
    private void siteFileChanged(PropChange aPC)
    {
        // Get source and property name
        WebFile file = (WebFile) aPC.getSource();
        String propName = aPC.getPropName();

        // Handle Saved property: Call fileAdded or fileSaved
        if (propName == WebFile.Exists_Prop) {
            if ((Boolean) aPC.getNewValue())
                fileAdded(file);
            else fileRemoved(file);
        }

        // Handle ModifedTime property: Call file saved
        if (propName == WebFile.ModTime_Prop && file.getExists())
            fileSaved(file);
    }

    /**
     * Called when file added to project.
     */
    private void fileAdded(WebFile aFile)
    {
        // If BuildDir file, just return
        if (_project.getBuildDir().containsFile(aFile)) return;

        // Add file to project and build workspace
        _project.fileAdded(aFile);
        buildWorkspace();
    }

    /**
     * Called when file removed from project.
     */
    private void fileRemoved(WebFile aFile)
    {
        // If BuildDir file, just return
        if (_project.getBuildDir().containsFile(aFile)) return;

        // Remove from project and build workspace
        _project.fileRemoved(aFile);
        buildWorkspace();
    }

    /**
     * Called when file saved in project.
     */
    private void fileSaved(WebFile aFile)
    {
        // If BuildDir file, just return
        if (_project.getBuildDir().containsFile(aFile)) return;

        // Notify saved and build workspace
        _project.fileSaved(aFile);
        buildWorkspace();
    }

    /**
     * Called to build workspace.
     */
    private void buildWorkspace()
    {
        Workspace workspace = _project.getWorkspace();
        WorkspaceBuilder builder = workspace.getBuilder();
        if (builder.isAutoBuild() && builder.isAutoBuildEnabled())
            builder.buildWorkspaceLater(false);
    }

    /**
     * Returns the site pane for a site.
     */
    public static ProjectPane getProjectPaneForSite(WebSite aSite)
    {
        return (ProjectPane) aSite.getProp(ProjectPane.class.getName());
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        Project project = getProject();
        setViewText("ProjectNameLabel", "Project: " + project.getName());
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Update AutoBuildCheckBox
        Workspace workspace = getWorkspace();
        setViewValue("AutoBuildCheckBox", workspace.getBuilder().isAutoBuild());
    }

    /**
     * Respond to UI changes.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle AutoBuildCheckBox
        if (anEvent.equals("AutoBuildCheckBox")) {
            Workspace workspace = getWorkspace();
            workspace.getBuilder().setAutoBuild(anEvent.getBoolValue());
        }

        // Handle LOCButton (Lines of Code)
        if (anEvent.equals("LOCTitleView")) {
            TitleView titleView = getView("LOCTitleView", TitleView.class);
            if (titleView.isExpanded()) return;
            TextView tview = getView("LOCText", TextView.class);
            tview.setText(getLinesOfCodeText());
        }

        // Shows symbol check
        if (anEvent.equals("SymbolCheckTitleView"))
            showSymbolCheck();
    }

    /**
     * Returns the line of code text.
     */
    private String getLinesOfCodeText()
    {
        // Declare loop variables
        StringBuilder sb = new StringBuilder("Lines of Code:\n\n");
        DecimalFormat fmt = new DecimalFormat("#,##0");
        int total = 0;

        // Get projects
        Project proj = getProject();
        List<Project> projects = new ArrayList<>();
        projects.add(proj);
        Collections.addAll(projects, proj.getProjects());

        // Iterate over projects and add: ProjName: xxx
        for (Project prj : projects) {
            int loc = getLinesOfCode(prj.getSourceDir());
            total += loc;
            sb.append(prj.getName()).append(": ").append(fmt.format(loc)).append('\n');
        }

        // Add total and return string (trimmed)
        sb.append("\nTotal: ").append(fmt.format(total)).append('\n');
        return sb.toString().trim();
    }

    /**
     * Returns lines of code in a file (recursive).
     */
    private int getLinesOfCode(WebFile aFile)
    {
        int loc = 0;

        if (aFile.isFile() && (aFile.getType().equals("java") || aFile.getType().equals("snp"))) {
            String text = aFile.getText();
            for (int i = text.indexOf('\n'); i >= 0; i = text.indexOf('\n', i + 1)) loc++;
        }
        else if (aFile.isDir()) {
            for (WebFile child : aFile.getFiles())
                loc += getLinesOfCode(child);
        }

        return loc;
    }

    /**
     * Shows a list of symbols that are undefined in project source files.
     */
    public void showSymbolCheck()
    {
        TitleView titleView = getView("SymbolCheckTitleView", TitleView.class);
        if (titleView.isExpanded()) return;

        // Get TextArea
        TextView symbolCheckTextView = getView("SymbolCheckText", TextView.class);
        _symbolCheckTextArea = symbolCheckTextView.getTextArea();
        if (_symbolCheckTextArea.length() > 0)
            return;

        // Initialize
        _symbolCheckTextArea.addChars("Undefined Symbols:\n");
        _symbolCheckTextArea.setSel(0, 0);

        Runnable run = () -> findUndefines(getProject().getSourceDir());
        new Thread(run).start();
    }

    TextArea _symbolCheckTextArea;
    JFile _symFile;
    int _undefCount;

    /**
     * Loads the undefined symbols in file.
     */
    private void findUndefines(WebFile aFile)
    {
        if (aFile.isFile() && aFile.getType().equals("java")) {
            JavaAgent javaAgent = JavaAgent.getAgentForFile(aFile);
            JNode jfile = javaAgent.getJFile();
            findUndefines(jfile);
        }

        else if (aFile.isDir())
            for (WebFile child : aFile.getFiles())
                findUndefines(child);
    }

    /**
     * Loads the undefined symbols in file.
     */
    private void findUndefines(JNode aNode)
    {
        if (_undefCount > 49) return;

        if (aNode.getDecl() == null && NodeMatcher.isDeclExpected(aNode)) {
            aNode.getDecl();
            _undefCount++;

            if (aNode.getFile() != _symFile) {
                _symFile = aNode.getFile();
                showSymText("\n" + aNode.getFile().getSourceFile().getName() + ":\n\n");
            }
            try {
                showSymText("    " + _undefCount + ". " + aNode + '\n');
            }

            catch (Exception e) {
                showSymText(e.toString());
            }
        }

        else if (aNode.getChildCount() > 0)
            for (JNode child : aNode.getChildren())
                findUndefines(child);
    }

    private void showSymText(String aStr)
    {
        int textAreaLength = _symbolCheckTextArea.length();
        runLater(() -> _symbolCheckTextArea.replaceChars(aStr, null, textAreaLength, textAreaLength, false));

        // Sleep
        try { Thread.sleep(80); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * A WebPage subclass for ProjectPane.
     */
    public static class ProjectPanePage extends WebPage {

        /**
         * Initialize UI panel.
         */
        protected View createUI()
        {
            ProjectPane projectPane = ProjectPane.getProjectPaneForSite(getSite());
            return projectPane.getUI();
        }

        /**
         * Override to provide better title.
         */
        public String getTitle()
        {
            return getURL().getSite().getName() + " - Project Settings";
        }
    }
}