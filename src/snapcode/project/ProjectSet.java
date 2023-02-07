package snapcode.project;
import javakit.project.Project;
import javakit.project.ProjectConfig;
import javakit.project.ProjectFiles;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages a project and the set of projects it depends on.
 */
public class ProjectSet {

    // The master project
    private ProjectX  _proj;

    // The list of projects this project depends on
    private ProjectX[]  _projects;

    // The array of class paths
    private String[]  _classPaths;

    // The array of library paths
    private String[]  _libPaths;

    /**
     * Creates a new ProjectSet for given Project.
     */
    public ProjectSet(ProjectX aProj)
    {
        _proj = aProj;
    }

    /**
     * Returns the list of projects this project depends on.
     */
    public ProjectX[] getProjects()
    {
        // If already set, just return
        if (_projects != null) return _projects;

        // Create list of projects from ClassPath.ProjectPaths
        ProjectConfig projConfig = _proj.getProjectConfig();
        String[] projPaths = projConfig.getProjectPaths();
        List<ProjectX> projs = new ArrayList<>();

        // Get parent site
        WebSite projectSite = _proj.getSite();
        WebURL parentSiteURL = projectSite.getURL();
        WebSite parentSite = parentSiteURL.getSite();

        // Iterate over project paths
        for (String projPath : projPaths) {

            // Get URL and site for project path
            WebURL projURL = parentSite.getURL(projPath);
            WebSite projSite = projURL.getAsSite();

            // Get Project
            ProjectX proj = ProjectX.getProjectForSite(projSite);
            if (proj == null)
                proj = new ProjectX(projSite);
            proj._parent = _proj;

            // Add to list
            ProjectSet childProjectSet = proj.getProjectSet();
            ProjectX[] childProjects = childProjectSet.getProjects();
            ListUtils.addAllUnique(projs, childProjects);
            ListUtils.addUnique(projs, proj);
        }

        // Return list
        return _projects = projs.toArray(new ProjectX[0]);
    }

    /**
     * Adds a dependent project.
     */
    public void addProject(String aPath)
    {
        // Get project path
        String projPath = aPath;
        if (!projPath.startsWith("/"))
            projPath = '/' + projPath;

        // Add to ProjectConfig
        ProjectConfig projConfig = _proj.getProjectConfig();
        projConfig.addSrcPath(projPath);

        // Clear caches
        _projects = null;
        _classPaths = _libPaths = null;
    }

    /**
     * Removes a dependent project.
     */
    public void removeProject(String aPath)
    {
        ProjectConfig projConfig = _proj.getProjectConfig();
        projConfig.removeSrcPath(aPath);

        // Clear caches
        _projects = null;
        _classPaths = _libPaths = null;
    }

    /**
     * Returns the child project with given name.
     */
    public Project getProject(String aName)
    {
        String name = aName.startsWith("/") ? aName.substring(1) : aName;

        // Return project with matching name
        Project[] projects = getProjects();
        return ArrayUtils.findMatch(projects, proj -> proj.getName().equals(name));
    }

    /**
     * Returns a file for given path.
     */
    public WebFile getFile(String aPath)
    {
        // Check main project
        WebFile file = _proj.getFile(aPath);
        if (file != null)
            return file;

        // Check dependent projects
        Project[] projects = getProjects();
        for (Project proj : projects) {
            file = proj.getFile(aPath);
            if (file != null)
                return file;
        }

        // Return not found
        return null;
    }

    /**
     * Returns the source file for given path.
     */
    public WebFile getSourceFile(String aPath)
    {
        // Look for file in root project, then dependent projects
        WebFile file = _proj.getSourceFile(aPath, false, false);
        if (file != null)
            return file;

        Project[] projects = getProjects();
        for (Project proj : projects) {
            file = proj.getSourceFile(aPath, false, false);
            if (file != null)
                return file;
        }

        // Return not found
        return null;
    }

    /**
     * Returns the paths needed to compile/run project.
     */
    public String[] getClassPaths()
    {
        // If already set, just return
        if (_classPaths != null) return _classPaths;

        // Get Project ClassPaths
        String[] classPaths = _proj.getClassPaths();

        // Get dependent projects
        Project[] projects = getProjects();
        if (projects.length == 0)
            return _classPaths = classPaths;

        // Get list for LibPaths with base paths
        List<String> classPathsList = new ArrayList<>();
        Collections.addAll(classPathsList, classPaths);

        // Iterate over projects and add Project.ClassPaths for each
        for (Project proj : projects) {
            String[] projClassPaths = proj.getClassPaths();
            ListUtils.addAllUnique(classPathsList, projClassPaths);
        }

        // Set/return
        return _classPaths = classPathsList.toArray(new String[0]);
    }

    /**
     * Returns the paths needed to compile/run project, except build directory.
     */
    public String[] getLibPaths()
    {
        // If already set, just return
        if (_libPaths != null) return _libPaths;

        // Get LibPaths for this proj
        ProjectConfig projConfig = _proj.getProjectConfig();
        String[] libPaths = projConfig.getLibPathsAbsolute();

        // Get dependent projects (if none, just return LibPaths)
        Project[] projects = getProjects();
        if (projects.length == 0)
            return _libPaths = libPaths;

        // Get list for LibPaths with base paths
        List<String> libPathsList = new ArrayList<>();
        Collections.addAll(libPathsList, libPaths);

        // Iterate over projects and add Project.ClassPaths for each
        for (Project proj : projects) {
            String[] projClassPaths = proj.getClassPaths();
            ListUtils.addAllUnique(libPathsList, projClassPaths);
        }

        // Set/return
        return _libPaths = libPathsList.toArray(new String[0]);
    }

    /**
     * Adds a build file.
     */
    public void addBuildFilesAll()
    {
        _proj.addBuildFilesAll();
        Project[] projects = getProjects();
        for (Project proj : projects)
            proj.addBuildFilesAll();
    }

    /**
     * Builds the project.
     */
    public void buildProjects(TaskMonitor aTM)
    {
        boolean success = true;

        // Build child projects
        Project[] projects = getProjects();
        for (Project proj : projects) {
            if (!proj.buildProject(aTM)) {
                success = false;
                break;
            }
        }

        // Build main project
        if (success)
            _proj.buildProject(aTM);
    }

    /**
     * Returns a Java file for class name.
     */
    public WebFile getJavaFile(String aClassName)
    {
        // Check main project
        ProjectFiles projFiles = _proj.getProjectFiles();
        WebFile file = projFiles.getJavaFileForClassName(aClassName);
        if (file != null)
            return file;

        // Check subprojects
        Project[] projects = getProjects();
        for (Project proj : projects) {
            projFiles = proj.getProjectFiles();
            file = projFiles.getJavaFileForClassName(aClassName);
            if (file != null)
                return file;
        }

        // Return not found
        return null;
    }
}