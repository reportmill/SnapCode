/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaExecutable;
import javakit.resolver.JavaType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * A Java member for MethodDeclaration.
 */
public abstract class JExecutableDecl extends JMemberDecl implements WithBlockStmt, WithVarDecls, WithTypeVars {

    // The type/return-type
    protected JType  _type;

    // Type variables
    private JTypeVar[] _typeVars;

    // The formal parameters
    protected List<JVarDecl>  _params = new ArrayList<>();

    // The throws names list
    protected List<JExpr>  _throwsNameList = new ArrayList<>();

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
    public void setTypeVars(JTypeVar[] theTVs)
    {
        if (_typeVars != null)
            Stream.of(_typeVars).forEach(tvar -> removeChild(tvar));

        _typeVars = theTVs;

        if (_typeVars != null)
            Stream.of(_typeVars).forEach(tvar -> addChild(tvar));
    }

    /**
     * Returns the list of formal parameters.
     */
    public List<JVarDecl> getParameters()  { return _params; }

    /**
     * Returns the list of formal parameters.
     */
    public void addParam(JVarDecl aVarDecl)
    {
        if (aVarDecl == null) {
            System.err.println("JExecutableDecl.addParam: Add null param!");
            return;
        }
        _params.add(aVarDecl);
        addChild(aVarDecl);
    }

    /**
     * Returns the throws list.
     */
    public List<JExpr> getThrowsList()  { return _throwsNameList; }

    /**
     * Sets the throws list.
     */
    public void setThrowsList(List<JExpr> theThrows)
    {
        if (_throwsNameList != null)
            _throwsNameList.forEach(expr -> removeChild(expr));

        _throwsNameList = theThrows;

        if (_throwsNameList != null)
            _throwsNameList.forEach(expr -> addChild(expr));
    }

    /**
     * Returns the block.
     */
    public JStmtBlock getBlock()  { return _block; }

    /**
     * Sets the block.
     */
    public void setBlock(JStmtBlock aBlock)
    {
        replaceChild(_block, _block = aBlock);
    }

    /**
     * WithVarDecls method: Just returns parameters list.
     */
    @Override
    public List<JVarDecl> getVarDecls()
    {
        return getParameters();
    }

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
        JavaType[] paramTypes = new JavaType[_params.size()];

        // Iterate over params to get types
        for (int i = 0, iMax = _params.size(); i < iMax; i++) {
            JVarDecl varDecl = _params.get(i);

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
}