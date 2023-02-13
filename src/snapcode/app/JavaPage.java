package snapcode.app;
import javakit.ide.*;
import javakit.parse.*;
import javakit.project.JavaAgent;
import javakit.project.Project;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaMember;
import javakit.parse.JavaTextDoc;
import snap.util.Convert;
import snap.util.ListUtils;
import snapcode.apptools.SearchPane;
import snap.text.TextBoxLine;
import snap.view.View;
import snap.view.ViewEvent;
import snap.viewx.WebBrowser;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snap.web.WebResponse;
import snap.web.WebURL;
import java.util.List;
import java.util.Objects;

/**
 * A JavaPage subclass to view/edit Java files.
 */
public class JavaPage extends WebPage implements WebFile.Updater {

    // The JavaTextPane
    private JavaTextPane<?>  _javaTextPane = new JPJavaTextPane();

    /**
     * Constructor.
     */
    public JavaPage()
    {
        super();
    }

    /**
     * Return the PodPane.
     */
    PodPane getPodPane()
    {
        WebBrowser browser = getBrowser();
        return browser.getOwner(PodPane.class);
    }

    /**
     * Returns the JavaTextArea.
     */
    public JavaTextPane<?> getTextPane()  { return _javaTextPane; }

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
        // Do normal version
        super.initUI();

        // Create JavaTextDoc
        WebFile javaFile = getFile();
        JavaTextDoc javaTextDoc = JavaTextDoc.getJavaTextDocForSource(javaFile);

        // Set TextArea.TextDoc and FirstFocus
        JavaTextArea javaTextArea = getTextArea();
        javaTextArea.setTextDoc(javaTextDoc);
        setFirstFocus(javaTextArea);
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
    public void setResponse(WebResponse aResp)
    {
        // Do normal version
        super.setResponse(aResp);

        // If no real file, just return
        if (getFile() == null)
            return;

        // Load UI
        getUI();

        // Set selection
        WebURL url = aResp.getURL();
        setTextSelectionForUrlParams(url);
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
     * Reopen this page as SnapCodePage.
     */
    public void openAsSnapCode()
    {
        WebFile file = getFile();
        WebURL url = file.getURL();
        WebPage page = new SnapEditorPage(this);
        page.setFile(file);
        WebBrowser browser = getBrowser();
        browser.setPageForURL(url, page);
        browser.setURL(file.getURL());
    }

    /**
     * Creates a new file for use with showNewFilePanel method.
     */
    protected WebFile createNewFile(String aPath)
    {
        // Create file
        WebFile file = super.createNewFile(aPath);

        // Get project
        Project proj = Project.getProjectForFile(file);

        // Append package declaration
        StringBuilder sb = new StringBuilder();
        WebFile fileDir = file.getParent();
        String pkgName = proj != null ? proj.getClassNameForFile(fileDir) : file.getSimpleName();
        if (pkgName.length() > 0)
            sb.append("package ").append(pkgName).append(";\n");

        // Append Comment
        sb.append("\n/**\n * A custom class.\n */\n");

        // Append class declaration: "public class <File-Name> extends Object {   }"
        String className = file.getSimpleName();
        sb.append("public class ").append(className).append(" extends Object {\n\n\n\n}");

        // Set text
        file.setText(sb.toString());

        // Return
        return file;
    }

    /**
     * Override to set selection using browser.
     */
    private void setTextSel(int aStart, int anEnd)
    {
        String urls = getFile().getURL().getString() + String.format("#Sel=%d-%d", aStart, anEnd);
        getBrowser().setURLString(urls);
    }

    /**
     * Override to open declaration.
     */
    private void openDeclaration(JNode aNode)
    {
        JavaDecl decl = aNode.getDecl();
        if (decl != null) openDecl(decl);
    }

    /**
     * Open a super declaration.
     */
    private void openSuperDeclaration(JMemberDecl aMemberDecl)
    {
        JavaDecl sdecl = aMemberDecl.getSuperDecl();
        if (sdecl == null) return;
        openDecl(sdecl);
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
        if (proj == null) return;

        // Get source file
        WebFile file = proj.getProjectSet().getJavaFile(className);
        if (file == null) return;

        // Get matching node
        JavaAgent javaAgent = JavaAgent.getAgentForFile(file);
        JFile jfile = javaAgent.getJFile();
        JNode node = NodeMatcher.getDeclMatch(jfile, aDecl);

        // Get URL
        String urls = file.getURL().getString();
        if (node != null)
            urls += String.format("#Sel=%d-%d", node.getStartCharIndex(), node.getEndCharIndex());

        // Open URL
        getBrowser().setURLString(urls);
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
        String urlString = javaURL.getString() + "#Member=" + aDecl.getSimpleName();

        // Open URL
        getBrowser().setURLString(urlString);
    }

    /**
     * Show references for given node.
     */
    private void showReferences(JNode aNode)
    {
        if (getPodPane() == null) return;
        PodTools podTools = getPodPane().getPodTools();
        podTools.getSearchTool().searchReference(aNode);
        podTools.showToolForClass(SearchPane.class);
    }

    /**
     * Show declarations for given node.
     */
    private void showDeclarations(JNode aNode)
    {
        if (getPodPane() == null) return;
        PodTools podTools = getPodPane().getPodTools();
        podTools.getSearchTool().searchDeclaration(aNode);
        podTools.showToolForClass(SearchPane.class);
    }

    /**
     * Override to update Page.Modified.
     */
    private void setTextModified(boolean aFlag)
    {
        getFile().setUpdater(aFlag ? this : null);
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
    private int getProgramCounterLine()
    {
        PodPane podPane = getPodPane();
        return podPane != null ? podPane.getProcPane().getProgramCounter(getFile()) : -1;
    }

    /**
     * A JavaTextPane for a JavaPage to implement symbol features and such.
     */
    public class JPJavaTextPane extends JavaTextPaneX {

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

        /**
         * Respond to UI controls.
         */
        @Override
        public void respondUI(ViewEvent anEvent)
        {
            // Handle SnapCodeButton
            if (anEvent.equals("SnapCodeButton"))
                openAsSnapCode();

                // Do normal version
            else super.respondUI(anEvent);
        }
    }
}