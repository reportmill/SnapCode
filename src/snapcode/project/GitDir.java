package snapcode.project;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import snap.util.ArrayUtils;
import snap.util.FilePathUtils;
import snap.util.TaskMonitor;
import snap.web.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class to perform git operations for a git directory.
 */
public class GitDir {

    // The git dir
    private WebFile _gdir;

    // The repository
    private Repository _repo;

    // A map of branches
    private Map<String, GitBranch> _branches = new HashMap<>();

    // The index file site
    private GitIndex _index;

    // The index file site
    private GitIndexSite _indexSite;

    /**
     * Creates a new Git dir.
     */
    public GitDir(WebFile aGitDir)
    {
        _gdir = aGitDir;
    }

    /**
     * Returns the git dir file.
     */
    public WebFile getDir()  { return _gdir; }

    /**
     * Returns the git.
     */
    protected Git getGit()  { return new Git(_repo); }

    /**
     * Returns the repo.
     */
    protected Repository getRepo()
    {
        if (_repo != null) return _repo;

        File gitDir = getDir().getJavaFile();
        if (!gitDir.exists())
            return null;
        try { _repo = new FileRepositoryBuilder().setGitDir(gitDir).readEnvironment().build(); }
        catch (Exception e) { throw new RuntimeException(e); }
        return _repo;
    }

    /**
     * Returns a git ref for given name.
     */
    public GitRef getRef(String aName)
    {
        Ref ref;
        System.out.println("GitDir.getRef: Used to be getRef() but that is gone now. Don't know if this is okay");
        try { ref = getRepo().exactRef(aName); }
        catch (Exception e) { throw new RuntimeException(e); }
        return ref != null ? new GitRef(ref) : null;
    }

    /**
     * Returns the head branch.
     */
    public GitRef getHead()  { return getRef(Constants.HEAD); }

    /**
     * Returns the named branch.
     */
    public GitBranch getBranch(String aName)
    {
        Ref ref;
        System.out.println("GitDir.getRef: Used to be getRef() but that is gone now. Don't know if this is okay");
        try {
            ref = getRepo().exactRef(aName);
            if (ref == null)
                return null;
        }
        catch (Exception e) { throw new RuntimeException(e); }
        String name = ref.getTarget().getName();
        GitBranch branch = _branches.get(name);
        if (branch == null)
            _branches.put(name, branch = new GitBranch(name));
        return branch;
    }

    /**
     * Returns the index.
     */
    public GitIndex getIndex()
    {
        if (_index != null) return _index;
        return _index = new GitIndex();
    }

    /**
     * Returns the index site.
     */
    public GitIndexSite getIndexSite()
    {
        if (_indexSite != null) return _indexSite;
        return _indexSite = new GitIndexSite();
    }

    /**
     * Returns the default remote.
     */
    public GitRemote getRemote()  { return getRemote("origin"); }

    /**
     * Returns the remote for given name.
     */
    public GitRemote getRemote(String aName)
    {
        return new GitRemote(aName);
    }

    /**
     * Override to close repository and delete directory.
     */
    public void deleteDir() throws Exception
    {
        if (_repo != null)
            _repo.close();
        _repo = null;
        _gdir.delete();
        _gdir.setProp(GitDir.class.getName(), null);
    }

    /**
     * Commits a file.
     */
    public void commitFiles(List<WebFile> theFiles, String aMessage) throws Exception
    {
        // Get repository and git
        Repository repo = getRepo();
        Git git = new Git(repo);

        // Add files
        AddCommand addCmd = null;
        for (WebFile file : theFiles)
            if (file.isFile() && file.getExists()) {
                if (addCmd == null)
                    addCmd = git.add();
                addCmd.addFilepattern(file.getPath().substring(1));
            }
        if (addCmd != null)
            addCmd.call();

        // Remove files
        RmCommand rmCmd = null;
        for (WebFile file : theFiles) {
            if (file.isFile() && !file.getExists()) {
                if (rmCmd == null)
                    rmCmd = git.rm();
                rmCmd.addFilepattern(file.getPath().substring(1));
            }
        }
        if (rmCmd != null)
            rmCmd.call();

        // Commit files
        CommitCommand commitCmd = git.commit();
        commitCmd.setMessage(aMessage);
        commitCmd.setAuthor(new PersonIdent(repo));
        commitCmd.setCommitter(new PersonIdent(repo));
        RevCommit rc = commitCmd.call();
        System.out.println("Commited: " + rc);

        // Reset index
        getIndexSite().resetFiles();
        _index = null;
    }

    /**
     * Pushes current committed files.
     */
    public void push(TaskMonitor aTM) throws Exception
    {
        // Get repository and git
        Git git = getGit();

        // Get push
        PushCommand pushCmd = git.push();
        pushCmd.setProgressMonitor(getProgressMonitor(aTM));
        CredentialsProvider credentialsProvider = getCD();
        pushCmd.setCredentialsProvider(credentialsProvider);
        for (PushResult pr : pushCmd.call())
            System.out.println("Pushed: " + pr);
    }

    /**
     * Fetches updates to repo.
     */
    public void fetch(TaskMonitor aTM) throws Exception
    {
        // Do fetch
        Git git = getGit();
        FetchCommand fetch = git.fetch();
        CredentialsProvider credentialsProvider = getCD();
        fetch.setCredentialsProvider(credentialsProvider);
        if (aTM != null)
            fetch.setProgressMonitor(getProgressMonitor(aTM));
        fetch.call();

        // Refresh files
        //getRootDir().refresh();
    }

    /**
     * Merges updates to working dir and commits.
     */
    public void merge() throws Exception
    {
        Git git = getGit();
        MergeCommand merge = git.merge();
        ObjectId remoteOriginMaster = getRepo().resolve("refs/remotes/origin/master");
        merge.include(remoteOriginMaster);
        MergeResult result = merge.call();
        System.out.println("Merge Result: " + result.getMergeStatus());

        // Reset index
        getIndexSite().resetFiles();
        _index = null;
    }

    /**
     * Returns the RevObject for given commit and path.
     */
    private RevObject getRevObject(ObjectId anId)
    {
        RevWalk rwalk = new RevWalk(getRepo());
        try { return rwalk.parseAny(anId); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Returns credentials provider.
     */
    private CredentialsProvider getCD()
    {
        //WebSite rsite = getRemoteSite();
        //ClientUtils.setAccess(this); if(rsite.getUserName()==null) return null;
        //return new UsernamePasswordCredentialsProvider(rsite.getUserName(), rsite.getPassword());
        return new UsernamePasswordCredentialsProvider("reportmill", "rmgithub1");
    }

    /**
     * A class to represent a reference.
     */
    public class GitRef {

        // The Ref
        private Ref _ref;

        /** Constructor. */
        GitRef(Ref aRef)
        {
            _ref = aRef;
        }

        /**
         * Returns the name.
         */
        public String getName()  { return _ref.getName(); }

        /**
         * Returns the branch for ref.
         */
        public GitBranch getBranch()
        {
            return GitDir.this.getBranch(getName());
        }

        /**
         * Standard toString implementation.
         */
        public String toString()
        {
            return _ref.toString();
        }
    }

    /**
     * A class to represent a remote.
     */
    public class GitRemote {

        // The name
        private String _name;

        /**
         * Constructor.
         */
        GitRemote(String aName)
        {
            _name = aName;
        }

        /**
         * Returns the name.
         */
        public String getName()  { return _name; }

        /**
         * Returns a branch for name.
         */
        public GitBranch getBranch(String aName)
        {
            return GitDir.this.getBranch(_name + '/' + aName);
        }
    }

    /**
     * A class to represent a branch.
     */
    public class GitBranch {

        // The name of the branch
        private String _name;

        /**
         * Constructor.
         */
        public GitBranch(String aName)
        {
            _name = aName;
        }

        /**
         * Returns the full branch name.
         */
        public String getName()  { return _name; }

        /**
         * Returns the simple branch name.
         */
        public String getSimpleName()
        {
            return FilePathUtils.getFilename(_name);
        }

        /**
         * Returns the plain branch name (no refs/heads or ref/remotes prefix).
         */
        public String getPlainName()
        {
            return _name.replace("refs/heads/", "").replace("refs/remotes/", "");
        }

        /**
         * Returns the Commit.
         */
        public GitCommit getCommit()
        {
            ObjectId id;
            try { id = getRepo().resolve(_name); }
            catch (Exception e) { throw new RuntimeException(e); }
            RevCommit rc = (RevCommit) getRevObject(id);
            return new GitCommit(rc);
        }

        /**
         * Returns the list of all commits for branch.
         */
        public GitCommit[] getCommits()
        {
            List<GitCommit> commitsList = new ArrayList<>();
            for (GitCommit commit = getCommit(); commit != null; commit = commit.getParent())
                commitsList.add(commit);
            return commitsList.toArray(new GitCommit[0]);
        }

        /**
         * Returns the remote tracking branch.
         */
        public GitBranch getRemoteBranch()
        {
            if (getName().contains("/remotes/"))
                return null;
            return getRemote().getBranch(getSimpleName());
        }
    }

    /**
     * A class to represent a file.
     */
    public static class GitFile<T extends RevObject> {

        // The RevObject
        protected T _rev;

        // The path
        protected String _path;

        /**
         * Constructor.
         */
        public GitFile()
        {
            super();
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
         * Standard toString implementation.
         */
        public String toString()
        {
            return getClass().getSimpleName() + ": " + _path + ", " + _rev;
        }
    }

    /**
     * A class to represent a commit file.
     */
    public class GitCommit extends GitFile<RevCommit> {

        // The Parent commit
        private GitCommit _par;

        // The Tree
        private GitTree _tree;

        // The TreeSite
        private GitFileSite _site;

        /**
         * Constructor.
         */
        GitCommit(RevCommit anRC)
        {
            _rev = anRC;
        }

        /**
         * Returns the commit time.
         */
        public long getCommitTime()  { return _rev.getCommitTime() * 1000L; }

        /**
         * Returns the parent commit.
         */
        public GitCommit getParent()
        {
            if (_par != null) return _par;
            return _par = getParentImpl();
        }

        /**
         * Returns the parent commit.
         */
        private GitCommit getParentImpl()
        {
            RevCommit r = _rev.getParentCount() > 0 ? _rev.getParent(0) : null;
            if (r != null)
                r = (RevCommit) getRevObject(r); // They return a rev commit, but it isn't loaded!
            return r != null ? new GitCommit(r) : null;
        }

        /**
         * Returns the tree.
         */
        public GitTree getTree()
        {
            if (_tree != null) return _tree;
            return _tree = new GitTree(_rev.getTree(), "/");
        }

        /**
         * Returns the site.
         */
        public GitFileSite getSite()
        {
            if (_site != null) return _site;
            return _site = new GitFileSite(this);
        }
    }

    /**
     * A class to represent a tree file.
     */
    public class GitTree extends GitFile<RevTree> {

        // The child files
        private GitFile<?>[] _files;

        /**
         * Constructor.
         */
        GitTree(RevTree aRT, String aPath)
        {
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
        protected GitFile<?>[] getFilesImpl() throws Exception
        {
            TreeWalk treeWalk = new TreeWalk(getRepo());
            treeWalk.addTree(_rev);

            // Get files for tree walk
            List<GitFile<?>> files = new ArrayList<>();
            while (treeWalk.next()) {
                ObjectId id = treeWalk.getObjectId(0);
                RevObject rid = getRevObject(id);
                String path = _path + (_path.length() > 1 ? "/" : "") + treeWalk.getNameString();
                GitFile<?> child = rid instanceof RevTree ? new GitTree((RevTree) rid, path) : new GitBlob((RevBlob) rid, path);
                files.add(child);
            }

            // Return files array
            return files.toArray(new GitFile[0]);
        }

        /**
         * Returns a file for a given path.
         */
        public GitFile<?> getFile(String aPath)
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
    public class GitBlob extends GitFile<RevBlob> {

        /**
         * Constructor.
         */
        GitBlob(RevBlob aRB, String aPath)
        {
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
                ObjectReader objectReader = getRepo().newObjectReader();
                return objectReader.open(_rev).getBytes();
            }
            catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    /**
     * A class to represent a GitIndex.
     */
    public class GitIndex {

        // The repository index
        private DirCache _index;

        /**
         * Returns the index.
         */
        public DirCache getIndex()
        {
            if (_index != null) return _index;
            try { _index = getRepo().readDirCache(); }
            catch (Exception e) { throw new RuntimeException(e); }
            return _index;
        }

        /**
         * Returns an index entry for given path.
         */
        public Entry getEntry(String aPath)
        {
            if (getRepo() == null)
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
        public class Entry {

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
                    ObjectReader objectReader = getRepo().newObjectReader();
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

    /**
     * A WebSite implementation for a GitCommit.
     */
    public class GitFileSite extends WebSite {

        // The GitCommit
        private GitCommit _commit;

        /**
         * Constructor.
         */
        public GitFileSite(GitCommit aGC)
        {
            super();
            _commit = aGC;

            // Get URL for site and set
            String gitDirPath = getDir().getURL().getString();
            String sitePath = gitDirPath + "!/" + _commit._rev.getId().getName();
            WebURL siteUrl = WebURL.getURL(sitePath);
            setURL(siteUrl);
        }

        /**
         * Returns the tree for this site.
         */
        public GitTree getTree()  { return _commit.getTree(); }

        /**
         * Handles a get or head request.
         */
        protected void doGetOrHead(WebRequest aReq, WebResponse aResp, boolean isHead)
        {
            // Get URL and path and create empty response
            WebURL fileUrl = aReq.getURL();
            String filePath = fileUrl.getPath();
            if (filePath == null)
                filePath = "/";

            // Get Head branch Commit Tree and look for file - f not found, set Response.Code to NOT_FOUND and return
            GitFile<?> gitFile = getTree().getFile(filePath);
            if (gitFile == null) {
                aResp.setCode(WebResponse.NOT_FOUND);
                return;
            }

            // Otherwise configure
            aResp.setCode(WebResponse.OK);
            aResp.setDir(gitFile.isDir());
            aResp.setLastModTime(_commit.getCommitTime());

            // If Head, just return
            if (isHead)
                return;

            // Handle plain file
            if (aResp.isFile()) {
                byte[] bytes = gitFile.getBytes();
                aResp.setBytes(bytes);
            }

            // Handle directory: Walk RevTree and get files for children
            else {
                GitDir.GitFile<?>[] dirFiles = gitFile.getFiles();
                FileHeader[] dirFileHeaders = ArrayUtils.map(dirFiles, file -> getFileHeader(file.getPath()), FileHeader.class);
                aResp.setFileHeaders(dirFileHeaders);
            }
        }

        /**
         * Get file from directory.
         */
        protected FileHeader getFileHeader(String aPath)
        {
            // Get Head branch Commit Tree and look for file
            GitFile<?> gitFile = getTree().getFile(aPath);
            if (gitFile == null)
                return null;

            // Create file for path and commit time
            FileHeader fileHeader = new FileHeader(aPath, gitFile.isDir());
            fileHeader.setLastModTime(_commit.getCommitTime());
            return fileHeader;
        }
    }

    /**
     * A WebSite implementation for GitDirIndex.
     */
    protected class GitIndexSite extends WebSite {

        /**
         * Constructor.
         */
        public GitIndexSite()
        {
            super();

            // Get/set site URL
            String gitDirPath = getDir().getURL().getString();
            String sitePath = gitDirPath + ".index";
            WebURL siteUrl = WebURL.getURL(sitePath);
            setURL(siteUrl);
        }

        /**
         * Handles a get or head request.
         */
        protected void doGetOrHead(WebRequest aReq, WebResponse aResp, boolean isHead)
        {
            // Get URL and path and create empty response
            WebURL fileUrl = aReq.getURL();
            String filePath = fileUrl.getPath();
            if (filePath == null)
                filePath = "/";

            // Get entry - if not found, set Response.Code to NOT_FOUND and return
            GitIndex.Entry entry = getIndex().getEntry(filePath);
            if (entry == null) {
                aResp.setCode(WebResponse.NOT_FOUND);
                return;
            }

            // Otherwise configure
            aResp.setCode(WebResponse.OK);
            aResp.setDir(entry.isDir());
            aResp.setLastModTime(entry.getLastModified());
            aResp.setSize(entry.getLength());

            // If Head, just return
            if (isHead)
                return;

            // Handle plain file
            if (aResp.isFile()) {
                byte[] bytes = entry.getBytes();
                aResp.setBytes(bytes);
            }

            // Handle directory: Walk RevTree and get files for children
            else {
                GitDir.GitIndex.Entry[] gitIndexEntries = entry.getEntries();
                FileHeader[] fileHeaders = ArrayUtils.map(gitIndexEntries, indexEntry -> getFileHeader(indexEntry.getPath()), FileHeader.class);
                aResp.setFileHeaders(fileHeaders);
            }
        }

        /**
         * Get file from directory.
         */
        protected FileHeader getFileHeader(String aPath)
        {
            GitIndex.Entry entry = getIndex().getEntry(aPath);
            if (entry == null)
                return null;

            FileHeader fileHeader = new FileHeader(aPath, entry.isDir());
            fileHeader.setLastModTime(entry.getLastModified());
            fileHeader.setSize(entry.getLength());
            return fileHeader;
        }
    }

    /**
     * Returns a ProgressMonitor for given TaskMonitor.
     */
    public static ProgressMonitor getProgressMonitor(final TaskMonitor aTM)
    {
        return new ProgressMonitor() {
            public void update(int arg0)  { aTM.updateTask(arg0); }
            public void start(int arg0)  { aTM.startTasks(arg0); }
            public boolean isCancelled()  { return aTM.isCancelled(); }
            public void endTask()  { aTM.endTask(); }
            public void beginTask(String arg0, int arg1)  { aTM.beginTask(arg0, arg1); }
        };
    }

    /**
     * Returns a GitDir for a git directory file.
     */
    public synchronized static GitDir get(WebFile aFile)
    {
        GitDir gitDir = (GitDir) aFile.getProp(GitDir.class.getName());
        if (gitDir == null)
            aFile.setProp(GitDir.class.getName(), gitDir = new GitDir(aFile));
        return gitDir;
    }
}