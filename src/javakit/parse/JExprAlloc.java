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
public class JExprAlloc extends JExpr {

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
    public JExpr[] getArgs()  { return _args; }

    /**
     * Sets the allocation arguments.
     */
    public void setArgs(JExpr[] theArgs)
    {
        _args = theArgs;
        Stream.of(_args).forEach(this::addChild);
    }

    /**
     * Returns the arg eval classes.
     */
    public JavaClass[] getArgClasses()
    {
        JExpr[] args = getArgs();
        JavaClass[] argClasses = new JavaClass[args.length];

        // Iterate over args and map to eval types
        for (int i = 0, iMax = args.length; i < iMax; i++) {
            JExpr arg = args[i];
            if ((arg instanceof JExprLambda || arg instanceof JExprMethodRef) && arg._decl == null)
                arg = null;
            argClasses[i] = arg != null ? arg.getEvalClass() : null;
        }

        // Return
        return argClasses;
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
     * Tries to resolve the method declaration for this node.
     */
    protected JavaDecl getDeclImpl()
    {
        // If array alloc, just return Type decl
        if (_arrayDims != null || _arrayInit != null)
            return getJavaType();

        // Return Constructor
        return getConstructor();
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
        JavaClass[] argClasses = getArgClasses();

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
        JavaClass[] argClasses = getArgClasses();
        String argTypeString = ArrayUtils.mapToStringsAndJoin(argClasses, type -> type != null ? type.getSimpleName() : "null", ",");
        return '(' + argTypeString + ')';
    }
}