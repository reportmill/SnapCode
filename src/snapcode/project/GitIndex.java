package snapcode.project;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.ObjectReader;
import snap.util.FilePathUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to represent a GitIndex.
 */
public class GitIndex {

    // The GitDir
    private GitDir _gitDir;

    // The repository index
    private DirCache _index;

    /**
     * Constructor.
     */
    public GitIndex(GitDir gitDir)
    {
        _gitDir = gitDir;
    }

    /**
     * Returns the index.
     */
    public DirCache getIndex()
    {
        if (_index != null) return _index;
        try { _index = _gitDir.getRepo().readDirCache(); }
        catch (Exception e) { throw new RuntimeException(e); }
        return _index;
    }

    /**
     * Returns an index entry for given path.
     */
    public Entry getEntry(String aPath)
    {
        if (_gitDir.getRepo() == null)
            return null;

        // Handle root special
        if (aPath.equals("/"))
            return new Entry(null, aPath);

        // Get repository index and entry for path
        DirCache index = getIndex();
        String path = aPath.substring(1);
        DirCacheEntry entry = index.getEntry(aPath.substring(1));
        boolean isDir = entry == null && index.getEntriesWithin(path).length > 0;
        if (entry == null && !isDir)
            return null;

        // Create file for path and index entry
        return new Entry(entry, aPath);
    }

    /**
     * Returns child entries.
     */
    protected Entry[] getEntries(Entry anEntry)
    {
        // Get repository index and entry for path
        DirCache index = getIndex();
        String path = anEntry.getPath().substring(1);
        DirCacheEntry[] entries = index.getEntriesWithin(path);

        // Iterate over entries
        List<Entry> entriesList = new ArrayList<>();
        String lastPath = "";
        for (DirCacheEntry entry : entries) {
            String entryPath = entry.getPathString();
            int ind = entryPath.indexOf('/', path.length() + 1);
            if (ind > 0)
                entryPath = entryPath.substring(0, ind);
            if (entryPath.equals(lastPath))
                continue;
            lastPath = entryPath;
            Entry child = new Entry(ind < 0 ? entry : null, '/' + entryPath);
            entriesList.add(child);
        }

        // Return list
        return entriesList.toArray(new Entry[0]);
    }

    /**
     * A class to represent a GitIndex entry.
     */
    protected class Entry {

        // The DirCacheEntry
        DirCacheEntry _entry;
        String _path;

        /**
         * Creates a new GitIndex Entry for given DirCacheEntry.
         */
        public Entry(DirCacheEntry aDCE, String aPath)
        {
            _entry = aDCE;
            _path = aPath;
        }

        /**
         * Returns the path.
         */
        public String getPath()  { return _path; }

        /**
         * Returns the name.
         */
        public String getName()  { return FilePathUtils.getFilename(_path); }

        /**
         * Returns whether entry is directory.
         */
        public boolean isDir()  { return _entry == null; }

        /**
         * Returns the file length.
         */
        public int getLength()  { return _entry != null ? _entry.getLength() : 0; }

        /**
         * Returns the last modified time.
         */
        public long getLastModified()  { return _entry != null ? _entry.getLastModified() : 0; }

        /**
         * Returns the bytes for entry.
         */
        public byte[] getBytes()
        {
            try {
                ObjectReader objectReader = _gitDir.getRepo().newObjectReader();
                return objectReader.open(_entry.getObjectId()).getBytes();
            }
            catch (Exception e) { throw new RuntimeException(e); }
        }

        /**
         * Returns a list of entries.
         */
        public Entry[] getEntries()
        {
            return GitIndex.this.getEntries(this);
        }

        /**
         * Standard toString implementation.
         */
        public String toString()
        {
            return "Entry: " + _path + ", " + _entry;
        }
    }
}
