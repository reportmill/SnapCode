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

    // The selected build dependency
    private BuildDependency _selDependency;

    // A prop change listener for selected dependency
    private PropChangeListener _selDependencyDidPropChangeLsnr = this::selDependencyDidPropChange;

    // Dependencies ListView
    private ListView<BuildDependency> _dependenciesListView;

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
            _selDependency.removePropChangeListener(_selDependencyDidPropChangeLsnr);
        _selDependency = buildDependency;
        if (_selDependency != null)
            _selDependency.addPropChangeListener(_selDependencyDidPropChangeLsnr);

        // Update DependenciesListView.Selection
        _dependenciesListView.setSelItem(buildDependency);
        resetLater();
    }

    /**
     * Called when selected dependency does prop change.
     */
    private void selDependencyDidPropChange(PropChange aPC)
    {
        BuildDependency buildDependency = (BuildDependency) aPC.getSource();
        _dependenciesListView.updateItem(buildDependency);
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

        // Configure SourceCompatibilityComboBox, TargetCompatibilityComboBox
        getView("SourceCompatibilityComboBox", ComboBox.class).setItems(JAVA_VERSIONS);
        getView("TargetCompatibilityComboBox", ComboBox.class).setItems(JAVA_VERSIONS);

        // Configure DependenciesListView
        _dependenciesListView = getView("DependenciesListView", ListView.class);
        _dependenciesListView.setRowHeight(26);
        _dependenciesListView.setCellConfigure(this::configureDependenciesListCell);
        _dependenciesListView.addEventHandler(this::handleDependenciesListDragEvent, DragEvents);

        // Set DependenciesListView items
        BuildFile buildFile = getBuildFile();
        BuildDependency[] dependencies = buildFile.getDependencies();
        _dependenciesListView.setItems(dependencies);
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

        // Update SourcePathText, BuildPathText
        setViewValue("SourcePathText", buildFile.getSourcePath());
        setViewValue("BuildPathText", buildFile.getBuildPath());

        // Update SourceCompatibilityComboBox, TargetCompatibilityComboBox
        setViewValue("SourceCompatibilityComboBox", buildFile.getSourceCompatibility());
        setViewValue("TargetCompatibilityComboBox", buildFile.getTargetCompatibility());

        // Update IncludeSnapKitRuntimeCheckBox, IncludeSnapChartsRuntimeCheckBox
        setViewValue("IncludeSnapKitRuntimeCheckBox", buildFile.isIncludeSnapKitRuntime());
        setViewValue("IncludeSnapChartsRuntimeCheckBox", buildFile.isIncludeSnapChartsRuntime());

        // Update RemoveDependencyButton
        BuildDependency selDependency = getSelDependency();
        setViewDisabled("RemoveDependencyButton", selDependency == null);

        // Get selected dependency
        setViewVisible("DependencyTypeBox", selDependency != null);
        if (selDependency != null)
            setViewSelItem("DependencyTypeComboBox", selDependency.getType());

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

        // Update MainClassNameText
        setViewValue("MainClassNameText", buildFile.getMainClassName());
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
            case "SourcePathText": buildFile.setSourcePath(anEvent.getStringValue()); break;
            case "BuildPathText": buildFile.setBuildPath(anEvent.getStringValue()); break;

            // Update SourceCompatibilityComboBox, TargetCompatibilityComboBox
            case "SourceCompatibilityComboBox": buildFile.setSourceCompatibility(anEvent.getIntValue()); break;
            case "TargetCompatibilityComboBox": buildFile.setTargetCompatibility(anEvent.getIntValue()); break;

            // Update IncludeSnapKitRuntimeCheckBox, IncludeSnapChartsRuntimeCheckBox
            case "IncludeSnapKitRuntimeCheckBox": buildFile.setIncludeSnapKitRuntime(anEvent.getBoolValue()); break;
            case "IncludeSnapChartsRuntimeCheckBox": buildFile.setIncludeSnapChartsRuntime(anEvent.getBoolValue()); break;

            // Handle AddDependencyButton, RemoveDependencyButton
            case "AddDependencyButton": showAddDependencyPanel(); break;
            case "RemoveDependencyButton": removeSelectedDependency(); break;

            // Handle DependenciesList
            case "DependenciesListView":
                BuildDependency buildDependency = _dependenciesListView.getSelItem();
                setSelDependency(buildDependency);
                break;

            // Handle DeleteAction
            case "DeleteAction": case "BackSpaceAction":
                if (getView("DependenciesListView").isFocused()) {
                    BuildDependency dependency = (BuildDependency) getViewSelItem("DependenciesListView");
                    buildFile.removeDependency(dependency);
                }
                break;

            // Handle DependencyTypeComboBox
            case "DependencyTypeComboBox": changeSelectedDependencyType(); break;

            // Handle MainClassNameText
            case "MainClassNameText": buildFile.setMainClassName(anEvent.getStringValue()); break;

            // Handle dependency
            default: respondDependencyUI(anEvent); break;
        }
    }

    /**
     * Respond to UI changes for dependency.
     */
    private void respondDependencyUI(ViewEvent anEvent)
    {
        // Handle MavenIdText, GroupText, PackageNameText, VersionText, RepositoryURLText, ShowButton, ReloadButton
        BuildDependency selDependency = getSelDependency();
        if (selDependency instanceof MavenDependency) {
            MavenDependency mavenDependency = (MavenDependency) selDependency;
            switch (anEvent.getName()) {
                case "MavenIdText": mavenDependency.setId(anEvent.getStringValue()); break;
                case "GroupText": mavenDependency.setGroup(anEvent.getStringValue()); break;
                case "PackageNameText": mavenDependency.setName(anEvent.getStringValue()); break;
                case "VersionText": mavenDependency.setVersion(anEvent.getStringValue()); break;
                case "RepositoryURLText": mavenDependency.setRepositoryURL(anEvent.getStringValue()); break;
                case "ShowButton": showMavenDependencyInFinder(mavenDependency); break;
                case "ReloadButton": mavenDependency.loadPackageFiles(); break;
            }
        }

        // Handle JarFileDependency: JarPathText
        else if (selDependency instanceof BuildDependency.JarFileDependency) {
            BuildDependency.JarFileDependency jarFileDependency = (BuildDependency.JarFileDependency) selDependency;
            if (anEvent.equals("JarPathText"))
                jarFileDependency.setJarPath(anEvent.getStringValue());
        }

        // Handle ProjectDependency: ProjectNameText
        else if (selDependency instanceof BuildDependency.ProjectDependency) {
            BuildDependency.ProjectDependency projectDependency = (BuildDependency.ProjectDependency) selDependency;
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
        int index = _dependenciesListView.getSelIndex();
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
            _dependenciesListView.setItems(buildFile.getDependencies());
            setSelDependency(dependency);
        }

        // If BuildFile removes dependency, remove from list
        else {
            BuildDependency dependency = (BuildDependency) aPC.getOldValue();
            _dependenciesListView.removeItemAndUpdateSel(dependency);
            setSelDependency(_dependenciesListView.getSelItem());
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
}