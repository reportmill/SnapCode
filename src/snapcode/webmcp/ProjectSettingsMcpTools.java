package snapcode.webmcp;
import snapcode.project.*;
import snapcode.webmcp.McpTool.Schema;
import java.util.*;

/**
 * WebMCP tools for project build settings and library dependencies (the project's Build File tool).
 */
public class ProjectSettingsMcpTools {

    /**
     * Adds tools.
     */
    public static void addTools(McpToolRegistry reg)
    {
        reg.addTool(new McpTool("get_build_settings",
                "Returns a project's build settings: source/build paths, main class, Java release, preview features, " +
                "bundled libraries (SnapKit, SnapCharts, Greenfoot, JavaFX), Maven repositories and dependencies " +
                "(with resolution status).",
                new Schema().str("project", McpUtils.PROJECT_ARG_DESC, false),
                ProjectSettingsMcpTools::getBuildSettings)).readOnly();

        reg.addTool(new McpTool("set_build_settings",
                "Changes a project's build settings. Only the given settings change. Changes are saved and trigger a rebuild.",
                new Schema()
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .str("source_path", "Source directory, e.g. 'src'.", false)
                        .str("build_path", "Build output directory, e.g. 'bin'.", false)
                        .str("main_class", "Fully qualified main class name to run by default.", false)
                        .integer("java_release", "Java release level to compile for (e.g. 21).", false)
                        .bool("enable_preview", "Enable Java preview features.", false)
                        .bool("include_snapkit", "Include the SnapKit UI library.", false)
                        .bool("include_snapcharts", "Include the SnapCharts charting library.", false)
                        .bool("include_greenfoot", "Include the Greenfoot library.", false)
                        .bool("include_javafx", "Include JavaFX (desktop only).", false),
                ProjectSettingsMcpTools::setBuildSettings));

        reg.addTool(new McpTool("add_dependency",
                "Adds a library dependency to a project: a Maven artifact ('group:artifact:version', fetched from the " +
                "project's Maven repositories), another open project (type 'project'), or a jar file in the project " +
                "(type 'jar'). Maven dependencies download in the background; check get_build_settings for status.",
                new Schema()
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .enumStr("type", "Dependency type (default 'maven').", false, "maven", "project", "jar")
                        .str("id", "Maven id 'group:artifact:version', project name, or jar path.", true),
                ProjectSettingsMcpTools::addDependency));

        reg.addTool(new McpTool("remove_dependency",
                "Removes a library dependency from a project by its id (as shown by get_build_settings).",
                new Schema()
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .str("id", "Dependency id.", true),
                ProjectSettingsMcpTools::removeDependency));

        reg.addTool(new McpTool("add_maven_repository",
                "Adds a Maven repository (name + URL) to a project, for dependencies not on Maven Central.",
                new Schema()
                        .str("project", McpUtils.PROJECT_ARG_DESC, false)
                        .str("name", "Repository name.", true)
                        .str("url", "Repository URL.", true),
                ProjectSettingsMcpTools::addRepository));
    }

    /**
     * get_build_settings
     */
    private static void getBuildSettings(McpArgs args, McpResult result)
    {
        Project proj = McpUtils.getProject(args.getString("project"));
        BuildFile buildFile = proj.getBuildFile();
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("project", proj.getName());
        map.put("source_path", buildFile.getSourcePath());
        map.put("build_path", buildFile.getBuildPath());
        map.put("main_class", buildFile.getMainClassName());
        map.put("java_release", buildFile.getCompileRelease());
        map.put("enable_preview", buildFile.isEnableCompilePreview());
        map.put("include_snapkit", buildFile.isIncludeSnapKitRuntime());
        map.put("include_snapcharts", buildFile.isIncludeSnapChartsRuntime());
        map.put("include_greenfoot", buildFile.isIncludeGreenfootRuntime());
        map.put("include_javafx", buildFile.isIncludeJavaFX());

        List<Map<String,Object>> repos = new ArrayList<>();
        for (MavenRepository repo : buildFile.getRepositories())
        {
            Map<String,Object> repoMap = new LinkedHashMap<>();
            repoMap.put("name", repo.getName());
            if (repo.getUrl() != null)
                repoMap.put("url", repo.getUrl());
            repos.add(repoMap);
        }
        map.put("repositories", repos);

        List<Map<String,Object>> deps = new ArrayList<>();
        for (BuildDependency dep : buildFile.getDependencies()) {
            Map<String,Object> depMap = new LinkedHashMap<>();
            depMap.put("type", dep.getType().toString().toLowerCase());
            depMap.put("id", dep.getId());
            if (dep instanceof MavenDependency mavenDep) {
                depMap.put("status", mavenDep.getStatus());
                if (mavenDep.getError() != null)
                    depMap.put("error", mavenDep.getError());
            }
            else depMap.put("resolved", dep.isResolved());
            deps.add(depMap);
        }
        map.put("dependencies", deps);
        result.json(map);
    }

    /**
     * set_build_settings
     */
    private static void setBuildSettings(McpArgs args, McpResult result)
    {
        Project proj = McpUtils.getProject(args.getString("project"));
        BuildFile buildFile = proj.getBuildFile();
        List<String> changed = new ArrayList<>();
        if (args.has("source_path")) { buildFile.setSourcePath(args.getString("source_path")); changed.add("source_path"); }
        if (args.has("build_path")) { buildFile.setBuildPath(args.getString("build_path")); changed.add("build_path"); }
        if (args.has("main_class")) {
            String mainClass = args.getString("main_class");
            buildFile.setMainClassName(mainClass.isEmpty() ? null : mainClass);
            changed.add("main_class");
        }
        if (args.has("java_release")) { buildFile.setCompileRelease(args.getInt("java_release", 21)); changed.add("java_release"); }
        if (args.has("enable_preview")) { buildFile.setEnableCompilePreview(args.getBool("enable_preview", false)); changed.add("enable_preview"); }
        if (args.has("include_snapkit")) { buildFile.setIncludeSnapKitRuntime(args.getBool("include_snapkit", true)); changed.add("include_snapkit"); }
        if (args.has("include_snapcharts")) { buildFile.setIncludeSnapChartsRuntime(args.getBool("include_snapcharts", false)); changed.add("include_snapcharts"); }
        if (args.has("include_greenfoot")) { buildFile.setIncludeGreenfootRuntime(args.getBool("include_greenfoot", false)); changed.add("include_greenfoot"); }
        if (args.has("include_javafx")) { buildFile.setIncludeJavaFX(args.getBool("include_javafx", false)); changed.add("include_javafx"); }
        if (changed.isEmpty())
            throw new McpException("No settings given");
        buildFile.writeFile();
        proj.getWorkspace().getBuilder().buildWorkspaceLater();
        result.json(Map.of("project", proj.getName(), "changed", changed));
    }

    /**
     * add_dependency
     */
    private static void addDependency(McpArgs args, McpResult result)
    {
        Project proj = McpUtils.getProject(args.getString("project"));
        BuildFile buildFile = proj.getBuildFile();
        String type = args.getString("type", "maven");
        String id = args.requireString("id").trim();

        BuildDependency dependency;
        switch (type) {
            case "maven" -> {
                if (id.split(":").length < 3)
                    throw new McpException("Maven id must be 'group:artifact:version'");
                dependency = new MavenDependency(id);
            }
            case "project" -> {
                Project otherProj = McpUtils.getProject(id);
                if (otherProj == proj)
                    throw new McpException("A project can't depend on itself");
                BuildDependency.ProjectDependency projDep = new BuildDependency.ProjectDependency();
                projDep.setProjectName(otherProj.getName());
                dependency = projDep;
            }
            case "jar" -> {
                dependency = BuildDependency.getJarFileDependencyForPath(proj, McpUtils.normalizePath(id));
                if (dependency == null)
                    throw new McpException("Jar file not found: " + id);
            }
            default -> throw new McpException("Unknown dependency type: " + type);
        }

        if (buildFile.getDependencies().contains(dependency))
            throw new McpException("Dependency already present: " + dependency.getId());
        buildFile.addDependency(dependency);
        if (dependency instanceof MavenDependency mavenDep)
            mavenDep.preloadPackageFiles();
        result.json(Map.of("project", proj.getName(), "added", dependency.getId()));
    }

    /**
     * remove_dependency
     */
    private static void removeDependency(McpArgs args, McpResult result)
    {
        Project proj = McpUtils.getProject(args.getString("project"));
        BuildFile buildFile = proj.getBuildFile();
        String id = args.requireString("id").trim();
        BuildDependency match = buildFile.getDependencies().stream().filter(dep -> id.equals(dep.getId())).findFirst().orElse(null);
        if (match == null)
            throw new McpException("No dependency with id '" + id + "'");
        buildFile.removeDependency(match);
        result.json(Map.of("project", proj.getName(), "removed", id));
    }

    /**
     * add_maven_repository
     */
    private static void addRepository(McpArgs args, McpResult result)
    {
        Project proj = McpUtils.getProject(args.getString("project"));
        MavenRepository repo = new MavenRepository();
        repo.setPropValue(MavenRepository.Name_Prop, args.requireString("name"));
        repo.setPropValue(MavenRepository.Url_Prop, args.requireString("url"));
        proj.getBuildFile().addRepository(repo);
        result.json(Map.of("project", proj.getName(), "added", repo.getName()));
    }
}
