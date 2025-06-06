/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.parse.JNode;
import javakit.parse.NodeError;
import snap.web.WebFile;

/**
 * This class represents a build error (or warning) for a file.
 */
public class BuildIssue implements Comparable<BuildIssue> {

    // The file that has the error
    private WebFile  _file;

    // The status
    private Kind  _kind;

    // The error text
    private String  _text;

    // The line and column index
    private int  _line, _column;

    // The character start and end
    private int  _start, _end;

    // Constants for kind of issue
    public enum Kind { Error, Warning, Note }

    /**
     * Constructor.
     */
    public BuildIssue()
    {
        super();
    }

    /**
     * Creates a new error.
     */
    public BuildIssue init(WebFile aFile, Kind aKind, String theText, int aLine, int aColumn, int aStart, int anEnd)
    {
        _file = aFile;
        _kind = aKind;
        _text = theText;
        _line = aLine;
        _column = aColumn;
        _start = aStart;
        _end = Math.max(anEnd, _start);
        return this;
    }

    /**
     * Returns the file.
     */
    public WebFile getFile()  { return _file; }

    /**
     * Returns the kind.
     */
    public Kind getKind()  { return _kind; }

    /**
     * Returns whether issue is error.
     */
    public boolean isError()  { return _kind == Kind.Error; }

    /**
     * Returns the error text.
     */
    public String getText()  { return _text; }

    /**
     * Returns the line index.
     */
    public int getLine()  { return _line; }

    /**
     * Returns the column index.
     */
    public int getColumn()  { return _column; }

    /**
     * Returns the start char index.
     */
    public int getStart()  { return _start; }

    /**
     * Sets the start char index.
     */
    public void setStart(int aStart)
    {
        _start = aStart;
    }

    /**
     * Returns the end char index.
     */
    public int getEnd()  { return _end; }

    /**
     * Returns the end char index.
     */
    public void setEnd(int anEnd)
    {
        _end = anEnd;
    }

    /**
     * Returns the line number (convenience).
     */
    public int getLineNumber()  { return _line + 1; }

    /**
     * Standard compareTo implementation.
     */
    public int compareTo(BuildIssue aBI)
    {
        // Sort by Kind (Errors first)
        Kind k1 = getKind();
        Kind k2 = aBI.getKind();
        if (k1 != k2)
            return k1.ordinal() - k2.ordinal();

        // Sort by file name
        WebFile file1 = getFile();
        WebFile file2 = aBI.getFile();
        int comp = file1.compareTo(file2);
        if (comp != 0)
            return comp;

        // Sort by line number
        int line1 = getLine();
        int line2 = aBI.getLine();
        if (line1 != line2)
            return line1 - line2;

        // Sort by start char index
        int startCharIndex1 = getStart();
        int startCharIndex2 = aBI.getStart();
        if (startCharIndex1 != startCharIndex2)
            return startCharIndex1 - startCharIndex2;

        // Sort by text (really shouldn't get here)
        String text1 = getText();
        String text2 = aBI.getText();;
        return text1.compareTo(text2);
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        String filePath = getFile().getPath();
        return String.format("%s:%d: %s", filePath, getLine() + 1, getText());
    }

    /**
     * Creates a BuildIssue for given NodeError.
     */
    public static BuildIssue createIssueForNodeError(NodeError aNodeError, WebFile aSourceFile)
    {
        // Get info
        JNode node = aNodeError.getNode();
        String errorStr = aNodeError.getString();
        int lineIndex = node.getLineIndex();
        int startCharIndex = node.getStartCharIndex();
        int endCharIndex = node.getEndCharIndex();

        // Create and return BuildIssue
        BuildIssue buildIssue = new BuildIssue();
        buildIssue.init(aSourceFile, BuildIssue.Kind.Error, errorStr, lineIndex,0, startCharIndex, endCharIndex);
        return buildIssue;
    }
}