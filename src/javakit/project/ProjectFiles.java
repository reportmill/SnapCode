/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import snap.web.WebFile;
import snap.web.WebSite;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages files for a project.
 */
public class ProjectFiles {

    // The Project
    private Project  _proj;

    // The project source directory
    protected WebFile  _srcDir;

    // The project build directory
    protected WebFile  _buildDir;

    /**
     * Constructor.
     */
    public ProjectFiles(Project aProject)
    {
        _proj = aProject;
    }

    /**
     * Returns the source root directory.
     */
    public WebFile getSourceDir()
    {
        // If already set, just return
        if (_srcDir != null) return _srcDir;

        // Get SourcePath
        BuildFile buildFile = _proj.getBuildFile();
        String path = buildFile.getSourcePath();
        if (path != null && !path.startsWith("/"))
            path = '/' + path;

        // Get dir file from Project.Site
        WebSite projSite = _proj.getSite();
        WebFile srcDir = path != null ? projSite.getFileForPath(path) : projSite.getRootDir();
        if (srcDir == null)
            srcDir = projSite.createFileForPath(path, true);

        // Set/return
        return _srcDir = srcDir;
    }

    /**
     * Returns the build directory.
     */
    public WebFile getBuildDir()
    {
        // If already set, just return
        if (_buildDir != null) return _buildDir;

        // Get from BuildPath and site
        BuildFile buildFile = _proj.getBuildFile();
        String path = buildFile.getBuildPath();
        if (path != null && !path.startsWith("/"))
            path = '/' + path;

        // Get dir file from Project.Site
        WebSite projSite = _proj.getSite();
        WebFile bldDir = path != null ? projSite.getFileForPath(path) : projSite.getRootDir();
        if (bldDir == null)
            bldDir = projSite.createFileForPath(path, true);

        // Set/return
        return _buildDir = bldDir;
    }

    /**
     * Returns the source file for given path.
     */
    public WebFile getSourceFile(String aPath, boolean doCreate, boolean isDir)
    {
        // Get path
        String path = aPath;

        // If Path in BuildDir, strip BuildPath
        String buildPath = getBuildDir().getDirPath();
        if (buildPath.length() > 1 && path.startsWith(buildPath))
            path = path.substring(buildPath.length() - 1);

        // If Path not in SourceDir, add SourcePath
        String sourcePath = getSourceDir().getPath();
        if (sourcePath.length() > 1 && !path.startsWith(sourcePath))
            path = sourcePath + path;

        // Get file from site
        WebSite projSite = _proj.getSite();
        WebFile file = projSite.getFileForPath(path);

        // If file still not found, maybe create
        if (file == null && doCreate)
            file = projSite.createFileForPath(path, isDir);

        // Return
        return file;
    }

    /**
     * Returns the build file for given path.
     */
    public WebFile getBuildFile(String aPath, boolean doCreate, boolean isDir)
    {
        // Look for file in build dir
        String path = aPath;

        // If Path in SourceDir, strip SourcePath
        String sourcePath = getSourceDir().getDirPath();
        if (sourcePath.length() > 1 && path.startsWith(sourcePath))
            path = path.substring(sourcePath.length() - 1);

        // If Path not in BuildDir, add BuildPath
        WebFile buildDir = getBuildDir();
        String buildPath = buildDir.getPath();
        if (buildPath.length() > 1 && !path.startsWith(buildPath))
            path = buildPath + path;

        // Get file from site
        WebSite projSite = _proj.getSite();
        WebFile file = projSite.getFileForPath(path);

        // If file still not found, maybe create and return
        if (file == null && doCreate)
            file = projSite.createFileForPath(path, isDir);

        // Return
        return file;
    }

    /**
     * Returns a Java file for class name.
     */
    public WebFile getJavaFileForClassName(String aClassName)
    {
        // Get base class name (strip inner class names)
        String className = aClassName;
        int inner = className.indexOf('$');
        if (inner > 0)
            className = className.substring(0, inner);

        // Get JavaFilePath
        String javaFilePath = '/' + className.replace('.', '/') + ".java";

        // Return Source file for JavaFilePath
        return getSourceFile(javaFilePath, false, false);
    }

    /**
     * Returns the Java for a class file, if it can be found.
     */
    public WebFile getJavaFileForClassFile(WebFile aClassFile)
    {
        String classFilePath = aClassFile.getPath();
        String javaFilePath = classFilePath.replace(".class", ".java");

        // Strip inner class names
        int inner = javaFilePath.indexOf('$');
        if (inner > 0) {
            javaFilePath = javaFilePath.substring(0, inner);
            javaFilePath += ".java";
        }

        // Return Source file for JavaFilePath
        return getSourceFile(javaFilePath, false, false);
    }

    /**
     * Returns a ClassFile for class name.
     */
    public WebFile getClassFileForClassName(String aClassName)
    {
        // Get class file path from class name
        String classFilePath = '/' + aClassName.replace('.', '/') + ".class";

        // Return build file for classFilePath
        return getBuildFile(classFilePath, false, false);
    }

    /**
     * Returns the class file for a given Java file.
     */
    public WebFile getClassFileForJavaFile(WebFile aJavaFile)
    {
        // Get Class file path
        String javaFilePath = aJavaFile.getPath();
        String classFilePath = javaFilePath.replace(".java", ".class");

        // Return class file for classFilePath
        return getBuildFile(classFilePath, true, false);
    }

    /**
     * Returns the class files for a given Java file.
     */
    public WebFile[] getClassFilesForJavaFile(WebFile aFile)
    {
        // Get Class file
        WebFile classFile = getClassFileForJavaFile(aFile);
        if (classFile.getBytes() == null)
            return null;

        // Create list to hold files with at least Class file
        List<WebFile> classFiles = new ArrayList<>();
        classFiles.add(classFile);

        // Get Package dir files
        WebFile pkgDir = classFile.getParent();
        WebFile[] pkgDirFiles = pkgDir.getFiles();

        // Get prefix that inner class files should have
        String classNamePrefix = classFile.getSimpleName() + '$';

        // Iterate over package dir files and add .class files that match <ClassName$...>
        for (WebFile file : pkgDirFiles)
            if (file.getType().equals("class") && file.getName().startsWith(classNamePrefix))
                classFiles.add(file);

        // Return
        return classFiles.toArray(new WebFile[0]);
    }

    /**
     * Returns the given path stripped of SourcePath or BuildPath if file is in either.
     */
    public String getSimplePath(String aPath)
    {
        // Get path (make sure it's a path) and get SourcePath/BuildPath
        String path = aPath.startsWith("/") ? aPath : "/" + aPath;
        String sourcePath = getSourceDir().getPath();
        String buildPath = getBuildDir().getPath();

        // If in SourcePath (or is SourcePath) strip SourcePath prefix
        if (sourcePath.length() > 1 && path.startsWith(sourcePath)) {
            if (path.length() == sourcePath.length())
                path = "/";
            else if (path.charAt(sourcePath.length()) == '/')
                path = path.substring(sourcePath.length());
        }

        // If in BuildPath (or is BuildPath) strip BuildPath prefix
        else if (buildPath.length() > 1 && path.startsWith(buildPath)) {
            if (path.length() == sourcePath.length())
                path = "/";
            else if (path.charAt(buildPath.length()) == '/')
                path = path.substring(buildPath.length());
        }

        // Return path
        return path;
    }

    /**
     * Returns the class name for given class file.
     */
    public String getClassNameForFile(WebFile aFile)
    {
        // If directory, assume we really want package name
        if (aFile.isDir())
            return getPackageNameForFile(aFile);

        // Get File.Path
        String filePath = aFile.getPath();

        // Strip extension
        int dotIndex = filePath.lastIndexOf('.');
        if (dotIndex > 0)
            filePath = filePath.substring(0, dotIndex);

        String simplePath = getSimplePath(filePath);
        return simplePath.substring(1).replace('/', '.');
    }

    /**
     * Returns the package name for given source file.
     */
    public String getPackageNameForFile(WebFile aFile)
    {
        // Make sure we have dir file with no '.' in name (those are more likely resource dirs)
        WebFile file = aFile;
        while (file.isFile() || file.getName().indexOf('.') >= 0)
            file = file.getParent();

        // Get Path at SourcePath root
        String filePath = getSimplePath(file.getPath());

        // Get package name by swapping slashes for dots
        String packageName = filePath.substring(1).replace('/', '.');
        return packageName;
    }
}
