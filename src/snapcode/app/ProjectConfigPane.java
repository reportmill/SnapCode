package snapcode.app;
import javakit.parse.JFile;
import javakit.parse.JNode;
import javakit.ide.NodeMatcher;
import javakit.project.JavaAgent;
import javakit.project.Project;
import javakit.project.ProjectConfig;
import snapcode.apptools.ProblemsTool;
import snapcode.project.ProjectX;
import snapcode.project.ProjectSet;
import snapcode.project.VersionControl;
import snap.util.ClientUtils;
import snap.util.TaskRunner;
import snap.view.*;
import snap.viewx.*;
import snap.web.WebFile;
import snap.web.WebSite;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class to manage UI aspects of a Project.
 */
public class ProjectConfigPane extends ViewOwner {

    // The AppPane
    private AppPane  _appPane;

    // The SitePane
    private SitePane  _sitePane;

    // The WebSite
    private WebSite  _site;

    // The project
    private ProjectX  _proj;

    // The project set
    private ProjectSet  _projSet;

    // Whether to auto build project when files change
    private boolean  _autoBuild = true;

    // Whether to auto build project feature is enabled
    private boolean  _autoBuildEnabled = true;

    // The runner to build files
    private BuildFilesRunner  _buildFilesRunner;

    // The selected JarPath
    private String  _jarPath;

    // The selected ProjectPath
    private String  _projPath;

    // Runnable for build later
    private Runnable  _buildLaterRun;

    /**
     * Constructor for given project.
     */
    public ProjectConfigPane(WebSite aSite)
    {
        _site = aSite;
        _proj = ProjectX.getProjectForSite(_site);
        if (_proj == null)
            _proj = new ProjectX(_site);
        _projSet = _proj.getProjectSet();
    }

    /**
     * Creates a new ProjectPane for given project.
     */
    public ProjectConfigPane(SitePane aSitePane)
    {
        this(aSitePane.getSite());
        _sitePane = aSitePane;
        _site.setProp(ProjectConfigPane.class.getName(), this);
    }

    /**
     * Returns the AppPane.
     */
    public AppPane getAppPane()  { return _appPane; }

    /**
     * Sets the AppPane.
     */
    protected void setAppPane(AppPane anAP)
    {
        _appPane = anAP;
    }

    /**
     * Returns the SitePane.
     */
    public SitePane getSitePane()  { return _sitePane; }

    /**
     * Returns the project.
     */
    public ProjectX getProject()  { return _proj; }

    /**
     * Returns whether to automatically build files when changes are detected.
     */
    public boolean isAutoBuild()  { return _autoBuild; }

    /**
     * Sets whether to automatically build files when changes are detected.
     */
    public void setAutoBuild(boolean aValue)  { _autoBuild = aValue; }

    /**
     * Returns whether to project AutoBuild has been disabled (possibly for batch processing).
     */
    public boolean isAutoBuildEnabled()  { return isAutoBuild() && _autoBuildEnabled; }

    /**
     * Sets whether to project AutoBuild has been disabled (possibly for batch processing).
     */
    public boolean setAutoBuildEnabled(boolean aFlag)
    {
        boolean o = _autoBuildEnabled;
        _autoBuildEnabled = aFlag;
        return o;
    }

    /**
     * Activate project.
     */
    public void openSite()
    {
        // Kick off site build
        if (_sitePane.isAutoBuildEnabled())
            buildProjectLater(true);
    }

    /**
     * Delete a project.
     */
    public void deleteProject(View aView)
    {
        _sitePane.setAutoBuild(false);
        try {
            _proj.deleteProject(new TaskMonitorPanel(aView, "Delete Project"));
        } catch (Exception e) {
            DialogBox.showExceptionDialog(aView, "Delete Project Failed", e);
        }
    }

    /**
     * Adds a project with given name.
     */
    public void addProject(String aName, String aURLString)
    {
        View view = isUISet() && getUI().isShowing() ? getUI() : getAppPane().getUI();
        addProject(aName, aURLString, view);
    }

    /**
     * Adds a project with given name.
     */
    public void addProject(String aName, String aURLString, View aView)
    {
        // If already set, just return
        ProjectSet projectSet = _proj.getProjectSet();
        if (projectSet.getProject(aName) != null) {
            DialogBox.showWarningDialog(aView, "Error Adding Project", "Project already present: " + aName);
            return;
        }

        // Get site - if not present, create and clone
        WebSite site = WelcomePanel.getShared().getSite(aName);
        if ((site == null || !site.getExists()) && aURLString != null) {
            if (site == null) site = WelcomePanel.getShared().createSite(aName, false);
            VersionControl.setRemoteURLString(site, aURLString);
            VersionControl vc = VersionControl.create(site);
            checkout(aView, vc);
            return;
        }

        // If site still null complain and return
        if (site == null) {
            DialogBox.showErrorDialog(aView, "Error Adding Project", "Project not found.");
            return;
        }

        // Add project for name
        _proj.getProjectSet().addProject(aName);
        if (_appPane != null)
            _appPane.addSite(site);
    }

    /**
     * Load all remote files into project directory.
     */
    public void checkout(View aView, VersionControl aVC)
    {
        WebSite site = aVC.getSite();
        String title = "Checkout from " + aVC.getRemoteURLString();

        TaskRunner<Object> runner = new TaskRunnerPanel<Object>(_appPane.getUI(), title) {

            public Object run() throws Exception
            {
                aVC.checkout(this);
                return null;
            }

            public void success(Object aRes)
            {
                // Add new project to root project
                _proj.getProjectSet().addProject(site.getName());
                if (_appPane == null) return;

                // Add new project site to app pane and build
                _appPane.addSite(site);
                ProjectX proj = ProjectX.getProjectForSite(site);
                proj.addBuildFilesAll();
                buildProjectLater(false);
            }

            public void failure(Exception e)
            {
                if (ClientUtils.setAccess(aVC.getRemoteSite()))
                    checkout(aView, aVC);
                else if (new LoginPage().showPanel(_appPane.getUI(), aVC.getRemoteSite()))
                    checkout(aView, aVC);
                else super.failure(e);
            }
        };
        runner.start();
    }

    /**
     * Removes a project with given name.
     */
    public void removeProject(String aName)
    {
        // Just return if bogus
        if (aName == null || aName.length() == 0) {
            beep();
            return;
        }

        // Get named project
        ProjectSet projectSet = _proj.getProjectSet();
        Project proj = projectSet.getProject(aName);
        if (proj == null) {
            View view = isUISet() && getUI().isShowing() ? getUI() : getAppPane().getUI();
            DialogBox.showWarningDialog(view, "Error Removing Project", "Project not found");
            return;
        }

        // Remove dependent project from root project and AppPane
        _proj.getProjectSet().removeProject(aName);
        WebSite site = proj.getSite();
        getAppPane().removeSite(site);
    }

    /**
     * Removes the given Jar path.
     */
    public void removeJarPath(String aJarPath)
    {
        // Just return if bogus
        if (aJarPath == null || aJarPath.length() == 0) {
            beep();
            return;
        }

        // Remove path from classpath
        _proj.getProjectConfig().removeLibPath(aJarPath);
    }

    /**
     * Build project.
     */
    public void buildProjectLater(boolean doAddFiles)
    {
        // If not root ProjectPane, forward on to it
        ProjectX rootProj = _proj.getRootProject();
        ProjectConfigPane rootProjPane = _proj != rootProj ? ProjectConfigPane.getProjectPane(rootProj.getSite()) : this;
        if (this != rootProjPane) {
            rootProjPane.buildProjectLater(doAddFiles);
            return;
        }

        // If not already set, register for buildLater run
        if (_buildLaterRun != null) return;
        runLater(_buildLaterRun = () -> buildProject(doAddFiles));
    }

    /**
     * Build project.
     */
    public void buildProject(boolean doAddFiles)
    {
        getBuildFilesRunner(doAddFiles);
        _buildLaterRun = null;
    }

    /**
     * Returns the build files runner.
     */
    private synchronized void getBuildFilesRunner(boolean addBuildFiles)
    {
        if (_buildFilesRunner != null) {
            if (addBuildFiles)
                _buildFilesRunner._addFiles = addBuildFiles;
            _buildFilesRunner._runAgain = true;
            _proj.interruptBuild();
        }

        else {
            _buildFilesRunner = new BuildFilesRunner();
            _buildFilesRunner._addFiles = addBuildFiles;
            _buildFilesRunner.start();
        }
    }

    /**
     * An Runner subclass to build project files in the background.
     */
    public class BuildFilesRunner extends TaskRunner<Object> {

        // Whether to add files and run again
        boolean _addFiles, _runAgain;

        /**
         * BuildFiles.
         */
        public Object run()
        {
            if (_addFiles) {
                _addFiles = false;
                _projSet.addBuildFilesAll();
            }
            _projSet.buildProjects(this);
            return true;
        }

        public void beginTask(final String aTitle, int theTotalWork)
        {
            setActivity(aTitle);
        }

        public void finished()
        {
            boolean runAgain = _runAgain;
            _runAgain = false;
            if (runAgain) start();
            else _buildFilesRunner = null;
            setActivity("Build Completed");
            runLater(() -> handleBuildCompleted());
        }

        void setActivity(String aStr)
        {
            if (_appPane != null) _appPane.getBrowser().setActivity(aStr);
        }

        public void failure(final Exception e)
        {
            e.printStackTrace();
            runLater(() -> DialogBox.showExceptionDialog(null, "Build Failed", e));
            _runAgain = false;
        }
    }

    /**
     * Removes build files from the project.
     */
    public void cleanProject()
    {
        boolean old = setAutoBuildEnabled(false);
        _proj.cleanProject();
        setAutoBuildEnabled(old);
    }

    /**
     * Called when file added to project.
     */
    void fileAdded(WebFile aFile)
    {
        if (_proj.getBuildDir().contains(aFile)) return;
        _proj.fileAdded(aFile);
        if (_sitePane.isAutoBuild() && _sitePane.isAutoBuildEnabled())
            buildProjectLater(false);
    }

    /**
     * Called when file removed from project.
     */
    void fileRemoved(WebFile aFile)
    {
        if (_proj.getBuildDir().contains(aFile)) return;
        _proj.fileRemoved(aFile);
        if (_sitePane.isAutoBuild() && _sitePane.isAutoBuildEnabled())
            buildProjectLater(false);
    }

    /**
     * Called when file saved in project.
     */
    void fileSaved(WebFile aFile)
    {
        if (_proj.getBuildDir().contains(aFile)) return;
        _proj.fileSaved(aFile);
        if (_sitePane.isAutoBuild() && _sitePane.isAutoBuildEnabled())
            buildProjectLater(false);
    }

    /**
     * Returns the list of jar paths.
     */
    public String[] getJarPaths()
    {
        return _proj.getProjectConfig().getLibPaths();
    }

    /**
     * Returns the selected JarPath.
     */
    public String getSelectedJarPath()
    {
        if (_jarPath == null && getJarPaths().length > 0)
            _jarPath = getJarPaths()[0];
        return _jarPath;
    }

    /**
     * Sets the selected JarPath.
     */
    public void setSelectedJarPath(String aJarPath)
    {
        _jarPath = aJarPath;
    }

    /**
     * Returns the list of dependent project paths.
     */
    public String[] getProjectPaths()
    {
        return _proj.getProjectConfig().getProjectPaths();
    }

    /**
     * Returns the selected Project Path.
     */
    public String getSelectedProjectPath()
    {
        if (_projPath == null && getProjectPaths().length > 0)
            _projPath = getProjectPaths()[0];
        return _projPath;
    }

    /**
     * Sets the selected Project Path.
     */
    public void setSelectedProjectPath(String aProjPath)
    {
        _projPath = aProjPath;
    }

    /**
     * Called when a build is completed.
     */
    protected void handleBuildCompleted()
    {
        // If final error count non-zero, show problems pane
        int errorCount = _proj.getRootProject().getBuildIssues().getErrorCount();
        if (errorCount > 0) {
            SupportTray supportTray = _appPane.getSupportTray();
            if (supportTray.getSelTool() instanceof ProblemsTool)
                supportTray.showProblemsTool();
        }

        // If error count zero and SupportTray showing problems, close
        if (errorCount == 0) {
            SupportTray supportTray = _appPane.getSupportTray();
            if (supportTray.getSelTool() instanceof ProblemsTool)
                supportTray.hideTools();
        }
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        // Have Backspace and Delete remove selected Jar path
        addKeyActionHandler("DeleteAction", "DELETE");
        addKeyActionHandler("BackSpaceAction", "BACK_SPACE");
        enableEvents("JarPathsList", DragEvents);
        enableEvents("ProjectPathsList", MouseRelease);
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Update HomePageText, AutoBuildCheckBox
        SitePane sitePane = getSitePane();
        setViewValue("HomePageText", sitePane.getHomePageURLString());
        setViewValue("AutoBuildCheckBox", sitePane.isAutoBuild());

        // Update SourcePathText, BuildPathText
        ProjectX proj = getProject();
        ProjectConfig projConfig = proj.getProjectConfig();
        setViewValue("SourcePathText", projConfig.getSourcePath());
        setViewValue("BuildPathText", projConfig.getBuildPath());

        // Update JarPathsList, ProjectPathsList
        setViewItems("JarPathsList", getJarPaths());
        setViewSelItem("JarPathsList", getSelectedJarPath());
        setViewItems("ProjectPathsList", getProjectPaths());
        setViewSelItem("ProjectPathsList", getSelectedProjectPath());
    }

    /**
     * Respond to UI changes.
     */
    public void respondUI(ViewEvent anEvent)
    {
        SitePane sitePane = getSitePane();
        ProjectX proj = getProject();
        ProjectConfig projConfig = proj.getProjectConfig();

        // Handle HomePageText, AutoBuildCheckBox
        if (anEvent.equals("HomePageText"))
            sitePane.setHomePageURLString(anEvent.getStringValue());
        if (anEvent.equals("AutoBuildCheckBox"))
            sitePane.setAutoBuild(anEvent.getBoolValue());

        // Update SourcePathText, BuildPathText
        if (anEvent.equals("SourcePathText"))
            projConfig.setSourcePath(anEvent.getStringValue());
        if (anEvent.equals("BuildPathText"))
            projConfig.setBuildPath(anEvent.getStringValue());

        // Handle ResetHomePageButton
        if (anEvent.equals("ResetHomePageButton"))
            _sitePane.setHomePageURLString(null);

        // Handle JarPathsList
        if (anEvent.equals("JarPathsList")) {

            // Handle DragEvent
             if (anEvent.isDragEvent()) {
                 anEvent.acceptDrag(); //TransferModes(TransferMode.COPY);
                 anEvent.consume();
                 if (anEvent.isDragDropEvent()) {
                     List<File> files = anEvent.getClipboard().getJavaFiles();
                     if (files == null || files.size() == 0) return;
                     for (File file : files) {
                         String path = file.getAbsolutePath(); //if(StringUtils.endsWithIC(path, ".jar"))
                         _proj.getProjectConfig().addLibPath(path);
                     }
                     _sitePane.buildSite(false);
                     anEvent.dropComplete();
                 }
             }

             // Handle click
            else {
                String jarPath = anEvent.getStringValue();
                setSelectedJarPath(jarPath);
             }
        }

        // Handle ProjectPathsList
        if (anEvent.equals("ProjectPathsList")) {

            // Handle double click
            if (anEvent.getClickCount() > 1) {
                DialogBox dbox = new DialogBox("Add Project Dependency");
                dbox.setQuestionMessage("Enter Project Name:");
                String pname = dbox.showInputDialog(getUI(), null);
                if (pname == null || pname.length() == 0) return;
                addProject(pname, null);
            }

            // Handle click
            else {
                String projPath = anEvent.getStringValue();
                setSelectedProjectPath(projPath);
            }
        }

        // Handle DeleteAction
        if (anEvent.equals("DeleteAction") || anEvent.equals("BackSpaceAction")) {
            if (getView("JarPathsList").isShowing())
                removeJarPath(getSelectedJarPath());
            else if (getView("ProjectPathsList").isShowing())
                removeProject(getSelectedProjectPath());
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
        ProjectX proj = getProject();
        List<ProjectX> projects = new ArrayList<>();
        projects.add(proj);
        Collections.addAll(projects, proj.getProjects());

        // Iterate over projects and add: ProjName: xxx
        for (ProjectX prj : projects) {
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
        _symText = getView("SymbolCheckText", TextView.class);
        if (_symText.length() > 0) return;
        _symText.addChars("Undefined Symbols:\n");
        _symText.setSel(0, 0);

        Runnable run = () -> findUndefines(getProject().getSourceDir());
        new Thread(run).start();
    }

    TextView _symText;
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
        runLater(() -> _symText.replaceChars(aStr, null, _symText.length(), _symText.length(), false));

        // Sleep
        try { Thread.sleep(80); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Returns the ProjectPane for a site.
     */
    public synchronized static ProjectConfigPane getProjectPane(WebSite aSite)
    {
        return (ProjectConfigPane) aSite.getProp(ProjectConfigPane.class.getName());
    }
}