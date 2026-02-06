package snapcode.apptools;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.gfx.GFXEnv;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.util.ListUtils;
import snap.util.SnapEnv;
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

    // The selected build repository
    private MavenRepository _selRepository;

    // The selected build dependency
    private BuildDependency _selDependency;

    // A prop change listener for selected dependency
    private PropChangeListener _selDependencyPropChangeLsnr = this::handleSelDependencyPropChange;

    // Repositories ListView
    private ListView<MavenRepository> _repositoriesListView;

    // Dependencies TreeView
    private TreeView<BuildDependency> _dependenciesTreeView;

    // The Java versions
    private static final Integer[] JAVA_VERSIONS = new Integer[] { 8, 9, 10, 11, 12, 13, 14, 15, 16, 17 };

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
     * Returns the selected build repository.
     */
    public MavenRepository getSelRepository()  { return _selRepository; }

    /**
     * Sets the selected build repository.
     */
    public void setSelRepository(MavenRepository buildRepository)
    {
        if (buildRepository == _selRepository) return;

        // Set value and add/remove prop change listener
        _selRepository = buildRepository;

        // Update RepositoriesListView.Selection
        _repositoriesListView.setSelItem(buildRepository);
        resetLater();
    }

    /**
     * Returns the selected build dependency.
     */
    public BuildDependency getSelDependency()  { return _selDependency; }

    /**
     * Sets the selected build dependency.
     */
    public void setSelDependency(BuildDependency buildDependency)
    {
        if (buildDependency == _selDependency) return;

        // Set value and add/remove prop change listener
        if (_selDependency != null)
            _selDependency.removePropChangeListener(_selDependencyPropChangeLsnr);
        _selDependency = buildDependency;
        if (_selDependency != null)
            _selDependency.addPropChangeListener(_selDependencyPropChangeLsnr);

        // Update DependenciesTreeView.Selection
        _dependenciesTreeView.setSelItem(buildDependency);
        resetLater();
    }

    /**
     * Called when selected dependency does prop change.
     */
    private void handleSelDependencyPropChange(PropChange propChange)
    {
        BuildDependency buildDependency = (BuildDependency) propChange.getSource();
        _dependenciesTreeView.updateItem(buildDependency);
        resetLater();
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        // Have Backspace and Delete remove selected Jar path
        addKeyActionHandler("DeleteAction", "DELETE");
        addKeyActionHandler("BackSpaceAction", "BACK_SPACE");

        // Configure CompileReleaseComboBox
        getView("CompileReleaseComboBox", ComboBox.class).setItems(JAVA_VERSIONS);

        // Move RepositoryButtonsView from top level to RepositoriesTitleView
        View repositoryButtonsView = getView("RepositoryButtonsView", View.class);
        TitleView repositoriesTitleView = getView("RepositoriesTitleView", TitleView.class);
        repositoriesTitleView.getLabel().setGraphicAfter(repositoryButtonsView);
        ViewUtils.bindExpr(repositoriesTitleView, TitleView.Expanded_Prop, repositoryButtonsView, View.Visible_Prop, TitleView.Expanded_Prop);
        repositoryButtonsView.setVisible(false);

        // Configure RepositoriesListView
        BuildFile buildFile = getBuildFile();
        _repositoriesListView = getView("RepositoriesListView", ListView.class);
        _repositoriesListView.setItemTextFunction(MavenRepository::getName);

        // Configure RepositoriesListView items
        List<MavenRepository> repositories = buildFile.getRepositories();
        _repositoriesListView.setItems(repositories);
        buildFile.addPropChangeListener(this::handleBuildFileRepositoryChange, BuildFile.Repository_Prop);

        // Configure DependenciesTreeView
        _dependenciesTreeView = getView("DependenciesTreeView", TreeView.class);
        _dependenciesTreeView.setRowHeight(26);
        _dependenciesTreeView.setResolver(new DependenciesTreeResolver());
        _dependenciesTreeView.setCellConfigure(this::configureDependenciesListCell);
        _dependenciesTreeView.addEventHandler(this::handleDependenciesListDragEvent, DragEvents);

        // Set DependenciesTreeView items
        List<BuildDependency> dependencies = buildFile.getDependencies();
        _dependenciesTreeView.setItems(dependencies);
        buildFile.addPropChangeListener(this::handleBuildFileDependencyChange, BuildFile.Dependency_Prop);

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

        // Update SourcePathText, BuildPathText
        setViewValue("SourcePathText", buildFile.getSourcePath());
        setViewValue("BuildPathText", buildFile.getBuildPath());

        // Update CompileReleaseComboBox
        setViewValue("CompileReleaseComboBox", buildFile.getCompileRelease());

        // Update IncludeSnapKitRuntimeCheckBox, IncludeSnapChartsRuntimeCheckBox, IncludeJavaFXCheckBox
        setViewValue("IncludeSnapKitRuntimeCheckBox", buildFile.isIncludeSnapKitRuntime());
        setViewValue("IncludeSnapChartsRuntimeCheckBox", buildFile.isIncludeSnapChartsRuntime());
        setViewValue("IncludeJavaFXCheckBox", buildFile.isIncludeJavaFX());
        setViewEnabled("IncludeJavaFXCheckBox", SnapEnv.isDesktop);

        // Update RemoveRepositoryButton
        MavenRepository selRepository = getSelRepository();
        setViewEnabled("RemoveRepositoryButton", selRepository != null);

        // Update RemoveDependencyButton, ExpandDependenciesButton, CollapseDependenciesButton
        BuildDependency selDependency = getSelDependency();
        setViewDisabled("RemoveDependencyButton", selDependency == null);
        boolean hasChildren = ListUtils.hasMatch(_dependenciesTreeView.getItems(), item -> _dependenciesTreeView.isItemParent(item));
        setViewVisible("ExpandDependenciesButton", hasChildren);
        setViewVisible("CollapseDependenciesButton", hasChildren);

        // Get selected dependency
        setViewVisible("DependencyTypeBox", selDependency != null);
        if (selDependency != null)
            setViewSelItem("DependencyTypeComboBox", selDependency.getType());

        // Update MavenDependencyBox
        boolean isMavenDependency = selDependency instanceof MavenDependency;
        setViewVisible("MavenDependencyBox", isMavenDependency);
        if (isMavenDependency) {

            // Update MavenIdText, GroupText, PackageNameText, VersionText, ClassifierText
            MavenDependency mavenDependency = (MavenDependency) selDependency;
            setViewValue("MavenIdText", mavenDependency.getId());
            setViewValue("GroupText", mavenDependency.getGroup());
            setViewValue("PackageNameText", mavenDependency.getName());
            setViewValue("VersionText", mavenDependency.getVersion());
            setViewValue("ClassifierText", mavenDependency.getClassifier());

            // Update StatusText, StatusProgressBar, ShowButton, ReloadButton, ClassPathsText
            String status = mavenDependency.getStatus();
            String error = mavenDependency.getError();
            setViewValue("StatusText", status);
            getView("StatusText", Label.class).setTextColor(error != null ? Color.RED : Color.BLACK);
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

        // Update MainClassNameText, EnableCompilePreviewCheckBox
        setViewValue("MainClassNameText", buildFile.getMainClassName());
        setViewValue("EnableCompilePreviewCheckBox", buildFile.isEnableCompilePreview());
    }

    /**
     * Respond to UI changes.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        BuildFile buildFile = getBuildFile();

        switch (anEvent.getName()) {

            // Update SourcePathText, BuildPathText
            case "SourcePathText" -> buildFile.setSourcePath(anEvent.getStringValue());
            case "BuildPathText" -> buildFile.setBuildPath(anEvent.getStringValue());

            // Update CompileReleaseComboBox
            case "CompileReleaseComboBox" -> buildFile.setCompileRelease(anEvent.getIntValue());

            // Update IncludeSnapKitRuntimeCheckBox, IncludeSnapChartsRuntimeCheckBox, IncludeJavaFXCheckBox
            case "IncludeSnapKitRuntimeCheckBox" -> buildFile.setIncludeSnapKitRuntime(anEvent.getBoolValue());
            case "IncludeSnapChartsRuntimeCheckBox" -> buildFile.setIncludeSnapChartsRuntime(anEvent.getBoolValue());
            case "IncludeJavaFXCheckBox" -> buildFile.setIncludeJavaFX(anEvent.getBoolValue());

            // Handle AddRepositoryButton, RemoveRepositoryButton
            case "AddRepositoryButton" -> showAddRepositoryPanel();
            case "RemoveRepositoryButton" -> removeSelectedRepository();

            // Handle AddDependencyButton, RemoveDependencyButton, ExpandDependenciesButton, CollapseDependenciesButton
            case "AddDependencyButton" -> showAddDependencyPanel();
            case "RemoveDependencyButton" -> removeSelectedDependency();
            case "ExpandDependenciesButton" -> _dependenciesTreeView.expandAll();
            case "CollapseDependenciesButton" -> _dependenciesTreeView.collapseAll();

            // Handle RepositoriesListView, DependenciesTreeView
            case "RepositoriesListView" -> setSelRepository(_repositoriesListView.getSelItem());
            case "DependenciesTreeView" -> setSelDependency(_dependenciesTreeView.getSelItem());

            // Handle DeleteAction
            case "DeleteAction", "BackSpaceAction" -> {
                if (_dependenciesTreeView.isFocused()) {
                    BuildDependency dependency = _dependenciesTreeView.getSelItem();
                    buildFile.removeDependency(dependency);
                }
            }

            // Handle DependencyTypeComboBox
            case "DependencyTypeComboBox" -> changeSelectedDependencyType();

            // Handle MainClassNameText, EnableCompilePreviewCheckBox
            case "MainClassNameText" -> buildFile.setMainClassName(anEvent.getStringValue());
            case "EnableCompilePreviewCheckBox" -> buildFile.setEnableCompilePreview(anEvent.getBoolValue());

            // Handle dependency
            default -> respondDependencyUI(anEvent);
        }
    }

    /**
     * Shows a panel to add a repository.
     */
    private void showAddRepositoryPanel()
    {
        String repoName = DialogBox.showInputDialog(getUI(), "Add Repository", "Enter Repository Name", null);
        if (repoName == null)
            return;

        // Get repo for name and add
        MavenRepository newRepository = MavenRepository.getRepoForName(repoName);
        BuildFile buildFile = getBuildFile();
        buildFile.addRepository(newRepository);
    }

    /**
     * Removes the selected repository.
     */
    private void removeSelectedRepository()
    {
        // Ask user to confirm (just return if cancelled)
        String msg = "Are you sure you want to remove selected repository?";
        if (!DialogBox.showConfirmDialog(getUI(), "Remove Repository", msg))
            return;

        // Remove selected
        BuildFile buildFile = getBuildFile();
        MavenRepository selRepository = getSelRepository();
        buildFile.removeRepository(selRepository);
    }

    /**
     * Respond to UI changes for dependency.
     */
    private void respondDependencyUI(ViewEvent anEvent)
    {
        // Handle MavenIdText, GroupText, PackageNameText, VersionText, ClassifierText, ShowButton, ReloadButton
        BuildDependency selDependency = getSelDependency();
        if (selDependency instanceof MavenDependency mavenDependency) {
            switch (anEvent.getName()) {
                case "MavenIdText" -> mavenDependency.setId(anEvent.getStringValue());
                case "GroupText" -> mavenDependency.setGroup(anEvent.getStringValue());
                case "PackageNameText" -> mavenDependency.setName(anEvent.getStringValue());
                case "VersionText" -> mavenDependency.setVersion(anEvent.getStringValue());
                case "ClassifierText" -> mavenDependency.setClassifier(anEvent.getStringValue());
                case "ShowButton" -> showMavenDependencyInFinder(mavenDependency);
                case "ReloadButton" -> mavenDependency.reloadPackageFiles();
            }

            // If not loaded, trigger load
            if (!mavenDependency.isLoaded()) {
                mavenDependency.preloadPackageFiles();
                runDelayed(this::resetLater, 1000); // Really need to have 'handleDependencyLoadedChange'
            }
        }

        // Handle JarFileDependency: JarPathText
        else if (selDependency instanceof BuildDependency.JarFileDependency jarFileDependency) {
            if (anEvent.equals("JarPathText"))
                jarFileDependency.setJarPath(anEvent.getStringValue());
        }

        // Handle ProjectDependency: ProjectNameText
        else if (selDependency instanceof BuildDependency.ProjectDependency projectDependency) {
            if (anEvent.equals("ProjectNameText"))
                projectDependency.setProjectName(anEvent.getStringValue());
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
        BuildDependency selDependency = getSelDependency();
        buildFile.removeDependency(selDependency);
    }

    /**
     * Changes selected dependency type.
     */
    private void changeSelectedDependencyType()
    {
        // Remove selected dependency
        BuildFile buildFile = getBuildFile();
        int index = _dependenciesTreeView.getSelIndex();
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
        if (buildDependency instanceof MavenDependency mavenDependency && mavenDependency.getParent() != null) {
            aCell.setTextColor(Color.DARKGRAY);
            label.setTextColor(Color.DARKGRAY);
        }
    }

    /**
     * Shows the given maven dependency in finder/explorer.
     */
    private void showMavenDependencyInFinder(MavenDependency mavenDependency)
    {
        WebFile mavenDir = mavenDependency.getLocalMavenDir();
        if (mavenDir != null)
            GFXEnv.getEnv().openFile(mavenDir);
    }

    /**
     * Called when BuildFile changes repository.
     */
    private void handleBuildFileRepositoryChange(PropChange propChange)
    {
        BuildFile buildFile = getBuildFile();

        // If BuildFile adds repository, update list
        if (propChange.getNewValue() != null) {
            MavenRepository repository = (MavenRepository) propChange.getNewValue();
            _repositoriesListView.setItems(buildFile.getRepositories());
            setSelRepository(repository);
        }

        // If BuildFile removes dependency, remove from list
        else {
            MavenRepository repository = (MavenRepository) propChange.getOldValue();
            _repositoriesListView.removeItemAndUpdateSel(repository);
            setSelRepository(_repositoriesListView.getSelItem());
        }
    }

    /**
     * Called when BuildFile changes dependency.
     */
    private void handleBuildFileDependencyChange(PropChange propChange)
    {
        BuildFile buildFile = getBuildFile();

        // If BuildFile adds dependency, update list
        if (propChange.getNewValue() != null) {
            BuildDependency dependency = (BuildDependency) propChange.getNewValue();
            _dependenciesTreeView.setItems(buildFile.getDependencies());
            setSelDependency(dependency);
        }

        // If BuildFile removes dependency, remove from list
        else {
            BuildDependency dependency = (BuildDependency) propChange.getOldValue();
            _dependenciesTreeView.removeItemAndUpdateSel(dependency);
            setSelDependency(_dependenciesTreeView.getSelItem());
        }
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

    /**
     * A tree resolver for DependenciesTreeView.
     */
    private static class DependenciesTreeResolver extends TreeResolver<BuildDependency> {

        @Override
        public BuildDependency getParent(BuildDependency anItem)
        {
            if (anItem instanceof MavenDependency mavenDependency)
                return mavenDependency.getParent();
            return null;
        }

        @Override
        public boolean isParent(BuildDependency anItem)
        {
            return anItem instanceof MavenDependency mvnDependency && !mvnDependency.isRedundant() &&
                !mvnDependency.getDependencies().isEmpty();
        }

        @Override
        public List<BuildDependency> getChildren(BuildDependency aParent)
        {
            if (aParent instanceof MavenDependency mvnDependency)
                return (List<BuildDependency>) (List<?>) mvnDependency.getDependencies();
            return null;
        }
    }
}