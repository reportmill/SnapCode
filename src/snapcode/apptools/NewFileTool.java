package snapcode.apptools;
import javakit.parse.JFile;
import javakit.parse.JavaParser;
import snap.util.ArrayUtils;
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
        String fileType = showNewFileTypePanel(_workspacePane.getUI());
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
        WebFile selDir = getSelDirOrFirst();
        if (fileType.equals("java") || fileType.equals("jepl")) {
            if (selDir.isRoot()) {
                Project proj = Project.getProjectForFile(selDir);
                selDir = proj.getSourceDir();
            }
        }

        // Forward to
        createFileForDirAndType(selDir, fileType);
    }

    /**
     * Creates a file for given path.
     */
    private void createFileForDirAndType(WebFile parentDir, String fileType)
    {
        // ShowNewFilePanel (just return if cancelled)
        WebFile newFile = showNewFilePanelForDirAndType(_workspacePane.getUI(), parentDir, fileType);
        if (newFile == null)
            return;

        // Initialize file
        switch (newFile.getFileType()) {
            case "java": JavaPage.initJavaFile(newFile); break;
            case "snp": snapcode.webbrowser.SnapBuilderPage.initSnapFile(newFile); break;
        }

        // Save file
        try { newFile.save(); }
        catch (Exception e) {
            _pagePane.showException(newFile.getURL(), e);
            return;
        }

        // Select file and hide right tray
        setSelFile(newFile);
        _workspaceTools.getRightTray().setSelTool(null);
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
        filePanel.getUI();

        // Set SelFile to SnapCodeDir
        WebFile snapCodeDir = SnapCodeUtils.getSnapCodeDir();
        filePanel.getSelSitePane().setSelFile(snapCodeDir);

        // Set TargFile to 'NewProjectX' dir
        for (int i = 0; i < 100; i++) {
            String newProjName = "NewProject" + (i > 0 ? i : "");
            WebFile newProjDir = SnapCodeUtils.getSnapCodeProjectDirForName(newProjName);
            if (!newProjDir.getExists()) {
                filePanel.getSelSitePane().setTargFile(newProjDir);
                break;
            }
        }

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
        String sourceType = sourceFile.getFileType();
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
    private static String showNewFileTypePanel(View aView)
    {
        // Get new FormBuilder and configure
        FormBuilder form = new FormBuilder();
        form.setPadding(20, 5, 15, 5);
        form.addLabel("Select file type:           ").setFont(new snap.gfx.Font("Arial", 24));
        form.setSpacing(15);

        // Define file options
        String[][] fileOptions = {
                {"Java File", "java"},
                {"Java File from clipboard", "java-from-clipboard"},
                {"Java REPL File", "jepl"},
                {"SnapKit UI File", "snp"},
                {"Directory", "dir"},
                {"Sound File", "wav"},
                {"ReportMill Report Template", "rpt"}
        };

        // Add and configure radio buttons
        for (int i = 0; i < fileOptions.length; i++) {
            String option = fileOptions[i][0];
            form.addRadioButton("EntryType", option, i == 0);
        }

        // Run dialog panel (just return if null)
        if (!form.showPanel(aView, "New Project File", DialogBox.infoImage))
            return null;

        // Get selected index and return file type
        String desc = form.getStringValue("EntryType");
        int selIndex = ArrayUtils.findMatchIndex(fileOptions, optionInfo -> desc.equals(optionInfo[0]));
        return fileOptions[selIndex][1];
    }

    /**
     * Runs a new show file panel for parent dir and type.
     */
    private static WebFile showNewFilePanelForDirAndType(View aView, WebFile parentDir, String fileType)
    {
        // Run input panel to get new file name
        String newFilePanelTitle = "New " + fileType + " File";
        DialogBox newFilePanel = new DialogBox(newFilePanelTitle);
        newFilePanel.setQuestionMessage("Enter " + fileType + " file name");
        String filename = newFilePanel.showInputDialog(aView, "Untitled");
        if (filename == null)
            return null;

        // Strip spaces from filename (for now) and make sure it has extension
        filename = filename.replace(" ", "");
        if (!filename.toLowerCase().endsWith('.' + fileType) && fileType.length() > 0)
            filename = filename + '.' + fileType;

        // If file already exists, run option panel for replace
        WebFile newFile = parentDir.getFileForName(filename);
        if (newFile != null) {
            String replaceMessage = "A file named " + newFile.getName() + " already exists.\n Do you want to replace it with new file?";
            DialogBox replaceFilePanel = new DialogBox(newFilePanelTitle);
            replaceFilePanel.setWarningMessage(replaceMessage);
            if (!replaceFilePanel.showConfirmDialog(aView))
                return showNewFilePanelForDirAndType(aView, parentDir, fileType);
            return newFile;
        }

        // Create file and return
        WebSite fileSite = parentDir.getSite();
        String filePath = filename.startsWith("/") ? filename : parentDir.getDirPath() + filename;
        boolean isDir = fileType.length() == 0 || fileType.equals("dir");
        return fileSite.createFileForPath(filePath, isDir);
    }
}
