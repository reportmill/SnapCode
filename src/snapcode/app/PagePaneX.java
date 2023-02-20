package snapcode.app;
import snap.viewx.WebPage;
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

        //if(type.equals("snp")) return snapbuild.app.EditorPage.class;
        if (type.equals("class") && isProjectFile(file))
            return ClassInfoPage.class;
        if (type.equals("pgd"))
            return JavaShellPage.class;

        // Do normal version
        return super.getPageClass(aResp);
    }
}
