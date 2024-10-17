/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.util.ArrayUtils;
import snap.web.WebFile;
import snap.web.WebSite;

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
        String sourcePath = buildFile.getSourcePath();
        if (!sourcePath.startsWith("/"))
            sourcePath = '/' + sourcePath;

        // Get dir file from Project.Site
        WebSite projSite = _proj.getSite();
        WebFile srcDir = projSite.createFileForPath(sourcePath, true);

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
        String buildPath = buildFile.getBuildPath();
        if (!buildPath.startsWith("/"))
            buildPath = '/' + buildPath;

        // Get dir file from Project.Site
        WebSite projSite = _proj.getSite();
        WebFile bldDir = projSite.createFileForPath(buildPath, true);

        // Set/return
        return _buildDir = bldDir;
    }

    /**
     * Returns the source file for given path.
     */
    public WebFile getSourceFileForPath(String aPath)
    {
        String sourceFilePath = getSourceFilePathForPath(aPath);
        WebSite projSite = _proj.getSite();
        return projSite.getFileForPath(sourceFilePath);
    }

    /**
     * Returns the build file for given path.
     */
    public WebFile getBuildFileForPath(String aPath)
    {
        String filePath = getBuildFilePathForPath(aPath);
        WebSite projSite = _proj.getSite();
        return projSite.getFileForPath(filePath);
    }

    /**
     * Returns the build file for given path.
     */
    public WebFile createBuildFileForPath(String aPath, boolean isDir)
    {
        String filePath = getBuildFilePathForPath(aPath);
        WebSite projSite = _proj.getSite();
        return projSite.createFileForPath(filePath, isDir);
    }

    /**
     * Returns a source file for class name.
     */
    public WebFile getSourceFileForClassName(String aClassName)
    {
        // Get base class name (strip inner class names)
        String className = aClassName;
        int inner = className.indexOf('$');
        if (inner > 0)
            className = className.substring(0, inner);

        // Get JavaFilePath
        String javaFilePath = '/' + className.replace('.', '/') + ".java";

        // Return Source file for JavaFilePath
        WebFile javaFile = getSourceFileForPath(javaFilePath);
        if (javaFile != null)
            return javaFile;

        // Try for Jepl file
        String jeplFilePath = '/' + className.replace('.', '/') + ".jepl";
        WebFile jeplFile = getSourceFileForPath(jeplFilePath);
        if (jeplFile != null)
            return jeplFile;

        // Try for JMD file
        String jmdFilePath = '/' + className.replace('.', '/') + ".jmd";
        return getSourceFileForPath(jmdFilePath);
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
        return getSourceFileForPath(javaFilePath);
    }

    /**
     * Returns a ClassFile for class name.
     */
    public WebFile getClassFileForClassName(String aClassName)
    {
        String classFilePath = '/' + aClassName.replace('.', '/') + ".class";
        return getBuildFileForPath(classFilePath);
    }

    /**
     * Returns the class file for a given Java file.
     */
    public WebFile getClassFileForJavaFile(WebFile aJavaFile)
    {
        // Get Class file path
        String javaFilePath = aJavaFile.getPath();
        String classFilePath = javaFilePath.replace(".java", ".class");
        if (javaFilePath.endsWith("jepl"))
            classFilePath = javaFilePath.replace(".jepl", ".class");
        else if (javaFilePath.endsWith("jmd"))
            classFilePath = javaFilePath.replace(".jmd", ".class");

        // Return class file for classFilePath
        return getBuildFileForPath(classFilePath);
    }

    /**
     * Returns all class files for a given Java file.
     */
    public WebFile[] getClassFilesForJavaFile(WebFile aFile)
    {
        // Get Class file - if missing, return empty list
        WebFile classFile = getClassFileForJavaFile(aFile);
        if (classFile == null || classFile.getBytes() == null)
            return new WebFile[0];

        // Get inner class file - if empty, return class file as array
        WebFile[] innerClassFiles = getInnerClassFilesForJavaFile(aFile);
        if (innerClassFiles.length == 0)
            return new WebFile[] { classFile };

        // Return classFile + innerClassFiles array
        return ArrayUtils.add(innerClassFiles, classFile, 0);
    }

    /**
     * Returns the inner class files for a given Java file.
     */
    public WebFile[] getInnerClassFilesForJavaFile(WebFile aFile)
    {
        // Get Class file - if missing, return empty list
        WebFile classFile = getClassFileForJavaFile(aFile);
        if (classFile == null || classFile.getBytes() == null)
            return new WebFile[0];

        // Get Package files
        WebFile pkgDir = classFile.getParent();
        WebFile[] pkgFiles = pkgDir.getFiles();

        // Return inner class files (start with 'class_name$' and end with '.class')
        String classNamePrefix = classFile.getSimpleName() + '$';
        return ArrayUtils.filter(pkgFiles, file -> file.getName().startsWith(classNamePrefix) && file.getFileType().equals("class"));
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

        String simplePath = getSimplePathForPath(filePath);
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
        String filePath = getSimplePathForPath(file.getPath());

        // Get package name by swapping slashes for dots
        return filePath.substring(1).replace('/', '.');
    }

    /**
     * Returns the given path stripped of SourcePath or BuildPath if file is in either.
     */
    private String getSimplePathForPath(String aPath)
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
     * Returns the source file for given path.
     */
    private String getSourceFilePathForPath(String aPath)
    {
        // Get path
        String sourceFilePath = aPath;

        // If Path in BuildDir, strip BuildPath
        String buildPath = getBuildDir().getDirPath();
        if (buildPath.length() > 1 && sourceFilePath.startsWith(buildPath))
            sourceFilePath = sourceFilePath.substring(buildPath.length() - 1);

        // If Path not in SourceDir, add SourcePath
        String sourcePath = getSourceDir().getPath();
        if (sourcePath.length() > 1 && !sourceFilePath.startsWith(sourcePath))
            sourceFilePath = sourcePath + sourceFilePath;

        // Return
        return sourceFilePath;
    }

    /**
     * Returns the build path for given file path.
     */
    private String getBuildFilePathForPath(String aPath)
    {
        // Get path
        String buildFilePath = aPath;

        // If path in SourceDir, strip SourcePath
        String sourceDirPath = getSourceDir().getDirPath();
        if (sourceDirPath.length() > 1 && buildFilePath.startsWith(sourceDirPath))
            buildFilePath = buildFilePath.substring(sourceDirPath.length() - 1);

        // If path not in BuildDir, add BuildPath
        String buildDirPath = getBuildDir().getPath();
        if (buildDirPath.length() > 1 && !buildFilePath.startsWith(buildDirPath))
            buildFilePath = buildDirPath + buildFilePath;

        // Return
        return buildFilePath;
    }
}
