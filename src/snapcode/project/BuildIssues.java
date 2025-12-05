/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.props.PropObject;
import snap.web.WebFile;
import java.util.*;

/**
 * A class to manage a list of BuildIssue.
 */
public class BuildIssues extends PropObject {

    // The actual list of BuildIssuess
    private List<BuildIssue> _buildIssues = new ArrayList<>();

    // The total count of errors and warnings
    private int  _errorCount;

    // The total count of errors and warnings
    private int  _warningCount;

    // A map to track BuildIssues by WebFile
    private Map<WebFile,List<BuildIssue>>  _fileIssues = new Hashtable<>();

    // Constant for Items
    public static final String ITEMS_PROP = "Items";

    /**
     * Constructor.
     */
    public BuildIssues()
    {
        super();
    }

    /**
     * Returns the number of breakpoints.
     */
    public int size()  { return _buildIssues.size(); }

    /**
     * Returns the number of errors currently tracked.
     */
    public int getErrorCount()  { return _errorCount; }

    /**
     * Returns the number of warnings currently tracked.
     */
    public int getWarningCount()  { return _warningCount; }

    /**
     * Returns an array of the currently tracked issues.
     */
    public List<BuildIssue> getBuildIssues()  { return List.copyOf(_buildIssues); }

    /**
     * Adds a BuildIssue at sorted index.
     */
    public void addBuildIssue(BuildIssue aBuildIssue)
    {
        // Get insertion index (just return if already in list)
        int index = -Collections.binarySearch(_buildIssues, aBuildIssue) - 1;
        if (index < 0)
            return;

        // Add to file list
        WebFile buildIssueFile = aBuildIssue.getFile();
        List<BuildIssue> buildIssuesForFile = _fileIssues.computeIfAbsent(buildIssueFile, k -> new ArrayList<>());
        buildIssuesForFile.add(aBuildIssue);

        // Update ErrorCount/WarningCount
        if (aBuildIssue.isError())
            _errorCount++;
        else _warningCount++;

        // Add issue
        _buildIssues.add(index, aBuildIssue);

        // Fire prop change
        firePropChange(ITEMS_PROP, null, aBuildIssue, index);
    }

    /**
     * Removes a BuildIssue.
     */
    public void removeBuildIssue(BuildIssue aBuildIssue)
    {
        // Remove from file
        WebFile buildIssueFile = aBuildIssue.getFile();
        List<BuildIssue> buildIssues = _fileIssues.get(buildIssueFile);
        if (buildIssues != null) {
            buildIssues.remove(aBuildIssue);
            if (buildIssues.isEmpty())
                _fileIssues.remove(buildIssueFile);
        }

        // Update ErrorCount/WarningCount
        if (aBuildIssue.isError())
            _errorCount--;
        else _warningCount--;

        // Remove from master list
        int index = _buildIssues.indexOf(aBuildIssue);
        _buildIssues.remove(aBuildIssue);

        // Fire prop change
        firePropChange(ITEMS_PROP, aBuildIssue, null, index);
    }

    /**
     * Override to clear FileIssues cache.
     */
    public void clear()
    {
        _fileIssues.clear();
        getBuildIssues().forEach(this::removeBuildIssue);
    }

    /**
     * Returns the BuildIssues for a given file.
     */
    public List<BuildIssue> getIssuesForFile(WebFile aFile)
    {
        // Handle file: Just load from map
        if (aFile.isFile()) {
            List<BuildIssue> buildIssues = _fileIssues.get(aFile);
            return buildIssues != null ? List.copyOf(buildIssues) : Collections.emptyList();
        }

        // Handle directory: aggregate directory files
        List<BuildIssue> buildIssues = new ArrayList<>();
        for (WebFile file : aFile.getFiles())
            buildIssues.addAll(getIssuesForFile(file));
        return buildIssues;
    }

    /**
     * Returns the build status for a file.
     */
    public BuildIssue.Kind getBuildStatusForFile(WebFile aFile)
    {
        // Handle file: Get build issues and return worst kind
        if (aFile.isFile()) {

            // Get build issues - just return null if none
            List<BuildIssue> buildIssues = getIssuesForFile(aFile);
            if (buildIssues.isEmpty())
                return null;

            // If any issue is error, return error
            boolean containsError = buildIssues.stream().anyMatch(issue -> issue.getKind() == BuildIssue.Kind.Error);
            if (containsError)
                return BuildIssue.Kind.Error;

            // Return warning
            return BuildIssue.Kind.Warning;
        }

        // Handle directory
        List<WebFile> dirFiles = aFile.getFiles();
        boolean isPackage = isPackage(aFile);
        BuildIssue.Kind buildStatus = null;

        // Iterate over directory files: get worst status of children (if package, don't recurse)
        for (WebFile childFile : dirFiles) {

            // Skip packages
            if (childFile.isDir() && isPackage)
                continue;

            // Get status for child file
            BuildIssue.Kind childStatus = getBuildStatusForFile(childFile);
            if (childStatus != null)
                buildStatus = childStatus;
            if (childStatus == BuildIssue.Kind.Error)
                return buildStatus;
        }

        // Return
        return buildStatus;
    }

    /**
     * Returns whether given file is a package.
     */
    private static boolean isPackage(WebFile aFile)
    {
        // If file name has extension, return false
        if (!aFile.getFileType().isEmpty())
            return false;

        // If file in Project.SourceDir
        Project proj = Project.getProjectForFile(aFile);
        WebFile sourceDir = proj.getSourceDir();
        if (sourceDir.containsFile(aFile))
            return true;

        // Return not package
        return false;
    }
}