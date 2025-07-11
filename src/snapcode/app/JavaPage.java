package snapcode.app;
import javakit.parse.*;
import javakit.resolver.JavaExecutable;
import snap.util.ArrayUtils;
import snap.view.ViewEvent;
import snapcode.apptools.DebugTool;
import snapcode.apptools.RunTool;
import snapcode.apptools.SearchTool;
import snapcode.project.JavaAgent;
import snapcode.project.Project;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaMember;
import snapcode.project.JavaTextModel;
import snap.util.Convert;
import snap.text.TextLine;
import snap.view.View;
import snapcode.javatext.JavaTextArea;
import snapcode.javatext.JavaTextPane;
import snapcode.javatext.NodeMatcher;
import snapcode.views.SnapEditorPage;
import snapcode.webbrowser.WebBrowser;
import snapcode.webbrowser.WebPage;
import snap.web.WebFile;
import snap.web.WebURL;
import java.util.Objects;

/**
 * A JavaPage subclass to view/edit Java files.
 */
public class JavaPage extends WebPage {

    // The JavaTextPane
    private JavaTextPane _javaTextPane;

    /**
     * Constructor.
     */
    public JavaPage()
    {
        super();
        _javaTextPane = createJavaTextPane();
    }

    /**
     * Return the WorkspacePane.
     */
    WorkspacePane getWorkspacePane()
    {
        WebBrowser browser = getBrowser();
        return browser.getOwner(WorkspacePane.class);
    }

    /**
     * Returns the JavaTextArea.
     */
    public JavaTextPane getTextPane()  { return _javaTextPane; }

    /**
     * Creates the JavaTextPane.
     */
    protected JavaTextPane createJavaTextPane()  { return new JavaPageJavaTextPane(); }

    /**
     * Returns the JavaTextArea.
     */
    public JavaTextArea getTextArea()  { return _javaTextPane.getTextArea(); }

    /**
     * Creates UI panel.
     */
    protected View createUI()
    {
        return _javaTextPane.getUI();
    }

    /**
     * Init UI.
     */
    protected void initUI()
    {
        // Create Java text
        WebFile javaFile = getFile();
        JavaTextModel javaTextModel = JavaTextModel.getJavaTextModelForFile(javaFile);

        // Set java text and FirstFocus
        JavaTextArea javaTextArea = getTextArea();
        javaTextArea.setTextModel(javaTextModel);
        setFirstFocus(javaTextArea);

        // Register for enter action
        addKeyActionFilter("EnterAction", "Shortcut+ENTER");
    }

    /**
     * Override to update selection from URL.
     */
    @Override
    protected void initShowing()
    {
        WebURL url = getURL();
        setTextSelectionForUrlParams(url);
        if (WorkspacePane._embedMode)
            getTextArea().setSel(getTextArea().length());
    }

    /**
     * Respond UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle EnterAction (Shortcut+Enter): Run app
        if (anEvent.equals("EnterAction")) {
            WorkspacePane workspacePane = getWorkspacePane();
            RunTool runTool = workspacePane.getRunTool();
            runTool.runAppForSelFile(false);
        }
    }

    /**
     * Override to set parameters.
     */
    @Override
    public void setURL(WebURL aURL)
    {
        // Do normal version
        super.setURL(aURL);

        // Set selection
        if (isUISet())
            setTextSelectionForUrlParams(aURL);
    }

    /**
     * Sets selection of text based on URL params.
     */
    private void setTextSelectionForUrlParams(WebURL aURL)
    {
        // Look for LineNumber
        String lineNumberString = aURL.getRefValue("LineNumber");
        JavaTextArea textArea = getTextArea();
        if (lineNumberString != null) {
            int lineNumber = Convert.intValue(lineNumberString);
            textArea.getTextAdapter().selectLine(lineNumber - 1);
        }

        // Look for Sel (selection)
        String sel = aURL.getRefValue("Sel");
        if (sel != null) {
            int start = Convert.intValue(sel);
            sel = sel.substring(sel.indexOf('-') + 1);
            int end = Convert.intValue(sel);
            if (end < start)
                end = start;
            textArea.setSel(start, end);
        }

        // Look for SelLine (select line)
        String selLine = aURL.getRefValue("SelLine");
        if (selLine != null) {
            int lineNum = Convert.intValue(selLine) - 1;
            TextLine textLine = lineNum >= 0 && lineNum < textArea.getLineCount() ? textArea.getLine(lineNum) : null;
            if (textLine != null)
                textArea.setSel(textLine.getStartCharIndex());
        }

        // Look for Find
        String findString = aURL.getRefValue("Find");
        if (findString != null) {
            JavaTextPane textPane = getTextPane();
            textPane.find(findString, true, true);
        }

        // Look for Member selection request
        String memberName = aURL.getRefValue("Member");
        if (memberName != null) {

            // Get ClassDecl
            WebFile javaFile = getFile();
            JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(javaFile);
            JFile jfile = javaAgent.getJFile();
            JClassDecl classDecl = jfile.getClassDecl();
            String className = classDecl.getName();

            // Handle ClassDecl name
            JExprId id = null;
            if (className.equals(memberName))
                id = classDecl.getId();

            // Look for member matching name
            else {
                JMemberDecl[] memberDecls = classDecl.getMemberDecls();
                JMemberDecl match = ArrayUtils.findMatch(memberDecls, md -> Objects.equals(memberName, md.getName()));
                if (match != null)
                    id = match.getId();
            }

            // If member found, select it
            if (id != null) {
                int start = id.getStartCharIndex();
                int end = id.getEndCharIndex();
                textArea.setSel(start, end);
            }
        }
    }

    /**
     * Initializes a Java file.
     */
    public static void initJavaFile(WebFile javaFile)
    {
        // Get package name and class name
        Project project = Project.getProjectForFile(javaFile);
        WebFile parentDir = javaFile.getParent();
        String packageName = project != null ? project.getClassNameForFile(parentDir) : javaFile.getSimpleName();
        String className = javaFile.getSimpleName();

        // If Java file, set default java text
        String javaText = JavaPage.getJavaContentStringForPackageAndClassName(packageName, className);
        javaFile.setText(javaText);
    }

    /**
     * Creates a new file for use with showNewFilePanel method.
     */
    public static String getJavaContentStringForPackageAndClassName(String packageName, String className)
    {
        // Append package declaration
        StringBuilder sb = new StringBuilder();
        if (packageName != null && packageName.length() > 0)
            sb.append("package ").append(packageName).append(";\n");

        // Append Comment
        sb.append("\n/**\n * A custom class.\n */\n");

        // Append class declaration: "public class <File-Name> extends Object {   }"
        sb.append("public class ").append(className).append(" {\n\n");

        // Append standard main implementation
        sb.append("    /**\n");
        sb.append("     * Standard main implementation.\n");
        sb.append("     */\n");
        sb.append("    public static void main(String[] args)\n");
        sb.append("    {\n");
        sb.append("    }\n");
        sb.append('\n');

        // Append close
        sb.append("}");

        // Return
        return sb.toString();
    }

    /**
     * Override to open declaration.
     */
    protected void openDeclaration(JNode aNode)
    {
        JavaDecl decl = aNode.getDecl();
        if (decl != null)
            openDecl(decl);
    }

    /**
     * Open a super declaration.
     */
    protected void openSuperDeclaration(JExecutableDecl aMethodOrConstrDecl)
    {
        JavaExecutable methodOrConstr = aMethodOrConstrDecl.getExecutable();
        JavaExecutable superMethodOrConstr = methodOrConstr.getSuper();
        if (superMethodOrConstr != null)
            openDecl(superMethodOrConstr);
    }

    /**
     * Opens a project declaration.
     */
    private void openDecl(JavaDecl aDecl)
    {
        // Get class name for decl
        String className = aDecl instanceof JavaMember ? ((JavaMember) aDecl).getDeclaringClassName() : aDecl.getEvalClassName();
        if (className == null)
            return;

        // Open System class
        if (className.startsWith("java.") || className.startsWith("javax.")) {
            openJavaDecl(aDecl);
            return;
        }

        // Get project
        Project proj = Project.getProjectForSite(getSite());
        if (proj == null)
            return;

        // Get source file
        WebFile file = proj.getJavaFileForClassName(className);
        if (file == null)
            return;

        // Get matching node
        JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(file);
        JFile jfile = javaAgent.getJFile();
        JNode declarationNode = NodeMatcher.getDeclarationNodeForDecl(jfile, aDecl);

        // Get URL
        String urlString = file.getUrl().getString();
        if (declarationNode != null)
            urlString += String.format("#Sel=%d-%d", declarationNode.getStartCharIndex(), declarationNode.getEndCharIndex());

        // Open URL
        getBrowser().setSelUrlForUrlAddress(urlString);
    }

    /**
     * Override to open declaration.
     */
    private void openJavaDecl(JavaDecl aDecl)
    {
        String className = aDecl instanceof JavaMember ? ((JavaMember) aDecl).getDeclaringClassName() :
                aDecl.getEvalClassName();
        if (className == null)
            return;

        String javaPath = '/' + className.replace('.', '/') + ".java";

        // Get URL
        WebURL javaURL = WebURL.getUrl("https://reportmill.com/jars/8u05/src.zip!" + javaPath);
        assert (javaURL != null);
        String urlString = javaURL.getString() + "#Member=" + aDecl.getSimpleName();

        // Open URL
        getBrowser().setSelUrlForUrlAddress(urlString);
    }

    /**
     * Show references for given node.
     */
    protected void showReferences(JNode aNode)
    {
        if (getWorkspacePane() == null) return;
        WorkspaceTools workspaceTools = getWorkspacePane().getWorkspaceTools();
        SearchTool searchTool = workspaceTools.getToolForClass(SearchTool.class);
        searchTool.searchReference(aNode);
        workspaceTools.showToolForClass(SearchTool.class);
    }

    /**
     * Show declarations for given node.
     */
    protected void showDeclarations(JNode aNode)
    {
        if (getWorkspacePane() == null) return;
        WorkspaceTools workspaceTools = getWorkspacePane().getWorkspaceTools();
        SearchTool searchTool = workspaceTools.getToolForClass(SearchTool.class);
        searchTool.searchDeclaration(aNode);
        workspaceTools.showToolForClass(SearchTool.class);
    }

    /**
     * Override to get ProgramCounter from ProcPane.
     */
    protected int getProgramCounterLine()
    {
        WorkspacePane workspacePane = getWorkspacePane();
        WorkspaceTools workspaceTools = workspacePane.getWorkspaceTools();
        DebugTool debugTool = workspaceTools.getToolForClass(DebugTool.class);
        return debugTool != null ? debugTool.getProgramCounter(getFile()) : -1;
    }

    /**
     * Reopen this page as SnapCodePage.
     */
    public void openAsSnapCode()
    {
        WebFile file = getFile();
        WebURL url = file.getUrl();
        WebPage page = new SnapEditorPage(this);
        page.setFile(file);
        WebBrowser browser = getBrowser();
        browser.setPageForURL(url, page);
        browser.setSelPage(page);
    }

    /**
     * A JavaTextPane for a JavaPage to implement symbol features and such.
     */
    public class JavaPageJavaTextPane extends JavaTextPane {

        /**
         * Override to open declaration.
         */
        public void openDeclaration(JNode aNode)
        {
            JavaPage.this.openDeclaration(aNode);
        }

        /**
         * Open a super declaration.
         */
        public void openSuperDeclaration(JExecutableDecl aMethodOrConstrDecl)
        {
            JavaPage.this.openSuperDeclaration(aMethodOrConstrDecl);
        }

        /**
         * Show references for given node.
         */
        public void showReferences(JNode aNode)
        {
            JavaPage.this.showReferences(aNode);
        }

        /**
         * Show declarations for given node.
         */
        public void showDeclarations(JNode aNode)
        {
            JavaPage.this.showDeclarations(aNode);
        }

        /**
         * Override to get ProgramCounter from ProcPane.
         */
        public int getProgramCounterLine()
        {
            return JavaPage.this.getProgramCounterLine();
        }

        /**
         * Respond to UI controls.
         */
        @Override
        public void respondUI(ViewEvent anEvent)
        {
            // Handle ShowSnapCodeMenuItem
            if (anEvent.equals("SnapCodeButton") || anEvent.equals("ShowSnapCodeMenuItem"))
                openAsSnapCode();

                // Do normal version
            else super.respondUI(anEvent);
        }
    }
}