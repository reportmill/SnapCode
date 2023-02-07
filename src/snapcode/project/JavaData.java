package snapcode.project;
import javakit.project.ProjectFiles;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaClass;
import javakit.resolver.Resolver;
import snap.web.WebFile;
import java.util.*;

/**
 * A file object for managing Java files.
 */
public class JavaData {

    // The java file
    private WebFile  _javaFile;

    // The Project that owns this file
    private ProjectX _proj;

    // The set of declarations in this JavaFile
    private Set<JavaDecl>  _decls = new HashSet<>();

    // The set of references in this JavaFile
    private Set<JavaDecl>  _refs = new HashSet<>();

    // The set of files that our file depends on
    private Set<WebFile>  _dependencies = new HashSet<>();

    // The set of files that depend on our file
    private Set<WebFile>  _dependents = new HashSet<>();

    // Whether Dependencies have been set - should  maybe change to NeedsDependenciesUpdate
    private boolean  _isDependenciesSet;

    /**
     * Creates a new JavaData for given file.
     */
    public JavaData(WebFile aFile)
    {
        _javaFile = aFile;
    }

    /**
     * Returns the project for this JavaFile.
     */
    public ProjectX getProject()
    {
        // If already set, just return
        if (_proj != null) return _proj;

        // Get, set, return
        ProjectX proj = ProjectX.getProjectForFile(_javaFile);
        return _proj = proj;
    }

    /**
     * Returns the class files for this JavaFile.
     */
    public WebFile[] getClassFiles()
    {
        ProjectX proj = getProject();
        ProjectFiles projectFiles = proj.getProjectFiles();
        WebFile[] classFiles = projectFiles.getClassFilesForJavaFile(_javaFile);
        return classFiles;
    }

    /**
     * Returns the declarations in this JavaFile.
     */
    public synchronized Set<JavaDecl> getDecls()
    {
        // If already loaded, just return
        if (_decls.size() > 0) return _decls;

        // Get Resolver
        ProjectX proj = getProject();
        Resolver resolver = proj.getResolver();

        // Iterate over JavaFile.Class files
        WebFile[] classFiles = getClassFiles();
        for (WebFile classFile : classFiles) {
            String className = proj.getClassNameForFile(classFile);
            JavaClass javaClass = resolver.getJavaClassForName(className);
            if (javaClass == null) {
                System.err.println("JavaData.getDecls: Can't find decl " + className);
                continue;
            }
            _decls.addAll(javaClass.getAllDecls());
        }

        // Return decls
        return _decls;
    }

    /**
     * Returns the references in this JavaFile.
     */
    public Set<JavaDecl> getRefs()  { return _refs; }

    /**
     * Returns the set of files that depend on our file.
     */
    public Set<WebFile> getDependents()  { return _dependents; }

    /**
     * Returns whether dependencies are set.
     */
    public boolean isDependenciesSet()  { return _isDependenciesSet; }

    /**
     * Updates dependencies for a given file and list of new/old dependencies.
     *
     * @return whether any dependencies (the declarations or references) have changed since last update.
     */
    public synchronized boolean updateDependencies()
    {
        // Get Project and Resolver
        ProjectX proj = getProject();
        Resolver resolver = proj.getResolver();

        // Get Class files
        WebFile[] classFiles = getClassFiles();

        // Get new declarations
        boolean declsChanged = false;
        if (classFiles != null) {
            for (WebFile classFile : classFiles) {
                String className = proj.getClassNameForFile(classFile);
                JavaClass javaClass = resolver.getJavaClassForName(className);
                if (javaClass == null)
                    return false;

                // Update decls
                try {
                    boolean changed = javaClass.updateDecls();
                    if (changed)
                        declsChanged = true;
                }

                catch (Throwable t) {
                    System.err.printf("JavaData.updateDepends failed to get decls in %s: %s\n", classFile, t);
                }
            }
        }

        // If declarations have changed, clear cached list
        if (declsChanged)
            _decls.clear();

        // Set isDependenciesSet
        _isDependenciesSet = true;

        // Get new refs
        Set<JavaDecl> newRefs = new HashSet<>();
        if (classFiles != null) {
            for (WebFile classFile : classFiles) {
                ClassData classData = ClassData.getClassDataForFile(classFile);
                try {
                    classData.getRefs(newRefs);
                }

                catch (Throwable t) {
                    System.err.printf("JavaData.updateDepends failed to get refs in %s: %s\n", classFile, t);
                }
            }
        }

        // If references haven't changed, just return
        if (newRefs.equals(_refs))
            return declsChanged;

        // Get set of added/removed refs
        Set<JavaDecl> refsAdded = new HashSet<>(_refs);
        refsAdded.addAll(newRefs);
        Set<JavaDecl> refsRemoved = new HashSet<>(refsAdded);
        refsRemoved.removeAll(newRefs);
        refsAdded.removeAll(_refs);
        _refs = newRefs;

        // Get project
        ProjectX rootProj = proj.getRootProject();
        ProjectSet projSet = rootProj.getProjectSet();

        // Iterate over added refs and add dependencies
        for (JavaDecl ref : refsAdded) {

            // Get Class ref
            JavaClass javaClass = ref instanceof JavaClass ? (JavaClass) ref : null;
            if (javaClass == null)
                continue;

            // Skip system classes
            String className = javaClass.getRootClassName();
            if (className.startsWith("java") && (className.startsWith("java.") || className.startsWith("javax.")))
                continue;

            //
            WebFile file = projSet.getJavaFile(className);
            if (file != null && file != _javaFile && !_dependencies.contains(file)) {
                _dependencies.add(file);
                JavaData javaData = JavaData.getJavaDataForFile(file);
                javaData._dependents.add(_javaFile);
            }
        }

        // Iterate over removed refs and add dependencies
        for (JavaDecl ref : refsRemoved) {

            // Get Class ref
            JavaClass javaClass = ref instanceof JavaClass ? (JavaClass) ref : null;
            if (javaClass == null) continue;

            String className = javaClass.getRootClassName();
            WebFile file = projSet.getJavaFile(className);
            if (file != null && _dependencies.contains(file)) {
                _dependencies.remove(file);
                JavaData javaData = JavaData.getJavaDataForFile(file);
                javaData._dependents.remove(_javaFile);
            }
        }

        // Return true since references changed
        return true;
    }

    /**
     * Removes dependencies.
     */
    public void removeDependencies()
    {
        for (WebFile depFile : _dependencies) {
            JavaData javaData = JavaData.getJavaDataForFile(depFile);
            javaData._dependents.remove(_javaFile);
        }

        _dependencies.clear();
        _decls.clear();
        _refs.clear();
        _isDependenciesSet = false;
    }

    /**
     * Returns the JavaData for given file.
     */
    public static JavaData getJavaDataForFile(WebFile aFile)
    {
        // Get JavaData for given source file
        JavaData javaData = (JavaData) aFile.getProp(JavaData.class.getName());

        // If missing, create/set
        if (javaData == null) {
            javaData = new JavaData(aFile);
            aFile.setProp(JavaData.class.getName(), javaData);
        }

        // Return
        return javaData;
    }
}