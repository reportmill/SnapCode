package snapcode.apptools;
import javakit.parse.JFile;
import javakit.parse.JavaParser;
import snap.view.Clipboard;
import snap.viewx.FilePanel;
import snap.web.*;
import snapcode.app.JavaPage;
import snapcode.app.SnapCodeUtils;
import snapcode.project.Project;
import snapcode.project.WorkspaceBuilder;
import snap.util.ArrayUtils;
import snap.util.FilePathUtils;
import snap.util.StringUtils;
import snap.view.View;
import snap.viewx.DialogBox;
import snap.viewx.FormBuilder;
import snapcode.webbrowser.WebPage;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import java.io.File;
import java.util.List;

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
        WebFile selDir = getSelDir();
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
    public void addFiles(List<File> theFiles)
    {
        // Get target (selected) directory
        WebFile selDir = getSelDir();

        // Get builder and disable AutoBuild
        WorkspaceBuilder builder = _workspace.getBuilder();
        builder.setAutoBuildEnabled(false);

        // Add files (disable site build)
        for (File file : theFiles) {
            WebFile webFile = WebFile.getFileForJavaFile(file); assert (webFile != null);
            if (!addFileToDirectory(selDir, webFile)) {
                break;
            }
        }

        // Enable auto build and build
        builder.setAutoBuildEnabled(true);
        builder.buildWorkspaceLater();
    }

    /**
     * Adds a file to given directory
     */
    public boolean addFileToDirectory(WebFile aDirectory, WebFile sourceFile)
    {
        // Handle directory: Create new directory and recurse
        if (sourceFile.isDir()) {

            // Create new directory
            WebSite dirSite = aDirectory.getSite();
            String dirPath = aDirectory.getDirPath() + sourceFile.getName();
            WebFile newDir = dirSite.createFileForPath(dirPath, true);

            // Recurse for source dir files
            WebFile[] dirFiles = sourceFile.getFiles();
            for (WebFile file : dirFiles)
                if (!addFileToDirectory(newDir, file))
                    return false;
            return true;
        }

        // Handle plain file
        return addFileToDirectoryImpl(aDirectory, sourceFile);
    }

    /**
     * Adds a simple file to given directory.
     */
    private boolean addFileToDirectoryImpl(WebFile aDirectory, WebFile sourceFile)
    {
        // Get site
        WebSite site = aDirectory.getSite();

        // Get name and file
        String fileName = sourceFile.getName();
        WebFile newFile = site.getFileForPath(aDirectory.getDirPath() + fileName);

        // If file exists, run option panel for replace
        if (newFile != null) {

            // If not duplicating, ask user if they want to Replace, Rename, Cancel
            String[] options = new String[] { "Replace", "Rename", "Cancel" };
            String defaultOption = "Replace";
            int option = 1;

            // If not duplicating, ask if user wants to proceed
            if (sourceFile != newFile) {
                String msg = "A file named " + fileName + " already exists in this location.\n Do you want to proceed?";
                DialogBox dialogBox = new DialogBox("Add File");
                dialogBox.setWarningMessage(msg);
                dialogBox.setOptions(options);
                option = dialogBox.showOptionDialog(_workspacePane.getUI(), defaultOption);
                if (option < 0 || options[option].equals("Cancel"))
                    return false;
            }

            // If duplicating or user wants to Rename, ask for new name
            if (options[option].equals("Rename")) {
                if (sourceFile == newFile)
                    fileName = "Duplicate " + fileName;
                DialogBox dialogBox = new DialogBox("Rename File");
                dialogBox.setQuestionMessage("Enter new file name:");
                fileName = dialogBox.showInputDialog(_workspacePane.getUI(), fileName);
                if (fileName == null)
                    return false;
                fileName = fileName.replace(" ", "");
                if (!StringUtils.endsWithIC(fileName, '.' + newFile.getType()))
                    fileName = fileName + '.' + newFile.getType();
                if (fileName.equals(sourceFile.getName()))
                    return addFileToDirectory(aDirectory, sourceFile);
            }
        }

        // Get file (force this time), set bytes, save and select file
        byte[] fileBytes = sourceFile.getBytes();
        newFile = addFileToDirectoryForNameAndBytes(aDirectory, fileName, fileBytes);
        setSelFile(newFile);
        return true;
    }

    /**
     * Adds a new file to selected directory for given name and bytes.
     */
    public void addFileForNameAndBytes(String fileName, byte[] fileBytes)
    {
        // Get target (selected) directory
        WebFile selDir = getSelDir();

        // Create new file for path and set bytes
        addFileToDirectoryForNameAndBytes(selDir, fileName, fileBytes);
    }

    /**
     * Adds a new file to given directory for given name and bytes.
     */
    public WebFile addFileToDirectoryForNameAndBytes(WebFile parentDir, String fileName, byte[] fileBytes)
    {
        // Create new file for path and set bytes
        String filePath = parentDir.getDirPath() + fileName;
        WebSite parentSite = parentDir.getSite();
        WebFile newFile = parentSite.createFileForPath(filePath, false);
        newFile.setBytes(fileBytes);

        // Save file
        newFile.save();

        // Return file
        return newFile;
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
    public Project createNewProjectForProjectDir(WebFile newProjectFile)
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
     * Returns the selected dir.
     */
    private WebFile getSelDir()
    {
        WebSite selSite = getSelSiteOrFirst();
        WebFile selFile = getSelFile();
        if (selFile == null || selFile.getSite() != selSite)
            selFile = selSite.getRootDir();
        return selFile.isDir() ? selFile : selFile.getParent();
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
