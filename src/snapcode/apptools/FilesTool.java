package snapcode.apptools;
import javakit.parse.JFile;
import javakit.parse.JavaParser;
import snap.view.Clipboard;
import snap.viewx.FilePanel;
import snap.web.RecentFiles;
import snapcode.app.JavaPage;
import snapcode.app.SnapCodeUtils;
import snapcode.project.Project;
import snapcode.project.WorkspaceBuilder;
import snap.util.ArrayUtils;
import snap.util.FilePathUtils;
import snap.util.FileUtils;
import snap.util.StringUtils;
import snap.view.View;
import snap.viewx.DialogBox;
import snap.viewx.FormBuilder;
import snapcode.webbrowser.WebPage;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebUtils;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * This class is a WorkspaceTool to manage file operations on project files: create, add, remove, rename.
 */
public class FilesTool extends WorkspaceTool {

    /**
     * Constructor.
     */
    public FilesTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Runs a panel for a new file (Java, Jepl, snp, etc.).
     */
    public void showNewFilePanel()
    {
        // Run dialog panel for extension
        View workspacePaneUI = _workspacePane.getUI();
        String extension = showNewFileExtensionPanel(workspacePaneUI);
        if (extension == null)
            return;

        // Handle Java from clipboard
        if (extension.equals(".java-from-clipboard")) {
            newJavaFileFromClipboard();
            return;
        }

        // Get source dir
        WebSite selSite = getSelSiteOrFirst();
        WebFile selFile = getSelFile();
        if (selFile == null || selFile.getSite() != selSite)
            selFile = selSite.getRootDir();
        WebFile selDir = selFile.isDir() ? selFile : selFile.getParent();
        if (extension.equals(".java") || extension.equals(".jepl")) {
            if (selDir == selSite.getRootDir()) {
                Project proj = Project.getProjectForSite(selSite);
                selDir = proj.getSourceDir();
            }
        }

        // Get suggested "Untitled.xxx" path for SelDir and extension
        String filePath = selDir.getDirPath() + "Untitled" + extension;

        // Create file
        WebFile file = createFileForPath(filePath, workspacePaneUI);

        // Select file
        setSelFile(file);

        // Hide RightTray
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
     * Adds a list of files.
     */
    public boolean addFiles(List<File> theFiles)
    {
        // Get target (selected) directory
        WebSite site = getSelSiteOrFirst();
        WebFile selFile = getSelFile();
        if (selFile == null || selFile.getSite() != site)
            selFile = site.getRootDir();
        WebFile selDir = selFile.isDir() ? selFile : selFile.getParent();

        // Get builder and disable AutoBuild
        WorkspaceBuilder builder = _workspace.getBuilder();
        builder.setAutoBuildEnabled(false);

        // Add files (disable site build)
        boolean success = true;
        for (File file : theFiles) {
            if (!addFileToDirectory(selDir, file)) {
                success = false;
                break;
            }
        }

        // Enable auto build and build
        builder.setAutoBuildEnabled(true);
        builder.buildWorkspaceLater();

        // Return files
        return success && theFiles.size() > 0;
    }

    /**
     * Adds a file.
     */
    public boolean addFileToDirectory(WebFile aDirectory, File aFile)
    {
        // Get site
        WebSite site = aDirectory.getSite();

        // Handle directory
        if (aFile.isDirectory()) {

            // Create new directory
            String dirPath = aDirectory.getDirPath() + aFile.getName();
            WebFile directory = site.createFileForPath(dirPath, true);
            File[] dirFiles = aFile.listFiles();
            if (dirFiles != null) {
                for (File file : dirFiles)
                    addFileToDirectory(directory, file);
            }
        }

        // Handle plain file
        else {

            // Get name and file
            String name = aFile.getName();
            WebFile siteFile = site.getFileForPath(aDirectory.getDirPath() + name);

            // See if IsDuplicating (there is a local file and it is the same as given file)
            File siteLocalFile = siteFile != null ? siteFile.getJavaFile() : null;
            boolean isDuplicating = Objects.equals(aFile, siteLocalFile);

            // If file exists, run option panel for replace
            if (siteFile != null) {

                // If not duplicating, ask user if they want to Replace, Rename, Cancel
                String[] options = new String[]{"Replace", "Rename", "Cancel"};
                String defaultOption = "Replace";
                int option = 1;
                if (!isDuplicating) {
                    String msg = "A file named " + name + " already exists in this location.\n Do you want to proceed?";
                    DialogBox dbox = new DialogBox("Add File");
                    dbox.setWarningMessage(msg);
                    dbox.setOptions(options);
                    option = dbox.showOptionDialog(_workspacePane.getUI(), defaultOption);
                    if (option < 0 || options[option].equals("Cancel")) return false;
                }

                // If user wants to Rename, ask for new name
                if (options[option].equals("Rename")) {
                    if (isDuplicating) name = "Duplicate " + name;
                    DialogBox dbox = new DialogBox("Rename File");
                    dbox.setQuestionMessage("Enter new file name:");
                    name = dbox.showInputDialog(_workspacePane.getUI(), name);
                    if (name == null) return false;
                    name = name.replace(" ", "");
                    if (!StringUtils.endsWithIC(name, '.' + siteFile.getType())) name = name + '.' + siteFile.getType();
                    if (name.equals(aFile.getName()))
                        return addFileToDirectory(aDirectory, aFile);
                }
            }

            // Get file (force this time), set bytes, save and select file
            siteFile = site.createFileForPath(aDirectory.getDirPath() + name, false);
            siteFile.setBytes(FileUtils.getBytes(aFile));
            try { siteFile.save(); }
            catch (Exception e) { throw new RuntimeException(e); }
            setSelFile(siteFile);
        }

        // Return true
        return true;
    }

    /**
     * Removes a list of files.
     */
    public void removeFiles(List<WebFile> theFiles)
    {
        // Get ProjectPane and disable AutoBuild
        WebFile file0 = theFiles.size() > 0 ? theFiles.get(0) : null;
        if (file0 == null) {
            beep();
            return;
        }

        // Get builder and disable AutoBuild
        WorkspaceBuilder builder = _workspace.getBuilder();
        builder.setAutoBuildEnabled(false);

        // Add files (disable site build)
        for (WebFile file : theFiles) {
            try {
                if (file.getExists())
                    removeFile(file);
            }
            catch (Exception e) {
                e.printStackTrace();
                beep();
            }
        }

        // Enable auto build and build
        builder.setAutoBuildEnabled(true);
        builder.buildWorkspaceLater();
    }

    /**
     * Deletes a file.
     */
    public void removeFile(WebFile aFile)
    {
        try { aFile.delete(); }
        catch (Exception e) { throw new RuntimeException(e); }
        _pagePane.removeOpenFile(aFile);
    }

    /**
     * Renames a file.
     */
    public boolean renameFile(WebFile aFile, String aName)
    {
        // TODO - this is totally bogus
        if (aFile.isDir() && aFile.getFileCount() > 0) {
            //File file = getLocalFile(aFile), file2 = new File(file.getParentFile(), aName); file.renameTo(file2);
            DialogBox dbox = new DialogBox("Can't rename non-empty directory");
            dbox.setErrorMessage("I know this is bogus, but app can't yet rename non-empty directory");
            dbox.showMessageDialog(_workspacePane.getUI());
            return false;
        }

        // Get file name (if no extension provided, default to file extension) and path
        String name = aName;
        if (name.indexOf('.') < 0 && aFile.getType().length() > 0) name += "." + aFile.getType();
        String path = aFile.getParent().getDirPath() + name;

        // If file for NewPath already exists, complain
        if (aFile.getSite().getFileForPath(path) != null) {
            beep();
            return false;
        }

        // Set bytes and save
        WebFile newFile = aFile.getSite().createFileForPath(path, aFile.isDir());
        newFile.setBytes(aFile.getBytes());
        try { newFile.save(); }
        catch (Exception e) { throw new RuntimeException(e); }

        // Remove old file
        removeFile(aFile);

        // Select new file pane
        setSelFile(newFile);

        // Return true
        return true;
    }

    /**
     * Runs the remove file panel.
     */
    public void showRemoveFilePanel()
    {
        // Get selected files - if any are root, beep and return
        List<WebFile> files = getSelFiles();
        for (WebFile file : files)
            if (file.isRoot()) {
                beep();
                return;
            }

        // Give the user one last chance to bail
        DialogBox dialogBox = new DialogBox("Remove File(s)");
        dialogBox.setQuestionMessage("Are you sure you want to remove the currently selected File(s)?");
        if (!dialogBox.showConfirmDialog(_workspacePane.getUI()))
            return;

        // Get top parent
        WebFile parent = files.size() > 0 ? files.get(0).getParent() : null;
        for (WebFile file : files)
            parent = WebUtils.getCommonAncestor(parent, file);

        // Remove files (check File.Exists in case previous file was a parent directory)
        removeFiles(files);

        // Update tree again
        setSelFile(parent);
    }

    /**
     * Shows a panel to create new project.
     */
    public Project showNewProjectPanel(View aView)
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
        WebFile newProjectFile = filePanel.showFilePanel(aView);
        if (newProjectFile == null)
            return null;

        // Make sure file is dir
        if (!newProjectFile.isDir()) {
            WebSite fileSite = newProjectFile.getSite();
            newProjectFile = fileSite.createFileForPath(newProjectFile.getPath(), true);
        }

        // Return
        return createNewProjectForProjectDir(newProjectFile);
    }

    /**
     * Creates a new project.
     */
    private Project createNewProjectForProjectDir(WebFile newProjectFile)
    {
        // Create new project
        WebSite projectSite = newProjectFile.getURL().getAsSite();
        Project newProject = _workspace.getProjectForSite(projectSite);

        // Create src and build files
        newProject.getSourceDir().save();
        newProject.getBuildFile().setIncludeSnapKitRuntime(true);
        newProject.getBuildFile().writeFile();
        newProjectFile.resetAndVerify();

        // Add project
        _workspace.addProject(newProject);

        // Add project to recent files
        RecentFiles.addURL(newProjectFile.getURL());

        // Return
        return newProject;
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

        if (sourceType.equals("java"))
            return newJavaFileForString(sourceText);
        if (sourceType.equals("jepl"))
            return newJeplFileForNameAndString(sourceName, sourceText);
        return null;
    }

    /**
     * Creates a new Java file from given string.
     */
    public WebFile newJavaFileForString(String javaString)
    {
        // Get Java class name
        JavaParser javaParser = JavaParser.getShared();
        JFile jfile = javaParser.getJavaFile(javaString);
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
     * Runs a panel for a new file extension (Java, Jepl, snp, etc.).
     */
    public static String showNewFileExtensionPanel(View aView)
    {
        // Get new FormBuilder and configure
        FormBuilder form = new FormBuilder();
        form.setPadding(20, 5, 15, 5);
        form.addLabel("Select file type:           ").setFont(new snap.gfx.Font("Arial", 24));
        form.setSpacing(15);

        // Define options
        String[][] options = {
                {"Java File", ".java"},
                {"Java File from clipboard", ".java-from-clipboard"},
                {"Java REPL File", ".jepl"},
                {"SnapKit UI File", ".snp"},
                {"Directory", ".dir"},
                {"Sound File", ".wav"},
                {"ReportMill\u2122 Report Template", ".rpt"}
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
        boolean isDir = extension.equals(".dir");
        if (isDir)
            extension = "";

        // Return
        return extension;
    }

    /**
     * Returns the selected site or first site.
     */
    private WebSite getSelSiteOrFirst()
    {
        WebSite selSite = getSelSite();
        if (selSite == null)
            selSite = getRootSite();
        return selSite;
    }
}
