package snapcode.apptools;
import snap.gfx.Color;
import snap.gfx.Font;
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

    // Dependencies ListArea
    private ListArea<BuildDependency> _dependenciesListArea;

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
     * Adds a dependency to build file.
     */
    public void addDependency(BuildDependency dependency, int anIndex)
    {
        // Add dependency
        BuildFile buildFile = getBuildFile();
        buildFile.addDependency(dependency, anIndex);

        // Select dependency and reset
        _dependenciesListArea.setItems(buildFile.getDependencies());
        _dependenciesListArea.setSelItem(dependency);
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
     * Initialize UI.
     */
    protected void initUI()
    {
        // Have Backspace and Delete remove selected Jar path
        addKeyActionHandler("DeleteAction", "DELETE");
        addKeyActionHandler("BackSpaceAction", "BACK_SPACE");

        // Configure DependenciesListView
        ListView<BuildDependency> dependenciesListView = getView("DependenciesListView", ListView.class);
        enableEvents(dependenciesListView, DragEvents);

        // Configure DependenciesListArea
        _dependenciesListArea = dependenciesListView.getListArea();
        _dependenciesListArea.setRowHeight(26);
        _dependenciesListArea.setCellConfigure(this::configureDependenciesListCell);

        // Set DependenciesListArea items
        BuildFile buildFile = getBuildFile();
        BuildDependency[] dependencies = buildFile.getDependencies();
        _dependenciesListArea.setItems(dependencies);

        // Configure DependencyTypeComboBox
        ComboBox<BuildDependency.Type> dependencyTypeComboBox = getView("DependencyTypeComboBox", ComboBox.class);
        dependencyTypeComboBox.setItems(BuildDependency.Type.values());
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        BuildFile buildFile = getBuildFile();

        // Update SourcePathText, BuildPathText, IncludeSnapKitRuntimeCheckBox
        setViewValue("SourcePathText", buildFile.getSourcePath());
        setViewValue("BuildPathText", buildFile.getBuildPath());
        setViewValue("IncludeSnapKitRuntimeCheckBox", buildFile.isIncludeSnapKitRuntime());

        // Update RemoveDependencyButton
        setViewDisabled("RemoveDependencyButton", _dependenciesListArea.getSelItem() == null);

        // Get selected dependency
        BuildDependency selDependency = _dependenciesListArea.getSelItem();
        setViewVisible("DependencyTypeBox", selDependency != null);
        if (selDependency != null) {
            setViewSelItem("DependencyTypeComboBox", selDependency.getType());
            _dependenciesListArea.updateItem(selDependency);
        }

        // Update MavenDependencyBox, MavenIdText, GroupText, PackageNameText, VersionText, RepositoryURLText
        boolean isMavenDependency = selDependency instanceof BuildDependency.MavenDependency;
        setViewVisible("MavenDependencyBox", isMavenDependency);
        if (isMavenDependency) {
            BuildDependency.MavenDependency mavenDependency = (BuildDependency.MavenDependency) selDependency;
            setViewValue("MavenIdText", mavenDependency.getId());
            setViewValue("GroupText", mavenDependency.getGroup());
            setViewValue("PackageNameText", mavenDependency.getName());
            setViewValue("VersionText", mavenDependency.getVersion());
            String repoURL = mavenDependency.getRepositoryURL();
            setViewValue("RepositoryURLText", repoURL);
            if (repoURL == null)
                getView("RepositoryURLText", TextField.class).setPromptText(mavenDependency.getRepositoryDefaultName());
        }

        // Update JarFileDependencyBox, JarPathText
        boolean isJarFileDependency = selDependency instanceof BuildDependency.JarFileDependency;
        setViewVisible("JarFileDependencyBox", isJarFileDependency);
        if (isJarFileDependency) {
            BuildDependency.JarFileDependency jarFileDependency = (BuildDependency.JarFileDependency) selDependency;
            setViewValue("JarPathText", jarFileDependency.getJarPath());
        }

        // Update ProjectDependencyBox, ProjectNameText
        boolean isProjectDependency = selDependency instanceof BuildDependency.ProjectDependency;
        setViewVisible("ProjectDependencyBox", isProjectDependency);
        if (isProjectDependency) {
            BuildDependency.ProjectDependency projectDependency = (BuildDependency.ProjectDependency) selDependency;
            setViewValue("ProjectNameText", projectDependency.getProjectName());
        }
    }

    /**
     * Respond to UI changes.
     */
    public void respondUI(ViewEvent anEvent)
    {
        BuildFile buildFile = getBuildFile();

        // Update SourcePathText, BuildPathText, IncludeSnapKitRuntimeCheckBox
        if (anEvent.equals("SourcePathText"))
            buildFile.setSourcePath(anEvent.getStringValue());
        if (anEvent.equals("BuildPathText"))
            buildFile.setBuildPath(anEvent.getStringValue());
        if (anEvent.equals("IncludeSnapKitRuntimeCheckBox"))
            buildFile.setIncludeSnapKitRuntime(anEvent.getBoolValue());

        // Handle AddDependencyButton, RemoveDependencyButton
        if (anEvent.equals("AddDependencyButton"))
            showAddDependencyPanel();
        if (anEvent.equals("RemoveDependencyButton")) {

            // Ask user to confirm (just return if cancelled)
            String msg = "Are you sure you want to remove selected dependency?";
            if (!DialogBox.showConfirmDialog(getUI(), "Remove Dependency", msg))
                return;

            // Remove selected
            int selIndex = _dependenciesListArea.getSelIndex();
            if (selIndex >= 0)
                buildFile.removeDependency(selIndex);
            int newSelIndex = Math.max(selIndex, _dependenciesListArea.getItemCount() - 1);
            _dependenciesListArea.setSelIndex(newSelIndex);
        }

        // Handle DependenciesList
        if (anEvent.equals("DependenciesListView")) {

            // Handle DragEvent
            if (anEvent.isDragEvent())
                handleDependenciesListDragEvent(anEvent);
        }

        // Handle DeleteAction
        if (anEvent.equals("DeleteAction") || anEvent.equals("BackSpaceAction")) {
            if (getView("DependenciesListView").isFocused()) {
                BuildDependency dependency = (BuildDependency) getViewSelItem("DependenciesListView");
                removeDependency(dependency);
            }
        }

        // Handle MavenIdText, GroupText, PackageNameText, VersionText, RepositoryURLText
        BuildDependency selDependency = _dependenciesListArea.getSelItem();
        if (anEvent.equals("MavenIdText"))
            ((BuildDependency.MavenDependency) selDependency).setId(anEvent.getStringValue());
        if (anEvent.equals("GroupText"))
            ((BuildDependency.MavenDependency) selDependency).setGroup(anEvent.getStringValue());
        if (anEvent.equals("PackageNameText"))
            ((BuildDependency.MavenDependency) selDependency).setName(anEvent.getStringValue());
        if (anEvent.equals("VersionText"))
            ((BuildDependency.MavenDependency) selDependency).setVersion(anEvent.getStringValue());
        if (anEvent.equals("RepositoryURLText"))
            ((BuildDependency.MavenDependency) selDependency).setRepositoryURL(anEvent.getStringValue());

        // Handle JarPathText, ProjectNameText
        if (anEvent.equals("JarPathText"))
            ((BuildDependency.JarFileDependency) selDependency).setJarPath(anEvent.getStringValue());
        if (anEvent.equals("ProjectNameText"))
            ((BuildDependency.ProjectDependency) selDependency).setProjectName(anEvent.getStringValue());

        // Handle DependencyTypeComboBox
        if (anEvent.equals("DependencyTypeComboBox"))
            changeSelectedDependencyType();
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
     * Shows an add project dialog box to add project dependency to this project.
     */
    private void showAddDependencyPanel()
    {
//        // Show dialog box to get dependency name/path/string (just return if cancelled/empty)
//        DialogBox dialogBox = new DialogBox("Add Build Dependency");
//        dialogBox.setQuestionMessage("Enter dependency string:");
//        String dependencyStr = dialogBox.showInputDialog(getUI(), null);
//        if (dependencyStr == null || dependencyStr.length() == 0)
//            return;
//
//        // Get dependency for id string (just return if not found)
//        BuildDependency newDependency = BuildDependency.getDependencyForPath(_proj, dependencyStr);
//        if (newDependency == null)
//            return;

        // Create new dependency
        BuildDependency newDependency = new BuildDependency.MavenDependency();

        // Add dependency
        addDependency(newDependency, _dependenciesListArea.getItemCount());

        // Focus MavenIdText
        runLater(() -> getView("MavenIdText").requestFocus());
    }

    /**
     * Changes selected dependency type.
     */
    private void changeSelectedDependencyType()
    {
        // Remove selected dependency
        BuildFile buildFile = getBuildFile();
        int index = _dependenciesListArea.getSelIndex();
        buildFile.removeDependency(index);

        // Create new dependency for type and add
        ComboBox<BuildDependency.Type> dependencyTypeComboBox = getView("DependencyTypeComboBox", ComboBox.class);
        BuildDependency.Type newType = dependencyTypeComboBox.getSelItem();
        BuildDependency newDependency = newType == BuildDependency.Type.Maven ? new BuildDependency.MavenDependency() :
                newType == BuildDependency.Type.JarFile ? new BuildDependency.JarFileDependency() :
                new BuildDependency.ProjectDependency();
        addDependency(newDependency, index);
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
     * Configure
     */
    private void configureDependenciesListCell(ListCell<BuildDependency> aCell)
    {
        BuildDependency buildDependency = aCell.getItem();
        if (buildDependency == null)
            return;

        // Set text
        aCell.setText(buildDependency.getId());

        // Set type as label on left
        Label label = new Label(buildDependency.getType().toString());
        label.setFont(Font.Arial10);
        label.setBorder(Color.DARKGRAY, 1);
        label.setMargin(0, 8, 0, 4);
        label.setPadding(1, 1, 1, 2);
        aCell.setGraphic(label);
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