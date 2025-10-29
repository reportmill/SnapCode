package snapcode.app;
import snap.util.*;
import snap.viewx.DialogBox;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.javatext.JavaTextArea;
import snapcode.project.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * This class imports a greenfoot gfar file.
 */
public class GreenImport {

    /**
     * Show open panel.
     */
    public static void showGreenfootPanel(WorkspacePane workspacePane)
    {
        // Show dialog box for scenario id
        String title = "Open Greenfoot Scenario";
        String msg = "Enter scenario id:";
        String scenarioIdStr = DialogBox.showInputDialog(workspacePane.getUI(), title, msg, "192");
        int scenarioId = scenarioIdStr != null ? Convert.intValue(scenarioIdStr) : 0;
        if (scenarioId == 0)
            return;

        // Open new project for scenario id
        openProjectForGreenfootScenarioId(workspacePane, scenarioId);
    }

    /**
     * Opens a new project for Greenfoot scenario id.
     */
    public static void openProjectForGreenfootScenarioId(WorkspacePane workspacePane, int scenarioId)
    {
        // Get greenfoot archive file for scenario id
        File greenfootArchiveFile;
        try { greenfootArchiveFile = getGreenfootArchiveFileForScenarioId(scenarioId); }
        catch (Exception e) {
            complain("Can't download scenario: " + e);
            return;
        }

        // Get URL for archive file and open
        WebURL gfarFileUrl = WebURL.getUrl(greenfootArchiveFile);
        openProjectForGreenfootArchiveUrl(workspacePane, gfarFileUrl);
    }

    /**
     * Opens a new project for Greenfoot archive file URL.
     */
    public static void openProjectForGreenfootArchiveUrl(WorkspacePane workspacePane, WebURL archiveUrl)
    {
        // Create download archive and open project task
        Workspace workspace = workspacePane.getWorkspace();
        TaskRunner<Project> openProjectTask = (TaskRunner<Project>) workspace.getTaskManager().createTask();
        ActivityMonitor activityMonitor = openProjectTask.getMonitor();
        activityMonitor.setTitle("Opening Greenfoot");

        // Configure function
        openProjectTask.setTaskFunction(() -> openProjectForGreenfootArchiveUrlImpl(workspacePane, archiveUrl, activityMonitor));

        // Start task
        workspace.getBuilder().setAutoBuildEnabled(false);
        openProjectTask.setOnSuccess(proj -> handleOpenProjectForGreenfootArchiveUrlSuccess(workspacePane, proj));
        openProjectTask.setOnFinished(() -> handleOpenProjectForGreenfootArchiveUrlFinished(workspacePane));
        openProjectTask.start();
    }

    /**
     * Opens a new project for Greenfoot archive file URL.
     */
    private static Project openProjectForGreenfootArchiveUrlImpl(WorkspacePane workspacePane, WebURL archiveUrl, ActivityMonitor activityMonitor)
    {
        // Init activity monitor
        activityMonitor.startForTaskCount(1);
        activityMonitor.beginTask("Download project", 2);
        activityMonitor.updateTask(1);

        // Get greenfoot dir for archive URL
        WebFile greenfootDir = getGreenfootDirForArchiveUrl(archiveUrl);

        // Get scenario name
        String scenarioName = greenfootDir.getSimpleName().replace(' ', '_');

        // Get project site - if already exists and is project, open and return
        Workspace workspace = workspacePane.getWorkspace();
        WebSite projectSite = SnapCodeUtils.getSnapCodeProjectSiteForName(scenarioName);
        if (projectSite.getExists() && ProjectUtils.isProjectDir(projectSite.getRootDir())) {
            Project project = workspace.openProjectForSite(projectSite);
            activityMonitor.endTask();
            return project;
        }

        // Register remote source
        VersionControlUtils.setRemoteSiteUrl(projectSite, archiveUrl);

        // Create new project and get src dir
        Project project = new Project(workspace, projectSite);

        // Get build file and configure
        BuildFile buildFile = project.getBuildFile();
        buildFile.setIncludeSnapKitRuntime(true);
        buildFile.setIncludeGreenfootRuntime(true);
        buildFile.setMainClassName("Main");
        buildFile.writeFile();

        // Copy all non-class files from greenfoot project dir to new project src dir
        WebFile projectSourceDir = project.getSourceDir();
        copyDirFileFilesToDir(greenfootDir, projectSourceDir);

        // Create Main class that launches world.lastInstantiated prop
        createMainClassForGreenfootProject(project);

        // End task
        activityMonitor.endTask();

        // Return
        return project;
    }

    /**
     * Called when open greenfoot project has success.
     */
    private static void handleOpenProjectForGreenfootArchiveUrlSuccess(WorkspacePane workspacePane, Project project)
    {
        // Open project
        Workspace workspace = workspacePane.getWorkspace();
        workspace.openProject(project);

        // Show greenfoot button and enable Greenfoot scope boxes
        workspacePane.getToolBar().showGreenfootButton();
        JavaTextArea.setShowScopeBoxes(true);
    }

    /**
     * Called when open greenfoot project has finished.
     */
    private static void handleOpenProjectForGreenfootArchiveUrlFinished(WorkspacePane workspacePane)
    {
        Workspace workspace = workspacePane.getWorkspace();
        WorkspaceBuilder workspaceBuilder = workspace.getBuilder();
        workspaceBuilder.setAutoBuildEnabled(true);
        workspaceBuilder.buildWorkspaceLater();
    }

    /**
     * Returns a greenfoot dir for given archive file.
     */
    private static WebFile getGreenfootDirForArchiveUrl(WebURL greenfootArchiveURL)
    {
        // Get greenfoot archive file as zip site
        WebSite gfarSite = greenfootArchiveURL.getAsSite();

        // Get greenfoot archive file site root dir files for greenfoot archive
        WebFile gfarRootDir = gfarSite.getRootDir();
        List<WebFile> gfarRootFiles = gfarRootDir.getFiles();

        // Return the first file in top level of greenfoot archive zip file that starts with letter
        return ListUtils.findMatch(gfarRootFiles, file -> Character.isLetter(file.getName().charAt(0)));
    }

    /**
     * Returns a greenfoot archive file for given scenario id.
     */
    private static File getGreenfootArchiveFileForScenarioId(int gfId) throws IOException
    {
        // Get URL address for scenario
        String scenarioUrlAddr = "https://www.greenfoot.org/scenarios/" + gfId + "/get_gfar";
        if (SnapEnv.isWebVM) {
            //String CORS_PROXY_SERVER = "https://corsproxy.io/?url=";
            String CORS_PROXY_SERVER = "https://snap-proxy-4b.jeff-b76.workers.dev/?url=";
            scenarioUrlAddr = CORS_PROXY_SERVER + scenarioUrlAddr;
        }

        // Get URL for scenario
        URL scenarioURL = new URL(scenarioUrlAddr);

        // Get local file (delete if already there)
        File greenfootArchiveFile = FileUtils.getTempFile("GreenfootArchiveFile.zip");
        if (greenfootArchiveFile.exists())
            greenfootArchiveFile.delete();

        // Fetch url to local file
        URLUtils.getLocalFile(scenarioURL, greenfootArchiveFile);

        // Return
        return greenfootArchiveFile;
    }

    /**
     * Creates a main class to launch app.
     */
    private static void createMainClassForGreenfootProject(Project project)
    {
        // Get main world name
        WebFile projectSourceDir = project.getSourceDir();
        String mainWorldName = getMainWorldName(projectSourceDir);
        if (mainWorldName == null) {
            complain("Can't find main world name");
            return;
        }

        // Get Main.java file
        String mainJavaFilePath = projectSourceDir.getDirPath() + "Main.java";
        WebFile mainJavaFile = projectSourceDir.getSite().createFileForPath(mainJavaFilePath, false);

        // Get java string, set in main file and save
        String javaString = getJavaMainClassString(mainWorldName);
        mainJavaFile.setText(javaString);
        mainJavaFile.save();
    }

    /**
     * Copies files in given directory file to given directory.
     */
    private static void copyDirFileFilesToDir(WebFile dirFile, WebFile toDir)
    {
        List<WebFile> dirFiles = dirFile.getFiles();
        List<WebFile> nonClassFiles = ListUtils.filter(dirFiles, file -> !isIgnoreFile(file));
        for (WebFile file : nonClassFiles)
            copyFileToDir(file, toDir);
    }

    /**
     * Copies given file to given directory (recursively if given file is dir).
     */
    private static void copyFileToDir(WebFile aFile, WebFile toDir)
    {
        // If file should be top level file, copy there instead
        if (isTopLevelFile(aFile))
            toDir = toDir.getParent();

        // Get ToFile
        String toFilePath = toDir.getDirPath() + aFile.getName();
        WebFile toFile = toDir.getSite().createFileForPath(toFilePath, aFile.isDir());

        // If plain file, copy bytes and save
        if (toFile.isFile())
            copyFileToFile(aFile, toFile);

        // If file is dir, forward to copyDirFile
        else copyDirFileFilesToDir(aFile, toFile);
    }

    /**
     * Copies given file to given file.
     */
    private static void copyFileToFile(WebFile aFile, WebFile toFile)
    {
        // If Java file, fix text for older greenfoot files that reference java.awt.Color/Font instead of greenfoot.Color/Font
        if (aFile.getFileType().equals("java")) {
            String text = aFile.getText();
            if (text.contains("java.awt.")) {
                text = text.replace("java.awt.Font;", "greenfoot.Font;");
                text = text.replace("java.awt.Color;", "greenfoot.Color;");
            }
            toFile.setText(text);
        }

        // Otherwise just copy bytes
        else {
            byte[] bytes = aFile.getBytes();
            toFile.setBytes(bytes);
        }

        // Save file
        toFile.save();
    }

    /**
     * Creates a new file for use with showNewFilePanel method.
     */
    public static String getJavaMainClassString(String className)
    {
        // Append Comment
        return "\n/**\n * The main class for this greenfoot app.\n */\n" +

            // Append class declaration: "public class <File-Name> {"
            "public class " + "Main" + " {\n\n" +

            // Append standard main implementation
            "    /**\n" +
            "     * Standard main implementation: Show main world.\n" +
            "     */\n" +
            "    public static void main(String[] args)\n" +
            "    {\n" +
            "        greenfoot.Greenfoot.showWorldForClass(" + className + ".class);\n" +
            "    }\n" +

            // Append close
            "}";
    }

    /**
     * Returns the 'world.lastInstantiated' property.
     */
    private static String getMainWorldName(WebFile projectSrcDir)
    {
        // Get project file
        WebFile file = projectSrcDir.getFileForName("project.greenfoot");
        if (file == null)
            return null;

        // Get file lines (just return if null)
        String text = file.getText();
        String[] lines = text != null ? text.split("\\n") : null;
        if (lines == null)
            return null;

        // Iterate over lines and return world name
        for (String line : lines) {
            String[] parts = line.split("=");
            if (parts.length > 1) {
                String key = parts[0].trim();
                if (key.equals("world.lastInstantiated"))
                    return parts[1].trim();
            }
        }

        // Return
        return null;
    }

    /**
     * Returns whether given file should be ignored.
     */
    private static boolean isIgnoreFile(WebFile aFile)
    {
        String fileType = aFile.getFileType();
        if (fileType.equals("class") || fileType.equals("ctxt"))
            return true;
        return false;
    }

    /**
     * Returns whether given file should be in top level.
     */
    private static boolean isTopLevelFile(WebFile aFile)
    {
        if (StringUtils.equalsIC(aFile.getSimpleName(), "readme"))
            return true;
        return false;
    }

    /**
     * Complains.
     */
    private static void complain(String aStr)
    {
        System.out.println(aStr);
    }
}
