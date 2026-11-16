package snapcode.webmcp;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.app.*;
import snapcode.apptools.NewFileTool;
import snapcode.apptools.RunTool;
import snapcode.debug.RunApp;
import snapcode.project.*;
import snapcode.webbrowser.WebPage;
import snapcode.webmcp.McpTool.Schema;
import java.util.*;

/**
 * WebMCP tools for workspace state, projects and samples.
 */
public class WorkspaceMcpTools {

    // The samples root
    private static final String SAMPLES_PAGE = "SamplesPage.md";

    /**
     * Adds tools.
     */
    public static void addTools(McpToolRegistry reg)
    {
        reg.addTool(new McpTool("get_state",
                "Returns an overview of the SnapCode IDE: open projects (with source dir and main class), open file tabs, " +
                "the selected file, whether a build is running, the last build error/warning counts and running apps. " +
                "Call this first to orient yourself. SnapCode is a Java IDE: '.java' files are normal Java classes, " +
                "'.jepl' files are Java REPL scripts (statements at top level, no class/main boilerplate), " +
                "'.jmd' files are Java markdown, and '.snp' files are SnapKit UI layouts edited with the GUI builder " +
                "(see the builder_* tools). Before writing app code, read snapcode_guide (how SnapKit apps are " +
                "structured and what works in the browser), and use lookup_class / find_classes to check APIs.",
                new Schema(), WorkspaceMcpTools::getState)).readOnly();

        reg.addTool(new McpTool("list_local_projects",
                "Lists projects saved in the SnapCode projects directory (browser storage), whether or not they are open.",
                new Schema(), WorkspaceMcpTools::listLocalProjects)).readOnly();

        reg.addTool(new McpTool("create_project",
                "Creates a new empty SnapCode project (with the SnapKit UI library on its classpath) and opens it in the " +
                "workspace. Then add files with create_file.",
                new Schema()
                        .str("name", "Project name (letters, digits, '_' or '-'). Must not already exist.", true)
                        .bool("include_snapcharts", "Also include the SnapCharts charting library (default false).", false),
                WorkspaceMcpTools::createProject));

        reg.addTool(new McpTool("open_project",
                "Opens a project in the workspace: an existing local project by name, a GitHub repository " +
                "('https://github.com/owner/repo' or 'github:/owner/repo'), a SnapCloud URL, or a URL to a project zip file. " +
                "Remote projects are downloaded first; this waits until the project appears (up to 'timeout_seconds').",
                new Schema()
                        .str("name_or_url", "Local project name or project URL.", true)
                        .integer("timeout_seconds", "Max seconds to wait for a remote project (default 60).", false),
                WorkspaceMcpTools::openProject));

        reg.addTool(new McpTool("close_project",
                "Closes an open project (removes it from the workspace; files stay saved locally). Unsaved files are saved first.",
                new Schema().str("project", "Name of the project to close.", true),
                WorkspaceMcpTools::closeProject));

        reg.addTool(new McpTool("list_samples",
                "Lists the built-in SnapCode sample projects and files (games, graphics, charts, UI) with descriptions. " +
                "Open one with open_sample.",
                new Schema(), WorkspaceMcpTools::listSamples)).readOnly();

        reg.addTool(new McpTool("open_sample",
                "Opens a SnapCode sample by name (from list_samples), replacing other open non-temporary projects as the " +
                "Samples page does.",
                new Schema().str("name", "Sample name, e.g. 'Tetris' or 'Charting'.", true),
                WorkspaceMcpTools::openSample));

        reg.addTool(new McpTool("open_greenfoot_scenario",
                "Imports a Greenfoot scenario from greenfoot.org by its numeric scenario id and opens it as a project.",
                new Schema().integer("scenario_id", "The greenfoot.org scenario id.", true),
                WorkspaceMcpTools::openGreenfootScenario));

        reg.addTool(new McpTool("get_share_link",
                "Returns the shareable SnapCode web link for the selected file/project (same as 'Copy Web Link').",
                new Schema(), WorkspaceMcpTools::getShareLink)).readOnly();
    }

    /**
     * get_state
     */
    private static void getState(McpArgs args, McpResult result)
    {
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        Workspace workspace = workspacePane.getWorkspace();
        Map<String,Object> state = new LinkedHashMap<>();

        // Projects
        List<Map<String,Object>> projects = new ArrayList<>();
        for (Project proj : workspace.getProjects()) {
            Map<String,Object> projMap = new LinkedHashMap<>();
            projMap.put("name", proj.getName());
            WebFile srcDir = proj.getSourceDir();
            projMap.put("sourceDir", srcDir != null ? srcDir.getPath() : null);
            BuildFile buildFile = proj.getBuildFile();
            projMap.put("mainClass", buildFile.getMainClassName());
            projMap.put("url", proj.getSite().getURL().getString());
            projects.add(projMap);
        }
        state.put("projects", projects);

        // Open files and selected file
        state.put("openFiles", workspacePane.getPagePane().getOpenFiles().stream().map(WorkspaceMcpTools::getFileRef).toList());
        WebFile selFile = workspacePane.getSelFile();
        state.put("selectedFile", selFile != null ? getFileRef(selFile) : null);
        WebPage selPage = workspacePane.getPagePane().getSelPage();
        state.put("selectedPage", selPage != null ? selPage.getClass().getSimpleName() : null);

        // Build
        BuildIssues buildIssues = workspace.getBuildIssues();
        Map<String,Object> build = new LinkedHashMap<>();
        build.put("building", workspace.isBuilding());
        build.put("errors", buildIssues.getErrorCount());
        build.put("warnings", buildIssues.getWarningCount());
        state.put("build", build);

        // Running apps
        RunTool runTool = workspacePane.getRunTool();
        List<Map<String,Object>> apps = new ArrayList<>();
        for (RunApp app : runTool.getApps()) {
            Map<String,Object> appMap = new LinkedHashMap<>();
            appMap.put("name", app.getName());
            appMap.put("running", app.isRunning());
            appMap.put("selected", app == runTool.getSelApp());
            apps.add(appMap);
        }
        state.put("apps", apps);

        result.json(state);
    }

    /**
     * Returns a reference for a file: "Project:/path".
     */
    static String getFileRef(WebFile aFile)
    {
        Project proj = Project.getProjectForFile(aFile);
        return (proj != null ? proj.getName() + ":" : "") + aFile.getPath();
    }

    /**
     * list_local_projects
     */
    private static void listLocalProjects(McpArgs args, McpResult result)
    {
        WebFile snapCodeDir = SnapCodeUtils.getSnapCodeDir();
        List<String> openNames = McpUtils.getWorkspacePane().getProjects().stream().map(Project::getName).toList();
        List<Map<String,Object>> projects = new ArrayList<>();
        if (snapCodeDir != null && snapCodeDir.isDir()) {
            for (WebFile dir : snapCodeDir.getFiles()) {
                if (!dir.isDir() || !ProjectUtils.isProjectDir(dir))
                    continue;
                Map<String,Object> projMap = new LinkedHashMap<>();
                projMap.put("name", dir.getName());
                projMap.put("open", openNames.contains(dir.getName()));
                projects.add(projMap);
            }
        }
        result.json(projects);
    }

    /**
     * create_project
     */
    private static void createProject(McpArgs args, McpResult result)
    {
        String name = args.requireString("name").trim();
        if (!name.matches("[A-Za-z0-9_\\-]+"))
            throw new McpException("Invalid project name: use only letters, digits, '_' or '-'");
        WebFile projDir = SnapCodeUtils.getSnapCodeProjectDirForName(name);
        if (projDir.getExists())
            throw new McpException("A project named '" + name + "' already exists. Use open_project to open it.");

        // Create dir and project
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        projDir = projDir.getSite().createFileForPath(projDir.getPath(), true);
        McpUtils.saveFile(projDir);
        NewFileTool newFileTool = workspacePane.getNewFileTool();
        Project proj = newFileTool.createNewProjectForProjectDir(projDir);
        if (args.getBool("include_snapcharts", false))
            proj.getBuildFile().setIncludeSnapChartsRuntime(true);
        proj.getBuildFile().writeFile();

        Map<String,Object> info = new LinkedHashMap<>();
        info.put("project", proj.getName());
        info.put("sourceDir", proj.getSourceDir().getPath());
        info.put("next", "Create files with create_file, e.g. path '/src/Main.java' (type java) or '/src/Main.jepl'.");
        result.json(info);
    }

    /**
     * open_project
     */
    private static void openProject(McpArgs args, McpResult result)
    {
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        Workspace workspace = workspacePane.getWorkspace();
        String nameOrUrl = args.requireString("name_or_url").trim();
        int timeout = args.getInt("timeout_seconds", 60) * 1000;

        // Handle local project name
        boolean isUrl = nameOrUrl.contains(":") || nameOrUrl.contains("/");
        String projName;
        if (!isUrl) {
            WebFile projDir = SnapCodeUtils.getSnapCodeProjectDirForName(nameOrUrl);
            if (!ProjectUtils.isProjectDir(projDir))
                throw new McpException("No local project named '" + nameOrUrl + "'. See list_local_projects.");
            WorkspacePaneUtils.openProjectForProjectFile(workspacePane, projDir);
            projName = projDir.getName();
        }

        // Handle URL
        else {
            WebURL url = WebURL.getUrl(nameOrUrl);
            if (url == null)
                throw new McpException("Invalid URL: " + nameOrUrl);
            projName = url.getFilenameSimple();
            if (!WorkspacePaneUtils.openProjectUrl(workspacePane, url))
                throw new McpException("Could not open project for URL: " + nameOrUrl);
        }

        // Wait for project to appear
        String finalProjName = projName;
        McpUtils.waitUntil(result, () -> workspace.getProjectForName(finalProjName) != null, timeout,
                () -> {
                    Project proj = workspace.getProjectForName(finalProjName);
                    WorkspacePaneUtils.selectGoodDefaultFile(workspacePane, proj);
                    result.json(Map.of("project", proj.getName(), "opened", true));
                },
                () -> result.error("Timed out waiting for project '" + finalProjName + "' to open. It may still be downloading; check get_state."));
    }

    /**
     * close_project
     */
    private static void closeProject(McpArgs args, McpResult result)
    {
        String projName = args.requireString("project");
        Project proj = McpUtils.getProject(projName);
        String name = proj.getName();
        Workspace workspace = McpUtils.getWorkspace();
        workspace.saveAllFiles();
        workspace.closeProject(proj);
        result.json(Map.of("closed", name));
    }

    /**
     * Returns the samples as list of [name, url, description].
     */
    private static List<String[]> getSamples()
    {
        WebURL samplesUrl = WebURL.getResourceUrl(SamplesPage.class, SAMPLES_PAGE);
        String text = samplesUrl != null ? samplesUrl.getText() : null;
        if (text == null)
            throw new McpException("Samples list not available");

        List<String[]> samples = new ArrayList<>();
        String category = "";
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("## "))
                category = line.substring(3).trim();
            else if (line.startsWith("* ") && i + 1 < lines.length) {
                String name = line.substring(2).trim();
                String urlLine = lines[i + 1].trim();
                int urlStart = urlLine.indexOf("](");
                int urlEnd = urlLine.lastIndexOf(')');
                if (urlStart < 0 || urlEnd < urlStart) continue;
                String url = urlLine.substring(urlStart + 2, urlEnd);
                String desc = i + 2 < lines.length && !lines[i + 2].trim().startsWith("*") ? lines[i + 2].trim() : "";
                samples.add(new String[] { name, url, desc, category });
            }
        }
        return samples;
    }

    /**
     * list_samples
     */
    private static void listSamples(McpArgs args, McpResult result)
    {
        List<Map<String,Object>> list = new ArrayList<>();
        for (String[] sample : getSamples()) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("name", sample[0]);
            map.put("category", sample[3]);
            map.put("description", sample[2]);
            map.put("kind", sample[1].endsWith(".zip") ? "project" : "file");
            list.add(map);
        }
        result.json(list);
    }

    /**
     * open_sample
     */
    private static void openSample(McpArgs args, McpResult result)
    {
        String name = args.requireString("name").trim();
        String[] sample = getSamples().stream().filter(s -> s[0].equalsIgnoreCase(name)).findFirst().orElse(null);
        if (sample == null)
            throw new McpException("No sample named '" + name + "'. See list_samples.");

        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        WebURL sampleUrl = WebURL.getUrl(sample[1]);
        WorkspacePaneUtils.openSamplesUrl(workspacePane, sampleUrl);

        // Wait for a file (or project) from the sample to be selected
        String sampleFileName = sampleUrl.getFilenameSimple();
        McpUtils.waitUntil(result, () -> {
                    WebFile selFile = workspacePane.getSelFile();
                    if (selFile != null && selFile.getSimpleName().equals(sampleFileName)) return true;
                    return workspacePane.getWorkspace().getProjectForName(sampleFileName) != null;
                }, 60000,
                () -> result.json(Map.of("opened", sample[0], "selectedFile", getSelFileRef(workspacePane))),
                () -> result.text("Sample '" + sample[0] + "' is still loading; check get_state."));
    }

    private static String getSelFileRef(WorkspacePane workspacePane)
    {
        WebFile selFile = workspacePane.getSelFile();
        return selFile != null ? getFileRef(selFile) : "";
    }

    /**
     * open_greenfoot_scenario
     */
    private static void openGreenfootScenario(McpArgs args, McpResult result)
    {
        int scenarioId = args.getInt("scenario_id", -1);
        if (scenarioId <= 0)
            throw new McpException("scenario_id must be a positive number");
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        int projCount = workspacePane.getProjects().size();
        GreenImport.openProjectForGreenfootScenarioId(workspacePane, scenarioId);
        McpUtils.waitUntil(result, () -> workspacePane.getProjects().size() > projCount, 90000,
                () -> result.json(Map.of("opened", true, "projects", McpUtils.getProjectNames())),
                () -> result.text("Greenfoot scenario is still importing; check get_state."));
    }

    /**
     * get_share_link
     */
    private static void getShareLink(McpArgs args, McpResult result)
    {
        WorkspacePane workspacePane = McpUtils.getWorkspacePane();
        String hash = workspacePane.getSelFileTool().getWindowLocationHash();
        result.text("https://reportmill.com/SnapCode/app/#" + (hash != null ? hash : ""));
    }
}
