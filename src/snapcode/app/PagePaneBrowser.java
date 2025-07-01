package snapcode.app;
import snap.web.WebFile;
import snap.web.WebResponse;
import snapcode.apptools.BuildFileTool;
import snapcode.util.ClassInfoPage;
import snapcode.webbrowser.*;

/**
 * This class is a WebBrowser subclass for PagePane.
 */
public class PagePaneBrowser extends WebBrowser {

    // The PagePane
    private PagePane _pagePane;

    /**
     * Constructor.
     */
    public PagePaneBrowser(PagePane aPagePane)
    {
        super();
        _pagePane = aPagePane;
        setGrowHeight(true);
    }

    /**
     * Sets the selected site file.
     */
    @Override
    public void setSelFile(WebFile aFile)
    {
        // If file already set, just return
        if (aFile == null || aFile == getSelFile()) return;

        // If file already open, make it show instantly
        if (_pagePane.getOpenFiles().contains(aFile))
            setTransition(WebBrowser.Instant);

        // Do normal version
        super.setSelFile(aFile);
    }

    /**
     * Returns WebPage class for given response.
     */
    @Override
    protected Class<? extends WebPage> getPageClassForResponse(WebResponse aResp)
    {
        // Handle common types
        switch (aResp.getFileType()) {

            // Handle Java / Jepl
            case "java": case "jepl": return JavaPage.class;

            // Handle JMD
            case "jmd": return JMDPage.class;

            // Handle Snap file
            case "snp": return SnapBuilderPage.class;

            // Handle mark down file
            case "md": return MarkDownPage.class;

            // Handle build file (build.snapcode)
            case "snapcode": return BuildFileTool.BuildFilePage.class;

            // Handle project.greenfoot file
            case "greenfoot": return GreenfootPage.class;

            // Handle class file
            case "class": return ClassInfoPage.class;
        }

        // Handle Project Root directory
        WebFile file = aResp.getFile();
        if (file != null && file.isRoot() && _pagePane.isProjectFile(file))
            return ProjectPane.ProjectPanePage.class;

        // Handle BuildDir
        if (file == _pagePane._workspacePane.getBuildDir())
            return BuildDirPage.class;

        // Do normal version
        return super.getPageClassForResponse(aResp);
    }
}
