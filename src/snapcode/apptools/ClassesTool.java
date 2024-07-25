package snapcode.apptools;
import snap.geom.Insets;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.view.ColView;
import snap.view.Label;
import snap.view.ScrollView;
import snap.view.ViewEvent;
import snap.web.WebFile;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import snapcode.project.Project;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * This tool class shows the project source files in class hierarchy.
 */
public class ClassesTool extends WorkspaceTool {

    // The root class node
    private ClassNode _rootClassNode;

    // The ClassesView
    private ColView _classesView;

    /**
     * Constructor.
     */
    public ClassesTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Returns the root class node.
     */
    private ClassNode getRootClassNode()
    {
        if (_rootClassNode != null) return _rootClassNode;

        // Create RootClassNode for Object
        _rootClassNode = new ClassNode(Object.class, null);

        // Iterate over all source files and add node for each
        WebFile[] sourceFiles = getAllSourceFiles();
        for (WebFile sourceFile : sourceFiles) {
            Project project = Project.getProjectForFile(sourceFile);
            Class<?> sourceFileClass = project.getClassForFile(sourceFile);
            if (sourceFileClass != null)
                _rootClassNode.addChildNodeForClassAndFile(sourceFileClass, sourceFile);
        }

        // Return
        return _rootClassNode;
    }

    /**
     * Rebuild the ClassesView by adding view for all class files.
     */
    protected void rebuildClassesView()
    {
        // Clear ClassNodes and views
        _rootClassNode = null;
        _classesView.removeChildren();

        // Get RootClassNode and add node views
        ClassNode rootClassNode = getRootClassNode();
        rootClassNode.getChildNodes().forEach(classNode -> addViewForClassNode(classNode, 0));
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Create ClassesView
        _classesView = new ColView();
        _classesView.setPropsString("Font:Arial 14; Fill:WHITE; Padding:15;");

        // Add to ScrollView
        ScrollView scrollView = getView("ScrollView", ScrollView.class);
        scrollView.setContent(_classesView);

        // Rebuild classes view
        rebuildClassesView();
    }

    /**
     * Respond UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle ReloadButton
        if (anEvent.equals("ReloadButton"))
            rebuildClassesView();

        // Do normal version
        else super.respondUI(anEvent);
    }

    /**
     * Add views for ClassNodes.
     */
    private void addViewForClassNode(ClassNode classNode, int level)
    {
        // Create label for class node and add to view
        Label label = new Label(classNode.getNodeClass().getSimpleName());
        label.setPropsString("Fill:#F5CC9B; Border:BLACK 1; MinWidth:60; Margin:8; Padding:4,8,4,8; BorderRadius:4;");
        label.setMargin(Insets.add(label.getMargin(), 0, 0,0, level * 25));
        _classesView.addChild(label);

        // Add child nodes
        classNode.getChildNodes().forEach(childNode -> addViewForClassNode(childNode, level + 1));
    }

    /**
     * Returns all source files in workspace projects.
     */
    public WebFile[] getAllSourceFiles()
    {
        List<WebFile> sourceFiles = new ArrayList<>();
        Project[] projects = _workspace.getProjects();
        WebFile[] sourceDirs = ArrayUtils.map(projects, Project::getSourceDir, WebFile.class);
        Stream.of(sourceDirs).forEach(dir -> findSourceFiles(dir, sourceFiles));
        return sourceFiles.toArray(new WebFile[0]);
    }

    /**
     * Finds source files in given directory and adds to given list (recursively).
     */
    private void findSourceFiles(WebFile aDir, List<WebFile> theFiles)
    {
        WebFile[] dirFiles = aDir.getFiles();

        for (WebFile file : dirFiles) {
            if (file.getFileType().equals("java"))
                theFiles.add(file);
            else if (file.isDir())
                findSourceFiles(file, theFiles);
        }
    }

    /**
     * Returns the title.
     */
    public String getTitle()  { return "Classes"; }

    /**
     * A class to represent the class hierarchy.
     */
    private static class ClassNode implements Comparable<ClassNode> {

        // The node class
        private Class<?> _nodeClass;

        // The node file
        private WebFile _nodeFile;

        // The child nodes
        private List<ClassNode> _childNodes = Collections.EMPTY_LIST;

        /**
         * Constructor.
         */
        public ClassNode(Class<?> nodeClass, WebFile nodeFile)
        {
            _nodeClass = nodeClass;
            _nodeFile = nodeFile;
        }

        /**
         * Returns the node class.
         */
        public Class<?> getNodeClass()  { return _nodeClass; }

        /**
         * Returns the node file.
         */
        public WebFile getNodeFile()  { return _nodeFile; }

        /**
         * Returns the child nodes.
         */
        public List<ClassNode> getChildNodes()  { return _childNodes; }

        /**
         * Returns a child node for given class.
         */
        public ClassNode getChildNodeForClassDeep(Class<?> aClass)
        {
            // Search for child that is superclass, return if not found or exact match
            ClassNode childNode = ListUtils.findMatch(_childNodes, node -> node.getNodeClass().isAssignableFrom(aClass));
            if (childNode == null || childNode._nodeClass == aClass)
                return childNode;

            // Forward to child to look for node class
            return childNode.getChildNodeForClassDeep(aClass);
        }

        /**
         * Adds a child node for given class and file.
         */
        public ClassNode addChildNodeForClassAndFile(Class<?> nodeClass, WebFile nodeFile)
        {
            // Get superclass node - add if missing
            Class<?> superClass = nodeClass.getSuperclass();
            ClassNode superClassNode = superClass == null || superClass == Object.class ? this : getChildNodeForClassDeep(superClass);
            if (superClassNode == null)
                superClassNode = addChildNodeForClassAndFile(superClass, null);

            // Get class node from superclass node - add if missing
            ClassNode classNode = superClassNode.getChildNodeForClassDeep(nodeClass);
            if (classNode == null)
                classNode = superClassNode.addChildNodeForClassAndFileImpl(nodeClass, nodeFile);
            else if (nodeFile != null)
                classNode._nodeFile = nodeFile;

            // Return
            return classNode;
        }

        /**
         * Adds a child node for given class and file.
         */
        private ClassNode addChildNodeForClassAndFileImpl(Class<?> nodeClass, WebFile nodeFile)
        {
            if (_childNodes.isEmpty()) _childNodes = new ArrayList<>();
            ClassNode classNode = new ClassNode(nodeClass, nodeFile);
            int insertIndex = -Collections.binarySearch(_childNodes, classNode) - 1;
            _childNodes.add(insertIndex, classNode);
            return classNode;
        }

        /**
         * Standard compareTo implementation.
         */
        @Override
        public int compareTo(ClassNode classNode)
        {
            if (_nodeFile != null)
                return classNode._nodeFile != null ? _nodeFile.compareTo(classNode._nodeFile) : -1;
            if (classNode._nodeFile != null)
                return 1;
            return _nodeClass.getSimpleName().compareTo(classNode._nodeClass.getSimpleName());
        }

        /**
         * Standard toString implementation.
         */
        @Override
        public String toString()
        {
            return "ClassNode: " + _nodeClass.getSimpleName();
        }
    }
}
