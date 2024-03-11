/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.parse.*;
import snap.gfx.Font;
import snap.parse.*;
import snap.text.TextBlockUtils;
import snap.util.CharSequenceUtils;
import snap.util.Prefs;

/**
 * Utility methods and support for JavaTextDoc.
 */
public class JavaTextDocUtils {

    // The recommended default font for code
    private static Font _defaultJavaFont;

    /**
     * Returns the default font used to display Java text.
     */
    public static Font getDefaultJavaFont()
    {
        if (_defaultJavaFont != null) return _defaultJavaFont;

        // Get font names and size
        String[] names = { "Monaco", "Consolas", "Lucida Console", "Courier" };
        double fontSize = getDefaultJavaFontSize();

        // Look for font
        for (String name : names) {
            _defaultJavaFont = new Font(name, fontSize);
            if (_defaultJavaFont.getFamily().startsWith(name))
                break;
        }

        // Return
        return _defaultJavaFont;
    }

    /**
     * Returns the default Java font size.
     */
    public static double getDefaultJavaFontSize()
    {
        double fontSize = Prefs.getDefaultPrefs().getDouble("JavaFontSize", 12);
        if (fontSize < 8) fontSize = 12;
        return fontSize;
    }

    /**
     * Sets the default Java font size.
     */
    public static void setDefaultJavaFontSize(double aSize)
    {
        Prefs.getDefaultPrefs().setValue("JavaFontSize", aSize);
    }

    /**
     * Updates JFile for given range change.
     */
    public static boolean updateJFileForChange(JavaTextDoc javaTextDoc, JFile aJFile, TextBlockUtils.CharsChange aCharsChange)
    {
        // If no JFile, just bail
        if (aJFile == null) return true;
        aJFile.setException(null);

        // Get CharsChange and charIndex
        CharSequence addChars = aCharsChange.getNewValue();
        CharSequence removeChars = aCharsChange.getOldValue();
        int startCharIndex = aCharsChange.getIndex();
        int endOldCharIndex = startCharIndex + (addChars != null ? 0 : removeChars.length());

        // If change is more than 50 chars or contains newline, just reparse all
        CharSequence changeChars = addChars != null ? addChars : removeChars;
        if (changeChars.length() > 50 || CharSequenceUtils.indexOfNewline(changeChars, 0) >= 0)
            return false;

        // Get outer statement enclosing range
        JNode jnode = aJFile.getNodeForCharIndex(startCharIndex);
        JStmtBlock oldStmt = jnode instanceof JStmtBlock ? (JStmtBlock) jnode : jnode.getParent(JStmtBlock.class);
        while (oldStmt != null && oldStmt.getEndCharIndex() < endOldCharIndex)
            oldStmt = oldStmt.getParent(JStmtBlock.class);

        // If enclosing statement not found, just reparse all
        if (oldStmt == null)
            return false;

        // Parse new JStmtBlock (create empty one if there wasn't enough in block to create it)
        JavaParser javaParser = JavaParser.getShared();
        int charIndex = oldStmt.getStartCharIndex();
        int lineIndex = oldStmt.getLineIndex();

        // Parse new statement
        JStmtBlock newStmt = null;
        try { newStmt = (JStmtBlock) javaParser.parseStatement(javaTextDoc, charIndex, lineIndex); }
        catch (Exception ignore) { }

        // If parse failed, return failed
        ParseToken endToken = newStmt != null ? newStmt.getEndToken() : null;
        if (endToken == null || !endToken.getPattern().equals(oldStmt.getEndToken().getPattern()))
            return false;

        // Replace old statement with new statement
        JNode stmtParent = oldStmt.getParent();
        if (stmtParent instanceof WithBlockStmt)
            ((WithBlockStmt) stmtParent).replaceBlock(newStmt);
        else System.err.println("JavaTextDocUtils.updateJFileForChange: Parent not WithBlockStmt: " + stmtParent);

        // Return success
        return true;
    }
}
