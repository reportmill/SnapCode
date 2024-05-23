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

    // The ".git" dir
    private WebFile _dirFile;

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
        _dirFile = aGitDir;
    }

    /**
     * Returns the git dir file.
     */
    public WebFile getDir()  { return _dirFile; }

    /**
     * Returns the git.
     */
    protected Git getGit()
    {
        Repository repository = getRepo();
        return new Git(repository);
    }

    /**
     * Returns the repo.
     */
    protected Repository getRepo()
    {
        if (_repo != null) return _repo;

        // Get ".git" dir file (just return if null)
        File gitDir = _dirFile.getJavaFile();
        if (!gitDir.exists())
            return null;

        // Create repository and return
        try { _repo = new FileRepositoryBuilder().setGitDir(gitDir).readEnvironment().build(); }
        catch (Exception e) { throw new RuntimeException(e); }
        return _repo;
    }

    /**
     * Returns the head branch.
     */
    public GitBranch getHeadBranch()
    {
        Ref headRef = findRefForName(Constants.HEAD);
        String headBranchName = headRef.getName();
        return getBranchForName(headBranchName);
    }

    /**
     * Returns the named branch.
     */
    public GitBranch getBranchForName(String aName)
    {
        // Get branch ref
        Ref branchRef = findRefForName(aName);
        String branchName = branchRef.getTarget().getName();

        // Get cached branch for name (just return if found)
        GitBranch branch = _branches.get(branchName);
        if (branch != null)
            return branch;

        // Create branch and add to cache
        branch = new GitBranch(this, branchName);
        _branches.put(branchName, branch);

        // Return
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
     * Returns the remote site.
     */
    public WebSite getRemoteSite()
    {
        // Get head branch
        GitBranch headBranch = getHeadBranch();

        // Get remote and last commit (just return if null)
        GitBranch remoteBranch = headBranch.getRemoteBranch();
        GitCommit commit = remoteBranch != null ? remoteBranch.getCommit() : null;
        if (commit == null)
            return null;

        // Return site for commit
        return commit.getSite();
    }

    /**
     * Override to close repository and delete directory.
     */
    public void deleteDir() throws Exception
    {
        if (_repo != null)
            _repo.close();
        _repo = null;
        _dirFile.delete();
        _dirFile.setProp(GitDir.class.getName(), null);
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
        ObjectId remoteOriginMaster = getResolvedObjectId("refs/remotes/origin/master");
        merge.include(remoteOriginMaster);
        MergeResult result = merge.call();
        System.out.println("Merge Result: " + result.getMergeStatus());

        // Reset index
        getIndexSite().resetFiles();
        _index = null;
    }

    /**
     * Returns a JGit ref object for name.
     */
    protected Ref findRefForName(String aName)
    {
        Repository repository = getRepo();
        try { return repository.findRef(aName); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Returns an object id.
     */
    private ObjectId getResolvedObjectId(String revisionStr)
    {
        Repository repository = getRepo();
        try { return repository.resolve(revisionStr); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Returns the RevObject for given commit and path.
     */
    protected RevObject getRevObject(ObjectId anId)
    {
        Repository repository = getRepo();
        RevWalk revWalk = new RevWalk(repository);
        try { return revWalk.parseAny(anId); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * A class to represent a branch.
     */
    public static class GitBranch {

        // The GitDir
        private GitDir _gitDir;

        // The name of the branch
        private String _name;

        /**
         * Constructor.
         */
        public GitBranch(GitDir gitDir, String aName)
        {
            _gitDir = gitDir;
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
            String plainName = _name.replace("refs/heads/", "");
            plainName = plainName.replace("refs/remotes/", "");
            return plainName;
        }

        /**
         * Returns the Commit.
         */
        public GitCommit getCommit()
        {
            ObjectId id = _gitDir.getResolvedObjectId(_name);

            // Get commit and return
            RevCommit commit = (RevCommit) _gitDir.getRevObject(id);
            return new GitCommit(_gitDir, commit);
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
            if (_name.contains("/remotes/"))
                return null;

            String remoteName = "origin";
            String simpleName = getSimpleName();
            String remoteBranchName = remoteName + '/' + simpleName;
            return _gitDir.getBranchForName(remoteBranchName);
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