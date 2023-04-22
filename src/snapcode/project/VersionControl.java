/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.props.PropChangeSupport;
import snap.util.ArrayUtils;
import snapcode.webbrowser.ClientUtils;
import snap.util.TaskMonitor;
import snap.web.AccessException;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.*;

/**
 * This is a class to handle file synchronization with a remote WebSite for a data source.
 */
public class VersionControl {

    // The local site
    private WebSite  _site;

    // The remote URL
    private WebURL  _remoteURL;

    // A map of file to it's status
    private Map<WebFile, Status>  _filesStatus = new HashMap<>();

    // The PropChangeSupport
    private PropChangeSupport _pcs = PropChangeSupport.EMPTY;

    // Constants for Synchronization operations
    public enum Op { Update, Replace, Commit }

    // Constants for the state of files relative to remote cache
    public enum Status { Added, Removed, Modified, Identical }

    /**
     * Creates a VersionControl for a given site.
     */
    public VersionControl(WebSite aSite)
    {
        _site = aSite;
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
        WebSite sandboxSite = getSite().getSandbox();
        WebFile cloneDir = sandboxSite.getFileForPath("Remote.clone");
        if (cloneDir == null)
            cloneDir = sandboxSite.createFileForPath("Remote.clone", true);
        return cloneDir;
    }

    /**
     * Returns the local site.
     */
    public WebSite getSite()  { return _site; }

    /**
     * Returns the local cache site of remote site.
     */
    public WebSite getCloneSite()
    {
        return getCloneDir().getURL().getAsSite();
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
    public WebSite getRemoteSite()  { return _remoteURL != null ? _remoteURL.getAsSite() : null; }

    /**
     * Returns the site file for path.
     */
    private WebFile getSiteFile(String aPath, boolean isDir)
    {
        WebSite site = getSite();
        return getFile(site, aPath, true, isDir);
    }

    /**
     * Returns the local cache file of given path.
     */
    public WebFile getCloneFile(String aPath, boolean doCreate, boolean isDir)
    {
        WebSite cloneSite = getCloneSite();
        return getFile(cloneSite, aPath, doCreate, isDir);
    }

    /**
     * Returns the remote file for path.
     */
    public WebFile getRepoFile(String aPath, boolean doCreate, boolean isDir)
    {
        WebSite repoSite = getRepoSite();
        return getFile(repoSite, aPath, doCreate, isDir);
    }

    /**
     * Returns the file for site and path.
     */
    private WebFile getFile(WebSite aSite, String aPath, boolean doCreate, boolean isDir)
    {
        if (aSite == null) return null;
        WebFile file = aSite.getFileForPath(aPath);
        if (file == null && doCreate)
            file = aSite.createFileForPath(aPath, isDir);
        return file;
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
    public void checkout(TaskMonitor aTM) throws Exception
    {
        List<WebFile> files = new ArrayList<>();
        WebFile rootDir = getSite().getRootDir();
        getUpdateFiles(rootDir, files);
        updateFiles(files, aTM);
    }

    /**
     * Delete VCS support files from site directory.
     */
    public void disconnect(TaskMonitor aTM) throws Exception
    {
        WebSite cloneSite = getCloneSite();
        if (cloneSite != null)
            cloneSite.deleteSite();
    }

    /**
     * Returns whether the file has been modified from VCS.
     */
    public boolean isModified(WebFile aFile)
    {
        Status status = getStatus(aFile);
        return status != Status.Identical;
    }

    /**
     * Returns the file VersionControl status.
     */
    public synchronized Status getStatus(WebFile aFile)
    {
        // Get cached status
        Status status = _filesStatus.get(aFile);
        if (status != null)
            return status;

        // Determine status, cache and return
        status = getStatus(aFile, true);
        _filesStatus.put(aFile, status);
        return status;
    }

    /**
     * Returns the status for a given remote file info.
     */
    protected Status getStatus(WebFile aFile, boolean isDeep)
    {
        // If no clone site or is ignore file, just return
        if (!getExists())
            return Status.Identical;
        if (isIgnoreFile(aFile))
            return Status.Identical;

        // If directory, iterate over child files and if any ChangedLocal, return ChangedLocal, otherwise return Identical
        if (aFile.isDir()) {
            if (isDeep) {
                for (WebFile file : aFile.getFiles()) {
                    if (isModified(file))
                        return Status.Modified;
                }
            }
            return Status.Identical;
        }

        // If file doesn't exist, return removed
        if (!aFile.getExists())
            return Status.Removed;

        // Get CloneFile - if not found, return Status.Added
        WebFile cloneFile = getCloneFile(aFile.getPath(), false, false);
        if (cloneFile == null)
            return Status.Added;

        // Check modified times match, or bytes match, return identical
        long cloneMT = cloneFile.getLastModTime(), localMT = aFile.getLastModTime();
        if (localMT == cloneMT)
            return Status.Identical;
        if (aFile.isFile() && cloneFile.isFile() && ArrayUtils.equals(aFile.getBytes(), cloneFile.getBytes()))
            return Status.Identical;

        // Otherwise, return Modified
        return Status.Modified;
    }

    /**
     * Returns the file VersionControl status.
     */
    public void setStatus(WebFile aFile, Status aStatus)
    {
        // Set new status, get old
        Status old;
        synchronized (this) {
            old = _filesStatus.put(aFile, aStatus);
        }

        // If parent, clear Parent.Status
        if (aFile.getParent() != null)
            setStatus(aFile.getParent(), null);

        // Fire PropChange
        firePropChange(new PropChange(aFile, "Status", old, aStatus));
    }

    /**
     * Returns the local files for given file or directory that need to be updated.
     */
    public void getUpdateFiles(WebFile aFile, List<WebFile> theFiles)
    {
        // If no clone site, just return
        if (!getExists()) return;

        // Get remote file for given file
        WebFile remoteFile = getRepoFile(aFile.getPath(), true, aFile.isDir());

        // Get remote changed files (update files)
        Set<WebFile> updateFiles = getChangedFiles(remoteFile, new HashSet<>());

        // Add local versions to list
        for (WebFile file : updateFiles) {
            WebFile lfile = getSiteFile(file.getPath(), file.isDir());
            theFiles.add(lfile);
        }
    }

    /**
     * Returns the local files for given file or directory that need to be replaced.
     */
    public void getReplaceFiles(WebFile aFile, List<WebFile> theFiles)
    {
        // If no clone site, just return
        if (!getExists()) return;

        // Get local changed files and add to list
        Set<WebFile> commitFiles = getChangedFiles(aFile, new HashSet<>());
        theFiles.addAll(commitFiles);
    }

    /**
     * Returns the files that need to be committed to server.
     */
    public void getCommitFiles(WebFile aFile, List<WebFile> theFiles)
    {
        // If no clone site, just return
        if (!getExists()) return;

        // Get local changed files and add to list
        Set<WebFile> commitFiles = getChangedFiles(aFile, new HashSet<>());
        theFiles.addAll(commitFiles);
    }

    /**
     * Returns the files that changed from last checkout.
     */
    protected Set<WebFile> getChangedFiles(WebFile aFile, Set<WebFile> theFiles)
    {
        // If Added/Updated/Removed, add file
        Status status = getStatus(aFile, false);
        if (status != Status.Identical)
            theFiles.add(aFile);

        // If file is directory, recurse for child files
        if (aFile.isDir() && !isIgnoreFile(aFile)) {

            // Recurse for child files
            for (WebFile file : aFile.getFiles())
                getChangedFiles(file, theFiles);

            // Add missing files
            WebSite fsite = aFile.getSite();
            WebFile cfile = getCloneFile(aFile.getPath(), false, false);
            if (cfile != null) for (WebFile file : cfile.getFiles()) {
                if (fsite.getFileForPath(file.getPath()) == null)
                    getChangedFiles(fsite.createFileForPath(file.getPath(), file.isDir()), theFiles);
            }
        }

        // Return file set
        return theFiles;
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
        return name.equals(".git");
    }

    /**
     * Commits (copies) local site files to remote site.
     */
    public void commitFiles(List<WebFile> theLocalFiles, String aMessage, TaskMonitor aTM)
    {
        // Make sure we have a TaskMonitor and call TaskMonitor.startTasks
        if (aTM == null)
            aTM = TaskMonitor.NULL;
        aTM.startTasks(theLocalFiles.size());

        // Iterate over files
        for (WebFile file : theLocalFiles) {
            if (aTM.isCancelled())
                break;

            try { commitFile(file, aMessage); }
            catch (AccessException e) {
                ClientUtils.setAccess(e.getSite());
                commitFile(file, aMessage);
            }
            aTM.endTask();
        }
    }

    /**
     * Commits (copies) local site file to remote site.
     */
    protected void commitFile(WebFile aLocalFile, String aMessage)
    {
        // Get RepoFile and CloneFile
        String filePath = aLocalFile.getPath();
        boolean isDir = aLocalFile.isDir();
        WebFile repoFile = getRepoFile(filePath, true, isDir);
        WebFile cloneFile = getCloneFile(filePath, true, isDir);

        // If LocalFile was deleted, delete RepoFile and CloneFile
        if (!aLocalFile.getExists()) {
            if (repoFile.getExists()) repoFile.delete();
            if (cloneFile.getExists()) cloneFile.delete();
            setStatus(aLocalFile, null);
        }

        // Otherwise save LocalFile bytes to RepoFile and CloneFile
        else {
            if (aLocalFile.isFile())
                repoFile.setBytes(aLocalFile.getBytes());
            repoFile.save();
            if (aLocalFile.isFile())
                cloneFile.setBytes(aLocalFile.getBytes());
            cloneFile.save();
            cloneFile.setModTimeSaved(repoFile.getLastModTime());
            aLocalFile.setModTimeSaved(repoFile.getLastModTime());
            setStatus(aLocalFile, null);
        }
    }

    /**
     * Updates (merges) local site files from remote site.
     */
    public void updateFiles(List<WebFile> theLocalFiles, TaskMonitor aTM)
    {
        // Make sure we have a TaskMonitor and call TaskMonitor.startTasks
        if (aTM == null) aTM = TaskMonitor.NULL;
        aTM.startTasks(theLocalFiles.size());

        // Iterate over files
        for (WebFile file : theLocalFiles) {
            aTM.beginTask("Updating " + file.getPath(), -1);
            if (aTM.isCancelled()) break;
            try {
                updateFile(file);
            } catch (AccessException e) {
                ClientUtils.setAccess(e.getSite());
                updateFile(file);
            }
            aTM.endTask();
        }
    }

    /**
     * Updates (merges) local site files from remote site.
     */
    protected void updateFile(WebFile aLocalFile)
    {
        // Get RepoFile and CloneFile
        String filePath = aLocalFile.getPath();
        boolean isDir = aLocalFile.isDir();
        WebFile repoFile = getRepoFile(filePath, true, isDir);
        WebFile cloneFile = getCloneFile(filePath, true, isDir);

        // Set new file bytes and save
        if (repoFile.getExists()) {
            if (aLocalFile.isFile())
                aLocalFile.setBytes(repoFile.getBytes());
            aLocalFile.save();
            aLocalFile.setModTimeSaved(repoFile.getLastModTime());
            if (cloneFile.isFile())
                cloneFile.setBytes(repoFile.getBytes());
            cloneFile.save();
            cloneFile.setModTimeSaved(repoFile.getLastModTime());
            setStatus(aLocalFile, null);
        }

        // Otherwise delete LocalFile and CloneFile
        else {
            if (aLocalFile.getExists())
                aLocalFile.delete();
            if (cloneFile.getExists())
                cloneFile.delete();
            setStatus(aLocalFile, null);
        }
    }

    /**
     * Replaces (overwrites) local site files from clone site.
     */
    public void replaceFiles(List<WebFile> theLocalFiles, TaskMonitor aTM) throws Exception
    {
        // Make sure we have a TaskMonitor and call TaskMonitor.startTasks
        if (aTM == null) aTM = TaskMonitor.NULL;
        aTM.startTasks(theLocalFiles.size());

        // Iterate over files
        for (WebFile file : theLocalFiles) {
            aTM.beginTask("Updating " + file.getPath(), -1);
            if (aTM.isCancelled()) break;
            try {
                replaceFile(file);
            } catch (AccessException e) {
                ClientUtils.setAccess(e.getSite());
                updateFile(file);
            }
            aTM.endTask();
        }
    }

    /**
     * Replaces (overwrites) local site files from clone site.
     */
    protected void replaceFile(WebFile aLocalFile)
    {
        // Get CloneFile
        WebFile cloneFile = getCloneFile(aLocalFile.getPath(), true, aLocalFile.isDir());

        // Set new file bytes and save
        if (cloneFile.getExists()) {
            if (aLocalFile.isFile())
                aLocalFile.setBytes(cloneFile.getBytes());
            aLocalFile.save();
            aLocalFile.setModTimeSaved(cloneFile.getLastModTime());
            setStatus(aLocalFile, null);
        }

        // Otherwise delete LocalFile and CloneFile
        else {
            if (aLocalFile.getExists())
                aLocalFile.delete();
            setStatus(aLocalFile, null);
        }
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
        setStatus(aFile, null);
    }

    /**
     * Called when file saved.
     */
    public void fileSaved(WebFile aFile)
    {
        setStatus(aFile, null);
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
        WebFile file = sandboxSite.getFileForPath("/settings/remote");
        if (file == null)
            return null;

        // Get file text and return
        String text = file.getText();
        String urls = text.trim();
        return urls.length() > 0 ? urls : null;
    }

    /**
     * Sets the Remote URL string.
     */
    public static void setRemoteURLString(WebSite aSite, String aURLS)
    {
        // If already set, just return
        if (Objects.equals(aURLS, getRemoteURLString(aSite))) return;

        // Get remote settings file
        WebSite sandboxSite = aSite.getSandbox();
        WebFile file = sandboxSite.getFileForPath("/settings/remote");
        if (file == null)
            file = sandboxSite.createFileForPath("/settings/remote", false);

        // Save URL to remote settings file
        try {

            // If empty URL, delete file
            if (aURLS == null || aURLS.length() == 0) {
                if (file.getExists())
                    file.delete();
            }

            // Set file text and save
            else {
                file.setText(aURLS);
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
    public synchronized static VersionControl get(WebSite aSite)
    {
        VersionControl versionControl = (VersionControl) aSite.getProp(VersionControl.class.getName());
        if (versionControl == null)
            aSite.setProp(VersionControl.class.getName(), versionControl = create(aSite));
        return versionControl;
    }

    /**
     * Returns the VersionControl for a site.
     */
    public static VersionControl create(WebSite aSite)
    {
        //String urls = getRemoteURLString(aSite);
        //if (urls != null) urls = urls.toLowerCase();

        // Handle Git
        //if (urls != null && (urls.startsWith("git:") || urls.endsWith(".git"))) return new VersionControlGit(aSite);

        // Handle plain
        return new VersionControl(aSite);
    }
}