/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import snap.util.ArrayUtils;
import snap.web.WebURL;
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
    public VersionControlGit(WebSite projectSite, WebURL remoteSiteUrl)
    {
        super(projectSite, remoteSiteUrl);
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
    public boolean isCheckedOut()
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
        return gitDir.getRemoteSite();
    }

    /**
     * Override to return the GitDirSite.
     */
    @Override
    public WebSite getCloneSite()
    {
        GitDir gitDir = getGitDir();
        return gitDir.getIndexSite();
    }

    /**
     * Load all remote files into project directory.
     */
    @Override
    public boolean checkout(TaskMonitor taskMonitor) throws Exception
    {
        // Make sure local site exists
        WebSite localSite = getLocalSite();
        if (!localSite.getExists()) {
            WebFile rootDir = localSite.getRootDir();
            rootDir.save();
        }

        // Get SiteDir, CloneDir and RemoteURL
        File siteDir = getLocalSite().getRootDir().getJavaFile();
        File tempDir = new File(siteDir, "git-temp");
        String gitRemoteUrlAddress = getRemoteSiteUrlAddress();
        if (gitRemoteUrlAddress.startsWith("git:"))
            gitRemoteUrlAddress = gitRemoteUrlAddress.replace("git:", "https:") + ".git";

        // Create CloneCommand and configure
        CloneCommand cloneCmd = Git.cloneRepository().setURI(gitRemoteUrlAddress);
        cloneCmd.setDirectory(tempDir);
        cloneCmd.setCredentialsProvider(GitUtils.getCredentialsProvider());

        // Wrap TaskMonitor in ProgressMonitor
        if (taskMonitor == null)
            taskMonitor = new TaskMonitor(System.out);
        cloneCmd.setProgressMonitor(GitUtils.getProgressMonitor(taskMonitor));

        // Run clone and move files to site directory
        try {

            // Call clone
            Git result = cloneCmd.call();
            result.close();

            // Moves files to project dir
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
     * Override to merge.
     */
    @Override
    public boolean updateFiles(List<WebFile> theLocalFiles, TaskMonitor taskMonitor) throws Exception
    {
        GitDir gitDir = getGitDir();
        gitDir.merge(taskMonitor);
        return true;
    }

    /**
     * Override to do commit.
     */
    @Override
    public boolean commitFiles(List<WebFile> theFiles, String aMessage, TaskMonitor taskMonitor) throws Exception
    {
        GitDir gitDir = getGitDir();
        gitDir.commitFiles(theFiles, aMessage);
        gitDir.push(taskMonitor);

        // Clear file status
        theFiles.forEach(this::clearFileStatus);
        return true;
    }

    /**
     * Override to do fetch first.
     */
    @Override
    public List<WebFile> getUpdateFilesForLocalFiles(List<WebFile> localFiles, TaskMonitor taskMonitor)
    {
        // Do git fetch to bring repo up to date
        try {
            GitDir gdir = getGitDir();
            gdir.fetch(new TaskMonitor(System.out));
        }
        catch (Exception e) { throw new RuntimeException(e); }

        // Do normal version
        return super.getUpdateFilesForLocalFiles(localFiles, taskMonitor);
    }

    /**
     * Delete VCS support files from project directory.
     */
    @Override
    public void disconnect(TaskMonitor taskMonitor) throws Exception
    {
        GitDir gitDir = getGitDir();
        gitDir.deleteDir();
    }

    /**
     * Whether Version control supports commit messages.
     */
    @Override
    public boolean supportsCommitMessages()  { return true; }

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