package snapcode.apptools;
import snap.web.*;
import snapcode.project.WorkspaceBuilder;
import snap.util.StringUtils;
import snap.viewx.DialogBox;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
     * Adds a list of files.
     */
    public void addFiles(List<File> theFiles)
    {
        // Get target (selected) directory
        WebFile selDir = getSelDirOrFirst();

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
                if (!StringUtils.endsWithIC(fileName, '.' + newFile.getFileType()))
                    fileName = fileName + '.' + newFile.getFileType();
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
        WebFile selDir = getSelDirOrFirst();

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
        if (name.indexOf('.') < 0 && aFile.getFileType().length() > 0) name += "." + aFile.getFileType();
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
     * Creates a zip file for given directory file.
     */
    public static void zipDirectory(File dirToZip, File zipFile) throws IOException
    {
        try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
            addDirectoryToZip(dirToZip, dirToZip.getName(), zos);
        }
    }

    /**
     * Adds a directory to zip output stream.
     */
    private static void addDirectoryToZip(File dir, String baseName, ZipOutputStream zos) throws IOException
    {
        File[] files = dir.listFiles();

        // Handle empty directory: Create a directory entry in the zip file
        if (files == null || files.length == 0) {
            ZipEntry entry = new ZipEntry(baseName + "/");
            zos.putNextEntry(entry);
            zos.closeEntry();
        }

        // Handle normal directory: Iterate over files and either Recursively add directory or add file
        else {
            for (File file : files) {
                if (file.isDirectory())
                    addDirectoryToZip(file, baseName + "/" + file.getName(), zos);
                else addFileToZip(file, baseName, zos);
            }
        }
    }

    /**
     * Adds a file to zip output stream.
     */
    private static void addFileToZip(File file, String baseName, ZipOutputStream zos) throws IOException
    {
        try (FileInputStream fis = new FileInputStream(file)) {
            String zipEntryName = baseName + "/" + file.getName();
            ZipEntry entry = new ZipEntry(zipEntryName);
            zos.putNextEntry(entry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
        }
    }
}
