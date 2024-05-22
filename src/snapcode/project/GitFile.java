package snapcode.project;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevBlob;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import snap.util.ArrayUtils;
import snap.util.FilePathUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to represent a file.
 */
public class GitFile<T extends RevObject> {

    // The GitDir
    protected GitDir _gitDir;

    // The RevObject
    protected T _rev;

    // The path
    protected String _path;

    /**
     * Constructor.
     */
    public GitFile(GitDir gitDir)
    {
        super();
        _gitDir = gitDir;
    }

    /**
     * Returns the path.
     */
    public String getPath()  { return _path; }

    /**
     * Returns the resource name.
     */
    public String getName()  { return FilePathUtils.getFilename(_path); }

    /**
     * Returns whether file is directory.
     */
    public boolean isDir()  { return false; }

    /**
     * Returns whether file is file (blob).
     */
    public boolean isFile()  { return false; }

    /**
     * Returns the list of child files.
     */
    public GitFile<?>[] getFiles()  { return null; }

    /**
     * Returns the bytes.
     */
    public byte[] getBytes()  { return null; }

    /**
     * Returns a file for a given path.
     */
    public GitFile<?> getFileForPath(String aPath)
    {
        throw new RuntimeException("GitFile: Not a tree file: " + _path);
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return getClass().getSimpleName() + ": " + _path + ", " + _rev;
    }

    /**
     * A class to represent a tree file.
     */
    public static class GitTree extends GitFile<RevTree> {

        // The child files
        private GitFile<?>[] _files;

        /**
         * Constructor.
         */
        GitTree(GitDir gitDir, RevTree aRT, String aPath)
        {
            super(gitDir);
            _rev = aRT;
            _path = aPath;
        }

        /**
         * Returns whether file is directory.
         */
        @Override
        public boolean isDir()  { return true; }

        /**
         * Returns the list of child files.
         */
        @Override
        public GitFile<?>[] getFiles()
        {
            if (_files != null) return _files;
            try { _files = getFilesImpl(); }
            catch (Exception e) { throw new RuntimeException(e); }
            return _files;
        }

        /**
         * Returns the list of child files.
         */
        private GitFile<?>[] getFilesImpl() throws Exception
        {
            Repository repo = _gitDir.getRepo();
            TreeWalk treeWalk = new TreeWalk(repo);
            treeWalk.addTree(_rev);

            // Get files for tree walk
            List<GitFile<?>> files = new ArrayList<>();
            while (treeWalk.next()) {
                ObjectId id = treeWalk.getObjectId(0);
                RevObject rid = _gitDir.getRevObject(id);
                String path = _path + (_path.length() > 1 ? "/" : "") + treeWalk.getNameString();
                GitFile<?> child = rid instanceof RevTree ? new GitTree(_gitDir, (RevTree) rid, path) : new GitBlob(_gitDir, (RevBlob) rid, path);
                files.add(child);
            }

            // Return files array
            return files.toArray(new GitFile[0]);
        }

        /**
         * Returns a file for a given path.
         */
        @Override
        public GitFile<?> getFileForPath(String aPath)
        {
            if (aPath.equals("/"))
                return this;

            String[] pathParts = aPath.split("/");
            GitFile<?> file = this;
            for (int i = 0; i < pathParts.length && file != null; i++) {
                String pathPart = pathParts[i];
                if (pathPart.length() == 0)
                    continue;
                GitFile<?>[] childFiles = file.getFiles();
                file = ArrayUtils.findMatch(childFiles, f -> pathPart.equals(f.getName()));
            }

            return file;
        }
    }

    /**
     * A class to represent a blob.
     */
    public static class GitBlob extends GitFile<RevBlob> {

        /**
         * Constructor.
         */
        GitBlob(GitDir gitDir, RevBlob aRB, String aPath)
        {
            super(gitDir);
            _rev = aRB;
            _path = aPath;
        }

        /**
         * Returns whether file is file (blob).
         */
        public boolean isFile()  { return true; }

        /**
         * Returns the bytes.
         */
        public byte[] getBytes()
        {
            try {
                Repository repo = _gitDir.getRepo();
                ObjectReader objectReader = repo.newObjectReader();
                return objectReader.open(_rev).getBytes();
            }
            catch (Exception e) { throw new RuntimeException(e); }
        }
    }
}
