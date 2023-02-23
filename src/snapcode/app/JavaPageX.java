package snapcode.app;
import javakit.ide.JavaTextPane;
import javakit.ide.JavaTextPaneX;
import javakit.parse.*;
import snap.view.ViewEvent;
import snap.viewx.WebBrowser;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snap.web.WebURL;
import snapcode.apptools.ProcPane;
import snapcode.apptools.SearchTool;
import snapcode.views.SnapEditorPage;

/**
 * A JavaPage subclass to view/edit Java files.
 */
public class JavaPageX extends JavaPage {

    /**
     * Constructor.
     */
    public JavaPageX()
    {
        super();
    }

    /**
     * Override to return custom.
     */
    @Override
    protected JavaTextPane<?> createJavaTextPane()  { return new JPJavaTextPaneX(); }

    /**
     * Show references for given node.
     */
    @Override
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
    @Override
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
    @Override
    protected int getProgramCounterLine()
    {
        WorkspacePane workspacePane = getWorkspacePane();
        WorkspaceTools workspaceTools = workspacePane.getWorkspaceTools();
        ProcPane procPane = workspaceTools.getToolForClass(ProcPane.class);
        return procPane != null ? procPane.getProgramCounter(getFile()) : -1;
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
     * A JavaTextPane for a JavaPage to implement symbol features and such.
     */
    public class JPJavaTextPaneX extends JavaTextPaneX {

        /**
         * Override to set selection using browser.
         */
        public void setTextSel(int aStart, int anEnd)
        {
            JavaPageX.this.setTextSel(aStart, anEnd);
        }

        /**
         * Override to open declaration.
         */
        public void openDeclaration(JNode aNode)
        {
            JavaPageX.this.openDeclaration(aNode);
        }

        /**
         * Open a super declaration.
         */
        public void openSuperDeclaration(JMemberDecl aMemberDecl)
        {
            JavaPageX.this.openSuperDeclaration(aMemberDecl);
        }

        /**
         * Show references for given node.
         */
        public void showReferences(JNode aNode)
        {
            JavaPageX.this.showReferences(aNode);
        }

        /**
         * Show declarations for given node.
         */
        public void showDeclarations(JNode aNode)
        {
            JavaPageX.this.showDeclarations(aNode);
        }

        /**
         * Override to update Page.Modified.
         */
        public void setTextModified(boolean aFlag)
        {
            super.setTextModified(aFlag);
            JavaPageX.this.setTextModified(aFlag);
        }

        /**
         * Override to get ProgramCounter from ProcPane.
         */
        public int getProgramCounterLine()
        {
            return JavaPageX.this.getProgramCounterLine();
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