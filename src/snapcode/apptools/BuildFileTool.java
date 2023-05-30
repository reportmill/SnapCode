package snapcode.apptools;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.app.*;
import snapcode.project.*;
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
     * Returns the BuildFile.
     */
    public BuildFile getBuildFile()  { return _proj.getBuildFile(); }

    /**
     * Adds a dependency for given string.
     */
    public void addDependencyForIdString(String idString)
    {
        // Get dependency for id string (just return if not found)
        BuildDependency dependency = BuildDependency.getDependencyForPath(_proj, idString);
        if (dependency == null)
            return;

        // Add dependency
        BuildFile buildFile = getBuildFile();
        buildFile.addDependency(dependency);

        // Select dependency and reset
        setViewItems("DependenciesListView", buildFile.getDependencies());
        setViewSelItem("DependenciesListView", dependency);
        resetLater();
    }

    /**
     * Removes a dependency.
     */
    public void removeDependency(BuildDependency buildDependency)
    {
        // Add dependency
        BuildFile buildFile = getBuildFile();
        buildFile.removeDependency(buildDependency);

        // Select dependency and reset
        setViewItems("DependenciesListView", buildFile.getDependencies());
        setViewSelItem("DependenciesListView", null);
        resetLater();
    }

    /**
     * Adds a project with given name.
     */
    public void addProjectForName(String aName, String aURLString)
    {
        // If project already present, just return
        Project existingProj = _proj.getProjectForName(aName);
        if (existingProj != null) {
            View view = isUISet() && getUI().isShowing() ? getUI() : _workspacePane.getUI();
            DialogBox.showWarningDialog(view, "Error Adding Project", "Project already present: " + aName);
            return;
        }

        // Get project site
        WebSite projSite = findProjectForName(aName);

        // If project site not found but VersionControl URL provided, try to check out project
        boolean projectNotFound = projSite == null || !projSite.getExists();
        if (projectNotFound && aURLString != null) {

            // Create project site if missing
            //if (projSite == null) projSite = AppBase.getShared().createProjectSiteForName(aName);

            // Checkout project for URL
            VersionControl.setRemoteURLString(projSite, aURLString);
            VersionControl versionControl = VersionControl.createVersionControlForProjectSite(projSite);
            Runnable successCallback = () -> addProjectForNameImpl(aName);
            VersionControlTool.checkoutProject(versionControl, _workspacePane.getUI(), successCallback);
            return;
        }

        // If site still null complain and return
        if (projSite == null) {
            View view = isUISet() && getUI().isShowing() ? getUI() : _workspacePane.getUI();
            DialogBox.showErrorDialog(view, "Error Adding Project", "Project not found.");
            return;
        }

        // Add project for name
        addProjectForNameImpl(aName);
    }

    /**
     * Adds a project with given name.
     */
    public void addProjectForNameImpl(String aName)
    {
        // Add project for name
        _proj.addProjectForPath(aName);

        // Build workspace
        WorkspaceBuilder builder = _workspace.getBuilder();
        builder.buildWorkspaceLater(true);
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
        getBuildFile().removeLibPath(aJarPath);
    }

    /**
     * Returns the list of jar paths.
     */
    public String[] getJarPaths()
    {
        return getBuildFile().getLibPaths();
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
        return getBuildFile().getProjectPaths();
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

        // Configure DependenciesListView
        ListView<BuildDependency> dependenciesListView = getView("DependenciesListView", ListView.class);
        dependenciesListView.setItemTextFunction(dep -> dep.getType() + " " + dep.getId());
        enableEvents(dependenciesListView, DragEvents);
        dependenciesListView.addEventFilter(e -> { if (e.getClickCount() == 2) showAddDependencyPanel(); }, MousePress);

        // Configure JarPathsList, ProjectPathsList
        enableEvents("JarPathsList", DragEvents);
        enableEvents("ProjectPathsList", MouseRelease);
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        BuildFile buildFile = getBuildFile();

        // Update SourcePathText, BuildPathText
        setViewValue("SourcePathText", buildFile.getSourcePath());
        setViewValue("BuildPathText", buildFile.getBuildPath());

        // Update DependenciesList
        setViewItems("DependenciesListView", buildFile.getDependencies());

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
        BuildFile buildFile = getBuildFile();

        // Update SourcePathText, BuildPathText
        if (anEvent.equals("SourcePathText"))
            buildFile.setSourcePath(anEvent.getStringValue());
        if (anEvent.equals("BuildPathText"))
            buildFile.setBuildPath(anEvent.getStringValue());

        // Handle DependenciesList
        if (anEvent.equals("DependenciesListView")) {

            // Handle DragEvent
             if (anEvent.isDragEvent())
                 handleDependenciesListDragEvent(anEvent);

            // Handle double click: Show add dependency panel
            if (anEvent.getClickCount() > 1)
                showAddDependencyPanel();
        }

        // Handle JarPathsList
        if (anEvent.equals("JarPathsList")) {

            // Handle DragEvent
             if (anEvent.isDragEvent())
                 handleJarFilesListDragEvent(anEvent);

             // Handle click
            else {
                String jarPath = anEvent.getStringValue();
                setSelectedJarPath(jarPath);
             }
        }

        // Handle ProjectPathsList
        if (anEvent.equals("ProjectPathsList")) {

            // Handle double click: Show add project panel
            if (anEvent.getClickCount() > 1)
                showAddProjectPanel();

            // Handle click
            else {
                String projPath = anEvent.getStringValue();
                setSelectedProjectPath(projPath);
            }
        }

        // Handle DeleteAction
        if (anEvent.equals("DeleteAction") || anEvent.equals("BackSpaceAction")) {
            if (getView("DependenciesListView").isFocused()) {
                BuildDependency dependency = (BuildDependency) getViewSelItem("DependenciesListView");
                removeDependency(dependency);
            }
            else if (getView("JarPathsList").isFocused())
                removeJarPath(getSelectedJarPath());
            else if (getView("ProjectPathsList").isFocused())
                removeProjectForName(getSelectedProjectPath());
        }
    }

    /**
     * Handles drag/drop jar files.
     */
    private void handleDependenciesListDragEvent(ViewEvent dragEvent)
    {
        dragEvent.acceptDrag();
        dragEvent.consume();
        if (!dragEvent.isDragDropEvent())
            return;

        // Get dropped dependency files
        List<File> dependencyFiles = dragEvent.getClipboard().getJavaFiles();
        if (dependencyFiles != null) {

            // Iterate over dependency files and add dependency for each
            for (File dependencyFile : dependencyFiles) {
                String dependencyFilePath = dependencyFile.getAbsolutePath();
                BuildDependency dependency = BuildDependency.getDependencyForPath(_proj, dependencyFilePath);
                if (dependency != null) {
                    BuildFile buildFile = getBuildFile();
                    buildFile.addDependency(dependency);
                }
            }
        }

        // Trigger build
        WorkspaceBuilder builder = _workspace.getBuilder();
        builder.buildWorkspaceLater(false);
        dragEvent.dropComplete();
    }

    /**
     * Handles drag/drop jar files.
     */
    private void handleJarFilesListDragEvent(ViewEvent dragEvent)
    {
        dragEvent.acceptDrag(); //TransferModes(TransferMode.COPY);
        dragEvent.consume();
        if (!dragEvent.isDragDropEvent())
            return;

        // Get dropped jar files
        List<File> jarFiles = dragEvent.getClipboard().getJavaFiles();
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
        dragEvent.dropComplete();
    }

    /**
     * Shows an add project dialog box to add project dependency to this project.
     */
    private void showAddDependencyPanel()
    {
        // Show dialog box to get dependency name/path/string (just return if cancelled/empty)
        DialogBox dialogBox = new DialogBox("Add Build Dependency");
        dialogBox.setQuestionMessage("Enter dependency string:");
        String dependencyStr = dialogBox.showInputDialog(getUI(), null);
        if (dependencyStr == null || dependencyStr.length() == 0)
            return;

        // Add Project for name
        addDependencyForIdString(dependencyStr);
    }

    /**
     * Shows an add project dialog box to add project dependency to this project.
     */
    private void showAddProjectPanel()
    {
        // Show dialog box to get project name (just return if cancelled/empty)
        DialogBox dialogBox = new DialogBox("Add Project Dependency");
        dialogBox.setQuestionMessage("Enter Project Name:");
        String projectName = dialogBox.showInputDialog(getUI(), null);
        if (projectName == null || projectName.length() == 0)
            return;

        // Add Project for name
        addProjectForName(projectName, null);
    }

    /**
     * Finds a project site for given name.
     */
    private WebSite findProjectForName(String aName)
    {
        // return AppBase.getShared().getProjectSiteForName(aName);
        WebSite thisProjSite = getProjectSite();
        WebURL thisProjSiteURL = thisProjSite.getURL();
        WebURL parentURL = thisProjSiteURL.getParent();
        WebFile parentDir = parentURL != null ? parentURL.getFile() : null;
        WebFile projSiteDir = parentDir != null ? parentDir.getFileForName(aName) : null;
        WebURL projSiteURL = projSiteDir != null ? projSiteDir.getURL() : null;
        return projSiteURL != null ? projSiteURL.getAsSite() : null;
    }

    /**
     * A WebPage subclass for BuildFileTool.
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