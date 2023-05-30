/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A JStatement for Class declaration.
 */
public class JStmtClassDecl extends JStmt {
    // The Class declaration
    JClassDecl _classDecl;

    /**
     * Returns the Class declaration.
     */
    public JClassDecl getClassDecl()
    {
        return _classDecl;
    }

    /**
     * Sets the Class declaration.
     */
    public void setClassDecl(JClassDecl aCD)
    {
        replaceChild(_classDecl, _classDecl = aCD);
    }

}