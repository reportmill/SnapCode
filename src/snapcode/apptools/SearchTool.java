/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.apptools;
import javakit.parse.*;
import javakit.resolver.*;
import snap.util.SetUtils;
import snapcode.project.JavaAgent;
import snapcode.project.Project;
import snapcode.javatext.NodeMatcher;
import snapcode.javatext.JavaTextUtils;
import snap.geom.HPos;
import snap.gfx.Image;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import snapcode.app.ProjectPane;
import snap.util.ArrayUtils;
import snap.view.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.util.FileIcons;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class manages project search.
 */
public class SearchTool extends WorkspaceTool {

    // The current search
    private Search  _search;

    // The current selected result
    private Result  _selResult;

    // Constant for search file types
    private static final List<String> SEARCH_FILE_TYPES = List.of("java", "snp", "txt", "js");

    /**
     * Constructor.
     */
    public SearchTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Search for given string.
     */
    public void searchForString(String aString)
    {
        _search = new Search();
        _search._string = aString;
        for (WebSite site : _workspacePane.getProjectSites())
            findResultsForStringAndFile(aString.toLowerCase(), site.getRootDir(), _search._results);
        resetLater();
    }

    /**
     * Search for given string in given file and adds results to given list.
     */
    private void findResultsForStringAndFile(String aString, WebFile aFile, List<Result> resultsList)
    {
        // If hidden file, just return
        Project proj = Project.getProjectForFile(aFile);
        ProjectPane projectPane = _workspacePane.getProjectPaneForProject(proj);
        if (projectPane != null && projectPane.isHiddenFile(aFile))
            return;

        // Handle directory
        if (aFile.isDir()) {
            if (aFile == _workspacePane.getBuildDir())
                return;
            for (WebFile file : aFile.getFiles())
                findResultsForStringAndFile(aString, file, resultsList);
            return;
        }

        // If not search file type, just return
        if (!SEARCH_FILE_TYPES.contains(aFile.getFileType()))
            return;

        // Handle search file types
        String text = aFile.getText().toLowerCase();
        Result result = null;
        int len = aString.length();
        for (int start = text.indexOf(aString); start >= 0; start = text.indexOf(aString, start + len)) {
            if (result == null)
                resultsList.add(result = new Result(aFile));
            else result._count++;
        }
    }

    /**
     * Search for given node references.
     */
    public void searchReference(JNode aNode)
    {
        // Get JavaDecl for node
        JavaDecl decl = aNode.getDecl();
        if (decl == null) {
            beep();
            return;
        }

        // If method, get root method
        if (decl instanceof JavaMethod method) {
            while (method.getSuper() != null)
                method = method.getSuper();
            decl = method;
        }

        // If param class
        if (decl instanceof JavaParameterizedType)
            decl = decl.getEvalClass();

        // Configure search
        _search = new Search();
        _search._string = decl.getFullNameWithParameterTypes();
        _search._kind = Search.Kind.Reference;

        // Iterate over all project sites
        for (WebSite site : _workspacePane.getProjectSites())
            findReferencesForDeclAndFile(decl, site.getRootDir(), _search._results);

        // Update UI
        resetLater();
    }

    /**
     * Search for given node references in given file and add to results list.
     */
    private void findReferencesForDeclAndFile(JavaDecl aDecl, WebFile aFile, List<Result> resultsList)
    {
        // If hidden file, just return
        Project proj = Project.getProjectForFile(aFile);
        ProjectPane projectPane = _workspacePane.getProjectPaneForProject(proj);
        if (projectPane != null && projectPane.isHiddenFile(aFile))
            return;

        // Handle directory
        if (aFile.isDir()) {
            if (aFile == _workspacePane.getBuildDir())
                return;
            List<WebFile> dirFiles = aFile.getFiles();
            for (WebFile file : dirFiles)
                findReferencesForDeclAndFile(aDecl, file, resultsList);
            return;
        }

        // If not Java file, just return
        if (!aFile.getFileType().equals("java"))
            return;

        // Get file external references
        JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(aFile);
        Set<JavaDecl> externalRefs = javaAgent.getExternalReferences();

        // Get whether file should be searched: If contains matching external ref or if decl is primitive static final
        boolean isDeclStaticFinalPrimitive = aDecl instanceof JavaField field && field.isStatic() && field.isFinal() &&
                (field.getEvalType().isPrimitive() || field.getEvalType().getName().equals("java.lang.String"));

        // If file needs search
        if (!isDeclStaticFinalPrimitive && !SetUtils.hasMatch(externalRefs, aDecl::matches))
            return;

        // Search file for references
        JFile jfile = javaAgent.getJFile();
        List<JExprId> referenceNodes = NodeMatcher.getReferenceNodesForDecl(jfile, aDecl);
        referenceNodes.forEach(node -> resultsList.add(new Result(node)));
    }

    /**
     * Search for given element reference.
     */
    public void searchDeclaration(JNode aNode)
    {
        // Get JavaDecl for node
        JavaDecl decl = aNode.getDecl();
        if (decl == null) {
            beep();
            return;
        }

        // If method, get root method
        if (decl instanceof JavaMethod method) {
            while (method.getSuper() != null)
                method = method.getSuper();
            decl = method;
        }

        // Configure search
        _search = new Search();
        _search._string = decl.getFullNameWithParameterTypes();
        _search._kind = Search.Kind.Declaration;

        // Iterate over all project sites
        for (WebSite site : _workspacePane.getProjectSites())
            findDeclarationsForDeclAndFile(decl, site.getRootDir(), _search._results);

        // Update UI
        resetLater();
    }

    /**
     * Search for given element reference.
     */
    private void findDeclarationsForDeclAndFile(JavaDecl aDecl, WebFile aFile, List<Result> resultsList)
    {
        // If hidden file, just return
        Project proj = Project.getProjectForFile(aFile);
        ProjectPane projectPane = _workspacePane.getProjectPaneForProject(proj);
        if (projectPane != null && projectPane.isHiddenFile(aFile))
            return;

        // Handle directory
        if (aFile.isDir()) {
            if (aFile == _workspacePane.getBuildDir())
                return;

            List<WebFile> dirFiles = aFile.getFiles();
            for (WebFile file : dirFiles)
                findDeclarationsForDeclAndFile(aDecl, file, resultsList);
            return;
        }

        // If not Java file, just return
        if (!aFile.getFileType().equals("java"))
            return;

        // Handle JavaFile: If file class contains matching decl, return node(s)
        JavaClass javaClass = proj.getJavaClassForFile(aFile);
        if (javaClassContainsMatchingDecl(javaClass, aDecl)) {
            JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(aFile);
            JFile jfile = javaAgent.getJFile();
            List<JExprId> declarationNodes = NodeMatcher.getDeclarationNodesForDecl(jfile, aDecl);
            declarationNodes.forEach(node -> resultsList.add(new Result(node)));
        }
    }

    /**
     * Returns the current search.
     */
    public Search getSearch()  { return _search; }

    /**
     * Returns the current search results.
     */
    public List<Result> getResults()  { return _search != null ? _search._results : null; }

    /**
     * Returns the current selected search result.
     */
    public Result getSelResult()  { return _selResult; }

    /**
     * Sets the current selected search result.
     */
    public void setSelResult(Result aResult)
    {
        _selResult = aResult;
    }

    /**
     * Clears the search.
     */
    public void clearSearch()
    {
        setViewValue("SearchText", null);
        _search = null;
        resetLater();
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        // Configure SearchText
        TextField searchText = getView("SearchText", TextField.class);
        CloseBox closeBox = new CloseBox();
        closeBox.setMargin(0, 4, 0, 4);
        closeBox.setLeanX(HPos.RIGHT);
        closeBox.addEventHandler(e -> clearSearch(), View.Action);
        searchText.getLabel().setGraphicAfter(closeBox);
        searchText.getLabel().setPickable(true);

        // Configure ResultsList
        ListView<Result> resultsList = getView("ResultsList", ListView.class);
        resultsList.setCellConfigure(this::configureResultListCell);
        resultsList.setRowHeight(24);

        setFirstFocus("SearchText");
    }

    /**
     * Reset UI.
     */
    public void resetUI()
    {
        // Update SearchResultsText
        String resultsStr = "";
        if (_search != null) {
            int hits = 0;
            for (Result results : _search._results)
                hits += results._count;
            String typ = _search._kind == Search.Kind.Declaration ? "declarations" : "references";
            resultsStr = String.format("'%s' - %d %s", _search._string, hits, typ);
        }
        setViewValue("SearchResultsText", resultsStr);

        // Update ResultsList
        setViewItems("ResultsList", getResults());
        setViewSelItem("ResultsList", getSelResult());
    }

    /**
     * Respond to UI changes.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle SearchText
        if (anEvent.equals("SearchText")) {
            String string = anEvent.getStringValue();
            if (string != null && !string.isEmpty())
                searchForString(string);
            else _search = null;
        }

        // Handle ResultsList
        if (anEvent.equals("ResultsList")) {

            // Update SelResult
            Result result = (Result) anEvent.getSelItem();
            setSelResult(result);

            // Set in browser
            if (result != null) {
                String resultURL = result.getURLString();
                getBrowser().setSelUrlForUrlAddress(resultURL);
            }
        }
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Search"; }

    /**
     * Returns whether given JavaDecl contains matching decl.
     */
    private static boolean javaClassContainsMatchingDecl(JavaClass javaClass, JavaDecl matchDecl)
    {
        // Check for class, field, constructor or method
        if (matchDecl instanceof JavaClass) {
            if (javaClass.matches(matchDecl))
                return true;
        }
        else if (matchDecl instanceof JavaField) {
            if (ArrayUtils.hasMatch(javaClass.getDeclaredFields(), field -> field.matches(matchDecl)))
                return true;
        }
        else if (matchDecl instanceof JavaConstructor) {
            if (ArrayUtils.hasMatch(javaClass.getDeclaredConstructors(), constr -> constr.matches(matchDecl)))
                return true;
        }
        else if (matchDecl instanceof JavaMethod) {
            if (ArrayUtils.hasMatch(javaClass.getDeclaredMethods(), method -> method.matches(matchDecl)))
                return true;
        }

        // Recurse into inner classes
        JavaClass[] innerClasses = javaClass.getDeclaredClasses();
        return ArrayUtils.hasMatch(innerClasses, cls -> javaClassContainsMatchingDecl(cls, matchDecl));
    }

    /**
     * A class to hold a search.
     */
    public static class Search {

        // The search string
        String _string;

        // The kind
        Kind _kind = Kind.Text;

        // The results
        List<Result> _results = new ArrayList<>();

        // Constants for kind
        public enum Kind {Text, Reference, Declaration}
    }

    /**
     * A class to hold a search result.
     */
    public class Result {

        // The file
        WebFile _file;

        // The JNode
        JNode _node;

        // The decl
        JavaDecl _decl;

        // The match count
        int _count = 1;

        /**
         * Creates a new result.
         */
        public Result(WebFile aFile)
        {
            _file = aFile;
        }

        /**
         * Creates a new result.
         */
        public Result(JNode aNode)
        {
            _node = aNode;
            _file = _node.getFile().getSourceFile();

            if (_node != null) {
                JNode enclosingDeclNode = _node.getParent(JMemberDecl.class);
                if (enclosingDeclNode instanceof JFieldDecl && _node.getParent(JVarDecl.class) != null)
                    enclosingDeclNode = _node.getParent(JVarDecl.class);
                if (enclosingDeclNode != null)
                    _decl = enclosingDeclNode.getDecl();
            }
        }

        /**
         * Standard toString implementation.
         */
        public String getDescriptor()
        {
            if (_decl != null)
                return _decl.getFullNameWithSimpleParameterTypes();
            String s = _file.getName() + " - " + _file.getParent().getPath();
            s += " (" + _count + " match" + (_count == 1 ? "" : "es") + ")";
            return s;
        }

        /**
         * Returns an image.
         */
        public Image getImage()
        {
            if (_decl != null)
                return JavaTextUtils.getImageForJavaDecl(_decl);
            return FileIcons.getFileIconImage(_file);
        }

        /**
         * Returns a URL for result.
         */
        public String getURLString()
        {
            String urlString = _file.getUrlAddress();
            if (_search._kind == Search.Kind.Text)
                urlString += "#Find=" + _search._string;
            else if (_node != null)
                urlString += String.format("#Sel=%d-%d", _node.getStartCharIndex(), _node.getEndCharIndex());
            return urlString;
        }
    }

    /**
     * Called to configure cell.
     */
    private void configureResultListCell(ListCell<Result> aCell)
    {
        Result result = aCell.getItem();
        if (result == null)
            return;
        aCell.setText(result.getDescriptor());
        aCell.setImage(result.getImage());
        aCell.getGraphic().setPadding(2, 4, 2, 4);
    }
}