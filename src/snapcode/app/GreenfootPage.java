package snapcode.app;
import greenfoot.*;
import greenfoot.GreenfootProject;
import snap.util.ArrayUtils;
import snap.view.View;
import snap.web.WebFile;
import snapcode.project.Project;
import snapcode.project.Workspace;
import snapcode.webbrowser.WebBrowser;
import snapcode.webbrowser.WebPage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class is a page to show greenfoot.project files.
 */
public class GreenfootPage extends WebPage {

    // The greenfoot player pane
    private PlayerPane _playerPane;

    // The project
    private Project _project;

    // The greenfoot project
    private GreenfootProject _greenfootProject;

    /**
     * Constructor.
     */
    public GreenfootPage()
    {
        super();
    }

    /**
     * Returns the WorkspacePane.
     */
    private WorkspacePane getWorkspacePane()
    {
        WebBrowser browser = getBrowser();
        return browser.getOwner(WorkspacePane.class);
    }

    /**
     * Returns the WorkspacePane.
     */
    private Workspace getWorkspace()
    {
        WorkspacePane workspacePane = getWorkspacePane();
        return workspacePane.getWorkspace();
    }

    /**
     * Returns the Greenfoot Project.
     */
    public GreenfootProject getGreenfootProject()
    {
        if (_greenfootProject != null) return _greenfootProject;

        // Find first (any) greenfoot project in workspace
        Workspace workspace = getWorkspace();
        Project[] projects = workspace.getProjects();

        // Find project
        for (Project project : projects) {
            _project = project;
            _greenfootProject = getGreenfootProjectForProject(project);
            if (_greenfootProject != null)
                break;
        }

        // Set
        return _greenfootProject;
    }

    /**
     * Returns the root class node.
     */
    private ClassNode getRootClassNode()
    {
        // Create RootClassNode for Object
        ClassNode rootClassNode = new ClassNode(Object.class, null);

        // Iterate over all source files and add node for each
        WebFile[] sourceFiles = getAllSourceFiles();
        for (WebFile sourceFile : sourceFiles) {
            Project project = Project.getProjectForFile(sourceFile);
            Class<?> sourceFileClass = project.getClassForFile(sourceFile);
            if (sourceFileClass != null)
                rootClassNode.addChildNodeForClassAndFile(sourceFileClass, sourceFile);
        }

        // Return
        return rootClassNode;
    }

    /**
     * Resets the root class node.
     */
    private void resetRootClassNode()
    {
        GreenfootProject greenfootProject = getGreenfootProject();
        if (greenfootProject == null)
            return;

        Greenfoot.env().setGreenfootProject(greenfootProject);

        // Set ClassNodes
        ClassNode rootClassNode = getRootClassNode();
        greenfootProject.setRootClassNode(rootClassNode);
    }

    /**
     * Returns all source files in workspace projects.
     */
    private WebFile[] getAllSourceFiles()
    {
        Workspace workspace = getWorkspace();
        Project[] projects = workspace.getProjects();
        WebFile[] sourceDirs = ArrayUtils.map(projects, Project::getSourceDir, WebFile.class);
        List<WebFile> sourceFiles = new ArrayList<>();
        Stream.of(sourceDirs).forEach(dir -> findSourceFilesForDir(dir, sourceFiles));
        return sourceFiles.toArray(new WebFile[0]);
    }

    @Override
    protected View createUI()
    {
        GreenfootProject greenfootProject = getGreenfootProject();
        Greenfoot.env().setGreenfootProject(greenfootProject);

        String worldClassName = greenfootProject.getProperty("world.lastInstantiated");
        if (worldClassName != null) {
            WebFile worldClassFile = _project.getJavaFileForClassName(worldClassName);
            Class<? extends World> worldClass = (Class<? extends World>) _project.getClassForFile(worldClassFile);
            Greenfoot.env().setWorldForClass(worldClass);
        }

        _playerPane = Greenfoot.env().getPlayerPane();
        return _playerPane.getUI();
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        resetRootClassNode();
    }

    /**
     * Finds source files in given directory and adds to given list (recursively).
     */
    private static void findSourceFilesForDir(WebFile aDir, List<WebFile> theFiles)
    {
        WebFile[] dirFiles = aDir.getFiles();

        for (WebFile file : dirFiles) {
            if (file.getFileType().equals("java"))
                theFiles.add(file);
            else if (file.isDir())
                findSourceFilesForDir(file, theFiles);
        }
    }

    /**
     * Returns the greenfoot project for given project dir.
     */
    private static GreenfootProject getGreenfootProjectForProject(Project aProject)
    {
        WebFile projDir = aProject.getRootDir();
        return GreenfootProject.getGreenfootProjectForDir(projDir);
    }
}
