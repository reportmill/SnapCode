/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.util.ArrayUtils;
import snap.util.FileUtils;
import snap.util.SnapUtils;
import snap.util.StringUtils;
import snap.web.*;

import java.io.File;

/**
 * Utility methods for Project.
 */
public class ProjectUtils {

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
        if (classPath.startsWith("/javafx/"))
            return "https://reportmill.com/jars/8u05/javafx-src.zip!" + classPath;

        // Look in project
        WebFile file = aProject.getSourceFile(classPath, false, false);
        if (file != null)
            return file.getUrlString();

        // Look in child projects
        Project[] projects = aProject.getProjects();
        for (Project proj : projects) {
            file = proj.getSourceFile(classPath, false, false);
            if (file != null)
                return file.getUrlString();
        }

        // Return not found
        return classPath;
    }

    /**
     * Returns a temp project for given workspace.
     */
    public static Project getTempProject(Workspace workspace)
    {
        // Get path to temp dir named TempProj
        String tempProjPath = FileUtils.getTempFile("TempProj").getAbsolutePath();
        if (SnapUtils.isMac)
            tempProjPath = "/tmp/TempProj";

        // Get URL and Site for TempProjPath
        WebURL tempProjURL = WebURL.getURL(tempProjPath);
        assert (tempProjURL != null);
        WebSite tempProjSite = tempProjURL.getAsSite();

        // Get project for site - create if missing
        Project tempProj = Project.getProjectForSite(tempProjSite);
        if (tempProj != null)
            return tempProj;

        // Delete old version
        deleteTempProject();

        // Create new temp project
        tempProj = workspace.addProjectForSite(tempProjSite);
        tempProj.getBuildFile().setIncludeSnapKitRuntime(true);
        tempProj.getBuildFile().writeFile();

        // Return
        return tempProj;
    }

    /**
     * Deletes the temp project.
     */
    public static void deleteTempProject()
    {
        // Get path to temp dir named TempProj
        String tempProjPath = FileUtils.getTempFile("TempProj").getAbsolutePath();
        if (SnapUtils.isMac)
            tempProjPath = "/tmp/TempProj";

        // Get URL and Site for TempProjPath
        WebURL tempProjURL = WebURL.getURL(tempProjPath);
        assert (tempProjURL != null);
        WebFile tempProjFile = tempProjURL.getFile();
        if (tempProjFile != null)
            tempProjFile.delete();
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
    public static String[] getSnapKitAndSnapChartsClassPaths()
    {
        // Get SnapKit class path
        String snapKitClassPath = getClassPathForClass(snap.view.View.class);
        String[] snapClassPaths = new String[] { snapKitClassPath };

        // Get SnapCharts class path (different if running from project build dir)
        String snapChartsClassPath = getClassPathForClass(snapcharts.repl.ReplObject.class);
        assert (snapChartsClassPath != null);
        if (!snapChartsClassPath.equals(snapKitClassPath))
            snapClassPaths = ArrayUtils.add(snapClassPaths, snapChartsClassPath);

        // Iterate over paths and add "/resources" if "/classes" found
        for (String snapClassPath : snapClassPaths) {
            if (snapClassPath.contains("/classes"))
                snapClassPaths = ArrayUtils.add(snapClassPaths, snapClassPath.replace("/classes", "/resources"));
        }

        // Return
        return snapClassPaths;
    }
}
