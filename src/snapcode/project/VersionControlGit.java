/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import snap.util.ArrayUtils;
import snapcode.project.GitDir.GitBranch;
import snapcode.project.GitDir.GitCommit;
import snap.util.FileUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import snap.web.WebSite;
import java.io.File;
import java.util.List;

/**
 * A VersionControl implementation for Git.
 */
public class VersionControlGit extends VersionControl {

    // The GitDir
    private GitDir _gitDir;

    // The WebFile for actual .git dir
    private WebFile _gitDirFile;

    /**
     * Constructor.
     */
    public VersionControlGit(WebSite projectSite)
    {
        super(projectSite, null);
    }

    /**
     * Returns the git dir.
     */
    public GitDir getGitDir()
    {
        if (_gitDir != null) return _gitDir;
        WebFile girDirFile = getGitDirFile();
        return _gitDir = GitDir.getGitDirForFile(girDirFile);
    }

    /**
     * Returns the git dir file.
     */
    public WebFile getGitDirFile()
    {
        if (_gitDirFile != null) return _gitDirFile;
        WebSite localSite = getLocalSite();
        return _gitDirFile = localSite.createFileForPath("/.git", true);
    }

    /**
     * Returns whether existing VCS artifacts are detected for project.
     */
    @Override
    public boolean isAvailable()
    {
        WebFile gitDirFile = getGitDirFile();
        return gitDirFile.getExists();
    }

    /**
     * Override to return the GitDirSite.
     */
    @Override
    public WebSite getRemoteSite()
    {
        GitDir gitDir = getGitDir();
        GitBranch remoteBranch = gitDir.getHead().getBranch().getRemoteBranch();
        GitCommit commit = remoteBranch != null ? remoteBranch.getCommit() : null;
        return commit != null ? commit.getSite() : null;
    }

    /**
     * Override to return the GitDirSite.
     */
    //@Override
    public WebSite getCloneSite()
    {
        GitDir gitDir = getGitDir();
        return gitDir.getIndexSite();
    }

    @Override
    protected boolean checkoutImpl(TaskMonitor aTM)
    {
        try { return checkoutImpl2(aTM); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Load all remote files into project directory.
     */
    protected boolean checkoutImpl2(TaskMonitor aTM) throws Exception
    {
        // Get SiteDir, CloneDir and RemoteURL
        File siteDir = getLocalSite().getRootDir().getJavaFile();
        File tempDir = new File(siteDir, "git-temp");
        String gitRemoteUrlAddress = getRemoteSiteUrlAddress();
        if (gitRemoteUrlAddress.startsWith("git:"))
            gitRemoteUrlAddress = gitRemoteUrlAddress.replace("git:", "https:") + ".git";

        // Create CloneCommand and configure
        CloneCommand cloneCmd = Git.cloneRepository();
        CredentialsProvider credentialsProvider = GitUtils.getCredentialsProvider();
        cloneCmd.setURI(gitRemoteUrlAddress).setDirectory(tempDir).setCredentialsProvider(credentialsProvider);

        // Wrap TaskMonitor in ProgressMonitor
        if (aTM != null)
            cloneCmd.setProgressMonitor(GitUtils.getProgressMonitor(aTM));

        // Run clone and move files to site directory
        try {
            cloneCmd.call().close();
            File[] tempDirFiles = tempDir.listFiles();
            if (tempDirFiles != null) {
                for (File tempDirFile : tempDirFiles) {
                    File projFile = new File(siteDir, tempDirFile.getName());
                    FileUtils.move(tempDirFile, projFile);
                }
            }
        }

        // Delete temp directory (should be empty)
        finally {
            FileUtils.deleteDeep(tempDir);
        }

        // Return success
        return true;
    }

    /**
     * Override to do commit.
     */
    @Override
    protected boolean commitFilesImpl(List<WebFile> theFiles, String aMessage, TaskMonitor aTM)
    {
        try {
            GitDir gitDir = getGitDir();
            gitDir.commitFiles(theFiles, aMessage);
            gitDir.push(aTM);

            // Clear file status
            theFiles.forEach(this::clearFileStatus);
            return true;
        }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Override to do fetch first.
     */
    @Override
    public List<WebFile> getUpdateFilesForRootFiles(List<WebFile> theFiles)
    {
        // Do git fetch to bring repo up to date
        try {
            GitDir gdir = getGitDir();
            gdir.fetch(new TaskMonitor(System.out));
        }
        catch (Exception e) { throw new RuntimeException(e); }

        // Do normal version
        return super.getUpdateFilesForRootFiles(theFiles);
    }

    /**
     * Override to merge.
     */
    @Override
    protected boolean updateFilesImpl(List<WebFile> theLocalFiles, TaskMonitor aTM)
    {
        try {
            GitDir gitDir = getGitDir();
            gitDir.merge();
            return true;
        }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Override for bogus implementation that copies clone back to local file.
     */
    @Override
    protected void replaceFile(WebFile aLocalFile)
    {
        try {
            // Get CloneFile
            WebSite cloneSite = getCloneSite();
            WebFile cloneFile = cloneSite.getFileForPath(aLocalFile.getPath());

            // Set new file bytes and save
            if (cloneFile.getExists()) { //_project.removeBuildFile(aLocalFile);
                if (aLocalFile.isFile())
                    aLocalFile.setBytes(cloneFile.getBytes());
                aLocalFile.save();
                aLocalFile.saveLastModTime(cloneFile.getLastModTime());
            }

            // Otherwise delete LocalFile and CloneFile
            else if (aLocalFile.getExists())
                aLocalFile.delete();

            // Clear file status
            clearFileStatus(aLocalFile);
        }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Delete VCS support files from project directory.
     */
    @Override
    public void disconnect(TaskMonitor aTM) throws Exception
    {
        GitDir gitDir = getGitDir();
        gitDir.deleteDir();
    }

    /**
     * Returns whether (local) file should be ignored.
     */
    @Override
    protected boolean isIgnoreFile(WebFile aFile)
    {
        if (super.isIgnoreFile(aFile))
            return true;
        if (aFile.getName().equals(".git"))
            return true;
        String[] gitIgnores = getGitIgnoreStrings();
        return ArrayUtils.hasMatch(gitIgnores, gi -> isFileMatchForPattern(aFile, gi));
    }

    /**
     * Returns the git ignore strings.
     */
    protected String[] getGitIgnoreStrings()
    {
        WebFile gitIgnoreFile = getLocalSite().getFileForPath(".gitignore");
        if (gitIgnoreFile == null)
            return new String[0];
        String gitIgnoreText = gitIgnoreFile.getText();
        return gitIgnoreText.split("\\s+");
    }

    /**
     * Returns whether a file matches a given gitignore pattern.
     */
    private static boolean isFileMatchForPattern(WebFile aFile, String anIgnorePattern)
    {
        String ignorePattern = anIgnorePattern;
        if (ignorePattern.endsWith("/"))
            ignorePattern = ignorePattern.substring(0, ignorePattern.length() - 1);
        String filePath = aFile.getPath();
        String filename = aFile.getName();
        return filePath.equals(ignorePattern) || filename.equals(ignorePattern);
    }
}