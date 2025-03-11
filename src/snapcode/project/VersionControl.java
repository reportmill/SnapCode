/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.props.PropChangeSupport;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.*;

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

    // The clone site
    private WebSite _cloneSite;

    // Whether this version control is available
    private Boolean _isAvailable;

    // A map of file to it's status
    private Map<WebFile, FileStatus> _filesStatusCache = Collections.synchronizedMap(new HashMap<>());

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
        return _remoteSite = getRemoteSiteImpl();
    }

    /**
     * Returns the remote site (e.g. Git repository site, Zip file site, etc.).
     */
    protected WebSite getRemoteSiteImpl()
    {
        return _remoteSiteUrl != null ? _remoteSiteUrl.getAsSite() : null;
    }

    /**
     * Returns the local cache site of remote site.
     */
    public WebSite getCloneSite()
    {
        if (_cloneSite != null) return _cloneSite;
        return _cloneSite = getCloneSiteImpl();
    }

    /**
     * Returns the local cache site of remote site.
     */
    protected WebSite getCloneSiteImpl()
    {
        // Get clone site (via clone dir and url)
        WebSite localSite = getLocalSite();
        WebSite sandboxSite = localSite.getSandboxSite();
        WebFile cloneDir = sandboxSite.createFileForPath("Remote.clone", true);
        WebURL cloneUrl = cloneDir.getURL();
        return cloneUrl.getAsSite();
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
    public boolean checkout(TaskMonitor taskMonitor) throws Exception
    {
        if (!isAvailable()) { System.err.println("VersionControl.checkout: Remote not available"); return false; }

        // Find all files to update
        WebSite localSite = getLocalSite();
        WebFile localSiteRootDir = localSite.getRootDir();
        List<WebFile> updateFiles = getUpdateFilesForRootFiles(Collections.singletonList(localSiteRootDir));

        // Update files
        return updateFiles(updateFiles, taskMonitor);
    }

    /**
     * Updates (merges) local site files from remote site.
     */
    public boolean updateFiles(List<WebFile> localFiles, TaskMonitor taskMonitor) throws Exception
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
        WebFile remoteFile = createRemoteFile(localFile);
        WebFile cloneFile = createCloneFile(localFile);

        // Set new file bytes and save
        if (remoteFile.getExists()) {

            // Update local file and save
            if (localFile.isFile())
                localFile.setBytes(remoteFile.getBytes());
            localFile.save();
            localFile.saveLastModTime(remoteFile.getLastModTime());

            // Update clone file
            if (cloneFile.isFile())
                cloneFile.setBytes(remoteFile.getBytes());
            cloneFile.save();
            cloneFile.saveLastModTime(remoteFile.getLastModTime());
        }

        // Otherwise delete LocalFile and CloneFile
        else {
            if (localFile.getExists())
                localFile.delete();
            if (cloneFile.getExists())
                cloneFile.delete();
        }

        // Clear file status
        clearFileStatus(localFile);
    }

    /**
     * Replaces (overwrites) local site files from remote site.
     */
    public boolean replaceFiles(List<WebFile> localFiles, TaskMonitor taskMonitor) throws Exception
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
    protected void replaceFile(WebFile localFile) throws Exception
    {
        // Get RemoteFile
        String filePath = localFile.getPath();
        WebFile cloneFile = getCloneFile(filePath);

        // Set new file bytes and save
        if (cloneFile != null) {
            if (localFile.isFile())
                localFile.setBytes(cloneFile.getBytes());
            localFile.save();
            localFile.saveLastModTime(cloneFile.getLastModTime());
        }

        // Otherwise delete LocalFile
        else if (localFile.getExists())
            localFile.delete();

        // Clear file status
        clearFileStatus(localFile);
    }

    /**
     * Commits (copies) local site files to remote site.
     */
    public boolean commitFiles(List<WebFile> localFiles, String aMessage, TaskMonitor taskMonitor) throws Exception
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
        // Get RemoteFile and clone file
        WebFile remoteFile = createRemoteFile(localFile);
        WebFile cloneFile = createCloneFile(localFile);

        // If LocalFile exists, save LocalFile bytes to RemoteFile
        if (localFile.getExists()) {
            if (localFile.isFile())
                remoteFile.setBytes(localFile.getBytes());
            remoteFile.save();
            localFile.saveLastModTime(remoteFile.getLastModTime());
            if (localFile.isFile())
                cloneFile.setBytes(localFile.getBytes());
            cloneFile.save();
            cloneFile.saveLastModTime(remoteFile.getLastModTime());
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
        List<WebFile> remoteRootFiles = ListUtils.map(rootFiles, file -> createRemoteFile(file));

        // Get remote modified files
        WebSite otherSite = getCloneSite();
        Set<WebFile> modifiedFilesSet = new TreeSet<>();
        remoteRootFiles.forEach(rootFile -> findModifiedFiles(rootFile, otherSite, modifiedFilesSet));
        List<WebFile> modifiedFiles = new ArrayList<>(modifiedFilesSet);

        // map remote modified files to local
        return ListUtils.map(modifiedFiles, file -> createLocalFile(file));
    }

    /**
     * Returns the local files that have been modified from remote for given root files.
     */
    public List<WebFile> getModifiedFilesForRootFiles(List<WebFile> rootFiles)
    {
        WebSite remoteSite = getCloneSite();
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
    public FileStatus getFileStatus(WebFile aFile)
    {
        // Get cached status
        FileStatus fileStatus = _filesStatusCache.get(aFile);
        if (fileStatus != null)
            return fileStatus;

        // Determine status, cache and return
        fileStatus = calcFileStatusForOtherSite(aFile, getCloneSite());
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
    protected void clearFileStatus(WebFile aFile)
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
     * Returns the site file for file.
     */
    private WebFile createLocalFile(WebFile aFile)
    {
        WebSite localSite = getLocalSite();
        return localSite.createFileForPath(aFile.getPath(), aFile.isDir());
    }

    /**
     * Returns the remote file for path.
     */
    private WebFile getRemoteFile(String filePath)
    {
        WebSite remoteSite = getRemoteSite(); if (remoteSite == null) return null;
        return remoteSite.getFileForPath(filePath);
    }

    /**
     * Returns the remote file for file.
     */
    private WebFile createRemoteFile(WebFile aFile)
    {
        WebSite remoteSite = getRemoteSite();
        return remoteSite.createFileForPath(aFile.getPath(), aFile.isDir());
    }

    /**
     * Returns the clone file for path.
     */
    private WebFile getCloneFile(String filePath)
    {
        WebSite cloneSite = getCloneSite();
        return cloneSite.getFileForPath(filePath);
    }

    /**
     * Returns the clone file for file.
     */
    private WebFile createCloneFile(WebFile aFile)
    {
        WebSite cloneSite = getCloneSite();
        return cloneSite.createFileForPath(aFile.getPath(), aFile.isDir());
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
    public static VersionControl getVersionControlForProjectSite(WebSite projectSite)
    {
        // Get VersionControl for project site (create if missing)
        VersionControl versionControl = (VersionControl) projectSite.getProp(VersionControl.class.getName());
        if (versionControl != null)
            return versionControl;

        // Return
        return createVersionControlForProjectSite(projectSite);
    }

    /**
     * Creates the VersionControl for the given site.
     */
    private static VersionControl createVersionControlForProjectSite(WebSite projectSite)
    {
        // Try again
        VersionControl versionControl = (VersionControl) projectSite.getProp(VersionControl.class.getName());
        if (versionControl != null)
            return versionControl;

        WebURL remoteUrl = VersionControlUtils.getRemoteSiteUrl(projectSite);

        // Handle Git
        if (remoteUrl != null && (remoteUrl.getScheme().equals("git") || remoteUrl.getFileType().equals("git")))
            return new VersionControlGit(projectSite, remoteUrl);

        // Handle Zip file
        if (remoteUrl != null && remoteUrl.getFileType().equals("zip"))
            return new VersionControlZip(projectSite, remoteUrl);

        // Handle plain
        return new VersionControl(projectSite, remoteUrl);
    }
}