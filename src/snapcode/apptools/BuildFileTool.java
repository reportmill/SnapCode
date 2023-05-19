package snapcode.apptools;
import javakit.project.*;
import snapcode.app.*;
import snapcode.project.VersionControl;
import snapcode.webbrowser.LoginPage;
import snapcode.webbrowser.ClientUtils;
import snap.util.TaskRunner;
import snap.view.*;
import snap.viewx.*;
import snap.web.WebSite;
import snapcode.webbrowser.WebPage;

import java.io.File;
import java.util.List;

/**
 * A class to manage UI aspects of a Project.
 */
public class BuildFileTool extends ProjectTool {

    // The selected JarPath
    private String  _jarPath;

    // The selected ProjectPath
    private String  _projPath;

    /**
     * Constructor.
     */
    public BuildFileTool(ProjectPane projectPane)
    {
        super(projectPane);
    }

    /**
     * Returns the project.
     */
    public Project getProject()  { return _proj; }

    /**
     * Adds a project with given name.
     */
    public void addProjectForName(String aName, String aURLString)
    {
        // If already set, just return
        Project existingProj = _proj.getProjectForName(aName);
        if (existingProj != null) {
            View view = isUISet() && getUI().isShowing() ? getUI() : _workspacePane.getUI();
            DialogBox.showWarningDialog(view, "Error Adding Project", "Project already present: " + aName);
            return;
        }

        // Get site
        AppBase app = AppBase.getShared();
        WebSite projSite = app.getProjectSiteForName(aName);

        // If project site doesn't exist, create it
        if ((projSite == null || !projSite.getExists()) && aURLString != null) {
            if (projSite == null)
                projSite = app.createProjectSiteForName(aName);
            VersionControl.setRemoteURLString(projSite, aURLString);
            VersionControl vc = VersionControl.create(projSite);
            checkoutProject(vc);
            return;
        }

        // If site still null complain and return
        if (projSite == null) {
            View view = isUISet() && getUI().isShowing() ? getUI() : _workspacePane.getUI();
            DialogBox.showErrorDialog(view, "Error Adding Project", "Project not found.");
            return;
        }

        // Add project for name
        _proj.addProjectForPath(aName);
    }

    /**
     * Removes a project with given name.
     */
    public void removeProjectForName(String aName)
    {
        // Just return if bogus
        if (aName == null || aName.length() == 0) {
            beep();
            return;
        }

        // Get named project
        Project proj = _proj.getProjectForName(aName);
        if (proj == null) {
            View view = isUISet() && getUI().isShowing() ? getUI() : _workspacePane.getUI();
            DialogBox.showWarningDialog(view, "Error Removing Project", "Project not found");
            return;
        }

        // Remove dependent project from root project and WorkspacePane
        _proj.removeProjectForPath(aName);
    }

    /**
     * Load all remote files into project directory.
     */
    public void checkoutProject(VersionControl aVC)
    {
        // Create checkout task runner
        String title = "Checkout from " + aVC.getRemoteURLString();
        TaskRunner<Object> checkoutRunner = new CheckoutTaskRunner(_workspacePane.getUI(), title, aVC);

        // Start task
        checkoutRunner.start();
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
        _proj.getBuildFile().removeLibPath(aJarPath);
    }

    /**
     * Returns the list of jar paths.
     */
    public String[] getJarPaths()
    {
        return _proj.getBuildFile().getLibPaths();
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
        return _proj.getBuildFile().getProjectPaths();
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
        // Update SourcePathText, BuildPathText
        Project proj = getProject();
        BuildFile projConfig = proj.getBuildFile();
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
        BuildFile projConfig = proj.getBuildFile();

        // Update SourcePathText, BuildPathText
        if (anEvent.equals("SourcePathText"))
            projConfig.setSourcePath(anEvent.getStringValue());
        if (anEvent.equals("BuildPathText"))
            projConfig.setBuildPath(anEvent.getStringValue());

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
                             _proj.getBuildFile().addLibPath(jarFilePath);
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
                addProjectForName(pname, null);
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
                removeProjectForName(getSelectedProjectPath());
        }
    }

    /**
     * This TaskRunner subclass checks out a project.
     */
    private class CheckoutTaskRunner extends TaskRunnerPanel<Object> {

        // The VersionControl
        private VersionControl  _versionControl;

        /**
         * Constructor.
         */
        public CheckoutTaskRunner(View aView, String aTitle, VersionControl aVC)
        {
            super(aView, aTitle);
            _versionControl = aVC;
        }

        public Object run() throws Exception
        {
            _versionControl.checkout(this);
            return null;
        }

        public void success(Object aRes)
        {
            // Add new project to root project
            WebSite vcSite = _versionControl.getSite();
            String projName = vcSite.getName();
            _proj.addProjectForPath(projName);

            // Build workspace
            WorkspaceBuilder builder = _workspace.getBuilder();
            builder.buildWorkspaceLater(true);
        }

        public void failure(Exception e)
        {
            WebSite remoteSite = _versionControl.getRemoteSite();

            // If attempt to set permissions succeeds, try again
            boolean setPermissionsSuccess = ClientUtils.setAccess(remoteSite);
            if (setPermissionsSuccess) {
                checkoutProject(_versionControl);
                return;
            }

            // If attempt to login succeeds, try again
            LoginPage loginPage = new LoginPage();
            boolean loginSuccess = loginPage.showPanel(_workspacePane.getUI(), remoteSite);
            if (loginSuccess) {
                checkoutProject(_versionControl);
                return;
            }

            // Do normal version
            super.failure(e);
        }
    }

    /**
     * A WebPage subclass for ProjectPane.
     */
    public static class BuildFilePage extends WebPage {

        /**
         * Initialize UI panel.
         */
        protected View createUI()
        {
            ProjectPane projectPane = ProjectPane.getProjectPaneForSite(getSite());
            BuildFileTool buildFileTool = projectPane.getProjectTools().getBuildFileTool();
            return buildFileTool.getUI();
        }

        /**
         * Override to provide better title.
         */
        public String getTitle()
        {
            return getURL().getSite().getName() + " - Build File";
        }
    }
}