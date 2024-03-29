/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaType;
import javakit.resolver.ResolverUtils;
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
    private JavaType _baseType;

    // The JavaType
    protected JavaType _javaType;

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
        addChild(aType);
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
        return typeArg.getJavaType();
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
        // Handle 'var'
        if (isVarType())
            return getDeclForVar();

        // Handle primitive type
        String baseName = getName();
        Class<?> primitiveClass = ResolverUtils.getPrimitiveClassForName(baseName);
        if (primitiveClass != null)
            return getJavaClassForClass(primitiveClass);

        // Try to find class directly
        JavaType javaClass = getJavaClassForName(baseName);
        if (javaClass != null)
            return javaClass;

        // If not primitive, try to resolve class (type might be package name or unresolvable)
        JavaDecl packageOrClassDecl = getDeclForChildType(this);
        if (packageOrClassDecl instanceof JavaType)
            javaClass = (JavaType) packageOrClassDecl;

        // Return
        return javaClass;
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

        // If type args, build array and get decl for ParamType
        int typeArgCount = getTypeArgCount();
        if (typeArgCount > 0) {

            // Get type arg types
            JavaType[] typeArgTypes = new JavaType[typeArgCount];
            for (int i = 0; i < typeArgCount; i++) {
                JavaType typeArgType = typeArgTypes[i] = getTypeArgType(i);
                if (typeArgType == null)
                    return getJavaClassForClass(Object.class); // Do something better here!
            }

            // Get parameterized type
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
     * Override to get name from base expression.
     */
    @Override
    protected String getNameImpl()
    {
        JExpr baseExpr = getBaseExpr();
        return baseExpr != null ? baseExpr.getName() : null;
    }

    /**
     * Override to return JavaType.
     */
    @Override
    protected JavaType getDeclImpl()  { return getJavaType(); }

    /**
     * Override to handle 'var' types.
     */
    @Override
    protected JavaDecl getDeclForChildId(JExprId anExprId)
    {
        // Handle 'var'
        if (isVarType() && anExprId == _baseExpr)
            return getDeclForVar();

        // Do normal version
        return super.getDeclForChildId(anExprId);
    }

    /**
     * Special code for getting 'var' type.
     */
    private JavaType getDeclForVar()
    {
        // Get parent StmtVarDecl: 'var' is only allowed as statement (can't be field or formal param)
        JNode parentNode = getParent();
        JExprVarDecl varDeclExpr = parentNode instanceof JExprVarDecl ? (JExprVarDecl) parentNode : null;
        if (varDeclExpr == null)
            return null;

        // Get StmtVarDecl.VarDecl
        List<JVarDecl> varDecls = varDeclExpr.getVarDecls();
        JVarDecl varDecl = varDecls.size() > 0 ? varDecls.get(0) : null;
        if (varDecl == null)
            return null;

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
        JNode grandparentNode = parentNode.getParent();
        if (grandparentNode instanceof JStmtFor)
            return ((JStmtFor) grandparentNode).getForEachIterationType();

        // Return not found
        return null;
    }

    /**
     * Override to provide errors for this class.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        NodeError[] errors = NodeError.NO_ERRORS;

        // Handle unresolved type
        JavaType javaType = getJavaType();
        if (javaType == null) {
            String className = getName();
            String errorString = "Can't resolve type: " + className;
            if ("var".equals(className))
                errorString = "Cannot infer type: 'var' on variable without initializer";
            errors = NodeError.addError(errors,this, errorString, 0);
        }

        // Return
        return errors;
    }

    /**
     * Override to customize for this class.
     */
    @Override
    protected String createString()
    {
        return getName();
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
            type._javaType = aType;
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