/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import snap.util.ArrayUtils;

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
        return getDeclForChildExprIdNode(this);
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
            case Class: return "ClassId";
            case Constructor: return "ConstrId";

            case Field: {
                JavaField field = (JavaField) decl;
                if (field.isEnumConstant())
                    return "EnumId";
                return "FieldId";
            }

            case Method: return "MethodId";
            case Package: return "PackageId";
            case TypeVar: return "TypeVarId";

            // Handle VarDecl
            case VarDecl:
                JNode parentNode = getParent();
                if (parentNode instanceof JStmtBreak || parentNode instanceof JStmtLabeled)
                    return "LabelId";
                return "VariableId";
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

        // Handle can't resolve id
        JavaDecl decl = getDecl();
        if (decl == null) {
            String name = getName();
            NodeError error = new NodeError(this, "Can't resolve id: " + name);
            errors = ArrayUtils.add(errors, error);
        }

        // Return
        return errors;
    }
}