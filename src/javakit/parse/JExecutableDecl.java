/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaExecutable;
import javakit.resolver.JavaType;
import snap.util.ArrayUtils;
import java.util.stream.Stream;

/**
 * This JMemberDecl subclass represents Java executables (methods & constructors).
 */
public abstract class JExecutableDecl extends JMemberDecl implements WithBlockStmt, WithVarDecls, WithTypeVars {

    // The type/return-type
    protected JType  _type;

    // Type variables
    private JTypeVar[] _typeVars = new JTypeVar[0];

    // The formal parameters
    protected JVarDecl[] _params = new JVarDecl[0];

    // The array of thrown exception class name expressions
    protected JExpr[] _throwsList = new JExpr[0];

    // The statement Block
    protected JStmtBlock  _block;

    /**
     * Constructor.
     */
    public JExecutableDecl()
    {
        super();
    }

    /**
     * Returns the field type.
     */
    public JType getType()  { return _type; }

    /**
     * Sets the field type.
     */
    public void setType(JType aType)
    {
        replaceChild(_type, _type = aType);
    }

    /**
     * Returns the method type variables.
     */
    public JTypeVar[] getTypeVars()  { return _typeVars; }

    /**
     * Sets the method type variables.
     */
    public void setTypeVars(JTypeVar[] typeVars)
    {
        Stream.of(_typeVars).forEach(tvar -> removeChild(tvar));
        _typeVars = typeVars;
        Stream.of(_typeVars).forEach(tvar -> addChild(tvar));
    }

    /**
     * Returns the list of formal parameters.
     */
    public JVarDecl[] getParameters()  { return _params; }

    /**
     * Returns the list of formal parameters.
     */
    public void setParameters(JVarDecl[] varDecls)
    {
        Stream.of(_params).forEach(vdecl -> removeChild(vdecl));
        _params = varDecls;
        Stream.of(_params).forEach(vdecl -> addChild(vdecl));
    }

    /**
     * Returns the array of thrown exception class name expressions.
     */
    public JExpr[] getThrowsList()  { return _throwsList; }

    /**
     * Sets the array of thrown exception class name expressions.
     */
    public void setThrowsList(JExpr[] throwsList)
    {
        Stream.of(_throwsList).forEach(expr -> removeChild(expr));
        _throwsList = throwsList;
        Stream.of(_throwsList).forEach(expr -> addChild(expr));
    }

    /**
     * Returns the parameter names.
     */
    public String[] getParameterNames()
    {
        JVarDecl[] paramTypes = getParameters();
        return ArrayUtils.map(paramTypes, pdecl -> pdecl.getName(), String.class);
    }

    /**
     * Returns the block.
     */
    @Override
    public JStmtBlock getBlock()  { return _block; }

    /**
     * Sets the block.
     */
    @Override
    public void setBlock(JStmtBlock aBlock)
    {
        replaceChild(_block, _block = aBlock);
    }

    /**
     * WithVarDecls method: Just returns parameters list.
     */
    @Override
    public JVarDecl[] getVarDecls()  { return getParameters(); }

    /**
     * Returns the actual method or constructor.
     */
    public abstract JavaExecutable getExecutable();

    /**
     * Override to check formal parameters.
     */
    @Override
    protected JavaDecl getDeclForChildId(JExprId anExprId)
    {
        // Handle TypeVar name: return typevar decl
        String name = anExprId.getName();
        JTypeVar typeVar = getTypeVar(name);
        if (typeVar != null)
            return typeVar.getDecl();

        // Do normal version
        return super.getDeclForChildId(anExprId);
    }

    /**
     * Override - from old getDeclForChildNode(). Is it really needed ???
     */
    @Override
    protected JavaDecl getDeclForChildType(JType aJType)
    {
        // Handle TypeVar name: return typevar decl
        String name = aJType.getName();
        JTypeVar typeVar = getTypeVar(name);
        if (typeVar != null)
            return typeVar.getDecl();

        // Do normal version
        return super.getDeclForChildType(aJType);
    }

    /**
     * Returns array of parameter class types suitable to resolve method.
     */
    protected JavaType[] getParamClassTypesSafe()
    {
        // Declare array for return types
        JavaType[] paramTypes = new JavaType[_params.length];

        // Iterate over params to get types
        for (int i = 0, iMax = _params.length; i < iMax; i++) {
            JVarDecl varDecl = _params[i];

            // Get current type and TypeVar (if type is one)
            JType varDeclType = varDecl.getType();
            JTypeVar typeVar = getTypeVar(varDeclType.getName());

            // If type is TypeVar, set to TypeVar.BoundsType
            if (typeVar != null)
                paramTypes[i] = typeVar.getBoundsType();
            else paramTypes[i] = varDeclType.getBaseType();

            // If param type is null, just return (can happen if params are bogus (being edited))
            if (paramTypes[i] == null) return null;

            // If array, get array type instead
            int arrayCount = varDeclType.getArrayCount();
            for (int j = 0; j < arrayCount; j++)
                paramTypes[i] = paramTypes[i].getArrayType();
        }

        // Return
        return paramTypes;
    }

    /**
     * Override to return errors for ReturnValue, Parameters, ThrowsList and TypeVars.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        // Get errors for type
        JType returnType = getType();
        if (returnType == null)
            return NodeError.newErrorArray(getChild(0), "Missing return type");
        NodeError[] errors = NodeError.NO_ERRORS; //returnType.getErrors(); Add this back when method type var works (e.g. <T> T ...)

        // Get errors for params
        JVarDecl[] parameters = getParameters();
        errors = NodeError.addNodeErrorsForNodes(errors, parameters);

        // Get errors for throws list
        JExpr[] throwsList = getThrowsList();
        errors = NodeError.addNodeErrorsForNodes(errors, throwsList);

        // Get errors for type vars
        JTypeVar[] typeVars = getTypeVars();
        errors = NodeError.addNodeErrorsForNodes(errors, typeVars);

        // Return
        return errors;
    }
}