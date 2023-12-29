package snapcode.app;
import snap.gfx.Color;
import snap.gfx.GFXEnv;
import snap.util.TaskRunner;
import snap.view.ScrollView;
import snap.view.View;
import snap.view.ViewArchiver;
import snap.view.ViewEvent;
import snap.web.WebURL;
import snapcode.project.Project;
import snapcode.project.Workspace;
import snapcode.webbrowser.WebBrowser;
import snapcode.webbrowser.WebPage;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.apptools.FilesTool;
import snapcode.project.RunConfig;
import snapcode.project.RunConfigs;

/**
 * A WebPage subclass that is the default homepage for SnapCode projects.
 */
public class HomePage extends WebPage {

    // Whether to do stupid animation (rotate buttons on mouse enter)
    boolean _stupidAnim;

    /**
     * Constructor.
     */
    public HomePage()
    {
        super();
    }

    /**
     * Returns the WorkspacePane.
     */
    public WorkspacePane getWorkspacePane()
    {
        WebBrowser browser = getBrowser();
        return browser.getOwner(WorkspacePane.class);
    }

    /**
     * Returns the WorkspacePane RootSite.
     */
    public WebSite getRootSite()
    {
        return getWorkspacePane().getRootSite();
    }

    /**
     * Override to put in Page pane.
     */
    @Override
    protected View createUI()
    {
        View superUI = super.createUI();
        ScrollView scrollView = new ScrollView(superUI);
        scrollView.setBorder(null);
        return scrollView;
    }

    /**
     * Initialize UI.
     */
    @Override
    public void initUI()
    {
        enableEvents("Header", MouseRelease);
        enableEvents("JavaPlayground", MouseEvents);
        enableEvents("NewSnapScene", MouseEvents);
        enableEvents("NewJavaFile", MouseEvents);
        enableEvents("NewFile", MouseEvents);
        enableEvents("SnapDocs", MouseEvents);
        enableEvents("RMDocs", MouseEvents);
    }

    /**
     * RespondUI.
     */
    @Override
    public void respondUI(ViewEvent anEvent)
    {
        // Trigger animations on main buttons for MouseEntered/MouseExited
        if (anEvent.isMouseEnter()) {
            if (_stupidAnim) anEvent.getView().getAnimCleared(200).setScale(1.12).getRoot(1000).setRotate(180).play();
            else anEvent.getView().getAnimCleared(200).setScale(1.12).play();
        }
        if (anEvent.isMouseExit()) {
            if (_stupidAnim) anEvent.getView().getAnimCleared(200).setScale(1).getRoot(1000).setRotate(0).play();
            else anEvent.getView().getAnimCleared(200).setScale(1).play();
        }

        // Handle Header: Play click anim and toggle StupidAnim
        if (anEvent.equals("Header")) {
            View hdr = anEvent.getView();
            _stupidAnim = !_stupidAnim;
            hdr.setBorder(_stupidAnim ? Color.MAGENTA : Color.BLACK, 1);
            hdr.setScale(1.05);
            hdr.getAnimCleared(200).setScale(1).play();
        }

        // Handle NewJavaFile
        if (anEvent.equals("JavaPlayground") && anEvent.isMouseRelease()) {

            // Create playground file
            WebSite rootSite = getRootSite();
            WebFile file = rootSite.createFileForPath("/Playground.pgd", false);

            // Set file
            if (file != null) {
                WebBrowser browser = getBrowser();
                try { browser.setFile(file); }
                catch (Exception e) { browser.showException(file.getURL(), e); }
            }
        }

        // Handle NewSnapScene
        if (anEvent.equals("NewSnapScene") && anEvent.isMouseRelease()) {
            addSnapKitDependency();
            addSceneFiles(getRootSite(), "Scene1");
        }

        // Handle NewJavaFile, NewFile
        if ((anEvent.equals("NewJavaFile") || anEvent.equals("NewFile")) && anEvent.isMouseRelease()) {
            WorkspaceTools workspaceTools = getWorkspacePane().getWorkspaceTools();
            FilesTool filesTool = workspaceTools.getFilesTool();
            filesTool.showNewFilePanel();
        }

        // Handle SnapDocs
        if (anEvent.equals("SnapDocs") && anEvent.isMouseRelease())
            GFXEnv.getEnv().openURL("https://www.reportmill.com/snap1/javadoc");

        // Handle RMDocs
        if (anEvent.equals("RMDocs") && anEvent.isMouseRelease())
            GFXEnv.getEnv().openURL("https://www.reportmill.com/support");
        //getBrowser().setURLString("http://www.reportmill.com/support/UserGuide.pdf");
    }

    /**
     * Adds SnapKit project to root project.
     */
    private void addSnapKitDependency()
    {
        // Get workspace
        WebSite rootSite = getRootSite();
        Project project = Project.getProjectForSite(rootSite);
        Workspace workspace = project.getWorkspace();

        // Get snapkit url
        WebURL snapkitRepoURL = WebURL.getURL("https://github.com/reportmill/SnapKit.git");
        assert (snapkitRepoURL != null);

        // Add SnapKit project to workspace
        TaskRunner<Boolean> taskRunner = workspace.addProjectForRepoURL(snapkitRepoURL);

        // When done, add project to root project
        taskRunner.setOnSuccess(completed -> project.addProjectForPath("SnapKit"));
    }

    /**
     * Makes given site a Studio project.
     */
    public void addSceneFiles(WebSite aSite, String aName)
    {
        // Create/add Scene Java and UI files
        addSceneJavaFile(aSite, aName);
        WebFile snpFile = addSceneUIFile(aSite, aName);
        if (snpFile == null)
            return;

        // Add run config
        RunConfigs rc = RunConfigs.get(aSite);
        if (rc.getRunConfig() == null) {
            rc.getRunConfigs().add(new RunConfig().setName("StudioApp").setMainClassName("Scene1"));
            rc.writeFile();
        }

        // Select and show snp file
        getBrowser().setFile(snpFile);
    }

    /**
     * Creates the Scene1.snp file.
     */
    protected WebFile addSceneUIFile(WebSite aSite, String aName)
    {
        // Get snap file (return if already exists)
        String path = "/src/" + aName + ".snp";
        WebFile sfile = aSite.getFileForPath(path);
        if (sfile != null) return null;

        // Create content
        snapcharts.repl.SnapScene scene = new snapcharts.repl.SnapScene();
        scene.setSize(800, 500);
        String str = new ViewArchiver().writeToXML(scene).getString();

        // Create file, set content, save and return
        sfile = aSite.createFileForPath(path, false);
        sfile.setText(str);
        try { sfile.save(); }
        catch (Exception e) { throw new RuntimeException(e); }
        sfile.setProp("OpenInEditor", true);
        return sfile;
    }

    /**
     * Creates the Scene file.
     */
    protected void addSceneJavaFile(WebSite aSite, String aName)
    {
        // Get snap file (return if already exists)
        String path = "/src/" + aName + ".java";
        WebFile jfile = aSite.getFileForPath(path);
        if (jfile != null)
            return;

        // Create content
        String sb = "import snap.view.*;\n\n" +
            "import snap.viewx.*;\n\n" +
            "/**\n" + " * A SnapStudio SceneOwner subclass. SnapEdit=true.\n" + " */\n" +
            "public class Scene1 extends SnapSceneOwner {\n\n" +
            "/**\n" + " * Initialize UI.\n" + " */\n" +
            "protected void initUI()\n" + "{\n}\n\n" +
            "/**\n" + " * Reset UI.\n" + " */\n" +
            "protected void resetUI()\n" + "{\n}\n\n" +
            "/**\n" + " * Respond to UI changes.\n" + " */\n" +
            "protected void respondUI(ViewEvent anEvent)\n" + "{\n}\n\n" +
            "/**\n" + " * Called on every frame.\n" + " */\n" +
            "public void act()\n" + "{\n}\n\n" +
            "/**\n" + " * Standard main method.\n" + " */\n" +
            "public static void main(String args[])\n{\n" +
            "    new Scene1().setWindowVisible(true);\n}\n\n" + "}";

        // Create file, set content, save and return
        jfile = aSite.createFileForPath(path, false);
        jfile.setText(sb);
        try { jfile.save(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Override to suppress.
     */
    public void reload()  { }

    /**
     * Return better title.
     */
    public String getTitle()  { return getRootSite().getName() + " Home Page";  }
}