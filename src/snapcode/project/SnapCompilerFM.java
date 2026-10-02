/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.web.WebFile;
import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A JavaFileManager subclass for SnapCompiler which forwards to standard fileManager for source and to classLoader for classes.
 */
public class SnapCompilerFM extends ForwardingJavaFileManager<JavaFileManager> {

    // The SnapCompiler
    private SnapCompiler _compiler;

    // The project
    private Project _proj;

    // A map of previously accessed SnapFileObjects for paths
    private Map<String, SnapCompilerJFO> _javaFileObjects = new HashMap<>();

    // The class loader to find project lib classes
    private ClassLoader _classLoader;

    // The base modules
    private static List<String> BASE_MODULE_NAMES = List.of("java.base", "java.prefs", "java.datatransfer", "java.desktop");

    /**
     * Constructor.
     */
    public SnapCompilerFM(SnapCompiler aCompiler, JavaFileManager aFileManager)
    {
        super(aFileManager);
        _compiler = aCompiler;
        _proj = _compiler._proj;
        _classLoader = _proj.createCompilerClassLoader();
    }

    /**
     * Override to return project src/bin files.
     */
    @Override
    public Iterable<JavaFileObject> list(Location aLoc, String packageName, Set<Kind> kinds, boolean recurse) throws IOException
    {
        //System.out.println("list: " + aLoc + ", kinds: " + kinds + ", recursive: " + recurse + ", package: " + packageName);

        // Handle modules (WebVM has these for some reason)
        if (isSystemModule(aLoc))
            return listSystemModule(aLoc, packageName, kinds, recurse);

        // If not CLASS_PATH or SOURCE_PATH, just return normal version
        if (aLoc != StandardLocation.CLASS_PATH && aLoc != StandardLocation.SOURCE_PATH)
            return super.list(aLoc, packageName, kinds, recurse);

        // If system path (package files were found), just return
        Iterable<JavaFileObject> iterable = super.list(aLoc, packageName, kinds, recurse);
        if (!packageName.isEmpty() && iterable.iterator().hasNext())
            return iterable;

        // If package not in project, just return
        if (getSourceDir(packageName) == null)
            return iterable;

        // Find source and class files
        List<JavaFileObject> filesList = new ArrayList<>();
        if (kinds.contains(Kind.SOURCE))
            findSourceFilesForPackageName(packageName, filesList);
        if (kinds.contains(Kind.CLASS))
            findClassFilesForPackageName(packageName, filesList);

        // Return
        return filesList;
    }

    /**
     * Special support for WebVM for SYSTEM_MODULES to remove module-info.class.
     */
    private Iterable<JavaFileObject> listSystemModule(Location aLoc, String packageName, Set<Kind> kinds, boolean recurse) throws IOException
    {
        // Do normal version - just return if not recursive
        Iterable<JavaFileObject> superFiles = super.list(aLoc, packageName, kinds, recurse);
        if (!recurse)
            return superFiles;

        // Create list copy and remove module-info.class
        List<JavaFileObject> moduleFiles = new ArrayList<>(); superFiles.forEach(moduleFiles::add);
        Iterable<JavaFileObject> moduleObject = super.list(aLoc, packageName, kinds, false);
        moduleObject.forEach(moduleFiles::remove);
        return moduleFiles;
    }

    /**
     * Override to filter uncommon modules.
     */
    @Override
    public Iterable<Set<Location>> listLocationsForModules(Location location) throws IOException
    {
        // If normal verion returns empty, just return
        Iterable<Set<Location>> superLocs = super.listLocationsForModules(location);
        if (!superLocs.iterator().hasNext())
            return superLocs;

        // Filter locations for modules to basic modules
        List<Set<Location>> locationsForModules = new ArrayList<>();
        for (Set<Location> set : superLocs) {
            Set<Location> set2 = set.stream().filter(SnapCompilerFM::isBasicSystemModule).collect(Collectors.toSet());
            locationsForModules.add(set2);
        }

        return locationsForModules;
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
     * Returns a JavaFleObject for given path (with option to provide file for efficiency).
     */
    synchronized SnapCompilerJFO getJavaFileObject(WebFile aFile)
    {
        // Get cached file for file path (just return if found)
        String filePath = aFile.getPath();
        SnapCompilerJFO javaFileObject = _javaFileObjects.get(filePath);
        if (javaFileObject != null)
            return javaFileObject;

        // Create java file object and add to cache
        javaFileObject = new SnapCompilerJFO(_proj, aFile, _compiler);
        _javaFileObjects.put(filePath, javaFileObject);

        return javaFileObject;
    }

    /**
     * Override to return Project.CompilerClassLoader.
     */
    @Override
    public ClassLoader getClassLoader(Location aLoc)  { return _classLoader; }

    /**
     * Return a FileObject for a given location from which compiler can obtain source or byte code.
     */
    @Override
    public FileObject getFileForInput(Location aLoc, String packageName, String aRelName) throws IOException
    {
        System.err.println("SnapCompilerFM:getFileForInput: " + packageName + "." + aRelName + ", loc: " + aLoc.getName());
        return super.getFileForInput(aLoc, packageName, aRelName);
    }

    /**
     * Return a FileObject for a given location from which compiler can obtain source or byte code.
     */
    @Override
    public JavaFileObject getJavaFileForInput(Location aLoc, String className, Kind kind)
    {
        //System.err.println("getJavaFileForInput: " + aClassName + ", kind: " + aKind);
        String sourceDirPath = _proj.getSourceDir().getDirPath();
        String javaFilePath = sourceDirPath + className.replace('.', '/') + ".java";
        WebFile javaFile = _proj.getFileForPath(javaFilePath);
        return javaFile != null ? getJavaFileObject(javaFile) : null;
    }

    /**
     * Create a JavaFileObject for an output class file and store it in the classloader.
     */
    @Override
    public JavaFileObject getJavaFileForOutput(Location aLoc, String className, Kind kind, FileObject aSibling)
    {
        WebFile javaFile = ((SnapCompilerJFO) aSibling).getFile();
        String classPath = "/" + className.replace('.', '/') + ".class";
        ProjectFiles projectFiles = _proj.getProjectFiles();
        WebFile classFile = projectFiles.createBuildFileForPath(classPath, false);
        SnapCompilerJFO javaFileObject = getJavaFileObject(classFile);
        javaFileObject._javaFile = javaFile;
        return javaFileObject;
    }

    /**
     * Finds source files for package name.
     */
    private void findSourceFilesForPackageName(String packageName, List<JavaFileObject> filesList)
    {
        WebFile packageDir = getSourceDir(packageName);
        findFilesForDirFileAndType(packageDir, "java", filesList);
    }

    /**
     * Finds class files for package name.
     */
    private void findClassFilesForPackageName(String packageName, List<JavaFileObject> filesList)
    {
        WebFile packageDir = getBuildDir(packageName);
        findFilesForDirFileAndType(packageDir, "class", filesList);
    }

    /**
     * Returns the WebFile (directory) for package name build files, if available.
     */
    private WebFile getBuildDir(String packageName)
    {
        WebFile buildDir = _proj.getBuildDir();
        if (packageName.isEmpty())
            return buildDir;

        String packagePath = '/' + packageName.replace('.', '/');
        return _proj.getProjectFiles().getBuildFileForPath(packagePath);
    }

    /**
     * Returns the WebFile (directory) for package name source files, if available.
     */
    private WebFile getSourceDir(String packageName)
    {
        WebFile sourceDir = _proj.getSourceDir();
        if (packageName.isEmpty())
            return sourceDir;

        String packagePath = '/' + packageName.replace('.', '/');
        return _proj.getSourceFileForPath(packagePath);
    }

    /**
     * Finds files in given dir file of given type and adds to given list.
     */
    private void findFilesForDirFileAndType(WebFile dirFile, String fileType, List<JavaFileObject> filesList)
    {
        if (dirFile == null)
            return;

        List<WebFile> dirFiles = dirFile.getFiles();
        for (WebFile file : dirFiles) {
            if (file.getFileType().equals(fileType)) {
                JavaFileObject javaFileObject = getJavaFileObject(file);
                filesList.add(javaFileObject);
            }
        }
    }

    /**
     * Returns whether given location is a system module.
     */
    private static boolean isSystemModule(Location aLoc)  { return aLoc.toString().startsWith("SYSTEM_MODULES["); }

    /**
     * Returns whether given location is a basic system module.
     */
    private static boolean isBasicSystemModule(Location aLoc)
    {
        if (!isSystemModule(aLoc))
            return false;
        String locStr = aLoc.toString();
        String moduleName = locStr.substring("SYSTEM_MODULES[".length(), locStr.length() - 1);
        return BASE_MODULE_NAMES.contains(moduleName);
    }
}