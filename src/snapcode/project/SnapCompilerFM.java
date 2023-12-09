/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.web.WebFile;
import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.IOException;
import java.util.*;

/**
 * A JavaFileManager subclass for SnapCompiler which forwards to standard fileManager for source and to classLoader for classes.
 */
public class SnapCompilerFM extends ForwardingJavaFileManager<JavaFileManager> {

    // The SnapCompiler
    protected SnapCompiler  _compiler;

    // The project
    private Project  _proj;

    // A map of previously accessed SnapFileObjects for paths
    private Map<String, SnapCompilerJFO> _javaFileObjects = new HashMap<>();

    // The class loader to find project lib classes
    private ClassLoader  _classLoader;

    /**
     * Constructor.
     */
    public SnapCompilerFM(SnapCompiler aCompiler, JavaFileManager aFileManager)
    {
        super(aFileManager);
        _compiler = aCompiler;
        _proj = _compiler._proj;
    }

    /**
     * Returns a JavaFleObject for given path (with option to provide file for efficiency).
     */
    public synchronized SnapCompilerJFO getJavaFileObject(WebFile aFile)
    {
        // Get cached file for file path (just return if found)
        String filePath = aFile.getPath();
        SnapCompilerJFO javaFileObject = _javaFileObjects.get(filePath);
        if (javaFileObject != null)
            return javaFileObject;

        // Create java file object and add to cache
        javaFileObject = new SnapCompilerJFO(_proj, aFile, _compiler);
        _javaFileObjects.put(filePath, javaFileObject);

        // Return
        return javaFileObject;
    }

    /**
     * Override to return project src/bin files.
     */
    @Override
    public Iterable<JavaFileObject> list(Location aLoc, String aPkgName, Set<Kind> kinds, boolean doRcrs) throws IOException
    {
        // Do normal version
        Iterable<JavaFileObject> iterable = super.list(aLoc, aPkgName, kinds, doRcrs);

        // If not CLASS_PATH or SOURCE_PATH, just return
        if (aLoc != StandardLocation.CLASS_PATH && aLoc != StandardLocation.SOURCE_PATH)
            return iterable;

        // If we need to explicitly exclude SnapKit (because it's in the standard CLASS_PATH), return empty list
        //if (_excludeSnapKit && aLoc == StandardLocation.CLASS_PATH && aPkgName.startsWith("snap.")) return Collections.EMPTY_LIST;

        // If system path (package files were found), just return
        if (aPkgName.length() > 0 && iterable.iterator().hasNext())
            return iterable;

        // If known system path (java., javax., etc.), just return
        if (aPkgName.startsWith("java.") || aPkgName.startsWith("javax") || aPkgName.startsWith("javafx") ||
                aPkgName.startsWith("com.sun") || aPkgName.startsWith("sun.") || aPkgName.startsWith("org.xml"))
            return iterable;

        // Find source and class files
        List<JavaFileObject> filesList = new ArrayList<>();
        if (kinds.contains(Kind.SOURCE))
            findSourceFilesForPackageName(aPkgName, filesList);
        if (kinds.contains(Kind.CLASS))
            findClassFilesForPackageName(aPkgName, filesList);

        // Return
        return filesList;
    }

    /**
     * Finds source files for package name.
     */
    private void findSourceFilesForPackageName(String aPkgName, List<JavaFileObject> filesList)
    {
        WebFile pkgDir = getSourceDir(aPkgName);
        findFilesForDirFileAndType(pkgDir, "java", filesList);
    }

    /**
     * Finds class files for package name.
     */
    private void findClassFilesForPackageName(String aPkgName, List<JavaFileObject> filesList)
    {
        WebFile pkgDir = getBuildDir(aPkgName);
        findFilesForDirFileAndType(pkgDir, "class", filesList);
    }

    /**
     * Override to return Project.CompilerClassLoader.
     */
    @Override
    public ClassLoader getClassLoader(Location aLoc)
    {
        if (_classLoader != null) return _classLoader;
        ClassLoader classLoader = _proj.createCompilerClassLoader();
        return _classLoader = classLoader;
    }

    /**
     * Return a FileObject for a given location from which compiler can obtain source or byte code.
     */
    @Override
    public FileObject getFileForInput(Location aLoc, String aPkgName, String aRelName) throws IOException
    {
        System.err.println("SnapCompilerFM:getFileForInput: " + aPkgName + "." + aRelName + ", loc: " + aLoc.getName());
        //FileObject o = _fileObjects.get(getURI(location, packageName, relativeName)); if(o!=null) return o;
        return super.getFileForInput(aLoc, aPkgName, aRelName);
    }

    /**
     * Return a FileObject for a given location from which compiler can obtain source or byte code.
     */
    @Override
    public JavaFileObject getJavaFileForInput(Location aLoc, String aClassName, Kind aKind)
    {
        System.err.println("getJavaFileForInput: " + aClassName + ", kind: " + aKind);
        String sourceDirPath = _proj.getSourceDir().getDirPath();
        String javaFilePath = sourceDirPath + aClassName.replace('.', '/') + ".java";
        WebFile javaFile = _proj.getFile(javaFilePath);
        return javaFile != null ? getJavaFileObject(javaFile) : null;
    }

    /**
     * Create a JavaFileObject for an output class file and store it in the classloader.
     */
    @Override
    public JavaFileObject getJavaFileForOutput(Location aLoc, String aClassName, Kind kind, FileObject aSblg)
    {
        WebFile javaFile = ((SnapCompilerJFO) aSblg).getFile();
        String classPath = "/" + aClassName.replace('.', '/') + ".class";
        ProjectFiles projectFiles = _proj.getProjectFiles();
        WebFile classFile = projectFiles.getBuildFile(classPath, true, false);
        SnapCompilerJFO jfo = getJavaFileObject(classFile);
        jfo._sourceFile = javaFile;
        return jfo;
    }

    /**
     * Returns whether we have location.
     */
    @Override
    public boolean hasLocation(Location aLoc)
    {
        if (aLoc == StandardLocation.SOURCE_PATH)
            return true;
        return super.hasLocation(aLoc);
    }

    /**
     * Override to handle JavaFileObjects (return the file's name).
     */
    @Override
    public String inferBinaryName(Location aLoc, JavaFileObject aFile)
    {
        if (aFile instanceof SnapCompilerJFO)
            return ((SnapCompilerJFO) aFile).getBinaryName();
        return super.inferBinaryName(aLoc, aFile);
    }

    /**
     * Compare files.
     */
    @Override
    public boolean isSameFile(FileObject file1, FileObject file2)
    {
        if (file1 == file2)
            return true;
        if (file1 instanceof SnapCompilerJFO || file2 instanceof SnapCompilerJFO)
            return false;
        return super.isSameFile(file1, file2);
    }

    /**
     * Returns the WebFile (directory) for package name build files, if available.
     */
    private WebFile getBuildDir(String aPackageName)
    {
        WebFile buildDir = _proj.getBuildDir();
        if (aPackageName.length() == 0)
            return buildDir;

        String pkgPath = '/' + aPackageName.replace('.', '/');
        return _proj.getProjectFiles().getBuildFile(pkgPath, false, true); //buildDir.getFile(dirname);
    }

    /**
     * Returns the WebFile (directory) for package name source files, if available.
     */
    private WebFile getSourceDir(String aPackageName)
    {
        WebFile sourceDir = _proj.getSourceDir();
        if (aPackageName.length() == 0)
            return sourceDir;

        String pkgPath = '/' + aPackageName.replace('.', '/');
        return _proj.getSourceFile(pkgPath, false, true);
    }

    /**
     * Finds files in given dir file of given type and adds to given list.
     */
    private void findFilesForDirFileAndType(WebFile dirFile, String fileType, List<JavaFileObject> filesList)
    {
        if (dirFile == null)
            return;

        WebFile[] dirFiles = dirFile.getFiles();
        for (WebFile file : dirFiles) {
            if (file.getType().equals(fileType)) {
                JavaFileObject javaFileObject = getJavaFileObject(file);
                filesList.add(javaFileObject);
            }
        }
    }
}