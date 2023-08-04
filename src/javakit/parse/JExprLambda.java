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

    // The type for this lambda
    private JavaType _lambdaType;

    // The actual interface method this lambda represents
    private JavaMethod _lambdaMethod;

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
            return paramTypes;

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
    protected JType createTypeNodeForVarDecl(JVarDecl varDecl)
    {
        // Get parameter index for var decl
        int parameterIndex = _params.indexOf(varDecl);
        if (parameterIndex < 0)
            return null;

        // Get parameter type for var decl
        JavaType[] parameterTypes = getParameterTypes();
        JavaType parameterType = parameterTypes[parameterIndex];

        // Create type for type decl and return
        JType type = JType.createTypeForTypeAndToken(parameterType, varDecl.getStartToken());
        type._parent = varDecl;
        return type;
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
     * Override to return as method.
     */
    @Override
    public JavaMethod getDecl()  { return (JavaMethod) super.getDecl(); }

    /**
     * Override to try to resolve decl from parent.
     */
    @Override
    protected JavaMethod getDeclImpl()
    {
        return getLambdaMethod();
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
        if (_expr != null)
            return _expr.getEvalType();

        // Do normal version
        return super.getEvalTypeImpl();
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
        JavaMethod lambdaMethod = lambdaClass != null ? lambdaClass.getLambdaMethod() : null;

        // Set/return
        return _lambdaMethod = lambdaMethod;
    }

    /**
     * Returns the lambda type.
     */
    public JavaType getLambdaType()
    {
        if (_lambdaType != null) return _lambdaType;
        JavaType lambdaType = getLambdaTypeImpl();
        return _lambdaType = lambdaType;
    }

    /**
     * Returns the lambda type.
     */
    private JavaType getLambdaTypeImpl()
    {
        // Get Parent (just return if null)
        JNode parentNode = getParent();
        if (parentNode == null)
            return null;

        // Handle parent is method call: Get lambda interface from method call decl param
        if (parentNode instanceof JExprMethodCall) {

            // Get methodCall method
            JExprMethodCall methodCall = (JExprMethodCall) parentNode;
            JavaMethod method = methodCall.getDecl();
            if (method == null)
                return null;

            // Get arg index of this lambda expr
            List<JExpr> args = methodCall.getArgs();
            int argIndex = ListUtils.indexOfId(args, this);
            if (argIndex < 0)
                return null;

            // Get arg type at arg index
            return method.getParameterType(argIndex);
        }

        // Handle parent is alloc expression: Get lambda interface from alloc expression param
        if (parentNode instanceof JExprAlloc) {

            // Get alloc expr contructor
            JExprAlloc allocExpr = (JExprAlloc) parentNode;
            JavaConstructor constructor = (JavaConstructor) allocExpr.getDecl();
            if (constructor == null)
                return null;

            // Get arg index of this lambda expr
            List<JExpr> args = allocExpr.getArgs();
            int argIndex = ListUtils.indexOfId(args, this);
            if (argIndex < 0)
                return null;

            // Get arg type at arg index
            return constructor.getParameterType(argIndex);
        }

        // Handle parent anything else (JVarDecl, JStmtExpr): Return parent eval type
        JavaType lambdaType = parentNode.getEvalType();
        if (lambdaType != null)
            return lambdaType;

        // Return not found
        return null;
    }

    /**
     * Returns the lambda class.
     */
    public JavaClass getLambdaClass()
    {
        JavaType lambdaType = getLambdaType();
        return lambdaType != null ? lambdaType.getEvalClass() : null;
    }

    /**
     * Returns the node name.
     */
    public String getNodeString()  { return "LambdaExpr"; }

    /**
     * Returns the node errors.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        NodeError[] errors = super.getErrorsImpl();

        // Handle missing class
        JavaMethod lambdaMethod = getLambdaMethod();
        if (lambdaMethod == null)
            errors = NodeError.addError(errors, this, "Can't resolve lambda method", 0);

        // Return
        return errors;
    }
}