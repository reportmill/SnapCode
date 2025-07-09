/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.parse.*;
import snap.gfx.Font;
import snap.text.TextModelUtils;
import snap.util.CharSequenceUtils;
import snap.util.Prefs;

/**
 * Utility methods and support for Java text.
 */
public class JavaTextUtils {

    /**
     * Returns the default font used to display Java text.
     */
    public static Font getDefaultJavaFont()
    {
        double fontSize = getDefaultJavaFontSize();
        return Font.getCodeFontForSize(fontSize);
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
    public static boolean updateJFileForChange(JavaTextModel javaTextModel, JFile aJFile, TextModelUtils.CharsChange aCharsChange)
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

        // If enclosing block is constructor decl, just reparse all
        if (oldStmt.getParent() instanceof JConstrDecl)
            return false;

        // Parse new JStmtBlock (create empty one if there wasn't enough in block to create it)
        JavaParser javaParser = JavaParser.getShared();
        int charIndex = oldStmt.getStartCharIndex();

        // Parse new statement
        JStmtBlock newStmt = null;
        try { newStmt = (JStmtBlock) javaParser.parseStatementForJavaText(javaTextModel, charIndex); }
        catch (Exception ignore) { }

        // If statement parse failed (no statement or alternate end token), return reparse all
        if (newStmt == null || newStmt.getEndToken() != oldStmt.getEndToken())
            return false;

        // Replace old statement with new statement
        JNode stmtParent = oldStmt.getParent();
        if (stmtParent instanceof WithBlockStmt)
            ((WithBlockStmt) stmtParent).replaceBlock(newStmt);
        else System.err.println("JavaTextUtils.updateJFileForChange: Parent not WithBlockStmt: " + stmtParent);

        // Return success
        return true;
    }
}
