/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaExecutable;
import javakit.resolver.JavaType;
import snap.util.ArrayUtils;
import java.util.stream.Stream;

/**
 * This JMemberDecl subclass represents Java executables (methods & constructors).
 */
public abstract class JExecutableDecl extends JMemberDecl implements WithBlockStmt, WithVarDecls, WithTypeParameters {

    // Type variables
    private JTypeVar[] _typeVars = new JTypeVar[0];

    // The formal parameters
    protected JVarDecl[] _params = new JVarDecl[0];

    // The array of thrown exception class name expressions
    protected JExpr[] _throwsList = JExpr.EMPTY_EXPR_ARRAY;

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
     * Returns the method type variables.
     */
    public JTypeVar[] getTypeParamDecls()  { return _typeVars; }

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
     * Returns array of parameter types suitable to resolve method.
     */
    public JavaType[] getGenericParameterTypes()
    {
        JVarDecl[] parameters = getParameters();
        return ArrayUtils.map(parameters, this::getGenericParameterTypeForVarDecl, JavaType.class);
    }

    /**
     * Return the JavaType for given parameter var decl (or java.lang.Object if null).
     */
    private JavaType getGenericParameterTypeForVarDecl(JVarDecl paramDecl)
    {
        JavaType javaType = paramDecl.getJavaType();
        if (javaType != null)
            return javaType;
        return getResolver().getJavaClassForName("java.lang.Object");
    }

    /**
     * Returns array of parameter class types suitable to resolve method.
     */
    public JavaClass[] getParameterClasses()
    {
        JVarDecl[] parameters = getParameters();
        return ArrayUtils.map(parameters, JVarDecl::getJavaClass, JavaClass.class);
    }

    /**
     * Override to resolve method/constructor type vars.
     */
    @Override
    protected JavaType getJavaTypeForChildType(JType aJType)
    {
        // Handle TypeVar name: return typevar decl
        String typeName = aJType.getName();
        JTypeVar typeVarDecl = getTypeParamDeclForName(typeName);
        if (typeVarDecl != null)
            return typeVarDecl.getTypeVariable();

        // Do normal version
        return super.getJavaTypeForChildType(aJType);
    }

    /**
     * Override to return errors for ReturnValue, Parameters, ThrowsList and TypeVars.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        NodeError[] errors = NodeError.NO_ERRORS;

        // Get errors for params
        JVarDecl[] parameters = getParameters();
        errors = NodeError.addNodeErrorsForNodes(errors, parameters);

        // Get errors for throws list
        JExpr[] throwsList = getThrowsList();
        errors = NodeError.addNodeErrorsForNodes(errors, throwsList);

        // Get errors for type vars
        JTypeVar[] typeVars = getTypeParamDecls();
        errors = NodeError.addNodeErrorsForNodes(errors, typeVars);

        // Return
        return errors;
    }
}