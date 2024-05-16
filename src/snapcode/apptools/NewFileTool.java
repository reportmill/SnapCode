package snapcode.apptools;
import javakit.parse.JFile;
import javakit.parse.JavaParser;
import snap.util.ArrayUtils;
import snap.util.FilePathUtils;
import snap.view.Clipboard;
import snap.view.View;
import snap.viewx.DialogBox;
import snap.viewx.FilePanel;
import snap.viewx.FormBuilder;
import snap.web.RecentFiles;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.app.JavaPage;
import snapcode.app.SnapCodeUtils;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import snapcode.project.Project;
import snapcode.project.ProjectUtils;
import snapcode.webbrowser.WebPage;

/**
 * This class is a WorkspaceTool to manage creating new files.
 */
public class NewFileTool extends WorkspaceTool {

    /**
     * Constructor.
     */
    public NewFileTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Runs a panel for a new file (Java, Jepl, snp, etc.).
     */
    public void showNewFilePanel()
    {
        // Run dialog panel for file type
        View workspacePaneUI = _workspacePane.getUI();
        String fileType = showNewFilePanel(workspacePaneUI);
        if (fileType == null)
            return;

        // Forward to createFileForType
        createFileForType(fileType);
    }

    /**
     * Creates a file for given type.
     */
    public void createFileForType(String fileType)
    {
        // If no projects, create starter file
        if (_workspace.getProjects().length == 0) {
            createStarterFileForType(fileType);
            return;
        }

        // Handle Java from clipboard
        if (fileType.equals("java-from-clipboard")) {
            newJavaFileFromClipboard();
            return;
        }

        // Get source dir
        WebSite selSite = getSelSiteOrFirst();
        WebFile selDir = getSelDirOrFirst();
        if (fileType.equals("java") || fileType.equals("jepl")) {
            if (selDir == selSite.getRootDir()) {
                Project proj = Project.getProjectForSite(selSite);
                selDir = proj.getSourceDir();
            }
        }

        // Get suggested "Untitled.xxx" path for SelDir and extension
        String filePath = selDir.getDirPath() + "Untitled." + fileType;

        // Create file
        WebFile file = createFileForPath(filePath, _workspacePane.getUI());

        // Select file and hide right tray
        setSelFile(file);
        _workspaceTools.getRightTray().setSelTool(null);
    }

    /**
     * Creates a file for given path.
     */
    public WebFile createFileForPath(String aPath, View aView)
    {
        // Create suggested file and page
        boolean isDir = FilePathUtils.getExtension(aPath).length() == 0;
        WebSite selSite = getSelSiteOrFirst();
        WebFile newFile = selSite.createFileForPath(aPath, isDir);
        WebPage page = _pagePane.createPageForURL(newFile.getURL());

        // ShowNewFilePanel (just return if cancelled)
        newFile = page.showNewFilePanel(aView, newFile);
        if (newFile == null)
            return null;

        // Save file
        try { newFile.save(); }
        catch (Exception e) {
            _pagePane.showException(newFile.getURL(), e);
            return null;
        }

        // Return
        return newFile;
    }

    /**
     * Shows a panel to create new project.
     */
    public void showNewProjectPanel()
    {
        // Create file panel to select new directory file
        FilePanel filePanel = new FilePanel();
        filePanel.setSaving(true);
        filePanel.setDesc("Create New Project");

        // Initialize to SnapCode dir
        filePanel.getUI();
        WebFile snapCodeDir = SnapCodeUtils.getSnapCodeDir();
        if (snapCodeDir.getExists())
            filePanel.getSelSitePane().setSelFile(snapCodeDir);

        // Show file panel to select new directory file
        WebFile newProjectFile = filePanel.showFilePanel(_workspacePane.getUI());
        if (newProjectFile == null)
            return;

        // Make sure file is dir
        if (!newProjectFile.isDir()) {
            WebSite fileSite = newProjectFile.getSite();
            newProjectFile = fileSite.createFileForPath(newProjectFile.getPath(), true);
        }

        // Return
        createNewProjectForProjectDir(newProjectFile);
    }

    /**
     * Creates a new project.
     */
    public void createNewProjectForProjectDir(WebFile newProjectFile)
    {
        // Create new project
        WebSite projectSite = newProjectFile.getURL().getAsSite();
        Project newProject = _workspace.getProjectForSite(projectSite);

        // Configure to include SnapKit
        newProject.getBuildFile().setIncludeSnapKitRuntime(true);

        // Add project
        _workspace.addProject(newProject);

        // Add project to recent files
        RecentFiles.addURL(newProjectFile.getURL());
    }

    /**
     * Creates a new Java or Jepl file for name and string.
     */
    private void createStarterFileForType(String fileType)
    {
        // Get Temp project
        ProjectUtils.getTempProject(_workspace);

        // Create file
        newJavaOrJeplFileForNameAndTypeAndString("JavaFiddle", fileType, "");
    }

    /**
     * Creates a new Java or Jepl file for name and string.
     */
    public WebFile newJavaOrJeplFileForNameAndTypeAndString(String className, String fileType, String javaString)
    {
        if (fileType.equals("jepl"))
            return newJeplFileForNameAndString(className, javaString);
        return newJavaFileForName(className);
    }

    /**
     * Creates a new Java file for given class name.
     */
    public WebFile newJavaFileForName(String className)
    {
        String javaString = JavaPage.getJavaContentStringForPackageAndClassName(null, className);
        return newJavaFileForString(javaString);
    }

    /**
     * Creates a new source file for given external source file.
     */
    public WebFile newSourceFileForExternalSourceFile(WebFile sourceFile)
    {
        String sourceName = sourceFile.getSimpleName();
        String sourceType = sourceFile.getType();
        String sourceText = sourceFile.getText();

        if (sourceType.equals("java") || sourceType.equals("jepl"))
            return newJavaOrJeplFileForNameAndTypeAndString(sourceName, sourceType, sourceText);
        return null;
    }

    /**
     * Creates a new Java file from given string.
     */
    public WebFile newJavaFileForString(String javaString)
    {
        // Get Java class name
        JavaParser javaParser = JavaParser.getShared();
        JFile jfile = javaParser.parseFile(javaString);
        String className = jfile.getName();
        if (className == null || className.length() == 0) {
            String title = "New Java File from clipboard";
            String msg = "No class name found";
            DialogBox.showErrorDialog(_workspacePane.getUI(), title, msg);
            return null;
        }

        // Get source dir
        WebSite selSite = getSelSiteOrFirst();
        Project proj = Project.getProjectForSite(selSite);
        WebFile selDir = proj.getSourceDir();

        // Create file and save
        String filePath = selDir.getDirPath() + className + ".java";
        WebFile newJavaFile = selSite.createFileForPath(filePath, false);
        newJavaFile.setText(javaString);
        newJavaFile.save();
        setSelFile(newJavaFile);

        // Start build?
        _workspace.getBuilder().buildWorkspaceLater();

        // Return
        return newJavaFile;
    }

    /**
     * Creates a new Java file from clipboard.
     */
    public void newJavaFileFromClipboard()
    {
        // Get Java string from clipboard
        Clipboard clipboard = Clipboard.get();
        String javaString = clipboard.getString();
        if (javaString == null || javaString.length() < 10) {
            String title = "New Java File from clipboard";
            String msg = "No text found in clibpard";
            DialogBox.showErrorDialog(_workspacePane.getUI(), title, msg);
            return;
        }

        // Create java file for java string
        newJavaFileForString(javaString);
    }

    /**
     * Creates a new Jepl file from given string.
     */
    public WebFile newJeplFileForNameAndString(String jeplName, String jeplString)
    {
        // Get source dir
        WebSite selSite = getSelSiteOrFirst();
        Project proj = Project.getProjectForSite(selSite);
        WebFile selDir = proj.getSourceDir();

        // Create file and save
        String filePath = selDir.getDirPath() + jeplName + ".jepl";
        WebFile newJeplFile = selSite.createFileForPath(filePath, false);
        newJeplFile.setText(jeplString);
        newJeplFile.save();
        setSelFile(newJeplFile);

        // Start build?
        _workspace.getBuilder().buildWorkspaceLater();

        // Return
        return newJeplFile;
    }

    /**
     * Runs a panel for a new file type (Java, Jepl, snp, etc.).
     */
    private static String showNewFilePanel(View aView)
    {
        // Get new FormBuilder and configure
        FormBuilder form = new FormBuilder();
        form.setPadding(20, 5, 15, 5);
        form.addLabel("Select file type:           ").setFont(new snap.gfx.Font("Arial", 24));
        form.setSpacing(15);

        // Define options
        String[][] options = {
                {"Java File", "java"},
                {"Java File from clipboard", "java-from-clipboard"},
                {"Java REPL File", "jepl"},
                {"SnapKit UI File", "snp"},
                {"Directory", "dir"},
                {"Sound File", "wav"},
                {"ReportMill Report Template", "rpt"}
        };

        // Add and configure radio buttons
        for (int i = 0; i < options.length; i++) {
            String option = options[i][0];
            form.addRadioButton("EntryType", option, i == 0);
        }

        // Run dialog panel (just return if null)
        if (!form.showPanel(aView, "New Project File", DialogBox.infoImage))
            return null;

        // Select type and extension
        String desc = form.getStringValue("EntryType");
        int index = ArrayUtils.findMatchIndex(options, optionInfo -> desc.equals(optionInfo[0]));
        String extension = options[index][1];

        // Return
        return extension;
    }
}
