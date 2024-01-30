/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import javakit.resolver.*;

/**
 * A JExpr to represent lambda expressions.
 */
public class JExprLambda extends JExprLambdaBase implements WithVarDecls, WithBlockStmt {

    // The parameters
    protected List<JVarDecl>  _params = new ArrayList<>();

    // The expression, if lambda has expression
    protected JExpr  _expr;

    // The statement Block, if lambda has block
    protected JStmtBlock  _block;

    // The parameter types
    private JavaType[] _parameterTypes;

    /**
     * Constructor.
     */
    public JExprLambda()
    {
        super();
    }

    /**
     * Returns the number of parameters.
     */
    public int getParameterCount()  { return _params.size(); }

    /**
     * Returns the list of formal parameters.
     */
    public List<JVarDecl> getParameters()  { return _params; }

    /**
     * Adds a formal parameter.
     */
    public void addParameter(JVarDecl aVarDecl)
    {
        _params.add(aVarDecl);
        addChild(aVarDecl);
    }

    /**
     * Returns the expression, if lambda has expression.
     */
    public JExpr getExpr()  { return _expr; }

    /**
     * Sets the expression.
     */
    public void setExpr(JExpr anExpr)
    {
        replaceChild(_expr, _expr = anExpr);
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
     * Returns the list of parameter classes.
     */
    public JavaType[] getParameterTypes()
    {
        // If already set, just return
        if (_parameterTypes != null) return _parameterTypes;

        // Create array
        int parameterCount = getParameterCount();
        JavaType[] paramTypes = new JavaType[parameterCount];

        // Get arg type for lambda arg index
        JavaMethod lambdaMethod = getLambdaMethod();
        if (lambdaMethod == null)
            return null;
        if (lambdaMethod.getParameterCount() != parameterCount)
            return null;

        // Iterate over parameters and get EvalClass for each
        for (int i = 0; i < parameterCount; i++) {

            // Get arg type for lambda arg index
            JavaType argType = lambdaMethod.getParameterType(i);
            //if (!argType.isResolvedType())
            //    argType = getResolvedTypeForType(argType);
            paramTypes[i] = argType;
        }

        // Set and return
        return _parameterTypes = paramTypes;
    }

    /**
     * Creates and returns a JType node for given VarDecl.
     */
    protected JType createTypeNodeForLambdaParameterVarDecl(JVarDecl varDecl)
    {
        // Get parameter index for var decl
        int parameterIndex = _params.indexOf(varDecl);
        if (parameterIndex < 0)
            return null;

        // Get resolved parameter types - just return if missing
        JavaClass[] parameterTypes = getLambdaMethodParameterTypesResolved();
        if (parameterTypes == null || parameterIndex >= parameterTypes.length)
            return null;

        // Get parameter type
        JavaType parameterType = parameterTypes[parameterIndex];
        if (parameterType == null)
            return null;

        // Create type for type decl and return
        JType type = JType.createTypeForTypeAndToken(parameterType, varDecl.getStartToken());
        type._parent = varDecl;
        return type;
    }

    /**
     * WithVarDecls method.
     */
    @Override
    public List<JVarDecl> getVarDecls()  { return _params; }
}