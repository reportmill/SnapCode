/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaType;
import snap.parse.ParseToken;
import snap.util.ClassUtils;

/**
 * A JNode for types.
 */
public class JType extends JNode {

    // The base expression
    private JExpr _baseExpr;

    // Whether type is primitive type
    protected boolean  _primitive;

    // Whether is reference (array or class/interface type)
    protected int  _arrayCount;

    // The generic Types
    private List<JType>  _typeArgs;

    // The base type
    private JavaType  _baseDecl;

    /**
     * Constructor.
     */
    public JType()
    {
        super();
    }

    /**
     * Returns the base expression.
     */
    public JExpr getBaseExpr()  { return _baseExpr; }

    /**
     * Sets the base expression.
     */
    public void setBaseExpr(JExpr anExpr)
    {
        replaceChild(_baseExpr, _baseExpr = anExpr);
    }

    /**
     * Adds an identifier.
     */
    public void addId(JExprId anId)
    {
        JExpr baseExpr = anId;
        if (_baseExpr != null)
            baseExpr = new JExprDot(_baseExpr, anId);
        setBaseExpr(baseExpr);
    }

    /**
     * Returns whether type is primitive type.
     */
    public boolean isPrimitive()  { return _primitive; }

    /**
     * Sets whether type is primitive type.
     */
    public void setPrimitive(boolean aValue)  { _primitive = aValue; }

    /**
     * Returns whether type is array.
     */
    public boolean isArrayType()  { return _arrayCount > 0; }

    /**
     * Returns the array count if array type.
     */
    public int getArrayCount()  { return _arrayCount; }

    /**
     * Sets the array count.
     */
    public void setArrayCount(int aValue)  { _arrayCount = aValue; }

    /**
     * Returns the generic types.
     */
    public List<JType> getTypeArgs()  { return _typeArgs; }

    /**
     * Adds a type arg.
     */
    public void addTypeArg(JType aType)
    {
        if (_typeArgs == null) _typeArgs = new ArrayList<>();
        _typeArgs.add(aType);
        addChild(aType, -1);
    }

    /**
     * Returns the number of type args.
     */
    public int getTypeArgCount()  { return _typeArgs != null ? _typeArgs.size() : 0; }

    /**
     * Returns the type arg type at given index.
     */
    public JType getTypeArg(int anIndex)  { return _typeArgs.get(anIndex); }

    /**
     * Returns the type arg decl at given index.
     */
    public JavaType getTypeArgType(int anIndex)
    {
        JType typeArg = getTypeArg(anIndex);
        return typeArg.getDecl();
    }

    /**
     * Returns the simple name.
     */
    public String getSimpleName()
    {
        String name = getName();
        int index = name.lastIndexOf('.');
        return index > 0 ? name.substring(index + 1) : name;
    }

    /**
     * Override to get name from base expression.
     */
    @Override
    protected String getNameImpl()
    {
        JExpr baseExpr = getBaseExpr();
        return baseExpr != null ? baseExpr.getName() : null;
    }

    /**
     * Override to return as JavaType.
     */
    @Override
    public JavaType getDecl()
    {
        JavaType javaType = (JavaType) super.getDecl();
        return javaType;
    }

    /**
     * Override to resolve type class name and create declaration from that.
     */
    protected JavaType getBaseDecl()
    {
        // If already set, just return
        if (_baseDecl != null) return _baseDecl;

        // Handle primitive type
        String baseName = getName();
        Class<?> primitiveClass = ClassUtils.getPrimitiveClassForName(baseName);
        if (primitiveClass != null)
            return _baseDecl = getJavaClassForClass(primitiveClass);

        // Try to find class directly
        JavaType javaClass = getJavaClassForName(baseName);
        if (javaClass != null)
            return _baseDecl = javaClass;

        // If not primitive, try to resolve class (type might be package name or unresolvable)
        JavaDecl packageOrClassDecl = getDeclForChildTypeNode(this);
        if (packageOrClassDecl instanceof JavaType)
            javaClass = (JavaType) packageOrClassDecl;

        // Set/return
        return _baseDecl = javaClass;
    }

    /**
     * Override to resolve type class name and create declaration from that.
     */
    protected JavaType getDeclImpl()
    {
        // Get base decl
        JavaType javaType = getBaseDecl();
        if (javaType == null)
            return null;

        // If type args, build array and get decl for ParamType
        int typeArgCount = getTypeArgCount();
        if (typeArgCount > 0) {
            JavaType[] typeArgTypes = new JavaType[typeArgCount];
            for (int i = 0; i < typeArgCount; i++)
                typeArgTypes[i] = getTypeArgType(i);
            javaType = javaType.getParamTypeDecl(typeArgTypes);
        }

        // If ArrayCount, get decl for array
        for (int i = 0; i < _arrayCount; i++)
            javaType = javaType.getArrayType();

        // Return
        return javaType;
    }

    /**
     * Override to provide errors for this class.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        NodeError[] errors = NodeError.NO_ERRORS;

        // Handle unresolved type
        JavaType typeClass = getDecl();
        if (typeClass == null) {
            String className = getName();
            String errorString = "Can't resolve type: " + className;
            errors = new NodeError[] { new NodeError(this, errorString) };
        }

        // Return
        return errors;
    }

    /**
     * Creates a type for type and token.
     */
    public static JType createTypeForTypeAndToken(JavaType aType, ParseToken aToken)
    {
        return createTypeForTypeAndNameAndToken(aType, null, aToken);
    }

    /**
     * Creates a type for name and token.
     */
    public static JType createTypeForNameAndToken(String aName, ParseToken aToken)
    {
        return createTypeForTypeAndNameAndToken(null, aName, aToken);
    }

    /**
     * Creates a type for name and token.
     */
    private static JType createTypeForTypeAndNameAndToken(JavaType aType, String aName, ParseToken aToken)
    {
        JType type = new JType();
        type._startToken = type._endToken = aToken;
        if (aType != null) {
            type._decl = aType;
            type._primitive = aType.isPrimitive();
            aName = aType.getName();
        }

        // Create/add ids for name
        String[] idStrings = aName.split("\\.");
        for (String idStr : idStrings) {
            JExprId id = new JExprId(idStr);
            id._startToken = id._endToken = aToken;
            type.addId(id);
        }

        // Return
        return type;
    }
}