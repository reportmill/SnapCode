package snapcode.app;
import javakit.project.BuildIssue;
import javakit.ide.JavaTextUtils;
import snap.geom.Pos;
import snap.gfx.Image;
import snapcode.project.ProjectX;
import snapcode.project.VersionControl;
import snap.view.*;
import snap.web.WebFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class to represent a project file in FilesPane.
 */
public class AppFile implements Comparable<AppFile> {

    // The parent file
    AppFile _parent;

    // The WebFile
    WebFile _file;

    // The children
    AppFile[] _children;

    // The file type
    FileType _type = FileType.PLAIN;

    // The project
    ProjectX _proj;

    // The WebFile ProjectFileInfo
    VersionControl _vc;

    // The priority of this item
    int _priority;

    // The file type
    public enum FileType {PLAIN, PACKAGE_DIR, SOURCE_DIR}

    // Icons
    static Image ErrorBadge = Image.get(AppFile.class, "ErrorBadge.png");
    static Image WarningBadge = Image.get(AppFile.class, "WarningBadge.png");
    static Image Package = JavaTextUtils.PackageImage;

    /**
     * Creates a new file.
     */
    public AppFile(AppFile aPar, WebFile aFile)
    {
        _parent = aPar;
        _file = aFile;
        _proj = ProjectX.getProjectForSite(aFile.getSite());
        _vc = VersionControl.get(_file.getSite());
    }

    /**
     * Returns the parent.
     */
    public AppFile getParent()
    {
        return _parent;
    }

    /**
     * Returns the real file.
     */
    public WebFile getFile()
    {
        return _file;
    }

    /**
     * Returns whether file is parent.
     */
    public boolean isParent()
    {
        return _file.isDir();
    }

    /**
     * Returns the children.
     */
    public AppFile[] getChildren()
    {
        if (_children != null) return _children;
        WebFile[] files = getChildFiles();
        List<AppFile> children = new ArrayList<>();
        for (WebFile file : files) {
            AppFile child = createChildAppFile(file);
            if (child != null)
                children.add(child);
        }

        // Sort and return
        Collections.sort(children);
        return _children = children.toArray(new AppFile[0]);
    }

    /**
     * Returns a AppPaneTreeItem for child file.
     */
    protected AppFile createChildAppFile(WebFile aFile)
    {
        // Get basic file info
        String name = aFile.getName(), path = aFile.getPath();
        boolean dir = aFile.isDir();
        String type = aFile.getType();
        int tlen = type.length();

        // Skip hidden files, build dir, child packages
        if (name.startsWith(".")) return null;
        if (dir && _proj != null && aFile == _proj.getBuildDir()) return null;
        if (_type == FileType.PACKAGE_DIR && dir && tlen == 0) return null;  // Skip child packages

        // Create AppFile
        AppFile fitem = new AppFile(this, aFile);
        if (dir && _proj != null && aFile == _proj.getSourceDir() && !aFile.isRoot()) {
            fitem._type = FileType.SOURCE_DIR;
            fitem._priority = 1;
        } else if (_type == FileType.SOURCE_DIR && dir && tlen == 0) {
            fitem._type = FileType.PACKAGE_DIR;
            fitem._priority = -1;
        }

        // Set priorities for special files
        if (type.equals("java")) fitem._priority = 1;
        if (type.equals("snp")) fitem._priority = 1;

        // Otherwise just add FileItem
        return fitem;
    }

    /**
     * Returns the list of child files.
     */
    public WebFile[] getChildFiles()
    {
        if (_type == FileType.SOURCE_DIR)
            return getSourceDirChildFiles();
        return _file.getFiles();
    }

    /**
     * Creates children list from Project files.
     */
    protected WebFile[] getSourceDirChildFiles()
    {
        // Iterate over source dir and add child packages and files
        List<WebFile> children = new ArrayList<>();
        for (WebFile child : getFile().getFiles()) {
            if (child.isDir() && child.getType().length() == 0)
                addPackageDirFiles(child, children);
            else children.add(child);
        }

        return children.toArray(new WebFile[0]);
    }

    /**
     * Adds child packages directory files.
     */
    private void addPackageDirFiles(WebFile aDir, List aList)
    {
        boolean hasNonPkgFile = false;
        for (WebFile child : aDir.getFiles())
            if (child.isDir() && child.getType().length() == 0)
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
        String prefix = _vc.isModified(_file) ? ">" : "";
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
        Image img = _type == FileType.PACKAGE_DIR ? Package : ViewUtils.getFileIconImage(_file);
        View grf = new ImageView(img);
        grf.setPrefSize(18, 18);

        // If error/warning add Error/Warning badge as composite icon
        BuildIssue.Kind status = _proj != null ? _proj.getRootProject().getBuildIssues().getBuildStatusForFile(_file) : null;
        if (status != null) {
            Image badge = status == BuildIssue.Kind.Error ? ErrorBadge : WarningBadge;
            ImageView bview = new ImageView(badge);
            bview.setLean(Pos.BOTTOM_LEFT);
            StackView spane = new StackView();
            spane.setChildren(grf, bview);
            grf = spane;
        }

        // Return node
        return grf;
    }

    /**
     * Comparable method to order FileItems by Priority then File.
     */
    public int compareTo(AppFile aAF)
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
        AppFile other = anObj instanceof AppFile ? (AppFile) anObj : null;
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
    public static class AppFileTreeResolver extends TreeResolver<AppFile> {

        /**
         * Returns the parent of given item.
         */
        public AppFile getParent(AppFile anItem)
        {
            return anItem.getParent();
        }

        /**
         * Whether given object is a parent (has children).
         */
        public boolean isParent(AppFile anItem)
        {
            return anItem.isParent();
        }

        /**
         * Returns the children.
         */
        public AppFile[] getChildren(AppFile aParent)
        {
            return aParent.getChildren();
        }

        /**
         * Returns the text to be used for given item.
         */
        public String getText(AppFile anItem)
        {
            return anItem.getText();
        }

        /**
         * Return the image to be used for given item.
         */
        public View getGraphic(AppFile anItem)
        {
            return anItem.getGraphic();
        }
    }

}