/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaParameterizedType;
import javakit.resolver.JavaType;
import snap.util.ArrayUtils;

/**
 * A JStatement for for() statements.
 */
public class JStmtFor extends JStmtConditional implements WithVarDecls {

    // Whether this for statement is really ForEach
    protected boolean _forEach;

    // The var decl expression
    protected JExprVarDecl _varDeclExpr;

    // The init expressions (if basic for and no VarDecl)
    private JExpr[] _initExprs = JExpr.EMPTY_EXPR_ARRAY;

    // The update expressions
    private JExpr[] _updateExprs = JExpr.EMPTY_EXPR_ARRAY;

    // The iterable expression (ForEach)
    private JExpr _iterableExpr;

    /**
     * Constructor.
     */
    public JStmtFor()
    {
        super();
    }

    /**
     * Returns whether for statement is ForEach.
     */
    public boolean isForEach()  { return _forEach; }

    /**
     * Returns the var decl expression.
     */
    public JExprVarDecl getVarDeclExpr()  { return _varDeclExpr; }

    /**
     * Sets the var decl expression.
     */
    public void setVarDeclExpr(JExprVarDecl varDeclExpr)
    {
        replaceChild(_varDeclExpr, _varDeclExpr = varDeclExpr);
    }

    /**
     * Returns the init expression.
     */
    public JExpr[] getInitExprs()  { return _initExprs; }

    /**
     * Adds an init expression.
     */
    public void addInitExpr(JExpr anExpr)
    {
        _initExprs = ArrayUtils.add(_initExprs, anExpr);
        addChild(anExpr);
    }

    /**
     * Returns the update expressions.
     */
    public JExpr[] getUpdateExprs()  { return _updateExprs; }

    /**
     * Add an update expressions.
     */
    public void addUpdateExpr(JExpr anExpr)
    {
        _updateExprs = ArrayUtils.add(_updateExprs, anExpr);
        addChild(anExpr);
    }

    /**
     * Returns the iterable expression (ForEach only).
     */
    public JExpr getIterableExpr()  { return _iterableExpr; }

    /**
     * Sets the iterable expression (ForEach only).
     */
    public void setIterableExpr(JExpr anExpr)
    {
        replaceChild(_iterableExpr, _iterableExpr = anExpr);
    }

    /**
     * WithVarDecls method.
     */
    @Override
    public JVarDecl[] getVarDecls()
    {
        return _varDeclExpr != null ? _varDeclExpr.getVarDecls() : new JVarDecl[0];
    }

    /**
     * Override to make sure we don't return ForEach var decl for anything in iterable expression.
     */
    @Override
    public JVarDecl getVarDeclForId(JExprId anId)
    {
        // Do normal version - just return if null
        JVarDecl varDecl = super.getVarDeclForId(anId);
        if (varDecl == null)
            return null;

        // If Id before end of iterable expression, return null
        JExpr iterableExpr = getIterableExpr();
        if (iterableExpr != null && anId.getStartCharIndex() < iterableExpr.getEndCharIndex())
            return null;

        // Return
        return varDecl;
    }

    /**
     * Override to do checks for for() statement.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        // Handle var decl errors
        JExprVarDecl varDeclExpr = getVarDeclExpr();
        if (varDeclExpr != null) {
            NodeError[] varDeclErrors = varDeclExpr.getErrors();
            if (varDeclErrors.length > 0)
                return varDeclErrors;
        }

        // Handle for each
        if (isForEach()) {

            // Handle missing or invalid iterable expression
            JExpr iterableExpr = getIterableExpr();
            if (iterableExpr == null)
                return NodeError.newErrorArray(this, "Missing iterable or array");
            NodeError[] iterableErrors = iterableExpr.getErrors();
            if (iterableErrors.length > 0)
                return iterableErrors;

            // Handle iterable expression not iterable
            JavaClass iterableClass = iterableExpr.getEvalClass();
            boolean isArrayOrIterable = iterableClass.isArray() || getJavaClassForClass(Iterable.class).isAssignableFrom(iterableClass);
            if (!isArrayOrIterable)
                return NodeError.newErrorArray(iterableExpr, "Expression must be array or iterable");

            // Handle iterable type not assignable to var decl
            assert (varDeclExpr != null);
            JavaClass varDeclClass = varDeclExpr.getEvalClass();
            JavaClass iterationClass = getForEachIterationTypeResolved();
            if (iterationClass != null && !varDeclClass.isAssignableFrom(iterationClass))
                return NodeError.newErrorArray(iterableExpr, "Incompatible types: " +
                    iterationClass.getSimpleName() + " cannot be assigned to " + varDeclClass.getSimpleName());
        }

        // Handle basic
        else {

            // If errors in init expressions, return
            JExpr[] initExprs = getInitExprs();
            for (JExpr initExpr : initExprs) {
                NodeError[] initErrors = initExpr.getErrors();
                if (initErrors.length > 0)
                    return initErrors;
            }

            // If errors in conditional expression, return
            JExpr condExpr = getConditional();
            if (condExpr != null) {
                NodeError[] condErrors = condExpr.getErrors();
                if (condErrors.length > 0)
                    return condErrors;
            }

            // If errors in update expressions, return
            JExpr[] updateExprs = getUpdateExprs();
            for (JExpr updateExpr : updateExprs) {
                NodeError[] updateErrors = updateExpr.getErrors();
                if (updateErrors.length > 0)
                    return updateErrors;
            }
        }

        // Do normal version
        return super.getErrorsImpl();
    }

    /**
     * Returns the ForEach iteration type.
     */
    public JavaType getForEachIterationType()
    {
        // Get ForEach iterable expression
        JExpr iterableExpr = getIterableExpr();
        if (iterableExpr == null)
            return null;

        // Get iterable type (just return if null)
        JavaType iterableType = iterableExpr.getEvalType();
        if (iterableType == null)
            return null;

        // Handle iterable is array: Return component type
        if (iterableType.isArray())
            return iterableType.getComponentType();

        // Handle iterable is ParameterizedType: Return first
        if (iterableType instanceof JavaParameterizedType) {
            JavaParameterizedType parameterizedType = (JavaParameterizedType) iterableType;
            JavaType[] parameterTypes = parameterizedType.getParamTypes();
            if (parameterTypes.length > 0)
                return parameterTypes[0];
        }

        // Show some surprise
        System.err.println("JStmtFor.getForEachIterationType: Can't determine type for iterable: " + iterableType);
        return null;
    }

    /**
     * Returns the ForEach iteration type.
     */
    public JavaClass getForEachIterationTypeResolved()
    {
        // Get iteration type - just return if null
        JavaType iterationType = getForEachIterationType();
        if (iterationType == null)
            return null;

        // If not resolved, resolve it
        if (!iterationType.isResolvedType())
            iterationType = getResolvedTypeForType(iterationType);

        // Return
        return iterationType.getEvalClass();
    }
}