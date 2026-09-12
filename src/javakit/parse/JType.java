/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;
import snap.parse.ParseToken;
import snap.util.ArrayUtils;

/**
 * A JNode for types.
 */
public class JType extends JNode {

    // The scope
    private JType _scopeType;

    // The id
    private JExprId _id;

    // Whether type is primitive type
    protected boolean  _primitive;

    // Whether is reference (array or class/interface type)
    protected int  _arrayCount;

    // The generic Types
    private JType[]  _typeArgs = EMPTY_TYPES_ARRAY;

    // The wildcard bounds
    private JType _wildcardBounds;

    // The base type
    private JavaType _baseType;

    // The JavaType
    protected JavaType _javaType;

    // The package - if this type is really a package
    private JavaPackage _javaPackage;

    // Constant for empty types array
    public static final JType[] EMPTY_TYPES_ARRAY = new JType[0];

    /**
     * Constructor.
     */
    public JType()
    {
        super();
    }

    /**
     * Constructor.
     */
    public JType(JType scopeType, JExprId idExpr)
    {
        super();
        setScopeType(scopeType);
        setId(idExpr);
    }

    /**
     * Returns the scope type.
     */
    public JType getScopeType()  { return _scopeType; }

    /**
     * Sets the scope type.
     */
    public void setScopeType(JType scopeType)
    {
        replaceChild(_scopeType, _scopeType = scopeType);
    }

    /**
     * Returns the type identifier.
     */
    public JExprId getId()  { return _id; }

    /**
     * Sets the type identifier.
     */
    public void setId(JExprId idExpr)
    {
        replaceChild(_id, _id = idExpr);
        if (_id != null && (_id.getName().equals("java") ||_id.getName().equals("com") || _id.getName().equals("snap")))
            System.currentTimeMillis();
    }

    /**
     * Returns whether type is 'var'.
     */
    public boolean isVarType()
    {
        String name = getName();
        return name.equals("var");
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
     * Returns the type args.
     */
    public JType[] getTypeArgs()  { return _typeArgs; }

    /**
     * Sets the type args.
     */
    public void setTypeArgs(JType[] theTypeArgs)
    {
        _typeArgs = theTypeArgs;
        for (JType typeArg : theTypeArgs)
            addChild(typeArg);
    }

    /**
     * Returns the type arg types.
     */
    public JavaType[] getTypeArgTypes()
    {
        if (_typeArgs == EMPTY_TYPES_ARRAY) return JavaType.EMPTY_TYPES_ARRAY;
        return ArrayUtils.map(_typeArgs, jtyp -> getJavaTypeForTypeArg(jtyp), JavaType.class);
    }

    /**
     * Returns whether type is wildcard type.
     */
    public boolean isWildcardType()  { return getName().equals("?"); }

    /**
     * Returns the wildcard bounds.
     */
    public JType getWildcardBounds()  { return _wildcardBounds; }

    /**
     * Sets the wildcard bounds.
     */
    public void setWildcardBounds(JType wildcardBounds)
    {
        replaceChild(_wildcardBounds, _wildcardBounds = wildcardBounds);
    }

    /**
     * Returns the wildcard bounds type.
     */
    private JavaType getWildcardBoundsType()
    {
        if (_wildcardBounds != null)
            return _wildcardBounds.getJavaType();
        return getJavaClassForName("java.lang.Object");
    }

    /**
     * Returns the simple name.
     */
    public String getSimpleName()  { return _id != null ? _id.getName() : null; }

    /**
     * Returns the base type.
     */
    protected JavaType getBaseType()
    {
        if (_baseType != null) return _baseType;
        return _baseType = getBaseTypeImpl();
    }

    /**
     * Returns the base type.
     */
    private JavaType getBaseTypeImpl()
    {
        String simpleName = getSimpleName();

        // Try to resolve with scope type
        JType scopeType = getScopeType();
        if (scopeType != null) {
            JavaDecl scopeDecl = scopeType.getDecl();
            if (scopeDecl instanceof JavaType) {
                JavaClass scopeClass = scopeType.getJavaClass();
                return scopeClass != null ? scopeClass.getClassForName(simpleName) : null;
            }
            else if (scopeDecl instanceof JavaPackage scopePackage) {
                JavaClass baseClass = scopePackage.getClassForName(simpleName);
                if (baseClass != null)
                    return baseClass;
            }
            return null;
        }

        // Handle 'var'
        if (isVarType())
            return getDeclForVar();

        // Handle wildcard
        if (isWildcardType())
            return getWildcardBoundsType();

        // If parent is parameterized type, see if name is nested TypeArg from class extends/implements (e.g.: public class XXX extends List<E>)
        if (getParent() instanceof JType parentType && parentType.getScopeType() != this && parentType._typeArgs != EMPTY_TYPES_ARRAY) {
            JavaClass baseClass = parentType.getBaseClass();
            if (baseClass != null) {
                JavaTypeVariable typeVarType = baseClass.getTypeParameterForName(simpleName);
                if (typeVarType != null)
                    return typeVarType;
            }
        }

        // Try to resolve from parents (maybe import class, inner class, method/class type arg, etc.)
        return ResolveDeclForChildType.getJavaTypeForChildType(this);
    }

    /**
     * Returns the JavaType.
     */
    public JavaType getJavaType()
    {
        if (_javaType != null) return _javaType;
        return _javaType = getJavaTypeImpl();
    }

    /**
     * Returns the JavaType.
     */
    private JavaType getJavaTypeImpl()
    {
        // Get base decl
        JavaType javaType = getBaseType();
        if (javaType == null)
            return null;

        // If type args, get ParameterizedType for types
        JavaType[] typeArgTypes = getTypeArgTypes();
        if (typeArgTypes.length > 0) {
            JavaClass javaClass = (JavaClass) javaType;
            javaType = javaClass.getParameterizedTypeForTypes(typeArgTypes);
        }

        // If ArrayCount, get decl for array
        for (int i = 0; i < _arrayCount; i++)
            javaType = javaType.getArrayType();

        // Return
        return javaType;
    }

    /**
     * Returns the JavaClass.
     */
    public JavaClass getJavaClass()
    {
        // Get base class
        JavaClass javaClass = getBaseClass();

        // If ArrayCount, get decl for array
        for (int i = 0; i < _arrayCount; i++)
            javaClass = javaClass.getArrayType();

        // Return
        return javaClass;
    }

    /**
     * Returns the base type.
     */
    protected JavaClass getBaseClass()
    {
        // Bogus - Look for TypeVar and use BoundsClass. Otherwise getBaseType() below can stack overflow
        String baseName = getName();
        for (JNode parent = _parent; parent != null; parent = parent.getParent()) {
            if (parent instanceof WithTypeParameters) {
                JTypeVar typeVar = ((WithTypeParameters) parent).getTypeParamDeclForName(baseName);
                if (typeVar != null)
                    return typeVar.getBoundsClass();
            }
        }

        // Look for normal type
        JavaType javaType = getBaseType();
        if (javaType != null)
            return javaType.getEvalClass();

        // Return java.lang.Object
        System.err.println("JType.getBaseClass: Couldn't find class for name: " + getName());
        return getJavaClassForName("java.lang.Object");
    }

    /**
     * Returns the package, if this type is really a package.
     */
    public JavaPackage getJavaPackage()
    {
        if (_javaPackage != null) return _javaPackage;
        return _javaPackage = getJavaPackageImpl();
    }

    /**
     * Returns the package, if this type is really a package.
     */
    private JavaPackage getJavaPackageImpl()
    {
        String simpleName = getSimpleName();
        JType scopeType = getScopeType();
        if (scopeType != null) {
            JavaDecl scopeDecl = scopeType.getDecl();
            return scopeDecl instanceof JavaPackage scopePackage ? scopePackage.getPackageForName(simpleName) : null;
        }
        return getJavaPackageForName(simpleName);
    }

    /**
     * Override to get name from base expression.
     */
    @Override
    protected String getNameImpl()
    {
        JExprId idExpr = getId();
        String name = idExpr.getName();
        JType scopeType = getScopeType();
        if (scopeType != null)
            name = scopeType.getName() + "." + name;
        return name;
    }

    /**
     * Override to return JavaType.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        JavaType javaType = getJavaType();
        if (javaType != null)
            return javaType;
        return getJavaPackage();
    }

    /**
     * Special code for getting 'var' type.
     */
    private JavaType getDeclForVar()
    {
        // Get StmtVarDecl.VarDecl
        JVarDecl varDecl = getParentVarDecl();
        if (varDecl == null)
            return null;

        // If parent.parent is Lambda expression, get type for var decl
        JNode parentNode = getParent();
        JNode grandparentNode = parentNode.getParent();
        if (grandparentNode instanceof JExprLambda lambdaExpr)
            return lambdaExpr.getJavaTypeForLambdaParameterVarDecl(varDecl);

        // If initializer expression set, return its EvalType
        JExpr initExpr = varDecl.getInitExpr();
        if (initExpr != null) {

            // If expression is just array init, return null - no type info there (will stack overflow)!
            if (initExpr instanceof JExprArrayInit)
                return null;

            // Return
            return initExpr.getEvalType();
        }

        // If parentNode.parent is ForEachStmt, get iterable type
        if (grandparentNode instanceof JStmtFor forStmt)
            return forStmt.getForEachIterationType();

        // Return not found
        return null;
    }

    /**
     * Return parent var decl for type.
     */
    private JVarDecl getParentVarDecl()
    {
        // If parent is var decl, return it (probably Lambda expression)
        JNode parentNode = getParent();
        if (parentNode instanceof JVarDecl)
            return (JVarDecl) parentNode;

        // If parent is var decl expression, return first var decl
        if (parentNode instanceof JExprVarDecl varDeclExpr) {
            JVarDecl[] varDecls = varDeclExpr.getVarDecls();
            return varDecls.length > 0 ? varDecls[0] : null;
        }

        // Return not found
        return null;
    }

    /**
     * Override to customize for this class.
     */
    @Override
    protected String createString()  { return getName(); }

    /**
     * Returns the JavaType for given type, using java.lang.Object if not found.
     */
    private static JavaType getJavaTypeForTypeArg(JType aType)
    {
        JavaType javaType = aType.getJavaType();
        return javaType != null ? javaType : aType.getJavaClassForName("java.lang.Object");
    }

    /**
     * Creates a type for type and token.
     */
    public static JType createTypeForTypeAndToken(JavaType aType, ParseToken aToken)
    {
        String typeName = aType.getName();
        String[] idStrings = typeName.split("\\.");
        JType type = null;

        // Create/add ids for name
        for (String idStr : idStrings) {
            type = new JType(type, null);
            type._startToken = type._endToken = aToken;
            type._javaType = aType;
            type._primitive = aType.isPrimitive();
            JExprId id = new JExprId(idStr);
            id._startToken = id._endToken = aToken;
            type.setId(id);
        }

        return type;
    }
}