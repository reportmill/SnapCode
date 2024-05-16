package snapcode.app;
import snap.util.*;
import snap.viewx.DialogBox;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.project.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;

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
        String scenarioIdStr = DialogBox.showInputDialog(null, title, msg, "192");
        int scenarioId = scenarioIdStr != null ? Convert.intValue(scenarioIdStr) : 0;
        if (scenarioId == 0)
            return;

        // Open new project for scenario id
        openGreenfootScenario(workspacePane, scenarioId);
    }

    /**
     * Opens a new project for Greenfoot scenario id.
     */
    public static void openGreenfootScenario(WorkspacePane workspacePane, int gfId)
    {
        // Get greenfoot dir for scenario id
        WebFile greenfootDir = getGreenfootDirForScenarioId(gfId);
        if (greenfootDir == null) {
            complain("GreenImport.openGreenfootScenario: Can't find scenario root dir");
            return;
        }

        // Open greenfoot dir
        openGreenfootDir(workspacePane, greenfootDir);
    }

    /**
     * Opens a new project for Greenfoot scenario id.
     */
    public static void openGreenfootForArchiveFilePath(WorkspacePane workspacePane, String archiveFilePath)
    {
        // Get archive file as zip site
        WebURL archiveUrl = WebURL.getURL(archiveFilePath);
        assert (archiveUrl != null);

        // Return greenfoot dir for archive URL
        WebFile greenfootDir = getGreenfootDirForArchiveUrl(archiveUrl);

        // Open greenfoot dir
        openGreenfootDir(workspacePane, greenfootDir);
    }

    /**
     * Opens a new project for Greenfoot scenario id.
     */
    public static void openGreenfootDir(WorkspacePane workspacePane, WebFile greenfootDir)
    {
        // Make sure workspacePane exists
        if (workspacePane == null)
            workspacePane = WelcomePanel.getShared().openEmptyWorkspace();

        // Get scenario name
        String scenarioName = greenfootDir.getSimpleName().replace(' ', '_');

        // Create new project and get src dir
        Workspace workspace = workspacePane.getWorkspace();
        Project project = getTempProject(workspace, scenarioName);
        WebFile projectSourceDir = project.getSourceDir();

        // Copy all non-class files from greenfoot project dir to new project src dir
        copyDirFileFilesToDir(greenfootDir, projectSourceDir);

        // Create Main class that launches world.lastInstantiated prop
        createMainClassForGreenfootProject(project);
    }

    /**
     * Returns a greenfoot dir for given scenario id.
     */
    private static WebFile getGreenfootDirForScenarioId(int scenarioId)
    {
        // Get greenfoot archive file for scenario id
        File greenfootArchiveFile;
        try { greenfootArchiveFile = getGreenfootArchiveFileForScenarioId(scenarioId); }
        catch (Exception e) {
            complain("Can't download scenario: " + e);
            return null;
        }

        // Return greenfoot dir for archive file
        WebURL greenfootArchiveUrl = WebURL.getURL(greenfootArchiveFile);
        assert (greenfootArchiveUrl != null);
        return getGreenfootDirForArchiveUrl(greenfootArchiveUrl);
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
        WebFile[] gfarRootFiles = gfarRootDir.getFiles();

        // Return the first file in top level of greenfoot archive zip file that starts with letter
        return ArrayUtils.findMatch(gfarRootFiles, file -> Character.isLetter(file.getName().charAt(0)));
    }

    /**
     * Returns a greenfoot archive file for given scenario id.
     */
    private static File getGreenfootArchiveFileForScenarioId(int gfId) throws IOException
    {
        // Get URL address for scenario
        String scenarioUrlAddr = "https://www.greenfoot.org/scenarios/" + gfId + "/get_gfar";
        if (SnapUtils.isWebVM) {
            String CORS_PROXY_SERVER = "https://corsproxy.io/?";
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
     * Returns a temp project for given workspace.
     */
    private static Project getTempProject(Workspace workspace, String projectName)
    {
        // Get path to temp dir named TempProj
        String tempProjPath = FileUtils.getTempFile(projectName).getAbsolutePath();
        if (SnapUtils.isMac)
            tempProjPath = "/tmp/" + projectName;

        // Get URL and Site for TempProjPath
        WebURL tempProjURL = WebURL.getURL(tempProjPath);
        assert (tempProjURL != null);
        WebSite tempProjSite = tempProjURL.getAsSite();

        // Get project for site - create if missing
        Project tempProj = Project.getProjectForSite(tempProjSite);
        if (tempProj != null)
            return tempProj;

        // Delete site root directory
        WebFile tempProjRootDir = tempProjSite.getRootDir();
        if (tempProjRootDir.getExists())
            tempProjRootDir.delete();

        // Create new temp project
        tempProj = workspace.addProjectForSite(tempProjSite);

        // Get build file and configure
        BuildFile buildFile = tempProj.getBuildFile();
        buildFile.setIncludeSnapKitRuntime(true);
        buildFile.addDependency(new MavenDependency("com.reportmill:greenfoot:2024.04"));
        buildFile.setMainClassName("Main");
        buildFile.writeFile();

        // Return
        return tempProj;
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
        WebFile[] dirFiles = dirFile.getFiles();
        WebFile[] nonClassFiles = ArrayUtils.filter(dirFiles, file -> !file.getType().equals("class"));
        for (WebFile file : nonClassFiles)
            copyFileToDir(file, toDir);
    }

    /**
     * Copies given file to given directory (recursively if given file is dir).
     */
    private static void copyFileToDir(WebFile aFile, WebFile toDir)
    {
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
        if (aFile.getType().equals("java")) {
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

        // Get file text
        String text = file.getText();
        if (text != null && text.length() > 0) {
            String[] lines = text.split("\\n");
            for (String line : lines) {
                String[] parts = line.split("=");
                if (parts.length > 1) {
                    String key = parts[0].trim();
                    if (key.equals("world.lastInstantiated"))
                        return parts[1].trim();
                }
            }
        }

        // Return
        return null;
    }

    /**
     * Complains.
     */
    private static void complain(String aStr)
    {
        System.out.println(aStr);
    }
}
