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
 * A Java File Manager class for Snap.
 */
public class SnapCompilerFM extends ForwardingJavaFileManager<JavaFileManager> {

    // The SnapCompiler
    protected SnapCompiler  _compiler;

    // The project
    private Project  _proj;

    // A map of previously accessed SnapFileObjects for paths
    private Map<String, SnapCompilerJFO>  _jfos = new HashMap<>();

    // The class loader to find project lib classes
    private ClassLoader  _classLoader;

    // Whether compile includes SnapKit
    protected boolean  _excludeSnapKit;

    /**
     * Construct a new FileManager which forwards to the <var>fileManager</var>
     * for source and to the <var>classLoader</var> for classes
     */
    public SnapCompilerFM(SnapCompiler aCompiler, JavaFileManager aFileManager)
    {
        // Do normal version and set vars
        super(aFileManager);
        _compiler = aCompiler;
        _proj = _compiler._proj;

        // Determine whether we're compiling something with SnapKit, so we can exclude it from standard ClassPath if not
        //_excludeSnapKit = _proj.getProjectSet().getProject("SnapKit")==null && !_proj.getName().equals("SnapKit");
    }

    /**
     * Override to return class loader.
     */
    public ClassLoader getClassLoader(Location aLoc)
    {
        // If already set, just return
        if (_classLoader != null) return _classLoader;

        // Get Project.CompilerClassLoader
        ClassLoader classLoader = _proj.createCompilerClassLoader();

        // Set/return
        return _classLoader = classLoader;
    }

    /**
     * Return a FileObject for a given location from which compiler can obtain source or byte code.
     */
    public FileObject getFileForInput(Location aLoc, String aPkgName, String aRelName) throws IOException
    {
        System.err.println("SnapCompilerFM:getFileForInput: " + aPkgName + "." + aRelName + ", loc: " + aLoc.getName());
        //FileObject o = _fileObjects.get(getURI(location, packageName, relativeName)); if(o!=null) return o;
        return super.getFileForInput(aLoc, aPkgName, aRelName);
    }

    /**
     * Return a FileObject for a given location from which compiler can obtain source or byte code.
     */
    public JavaFileObject getJavaFileForInput(Location aLoc, String aClassName, Kind aKind) throws IOException
    {
        System.err.println("getJavaFileForInput: " + aClassName + ", kind: " + aKind);
        String cpath = _proj.getSourceDir().getDirPath() + aClassName.replace('.', '/') + ".java";
        return getJFO(cpath, null);
    }

    /**
     * Create a JavaFileObject for an output class file and store it in the classloader.
     */
    public JavaFileObject getJavaFileForOutput(Location aLoc, String aClassName, Kind kind, FileObject aSblg)
    {
        WebFile javaFile = ((SnapCompilerJFO) aSblg).getFile();
        String classPath = "/" + aClassName.replace('.', '/') + ".class";
        ProjectFiles projectFiles = _proj.getProjectFiles();
        WebFile classFile = projectFiles.getBuildFile(classPath, true, false);
        SnapCompilerJFO jfo = getJFO(classFile.getPath(), classFile);
        jfo._sourceFile = javaFile;
        return jfo;
    }

    /**
     * Returns whether we have location.
     */
    public boolean hasLocation(Location aLoc)
    {
        if (aLoc == StandardLocation.SOURCE_PATH) return true;
        return super.hasLocation(aLoc);
    }

    /**
     * Override to handle JavaFileObjects (return the file's name).
     */
    public String inferBinaryName(Location aLoc, JavaFileObject aFile)
    {
        if (aFile instanceof SnapCompilerJFO) return ((SnapCompilerJFO) aFile).getBinaryName();
        return super.inferBinaryName(aLoc, aFile);
    }

    /**
     * Compare files.
     */
    public boolean isSameFile(FileObject a, FileObject b)
    {
        if (a == b) return true;
        if (a instanceof SnapCompilerJFO || b instanceof SnapCompilerJFO) return false;
        return super.isSameFile(a, b);
    }

    /**
     * Override to return project src/bin files.
     */
    public Iterable<JavaFileObject> list(Location aLoc, String aPkgName, Set<Kind> kinds, boolean doRcrs) throws IOException
    {
        // Do normal version
        Iterable<JavaFileObject> iterable = super.list(aLoc, aPkgName, kinds, doRcrs);

        // If not CLASS_PATH or SOURCE_PATH, just return
        if (aLoc != StandardLocation.CLASS_PATH && aLoc != StandardLocation.SOURCE_PATH)
            return iterable;

        // If we need to explicitly exclude SnapKit (because it's in the standard CLASS_PATH), return emtpy list
        if (_excludeSnapKit && aLoc == StandardLocation.CLASS_PATH && aPkgName.startsWith("snap."))
            return Collections.EMPTY_LIST;

        // If system path (package files were found), just return
        if (aPkgName.length() > 0 && iterable.iterator().hasNext())
            return iterable;

        // If known system path (java., javax., etc.), just return
        if (aPkgName.startsWith("java.") || aPkgName.startsWith("javax") || aPkgName.startsWith("javafx") ||
                aPkgName.startsWith("com.sun") || aPkgName.startsWith("sun.") || aPkgName.startsWith("org.xml"))
            return iterable;

        // Add Class files
        List<JavaFileObject> files = new ArrayList<>();
        if (kinds.contains(Kind.CLASS)) {
            WebFile pkgDir = getBuildDir(aPkgName);
            if (pkgDir != null)
                for (WebFile file : pkgDir.getFiles()) {
                    if (file.getType().equals("class"))
                        files.add(getJFO(file.getPath(), file));
                }
        }

        // Add Source files
        if (kinds.contains(Kind.SOURCE)) {
            WebFile pkgDir = getSourceDir(aPkgName);
            if (pkgDir != null)
                for (WebFile file : pkgDir.getFiles()) {
                    if (isJavaFile(file))
                        files.add(getJFO(file.getPath(), file));
                }
        }

        // Return files
        return files;
    }

    /**
     * Returns whether file is java file.
     */
    boolean isJavaFile(WebFile aFile)
    {
        return aFile.getType().equals("java");
    }

    /**
     * Returns the WebFile (directory) for package name build files, if available.
     */
    private WebFile getBuildDir(String aPackageName)
    {
        WebFile buildDir = _proj.getBuildDir();
        if (aPackageName.length() == 0)
            return buildDir;

        String dirname = aPackageName.replace('.', '/');
        ProjectFiles projectFiles = _proj.getProjectFiles();
        WebFile file = projectFiles.getBuildFile('/' + dirname, false, true); //buildDir.getFile(dirname);

        // Return
        return file;
    }

    /**
     * Returns the WebFile (directory) for package name source files, if available.
     */
    private WebFile getSourceDir(String aPackageName)
    {
        WebFile sourceDir = _proj.getSourceDir();
        if (aPackageName.length() == 0)
            return sourceDir;

        String dirname = aPackageName.replace('.', '/');
        return _proj.getSourceFile('/' + dirname, false, true); //sourceDir.getFile(dirname);
    }

    /**
     * Returns a JavaFleObject for given path (with option to provide file for efficiency).
     */
    public synchronized SnapCompilerJFO getJFO(String aPath, WebFile aFile)
    {
        SnapCompilerJFO jfo = _jfos.get(aPath);
        if (jfo == null) {
            WebFile dfile = aFile != null ? aFile : _proj.getFile(aPath);
            if (dfile != null)
                _jfos.put(aPath, jfo = new SnapCompilerJFO(_proj, dfile, _compiler));
        }

        // Return
        return jfo;
    }
}