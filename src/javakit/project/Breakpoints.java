/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import snap.props.PropObject;
import snap.web.WebFile;
import snap.web.WebSite;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * A class to manage project breakpoints.
 */
public class Breakpoints extends PropObject {

    // The Workspace
    private Workspace  _workspace;

    // The actual list of Breakpoints
    private List<Breakpoint>  _breakpointsList = new ArrayList<>();

    // An Empty array of BuildIssues
    public static final Breakpoint[] NO_BREAKPOINTS = new Breakpoint[0];

    // Constant for Items
    public static final String ITEMS_PROP = "Items";

    /**
     * Constructor.
     */
    public Breakpoints(Workspace workspace)
    {
        super();
        _workspace = workspace;

        // Read from file and add to this list
        //List<Breakpoint> breakpoints = readFile();
        //for (Breakpoint bp : breakpoints)
        //    super.add(size(), bp);
    }

    /**
     * Returns the number of breakpoints.
     */
    public int size()  { return _breakpointsList.size(); }

    /**
     * Adds a breakpoint.
     */
    public void add(int anIndex, Breakpoint aBP)
    {
        _breakpointsList.add(anIndex, aBP);
        writeFile();
        firePropChange(ITEMS_PROP, null, aBP, anIndex);
    }

    /**
     * Removes a breakpoint.
     */
    public Breakpoint remove(int anIndex)
    {
        Breakpoint breakpoint = _breakpointsList.remove(anIndex);
        writeFile();
        firePropChange(ITEMS_PROP, breakpoint, null, anIndex);
        return breakpoint;
    }

    /**
     * Removes a breakpoint.
     */
    public void remove(Breakpoint breakpoint)
    {
        int index = _breakpointsList.indexOf(breakpoint);
        if (index >= 0)
            remove(index);
    }

    /**
     * Adds a Breakpoint to file at line.
     */
    public void addBreakpointForFile(WebFile aFile, int aLine)
    {
        Breakpoint breakpoint = new Breakpoint(aFile, aLine);
        int index = Collections.binarySearch(_breakpointsList, breakpoint);
        if (index < 0) {
            index = -index - 1;
            add(index, breakpoint);
        }
    }

    /**
     * Returns the breakpoints for a given file.
     */
    public Breakpoint[] getBreakpointsForFile(WebFile aFile)
    {
        // If no BreakPoints, just return
        if (size() == 0) return NO_BREAKPOINTS;

        // Filter to get breakpoints for given file
        Breakpoint[] breakpoints = _breakpointsList.stream().filter(bp -> bp.getFile() == aFile).toArray(size -> new Breakpoint[size]);
        return breakpoints;
    }

    /**
     * Returns an array of breakpoints.
     */
    public Breakpoint[] getArray()
    {
        return _breakpointsList.toArray(new Breakpoint[0]);
    }

    /**
     * Clears the array of breakpoints.
     */
    public void clear()
    {
        for (int i = size() - 1; i >= 0; i--)
            remove(i);
    }

    /**
     * Reads breakpoints from file.
     */
    protected List<Breakpoint> readFile()
    {
        // Get breakpoint file and text
        WebFile breakpointFile = getFile();
        if (!breakpointFile.getExists())
            return Collections.EMPTY_LIST;

        // Get vars
        String text = breakpointFile.getText();
        Scanner scanner = new Scanner(text);
        List<Breakpoint> breakpointsList = new ArrayList<>();

        // Iterate over text
        while (scanner.hasNext()) {

            // Get Breakpoint Type, Path, LineNum
            String type = scanner.next();
            String path = scanner.next();
            int lineNum = scanner.nextInt();

            // Get Breakpoint source file (just continue if file no longer found in Workspace)
            Project rootProj = _workspace.getRootProject();
            WebFile sourceFile = rootProj.getFile(path); // Should be checking whole Workspace
            if (sourceFile == null)
                continue;

            // Create/add new breakpoint
            Breakpoint breakpoint = new Breakpoint(sourceFile, lineNum);
            breakpointsList.add(breakpoint);
        }

        // Return
        return breakpointsList;
    }

    /**
     * Writes breakpoints to file.
     */
    public void writeFile()
    {
        // Create file text
        StringBuilder sb = new StringBuilder();
        for (Breakpoint breakpoint : _breakpointsList) {
            sb.append(breakpoint.getType()).append(' ');
            sb.append(breakpoint.getFilePath()).append(' ');
            sb.append(breakpoint.getLine()).append('\n');
        }

        // Get file, set text and save
        WebFile file = getFile();
        file.setText(sb.toString());
        try { file.save(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Returns the breakpoint file.
     */
    protected WebFile getFile()
    {
        Project rootProj = _workspace.getRootProject();
        WebSite projSite = rootProj.getSite();
        WebSite sandboxSite = projSite.getSandbox();

        // Get Sandbox breakpoints file
        WebFile breakpointsFile = sandboxSite.getFileForPath("/settings/breakpoints");
        if (breakpointsFile == null)
            breakpointsFile = sandboxSite.createFileForPath("/settings/breakpoints", false);

        // Return
        return breakpointsFile;
    }
}