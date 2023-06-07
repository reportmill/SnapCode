/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import javakit.resolver.*;
import snap.util.ListUtils;

/**
 * A JExpr to represent lambda expressions.
 */
public class JExprLambda extends JExpr implements WithVarDecls, WithBlockStmt {

    // The parameters
    protected List<JVarDecl>  _params = new ArrayList<>();

    // The expression, if lambda has expression
    protected JExpr  _expr;

    // The statement Block, if lambda has block
    protected JStmtBlock  _block;

    // The class this lambda
    private JavaClass _lambdaClass;

    // The actual interface method this lambda represents
    private JavaMethod _lambdaMethod;

    /**
     * Returns the list of formal parameters.
     */
    public List<JVarDecl> getParams()  { return _params; }

    /**
     * Returns the number of parameters.
     */
    public int getParamCount()  { return _params.size(); }

    /**
     * Adds a formal parameter.
     */
    public void addParam(JVarDecl aVD)
    {
        _params.add(aVD);
        addChild(aVD, -1);
    }

    /**
     * Returns the list of parameter classes.
     */
    public String[] getParamNames()
    {
        // Iterate over params and get EvalClass for each
        String[] paramNames = new String[_params.size()];
        for (int i = 0, iMax = _params.size(); i < iMax; i++) {
            JVarDecl varDecl = _params.get(i);
            paramNames[i] = varDecl.getName();
        }

        // Return
        return paramNames;
    }

    /**
     * Returns the list of parameter classes.
     */
    public JavaClass[] getParamTypes()
    {
        // Iterate over params and get EvalClass for each
        JavaClass[] paramTypes = new JavaClass[_params.size()];
        for (int i = 0, iMax = _params.size(); i < iMax; i++) {
            JVarDecl varDecl = _params.get(i);
            paramTypes[i] = varDecl.getEvalClass();
        }

        // Return
        return paramTypes;
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
     * WithVarDecls method.
     */
    @Override
    public List<JVarDecl> getVarDecls()  { return _params; }

    /**
     * Override to return as type.
     */
    @Override
    public JavaMethod getDecl()
    {
        return (JavaMethod) super.getDecl();
    }

    /**
     * Override to try to resolve decl from parent.
     */
    @Override
    protected JavaMethod getDeclImpl()
    {
        return getLambdaMethod();
    }

    /**
     * Returns the specific method in the lambda class interface that is to be called.
     */
    public JavaMethod getLambdaMethod()
    {
        // If already set, just return
        if (_lambdaMethod != null) return _lambdaMethod;

        // Get lambda class and lambda method with correct arg count
        JavaClass lambdaClass = getLambdaClass();
        JavaMethod lambdaMethod = lambdaClass.getLambdaMethod();

        // Set/return
        return _lambdaMethod = lambdaMethod;
    }

    /**
     * Returns the lambda class.
     */
    public JavaClass getLambdaClass()
    {
        if (_lambdaClass != null) return _lambdaClass;
        JavaClass lambdaClass = getLambdaClassImpl();
        return _lambdaClass = lambdaClass;
    }

    /**
     * Returns the lambda class.
     */
    private JavaClass getLambdaClassImpl()
    {
        // Get Parent (just return if null)
        JNode par = getParent();
        if (par == null)
            return null;

        // Handle parent is method call: Get lambda interface from method call decl param
        if (par instanceof JExprMethodCall) {

            // Get methodCall method
            JExprMethodCall methodCall = (JExprMethodCall) par;
            JavaMethod method = methodCall.getDecl();
            if (method == null)
                return null;

            // Get arg index of this lambda expr
            List<JExpr> args = methodCall.getArgs();
            int argIndex = ListUtils.indexOfId(args, this);
            if (argIndex < 0)
                return null;

            // Get arg type at arg index
            JavaType lambdaType = method.getParamType(argIndex);
            JavaClass lambdaClass = lambdaType.getEvalClass();
            return lambdaClass;
        }

        // Handle parent anything else (JVarDecl, JStmtExpr): Return parent eval class
        JavaClass lambdaClass = par.getEvalClass();
        if (lambdaClass != null)
            return lambdaClass;

        // Return not found
        return null;
    }

    /**
     * Override to check lambda parameters.
     */
    protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
    {
        // If node is parameter name, return param decl
        String name = anExprId.getName();
        JVarDecl param = getVarDeclForName(name);
        if (param != null)
            return param.getDecl();

        // Do normal version
        return super.getDeclForChildExprIdNode(anExprId);
    }

    /**
     * Returns the node name.
     */
    public String getNodeString()  { return "LambdaExpr"; }
}