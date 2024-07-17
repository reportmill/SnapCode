/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import java.util.List;

/**
 * A Java member for a method declaration.
 */
public class JMethodDecl extends JExecutableDecl {

    // The return type
    protected JType _returnType;

    // The method
    private JavaMethod _method;

    /**
     * Constructor.
     */
    public JMethodDecl()
    {
        super();
    }

    /**
     * Returns the return type.
     */
    public JType getReturnType()  { return _returnType; }

    /**
     * Sets the return type.
     */
    public void setReturnType(JType aType)
    {
        replaceChild(_returnType, _returnType = aType);
    }

    /**
     * Returns the method.
     */
    public JavaMethod getMethod()
    {
        if (_method != null) return _method;
        return _method = getMethodImpl();
    }

    /**
     * Returns the method.
     */
    private JavaMethod getMethodImpl()
    {
        // Get method name
        String name = getName();
        if (name == null)
            return null;

        // Get param classes
        JavaClass[] paramTypes = getParameterClasses();

        // Get parent JClassDecl and JavaDecl
        JClassDecl enclosingClassDecl = getEnclosingClassDecl();
        JavaClass javaClass = enclosingClassDecl != null ? enclosingClassDecl.getJavaClass() : null;
        if (javaClass == null)
            return null;

        // Get method for name and param types - just return if found
        JavaMethod javaMethod = javaClass.getDeclaredMethodForNameAndClasses(name, paramTypes);
        if (javaMethod != null)
            return javaMethod;

        // Otherwise just create and return method
        return JavaClassUpdaterDecl.createMethodForDecl(this);
    }

    /**
     * Override to get decl from method.
     */
    @Override
    protected JavaMethod getDeclImpl()  { return getMethod(); }

    /**
     * Override to return method.
     */
    @Override
    public JavaExecutable getExecutable()  { return getMethod(); }

    /**
     * Returns the part name.
     */
    public String getNodeString()  { return "MethodDecl"; }

    /**
     * Override to return errors for ReturnValue, Parameters, ThrowsList and TypeVars.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        // Get errors for type
        JType returnType = getReturnType();
        if (returnType == null) {
            JNode errorNode = getChildCount() > 0 ? getChild(0) : this; // Typing "List<" can cause method decl with no children
            return NodeError.newErrorArray(errorNode, "Missing return type");
        }

        // Check for missing return statement
        /*if (!returnType.getName().equals("void")) {
            if (isMissingReturnStatement(this)) {
                JStmt lastStmt = getLastStatement(this);
                JNode errorNode = lastStmt != null ? lastStmt : this;
                return NodeError.newErrorArray(errorNode, "Missing return statement");
            }
        }*/

        // Do normal version
        return super.getErrorsImpl();
    }

    /**
     * Returns whether last statement is return or throw.
     */
    private static boolean isMissingReturnStatement(JNode node)
    {
        JStmt lastStmt = getLastStatement(node);
        return !(lastStmt instanceof JStmtReturn) && !(lastStmt instanceof JStmtThrow);
    }

    /**
     * Returns the last statement.
     */
    private static JStmt getLastStatement(JNode aNode)
    {
        // Handle Try statement
        if (aNode instanceof JStmtTry) {
            JStmtTry tryStmt = (JStmtTry) aNode;
            JStmtBlock tryBlock = tryStmt.getBlock();
            if (isMissingReturnStatement(tryBlock))
                return null;
            JStmtTryCatch[] catchBlocks = tryStmt.getCatchBlocks();
            for (JStmtTryCatch catchBlock : catchBlocks) {
                if (isMissingReturnStatement(catchBlock))
                    return null;
            }
            JStmtTryCatch lastCatchBlock = catchBlocks.length > 0 ? catchBlocks[0] : null;
            return getLastStatement(lastCatchBlock);
        }

        // Handle WithBlockStmt
        if (aNode instanceof WithBlockStmt) {
            JStmt[] blockStatements = ((WithBlockStmt) aNode).getBlockStatements();
            JStmt lastStmt = blockStatements.length > 0 ? blockStatements[blockStatements.length - 1] : null;
            return getLastStatement(lastStmt);
        }

        // Handle WithStmts
        if (aNode instanceof WithStmts) {
            List<JStmt> statements = ((WithStmts) aNode).getStatements();
            JStmt lastStmt = !statements.isEmpty() ? statements.get(statements.size() - 1) : null;
            return getLastStatement(lastStmt);
        }

        // Handle Switch statement
        if (aNode instanceof JStmtSwitch) {
            JStmtSwitch switchStmt = (JStmtSwitch) aNode;
            List<JStmtSwitchCase> caseStmts = switchStmt.getSwitchCases();
            for (JStmtSwitchCase caseStmt : caseStmts) {
                if (isMissingReturnStatement(caseStmt))
                    return null;
            }
            return getLastStatement(switchStmt.getDefaultCase());
        }

        // Handle any other kind of statement
        return aNode instanceof JStmt ? (JStmt) aNode : null;
    }
}