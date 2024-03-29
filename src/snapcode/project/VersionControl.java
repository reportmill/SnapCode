/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.props.PropChangeSupport;
import snap.util.ArrayUtils;
import snap.util.TaskRunner;
import snapcode.webbrowser.ClientUtils;
import snap.util.TaskMonitor;
import snap.web.AccessException;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcode.webbrowser.LoginPage;
import java.util.*;
import java.util.function.Supplier;

/**
 * This is a class to handle file synchronization for a local WebSite with a remote WebSite.
 */
public class VersionControl {

    // The local site
    private WebSite _localSite;

    // The remote site URL
    private WebURL _remoteSiteUrl;

    // The clone site
    private WebSite _cloneSite;

    // The remote site
    private WebSite _remoteSite;

    // Whether this version control is available
    private Boolean _isAvailable;

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
     * Creates a VersionControl for a given project site.
     */
    public VersionControl(WebSite aSite)
    {
        _localSite = aSite;

        // Get remote site URL
        String urlString = getRemoteSiteUrlString(aSite);
        if (urlString != null)
            _remoteSiteUrl = WebURL.getURL(urlString);

        // If remote size is Zip file with single dir with same name as Zip file, use dir
        if (_remoteSiteUrl != null && _remoteSiteUrl.getType().equals("zip")) {
            WebSite zipSite = _remoteSiteUrl.getAsSite();
            String siteName = _remoteSiteUrl.getFilenameSimple();
            WebFile dirFile = zipSite.getFileForPath('/' + siteName);
            if (dirFile != null && dirFile.isDir())
                _remoteSiteUrl = dirFile.getURL();
        }

        // Set this version control as prop in project site
        aSite.setProp(VersionControl.class.getName(), this);
    }

    /**
     * Returns the local site.
     */
    public WebSite getLocalSite()  { return _localSite; }

    /**
     * Returns the remote site URL.
     */
    public WebURL getRemoteSiteUrl()  { return _remoteSiteUrl; }

    /**
     * Returns the remote site URL string.
     */
    public String getRemoteSiteUrlString()  { return _remoteSiteUrl != null ? _remoteSiteUrl.getString() : null; }

    /**
     * Returns the local cache site of remote site.
     */
    public WebSite getCloneSite()
    {
        if (_cloneSite != null) return _cloneSite;

        // Get clone site (via clone dir and url)
        WebSite localSite = getLocalSite();
        WebSite sandboxSite = localSite.getSandboxSite();
        WebFile cloneDir = sandboxSite.createFileForPath("Remote.clone", true);
        WebURL cloneUrl = cloneDir.getURL();
        WebSite cloneSite = cloneUrl.getAsSite();

        // Set and return
        return _cloneSite = cloneSite;
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
        if (_remoteSite != null) return _remoteSite;
        WebSite remoteSite = _remoteSiteUrl != null ? _remoteSiteUrl.getAsSite() : null;
        return _remoteSite = remoteSite;
    }

    /**
     * Returns whether existing VCS artifacts are detected for site.
     */
    public boolean isAvailable()
    {
        if (_isAvailable != null) return _isAvailable;
        WebSite remoteSite = getRemoteSite();
        boolean remoteAvailable = remoteSite != null && remoteSite.getExists();
        return _isAvailable = remoteAvailable;
    }

    /**
     * Load remote files and VCS files into site directory.
     */
    public TaskRunner<Boolean> checkout()
    {
        String title = "Checkout from " + getRemoteSiteUrlString();
        TaskMonitor taskMonitor = new TaskMonitor(title);
        TaskRunner<Boolean> taskRunner = new TaskRunner<>(() -> checkoutImpl(taskMonitor));
        taskRunner.setMonitor(taskMonitor);
        taskRunner.start();
        return taskRunner;
    }

    /**
     * Load remote files and VCS files into site directory.
     */
    private boolean checkoutImpl(TaskMonitor taskMonitor)
    {
        // Try basic checkout
        try { return checkoutImpl2(taskMonitor); }

        // If failure
        catch (Exception e) {

            // If attempt to set permissions succeeds, try again
            WebSite remoteSite = getRemoteSite();
            boolean setPermissionsSuccess = ClientUtils.setAccess(remoteSite);
            if (setPermissionsSuccess)
                return checkoutImpl2(taskMonitor);

            // If attempt to login succeeds, try again
            LoginPage loginPage = new LoginPage();
            boolean loginSuccess = loginPage.showPanel(null, remoteSite);
            if (loginSuccess)
                return checkoutImpl2(taskMonitor);

            throw e;
        }
    }

    /**
     * Load remote files and VCS files into site directory.
     */
    private boolean checkoutImpl2(TaskMonitor taskMonitor)
    {
        // Find all files to update
        WebSite localSite = getLocalSite();
        WebFile localSiteRootDir = localSite.getRootDir();
        List<WebFile> updateFiles = new ArrayList<>();
        findUpdateFiles(localSiteRootDir, updateFiles);

        // Update files
        return updateFilesImpl(updateFiles, taskMonitor);
    }

    /**
     * Update files.
     */
    public TaskRunner<Boolean> updateFiles(List<WebFile> theFiles)
    {
        TaskMonitor taskMonitor = new TaskMonitor("Update files from remote site");
        Supplier<Boolean> updateFunc = () -> updateFilesImpl(theFiles, taskMonitor);
        TaskRunner<Boolean> taskRunner = new TaskRunner<>(updateFunc);
        taskRunner.setMonitor(taskMonitor);
        taskRunner.start();
        return taskRunner;
    }

    /**
     * Updates (merges) local site files from remote site.
     */
    protected boolean updateFilesImpl(List<WebFile> localFiles, TaskMonitor taskMonitor)
    {
        // Call TaskMonitor.startTasks
        taskMonitor.startTasks(localFiles.size());
        boolean completed = true;

        // Iterate over files and update each
        for (WebFile localFile : localFiles) {

            // Update monitor task message
            taskMonitor.beginTask("Updating " + localFile.getPath(), -1);
            if (taskMonitor.isCancelled()) {
                completed = false;
                break;
            }

            // Update file
            try { updateFile(localFile); }
            catch (AccessException e) {
                ClientUtils.setAccess(e.getSite());
                updateFile(localFile);
            }

            // Close task
            taskMonitor.endTask();
        }

        // Return
        return completed;
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
            localFile.saveLastModTime(repoFile.getLastModTime());

            // Update clone file and save
            if (cloneFile.isFile())
                cloneFile.setBytes(repoFile.getBytes());
            cloneFile.save();
            cloneFile.saveLastModTime(repoFile.getLastModTime());

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
     * Replace files.
     */
    public TaskRunner<Boolean> replaceFiles(List<WebFile> theFiles)
    {
        TaskMonitor taskMonitor = new TaskMonitor("Replace files from remote site");
        Supplier<Boolean> replaceFunc = () -> replaceFilesImpl(theFiles, taskMonitor);
        TaskRunner<Boolean> taskRunner = new TaskRunner<>(replaceFunc);
        taskRunner.setMonitor(taskMonitor);
        taskRunner.start();
        return taskRunner;
    }

    /**
     * Replaces (overwrites) local site files from clone site.
     */
    protected boolean replaceFilesImpl(List<WebFile> localFiles, TaskMonitor taskMonitor)
    {
        // Call TaskMonitor.startTasks
        taskMonitor.startTasks(localFiles.size());
        boolean completed = true;

        // Iterate over files
        for (WebFile file : localFiles) {

            // Update monitor task message
            taskMonitor.beginTask("Updating " + file.getPath(), -1);
            if (taskMonitor.isCancelled()) {
                completed = false;
                break;
            }

            // Replace file
            try { replaceFile(file); }
            catch (AccessException e) {
                ClientUtils.setAccess(e.getSite());
                updateFile(file);
            }

            // Close task
            taskMonitor.endTask();
        }

        // Return
        return completed;
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
            localFile.saveLastModTime(cloneFile.getLastModTime());
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
     * Commit files.
     */
    public TaskRunner<Boolean> commitFiles(List<WebFile> theFiles, String aMessage)
    {
        TaskMonitor taskMonitor = new TaskMonitor("Commit files to remote site");
        Supplier<Boolean> commitFunc = () -> commitFilesImpl(theFiles, aMessage, taskMonitor);
        TaskRunner<Boolean> taskRunner = new TaskRunner<>(commitFunc);
        taskRunner.setMonitor(taskMonitor);
        taskRunner.start();
        return taskRunner;
    }

    /**
     * Commits (copies) local site files to remote site.
     */
    protected boolean commitFilesImpl(List<WebFile> localFiles, String aMessage, TaskMonitor taskMonitor)
    {
        // Call TaskMonitor.startTasks
        taskMonitor.startTasks(localFiles.size());
        boolean completed = true;

        // Iterate over files
        for (WebFile file : localFiles) {

            // Update monitor task message
            taskMonitor.beginTask("Committing " + file.getPath(), -1);
            if (taskMonitor.isCancelled())
                break;

            try { commitFile(file); }
            catch (AccessException e) {
                ClientUtils.setAccess(e.getSite());
                commitFile(file);
            }

            // Close task
            taskMonitor.endTask();
        }

        // Return
        return completed;
    }

    /**
     * Commits (copies) local site file to remote site.
     */
    protected void commitFile(WebFile localFile)
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
            cloneFile.saveLastModTime(repoFile.getLastModTime());
            localFile.saveLastModTime(repoFile.getLastModTime());
            setFileStatus(localFile, null);
        }
    }

    /**
     * Returns the changed files for given root files and version control operation.
     */
    public List<WebFile> getChangedFilesForRootFiles(List<WebFile> rootFiles, VersionControl.Op operation)
    {
        List<WebFile> changedFiles = new ArrayList<>();

        try {
            for (WebFile file : rootFiles) {
                switch (operation) {
                    case Update: findUpdateFiles(file, changedFiles); break;
                    case Replace: findReplaceFiles(file, changedFiles); break;
                    case Commit: findCommitFiles(file, changedFiles); break;
                }
            }
        }

        // Handle AccessException:
        catch (AccessException e) {
            if (ClientUtils.setAccess(e.getSite()))
                return getChangedFilesForRootFiles(rootFiles, operation);
            throw e;
        }

        // Handle Exception
        /*catch (Exception e) { DialogBox dialogBox = new DialogBox("Disconnect Error");
            dialogBox.setErrorMessage(e.toString()); dialogBox.showMessageDialog(_workspacePane.getUI()); }*/

        // Sort and return
        Collections.sort(changedFiles);
        return changedFiles;
    }

    /**
     * Returns the local files for given file or directory that need to be updated.
     */
    private void findUpdateFiles(WebFile aFile, List<WebFile> updateFiles)
    {
        // If no clone site, just return
        if (!isAvailable()) return;

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
     * Returns the local files for given file or directory that need to be replaced.
     */
    private void findReplaceFiles(WebFile aFile, List<WebFile> replaceFiles)
    {
        // If no clone site, just return
        if (!isAvailable()) return;

        // Find local changed files and add to replace files list
        Set<WebFile> changedFiles = new HashSet<>();
        findChangedFiles(aFile, changedFiles);
        replaceFiles.addAll(changedFiles);
    }

    /**
     * Returns the files that need to be committed to server.
     */
    private void findCommitFiles(WebFile aFile, List<WebFile> commitFiles)
    {
        // If no clone site, just return
        if (!isAvailable()) return;

        // Find local changed files and add to commit files list
        Set<WebFile> changedFiles = new HashSet<>();
        findChangedFiles(aFile, changedFiles);
        commitFiles.addAll(changedFiles);
    }

    /**
     * Returns the files that changed from last checkout.
     */
    private void findChangedFiles(WebFile aFile, Set<WebFile> changedFiles)
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
        if (!isAvailable())
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
     * Returns the remote site URL string for a given project site.
     */
    public static String getRemoteSiteUrlString(WebSite projectSite)
    {
        // Get remote settings file
        WebSite sandboxSite = projectSite.getSandboxSite();
        WebFile remoteSettingsFile = sandboxSite.getFileForPath("/settings/remote");
        if (remoteSettingsFile == null)
            return null;

        // Get file text and return
        String remoteSettingsText = remoteSettingsFile.getText();
        String urlString = remoteSettingsText.trim();
        return urlString.length() > 0 ? urlString : null;
    }

    /**
     * Sets the remote site URL string for a given project site.
     */
    public static void setRemoteURLString(WebSite projectSite, String urlString)
    {
        // If already set, just return
        if (Objects.equals(urlString, getRemoteSiteUrlString(projectSite))) return;

        // Get remote settings file
        WebSite sandboxSite = projectSite.getSandboxSite();
        WebFile file = sandboxSite.getFileForPath("/settings/remote");
        if (file == null)
            file = sandboxSite.createFileForPath("/settings/remote", false);

        // Save URL to remote settings file
        try {

            // If empty URL, delete file
            if (urlString == null || urlString.length() == 0) {
                if (file.getExists())
                    file.delete();
            }

            // Set file text and save
            else {
                file.setText(urlString);
                file.save();
            }
        }

        // Rethrow exceptions
        catch (Exception e) { throw new RuntimeException(e); }

        // Set VersionControl
        projectSite.setProp(VersionControl.class.getName(), null);
    }

    /**
     * Returns the VersionControl for the given site.
     */
    public synchronized static VersionControl getVersionControlForProjectSite(WebSite projectSite)
    {
        // Get VersionControl for project site (create if missing)
        VersionControl versionControl = (VersionControl) projectSite.getProp(VersionControl.class.getName());
        if (versionControl == null)
            versionControl = createVersionControlForProjectSite(projectSite);

        // Return
        return versionControl;
    }

    /**
     * Creates the VersionControl for the given site.
     */
    private static VersionControl createVersionControlForProjectSite(WebSite projectSite)
    {
        //String urls = getRemoteURLString(projectSite);
        //if (urls != null) urls = urls.toLowerCase();

        // Handle Git
        //if (urls != null && (urls.startsWith("git:") || urls.endsWith(".git"))) return new VersionControlGit(projectSite);

        // Handle plain
        return new VersionControl(projectSite);
    }
}