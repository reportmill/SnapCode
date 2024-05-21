/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.props.PropChangeSupport;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.util.TaskRunner;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.*;
import java.util.function.Supplier;

/**
 * This is a class to handle file synchronization for a local WebSite with a remote WebSite.
 */
public class VersionControl {

    // The local site
    private WebSite _localSite;

    // The remote site URL
    protected WebURL _remoteSiteUrl;

    // The remote site
    private WebSite _remoteSite;

    // Whether this version control is available
    private Boolean _isAvailable;

    // A map of file to it's status
    private Map<WebFile, FileStatus> _filesStatusCache = new HashMap<>();

    // The PropChangeSupport
    private PropChangeSupport _pcs = PropChangeSupport.EMPTY;

    // Constants for the state of files relative to remote cache
    public enum FileStatus { Added, Removed, Modified, Identical }

    // Constants for properties
    public static final String FileStatus_Prop = "FileStatus";

    /**
     * Constructor.
     */
    protected VersionControl(WebSite localSite, WebURL remoteSiteUrl)
    {
        _localSite = localSite;
        _remoteSiteUrl = remoteSiteUrl;

        // Set this version control as prop in project site
        localSite.setProp(VersionControl.class.getName(), this);
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
     * Returns the remote site URL address string.
     */
    public String getRemoteSiteUrlAddress()  { return _remoteSiteUrl != null ? _remoteSiteUrl.getString() : null; }

    /**
     * Returns the remote site (e.g. Git repository site, Zip file site, etc.).
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
        String title = "Checkout from " + getRemoteSiteUrlAddress();
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
        if (!isAvailable()) { System.err.println("VersionControl.checkout: Remote not available"); return false; }

        // Find all files to update
        WebSite localSite = getLocalSite();
        WebFile localSiteRootDir = localSite.getRootDir();
        List<WebFile> updateFiles = getUpdateFilesForRootFiles(Collections.singletonList(localSiteRootDir));

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

        // Iterate over files and update each
        for (WebFile localFile : localFiles) {
            taskMonitor.beginTask("Updating " + localFile.getPath(), -1);
            updateFile(localFile);
            taskMonitor.endTask();
            if (taskMonitor.isCancelled())
                break;
        }

        // Return
        return !taskMonitor.isCancelled();
    }

    /**
     * Updates (merges) local site files from remote site.
     */
    protected void updateFile(WebFile localFile)
    {
        // Get RemoteFile
        String filePath = localFile.getPath();
        boolean isDir = localFile.isDir();
        WebFile remoteFile = getRemoteFile(filePath, true, isDir); assert (remoteFile != null);

        // Set new file bytes and save
        if (remoteFile.getExists()) {

            // Update local file and save
            if (localFile.isFile())
                localFile.setBytes(remoteFile.getBytes());
            localFile.save();
            localFile.saveLastModTime(remoteFile.getLastModTime());
        }

        // Otherwise delete LocalFile
        else if (localFile.getExists())
            localFile.delete();

        // Clear file status
        clearFileStatus(localFile);
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
     * Replaces (overwrites) local site files from remote site.
     */
    protected boolean replaceFilesImpl(List<WebFile> localFiles, TaskMonitor taskMonitor)
    {
        // Call TaskMonitor.startTasks
        taskMonitor.startTasks(localFiles.size());

        // Iterate over files and replace each
        for (WebFile file : localFiles) {
            taskMonitor.beginTask("Replacing " + file.getPath(), -1);
            replaceFile(file);
            taskMonitor.endTask();
            if (taskMonitor.isCancelled())
                break;
        }

        // Return
        return !taskMonitor.isCancelled();
    }

    /**
     * Replaces (overwrites) local site files from remote site.
     */
    protected void replaceFile(WebFile localFile)
    {
        // Get RemoteFile
        String filePath = localFile.getPath();
        WebFile remoteFile = getRemoteFile(filePath, false, localFile.isDir());

        // Set new file bytes and save
        if (remoteFile != null) {
            if (localFile.isFile())
                localFile.setBytes(remoteFile.getBytes());
            localFile.save();
            localFile.saveLastModTime(remoteFile.getLastModTime());
        }

        // Otherwise delete LocalFile
        else if (localFile.getExists())
            localFile.delete();

        // Clear file status
        clearFileStatus(localFile);
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

        // Iterate over files and commit each
        for (WebFile localFile : localFiles) {
            taskMonitor.beginTask("Committing " + localFile.getPath(), -1);
            commitFile(localFile);
            taskMonitor.endTask();
            if (taskMonitor.isCancelled())
                break;
        }

        // Return
        return !taskMonitor.isCancelled();
    }

    /**
     * Commits (copies) local site file to remote site.
     */
    protected void commitFile(WebFile localFile)
    {
        // Get RemoteFile
        String filePath = localFile.getPath();
        boolean isDir = localFile.isDir();
        WebFile remoteFile = getRemoteFile(filePath, true, isDir); assert (remoteFile != null);

        // If LocalFile exists, save LocalFile bytes to RemoteFile
        if (localFile.getExists()) {
            if (localFile.isFile())
                remoteFile.setBytes(localFile.getBytes());
            remoteFile.save();
            localFile.saveLastModTime(remoteFile.getLastModTime());
        }

        // Otherwise if LocalFile was deleted, delete RemoteFile
        else if (remoteFile.getExists())
            remoteFile.delete();

        // Clear file status
        clearFileStatus(localFile);
    }

    /**
     * Returns the local files that need to be updated from remote for given root files.
     */
    public List<WebFile> getUpdateFilesForRootFiles(List<WebFile> rootFiles)
    {
        // Map root files to remote files
        List<WebFile> remoteRootFiles = ListUtils.map(rootFiles, file -> getRemoteFile(file.getPath(), true, file.isDir()));

        // Get remote modified files
        WebSite otherSite = getLocalSite();
        Set<WebFile> modifiedFilesSet = new TreeSet<>();
        remoteRootFiles.forEach(rootFile -> findModifiedFiles(rootFile, otherSite, modifiedFilesSet));
        List<WebFile> modifiedFiles = new ArrayList<>(modifiedFilesSet);

        // map remote modified files to local
        return ListUtils.map(modifiedFiles, file -> getLocalFile(file.getPath(), file.isDir()));
    }

    /**
     * Returns the local files that have been modified from remote for given root files.
     */
    public List<WebFile> getModifiedFilesForRootFiles(List<WebFile> rootFiles)
    {
        WebSite remoteSite = getRemoteSite();
        Set<WebFile> modifiedFiles = new TreeSet<>();
        rootFiles.forEach(rootFile -> findModifiedFiles(rootFile, remoteSite, modifiedFiles));
        return new ArrayList<>(modifiedFiles);
    }

    /**
     * Finds the local files that have been modified from remote for given root files.
     */
    private void findModifiedFiles(WebFile aFile, WebSite otherSite, Set<WebFile> modifiedFiles)
    {
        // If ignored file, just return
        if (isIgnoreFile(aFile))
            return;

        // Handle file: If file status is Added/Updated/Removed, add file
        if (aFile.isFile()) {
            FileStatus fileStatus = calcFileStatusForOtherSite(aFile, otherSite);
            if (fileStatus != FileStatus.Identical)
                modifiedFiles.add(aFile);
            return;
        }

        // Handle directory: Recurse for child files
        WebFile[] childFiles = aFile.getFiles();
        for (WebFile childFile : childFiles)
            findModifiedFiles(childFile, otherSite, modifiedFiles);

        // Find files that exist in other site that are missing from given file site
        findMissingFiles(aFile, otherSite, modifiedFiles);
    }

    /**
     * Looks for given directory file in other site, and looks for files in other site that are missing in dir file site.
     */
    private void findMissingFiles(WebFile dirFile, WebSite otherSite, Set<WebFile> modifiedFiles)
    {
        // Get other dir file - just return if missing
        WebFile otherDir = otherSite.getFileForPath(dirFile.getPath());
        if (otherDir == null)
            return;

        // Iterate over child files and recurse for missing files
        WebFile[] otherDirChildFiles = otherDir.getFiles();
        WebSite fileSite = dirFile.getSite();

        // Iterate over child files and recurse for missing files
        for (WebFile otherChildFile : otherDirChildFiles) {

            // If matching dir child file exists, just skip
            WebFile dirChildFile = fileSite.getFileForPath(otherChildFile.getPath());
            if (dirChildFile != null)
                continue;

            // Create dir child file and recurse into findModifiedFiles
            dirChildFile = fileSite.createFileForPath(otherChildFile.getPath(), otherChildFile.isDir());
            findModifiedFiles(dirChildFile, otherSite, modifiedFiles);
        }
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
        fileStatus = calcFileStatusForOtherSite(aFile, getRemoteSite());
        _filesStatusCache.put(aFile, fileStatus);
        return fileStatus;
    }

    /**
     * Returns the status for a given remote file info.
     */
    protected FileStatus calcFileStatusForOtherSite(WebFile aFile, WebSite otherSite)
    {
        // If no remote site or is ignore file, just return
        if (!isAvailable())
            return FileStatus.Identical;
        if (isIgnoreFile(aFile))
            return FileStatus.Identical;

        // If directory, iterate over child files and if any ChangedLocal, return ChangedLocal, otherwise return Identical
        if (aFile.isDir()) {
            WebFile[] childFiles = aFile.getFiles();
            if (ArrayUtils.hasMatch(childFiles, file -> isFileModified(file)))
                return FileStatus.Modified;
            return FileStatus.Identical;
        }

        // If file doesn't exist, return removed
        if (!aFile.getExists())
            return FileStatus.Removed;

        // Get file from other site
        WebFile otherFile = otherSite.getFileForPath(aFile.getPath());
        if (otherFile == null)
            return FileStatus.Added;

        // If modified times match, return identical
        long localModTime = aFile.getLastModTime();
        long otherModTime = otherFile.getLastModTime();
        if (localModTime == otherModTime)
            return FileStatus.Identical;

        // If bytes match, return identical
        if (ArrayUtils.equals(aFile.getBytes(), otherFile.getBytes()))
            return FileStatus.Identical;

        // Return Modified
        return FileStatus.Modified;
    }

    /**
     * Clears the file status of given file.
     */
    private synchronized void clearFileStatus(WebFile aFile)
    {
        // Clear status, get old
        FileStatus oldStatus = _filesStatusCache.remove(aFile);

        // If parent, clear Parent.Status
        WebFile parentFie = aFile.getParent();
        if (parentFie != null)
            clearFileStatus(parentFie);

        // Fire PropChange
        firePropChange(new PropChange(aFile, FileStatus_Prop, oldStatus, null));
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
    public void disconnect(TaskMonitor taskMonitor) throws Exception  { }

    /**
     * Returns the site file for path.
     */
    private WebFile getLocalFile(String filePath, boolean isDir)
    {
        WebSite localSite = getLocalSite(); if (localSite == null) return null;
        WebFile localFile = localSite.getFileForPath(filePath);
        if (localFile == null)
            localFile = localSite.createFileForPath(filePath, isDir);
        return localFile;
    }

    /**
     * Returns the remote file for path.
     */
    private WebFile getRemoteFile(String filePath, boolean doCreate, boolean isDir)
    {
        WebSite remoteSite = getRemoteSite(); if (remoteSite == null) return null;
        WebFile remoteFile = remoteSite.getFileForPath(filePath);
        if (remoteFile == null && doCreate)
            remoteFile = remoteSite.createFileForPath(filePath, isDir);
        return remoteFile;
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
        clearFileStatus(aFile);
    }

    /**
     * Called when file saved.
     */
    public void fileSaved(WebFile aFile)
    {
        clearFileStatus(aFile);
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
    protected void firePropChange(PropChange aPC)  { _pcs.firePropChange(aPC); }

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
        WebURL remoteUrl = VersionControlUtils.getRemoteSiteUrl(projectSite);

        // Handle Git
        //if (urlAddr != null && (urlAddr.startsWith("git:") || urlAddr.endsWith(".git"))) return new VersionControlGit(projectSite);

        // Handle Zip file
        if (remoteUrl != null && remoteUrl.getFileType().equals("zip"))
            return new VersionControlZip(projectSite, remoteUrl);

        // Handle plain
        return new VersionControl(projectSite, remoteUrl);
    }
}