package snapcode.webmcp;
import snap.text.TextAgent;
import snap.text.TextModel;
import snap.view.ViewUtils;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.app.WorkspacePane;
import snapcode.project.Project;
import snapcode.project.Workspace;
import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * Utility methods shared by SnapCode WebMCP tools.
 */
public class McpUtils {

    // Common description fragments
    public static final String PROJECT_ARG_DESC = "Project name. Optional: defaults to the project of the selected file, else the first open project.";
    public static final String PATH_ARG_DESC = "File path relative to the project root, e.g. '/src/Hello.java' or '/src/HelloUI.snp'. " +
            "A bare file name (e.g. 'Hello.java') is also accepted if it is unique in the project.";

    /**
     * Returns the workspace pane.
     */
    public static WorkspacePane getWorkspacePane()  { return McpToolRegistry.getWorkspacePane(); }

    /**
     * Returns the workspace.
     */
    public static Workspace getWorkspace()  { return getWorkspacePane().getWorkspace(); }

    /**
     * Returns the project for given name (or the default project if name is null).
     */
    public static Project getProject(String projectName)
    {
        WorkspacePane workspacePane = getWorkspacePane();
        List<Project> projects = workspacePane.getProjects();
        if (projects.isEmpty())
            throw new McpException("No project is open. Use create_project, open_project or open_sample first.");

        // Handle named project
        if (projectName != null && !projectName.isEmpty()) {
            for (Project proj : projects)
                if (proj.getName().equalsIgnoreCase(projectName))
                    return proj;
            throw new McpException("No open project named '" + projectName + "'. Open projects: " + getProjectNames());
        }

        // Use project of selected file, else root project
        WebFile selFile = workspacePane.getSelFile();
        Project selProj = selFile != null ? Project.getProjectForFile(selFile) : null;
        if (selProj != null && projects.contains(selProj))
            return selProj;
        return projects.get(0);
    }

    /**
     * Returns the names of the open projects.
     */
    public static List<String> getProjectNames()
    {
        List<String> names = new ArrayList<>();
        for (Project proj : getWorkspacePane().getProjects())
            names.add(proj.getName());
        return names;
    }

    /**
     * Normalizes a project-relative path.
     */
    public static String normalizePath(String aPath)
    {
        String path = aPath.trim().replace('\\', '/');
        if (!path.startsWith("/"))
            path = '/' + path;
        while (path.length() > 1 && path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        return path;
    }

    /**
     * Returns the file for given project name and path, or null if not found.
     */
    public static WebFile findFile(String projectName, String aPath)
    {
        Project proj = getProject(projectName);
        String path = normalizePath(aPath);
        WebSite site = proj.getSite();
        WebFile file = site.getFileForPath(path);
        if (file != null)
            return file;

        // Try source dir relative path (e.g. 'pkg/Foo.java')
        WebFile srcDir = proj.getSourceDir();
        if (srcDir != null && !path.startsWith(srcDir.getDirPath())) {
            file = site.getFileForPath(srcDir.getDirPath() + path.substring(1));
            if (file != null)
                return file;
        }

        // Try bare file name search
        if (path.lastIndexOf('/') == 0) {
            List<WebFile> matches = new ArrayList<>();
            findFilesForName(site.getRootDir(), path.substring(1), proj, matches);
            if (matches.size() == 1)
                return matches.get(0);
            if (matches.size() > 1)
                throw new McpException("File name '" + path.substring(1) + "' is ambiguous: " +
                        matches.stream().map(WebFile::getPath).toList());
        }
        return null;
    }

    /**
     * Returns the file for given project name and path (throws McpException if not found).
     */
    public static WebFile getFile(String projectName, String aPath)
    {
        WebFile file = findFile(projectName, aPath);
        if (file == null)
            throw new McpException("File not found: " + aPath + " (project: " + getProject(projectName).getName() + ")");
        return file;
    }

    /**
     * Returns the file for the 'project' and 'path' args, defaulting to the selected file if no path given.
     */
    public static WebFile getFileOrSelFile(McpArgs args)
    {
        String path = args.getString("path");
        if (path != null && !path.isEmpty())
            return getFile(args.getString("project"), path);
        WebFile selFile = getWorkspacePane().getSelFile();
        if (selFile == null || selFile.isDir())
            throw new McpException("No path given and no file is selected");
        return selFile;
    }

    /**
     * Finds files for given name.
     */
    private static void findFilesForName(WebFile aDir, String aName, Project aProj, List<WebFile> matches)
    {
        for (WebFile file : aDir.getFiles()) {
            if (file.isDir()) {
                if (file != aProj.getBuildDir() && !file.getName().startsWith("."))
                    findFilesForName(file, aName, aProj, matches);
            }
            else if (file.getName().equals(aName))
                matches.add(file);
        }
    }

    /**
     * Returns the current text of a file, including unsaved edits from an open editor or GUI builder.
     */
    public static String getFileText(WebFile aFile)
    {
        // A .snp layout open in the GUI builder: its current state lives in the builder, not the file
        if (aFile.getFileType().equals("snp")) {
            snapbuild.app.EditorPane editorPane = null;
            try { editorPane = BuilderMcpTools.getOpenEditorPane(aFile); }
            catch (McpException ignore) { }
            if (editorPane != null)
                return editorPane.getEditor().getContentXML().getString();
        }

        TextAgent textAgent = getExistingTextAgent(aFile);
        if (textAgent != null) {
            TextModel textModel = textAgent.getTextModel();
            return textModel.getString();
        }
        String text = aFile.getText();
        return text != null ? text : "";
    }

    /**
     * Sets the text of a file, going through the open editor's text model if there is one (keeps undo,
     * syntax highlighting and error markers in sync). Optionally saves.
     */
    public static void setFileText(WebFile aFile, String aString, boolean doSave)
    {
        TextAgent textAgent = getExistingTextAgent(aFile);
        if (textAgent != null) {
            TextModel textModel = textAgent.getTextModel();
            if (!textModel.getString().equals(aString))
                textModel.replaceChars(aString, 0, textModel.length());
        }
        else aFile.setText(aString);

        if (doSave)
            saveFile(aFile);
    }

    /**
     * Saves a file (throws McpException on failure).
     */
    public static void saveFile(WebFile aFile)
    {
        try { aFile.save(); }
        catch (Exception e) { throw new McpException("Failed to save " + aFile.getPath() + ": " + e.getMessage()); }
    }

    /**
     * Returns the TextAgent for a file if one already exists with a loaded text model.
     */
    private static TextAgent getExistingTextAgent(WebFile aFile)
    {
        Object agent = aFile.getMetadataForKey(TextAgent.class.getName());
        return agent instanceof TextAgent textAgent ? textAgent : null;
    }

    /**
     * Returns whether file is a text file.
     */
    public static boolean isTextFile(WebFile aFile)
    {
        String type = aFile.getFileType();
        return TEXT_TYPES.contains(type) || type.isEmpty();
    }

    // Text file types
    private static final Set<String> TEXT_TYPES = Set.of("java", "jepl", "jmd", "snp", "md", "txt", "xml", "json", "html",
            "htm", "css", "js", "csv", "properties", "gradle", "build", "classpath", "project", "yml", "yaml", "kt", "py",
            "sh", "gitignore", "mf", "ts", "svg", "tsv");

    /**
     * Waits until the condition is true (polling on the event thread), then runs onDone. Runs onTimeout if the
     * condition is not met within the timeout. An exception from either callback completes the result with an
     * error, so a tool call can never be left waiting forever.
     */
    public static void waitUntil(McpResult aResult, BooleanSupplier aCondition, int timeoutMillis, Runnable onDone, Runnable onTimeout)
    {
        long endTime = System.currentTimeMillis() + timeoutMillis;
        Runnable[] poll = new Runnable[1];
        poll[0] = () -> {
            boolean met;
            try { met = aCondition.getAsBoolean(); }
            catch (Throwable t) { met = false; }
            if (met)
                guard(aResult, onDone).run();
            else if (System.currentTimeMillis() > endTime)
                guard(aResult, onTimeout).run();
            else ViewUtils.runDelayed(poll[0], 150);
        };
        ViewUtils.runDelayed(poll[0], 50);
    }

    /**
     * Returns a runnable that runs the given one and completes the result with an error if it throws. Use for any
     * tool code that runs later (timers, background task callbacks), outside McpToolRegistry.callTool's handler.
     */
    public static Runnable guard(McpResult aResult, Runnable aRun)
    {
        return () -> {
            try { aRun.run(); }
            catch (McpException e) { aResult.error(e.getMessage()); }
            catch (Throwable t) {
                t.printStackTrace();
                aResult.error("Tool failed: " + t);
            }
        };
    }

    /**
     * Returns a map for a file.
     */
    public static Map<String,Object> getFileInfo(WebFile aFile)
    {
        Map<String,Object> info = new LinkedHashMap<>();
        info.put("path", aFile.getPath());
        if (aFile.isDir())
            info.put("dir", true);
        else {
            info.put("size", aFile.getSize());
            if (aFile.isModified() || aFile.isUpdateSet())
                info.put("unsaved", true);
        }
        return info;
    }

    /**
     * Converts a char index in text to a 1-based line number.
     */
    public static int getLineForCharIndex(CharSequence aText, int anIndex)
    {
        int line = 1;
        for (int i = 0, iMax = Math.min(anIndex, aText.length()); i < iMax; i++)
            if (aText.charAt(i) == '\n')
                line++;
        return line;
    }
}
