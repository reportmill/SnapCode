package snapcode.project;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
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
        return _index = new GitIndex(this);
    }

    /**
     * Returns the index site.
     */
    public GitIndexSite getIndexSite()
    {
        if (_indexSite != null) return _indexSite;
        return _indexSite = new GitIndexSite(this);
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
        pushCmd.setProgressMonitor(GitUtils.getProgressMonitor(aTM));
        CredentialsProvider credentialsProvider = GitUtils.getCredentialsProvider();
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
        CredentialsProvider credentialsProvider = GitUtils.getCredentialsProvider();
        fetch.setCredentialsProvider(credentialsProvider);
        if (aTM != null)
            fetch.setProgressMonitor(GitUtils.getProgressMonitor(aTM));
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
    protected RevObject getRevObject(ObjectId anId)
    {
        RevWalk rwalk = new RevWalk(getRepo());
        try { return rwalk.parseAny(anId); }
        catch (Exception e) { throw new RuntimeException(e); }
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
        public String getSimpleName()  { return FilePathUtils.getFilename(_name); }

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
            return new GitCommit(GitDir.this, rc);
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
     * Returns a GitDir for a git directory file.
     */
    public synchronized static GitDir getGitDirForFile(WebFile aFile)
    {
        GitDir gitDir = (GitDir) aFile.getProp(GitDir.class.getName());
        if (gitDir == null)
            aFile.setProp(GitDir.class.getName(), gitDir = new GitDir(aFile));
        return gitDir;
    }
}