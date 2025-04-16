package snapcode.apptools;
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
import java.util.List;
import java.util.Objects;

/**
 * A class to represent a project file in ProjectFilesTool.
 */
public class ProjectFile implements Comparable<ProjectFile> {

    // The file system
    protected ProjectFileSystem _fileSystem;

    // The parent file
    protected ProjectFile _parent;

    // The WebFile
    protected WebFile _file;

    // Whether file is directory
    protected boolean _isDir;

    // The child files
    protected List<ProjectFile> _childFiles;

    // The text
    protected String _text;

    // The file type
    protected FileType _type = FileType.PLAIN;

    // The project
    protected Project _proj;

    // The version control
    protected VersionControl _vc;

    // The priority of this item
    protected int _priority;

    // The file type
    public enum FileType { PLAIN, PACKAGE_DIR, SOURCE_DIR }

    // Icons
    private static Image ErrorBadge = Image.getImageForClassResource(ProjectFile.class, "ErrorBadge.png");
    private static Image WarningBadge = Image.getImageForClassResource(ProjectFile.class, "WarningBadge.png");
    private static Image Package = JavaTextUtils.PackageImage;

    /**
     * Constructor.
     */
    public ProjectFile(ProjectFile aPar, WebFile aFile)
    {
        _fileSystem = aPar != null ? aPar._fileSystem : ProjectFileSystem.getDefaultProjectFileSystem();
        _parent = aPar;
        _file = aFile;
        if (_file != null) {
            _isDir = _file.isDir();
            _proj = Project.getProjectForSite(aFile.getSite());
            _vc = VersionControl.getVersionControlForProjectSite(_file.getSite());
        }
    }

    /**
     * Returns the parent.
     */
    public ProjectFile getParent()  { return _parent; }

    /**
     * Returns the real file.
     */
    public WebFile getFile()  { return _file; }

    /**
     * Returns whether file is directory.
     */
    public boolean isDir()  { return _isDir; }

    /**
     * Returns the child files.
     */
    public List<ProjectFile> getFiles()
    {
        if (_childFiles != null) return _childFiles;
        return _childFiles = _fileSystem.getChildFilesForFile(this);
    }

    /**
     * Returns the text to be used for this project file.
     */
    public String getText()
    {
        if (_text != null) return _text;

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
     * Return the image to be used for this project file.
     */
    public View getGraphic()
    {
        // If no file, return directory icon
        if (_file == null) {
            Image dirImage = FileIcons.getDirImage();
            View fileIconView = new ImageView(dirImage);
            fileIconView.setPrefSize(18, 18);
            return fileIconView;
        }

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
    public int compareTo(ProjectFile otherFile)
    {
        if (_priority != otherFile._priority) return _priority > otherFile._priority ? -1 : 1;
        return _file.compareTo(otherFile._file);
    }

    /**
     * Standard hashCode implementation.
     */
    public int hashCode()  { return Objects.hash(_file, _text); }

    /**
     * Standard equals implementation.
     */
    public boolean equals(Object anObj)
    {
        ProjectFile other = anObj instanceof ProjectFile ? (ProjectFile) anObj : null;
        if (other == null) return false;
        return other._file == _file;
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return "ProjectFile: " + getText();
    }

    /**
     * A resolver for ProjectFiles.
     */
    public static class ProjectFileTreeResolver extends TreeResolver<ProjectFile> {

        /** Returns the parent of given item. */
        public ProjectFile getParent(ProjectFile anItem)  { return anItem.getParent(); }

        /** Whether given object is a parent (has children). */
        public boolean isParent(ProjectFile anItem)  { return anItem.isDir(); }

        /** Returns the children. */
        public List<ProjectFile> getChildren(ProjectFile aParent)  { return aParent.getFiles(); }

        /** Returns the text to be used for given item. */
        public String getText(ProjectFile anItem)  { return anItem.getText(); }

        /** Return the image to be used for given item. */
        public View getGraphic(ProjectFile anItem)  { return anItem.getGraphic(); }
    }
}