/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaType;
import java.util.ArrayList;
import java.util.List;

/**
 * A Java member for MethodDeclaration.
 */
public class JExecutableDecl extends JMemberDecl implements WithBlockStmt, WithVarDecls {

    // The type/return-type
    protected JType  _type;

    // Type variables
    protected List<JTypeVar>  _typeVars;

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
    public List<JTypeVar> getTypeVars()  { return _typeVars; }

    /**
     * Sets the method type variables.
     */
    public void setTypeVars(List<JTypeVar> theTVs)
    {
        if (_typeVars != null)
            for (JTypeVar tvar : _typeVars)
                removeChild(tvar);

        _typeVars = theTVs;

        if (_typeVars != null)
            for (JTypeVar tvar : _typeVars)
                addChild(tvar, -1);
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
        addChild(aVarDecl, -1);
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
            for (JExpr t : _throwsNameList)
                removeChild(t);

        _throwsNameList = theThrows;

        if (_throwsNameList != null)
            for (JExpr t : _throwsNameList)
                addChild(t, -1);
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
     * Override to check formal parameters.
     */
    @Override
    protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
    {
        // If node is method name, return method decl
        if (anExprId == _id)
            return getDecl();

        // Handle parameter name id: return param decl
        String name = anExprId.getName();
        JVarDecl param = getVarDeclForName(name);
        if (param != null)
            return param.getDecl();

        // Handle TypeVar name: return typevar decl
        JTypeVar typeVar = getTypeVar(name);
        if (typeVar != null)
            return typeVar.getDecl();

        // Do normal version (search class)
        JavaDecl superValue = super.getDeclForChildExprIdNode(anExprId);
        if (superValue != null)
            return superValue;

        // REPL hack - Get/search initializers before this method
        return getDeclForChildExprIdNodeReplHack(anExprId);
    }

    /**
     * REPL hack - Get/search initializers before this method for unresolved ids.
     */
    protected JavaDecl getDeclForChildExprIdNodeReplHack(JExprId anExprId)
    {
        // Get class initializers
        JClassDecl classDecl = getEnclosingClassDecl();
        JInitializerDecl[] initDecls = classDecl.getInitDecls();

        // Search initializers before this method and return node decl if found
        for (JInitializerDecl initDecl : initDecls) {
            if (initDecl.getStartCharIndex() < getStartCharIndex()) {
                JStmtBlock blockStmt = initDecl.getBlock();
                JavaDecl nodeDecl = blockStmt.getDeclForChildExprIdNode(anExprId);
                if (nodeDecl != null)
                    return nodeDecl;
            }
            else break;
        }

        // Return not found
        return null;
    }

    /**
     * Override - from old getDeclForChildNode(). Is it really needed ???
     */
    @Override
    protected JavaDecl getDeclForChildTypeNode(JType aJType)
    {
        // Handle TypeVar name: return typevar decl
        String name = aJType.getName();
        JTypeVar typeVar = getTypeVar(name);
        if (typeVar != null)
            return typeVar.getDecl();

        // Do normal version
        return super.getDeclForChildTypeNode(aJType);
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
            else paramTypes[i] = varDeclType.getBaseDecl();

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