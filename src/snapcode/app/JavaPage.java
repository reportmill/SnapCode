package snapcode.app;
import javakit.parse.*;
import snap.view.ViewEvent;
import snapcode.apptools.EvalTool;
import snapcode.project.JavaAgent;
import snapcode.project.Project;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaMember;
import snapcode.project.JavaTextDoc;
import snap.util.Convert;
import snap.util.ListUtils;
import snap.text.TextBoxLine;
import snap.view.View;
import snapcode.javatext.JavaTextArea;
import snapcode.javatext.JavaTextPane;
import snapcode.javatext.NodeMatcher;
import snapcode.webbrowser.WebBrowser;
import snapcode.webbrowser.WebPage;
import snap.web.WebFile;
import snap.web.WebURL;
import java.util.List;
import java.util.Objects;

/**
 * A JavaPage subclass to view/edit Java files.
 */
public class JavaPage extends WebPage implements WebFile.Updater {

    // The JavaTextPane
    private JavaTextPane<?> _javaTextPane;

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
    public JavaTextPane<?> getTextPane()  { return _javaTextPane; }

    /**
     * Creates the JavaTextPane.
     */
    protected JavaTextPane<?> createJavaTextPane()  { return new JPJavaTextPane(); }

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
        // Create JavaTextDoc
        WebFile javaFile = getFile();
        JavaTextDoc javaTextDoc = JavaTextDoc.getJavaTextDocForSource(javaFile);

        // Set TextArea.TextDoc and FirstFocus
        JavaTextArea javaTextArea = getTextArea();
        javaTextArea.setTextDoc(javaTextDoc);
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
            WorkspaceTools workspaceTools = workspacePane.getWorkspaceTools();
            EvalTool evalTool = workspaceTools.getToolForClass(EvalTool.class);
            if (evalTool != null)
                evalTool.runApp(true);
        }
    }

    /**
     * Override to reload text from file.
     */
    public void reload()
    {
        super.reload();

        // Create JavaTextDoc
        WebFile javaFile = getFile();
        JavaTextDoc javaTextDoc = JavaTextDoc.getJavaTextDocForSource(javaFile);

        // Set TextArea.TextDoc
        JavaTextArea javaTextArea = getTextArea();
        javaTextArea.setTextDoc(javaTextDoc);
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
            textArea.selectLine(lineNumber - 1);
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
            TextBoxLine textLine = lineNum >= 0 && lineNum < textArea.getLineCount() ? textArea.getLine(lineNum) : null;
            if (textLine != null)
                textArea.setSel(textLine.getStartCharIndex());
        }

        // Look for Find
        String findString = aURL.getRefValue("Find");
        if (findString != null) {
            JavaTextPane<?> textPane = getTextPane();
            textPane.find(findString, true, true);
        }

        // Look for Member selection request
        String memberName = aURL.getRefValue("Member");
        if (memberName != null) {

            // Get ClassDecl
            WebFile javaFile = getFile();
            JavaAgent javaAgent = JavaAgent.getAgentForFile(javaFile);
            JFile jfile = javaAgent.getJFile();
            JClassDecl classDecl = jfile.getClassDecl();
            String className = classDecl.getName();

            // Handle ClassDecl name
            JExprId id = null;
            if (className.equals(memberName))
                id = classDecl.getId();

            // Look for member matching name
            else {
                List<JMemberDecl> memberDecls = classDecl.getMemberDecls();
                JMemberDecl match = ListUtils.findMatch(memberDecls, md -> Objects.equals(memberName, md.getName()));
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
     * Creates a new file for use with showNewFilePanel method.
     */
    protected WebFile createNewFile(String aPath)
    {
        // Create file
        WebFile newFile = super.createNewFile(aPath);

        // Get project
        Project proj = Project.getProjectForFile(newFile);

        // Append package declaration
        WebFile fileDir = newFile.getParent();
        String packageName = proj != null ? proj.getClassNameForFile(fileDir) : newFile.getSimpleName();
        String className = newFile.getSimpleName();

        // Set text
        String javaText = getJavaContentStringForPackageAndClassName(packageName, className);
        newFile.setText(javaText);

        // Return
        return newFile;
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
     * Override to set selection using browser.
     */
    protected void setTextSel(int aStart, int anEnd)
    {
        String urlString = getFile().getUrlString() + String.format("#Sel=%d-%d", aStart, anEnd);
        getBrowser().setURLString(urlString);
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
    protected void openSuperDeclaration(JMemberDecl aMemberDecl)
    {
        JavaDecl superDecl = aMemberDecl.getSuperDecl();
        if (superDecl != null)
            openDecl(superDecl);
    }

    /**
     * Opens a project declaration.
     */
    private void openDecl(JavaDecl aDecl)
    {
        // Get class name for decl
        String className = aDecl instanceof JavaMember ? ((JavaMember) aDecl).getDeclaringClassName() :
                aDecl.getEvalClassName();
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
        JavaAgent javaAgent = JavaAgent.getAgentForFile(file);
        JFile jfile = javaAgent.getJFile();
        JNode declarationNode = NodeMatcher.getDeclarationNodeForDecl(jfile, aDecl);

        // Get URL
        String urlString = file.getURL().getString();
        if (declarationNode != null)
            urlString += String.format("#Sel=%d-%d", declarationNode.getStartCharIndex(), declarationNode.getEndCharIndex());

        // Open URL
        getBrowser().setURLString(urlString);
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
        WebURL javaURL = WebURL.getURL("https://reportmill.com/jars/8u05/src.zip!" + javaPath);
        assert (javaURL != null);
        String urlString = javaURL.getString() + "#Member=" + aDecl.getSimpleName();

        // Open URL
        getBrowser().setURLString(urlString);
    }

    /**
     * Show references for given node.
     */
    protected void showReferences(JNode aNode)  { }

    /**
     * Show declarations for given node.
     */
    protected void showDeclarations(JNode aNode)  { }

    /**
     * Override to update Page.Modified.
     */
    protected void setTextModified(boolean aFlag)
    {
        WebFile javaFile = getFile();
        javaFile.setUpdater(aFlag ? this : null);
    }

    /**
     * WebFile.Updater method.
     */
    public void updateFile(WebFile aFile)
    {
        getFile().setText(getTextArea().getText());
    }

    /**
     * Override to get ProgramCounter from ProcPane.
     */
    protected int getProgramCounterLine()  { return -1; }

    /**
     * A JavaTextPane for a JavaPage to implement symbol features and such.
     */
    public class JPJavaTextPane extends JavaTextPane<JavaTextDoc> {

        /**
         * Override to set selection using browser.
         */
        public void setTextSel(int aStart, int anEnd)
        {
            JavaPage.this.setTextSel(aStart, anEnd);
        }

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
        public void openSuperDeclaration(JMemberDecl aMemberDecl)
        {
            JavaPage.this.openSuperDeclaration(aMemberDecl);
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
         * Override to update Page.Modified.
         */
        public void setTextModified(boolean aFlag)
        {
            super.setTextModified(aFlag);
            JavaPage.this.setTextModified(aFlag);
        }

        /**
         * Override to get ProgramCounter from ProcPane.
         */
        public int getProgramCounterLine()
        {
            return JavaPage.this.getProgramCounterLine();
        }
    }
}