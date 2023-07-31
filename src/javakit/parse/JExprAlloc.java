/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import javakit.resolver.*;
import snap.util.ArrayUtils;
import snap.util.StringUtils;

/**
 * A JExpr subclass for Allocation expressions.
 */
public class JExprAlloc extends JExpr {

    // The Allocation type
    protected JType  _type;

    // The allocation args
    protected List<JExpr>  _args = Collections.EMPTY_LIST;

    // The dimensions expression, if array
    protected JExpr  _arrayDims;

    // The array init expressions, if array
    protected List<JExpr>  _arrayInits = Collections.EMPTY_LIST;

    // The allocation JClassDecl
    protected JClassDecl  _classDecl;

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
    public List<JExpr> getArgs()  { return _args; }

    /**
     * Sets the allocation arguments.
     */
    public void setArgs(List<JExpr> theArgs)
    {
        if (_args != null) for (JExpr arg : _args) removeChild(arg);
        _args = theArgs;
        if (_args != null) for (JExpr arg : _args) addChild(arg, -1);
    }

    /**
     * Returns the arg eval classes.
     */
    public JavaClass[] getArgClasses()
    {
        List<JExpr> args = getArgs();
        JavaClass[] argClasses = new JavaClass[args.size()];

        // Iterate over args and map to eval types
        for (int i = 0, iMax = args.size(); i < iMax; i++) {
            JExpr arg = args.get(i);
            if (arg instanceof JExprLambda && arg._decl == null)
                arg = null;
            argClasses[i] = arg != null ? arg.getEvalClass() : null;
        }

        // Return
        return argClasses;
    }

    /**
     * Returns the array dimensions.
     */
    public JExpr getArrayDims()  { return _arrayDims; }

    /**
     * Sets the array dimension.
     */
    public void setArrayDims(JExpr theDims)
    {
        replaceChild(_arrayDims, _arrayDims = theDims);
    }

    /**
     * Returns the array init expressions, if array.
     */
    public List<JExpr> getArrayInits()  { return _arrayInits; }

    /**
     * Sets the array init expressions, if array.
     */
    public void setArrayInits(List<JExpr> theArrayInits)
    {
        if (_arrayInits != null) for (JExpr expr : _arrayInits) removeChild(expr);
        _arrayInits = theArrayInits;
        if (_arrayInits != null) for (JExpr expr : _arrayInits) addChild(expr, -1);
    }

    /**
     * Returns the allocation ClassBodyDecl.
     */
    public JClassDecl getClassDecl()  { return _classDecl; }

    /**
     * Sets the allocation ClassBodyDecl.
     */
    public void setClassDecl(JClassDecl aCD)
    {
        replaceChild(_classDecl, _classDecl = aCD);
    }

    /**
     * Tries to resolve the method declaration for this node.
     */
    protected JavaDecl getDeclImpl()
    {
        // Get type - just return if null
        JType type = getType();
        JavaType javaType = type != null ? type.getDecl() : null;
        if (javaType == null)
            return null;

        // If array alloc, just return Type decl
        if (_arrayDims != null || _arrayInits.size() > 0)
            return javaType;

        // Get class decl and constructor arg types
        JavaClass javaClass = javaType.getEvalClass();
        JavaClass[] argClasses = getArgClasses();

        // If inner class and not static, add implied class type to arg types array
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
        // Get arg classes
        JavaClass[] argClasses = getArgClasses();
        if (argClasses.length == 0)
            return "()";

        // Get arg type string and join by comma
        String[] argTypeStrings = ArrayUtils.map(argClasses, type -> type != null ? type.getSimpleName() : "null", String.class);
        String argTypeString = StringUtils.join(argTypeStrings, ",");

        // Return in parens
        return '(' + argTypeString + ')';
    }
}