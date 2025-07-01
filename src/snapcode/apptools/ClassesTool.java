package snapcode.apptools;
import snap.props.PropChangeListener;
import snap.util.ListUtils;
import snap.util.ObjectArray;
import snap.view.*;
import snap.web.WebFile;
import snapcode.app.SelFileTool;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import snapcode.project.Project;
import snapcode.project.ProjectUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This tool class shows the project source files in class hierarchy.
 */
public class ClassesTool extends WorkspaceTool {

    // The root class node
    private ClassNode _rootClassNode;

    // The TreeView to show classes
    private TreeView<ClassNode> _treeView;

    // Listener for PagePane.SelFile
    private PropChangeListener _pagePaneSelFileLsnr = pc -> handleSelFileChanged();

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
     * Resets the Classes tree.
     */
    protected void resetClassTree()
    {
        // Clear ClassNodes and views
        _rootClassNode = null;

        // Get RootClassNode and reset treeview
        ClassNode rootClassNode = getRootClassNode();
        _treeView.setItems(new ClassNode[] { rootClassNode });
        _treeView.expandAll();
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Calculate TreeView RowHeight
        //Label sampleLabel = createLabelForClassNode(new ClassNode(Object.class, null));
        int treeViewRowHeight = 32; //(int) Math.ceil(sampleLabel.getPrefHeight() + 10);

        // Configure TreeView
        _treeView = getView("TreeView", TreeView.class);
        _treeView.setRowHeight(treeViewRowHeight);
        _treeView.setResolver(new ClassTreeResolver());
        _treeView.setCellConfigure(this::configureClassTreeCell);

        // Rebuild classes view
        resetClassTree();
    }

    /**
     * Respond UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle ReloadButton
            case "ReloadButton": resetClassTree(); break;

            // Handle TreeView
            case "TreeView": handleTreeViewActionEvent(); break;

            // Do normal version
            default: super.respondUI(anEvent); break;
        }
    }

    /**
     * Called when TreeView gets Action event.
     */
    private void handleTreeViewActionEvent()
    {
        ClassNode selClassNode = _treeView.getSelItem();
        WebFile selFile = selClassNode != null ? selClassNode.getNodeFile() : null;
        if (selFile != null)
            _workspacePane.openFile(selFile);
    }

    /**
     * Returns all source files in workspace projects.
     */
    public WebFile[] getAllSourceFiles()
    {
        List<WebFile> sourceFiles = new ArrayList<>();
        List<Project> projects = _workspace.getProjects();
        List<WebFile> sourceDirs = ListUtils.map(projects, Project::getSourceDir);
        sourceDirs.forEach(dir -> findSourceFiles(dir, sourceFiles));
        return sourceFiles.toArray(new WebFile[0]);
    }

    /**
     * Finds source files in given directory and adds to given list (recursively).
     */
    private void findSourceFiles(WebFile aDir, List<WebFile> theFiles)
    {
        List<WebFile> dirFiles = aDir.getFiles();

        for (WebFile file : dirFiles) {
            if (file.getFileType().equals("java"))
                theFiles.add(file);
            else if (file.isDir())
                findSourceFiles(file, theFiles);
        }
    }

    /**
     * Called to configure a ClassTree cell.
     */
    private void configureClassTreeCell(ListCell<ClassNode> classTreeCell)
    {
        ClassNode classNode = classTreeCell.getItem();
        if (classNode == null) return;

        Label classNodeLabel = createLabelForClassNode(classNode);
        classTreeCell.setGraphicAfter(classNodeLabel);
    }

    /**
     * Creates a label for class node.
     */
    private Label createLabelForClassNode(ClassNode classNode)
    {
        // Create label for node class
        Class<?> nodeClass = classNode.getNodeClass();
        Label label = new Label(nodeClass.getSimpleName());
        label.setPropsString("Fill:#F5CC9B; Border:#66 1; MinWidth:60; MinHeight:26; Padding:2,4,2,8; BorderRadius:4;");

        // Return
        return label;
    }

    /**
     * Override to start/stop listening to PagePane.SelFile changes.
     */
    @Override
    protected void setShowing(boolean aValue)
    {
        if (aValue == isShowing()) return;
        super.setShowing(aValue);

        // Add remove PagePane.SelFile listener
        if (aValue) {
            _workspacePane.getSelFileTool().addPropChangeListener(_pagePaneSelFileLsnr, SelFileTool.SelFile_Prop);
            handleSelFileChanged();
        }
        else _pagePane.removePropChangeListener(_pagePaneSelFileLsnr);
    }

    /**
     * Called when WorkspacePane.SelFileTool.SelFile changes.
     */
    private void handleSelFileChanged()
    {
        // Get ClassNode for PagePane.SelFile
        WebFile selFile = _workspacePane.getSelFile();
        Project project = selFile != null && ProjectUtils.isSourceFile(selFile) ? Project.getProjectForFile(selFile) : null;
        Class<?> selFileClass = project != null ? project.getClassForFile(selFile) : null;
        ClassNode selFileClassNode = selFileClass != null ? getRootClassNode().getChildNodeForClassDeep(selFileClass) : null;

        // Set TreeView.SelItem
        _treeView.setSelItem(selFileClassNode);
    }

    /**
     * Returns the title.
     */
    public String getTitle()  { return "Classes"; }

    /**
     * A TreeResolver for ClassNode.
     */
    private static class ClassTreeResolver extends TreeResolver<ClassNode> {

        @Override
        public ClassNode getParent(ClassNode anItem)  { return anItem._parentNode; }

        @Override
        public boolean isParent(ClassNode anItem)  { return !anItem._childNodes.isEmpty(); }

        @Override
        public List<ClassNode> getChildren(ClassNode aParent)  { return Arrays.asList(aParent.getChildNodes()); }

        @Override
        public String getText(ClassNode anItem)  { return ""; }
    }

    /**
     * A class to represent the class hierarchy.
     */
    private static class ClassNode implements Comparable<ClassNode> {

        // The parent node
        private ClassNode _parentNode;

        // The node class
        private Class<?> _nodeClass;

        // The node file
        private WebFile _nodeFile;

        // The child nodes
        private ObjectArray<ClassNode> _childNodes = new ObjectArray<>(ClassNode.class, 0);

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
        public ClassNode[] getChildNodes()  { return _childNodes.getArray(); }

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
            ClassNode classNode = new ClassNode(nodeClass, nodeFile);
            classNode._parentNode = this;
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
