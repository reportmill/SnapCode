package snapcode.project;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaClass;
import snap.web.WebFile;
import java.util.*;

/**
 * A file object for managing Java files.
 */
public class JavaData {

    // The java file
    private WebFile  _javaFile;

    // The set of external references in this JavaFile
    private Set<JavaDecl> _externalReferences;

    // The set of files that our file depends on
    private Set<WebFile>  _dependencies = new HashSet<>();

    // The set of files that depend on our file
    private Set<WebFile>  _dependents = new HashSet<>();

    /**
     * Constructor for given java file.
     */
    protected JavaData(WebFile javaFile)
    {
        _javaFile = javaFile;
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

        // Get Class files
        Project project = Project.getProjectForFile(_javaFile);
        WebFile[] classFiles = project.getProjectFiles().getClassFilesForJavaFile(_javaFile);

        // Get new declarations
        boolean declsChanged = false;
        for (WebFile classFile : classFiles) {
            JavaClass javaClass = project.getJavaClassForFile(classFile);
            if (javaClass == null)
                continue;

            // Update decls
            boolean changed = javaClass.updateDecls();
            if (changed)
                declsChanged = true;
        }

        // Get new external references
        Set<JavaDecl> newExternalRefs = new HashSet<>();
        for (WebFile classFile : classFiles)
            ClassFileUtils.findRefsForClassFile(classFile, newExternalRefs);

        // If references haven't changed, just return
        if (newExternalRefs.equals(_externalReferences))
            return declsChanged;

        // Get set of added refs (new - old)
        Set<JavaDecl> addedRefs = new HashSet<>(newExternalRefs);
        addedRefs.removeAll(_externalReferences);

        // Iterate over added class refs and add to dependencies
        for (JavaDecl addedRef : addedRefs) {
            if (addedRef instanceof JavaClass) {
                JavaClass javaClass = (JavaClass) addedRef;
                String className = javaClass.getRootClassName();
                WebFile javaFile = project.getJavaFileForClassName(className);
                if (javaFile != null && javaFile != _javaFile && !_dependencies.contains(javaFile)) {
                    _dependencies.add(javaFile);
                    JavaData javaData = getJavaDataForFile(javaFile);
                    javaData._dependents.add(_javaFile);
                }
            }
        }

        // Get set of removed refs (old - new)
        Set<JavaDecl> removedRefs = new HashSet<>(_externalReferences);
        removedRefs.removeAll(newExternalRefs);
        _externalReferences = newExternalRefs;

        // Iterate over removed class refs and remove from dependencies
        for (JavaDecl removedRef : removedRefs) {
            if (removedRef instanceof JavaClass) {
                JavaClass javaClass = (JavaClass) removedRef;
                String className = javaClass.getRootClassName();
                WebFile javaFile = project.getJavaFileForClassName(className);
                if (javaFile != null && _dependencies.contains(javaFile)) {
                    _dependencies.remove(javaFile);
                    JavaData javaData = getJavaDataForFile(javaFile);
                    javaData._dependents.remove(_javaFile);
                }
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