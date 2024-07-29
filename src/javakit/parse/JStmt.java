/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.util.ListUtils;
import java.util.List;

/**
 * The Java part to handle Java statements.
 */
public class JStmt extends JNode {

    /**
     * Returns the next statement.
     */
    public JStmt getNextStmt()
    {
        if (_parent instanceof WithStmts) {
            List<JStmt> stmts = ((WithStmts) _parent).getStatements();
            int nextIndex = ListUtils.findMatchIndex(stmts, stmt -> stmt == this) + 1;
            return nextIndex > 0  && nextIndex < stmts.size() ? stmts.get(nextIndex) : null;
        }

        return null;
    }
}