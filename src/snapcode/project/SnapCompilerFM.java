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
    protected SnapCompiler  _compiler;

    // The project
    private Project  _proj;

    // A map of previously accessed SnapFileObjects for paths
    private Map<String, SnapCompilerJFO> _javaFileObjects = new HashMap<>();

    // The class loader to find project lib classes
    private ClassLoader  _classLoader;

    // The base modules
    private static List<String> BASE_MODULE_NAMES = List.of("java.base", "java.prefs", "java.datatransfer", "java.desktop");

    // Cache of module JavaFileObjects
    private static Map<String,List<JavaFileObject>> _moduleFileObjects = new HashMap<>();

    // Cache of package JavaFileObjects
    private static Map<String,List<JavaFileObject>> _packageFileObjects = new HashMap<>();

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
    public Iterable<JavaFileObject> list(Location aLoc, String packageName, Set<Kind> kinds, boolean doRcrs) throws IOException
    {
        // Handle modules
        if (aLoc.toString().startsWith("SYSTEM_MODULES["))
            return listModuleFiles(aLoc, packageName, kinds, doRcrs);

        // If not CLASS_PATH or SOURCE_PATH, just return normal version
        if (aLoc != StandardLocation.CLASS_PATH && aLoc != StandardLocation.SOURCE_PATH)
            return super.list(aLoc, packageName, kinds, doRcrs);

        // If system path (package files were found), just return
        Iterable<JavaFileObject> iterable = super.list(aLoc, packageName, kinds, doRcrs);
        if (!packageName.isEmpty() && iterable.iterator().hasNext())
            return iterable;

        // If known system path (java., javax., etc.), just return
        if (packageName.startsWith("java.") || packageName.startsWith("javax") || packageName.startsWith("javafx") ||
                packageName.startsWith("com.sun") || packageName.startsWith("sun.") || packageName.startsWith("org.xml"))
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
     * Return JavaFileObjects for module location and package.
     */
    private List<JavaFileObject> listModuleFiles(Location aLoc, String packageName, Set<Kind> kinds, boolean doRcrs) throws IOException
    {
        // Ignore non base modules
        if (!isBasicModule(aLoc))
            return Collections.emptyList();

        // Get cache key and cache map for location
        String locStr = aLoc.toString();
        String moduleName = locStr.substring("SYSTEM_MODULES[".length(), locStr.length() - "]".length());
        String cacheKey = packageName.isEmpty() ? moduleName : packageName;
        Map<String,List<JavaFileObject>> cacheMap = doRcrs ? _moduleFileObjects : _packageFileObjects;

        // If already cached, just return
        List<JavaFileObject> moduleFiles = cacheMap.get(cacheKey);
        if (moduleFiles != null)
            return moduleFiles;

        // Do normal version
        Iterable<JavaFileObject> superFiles = super.list(aLoc, packageName, kinds, doRcrs);
        moduleFiles = new ArrayList<>(); superFiles.forEach(moduleFiles::add);

        // If root package, remove module-info.class
        if (packageName.isEmpty() && doRcrs) {
            Iterable<JavaFileObject> moduleObject = super.list(aLoc, packageName, kinds, false);
            moduleObject.forEach(moduleFiles::remove);
        }

        // Cache and return
        cacheMap.put(packageName, moduleFiles);
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
            Set<Location> set2 = set.stream().filter(SnapCompilerFM::isBasicModule).collect(Collectors.toSet());
            locationsForModules.add(set2);
        }

        // Return
        return locationsForModules;
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
        //System.err.println("getJavaFileForInput: " + aClassName + ", kind: " + aKind);
        String sourceDirPath = _proj.getSourceDir().getDirPath();
        String javaFilePath = sourceDirPath + aClassName.replace('.', '/') + ".java";
        WebFile javaFile = _proj.getFileForPath(javaFilePath);
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
        WebFile classFile = projectFiles.createBuildFileForPath(classPath, false);
        SnapCompilerJFO jfo = getJavaFileObject(classFile);
        jfo._javaFile = javaFile;
        return jfo;
    }

    /**
     * Returns whether we have location.
     */
    @Override
    public boolean hasLocation(Location aLoc)
    {
        if (aLoc == StandardLocation.SOURCE_PATH || aLoc == StandardLocation.CLASS_PATH)
            return true;
        if (isSystemModule(aLoc) && !isBasicModule(aLoc))
            return false;
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
        if (aPackageName.isEmpty())
            return buildDir;

        String pkgPath = '/' + aPackageName.replace('.', '/');
        return _proj.getProjectFiles().getBuildFileForPath(pkgPath);
    }

    /**
     * Returns the WebFile (directory) for package name source files, if available.
     */
    private WebFile getSourceDir(String aPackageName)
    {
        WebFile sourceDir = _proj.getSourceDir();
        if (aPackageName.isEmpty())
            return sourceDir;

        String pkgPath = '/' + aPackageName.replace('.', '/');
        return _proj.getSourceFileForPath(pkgPath);
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
     * Returns whether given location is system module.
     */
    private static boolean isSystemModule(Location aLoc)  { return aLoc.toString().startsWith("SYSTEM_MODULES["); }

    /**
     * Returns whether given location is basic module.
     */
    private static boolean isBasicModule(Location location)
    {
        String locStr = location.toString();
        if (!locStr.startsWith("SYSTEM_MODULES["))
            return false;
        String moduleName = locStr.substring("SYSTEM_MODULES[".length(), locStr.length() - "]".length());
        return BASE_MODULE_NAMES.contains(moduleName);
    }
}