/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.runner;
import javakit.parse.*;
import snap.util.ArrayUtils;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods and support for JavaShell.
 */
public class JavaShellUtils {

    /**
     * Returns the main statements for a JFile.
     */
    public static JStmt[] getMainStatements(JFile jfile)
    {
        // Handle Jepl file special
        if (jfile.isRepl())
            return getMainStatementsRepl(jfile);

        // Get main method
        JClassDecl classDecl = jfile.getClassDecl();
        JMethodDecl mainMethod = classDecl.getMethodDeclForNameAndTypes("main", null);
        if (mainMethod == null) {
            System.err.println("JavaShell.getMainStatements: No main method for file: " + jfile.getName());
            return new JStmt[0];
        }

        // Return main method block statements
        return mainMethod.getBlockStatements();
    }

    /**
     * Returns the main statements for a Repl JFile.
     */
    private static JStmt[] getMainStatementsRepl(JFile jfile)
    {
        // Get JFile, ClassDecl (just return if not found)
        JClassDecl classDecl = jfile.getClassDecl();
        if (classDecl == null)
            return new JStmt[0];

        // Get initializers
        JInitializerDecl[] initDecls = classDecl.getInitDecls();
        JStmt[] stmtsAll = new JStmt[0];

        // Iterate over initializers and add statements
        for (JInitializerDecl initDecl : initDecls) {
            JStmt[] stmts = getStatementsForNode(initDecl);
            stmtsAll = ArrayUtils.addAll(stmtsAll, stmts);
        }

        // Return
        return stmtsAll;
    }

    /**
     * Utility method to returns an array of statements in given node.
     */
    private static JStmt[] getStatementsForNode(JNode aJNode)
    {
        List<JStmt> stmtsList = new ArrayList<>();
        findStatementsForNode(aJNode, stmtsList);
        return stmtsList.toArray(new JStmt[0]);
    }

    /**
     * Utility helper method: Recursively finds all statements in node and adds to given list.
     */
    private static void findStatementsForNode(JNode aJNode, List<JStmt> stmtsList)
    {
        // Handle statement node (but not block), get line index and set in array
        if (aJNode instanceof JStmt && !(aJNode instanceof JStmtBlock)) {
            stmtsList.add((JStmt) aJNode);
            return;
        }

        // Handle any node: Iterate over children and recurse
        List<JNode> children = aJNode.getChildren();
        for (JNode child : children)
            findStatementsForNode(child, stmtsList);
    }

    /**
     * A PrintStream to stand in for System.out and System.err.
     */
    protected static class ProxyPrintStream extends PrintStream {

        // The JavaShell
        private JavaShell  _javaShell;

        // Whether is standard err
        private boolean  _stdErr;

        /**
         * Constructor.
         */
        public ProxyPrintStream(JavaShell javaShell, PrintStream printStream)
        {
            super(printStream);
            _javaShell = javaShell;
            _stdErr = printStream == System.err;
        }

        /**
         * Override to send to ScanView.
         */
        public void write(int b)
        {
            // Do normal version
            super.write(b);

            // Write char to console
            String str = String.valueOf(Character.valueOf((char) b));
            _javaShell.appendConsoleOutput(str, _stdErr);
        }

        /**
         * Override to send to ScanView.
         */
        public void write(byte[] buf, int off, int len)
        {
            // Do normal version
            super.write(buf, off, len);

            // Write buff to console
            String str = new String(buf, off, len);
            _javaShell.appendConsoleOutput(str, _stdErr);
        }
    }
}
