package snapcode.webmcp;
import javakit.parse.JFile;
import javakit.parse.JNode;
import snap.util.FilePathUtils;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.app.JavaPage;
import snapcode.app.WorkspacePane;
import snapcode.apptools.FilesTool;
import snapcode.apptools.SearchTool;
import snapcode.project.*;
import snapcode.webbrowser.SnapBuilderPage;
import snapcode.webmcp.McpTool.Schema;
import java.util.*;

/**
 * WebMCP tools for project files: list, read, create, write, edit, rename, delete, open/close, save and search.
 */
public class FileMcpTools {

    // Java-like file types (only one of these may exist per class name in a directory)
    private static final List<String> JAVA_FILE_TYPES = List.of("java", "jepl", "jmd");

    /**
     * Adds tools.
     */
    public static void addTools(McpToolRegistry reg)
    {
        reg.addTool(new McpTool("list_files",
                "Lists the files of a project (recursively), skipping the build output directory and hidden files. " +
                "Each entry has a path (relative to project root), size, and 'unsaved' when it has unsaved edits.",
                new Schema()
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .str("dir", "Directory path to list (default '/', the project root).", false),
                FileMcpTools::listFiles)).readOnly();

        reg.addTool(new McpTool("read_file",
                "Reads a text file, including unsaved edits in an open editor. Lines are returned with 1-based line " +
                "numbers ('N| text') unless line_numbers is false.",
                new Schema()
                        .str("path", McpUtils.PATH_ARG_DESC, true)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .integer("start_line", "First line to return (1-based, default 1).", false)
                        .integer("end_line", "Last line to return (inclusive, default end of file).", false)
                        .bool("line_numbers", "Prefix lines with line numbers (default true).", false),
                FileMcpTools::readFile)).readOnly();

        reg.addTool(new McpTool("create_file",
                "Creates a new file in a project, saves it and opens it in an editor tab. Types: 'java' (a class; if " +
                "no content is given a class skeleton is generated), 'jepl' (Java REPL script), 'jmd' (Java markdown), " +
                "'snp' (SnapKit UI layout for the GUI builder; if no content, a starter layout is generated), 'dir', " +
                "or any other extension for plain files. Java-like files go in the project source dir by default. " +
                "To build a UI app: create 'Foo.java' with 'class Foo extends snap.view.ViewOwner' and a sibling " +
                "'Foo.snp' layout; ViewOwner.createUI() loads Foo.snp automatically, and the controller handles " +
                "control events in 'respondUI(ViewEvent e)' using e.equals(\"ControlName\"). Read snapcode_guide first " +
                "for the app structure and browser rules (e.g. use snap.view.ViewTimer for timers).",
                new Schema()
                        .str("path", "File path relative to project root (e.g. '/src/Game.java') or just a file " +
                                "name (placed in the source dir for java/jepl/jmd/snp, else project root).", true)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .str("content", "Initial file content (optional).", false)
                        .bool("overwrite", "Replace the file if it already exists (default false).", false)
                        .bool("open", "Open the new file in an editor tab (default true).", false),
                FileMcpTools::createFile));

        reg.addTool(new McpTool("write_file",
                "Replaces the entire content of an existing text file (creates it if missing). If the file is open in " +
                "an editor the edit goes through the editor (undoable). Saves by default, which triggers an " +
                "incremental build. For '.snp' files open in the GUI builder, the builder reloads the new layout.",
                new Schema()
                        .str("path", McpUtils.PATH_ARG_DESC, true)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .str("content", "The new full file content.", true)
                        .bool("save", "Save after writing (default true).", false),
                FileMcpTools::writeFile));

        reg.addTool(new McpTool("edit_file",
                "Edits a text file by replacing an exact string. 'old_text' must match exactly once unless " +
                "replace_all is true. Edits go through an open editor if there is one. Saves by default.",
                new Schema()
                        .str("path", McpUtils.PATH_ARG_DESC, true)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .str("old_text", "Exact text to find.", true)
                        .str("new_text", "Replacement text.", true)
                        .bool("replace_all", "Replace every occurrence (default false).", false)
                        .bool("save", "Save after editing (default true).", false),
                FileMcpTools::editFile));

        reg.addTool(new McpTool("rename_file",
                "Renames a file (or an empty directory) within its directory. Unsaved edits are saved first.",
                new Schema()
                        .str("path", McpUtils.PATH_ARG_DESC, true)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .str("new_name", "New file name (extension optional: keeps the old extension).", true),
                FileMcpTools::renameFile));

        reg.addTool(new McpTool("delete_file",
                "Deletes a file or directory from a project (directories are deleted with their contents). " +
                "This cannot be undone.",
                new Schema()
                        .str("path", McpUtils.PATH_ARG_DESC, true)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                FileMcpTools::deleteFile)).consequential();

        reg.addTool(new McpTool("open_file",
                "Opens a file in an editor tab and selects it (Java editor, GUI builder for .snp, viewer for others). " +
                "Optionally moves the caret to a line.",
                new Schema()
                        .str("path", McpUtils.PATH_ARG_DESC, true)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .integer("line", "1-based line to select (Java files).", false),
                FileMcpTools::openFile));

        reg.addTool(new McpTool("close_file",
                "Closes a file's editor tab (unsaved edits are kept in memory until saved or reverted).",
                new Schema()
                        .str("path", McpUtils.PATH_ARG_DESC + " Default: the selected file.", false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                FileMcpTools::closeFile));

        reg.addTool(new McpTool("save_file",
                "Saves a file with unsaved edits (default: all unsaved files in all projects).",
                new Schema()
                        .str("path", McpUtils.PATH_ARG_DESC + " Omit to save all files.", false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                FileMcpTools::saveFile));

        reg.addTool(new McpTool("revert_file",
                "Discards unsaved edits to a file and reloads it from storage.",
                new Schema()
                        .str("path", McpUtils.PATH_ARG_DESC, true)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false),
                FileMcpTools::revertFile)).consequential();

        reg.addTool(new McpTool("search_project",
                "Searches all open projects. kind 'text' (default): case-insensitive text search returning file, line " +
                "and line text. kind 'references' or 'declarations': finds references to / declarations of the Java " +
                "symbol at path+line+column (like the editor's 'Show References' / 'Show Declarations').",
                new Schema()
                        .enumStr("kind", "Search kind (default 'text').", false, "text", "references", "declarations")
                        .str("text", "Text to find (kind 'text').", false)
                        .str("path", "Java file containing the symbol (kind references/declarations).", false)
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .integer("line", "1-based line of the symbol.", false)
                        .integer("column", "1-based column of the symbol.", false)
                        .integer("max_results", "Maximum results (default 100).", false),
                FileMcpTools::searchProject)).readOnly();
    }

    /**
     * list_files
     */
    private static void listFiles(McpArgs args, McpResult result)
    {
        Project proj = McpUtils.getProject(args.getString("project"));
        String dirPath = McpUtils.normalizePath(args.getString("dir", "/"));
        WebFile dir = proj.getSite().getFileForPath(dirPath);
        if (dir == null || !dir.isDir())
            throw new McpException("Directory not found: " + dirPath);

        List<Map<String,Object>> files = new ArrayList<>();
        listFilesImpl(dir, proj, files);
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("project", proj.getName());
        map.put("sourceDir", proj.getSourceDir().getPath());
        map.put("files", files);
        result.json(map);
    }

    private static void listFilesImpl(WebFile aDir, Project aProj, List<Map<String,Object>> files)
    {
        for (WebFile file : aDir.getFiles()) {
            if (file.getName().startsWith(".") || file == aProj.getBuildDir())
                continue;
            files.add(McpUtils.getFileInfo(file));
            if (file.isDir())
                listFilesImpl(file, aProj, files);
        }
    }

    /**
     * read_file
     */
    private static void readFile(McpArgs args, McpResult result)
    {
        WebFile file = McpUtils.getFile(args.getString("project"), args.requireString("path"));
        if (file.isDir())
            throw new McpException(file.getPath() + " is a directory; use list_files");
        if (!McpUtils.isTextFile(file))
            throw new McpException(file.getPath() + " is not a text file (" + file.getSize() + " bytes)");

        String text = McpUtils.getFileText(file);
        String[] lines = text.split("\n", -1);
        int start = Math.max(1, args.getInt("start_line", 1));
        int end = Math.min(lines.length, args.getInt("end_line", lines.length));
        boolean lineNumbers = args.getBool("line_numbers", true);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; i++) {
            if (lineNumbers)
                sb.append(i).append("| ");
            sb.append(lines[i - 1]);
            if (i < end) sb.append('\n');
        }
        result.text(sb.toString());
    }

    /**
     * create_file
     */
    private static void createFile(McpArgs args, McpResult result)
    {
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        Project proj = McpUtils.getProject(args.getString("project"));
        WebSite site = proj.getSite();
        String pathArg = args.requireString("path").trim();
        String fileType = FilePathUtils.getExtension(pathArg).toLowerCase();
        boolean isDir = pathArg.endsWith("/") || fileType.equals("dir");

        // Get path: bare names go in source dir for java-like files
        String path;
        if (!pathArg.contains("/")) {
            boolean inSrc = JAVA_FILE_TYPES.contains(fileType) || fileType.equals("snp");
            WebFile parentDir = inSrc ? proj.getSourceDir() : site.getRootDir();
            path = parentDir.getDirPath() + pathArg;
        }
        else path = McpUtils.normalizePath(pathArg);
        if (isDir && path.endsWith(".dir"))
            path = path.substring(0, path.length() - 4);

        // Check existing
        WebFile existing = site.getFileForPath(path);
        boolean overwrite = args.getBool("overwrite", false);
        if (existing != null && !overwrite)
            throw new McpException("File already exists: " + path + " (use write_file, or overwrite=true)");

        // Check Java-like collisions (e.g. Foo.java vs Foo.jepl)
        if (JAVA_FILE_TYPES.contains(fileType)) {
            for (String otherType : JAVA_FILE_TYPES) {
                if (otherType.equals(fileType)) continue;
                String otherPath = FilePathUtils.getSisterPath(path, otherType);
                if (site.getFileForPath(otherPath) != null)
                    throw new McpException("A Java-like file already exists for this name: " + otherPath);
            }
        }

        // Create file
        WebFile newFile = existing != null ? existing : site.createFileForPath(path, isDir);
        String content = args.getString("content");
        if (!isDir) {
            if (content != null)
                newFile.setText(content);
            else if (fileType.equals("java"))
                JavaPage.initJavaFile(newFile);
            else if (fileType.equals("snp"))
                SnapBuilderPage.initSnapFile(newFile);
            else newFile.setText("");
        }
        McpUtils.saveFile(newFile);

        // Handle java-like files: resolve deps and build
        if (JAVA_FILE_TYPES.contains(fileType)) {
            JavaDeps.resolveDependenciesForFile(null, newFile);
            proj.getWorkspace().getBuilder().buildWorkspaceLater();
        }

        // Open file
        if (!isDir && args.getBool("open", true))
            workspacePane.openFile(newFile);

        Map<String,Object> info = new LinkedHashMap<>();
        info.put("created", WorkspaceMcpTools.getFileRef(newFile));
        if (!isDir && content == null && (fileType.equals("java") || fileType.equals("snp")))
            info.put("content", newFile.getText());
        result.json(info);
    }

    /**
     * write_file
     */
    private static void writeFile(McpArgs args, McpResult result)
    {
        String projName = args.getString("project");
        String pathArg = args.requireString("path");
        String content = args.requireString("content");
        WebFile file = McpUtils.findFile(projName, pathArg);

        // If missing, create
        if (file == null) {
            Map<String,Object> createArgs = new LinkedHashMap<>(args.getMap());
            createFile(new McpArgs(createArgs), result);
            return;
        }
        if (file.isDir())
            throw new McpException(file.getPath() + " is a directory");

        // Handle snp open in builder: update builder content instead
        if (file.getFileType().equals("snp") && BuilderMcpTools.getOpenEditorPane(file) != null) {
            BuilderMcpTools.setBuilderXml(file, content, args.getBool("save", true));
            result.json(Map.of("written", WorkspaceMcpTools.getFileRef(file), "via", "GUI builder"));
            return;
        }

        McpUtils.setFileText(file, content, args.getBool("save", true));
        result.json(Map.of("written", WorkspaceMcpTools.getFileRef(file), "length", content.length()));
    }

    /**
     * edit_file
     */
    private static void editFile(McpArgs args, McpResult result)
    {
        WebFile file = McpUtils.getFile(args.getString("project"), args.requireString("path"));
        String oldText = args.requireString("old_text");
        String newText = args.requireString("new_text");
        if (oldText.isEmpty())
            throw new McpException("old_text must not be empty");

        // Get text and count matches
        boolean isSnpInBuilder = file.getFileType().equals("snp") && BuilderMcpTools.getOpenEditorPane(file) != null;
        String text = McpUtils.getFileText(file);
        int count = 0;
        for (int i = text.indexOf(oldText); i >= 0; i = text.indexOf(oldText, i + oldText.length()))
            count++;
        if (count == 0)
            throw new McpException("old_text not found in " + file.getPath());
        boolean replaceAll = args.getBool("replace_all", false);
        if (count > 1 && !replaceAll)
            throw new McpException("old_text matches " + count + " times in " + file.getPath() +
                    "; add surrounding context to make it unique, or set replace_all=true");

        // Replace
        String newFileText = replaceAll ? text.replace(oldText, newText) : replaceFirst(text, oldText, newText);
        boolean save = args.getBool("save", true);
        if (isSnpInBuilder)
            BuilderMcpTools.setBuilderXml(file, newFileText, save);
        else McpUtils.setFileText(file, newFileText, save);

        int firstLine = McpUtils.getLineForCharIndex(newFileText, text.indexOf(oldText));
        result.json(Map.of("edited", WorkspaceMcpTools.getFileRef(file), "replacements", replaceAll ? count : 1, "line", firstLine));
    }

    private static String replaceFirst(String aText, String oldText, String newText)
    {
        int index = aText.indexOf(oldText);
        return aText.substring(0, index) + newText + aText.substring(index + oldText.length());
    }

    /**
     * rename_file
     */
    private static void renameFile(McpArgs args, McpResult result)
    {
        WebFile file = McpUtils.getFile(args.getString("project"), args.requireString("path"));
        String newName = args.requireString("new_name").trim();
        if (newName.contains("/"))
            throw new McpException("new_name must be a file name, not a path");
        if (file.isDir() && file.getFileCount() > 0)
            throw new McpException("Can't rename a non-empty directory");

        // Save first so the copy includes unsaved edits
        if (!file.isDir() && McpUtils.isTextFile(file))
            McpUtils.setFileText(file, McpUtils.getFileText(file), true);

        FilesTool filesTool = McpUtils.getWorkspacePane().getFilesTool();
        WebFile parent = file.getParent();
        if (!filesTool.renameFile(file, newName))
            throw new McpException("Rename failed (a file with that name may already exist)");
        String finalName = newName.contains(".") || file.getFileType().isEmpty() ? newName : newName + '.' + file.getFileType();
        result.json(Map.of("renamed", file.getPath(), "to", parent.getDirPath() + finalName));
    }

    /**
     * delete_file
     */
    private static void deleteFile(McpArgs args, McpResult result)
    {
        WebFile file = McpUtils.getFile(args.getString("project"), args.requireString("path"));
        if (file.isRoot())
            throw new McpException("Can't delete the project root directory");
        String path = file.getPath();
        FilesTool filesTool = McpUtils.getWorkspacePane().getFilesTool();
        filesTool.removeFiles(List.of(file));

        // FilesTool swallows delete failures, so confirm the file is really gone
        if (file.getExists())
            throw new McpException("Could not delete " + path);
        result.json(Map.of("deleted", path));
    }

    /**
     * open_file
     */
    private static void openFile(McpArgs args, McpResult result)
    {
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        WebFile file = McpUtils.getFile(args.getString("project"), args.requireString("path"));
        workspacePane.openFile(file);

        // Select line
        int line = args.getInt("line", 0);
        if (line > 0) {
            McpUtils.waitUntil(result, () -> workspacePane.getPagePane().getSelPage() instanceof JavaPage javaPage && javaPage.getFile() == file, 3000,
                    () -> {
                        JavaPage javaPage = (JavaPage) workspacePane.getPagePane().getSelPage();
                        javaPage.getTextArea().getTextAdapter().selectLine(line - 1);
                        result.json(Map.of("opened", WorkspaceMcpTools.getFileRef(file), "line", line));
                    },
                    () -> result.json(Map.of("opened", WorkspaceMcpTools.getFileRef(file))));
            return;
        }
        result.json(Map.of("opened", WorkspaceMcpTools.getFileRef(file)));
    }

    /**
     * close_file
     */
    private static void closeFile(McpArgs args, McpResult result)
    {
        WebFile file = McpUtils.getFileOrSelFile(args);
        McpUtils.getWorkspacePane().closeFile(file);
        result.json(Map.of("closed", WorkspaceMcpTools.getFileRef(file)));
    }

    /**
     * save_file
     */
    private static void saveFile(McpArgs args, McpResult result)
    {
        String path = args.getString("path");
        if (path == null || path.isEmpty()) {
            McpUtils.getWorkspace().saveAllFiles();
            result.json(Map.of("saved", "all"));
            return;
        }
        WebFile file = McpUtils.getFile(args.getString("project"), path);
        McpUtils.saveFile(file);
        result.json(Map.of("saved", WorkspaceMcpTools.getFileRef(file)));
    }

    /**
     * revert_file
     */
    private static void revertFile(McpArgs args, McpResult result)
    {
        WebFile file = McpUtils.getFile(args.getString("project"), args.requireString("path"));
        McpUtils.getWorkspacePane().getFilesTool().revertFile(file);
        result.json(Map.of("reverted", WorkspaceMcpTools.getFileRef(file)));
    }

    /**
     * search_project
     */
    private static void searchProject(McpArgs args, McpResult result)
    {
        String kind = args.getString("kind", "text");
        int maxResults = args.getInt("max_results", 100);
        List<Map<String,Object>> results = new ArrayList<>();

        // Handle text
        if (kind.equals("text")) {
            String text = args.requireString("text");
            if (text.isEmpty())
                throw new McpException("text must not be empty");
            for (Project proj : McpUtils.getWorkspacePane().getProjects())
                searchTextInFile(proj.getSite().getRootDir(), proj, text.toLowerCase(), results, maxResults);
            result.json(results);
            return;
        }

        // Handle symbol: get node
        WebFile file = McpUtils.getFile(args.getString("project"), args.requireString("path"));
        JavaAgent javaAgent = JavaAgent.getAgentForFile(file);
        if (javaAgent == null)
            throw new McpException(file.getPath() + " is not a Java file");
        int line = args.getInt("line", 0);
        int column = args.getInt("column", 0);
        if (line <= 0 || column <= 0)
            throw new McpException("line and column (1-based) are required for symbol searches");
        String text = McpUtils.getFileText(file);
        int charIndex = getCharIndexForLineAndColumn(text, line, column);
        JFile jfile = javaAgent.getJFile();
        JNode node = jfile.getNodeForCharIndex(charIndex);
        if (node == null || node.getDecl() == null)
            throw new McpException("No Java symbol found at line " + line + ", column " + column);

        // Run search with SearchTool
        SearchTool searchTool = McpUtils.getWorkspacePane().getWorkspaceTools().getToolForClass(SearchTool.class);
        if (kind.equals("references"))
            searchTool.searchReference(node);
        else searchTool.searchDeclaration(node);

        // Convert results
        List<SearchTool.Result> searchResults = searchTool.getResults();
        if (searchResults != null) {
            for (SearchTool.Result searchResult : searchResults) {
                if (results.size() >= maxResults) break;
                Map<String,Object> map = new LinkedHashMap<>();
                String urlStr = searchResult.getURLString();
                map.put("symbol", searchResult.getDescriptor());
                int hashIndex = urlStr.indexOf("#Sel=");
                String fileAddr = hashIndex > 0 ? urlStr.substring(0, hashIndex) : urlStr;
                WebURL resultUrl = WebURL.getUrl(fileAddr);
                WebFile resultFile = resultUrl != null ? resultUrl.getFile() : null;
                map.put("file", resultFile != null ? WorkspaceMcpTools.getFileRef(resultFile) : fileAddr);
                if (hashIndex > 0 && resultFile != null) {
                    int start = Integer.parseInt(urlStr.substring(hashIndex + 5, urlStr.indexOf('-', hashIndex)));
                    map.put("line", McpUtils.getLineForCharIndex(McpUtils.getFileText(resultFile), start));
                }
                results.add(map);
            }
        }
        result.json(results);
    }

    /**
     * Searches text in file (recursively).
     */
    private static void searchTextInFile(WebFile aFile, Project aProj, String aStr, List<Map<String,Object>> results, int max)
    {
        if (results.size() >= max)
            return;
        if (aFile.isDir()) {
            if (aFile == aProj.getBuildDir() || aFile.getName().startsWith(".") && !aFile.isRoot())
                return;
            for (WebFile file : aFile.getFiles())
                searchTextInFile(file, aProj, aStr, results, max);
            return;
        }
        if (!McpUtils.isTextFile(aFile))
            return;

        String[] lines = McpUtils.getFileText(aFile).split("\n", -1);
        for (int i = 0; i < lines.length && results.size() < max; i++) {
            if (lines[i].toLowerCase().contains(aStr)) {
                Map<String,Object> map = new LinkedHashMap<>();
                map.put("file", aProj.getName() + ":" + aFile.getPath());
                map.put("line", i + 1);
                map.put("text", lines[i].trim());
                results.add(map);
            }
        }
    }

    /**
     * Returns the char index for 1-based line and column.
     */
    static int getCharIndexForLineAndColumn(String aText, int aLine, int aColumn)
    {
        int index = 0;
        for (int line = 1; line < aLine; line++) {
            int nextNewline = aText.indexOf('\n', index);
            if (nextNewline < 0)
                throw new McpException("Line " + aLine + " is past end of file");
            index = nextNewline + 1;
        }
        return Math.min(index + aColumn - 1, aText.length());
    }
}
