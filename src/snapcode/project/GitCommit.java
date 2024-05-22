package snapcode.project;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * A class to represent a commit file.
 */
public class GitCommit {

    // The GitDir
    protected GitDir _gitDir;

    // The RevObject
    protected RevCommit _rev;

    // The Parent commit
    private GitCommit _par;

    // The Tree
    private GitFile<?> _tree;

    // The TreeSite
    private GitFileSite _site;

    /**
     * Constructor.
     */
    GitCommit(GitDir gitDir, RevCommit anRC)
    {
        super();
        _gitDir = gitDir;
        _rev = anRC;
    }

    /**
     * Returns the commit time.
     */
    public long getCommitTime()
    {
        return _rev.getCommitTime() * 1000L;
    }

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
            r = (RevCommit) _gitDir.getRevObject(r); // They return a rev commit, but it isn't loaded!
        return r != null ? new GitCommit(_gitDir, r) : null;
    }

    /**
     * Returns the tree.
     */
    public GitFile<?> getTree()
    {
        if (_tree != null) return _tree;
        return _tree = new GitFile.GitTree(_gitDir, _rev.getTree(), "/");
    }

    /**
     * Returns the site.
     */
    public GitFileSite getSite()
    {
        if (_site != null) return _site;
        return _site = new GitFileSite(_gitDir, this);
    }
}
