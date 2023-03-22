package snapcode.apptools;
import javakit.project.Project;
import javakit.project.WorkspaceBuilder;
import snap.util.ArrayUtils;
import snap.util.FileUtils;
import snap.util.SnapUtils;
import snap.util.StringUtils;
import snap.view.View;
import snap.viewx.DialogBox;
import snap.viewx.FormBuilder;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebUtils;
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
     * Runs a panel for a new file (Java, JFX, Swing, table, etc.).
     */
    public void showNewFilePanel()
    {
        // Get new FormBuilder and configure
        FormBuilder form = new FormBuilder();
        form.setPadding(20, 5, 15, 5);
        form.addLabel("Select file type:           ").setFont(new snap.gfx.Font("Arial", 24));
        form.setSpacing(15);

        // Define options
        String[][] options = {
            { "Java Programming File", ".java" },
            { "SnapKit UI File", ".snp" },
            { "Directory", ".dir" },
            { "Sound File", ".wav" },
            { "ReportMill\u2122 Report Template", ".rpt" }
        };

        // Add and configure radio buttons
        for (int i = 0; i < options.length; i++) {
            String option = options[i][0];
            form.addRadioButton("EntryType", option, i == 0);
        }

        // Run dialog panel (just return if null)
        View workspacePaneUI = _workspacePane.getUI();
        if (!form.showPanel(workspacePaneUI, "New Project File", DialogBox.infoImage))
            return;

        // Select type and extension
        String desc = form.getStringValue("EntryType");
        int index = ArrayUtils.findMatchIndex(options, optionInfo -> desc.equals(optionInfo[0]));
        String extension = options[index][1];
        boolean isDir = extension.equals(".dir");
        if (isDir)
            extension = "";

        // Get source dir
        WebSite selSite = getSelSite();
        WebFile selFile = getSelFile();
        if (selFile.getSite() != selSite)
            selFile = selSite.getRootDir();
        WebFile selDir = selFile.isDir() ? selFile : selFile.getParent();
        if (extension.equals(".java") && selDir == selSite.getRootDir())
            selDir = Project.getProjectForSite(selSite).getSourceDir();

        // Get suggested "Untitled.xxx" path for SelDir and extension
        String path = selDir.getDirPath() + "Untitled" + extension;

        // Create suggested file and page
        WebFile file = selSite.createFileForPath(path, isDir);
        WebPage page = _pagePane.createPageForURL(file.getURL());

        // ShowNewFilePanel (just return if cancelled)
        file = page.showNewFilePanel(workspacePaneUI, file);
        if (file == null)
            return;

        // Save file
        try { file.save(); }
        catch (Exception e) {
            _pagePane.showException(file.getURL(), e);
            return;
        }

        // Select file
        setSelFile(file);

        // Hide RightTray if not Java
        if (!(extension.equals("java") || extension.equals("jepl")))
            _workspaceTools.getRightTray().setSelTool(null);
    }

    /**
     * Adds a list of files.
     */
    public boolean addFiles(List<File> theFiles)
    {
        // Get target (selected) directory
        WebSite site = getSelSite();
        WebFile selFile = getSelFile();
        if (selFile.getSite() != site)
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
        builder.buildWorkspaceLater(false);

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
            boolean isDuplicating = SnapUtils.equals(aFile, siteLocalFile);

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
        builder.buildWorkspaceLater(false);
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
     * Saves all unsaved files.
     */
    public void saveAllFiles()
    {
        WebSite rootSite = getRootSite();
        WebFile rootDir = rootSite.getRootDir();
        saveFiles(rootDir, true);
    }

    /**
     * Saves any unsaved files in given directory.
     */
    public int saveFiles(WebFile aFile, boolean doSaveAll)
    {
        // Handle directory
        if (aFile.isDir()) {

            // Skip build dir
            if (aFile == _workspacePane.getBuildDir())
                return doSaveAll ? 1 : 0;

            // Iterate over child files
            WebFile[] dirFiles = aFile.getFiles();
            for (WebFile file : dirFiles) {
                int choice = saveFiles(file, doSaveAll);
                if (choice < 0 || choice == 2)
                    return -1;
                if (choice == 1)
                    doSaveAll = true;
            }
        }

        // Handle file
        else if (aFile.isUpdateSet()) {

            // Show dialog box
            DialogBox dialogBox = new DialogBox("Save Modified File");
            dialogBox.setMessage("File has been modified:\n" + aFile.getPath());
            dialogBox.setOptions("Save File", "Save All Files", "Cancel");
            int choice = doSaveAll ? 1 : dialogBox.showOptionDialog(_workspacePane.getUI(), "Save File");

            // Handle Save file
            if (choice == 0 || choice == 1) {
                try { aFile.save(); }
                catch (Exception e) { throw new RuntimeException(e); }
            }

            // Return
            return choice;
        }

        // Return
        return doSaveAll ? 1 : 0;
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
}
