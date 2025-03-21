package snapcode.apptools;
import snap.util.ArrayUtils;
import snapcode.project.BuildIssue;
import snapcode.javatext.JavaTextUtils;
import snapcode.project.BuildIssues;
import snapcode.project.Project;
import snapcode.project.Workspace;
import snap.geom.Pos;
import snap.gfx.Image;
import snapcode.project.VersionControl;
import snap.view.*;
import snap.web.WebFile;
import snapcode.util.FileIcons;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class to represent a project file in ProjectFilesTool.
 */
public class FileTreeFile implements Comparable<FileTreeFile> {

    // The parent file
    protected FileTreeFile _parent;

    // The WebFile
    protected WebFile  _file;

    // The children
    protected FileTreeFile[]  _children;

    // The file type
    protected FileType  _type = FileType.PLAIN;

    // The project
    protected Project  _proj;

    // The WebFile ProjectFileInfo
    protected VersionControl  _vc;

    // The priority of this item
    protected int  _priority;

    // The file type
    public enum FileType { PLAIN, PACKAGE_DIR, SOURCE_DIR }

    // Icons
    private static Image ErrorBadge = Image.getImageForClassResource(FileTreeFile.class, "ErrorBadge.png");
    private static Image WarningBadge = Image.getImageForClassResource(FileTreeFile.class, "WarningBadge.png");
    private static Image Package = JavaTextUtils.PackageImage;

    /**
     * Creates a new file.
     */
    public FileTreeFile(FileTreeFile aPar, WebFile aFile)
    {
        _parent = aPar;
        _file = aFile;
        _proj = Project.getProjectForSite(aFile.getSite());
        _vc = VersionControl.getVersionControlForProjectSite(_file.getSite());
    }

    /**
     * Returns the parent.
     */
    public FileTreeFile getParent()  { return _parent; }

    /**
     * Returns the real file.
     */
    public WebFile getFile()  { return _file; }

    /**
     * Returns whether file is parent.
     */
    public boolean isParent()  { return _file.isDir(); }

    /**
     * Returns the children.
     */
    public FileTreeFile[] getChildren()
    {
        // If already set, just return
        if (_children != null) return _children;

        // Get child files mapped to tree files and sort
        WebFile[] files = getChildFiles();
        FileTreeFile[] treeFiles = ArrayUtils.mapNonNull(files, file -> createChildFile(file), FileTreeFile.class);
        Arrays.sort(treeFiles);

        // Return
        return _children = treeFiles;
    }

    /**
     * Returns a FileTreeItem for child file.
     */
    private FileTreeFile createChildFile(WebFile aFile)
    {
        // Get basic file info
        String name = aFile.getName();
        boolean dir = aFile.isDir();
        String type = aFile.getFileType();
        int typeLen = type.length();

        // Skip hidden files, child packages
        if (name.startsWith("."))
            return null;
        if (_type == FileType.PACKAGE_DIR && dir && typeLen == 0)
            return null;

        // Create AppFile
        FileTreeFile treeFile = new FileTreeFile(this, aFile);
        if (dir && _proj != null && aFile == _proj.getSourceDir() && !aFile.isRoot()) {
            treeFile._type = FileType.SOURCE_DIR;
            treeFile._priority = 1;
        }
        else if (_type == FileType.SOURCE_DIR && dir && typeLen == 0) {
            treeFile._type = FileType.PACKAGE_DIR;
            treeFile._priority = -1;
        }

        // Set priorities for special files
        if (type.equals("java") || type.equals("snp"))
            treeFile._priority = 1;

        // Return
        return treeFile;
    }

    /**
     * Returns the list of child files.
     */
    public WebFile[] getChildFiles()
    {
        if (_type == FileType.SOURCE_DIR)
            return getSourceDirChildFiles();
        return _file.getFilesArray();
    }

    /**
     * Creates children list from Project files.
     */
    protected WebFile[] getSourceDirChildFiles()
    {
        // Iterate over source dir and add child packages and files
        List<WebFile> children = new ArrayList<>();
        for (WebFile child : getFile().getFiles()) {
            if (child.isDir() && child.getFileType().length() == 0)
                addPackageDirFiles(child, children);
            else children.add(child);
        }

        return children.toArray(new WebFile[0]);
    }

    /**
     * Adds child packages directory files.
     */
    private void addPackageDirFiles(WebFile aDir, List<WebFile> aList)
    {
        boolean hasNonPkgFile = false;
        for (WebFile child : aDir.getFiles())
            if (child.isDir() && child.getFileType().length() == 0)
                addPackageDirFiles(child, aList);
            else hasNonPkgFile = true;
        if (hasNonPkgFile || aDir.getFileCount() == 0) aList.add(aDir);
    }

    /**
     * Returns the text to be used for this AppFile.
     */
    public String getText()
    {
        // Get base name: Class/Package Name or site name or file name
        String base = _type == FileType.PACKAGE_DIR ? _proj.getClassNameForFile(_file) :
                _file.isRoot() ? _file.getSite().getName() : _file.getName();

        // Get Prefix, Suffix
        String prefix = _vc.isFileModified(_file) ? ">" : "";
        String suffix = _file.isUpdateSet() ? " *" : "";

        // Return all parts
        return prefix + base + suffix;
    }

    /**
     * Return the image to be used for this AppFile.
     */
    public View getGraphic()
    {
        // Get image for file
        Image fileImage = _type == FileType.PACKAGE_DIR ? Package : FileIcons.getFileIconImage(_file);
        View fileIconView = new ImageView(fileImage);
        fileIconView.setPrefSize(18, 18);

        // If error/warning add Error/Warning badge as composite icon
        Workspace workspace = _proj.getWorkspace();
        BuildIssues buildIssues = workspace.getBuildIssues();
        BuildIssue.Kind status = buildIssues != null ? buildIssues.getBuildStatusForFile(_file) : null;
        if (status != null) {
            Image badge = status == BuildIssue.Kind.Error ? ErrorBadge : WarningBadge;
            ImageView badgeImageView = new ImageView(badge);
            badgeImageView.setLean(Pos.BOTTOM_LEFT);

            // Create StackView
            StackView stackView = new StackView();
            stackView.setChildren(fileIconView, badgeImageView);
            fileIconView = stackView;
        }

        // Return
        return fileIconView;
    }

    /**
     * Comparable method to order FileItems by Priority then File.
     */
    public int compareTo(FileTreeFile aAF)
    {
        if (_priority != aAF._priority) return _priority > aAF._priority ? -1 : 1;
        return _file.compareTo(aAF._file);
    }

    /**
     * Standard hashCode implementation.
     */
    public int hashCode()
    {
        return _file.hashCode();
    }

    /**
     * Standard equals implementation.
     */
    public boolean equals(Object anObj)
    {
        FileTreeFile other = anObj instanceof FileTreeFile ? (FileTreeFile) anObj : null;
        if (other == null) return false;
        return other._file == _file;
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return "AppFile: " + getText();
    }

    /**
     * A resolver for AppFiles.
     */
    public static class AppFileTreeResolver extends TreeResolver<FileTreeFile> {

        /**
         * Returns the parent of given item.
         */
        public FileTreeFile getParent(FileTreeFile anItem)
        {
            return anItem.getParent();
        }

        /**
         * Whether given object is a parent (has children).
         */
        public boolean isParent(FileTreeFile anItem)
        {
            return anItem.isParent();
        }

        /**
         * Returns the children.
         */
        public List<FileTreeFile> getChildren(FileTreeFile aParent)
        {
            return Arrays.asList(aParent.getChildren());
        }

        /**
         * Returns the text to be used for given item.
         */
        public String getText(FileTreeFile anItem)
        {
            return anItem.getText();
        }

        /**
         * Return the image to be used for given item.
         */
        public View getGraphic(FileTreeFile anItem)
        {
            return anItem.getGraphic();
        }
    }
}