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

    // The declaration for the actual method for the interface this lambda represents
    protected JavaMethod  _meth;

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
     * Returns the specific method in the lambda class interface that is to be called.
     */
    public JavaMethod getMethod()
    {
        getDecl();
        return _meth;
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
    public JavaType getDecl()
    {
        return (JavaType) super.getDecl();
    }

    /**
     * Override to try to resolve decl from parent.
     */
    protected JavaType getDeclImpl()
    {
        // Get Parent (just return if null)
        JNode par = getParent();
        if (par == null)
            return null;

        // Handle parent is method call: Get lambda interface from method call decl param
        if (par instanceof JExprMethodCall) {

            // Get methodCall and matching methods
            JExprMethodCall methodCall = (JExprMethodCall) par;
            List<JavaMethod> methods = getCompatibleMethods();
            if (methods == null || methods.size() == 0)
                return null;

            // Get arg index of this lambda
            List<JExpr> argExpressions = methodCall.getArgs();
            int argIndex = ListUtils.indexOfId(argExpressions, this);
            int argCount = getParamCount();
            if (argIndex < 0)
                return null;

            // Iterate over methods and return first that matches arg count
            for (JavaMethod method : methods) {
                if (method.isDefault()) continue;
                JavaType paramType = method.getParamType(argIndex);
                JavaClass paramClass = paramType.getEvalClass();
                _meth = paramClass.getLambdaMethod(argCount);
                if (_meth != null)
                    return paramType;
            }

            // Otherwise, let's just return param type of first matching method (maybe TeaVM thing)
            if (methods.size() > 0) {
                JavaMethod method = methods.get(0);
                JavaType paramType = method.getParamType(argIndex);
                return paramType;
            }

            // Return not found
            return null;
        }

        // Handle parent anything else (JVarDecl, JStmtExpr): Get lambda interface from eval type
        if (par._decl != null) {

            // If type is interface, get lambda type
            JavaType parentType = par.getEvalType();
            if (parentType != null) {
                JavaClass parentClass = parentType.getEvalClass();
                _meth = parentClass.getLambdaMethod(getParamCount());
                if (_meth != null)
                    return parentType;
            }
        }

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
     * Returns the method decl for the parent method call (assumes this lambda is an arg).
     */
    protected List<JavaMethod> getCompatibleMethods()
    {
        // Get method call, method name and args
        JExprMethodCall methodCallExpr = (JExprMethodCall) getParent();
        String name = methodCallExpr.getName();
        List<JExpr> methodArgs = methodCallExpr.getArgs();
        int argCount = methodArgs.size();

        // Get arg types
        JavaType[] argTypes = new JavaType[argCount];
        for (int i = 0; i < argCount; i++) {
            JExpr arg = methodArgs.get(i);
            argTypes[i] = arg instanceof JExprLambda ? null : arg.getEvalType();
        }

        // Get scope node class type and search for compatible method for name and arg types
        JNode scopeNode = methodCallExpr.getScopeNode();
        JavaType scopeType = scopeNode != null ? scopeNode.getEvalType() : null;
        JavaClass scopeClass = scopeType != null ? scopeType.getEvalClass() : null;
        if (scopeClass == null)
            return null;

        // Get scope node class type and search for compatible method for name and arg types
        List<JavaMethod> compatibleMethods = JavaClassUtils.getCompatibleMethodsAll(scopeClass, name, argTypes);
        if (compatibleMethods.size() > 0)
            return compatibleMethods;

        // If scope node class type is member class and not static, go up parent classes
        while (scopeClass.isMemberClass() && !scopeClass.isStatic()) {
            scopeClass = scopeClass.getDeclaringClass();
            compatibleMethods = JavaClassUtils.getCompatibleMethodsAll(scopeClass, name, argTypes);
            if (compatibleMethods.size() > 0)
                return compatibleMethods;
        }

        // See if method is from static import
        //decl = getFile().getImportClassMember(name, argTypes);
        //if(decl!=null && decl.isMethod()) return decl;

        // Return null since not found
        return compatibleMethods;
    }

    /**
     * Returns the node name.
     */
    public String getNodeString()  { return "LambdaExpr"; }
}