/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.props.PropChange;
import snap.props.PropObject;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import snap.web.WebResponse;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.*;

/**
 * This is a class to handle file synchronization for a local WebSite with a remote WebSite.
 */
public class VersionControl extends PropObject {

    // The local site
    private WebSite _localSite;

    // The remote site URL
    protected WebURL _remoteSiteUrl;

    // The remote site
    private WebSite _remoteSite;

    // The clone site
    private WebSite _cloneSite;

    // A map of file to it's status
    private Map<WebFile, FileStatus> _filesStatusCache = Collections.synchronizedMap(new HashMap<>());

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
        localSite.setMetadataForKey(VersionControl.class.getName(), this);
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
        WebFile sandboxDir = localSite.getSandboxDir();
        WebFile cloneDir = sandboxDir.createChildFileForPath("/Clone", true);
        return cloneDir.getAsSite();
    }

    /**
     * Returns whether remote site exists.
     */
    public boolean isRemoteExists()
    {
        WebSite remoteSite = getRemoteSite();
        return remoteSite != null && remoteSite.getExists();
    }

    /**
     * Returns whether remote site can be created.
     */
    public boolean canCreateRemote()  { return false; }

    /**
     * Creates the remote site.
     */
    public boolean createRemoteSite(TaskMonitor taskMonitor)
    {
        // If remote site not available, return false
        WebSite remoteSite = getRemoteSite();
        if (remoteSite == null)
            return false;

        // If remote already exists, return true
        if (remoteSite.getExists())
            return true;

        // Get remote dir and save
        WebFile remoteRootDir = remoteSite.getRootDir();
        WebResponse remoteRootDirSaveResp = remoteRootDir.save();
        if (remoteRootDirSaveResp.getCode() != WebResponse.OK)
            return false;

        // Create clone site
        return createCloneSite(taskMonitor);
    }

    /**
     * Returns whether clone site exists.
     */
    public boolean isCloneExists()
    {
        WebSite cloneSite = getCloneSite();
        return cloneSite != null && cloneSite.getExists();
    }

    /**
     * Creates the clone site.
     */
    public boolean createCloneSite(TaskMonitor taskMonitor)
    {
        // Create clone
        WebSite cloneSite = getCloneSite();
        if (cloneSite == null)
            return false;

        // If clone already exists, return true
        if (cloneSite.getExists())
            return true;

        // Get remote dir and save
        WebFile cloneRootDir = cloneSite.getRootDir();
        WebResponse cloneRootDirSaveResp = cloneRootDir.save();
        return cloneRootDirSaveResp.getCode() == WebResponse.OK;
    }

    /**
     * Returns whether project has been checked out or cloned from remote.
     */
    public boolean isCheckedOut()
    {
        WebSite cloneSite = getCloneSite();
        return cloneSite != null && cloneSite.getExists();
    }

    /**
     * Load remote files and VCS files into site directory.
     */
    public boolean checkout(TaskMonitor taskMonitor) throws Exception
    {
        WebSite remoteSite = getRemoteSite();
        if (remoteSite == null) {
            System.err.println("VersionControl.checkout: Remote not available: " + getRemoteSiteUrlAddress());
            return false;
        }

        // Find all files to update
        WebSite localSite = getLocalSite();
        WebFile localSiteDir = localSite.getRootDir();
        List<WebFile> updateFiles = getUpdateFilesForLocalFiles(ListUtils.of(localSiteDir));

        // Update files
        return updateFiles(updateFiles, taskMonitor);
    }

    /**
     * Updates (merges) local site files from remote site.
     */
    public boolean updateFiles(List<WebFile> localFiles, TaskMonitor taskMonitor) throws Exception
    {
        // Call TaskMonitor.startTasks
        taskMonitor.startForTaskCount(localFiles.size());

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
     * Updates (merges) local and clone file from remote site.
     */
    protected void updateFile(WebFile localFile)
    {
        // Get RemoteFile
        WebFile remoteFile = createRemoteFile(localFile);
        WebFile cloneFile = createCloneFile(localFile);

        // Update clone and local file from remote
        updateFileFromFile(cloneFile, remoteFile);
        updateFileFromFile(localFile, remoteFile);
    }

    /**
     * Replaces (overwrites) local site files from remote site.
     */
    public boolean replaceFiles(List<WebFile> localFiles, TaskMonitor taskMonitor) throws Exception
    {
        // Call TaskMonitor.startTasks
        taskMonitor.startForTaskCount(localFiles.size());

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
     * Replaces (overwrites) local file from clone site.
     */
    private void replaceFile(WebFile localFile)
    {
        WebFile cloneFile = createCloneFile(localFile);
        updateFileFromFile(localFile, cloneFile);
    }

    /**
     * Commits (copies) local site files to remote site.
     */
    public boolean commitFiles(List<WebFile> localFiles, String aMessage, TaskMonitor taskMonitor) throws Exception
    {
        // Call TaskMonitor.startTasks
        taskMonitor.startForTaskCount(localFiles.size());

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
     * Commits (copies) local file to clone and remote site.
     */
    protected void commitFile(WebFile localFile)
    {
        // Get RemoteFile and clone file
        WebFile remoteFile = createRemoteFile(localFile);
        WebFile cloneFile = createCloneFile(localFile);

        // If LocalFile exists, save LocalFile bytes to RemoteFile
        updateFileFromFile(cloneFile, localFile);
        updateFileFromFile(remoteFile, localFile);

        // Clear file status
        clearFileStatus(localFile);
    }

    /**
     * Returns the local files that need to be updated from remote for given local files.
     * Files need to be updated if clone version is different from remote.
     */
    public List<WebFile> getUpdateFilesForLocalFiles(List<WebFile> localFiles)
    {
        List<WebFile> cloneFiles = ListUtils.map(localFiles, localFile -> createCloneFile(localFile));
        WebSite remoteSite = getRemoteSite();
        List<WebFile> modifiedCloneFiles = getModifiedFilesForFilesInOtherSite(cloneFiles, remoteSite);
        return ListUtils.map(modifiedCloneFiles, cloneFile -> createLocalFile(cloneFile));
    }

    /**
     * Returns the local files that have been modified from clone for given local files.
     * Files is modified if local version is different from clone.
     */
    public List<WebFile> getModifiedFilesForLocalFiles(List<WebFile> localFiles)
    {
        WebSite cloneSite = getCloneSite();
        return getModifiedFilesForFilesInOtherSite(localFiles, cloneSite);
    }

    /**
     * Returns the files that have been modified from given other site for given files.
     */
    private List<WebFile> getModifiedFilesForFilesInOtherSite(List<WebFile> theFiles, WebSite otherSite)
    {
        List<WebFile> modifiedFiles = new ArrayList<>();
        theFiles.forEach(rootFile -> findModifiedFilesForFileInOtherSite(rootFile, otherSite, modifiedFiles));
        return modifiedFiles;
    }

    /**
     * If given file is modified in given other site, it is added to given list. If file is directory, then recurse.
     */
    private void findModifiedFilesForFileInOtherSite(WebFile aFile, WebSite otherSite, List<WebFile> modifiedFiles)
    {
        // If ignored file, just return
        if (isIgnoreFile(aFile))
            return;

        // Handle file: If file status is Added/Updated/Removed, add file
        if (aFile.isFile()) {
            FileStatus fileStatus = getFileStatusForFileFileInOtherSite(aFile, otherSite);
            if (fileStatus != FileStatus.Identical)
                modifiedFiles.add(aFile);
            return;
        }

        // Recurse for child files
        if (aFile.getExists()) {
            List<WebFile> childFiles = aFile.getFiles();
            for (WebFile childFile : childFiles)
                findModifiedFilesForFileInOtherSite(childFile, otherSite, modifiedFiles);
        }

        // Recurse for other site child files
        findMissingFilesForDirFileInOtherSite(aFile, otherSite, modifiedFiles);
    }

    /**
     * Looks for given directory file in other site, and creates/adds missing child files.
     */
    private void findMissingFilesForDirFileInOtherSite(WebFile dirFile, WebSite otherSite, List<WebFile> modifiedFiles)
    {
        // Get other dir file - just return if also missing
        WebFile otherDir = otherSite.createFileForPath(dirFile.getPath(), true);
        if (!otherDir.getExists())
            return;

        // Get file site and other site child files
        WebSite fileSite = dirFile.getSite();
        List<WebFile> otherDirChildFiles = otherDir.getFiles();

        // Iterate over other dir child files and find missing child files
        for (WebFile otherDirChild : otherDirChildFiles) {
            WebFile dirFileChild = fileSite.createFileForPath(otherDirChild.getPath(), otherDirChild.isDir());
            if (!dirFileChild.getExists())
                findModifiedFilesForFileInOtherSite(dirFileChild, otherSite, modifiedFiles);
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
     * Returns the file status of given local file versus clone site.
     */
    public FileStatus getFileStatus(WebFile localFile)
    {
        // Get cached status
        FileStatus fileStatus = _filesStatusCache.get(localFile);
        if (fileStatus != null)
            return fileStatus;

        // If VCS not available, return identical
        if (!isCheckedOut())
            return FileStatus.Identical;

        // Get status of local file in clone site and cache
        WebSite cloneSite = getCloneSite();
        fileStatus = getFileStatusForFileFileInOtherSite(localFile, cloneSite);
        _filesStatusCache.put(localFile, fileStatus);

        // Return
        return fileStatus;
    }

    /**
     * Returns the status for a given remote file info.
     */
    protected FileStatus getFileStatusForFileFileInOtherSite(WebFile aFile, WebSite otherSite)
    {
        // If ignore file, just return
        if (isIgnoreFile(aFile))
            return FileStatus.Identical;

        // If directory, return modified if any child modified, otherwise return Identical
        if (aFile.isDir()) {
            List<WebFile> childFiles = aFile.getFiles();
            if (ListUtils.hasMatch(childFiles, file -> getFileStatusForFileFileInOtherSite(file, otherSite) != FileStatus.Identical))
                return FileStatus.Modified;
            return FileStatus.Identical;
        }

        // If file doesn't exist, return removed
        if (!aFile.getExists())
            return FileStatus.Removed;

        // Get file from other site
        WebFile otherFile = otherSite.createFileForPath(aFile.getPath(), aFile.isDir());
        if (!otherFile.getExists())
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
        String filename = aFile.getName();
        if (ArrayUtils.contains(IGNORE_FILENAMES, filename))
            return true;
        if (filename.startsWith("__"))
            return true;
        return false;
    }

    // Ignore filenames
    private static String[] IGNORE_FILENAMES = { "bin", ".git", "CVS", ".DS_Store" };

    /**
     * Delete VCS support files from site directory.
     */
    public void disconnect(TaskMonitor taskMonitor) throws Exception
    {
        // Delete clone site
        WebSite cloneSite = getCloneSite();
        if (cloneSite != null && cloneSite.getExists())
            cloneSite.deleteSite();
    }

    /**
     * Returns the site file for file.
     */
    private WebFile createLocalFile(WebFile aFile)
    {
        WebSite localSite = getLocalSite();
        return localSite.createFileForPath(aFile.getPath(), aFile.isDir());
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
     * Returns the clone file for file.
     */
    private WebFile createCloneFile(WebFile aFile)
    {
        WebSite cloneSite = getCloneSite();
        return cloneSite.createFileForPath(aFile.getPath(), aFile.isDir());
    }

    /**
     * Whether Version control supports commit messages.
     */
    public boolean supportsCommitMessages()  { return false; }

    /**
     * Called when a project file changes.
     */
    public void handleProjectFileChange(PropChange aPC)
    {
        // Get source and property name
        WebFile file = (WebFile) aPC.getSource();
        String propName = aPC.getPropName();

        // Handle Saved property: Call fileAdded or fileSaved
        if (propName == WebFile.Exists_Prop) {
            if ((Boolean) aPC.getOldValue())
                clearFileStatus(file);
        }

        // Handle LastModTime property: Call file saved
        else if (propName == WebFile.LastModTime_Prop)
            clearFileStatus(file);
    }

    /**
     * Updates given file from other file.
     */
    private static void updateFileFromFile(WebFile localFile, WebFile otherFile)
    {
        // If other file exists, copy to local file
        if (otherFile.getExists()) {
            if (localFile.isFile())
                localFile.setBytes(otherFile.getBytes());
            WebResponse resp = localFile.save();
            if (resp.getException() != null)
                throw new RuntimeException(resp.getException());
            localFile.saveLastModTime(otherFile.getLastModTime());
        }

        // Otherwise delete local file
        else if (localFile.getExists())
            localFile.delete();
    }

    /**
     * Returns the VersionControl for the given site.
     */
    public static VersionControl getVersionControlForProjectSite(WebSite projectSite)
    {
        // Get VersionControl for project site (create if missing)
        VersionControl versionControl = (VersionControl) projectSite.getMetadataForKey(VersionControl.class.getName());
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
        VersionControl versionControl = (VersionControl) projectSite.getMetadataForKey(VersionControl.class.getName());
        if (versionControl != null)
            return versionControl;

        // Get remote URL - just return if null
        WebURL remoteUrl = VersionControlUtils.getRemoteSiteUrl(projectSite);
        if (remoteUrl == null)
            return new VersionControl(projectSite, null);

        // Handle Git
        if (remoteUrl.getScheme().equals("git") || remoteUrl.getFileType().equals("git"))
            return new VersionControlGit(projectSite, remoteUrl);

        // Handle Zip file
        if (remoteUrl.getFileType().equals("zip"))
            return new VersionControlZip(projectSite, remoteUrl);

        // Handle Dropbox site
        if (remoteUrl.getScheme().equals("dropbox") || remoteUrl.getScheme().equals("dbox"))
            return new VersionControlDropbox(projectSite, remoteUrl);

        // Handle plain
        return new VersionControl(projectSite, remoteUrl);
    }
}