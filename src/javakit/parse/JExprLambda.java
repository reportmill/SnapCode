/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.stream.Stream;
import javakit.resolver.*;
import snap.util.ArrayUtils;

/**
 * A JExpr to represent lambda expressions.
 */
public class JExprLambda extends JExprLambdaBase implements WithVarDecls, WithBlockStmt {

    // The parameters
    protected JVarDecl[] _params = new JVarDecl[0];

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
     * Returns the list of formal parameters.
     */
    public JVarDecl[] getParameters()  { return _params; }

    /**
     * Set the formal parameters.
     */
    public void setParameters(JVarDecl[] varDecls)
    {
        _params = varDecls;
        Stream.of(varDecls).forEach(vdecl -> addChild(vdecl));
    }

    /**
     * Adds a formal parameter.
     */
    public void addParameter(JVarDecl aVarDecl)
    {
        _params = ArrayUtils.add(_params, aVarDecl);
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
     * Returns the resolved lambda method return type.
     */
    @Override
    public JavaType getLambdaMethodReturnTypeResolved()
    {
        // If expression is set, return its eval type
        if (_expr != null) {
            JavaType exprEvalType = _expr.getEvalType();
            if (exprEvalType != null)
                return exprEvalType;
        }

        // Do normal version
        return super.getLambdaMethodReturnTypeResolved();
    }

    /**
     * Creates and returns a JType node for given VarDecl.
     */
    protected JType createTypeNodeForLambdaParameterVarDecl(JVarDecl varDecl)
    {
        // Get parameter index for var decl
        int parameterIndex = ArrayUtils.indexOf(_params, varDecl);
        if (parameterIndex < 0)
            return null;

        // Get resolved parameter types - just return if missing
        JavaClass[] parameterTypes = getLambdaMethodParameterTypesResolved();
        if (parameterTypes == null || parameterIndex >= parameterTypes.length)
            return null;

        // Get parameter type
        JavaClass parameterType = parameterTypes[parameterIndex];
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
    public JVarDecl[] getVarDecls()  { return _params; }
}