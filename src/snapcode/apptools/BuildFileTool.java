package snapcode.apptools;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.gfx.GFXEnv;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.web.WebFile;
import snapcode.app.*;
import snapcode.project.*;
import snap.view.*;
import snap.viewx.*;
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
        buildFile.addPropChangeListener(this::buildFileDidChangeDependency, BuildFile.Dependency_Prop);

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

        // Update SourcePathText, BuildPathText, IncludeSnapKitRuntimeCheckBox, IncludeSnapChartsRuntimeCheckBox
        setViewValue("SourcePathText", buildFile.getSourcePath());
        setViewValue("BuildPathText", buildFile.getBuildPath());
        setViewValue("IncludeSnapKitRuntimeCheckBox", buildFile.isIncludeSnapKitRuntime());
        setViewValue("IncludeSnapChartsRuntimeCheckBox", buildFile.isIncludeSnapChartsRuntime());

        // Update RemoveDependencyButton
        setViewDisabled("RemoveDependencyButton", _dependenciesListArea.getSelItem() == null);

        // Get selected dependency
        BuildDependency selDependency = _dependenciesListArea.getSelItem();
        setViewVisible("DependencyTypeBox", selDependency != null);
        if (selDependency != null) {
            setViewSelItem("DependencyTypeComboBox", selDependency.getType());
            _dependenciesListArea.updateItem(selDependency);
        }

        // Update MavenDependencyBox
        boolean isMavenDependency = selDependency instanceof MavenDependency;
        setViewVisible("MavenDependencyBox", isMavenDependency);
        if (isMavenDependency) {

            // Update MavenIdText, GroupText, PackageNameText, VersionText, RepositoryURLText
            MavenDependency mavenDependency = (MavenDependency) selDependency;
            setViewValue("MavenIdText", mavenDependency.getId());
            setViewValue("GroupText", mavenDependency.getGroup());
            setViewValue("PackageNameText", mavenDependency.getName());
            setViewValue("VersionText", mavenDependency.getVersion());
            String repoURL = mavenDependency.getRepositoryURL();
            setViewValue("RepositoryURLText", repoURL);
            if (repoURL == null)
                getView("RepositoryURLText", TextField.class).setPromptText(mavenDependency.getRepositoryDefaultName());

            // Update StatusText, StatusProgressBar, ShowButton, ReloadButton, ClassPathsText
            String status = mavenDependency.getStatus();
            String error = mavenDependency.getError();
            setViewValue("StatusText", status);
            getView("StatusText", Label.class).setTextFill(error != null ? Color.RED : Color.BLACK);
            setViewVisible("StatusProgressBar", mavenDependency.isLoading());
            setViewVisible("ShowButton", status.equals("Loaded"));
            setViewVisible("ReloadButton", status.equals("Loaded"));
            setViewValue("ClassPathsLabel", error == null ? "Class path:" : "Error:");
            String classPathsText = error != null ? error : mavenDependency.getClassPathsJoined("\n");
            setViewValue("ClassPathsText", classPathsText);
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

        // Update MainClassNameText
        setViewValue("MainClassNameText", buildFile.getMainClassName());
    }

    /**
     * Respond to UI changes.
     */
    public void respondUI(ViewEvent anEvent)
    {
        BuildFile buildFile = getBuildFile();

        // Update SourcePathText, BuildPathText, IncludeSnapKitRuntimeCheckBox, IncludeSnapChartsRuntimeCheckBox
        if (anEvent.equals("SourcePathText"))
            buildFile.setSourcePath(anEvent.getStringValue());
        if (anEvent.equals("BuildPathText"))
            buildFile.setBuildPath(anEvent.getStringValue());
        if (anEvent.equals("IncludeSnapKitRuntimeCheckBox"))
            buildFile.setIncludeSnapKitRuntime(anEvent.getBoolValue());
        if (anEvent.equals("IncludeSnapChartsRuntimeCheckBox"))
            buildFile.setIncludeSnapChartsRuntime(anEvent.getBoolValue());

        // Handle AddDependencyButton, RemoveDependencyButton
        if (anEvent.equals("AddDependencyButton"))
            showAddDependencyPanel();
        if (anEvent.equals("RemoveDependencyButton"))
            removeSelectedDependency();

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
                buildFile.removeDependency(dependency);
            }
        }

        // Handle MavenIdText, GroupText, PackageNameText, VersionText, RepositoryURLText, ShowButton, ReloadButton
        BuildDependency selDependency = _dependenciesListArea.getSelItem();
        if (selDependency instanceof MavenDependency) {
            MavenDependency mavenDependency = (MavenDependency) selDependency;
            if (anEvent.equals("MavenIdText"))
                mavenDependency.setId(anEvent.getStringValue());
            if (anEvent.equals("GroupText"))
                mavenDependency.setGroup(anEvent.getStringValue());
            if (anEvent.equals("PackageNameText"))
                mavenDependency.setName(anEvent.getStringValue());
            if (anEvent.equals("VersionText"))
                mavenDependency.setVersion(anEvent.getStringValue());
            if (anEvent.equals("RepositoryURLText"))
                mavenDependency.setRepositoryURL(anEvent.getStringValue());
            if (anEvent.equals("ShowButton"))
                showMavenDependencyInFinder(mavenDependency);
            if (anEvent.equals("ReloadButton"))
                mavenDependency.loadPackageFiles();

            // If any of the above caused loading, make sure we resetUI after loading - I don't love this
            mavenDependency.getClassPaths();
            if (mavenDependency.isLoading())
                mavenDependency.addPropChangeListener(PropChangeListener.getOneShot(pc -> resetLater()), MavenDependency.Loading_Prop);
        }

        // Handle JarPathText, ProjectNameText
        if (anEvent.equals("JarPathText"))
            ((BuildDependency.JarFileDependency) selDependency).setJarPath(anEvent.getStringValue());
        if (anEvent.equals("ProjectNameText"))
            ((BuildDependency.ProjectDependency) selDependency).setProjectName(anEvent.getStringValue());

        // Handle DependencyTypeComboBox
        if (anEvent.equals("DependencyTypeComboBox"))
            changeSelectedDependencyType();

        // Handle MainClassNameText
        if (anEvent.equals("MainClassNameText"))
            buildFile.setMainClassName(anEvent.getStringValue());
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
                BuildDependency dependency = BuildDependency.getJarFileDependencyForPath(_proj, dependencyFilePath);
                if (dependency != null) {
                    BuildFile buildFile = getBuildFile();
                    buildFile.addDependency(dependency);
                }
            }
        }

        // Trigger build
        WorkspaceBuilder builder = _workspace.getBuilder();
        builder.buildWorkspaceLater();
        dragEvent.dropComplete();
    }

    /**
     * Shows an add project dialog box to add project dependency to this project.
     */
    private void showAddDependencyPanel()
    {
        // Create new dependency
        BuildDependency newDependency = new MavenDependency();

        // Add dependency
        BuildFile buildFile = getBuildFile();
        buildFile.addDependency(newDependency);

        // Focus MavenIdText
        runLater(() -> getView("MavenIdText").requestFocus());
    }

    /**
     * Removes the currently selected dependency.
     */
    private void removeSelectedDependency()
    {
        // Ask user to confirm (just return if cancelled)
        String msg = "Are you sure you want to remove selected dependency?";
        if (!DialogBox.showConfirmDialog(getUI(), "Remove Dependency", msg))
            return;

        // Remove selected
        BuildFile buildFile = getBuildFile();
        BuildDependency selDependency = _dependenciesListArea.getSelItem();
        buildFile.removeDependency(selDependency);
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
        BuildDependency newDependency = newType == BuildDependency.Type.Maven ? new MavenDependency() :
                newType == BuildDependency.Type.JarFile ? new BuildDependency.JarFileDependency() :
                new BuildDependency.ProjectDependency();
        buildFile.addDependency(newDependency, index);
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
     * Shows the given maven dependency in finder/explorer.
     */
    private void showMavenDependencyInFinder(MavenDependency mavenDependency)
    {
        WebFile file = mavenDependency.getLocalJarFile();
        if (file != null) {
            WebFile dirFile = file.isDir() ? file : file.getParent();
            GFXEnv.getEnv().openFile(dirFile);
        }
    }

    /**
     * Called when BuildFile changes dependency.
     */
    private void buildFileDidChangeDependency(PropChange aPC)
    {
        BuildFile buildFile = getBuildFile();

        // If BuildFile adds dependency, update list
        if (aPC.getNewValue() != null) {
            BuildDependency dependency = (BuildDependency) aPC.getNewValue();
            _dependenciesListArea.setItems(buildFile.getDependencies());
            _dependenciesListArea.setSelItem(dependency);
        }

        // If BuildFile removes dependency, remove from list
        else {
            BuildDependency dependency = (BuildDependency) aPC.getOldValue();
            _dependenciesListArea.removeItemAndUpdateSel(dependency);
        }

        resetLater();
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