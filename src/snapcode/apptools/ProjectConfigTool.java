package snapcode.apptools;
import javakit.parse.JFile;
import javakit.parse.JNode;
import javakit.ide.NodeMatcher;
import javakit.project.*;
import snapcode.app.*;
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
public class ProjectConfigTool extends ProjectTool {

    // The selected JarPath
    private String  _jarPath;

    // The selected ProjectPath
    private String  _projPath;

    /**
     * Constructor.
     */
    public ProjectConfigTool(ProjectPane projectPane)
    {
        super(projectPane);

        // Set Project.Site.Prop so instances can be located easily
        WebSite projSite = _proj.getSite();
        projSite.setProp(ProjectConfigTool.class.getName(), this);
    }

    /**
     * Returns the project.
     */
    public Project getProject()  { return _proj; }

    /**
     * Activate project.
     */
    public void openSite()
    {
        // Do AutoBuild
        WorkspaceBuilder builder = _workspace.getBuilder();
        if (builder.isAutoBuildEnabled())
            builder.buildWorkspaceLater(true);
    }

    /**
     * Delete a project.
     */
    public void deleteProject(View aView)
    {
        WorkspaceBuilder builder = _workspace.getBuilder();
        builder.setAutoBuild(false);

        try { _proj.deleteProject(new TaskMonitorPanel(aView, "Delete Project")); }
        catch (Exception e) {
            DialogBox.showExceptionDialog(aView, "Delete Project Failed", e);
        }
    }

    /**
     * Adds a project with given name.
     */
    public void addProject(String aName, String aURLString)
    {
        View view = isUISet() && getUI().isShowing() ? getUI() : _workspacePane.getUI();
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
            if (site == null)
                site = WelcomePanel.getShared().createSiteForName(aName, false);
            VersionControl.setRemoteURLString(site, aURLString);
            VersionControl vc = VersionControl.create(site);
            checkout(vc);
            return;
        }

        // If site still null complain and return
        if (site == null) {
            DialogBox.showErrorDialog(aView, "Error Adding Project", "Project not found.");
            return;
        }

        // Add project for name
        _proj.addProjectForPath(aName);
    }

    /**
     * Load all remote files into project directory.
     */
    public void checkout(VersionControl aVC)
    {
        WebSite site = aVC.getSite();
        String title = "Checkout from " + aVC.getRemoteURLString();

        TaskRunner<Object> runner = new TaskRunnerPanel<Object>(_workspacePane.getUI(), title) {

            public Object run() throws Exception
            {
                aVC.checkout(this);
                return null;
            }

            public void success(Object aRes)
            {
                // Add new project to root project
                String projName = site.getName();
                _proj.addProjectForPath(projName);

                // Build workspace
                WorkspaceBuilder builder = _workspace.getBuilder();
                builder.buildWorkspaceLater(true);
            }

            public void failure(Exception e)
            {
                WebSite remoteSite = aVC.getRemoteSite();

                // If attempt to set permissions succeeds, try again
                boolean setPermissionsSuccess = ClientUtils.setAccess(remoteSite);
                if (setPermissionsSuccess) {
                    checkout(aVC);
                    return;
                }

                // If attempt to login succeeds, try again
                LoginPage loginPage = new LoginPage();
                boolean loginSuccess = loginPage.showPanel(_workspacePane.getUI(), remoteSite);
                if (loginSuccess) {
                    checkout(aVC);
                    return;
                }

                // Do normal version
                super.failure(e);
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
            View view = isUISet() && getUI().isShowing() ? getUI() : _workspacePane.getUI();
            DialogBox.showWarningDialog(view, "Error Removing Project", "Project not found");
            return;
        }

        // Remove dependent project from root project and WorkspacePane
        _proj.removeProjectForPath(aName);
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
     * Called when file added to project.
     */
    public void fileAdded(WebFile aFile)
    {
        if (_proj.getBuildDir().contains(aFile)) return;
        _proj.fileAdded(aFile);

        // Do AutoBuild
        WorkspaceBuilder builder = _workspace.getBuilder();
        if (builder.isAutoBuild() && builder.isAutoBuildEnabled())
            builder.buildWorkspaceLater(false);
    }

    /**
     * Called when file removed from project.
     */
    public void fileRemoved(WebFile aFile)
    {
        if (_proj.getBuildDir().contains(aFile)) return;
        _proj.fileRemoved(aFile);

        // Do AutoBuild
        WorkspaceBuilder builder = _workspace.getBuilder();
        if (builder.isAutoBuild() && builder.isAutoBuildEnabled())
            builder.buildWorkspaceLater(false);
    }

    /**
     * Called when file saved in project.
     */
    public void fileSaved(WebFile aFile)
    {
        if (_proj.getBuildDir().contains(aFile)) return;
        _proj.fileSaved(aFile);

        // Do AutoBuild
        WorkspaceBuilder builder = _workspace.getBuilder();
        if (builder.isAutoBuild() && builder.isAutoBuildEnabled())
            builder.buildWorkspaceLater(false);
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
        //setViewValue("HomePageText", sitePane.getHomePageURLString());
        setViewValue("AutoBuildCheckBox", _workspace.getBuilder().isAutoBuild());

        // Update SourcePathText, BuildPathText
        Project proj = getProject();
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
        Project proj = getProject();
        ProjectConfig projConfig = proj.getProjectConfig();

        // Handle HomePageText, AutoBuildCheckBox
        //if (anEvent.equals("HomePageText")) sitePane.setHomePageURLString(anEvent.getStringValue());
        if (anEvent.equals("AutoBuildCheckBox"))
            _workspace.getBuilder().setAutoBuild(anEvent.getBoolValue());

        // Update SourcePathText, BuildPathText
        if (anEvent.equals("SourcePathText"))
            projConfig.setSourcePath(anEvent.getStringValue());
        if (anEvent.equals("BuildPathText"))
            projConfig.setBuildPath(anEvent.getStringValue());

        // Handle ResetHomePageButton
        //if (anEvent.equals("ResetHomePageButton")) _sitePane.setHomePageURLString(null);

        // Handle JarPathsList
        if (anEvent.equals("JarPathsList")) {

            // Handle DragEvent
             if (anEvent.isDragEvent()) {
                 anEvent.acceptDrag(); //TransferModes(TransferMode.COPY);
                 anEvent.consume();
                 if (anEvent.isDragDropEvent()) {

                     // Get dropped jar files
                     List<File> jarFiles = anEvent.getClipboard().getJavaFiles();
                     if (jarFiles != null) {

                         // Add JarPaths
                         for (File jarFile : jarFiles) {
                             String jarFilePath = jarFile.getAbsolutePath();
                             //if(StringUtils.endsWithIC(path, ".jar"))
                             _proj.getProjectConfig().addLibPath(jarFilePath);
                         }
                     }

                     // Trigger build
                     WorkspaceBuilder builder = _workspace.getBuilder();
                     builder.buildWorkspaceLater(false);
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
     * Returns the ProjectConfigPane for a site.
     */
    public synchronized static ProjectConfigTool getProjectPane(WebSite aSite)
    {
        return (ProjectConfigTool) aSite.getProp(ProjectConfigTool.class.getName());
    }
}