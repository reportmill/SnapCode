/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import snap.parse.ParseNode;

/**
 * A JExpr subclass for identifiers.
 */
public class JExprId extends JExpr {

    /**
     * Constructor.
     */
    public JExprId()
    {
        super();
    }

    /**
     * Constructor for given identifier name.
     */
    public JExprId(String aName)
    {
        setName(aName);
    }

    /**
     * Constructor for given identifier name.
     */
    public JExprId(ParseNode parseNode)
    {
        super();
        setName(parseNode.getId());
        setStartToken(parseNode.getStartToken());
        setEndToken(parseNode.getEndToken());
    }

    /**
     * Returns whether this is variable identifier.
     */
    public boolean isVarId()
    {
        JavaDecl decl = getDecl();
        return decl instanceof JavaLocalVar;
    }

    /**
     * Returns VarDecl node by looking in parent nodes (i.e., in scope) for this id.
     */
    public JVarDecl getVarDecl()
    {
        // Iterate up parents to look for a JVarDecl that defines this id name (Local vars, method params, class fields, etc.)
        for (JNode parentNode = getParent(); parentNode != null; parentNode = parentNode.getParent()) {

            // If parent is var decl and this is its id, return it
            if (parentNode instanceof JVarDecl varDecl && varDecl.getId() == this)
                return varDecl;

            // If parent has var decls, and has one that defines this id name, return it
            if (parentNode instanceof WithVarDecls) {
                JVarDecl varDecl = ((WithVarDecls) parentNode).getVarDeclForId(this);
                if (varDecl != null)
                    return varDecl;
            }
        }

        // Return not found
        return null;
    }

    /**
     * Returns whether this is package identifier.
     */
    public boolean isPackageName()
    {
        JavaDecl decl = getDecl();
        return decl instanceof JavaPackage;
    }

    /**
     * Returns the full package name for this package identifier.
     */
    public String getPackageName()
    {
        JavaDecl decl = getDecl();
        JavaPackage pkg = decl instanceof JavaPackage ? (JavaPackage) decl : null;
        return pkg != null ? pkg.getName() : null;
    }

    /**
     * Override to resolve id name from parents.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        // If parent is MethodRef, forward on - I don't like this, but don't want to confuse id with vars of same name
        JNode parent = getParent();
        if (parent instanceof JExprMethodRef methodRefExpr && methodRefExpr.getMethodId() == this)
            return getDeclForChildId(this);

        // Look for a master node, if this id is just part of another node or a var reference
        JNode declNode = getDeclNodeForId();
        if (declNode != null)
            return declNode.getDecl();

        // Forward to parents
        return getDeclForChildId(this);
    }

    /**
     * Override to resolve from DeclNode, if available.
     */
    @Override
    protected JavaType getEvalTypeImpl()
    {
        // Look for a master node, if this id is just part of another node or a var reference
        JNode declNode = getDeclNodeForId();
        if (declNode != null)
            return declNode.getEvalType();

        // Do normal version
        return super.getEvalTypeImpl();
    }

    /**
     * Returns the master node that declares this id, if this id is just a part of another node or a variable reference.
     */
    private JNode getDeclNodeForId()
    {
        // Handle parent WithId (method/class/field decl id, method call/ref id, local var decl id, etc.)
        JNode parent = getParent();
        if (parent instanceof WithId withId && withId.getId() == this)
            return parent;

        // Handle parent is dot expression: Return dot expression
        if (parent instanceof JExprDot dotExpr && dotExpr.getExpr() == this)
            return dotExpr;

        // Look for VarDecl that defines this id (if this id is var reference)
        JVarDecl varDecl = getVarDecl();
        if (varDecl != null)
            return varDecl;

        // Return not found
        return null;
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()
    {
        JavaDecl decl = getDecl();
        if (decl == null)
            return "UnknownId";

        // Handle decl types
        switch (decl.getType()) {

            // Handle Class, ParamType
            case Class:
            case ParamType: return "ClassId";

            // Handle Field (or enum)
            case Field:
                JavaField field = (JavaField) decl;
                return field.isEnumConstant() ? "EnumId" : "FieldId";

            // Handle Constructor, Method, Package, TypeVar
            case Constructor: return "ConstrId";
            case Method: return "MethodId";
            case Package: return "PackageId";
            case TypeVar: return "TypeVarId";

            // Handle VarDecl
            case VarDecl:
                JNode parentNode = getParent();
                if (parentNode instanceof JStmtBreak || parentNode instanceof JStmtLabeled)
                    return "LabelId";
                return "VarId";

            // Handle unknown
            default: return "UnknownId";
        }
    }

    /**
     * Override to provide errors for JExprId.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        // If Parent is WithId, just return
        JNode parentNode = getParent();
        if (parentNode instanceof WithId && ((WithId) parentNode).getId() == this)
            return NodeError.NO_ERRORS;

        // Handle can't resolve id
        JavaDecl decl = getDecl();
        if (decl == null)
            return NodeError.newErrorArray(this, "Can't resolve id: " + getName());

        // Return
        return NodeError.NO_ERRORS;
    }
}