/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import snap.props.PropObject;
import snap.web.WebFile;
import java.util.*;
import java.util.stream.Stream;

/**
 * A class to manage a list of BuildIssue.
 */
public class BuildIssues extends PropObject {

    // The actual list of BuildIssuess
    private List<BuildIssue>  _issues = new ArrayList<>();

    // The total count of errors and warnings
    private int  _errorCount;

    // The total count of errors and warnings
    private int  _warningCount;

    // A map to track BuildIssues by WebFile
    private Map<WebFile,List<BuildIssue>>  _fileIssues = new Hashtable<>();

    // An Empty array of BuildIssues
    public static final BuildIssue[] NO_ISSUES = new BuildIssue[0];

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
    public int size()  { return _issues.size(); }

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
    public BuildIssue[] getIssues()
    {
        return _issues.toArray(new BuildIssue[0]);
    }

    /**
     * Adds a BuildIssue at sorted index.
     */
    public boolean add(BuildIssue aBI)
    {
        // Get insertion index (just return if already in list)
        int index = -Collections.binarySearch(_issues, aBI) - 1;
        if (index < 0)
            return false;

        // Add to file list
        WebFile buildIssueFile = aBI.getFile();
        List<BuildIssue> buildIssuesForFile = _fileIssues.computeIfAbsent(buildIssueFile, k -> new ArrayList<>());
        buildIssuesForFile.add(aBI);

        // Update ErrorCount/WarningCount
        if (aBI.isError())
            _errorCount++;
        else _warningCount++;

        // Add issue
        _issues.add(index, aBI);

        // Fire prop change
        firePropChange(ITEMS_PROP, null, aBI, index);

        // Return
        return true;
    }

    /**
     * Removes a BuildIssue.
     */
    public void remove(BuildIssue aBI)
    {
        // Remove from file
        WebFile buildIssueFile = aBI.getFile();
        List<BuildIssue> buildIssues = _fileIssues.get(buildIssueFile);
        if (buildIssues != null) {
            buildIssues.remove(aBI);
            if (buildIssues.size() == 0)
                _fileIssues.remove(buildIssueFile);
        }

        // Update ErrorCount/WarningCount
        if (aBI.isError())
            _errorCount--;
        else _warningCount--;

        // Remove from master list
        int index = _issues.indexOf(aBI);
        _issues.remove(aBI);

        // Fire prop change
        firePropChange(ITEMS_PROP, aBI, null, index);
    }

    /**
     * Returns an array of build issues.
     */
    public BuildIssue[] getArray()
    {
        return _issues.toArray(new BuildIssue[0]);
    }

    /**
     * Override to clear FileIssues cache.
     */
    public void clear()
    {
        _fileIssues.clear();

        BuildIssue[] issues = getIssues();
        for (BuildIssue issue : issues)
            remove(issue);
    }

    /**
     * Returns the BuildIssues for a given file.
     */
    public BuildIssue[] getIssuesForFile(WebFile aFile)
    {
        List<BuildIssue> buildIssues;

        // Handle file: Just load from map
        if (aFile.isFile())
            buildIssues = _fileIssues.get(aFile);

            // Handle directory: aggregate directory files
        else {
            buildIssues = new ArrayList<>();
            for (WebFile file : aFile.getFiles())
                Collections.addAll(buildIssues, getIssuesForFile(file));
        }

        // Return list
        return buildIssues != null ? buildIssues.toArray(new BuildIssue[0]) : NO_ISSUES;
    }

    /**
     * Removes the build issues for a file.
     */
    public void removeIssuesForFile(WebFile aFile)
    {
        BuildIssue[] issues = getIssuesForFile(aFile);
        for (BuildIssue i : issues) remove(i);
    }

    /**
     * Returns the build status for a file.
     */
    public BuildIssue.Kind getBuildStatusForFile(WebFile aFile)
    {
        // Handle file: Get build issues and return worst kind
        if (aFile.isFile()) {

            // Get build issues - just return null if none
            BuildIssue[] buildIssues = getIssuesForFile(aFile);
            if (buildIssues.length == 0)
                return null;

            // If any issue is error, return error
            boolean containsError = Stream.of(buildIssues).anyMatch(issue -> issue.getKind() == BuildIssue.Kind.Error);
            if (containsError)
                    return BuildIssue.Kind.Error;

            // Return warning
            return BuildIssue.Kind.Warning;
        }

        // Handle directory
        WebFile[] dirFiles = aFile.getFiles();
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
        if (aFile.getType().length() > 0)
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