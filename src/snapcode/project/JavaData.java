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

    // The set of external references in this JavaFile
    private Set<JavaDecl> _externalReferences;

    // The set of files that our file depends on
    private Set<WebFile>  _dependencies = new HashSet<>();

    // The set of files that depend on our file
    private Set<WebFile>  _dependents = new HashSet<>();

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
     * Returns the external references in this JavaFile.
     */
    public Set<JavaDecl> getExternalReferences()
    {
        if (_externalReferences != null) return _externalReferences;
        updateDependencies();
        return _externalReferences;
    }

    /**
     * Returns the set of files that depend on our file.
     */
    public Set<WebFile> getDependents()  { return _dependents; }

    /**
     * Updates dependencies for a given file and list of new/old dependencies.
     *
     * @return whether any dependencies (the declarations or references) have changed since last update.
     */
    public synchronized boolean updateDependencies()
    {
        // If first time, set external references set
        if (_externalReferences == null)
            _externalReferences = new HashSet<>();

        // Get Project and Resolver
        Project project = getProject();
        Resolver resolver = project.getResolver();

        // Get Class files
        WebFile[] classFiles = getClassFiles();

        // Get new declarations
        boolean declsChanged = false;
        for (WebFile classFile : classFiles) {
            String className = project.getClassNameForFile(classFile);
            JavaClass javaClass = resolver.getJavaClassForName(className);
            if (javaClass == null)
                continue;

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

        // Get new external references
        Set<JavaDecl> newExternalReferences = new HashSet<>();
        for (WebFile classFile : classFiles)
            ClassFileUtils.findRefsForClassFile(classFile, newExternalReferences);

        // If references haven't changed, just return
        if (newExternalReferences.equals(_externalReferences))
            return declsChanged;

        // Get set of added/removed refs
        Set<JavaDecl> addedReferences = new HashSet<>(_externalReferences);
        addedReferences.addAll(newExternalReferences);
        Set<JavaDecl> removedReferences = new HashSet<>(addedReferences);
        removedReferences.removeAll(newExternalReferences);
        addedReferences.removeAll(_externalReferences);
        _externalReferences = newExternalReferences;

        // Iterate over added refs and add dependencies
        for (JavaDecl addedRef : addedReferences) {

            // Get Class ref
            JavaClass javaClass = addedRef instanceof JavaClass ? (JavaClass) addedRef : null;
            if (javaClass == null)
                continue;

            // Get java file for class name and add to dependencies
            String className = javaClass.getRootClassName();
            WebFile javaFile = project.getJavaFileForClassName(className);
            if (javaFile != null && javaFile != _javaFile && !_dependencies.contains(javaFile)) {
                _dependencies.add(javaFile);
                JavaData javaData = getJavaDataForFile(javaFile);
                javaData._dependents.add(_javaFile);
            }
        }

        // Iterate over removed refs and add dependencies
        for (JavaDecl removedRef : removedReferences) {

            // Get Class ref
            JavaClass javaClass = removedRef instanceof JavaClass ? (JavaClass) removedRef : null;
            if (javaClass == null)
                continue;

            String className = javaClass.getRootClassName();
            WebFile javaFile = project.getJavaFileForClassName(className);
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