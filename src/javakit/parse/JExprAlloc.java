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
     * Returns the arg eval types.
     */
    public JavaType[] getArgEvalTypes()
    {
        List<JExpr> args = getArgs();
        JavaType[] argTypes = new JavaType[args.size()];

        for (int i = 0, iMax = args.size(); i < iMax; i++) {
            JExpr arg = args.get(i);
            argTypes[i] = arg != null ? arg.getEvalType() : null;
        }

        // Return
        return argTypes;
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
        // If array alloc, just return Type decl
        JType type = getType();
        if (_arrayDims != null || _arrayInits.size() > 0)
            return type.getDecl();

        // Get class decl and constructor call arg types
        JavaType javaType = type.getDecl();
        if (javaType == null)
            return null;
        JavaClass javaClass = javaType.getEvalClass();
        JavaType[] argTypes = getArgEvalTypes();

        // If inner class and not static, add implied class type to arg types array
        if (javaClass.isMemberClass() && !javaClass.isStatic()) {
            JavaClass parentClass = javaClass.getDeclaringClass();
            argTypes = ArrayUtils.add(argTypes, parentClass, 0);
        }

        // Get scope node class type and search for compatible method for name and arg types
        JavaConstructor constructor = JavaClassUtils.getCompatibleConstructor(javaClass, argTypes);
        if (constructor != null)
            return constructor;

        // Return null since not found
        return null;
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()  { return "Allocation"; }

    /**
     * Override to provide errors for JStmtExpr.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        NodeError[] errors = NodeError.NO_ERRORS;

        // Handle can't resolve method
        JavaDecl classOrConstructor = getDecl();
        if (classOrConstructor == null) {
            String errorString = null;
            if (getArgs() != null) {
                String constrString = getConstructorString();
                errorString = "Can't resolve constructor: " + constrString;
            }
            else {
                JType type = getType();
                String className = type != null ? type.getName() : "Unknown";
                errorString = "Can't resolve type: " + className;
            }

            // Create error
            NodeError error = new NodeError(this, errorString);
            errors = ArrayUtils.add(errors, error);
        }

        // Return
        return errors;
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
        // Get arg types
        JavaType[] argTypes = getArgEvalTypes();
        if (argTypes.length == 0)
            return "()";

        // Get arg type string and join by comma
        String[] argTypeStrings = ArrayUtils.map(argTypes, type -> type != null ? type.getSimpleName() : "null", String.class);
        String argTypeString = StringUtils.join(argTypeStrings, ",");

        // Return in parens
        return '(' + argTypeString + ')';
    }
}