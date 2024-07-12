/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.stream.Stream;
import javakit.resolver.*;
import snap.util.ArrayUtils;

/**
 * A JExpr subclass for Allocation expressions.
 */
public class JExprAlloc extends JExpr implements WithArgs {

    // The Allocation type
    protected JType  _type;

    // The allocation args
    protected JExpr[] _args = JExpr.EMPTY_EXPR_ARRAY;

    // The array dimension expression, if array
    protected JExpr  _arrayDims;

    // The array initializer expression, if array (or array of arrays if multidimensional)
    protected JExprArrayInit _arrayInit;

    // The allocation ClassDecl
    protected JClassDecl _classDecl;

    // The allocation ClassBody body declarations
    protected JBodyDecl[] _classBodyDecls;

    // The constructor (if class type constructor)
    private JavaConstructor _constructor;

    /**
     * Constructor.
     */
    public JExprAlloc()
    {
        super();
    }

    /**
     * Returns the allocation JType.
     */
    public JType getType()  { return _type; }

    /**
     * Sets the allocation JType.
     */
    public void setType(JType aType)
    {
        replaceChild(_type, _type = aType);
    }

    /**
     * Returns the allocation arguments.
     */
    @Override
    public JExpr[] getArgs()  { return _args; }

    /**
     * Sets the allocation arguments.
     */
    @Override
    public void setArgs(JExpr[] theArgs)
    {
        _args = theArgs;
        Stream.of(_args).forEach(this::addChild);
    }

    /**
     * Returns the array dimensions (array only).
     */
    public JExpr getArrayDims()  { return _arrayDims; }

    /**
     * Sets the array dimension (array only).
     */
    public void setArrayDims(JExpr theDims)
    {
        replaceChild(_arrayDims, _arrayDims = theDims);
    }

    /**
     * Returns the array initializer expression (array only).
     */
    public JExprArrayInit getArrayInit()  { return _arrayInit; }

    /**
     * Sets the array initializer expression (array only).
     */
    public void setArrayInit(JExprArrayInit theArrayInits)
    {
        replaceChild(_arrayInit, _arrayInit = theArrayInits);
    }

    /**
     * Returns the ClassDecl for anonymous class, if allocation has body declarations.
     */
    public JClassDecl getClassDecl()  { return _classDecl; }

    /**
     * Returns the allocation ClassBody body declarations.
     */
    public JBodyDecl[] getClassBodyDecls()  { return _classBodyDecls; }

    /**
     * Sets the allocation ClassBodyDecl body declarations.
     */
    public void setClassBodyDecls(JBodyDecl[] bodyDecls)
    {
        _classBodyDecls = bodyDecls;

        _classDecl = new JClassDecl();
        _classDecl.getExtendsTypes().add(getType()); // Want to reference type, not steal it
        _classDecl.setBodyDecls(bodyDecls);
        addChild(_classDecl);
    }

    /**
     * Returns the JavaType of the allocation.
     */
    private JavaType getJavaType()
    {
        // If ClassDecl, return its type
        JClassDecl classDecl = getClassDecl();
        if (classDecl != null)
            return classDecl.getJavaClass();

        // Get type - just return if null
        JType type = getType();
        if (type != null)
            return type.getJavaType();

        // Return not found
        return null;
    }

    /**
     * Returns the constructor.
     */
    public JavaConstructor getConstructor()
    {
        if (_constructor != null) return _constructor;
        return _constructor = getConstructorImpl();
    }

    /**
     * Returns this alloc expressions constructor.
     */
    private JavaConstructor getConstructorImpl()
    {
        // Get JavaType - just return if null
        JavaType javaType = getJavaType();
        if (javaType == null)
            return null;

        // Get class decl
        JavaClass javaClass = javaType.getEvalClass();

        // If anonymous class, just return first constructor
        if (javaClass.isAnonymousClass()) {
            JavaConstructor[] constrs = javaClass.getDeclaredConstructors();
            if (constrs.length != 1)
                System.out.println("JExprAlloc.getConstructorImpl(): Invalid constructor count (should be 1): " + constrs.length);
            return constrs.length > 0 ? constrs[0] : null;
        }

        // Get arg classes
        JavaClass[] argClasses = ArrayUtils.map(_args, arg -> arg instanceof JExprLambdaBase ? null : arg.getEvalClass(), JavaClass.class);

        // If inner class and not static, add implied class types to arg types array
        if (javaClass.isMemberClass() && !javaClass.isStatic()) {
            JavaClass parentClass = javaClass.getDeclaringClass();
            argClasses = ArrayUtils.add(argClasses, parentClass, 0);
        }

        // Get scope node class type and search for compatible method for name and arg types
        JavaConstructor constructor = JavaClassUtils.getCompatibleConstructor(javaClass, argClasses);
        if (constructor != null)
            return constructor;

        // Return not found
        return null;
    }

    /**
     * Tries to resolve the method declaration for this node.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        // If array alloc, just return Type decl
        if (_arrayDims != null || _arrayInit != null)
            return getJavaType();

        // Return Constructor
        return getConstructor();
    }

    /**
     * Returns the JavaDecl that this nodes evaluates to (resolved, if TypeVar).
     */
    @Override
    protected JavaType getEvalTypeImpl()
    {
        // If constructor alloc
        if (getDecl() instanceof JavaConstructor)
            return getResolvedConstructorType();

        // Return
        return super.getEvalTypeImpl();
    }

    /**
     * Returns the constructor resolved type.
     */
    private JavaType getResolvedConstructorType()
    {
        // Get constructor class type params - just return class if no type params
        JavaConstructor constructor = getConstructor();
        JavaClass javaClass = constructor.getEvalClass();
        JavaTypeVariable[] typeParams = javaClass.getTypeParameters();
        if (typeParams.length == 0)
            return javaClass;

        // Get parameter types - just return class if no parameters (or all parameters are simple classes)
        JavaType[] paramTypes = constructor.getGenericParameterTypes();
        if (paramTypes.length == 0 || !ArrayUtils.hasMatch(paramTypes, type -> !(type instanceof JavaClass)))
            return javaClass;

        // Get arg types and resolved type parameters - just return class if nothing resolved
        JavaType[] argTypes = ArrayUtils.map(_args, arg -> arg instanceof JExprLambdaBase ? null : arg.getEvalType(), JavaType.class);
        JavaType[] resolvedTypeParams = ArrayUtils.map(typeParams, type -> JavaTypeUtils.getResolvedTypeVariableForTypeArrays(type, paramTypes, argTypes), JavaType.class);
        if (ArrayUtils.equalsId(typeParams, resolvedTypeParams))
            return javaClass;

        // Create parameterized type and return
        return javaClass.getParameterizedTypeForTypes(resolvedTypeParams);
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()  { return "Allocation"; }

    /**
     * Override to provide errors for this class.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        NodeError[] errors = super.getErrorsImpl();

        // If decl resolved, just return
        JavaDecl classOrConstructor = getDecl();
        if (classOrConstructor != null)
            return errors;

        // Handle unresolved type
        JType type = getType();
        if (type == null)
            return NodeError.addError(errors, this, "Identifier expected", 0);

        // Handle can't find constructor
        String constrString = getConstructorString();
        String errorString = "Can't resolve constructor: " + constrString;
        return NodeError.addError(errors, this, errorString);
    }

    /**
     * Returns a string for constructor.
     */
    private String getConstructorString()
    {
        // Get class name and arg types
        JType type = getType();
        String className = type != null ? type.getName() : null;
        if (className == null)
            return "No name found";
        String argTypesString = getArgTypesString();
        String constructorString = className + argTypesString;

        // Return
        return constructorString;
    }

    /**
     * Returns the parameter string.
     */
    private String getArgTypesString()
    {
        JavaType[] argTypes = ArrayUtils.map(_args, expr -> expr != null ? expr.getDeclEvalType() : null, JavaType.class);
        String argTypeString = ArrayUtils.mapToStringsAndJoin(argTypes, type -> type != null ? type.getSimpleName() : "null", ",");
        return '(' + argTypeString + ')';
    }
}