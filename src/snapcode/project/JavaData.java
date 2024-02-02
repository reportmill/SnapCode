package snapcode.project;
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
    private Project  _proj;

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
     * Constructor for given java file.
     */
    protected JavaData(WebFile javaFile, Project project)
    {
        _javaFile = javaFile;
        _proj = project;
    }

    /**
     * Returns the project for this JavaFile.
     */
    public Project getProject()  { return _proj; }

    /**
     * Returns the class files for this JavaFile.
     */
    public WebFile[] getClassFiles()
    {
        Project proj = getProject();
        ProjectFiles projectFiles = proj.getProjectFiles();
        return projectFiles.getClassFilesForJavaFile(_javaFile);
    }

    /**
     * Returns the declarations in this JavaFile.
     */
    public synchronized Set<JavaDecl> getDecls()
    {
        // If already loaded, just return
        if (_decls.size() > 0) return _decls;

        // Get Resolver
        Project proj = getProject();
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
        Project proj = getProject();
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
            for (WebFile classFile : classFiles)
                ClassFileUtils.findRefsForClassFile(classFile, newRefs);
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
        Project rootProj = proj.getRootProject();

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
            WebFile javaFile = rootProj.getJavaFileForClassName(className);
            if (javaFile != null && javaFile != _javaFile && !_dependencies.contains(javaFile)) {
                _dependencies.add(javaFile);
                JavaData javaData = getJavaDataForFile(javaFile);
                javaData._dependents.add(_javaFile);
            }
        }

        // Iterate over removed refs and add dependencies
        for (JavaDecl ref : refsRemoved) {

            // Get Class ref
            JavaClass javaClass = ref instanceof JavaClass ? (JavaClass) ref : null;
            if (javaClass == null) continue;

            String className = javaClass.getRootClassName();
            WebFile javaFile = rootProj.getJavaFileForClassName(className);
            if (javaFile != null && _dependencies.contains(javaFile)) {
                _dependencies.remove(javaFile);
                JavaData javaData = getJavaDataForFile(javaFile);
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
            JavaData javaData = getJavaDataForFile(depFile);
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
        JavaAgent javaAgent = JavaAgent.getAgentForFile(aFile);
        return javaAgent != null ? javaAgent.getJavaData() : null;
    }
}