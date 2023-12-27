/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.props.PropChangeSupport;
import snap.util.ArrayUtils;
import snap.util.TaskRunner;
import snap.view.View;
import snap.viewx.TaskMonitorPanel;
import snapcode.webbrowser.ClientUtils;
import snap.util.TaskMonitor;
import snap.web.AccessException;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.webbrowser.LoginPage;
import java.util.*;

/**
 * This is a class to handle file synchronization for a local WebSite with a remote WebSite.
 */
public class VersionControl {

    // The local site
    private WebSite _localSite;

    // The remote URL
    private WebURL _remoteURL;

    // A map of file to it's status
    private Map<WebFile, FileStatus> _filesStatusCache = new HashMap<>();

    // The PropChangeSupport
    private PropChangeSupport _pcs = PropChangeSupport.EMPTY;

    // Constants for Synchronization operations
    public enum Op { Update, Replace, Commit }

    // Constants for the state of files relative to remote cache
    public enum FileStatus { Added, Removed, Modified, Identical }

    // Constants for properties
    public static final String FileStatus_Prop = "FileStatus";

    /**
     * Creates a VersionControl for a given site.
     */
    public VersionControl(WebSite aSite)
    {
        _localSite = aSite;
        String urls = getRemoteURLString(aSite);
        if (urls != null)
            _remoteURL = WebURL.getURL(urls);
    }

    /**
     * Returns the Remote URL.
     */
    public WebURL getRemoteURL()  { return _remoteURL; }

    /**
     * Returns the Remote URL string.
     */
    public String getRemoteURLString()  { return _remoteURL != null ? _remoteURL.getString() : null; }

    /**
     * Returns the local cache directory of remote site.
     */
    protected WebFile getCloneDir()
    {
        WebSite localSite = getLocalSite();
        WebSite sandboxSite = localSite.getSandbox();
        WebFile cloneDir = sandboxSite.getFileForPath("Remote.clone");
        if (cloneDir == null) {
            cloneDir = sandboxSite.createFileForPath("Remote.clone", true);
            cloneDir.save();
        }
        return cloneDir;
    }

    /**
     * Returns the local site.
     */
    public WebSite getLocalSite()  { return _localSite; }

    /**
     * Returns the local cache site of remote site.
     */
    public WebSite getCloneSite()
    {
        WebFile cloneDir = getCloneDir();
        return cloneDir.getURL().getAsSite();
    }

    /**
     * Returns the repository site for remote URL (defaults to RemoteSite).
     */
    public WebSite getRepoSite()
    {
        return getRemoteSite();
    }

    /**
     * Returns the remote site.
     */
    public WebSite getRemoteSite()
    {
        if (_remoteURL != null)
            return _remoteURL.getAsSite();
        return null;
    }

    /**
     * Returns whether existing VCS artifacts are detected for site.
     */
    public boolean getExists()
    {
        WebSite cloneSite = getCloneSite();
        return cloneSite != null && cloneSite.getExists();
    }

    /**
     * Load remote files and VCS files into site directory.
     */
    public TaskRunner<Object> checkout(View aView)
    {
        String title = "Checkout from " + getRemoteURLString();
        TaskMonitor checkoutMonitor = aView != null ? new TaskMonitorPanel(aView, title) : TaskMonitor.NULL;
        TaskRunner<Object> checkoutRunner = new TaskRunner<>(() -> { checkout(aView, checkoutMonitor); return null; });
        checkoutRunner.start();
        return checkoutRunner;
    }

    /**
     * Load remote files and VCS files into site directory.
     */
    private void checkout(View aView, TaskMonitor taskMonitor)
    {
        // Try basic checkout
        try { checkoutImpl(taskMonitor); }

        // If failure
        catch (Exception e) {

            // If attempt to set permissions succeeds, try again
            WebSite remoteSite = getRemoteSite();
            boolean setPermissionsSuccess = ClientUtils.setAccess(remoteSite);
            if (setPermissionsSuccess) {
                checkoutImpl(taskMonitor);
                return;
            }

            // If attempt to login succeeds, try again
            LoginPage loginPage = new LoginPage();
            boolean loginSuccess = loginPage.showPanel(aView, remoteSite);
            if (loginSuccess) {
                checkoutImpl(taskMonitor);
                return;
            }

            throw e;
        }
    }

    /**
     * Load remote files and VCS files into site directory.
     */
    private void checkoutImpl(TaskMonitor taskMonitor)
    {
        // Find all files to update
        WebSite localSite = getLocalSite();
        WebFile localSiteRootDir = localSite.getRootDir();
        List<WebFile> updateFiles = new ArrayList<>();
        findUpdateFiles(localSiteRootDir, updateFiles);

        // Update files
        updateFiles(updateFiles, taskMonitor);
    }

    /**
     * Returns the local files for given file or directory that need to be updated.
     */
    public void findUpdateFiles(WebFile aFile, List<WebFile> updateFiles)
    {
        // If no clone site, just return
        if (!getExists()) return;

        // Get remote file for given file
        WebFile remoteFile = getRepoFile(aFile.getPath(), true, aFile.isDir());

        // Get remote changed files (update files)
        Set<WebFile> changedFiles = new HashSet<>();
        findChangedFiles(remoteFile, changedFiles);

        // Add local versions to list
        for (WebFile changedFile : changedFiles) {
            WebFile localFile = getLocalFile(changedFile.getPath(), changedFile.isDir());
            updateFiles.add(localFile);
        }
    }

    /**
     * Updates (merges) local site files from remote site.
     */
    public void updateFiles(List<WebFile> localFiles, TaskMonitor taskMonitor)
    {
        // Call TaskMonitor.startTasks
        taskMonitor.startTasks(localFiles.size());

        // Iterate over files and update each
        for (WebFile localFile : localFiles) {
            taskMonitor.beginTask("Updating " + localFile.getPath(), -1);
            if (taskMonitor.isCancelled())
                break;

            // Update file
            try { updateFile(localFile); }
            catch (AccessException e) {
                ClientUtils.setAccess(e.getSite());
                updateFile(localFile);
            }
            taskMonitor.endTask();
        }
    }

    /**
     * Updates (merges) local site files from remote site.
     */
    protected void updateFile(WebFile localFile)
    {
        // Get RepoFile and CloneFile
        String filePath = localFile.getPath();
        boolean isDir = localFile.isDir();
        WebFile repoFile = getRepoFile(filePath, true, isDir);
        WebFile cloneFile = getCloneFile(filePath, true, isDir);

        // Set new file bytes and save
        if (repoFile.getExists()) {

            // Update local file and save
            if (localFile.isFile())
                localFile.setBytes(repoFile.getBytes());
            localFile.save();
            localFile.setModTimeSaved(repoFile.getLastModTime());

            // Update clone file and save
            if (cloneFile.isFile())
                cloneFile.setBytes(repoFile.getBytes());
            cloneFile.save();
            cloneFile.setModTimeSaved(repoFile.getLastModTime());

            // Update status
            setFileStatus(localFile, null);
        }

        // Otherwise delete LocalFile and CloneFile
        else {
            if (localFile.getExists())
                localFile.delete();
            if (cloneFile.getExists())
                cloneFile.delete();
            setFileStatus(localFile, null);
        }
    }

    /**
     * Returns the files that need to be committed to server.
     */
    public void findCommitFiles(WebFile aFile, List<WebFile> commitFiles)
    {
        // If no clone site, just return
        if (!getExists()) return;

        // Find local changed files and add to commit files list
        Set<WebFile> changedFiles = new HashSet<>();
        findChangedFiles(aFile, changedFiles);
        commitFiles.addAll(changedFiles);
    }

    /**
     * Commits (copies) local site files to remote site.
     */
    public void commitFiles(List<WebFile> localFiles, String aMessage, TaskMonitor taskMonitor)
    {
        // Call TaskMonitor.startTasks
        taskMonitor.startTasks(localFiles.size());

        // Iterate over files
        for (WebFile file : localFiles) {
            if (taskMonitor.isCancelled())
                break;

            try { commitFile(file, aMessage); }
            catch (AccessException e) {
                ClientUtils.setAccess(e.getSite());
                commitFile(file, aMessage);
            }
            taskMonitor.endTask();
        }
    }

    /**
     * Commits (copies) local site file to remote site.
     */
    protected void commitFile(WebFile localFile, String aMessage)
    {
        // Get RepoFile and CloneFile
        String filePath = localFile.getPath();
        boolean isDir = localFile.isDir();
        WebFile repoFile = getRepoFile(filePath, true, isDir);
        WebFile cloneFile = getCloneFile(filePath, true, isDir);

        // If LocalFile was deleted, delete RepoFile and CloneFile
        if (!localFile.getExists()) {
            if (repoFile.getExists())
                repoFile.delete();
            if (cloneFile.getExists())
                cloneFile.delete();
            setFileStatus(localFile, null);
        }

        // Otherwise save LocalFile bytes to RepoFile and CloneFile
        else {
            if (localFile.isFile())
                repoFile.setBytes(localFile.getBytes());
            repoFile.save();
            if (localFile.isFile())
                cloneFile.setBytes(localFile.getBytes());
            cloneFile.save();
            cloneFile.setModTimeSaved(repoFile.getLastModTime());
            localFile.setModTimeSaved(repoFile.getLastModTime());
            setFileStatus(localFile, null);
        }
    }

    /**
     * Returns the local files for given file or directory that need to be replaced.
     */
    public void findReplaceFiles(WebFile aFile, List<WebFile> replaceFiles)
    {
        // If no clone site, just return
        if (!getExists()) return;

        // Find local changed files and add to replace files list
        Set<WebFile> changedFiles = new HashSet<>();
        findChangedFiles(aFile, changedFiles);
        replaceFiles.addAll(changedFiles);
    }

    /**
     * Replaces (overwrites) local site files from clone site.
     */
    public void replaceFiles(List<WebFile> localFiles, TaskMonitor taskMonitor)
    {
        // Call TaskMonitor.startTasks
        taskMonitor.startTasks(localFiles.size());

        // Iterate over files
        for (WebFile file : localFiles) {

            // Update monitor task message
            taskMonitor.beginTask("Updating " + file.getPath(), -1);
            if (taskMonitor.isCancelled())
                break;

            // Replace file
            try { replaceFile(file); }
            catch (AccessException e) {
                ClientUtils.setAccess(e.getSite());
                updateFile(file);
            }

            // Close task
            taskMonitor.endTask();
        }
    }

    /**
     * Replaces (overwrites) local site files from clone site.
     */
    protected void replaceFile(WebFile localFile)
    {
        // Get CloneFile
        WebFile cloneFile = getCloneFile(localFile.getPath(), true, localFile.isDir());

        // Set new file bytes and save
        if (cloneFile.getExists()) {
            if (localFile.isFile())
                localFile.setBytes(cloneFile.getBytes());
            localFile.save();
            localFile.setModTimeSaved(cloneFile.getLastModTime());
            setFileStatus(localFile, null);
        }

        // Otherwise delete LocalFile and CloneFile
        else {
            if (localFile.getExists())
                localFile.delete();
            setFileStatus(localFile, null);
        }
    }

    /**
     * Returns the files that changed from last checkout.
     */
    protected void findChangedFiles(WebFile aFile, Set<WebFile> changedFiles)
    {
        // If file status is Added/Updated/Removed, add file
        FileStatus fileStatus = getFileStatus(aFile, false);
        if (fileStatus != FileStatus.Identical)
            changedFiles.add(aFile);

        // If file is directory, recurse for child files
        if (aFile.isDir() && !isIgnoreFile(aFile)) {

            // Recurse for child files
            WebFile[] childFiles = aFile.getFiles();
            for (WebFile file : childFiles)
                findChangedFiles(file, changedFiles);

            // Add missing files
            WebSite fileSite = aFile.getSite();
            WebFile cloneFile = getCloneFile(aFile.getPath(), false, false);
            if (cloneFile != null) {

                // Iterate over child files and recurse for missing files
                WebFile[] cloneChildFiles = cloneFile.getFiles();
                for (WebFile cloneChildFile : cloneChildFiles) {
                    WebFile otherChildFile = fileSite.getFileForPath(cloneChildFile.getPath());
                    if (otherChildFile == null) {
                        otherChildFile = fileSite.createFileForPath(cloneChildFile.getPath(), cloneChildFile.isDir());
                        findChangedFiles(otherChildFile, changedFiles);
                    }
                }
            }
        }
    }

    /**
     * Returns the site file for path.
     */
    private WebFile getLocalFile(String aPath, boolean isDir)
    {
        WebSite localSite = getLocalSite(); if (localSite == null) return null;
        WebFile localFile = localSite.getFileForPath(aPath);
        if (localFile == null)
            localFile = localSite.createFileForPath(aPath, isDir);
        return localFile;
    }

    /**
     * Returns the local cache file of given path.
     */
    public WebFile getCloneFile(String aPath, boolean doCreate, boolean isDir)
    {
        WebSite cloneSite = getCloneSite(); if (cloneSite == null) return null;
        WebFile cloneFile = cloneSite.getFileForPath(aPath);
        if (cloneFile == null && doCreate)
            cloneFile = cloneSite.createFileForPath(aPath, isDir);
        return cloneFile;
    }

    /**
     * Returns the remote file for path.
     */
    public WebFile getRepoFile(String aPath, boolean doCreate, boolean isDir)
    {
        WebSite repoSite = getRepoSite(); if (repoSite == null) return null;
        WebFile repoFile = repoSite.getFileForPath(aPath);
        if (repoFile == null && doCreate)
            repoFile = repoSite.createFileForPath(aPath, isDir);
        return repoFile;
    }

    /**
     * Returns whether the file has been modified from VCS.
     */
    public boolean isFileModified(WebFile aFile)
    {
        FileStatus fileStatus = getFileStatus(aFile);
        return fileStatus != FileStatus.Identical;
    }

    /**
     * Returns the file VersionControl status.
     */
    public synchronized FileStatus getFileStatus(WebFile aFile)
    {
        // Get cached status
        FileStatus fileStatus = _filesStatusCache.get(aFile);
        if (fileStatus != null)
            return fileStatus;

        // Determine status, cache and return
        fileStatus = getFileStatus(aFile, true);
        _filesStatusCache.put(aFile, fileStatus);
        return fileStatus;
    }

    /**
     * Returns the status for a given remote file info.
     */
    protected FileStatus getFileStatus(WebFile aFile, boolean isDeep)
    {
        // If no clone site or is ignore file, just return
        if (!getExists())
            return FileStatus.Identical;
        if (isIgnoreFile(aFile))
            return FileStatus.Identical;

        // If directory, iterate over child files and if any ChangedLocal, return ChangedLocal, otherwise return Identical
        if (aFile.isDir()) {
            if (isDeep) {
                WebFile[] childFiles = aFile.getFiles();
                for (WebFile childFile : childFiles) {
                    if (isFileModified(childFile))
                        return FileStatus.Modified;
                }
            }
            return FileStatus.Identical;
        }

        // If file doesn't exist, return removed
        if (!aFile.getExists())
            return FileStatus.Removed;

        // Get CloneFile - if not found, return Status.Added
        WebFile cloneFile = getCloneFile(aFile.getPath(), false, false);
        if (cloneFile == null)
            return FileStatus.Added;

        // Check modified times match, or bytes match, return identical
        long cloneModTime = cloneFile.getLastModTime();
        long localModTime = aFile.getLastModTime();
        if (localModTime == cloneModTime)
            return FileStatus.Identical;
        if (aFile.isFile() && cloneFile.isFile() && ArrayUtils.equals(aFile.getBytes(), cloneFile.getBytes()))
            return FileStatus.Identical;

        // Return Modified
        return FileStatus.Modified;
    }

    /**
     * Returns the file VersionControl status.
     */
    public void setFileStatus(WebFile aFile, FileStatus aFileStatus)
    {
        // Set new status, get old
        FileStatus old;
        synchronized (this) {
            old = _filesStatusCache.put(aFile, aFileStatus);
        }

        // If parent, clear Parent.Status
        if (aFile.getParent() != null)
            setFileStatus(aFile.getParent(), null);

        // Fire PropChange
        firePropChange(new PropChange(aFile, FileStatus_Prop, old, aFileStatus));
    }

    /**
     * Returns whether (local) file should be ignored.
     */
    protected boolean isIgnoreFile(WebFile aFile)
    {
        String name = aFile.getName();
        if (name.equals("bin"))
            return true;
        if (name.equals("CVS"))
            return true;
        if (name.startsWith("__"))
            return true;
        return name.equals(".git");
    }

    /**
     * Delete VCS support files from site directory.
     */
    public void disconnect(TaskMonitor taskMonitor) throws Exception
    {
        WebSite cloneSite = getCloneSite();
        if (cloneSite != null)
            cloneSite.deleteSite();
    }

    /**
     * Called when file added.
     */
    public void fileAdded(WebFile aFile)  { }

    /**
     * Called when file removed.
     */
    public void fileRemoved(WebFile aFile)
    {
        setFileStatus(aFile, null);
    }

    /**
     * Called when file saved.
     */
    public void fileSaved(WebFile aFile)
    {
        setFileStatus(aFile, null);
    }

    /**
     * Add listener.
     */
    public void addPropChangeListener(PropChangeListener aLsnr)
    {
        if (_pcs == PropChangeSupport.EMPTY) _pcs = new PropChangeSupport(this);
        _pcs.addPropChangeListener(aLsnr);
    }

    /**
     * Fires a given property change.
     */
    protected void firePropChange(PropChange aPC)
    {
        _pcs.firePropChange(aPC);
    }

    /**
     * Returns the Remote URL string for a site.
     */
    public static String getRemoteURLString(WebSite aSite)
    {
        // Get remote settings file
        WebSite sandboxSite = aSite.getSandbox();
        WebFile remoteSettingsFile = sandboxSite.getFileForPath("/settings/remote");
        if (remoteSettingsFile == null)
            return null;

        // Get file text and return
        String remoteSettingsText = remoteSettingsFile.getText();
        String urlString = remoteSettingsText.trim();
        return urlString.length() > 0 ? urlString : null;
    }

    /**
     * Sets the Remote URL string.
     */
    public static void setRemoteURLString(WebSite aSite, String aUrlString)
    {
        // If already set, just return
        if (Objects.equals(aUrlString, getRemoteURLString(aSite))) return;

        // Get remote settings file
        WebSite sandboxSite = aSite.getSandbox();
        WebFile file = sandboxSite.getFileForPath("/settings/remote");
        if (file == null)
            file = sandboxSite.createFileForPath("/settings/remote", false);

        // Save URL to remote settings file
        try {

            // If empty URL, delete file
            if (aUrlString == null || aUrlString.length() == 0) {
                if (file.getExists())
                    file.delete();
            }

            // Set file text and save
            else {
                file.setText(aUrlString);
                file.save();
            }
        }

        // Rethrow exceptions
        catch (Exception e) { throw new RuntimeException(e); }

        // Set VersionControl
        aSite.setProp(VersionControl.class.getName(), null);
    }

    /**
     * Returns the VersionControl for a site.
     */
    public synchronized static VersionControl getVersionControlForProjectSite(WebSite projectSite)
    {
        // Get VersionControl for project site (create if missing)
        VersionControl versionControl = (VersionControl) projectSite.getProp(VersionControl.class.getName());
        if (versionControl == null) {
            versionControl = createVersionControlForProjectSite(projectSite);
            projectSite.setProp(VersionControl.class.getName(), versionControl);
        }

        // Return
        return versionControl;
    }

    /**
     * Returns the VersionControl for a site.
     */
    public static VersionControl createVersionControlForProjectSite(WebSite projectSite)
    {
        //String urls = getRemoteURLString(projectSite);
        //if (urls != null) urls = urls.toLowerCase();

        // Handle Git
        //if (urls != null && (urls.startsWith("git:") || urls.endsWith(".git"))) return new VersionControlGit(projectSite);

        // Handle plain
        return new VersionControl(projectSite);
    }
}