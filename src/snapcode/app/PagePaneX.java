package snapcode.app;
import snapcode.webbrowser.BuildDirPage;
import snapcode.webbrowser.WebPage;
import snap.web.WebFile;
import snap.web.WebResponse;
import snapcode.util.ClassInfoPage;

/**
 * This PagePane subclass is for SnapCodePro.
 */
public class PagePaneX extends PagePane {

    /**
     * Constructor.
     */
    public PagePaneX(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Returns whether page is available for file.
     */
    @Override
    protected boolean isPageAvailableForFile(WebFile aFile)
    {
        if (super.isPageAvailableForFile(aFile))
            return true;
        if (aFile == _workspacePane.getBuildDir())
            return true;
        return false;
    }

    /**
     * Creates a WebPage for given file.
     */
    protected Class<? extends WebPage> getPageClass(WebResponse aResp)
    {
        // Get file and data
        WebFile file = aResp.getFile();
        String type = aResp.getPathType();

        // Handle Project Root directory
        if (file != null && file.isRoot() && isProjectFile(file))
            return ProjectPane.ProjectPanePage.class;

        // Handle Java
        if (type.equals("java") || type.equals("jepl"))
            return JavaPageX.class;

        // Handle BuildDir
        WebFile snapFile = aResp.getFile();
        if (snapFile == _workspacePane.getBuildDir())
            return BuildDirPage.class;

        // Handle class file
        if (type.equals("class") && isProjectFile(file))
            return ClassInfoPage.class;
        if (type.equals("pgd"))
            return JavaShellPage.class;

        // Do normal version
        return super.getPageClass(aResp);
    }
}
