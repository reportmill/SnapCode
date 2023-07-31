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
     * Returns the variable declaration if parent is variable declaration.
     */
    public JVarDecl getVarDecl()
    {
        JNode p = getParent();
        return p instanceof JVarDecl ? (JVarDecl) p : null;
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
        // Handle parent WithId (method/class/field decl id, method call/ref id, local var decl id, etc.)
        JNode parent = getParent();
        if (parent instanceof WithId) {
            JExprId parentId = ((WithId) parent).getId();
            if (this == parentId)
                return parent.getDecl();
        }

        // Handle parent is dot expression: Return dot expression decl
        if (parent instanceof JExprDot) {
            JExprDot dotExpr = (JExprDot) parent;
            if (dotExpr.getExpr() == this)
                return parent.getDecl();
        }

        // Forward to parents
        return getDeclForChildId(this);
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
        NodeError[] errors = NodeError.NO_ERRORS;

        // If Parent is WithId, just return
        JNode parentNode = getParent();
        if (parentNode instanceof WithId && ((WithId) parentNode).getId() == this)
            return errors;

        // Handle can't resolve id
        JavaDecl decl = getDecl();
        if (decl == null) {
            String name = getName();
            errors = NodeError.addError(errors, this, "Can't resolve id: " + name);
        }

        // Return
        return errors;
    }
}