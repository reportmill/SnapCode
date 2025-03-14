/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.util.*;
import snap.web.*;
import java.io.File;
import java.util.List;

/**
 * Utility methods for Project.
 */
public class ProjectUtils {

    // Constants
    public static final String JAVA_FILE_EXT = "java";
    public static final String JEPL_FILE_EXT = "jepl";
    public static final String[] FILE_TYPES = { JAVA_FILE_EXT, JEPL_FILE_EXT };

    /**
     * Returns whether given file can be opened by app (java, jepl, project).
     */
    public static boolean isValidOpenFile(WebFile aFile)
    {
        if (isSourceFile(aFile))
            return true;
        if (aFile.getFileType().equals("zip"))
            return true;
        if (aFile.getFileType().equals("gfar"))
            return true;
        if (isProjectBuildFile(aFile))
            return true;
        return isProjectDir(aFile);
    }

    /**
     * Returns whether given file is Java/Jepl.
     */
    public static boolean isSourceFile(WebFile aFile)
    {
        String fileType = aFile.getFileType();
        return ArrayUtils.contains(FILE_TYPES, fileType);
    }

    /**
     * Returns whether given file is build.snapcode.
     */
    public static boolean isProjectBuildFile(WebFile aFile)
    {
        // If BuildFile, return true
        String BUILD_FILE_NAME = "build.snapcode";
        if (aFile.getName().equals(BUILD_FILE_NAME))
            return true;

        // Return not project file
        return false;
    }

    /**
     * Returns whether given file is an existing project directory.
     */
    public static boolean isProjectDir(WebFile aFile)
    {
        // If is dir with BuildFile or source dir or source file, return true
        if (aFile.isDir()) {

            // If file doesn't exist, return false
            if (!aFile.getExists())
                return false;

            // If dir contains build file, src dir, or src file, return true
            List<WebFile> dirFiles = aFile.getFiles();
            String[] projectFileNames = { "build.snapcode", "src" };
            for (WebFile file : dirFiles) {
                if (ArrayUtils.contains(projectFileNames, file.getName()) || isSourceFile(file))
                    return true;
            }
        }

        // Return not project file
        return false;
    }

    /**
     * Returns a good default file.
     */
    public static WebFile getGoodDefaultFile(Project aProj)
    {
        // Look for project.greenfoot file
        WebFile greenfootFile = aProj.getFileForPath("/src/project.greenfoot");
        if (greenfootFile != null)
            return greenfootFile;

        // Look for 'readme' file
        WebFile projRootDir = aProj.getRootDir();
        List<WebFile> projRootFiles = projRootDir.getFiles();
        WebFile readMeFile = ListUtils.findMatch(projRootFiles, file -> file.getSimpleName().equalsIgnoreCase("readme"));
        if (readMeFile != null)
            return readMeFile;

        // Look for first defined main class file
        String mainClassName = aProj.getBuildFile().getMainClassName();
        if (mainClassName != null) {
            WebFile mainClassFile = aProj.getJavaFileForClassName(mainClassName);
            if (mainClassFile != null)
                return mainClassFile;
        }

        // Return first source file
        WebFile srcDir = aProj.getSourceDir();
        for (WebFile file : srcDir.getFiles()) {
            if (isSourceFile(file))
                return file;
        }

        // Return project root
        return aProj.getRootDir();
    }

    /**
     * Returns the project root path.
     */
    public static String getRootDirPath(Project aProj)
    {
        // If Project not on local file system, return null
        WebSite projSite = aProj.getSite();
        WebURL projURL = projSite.getURL();
        String scheme = projURL.getScheme();
        if (!scheme.equals("file"))
            return null;

        // Get project root dir path
        WebFile rootDir = projSite.getRootDir();
        File rootDirFile = rootDir.getJavaFile();
        String rootDirPath = rootDirFile.getAbsolutePath();

        // Make sure separator is standard '/'
        if (File.separatorChar != '/')
            rootDirPath = rootDirPath.replace(File.separatorChar, '/');

        // Make sure path ends with dir char
        if (!rootDirPath.endsWith("/"))
            rootDirPath = rootDirPath + '/';

        // Make sure path start with dir char
        if (!rootDirPath.startsWith("/"))
            rootDirPath = '/' + rootDirPath;

        // Return
        return rootDirPath;
    }

    /**
     * Returns an absolute path for given relative path, with option to add trailing dir char ('/').
     */
    public static String getAbsolutePath(Project aProj, String aPath, boolean isDir)
    {
        // If path missing root dir path, add it
        String absPath = aPath;
        if (!absPath.startsWith("/")) {
            String rootPath = getRootDirPath(aProj);
            if (rootPath != null)
                absPath = rootPath + absPath;
            else {
                //System.err.println("ProjectUtils.getAbsolutePath: Can't find project root path for: " + aProj.getSite().getURLString());
                return null;
            }
        }

        // Handle dir (except .jar/.zip)
        if (isDir) {
            boolean exclude = StringUtils.endsWithIC(aPath, ".jar") || StringUtils.endsWithIC(aPath, ".zip") || aPath.endsWith("/");
            if (!exclude)
                absPath += '/';
        }

        // Return
        return absPath;
    }

    /**
     * Returns a relative path for given path.
     */
    public static String getRelativePath(Project aProj, String aPath)
    {
        // Get filePath
        String filePath = aPath;

        // Make sure separator is standard '/'
        if (File.separatorChar != '/')
            filePath = filePath.replace(File.separatorChar, '/');

        // If missing root (already relative), just return
        if (!aPath.startsWith("/"))
            return filePath;

        // If filePath starts with root dir path, strip it
        String rootDirPath = getRootDirPath(aProj);
        if (rootDirPath != null && filePath.startsWith(rootDirPath))
            filePath = filePath.substring(rootDirPath.length());

        // Return
        return filePath;
    }

    /**
     * Returns the URL of source code for given project and class path.
     */
    public static String getSourceCodeUrlForClassPath(Project aProject, String classPath)
    {
        if (classPath.startsWith("/java/") || classPath.startsWith("/javax/"))
            return "https://reportmill.com/jars/8u05/src.zip!" + classPath;

        // Look in project
        WebFile file = aProject.getSourceFileForPath(classPath);
        if (file != null)
            return file.getUrlAddress();

        // Look in child projects
        List<Project> projects = aProject.getProjects();
        for (Project proj : projects) {
            file = proj.getSourceFileForPath(classPath);
            if (file != null)
                return file.getUrlAddress();
        }

        // Return not found
        return classPath;
    }

    /**
     * Returns a temp project for given workspace.
     */
    public static Project getTempProject(Workspace workspace)
    {
        return getTempProjectForName(workspace, "TempProj");
    }

    /**
     * Returns a temp project for given workspace.
     */
    public static Project getTempProjectForName(Workspace workspace, String projectName)
    {
        // Get URL and Site for TempProjPath
        WebSite tempProjSite = getTempProjectSiteForProjectName(projectName);

        // Get project for site - create if missing
        Project tempProj = Project.getProjectForSite(tempProjSite);
        if (tempProj != null)
            return tempProj;

        // Delete site root directory
        WebFile tempProjRootDir = tempProjSite.getRootDir();
        if (tempProjRootDir.getExists())
            tempProjRootDir.delete();

        // Create new temp project
        tempProj = workspace.openProjectForSite(tempProjSite);
        tempProj.getBuildFile().setIncludeSnapKitRuntime(true);
        tempProj.getBuildFile().writeFile();

        // Return
        return tempProj;
    }

    /**
     * Returns a temp project site for given workspace and project name.
     */
    public static WebSite getTempProjectSiteForProjectName(String projectName)
    {
        // Get path to temp dir named TempProj
        String tempProjPath = FileUtils.getTempFile(projectName).getAbsolutePath();
        if (SnapUtils.isMac)
            tempProjPath = "/tmp/" + projectName;

        // Get URL and Site for TempProjPath
        WebURL tempProjURL = WebURL.getURL(tempProjPath);
        assert (tempProjURL != null);
        return tempProjURL.getAsSite();
    }

    /**
     * Returns a class path for given class.
     */
    public static String getClassPathForClass(Class<?> aClass)
    {
        // Get URL and Site
        WebURL url = WebURL.getURL(aClass);

        // If URL string has separator, use site
        assert (url != null);
        String urlString = url.getString();
        if (urlString.contains("!/")) {
            WebSite site = url.getSite();
            return site.getPath();
        }

        // Otherwise, trim className from path
        String path = url.getPath();
        int classNameLen = aClass.getName().length() + ".class".length() + 1;
        if (path.length() > classNameLen)
            return path.substring(0, path.length() - classNameLen);

        // Express concern and return null
        System.out.println("ProjectUtils.getClassPathForClass: Unexpected class url: " + url);
        return null;
    }

    /**
     * Returns SnapKit and SnapCharts class paths.
     */
    public static String[] getSnapKitAndSnapChartsClassPaths(boolean includeGreenfoot)
    {
        // Get SnapKit class path
        String snapKitClassPath = getClassPathForClass(snap.view.View.class);
        String[] snapClassPaths = new String[] { snapKitClassPath };

        // Get Greenfoot class path (different if running from project build dir)
        if (includeGreenfoot) {
            String greenfootClassPath = getClassPathForClass(greenfoot.Greenfoot.class);
            assert (greenfootClassPath != null);
            snapClassPaths = ArrayUtils.add(snapClassPaths, greenfootClassPath);
        }

        // Iterate over paths and add "/resources" if "/classes" found
        for (String snapClassPath : snapClassPaths) {
            if (snapClassPath.contains("/classes"))
                snapClassPaths = ArrayUtils.add(snapClassPaths, snapClassPath.replace("/classes", "/resources"));
        }

        // Return
        return snapClassPaths;
    }
}
