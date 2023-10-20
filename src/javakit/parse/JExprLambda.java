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
    public void addParameter(JVarDecl aVD)
    {
        _params.add(aVD);
        addChild(aVD, -1);
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
     * Override to get eval type from expression if possible.
     */
    @Override
    protected JavaType getEvalTypeImpl()
    {
        // If parent is variable declaration, return its type
        JNode parentNode = getParent();
        if (parentNode instanceof JVarDecl)
            return parentNode.getEvalType();

        // If expression is set, return it's eval type
        if (_expr != null) {
            JavaDecl exprDecl = _expr.getDecl();
            return exprDecl != null ? exprDecl.getEvalType() : null;
        }

        // Do normal version
        return super.getEvalTypeImpl();
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

        // Get parameter type for var decl
        JavaType[] parameterTypes = getParameterTypes();
        JavaType parameterType = parameterTypes != null && parameterTypes.length > parameterIndex ? parameterTypes[parameterIndex] : null;
        if (parameterType == null)
            return null;

        // If not resolved, try to resolve
        if (!parameterType.isResolvedType()) {
            JNode parentNode = getParent(); // Should be method call
            parameterType = parentNode.getResolvedTypeForType(parameterType);
        }

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