/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;

/**
 * A JNode for class body declarations: Members (fields, methods, constructors), Initializers, Annotations.
 * For JavaParseRule ClassBodyDecl.
 */
public class JBodyDecl extends JNode {

    /**
     * Returns whether this decl is static context.
     */
    public boolean isStatic()  { return true; }
}