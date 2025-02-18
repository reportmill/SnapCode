/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.*;

/**
 * A JNode to represent a defined variable.
 * Found in JFieldDecl, JStmtVarDecl, Method/Catch FormalParam(s)
 */
public class JVarDecl extends JNode implements WithId {

    // The type
    protected JType  _type;

    // The variable name
    protected JExprId  _id;

    // The variable dimension (if defined with variable instead of type)
    private int _arrayCount;

    // The initializer expression
    private JExpr _initExpr;

    /**
     * Constructor.
     */
    public JVarDecl()
    {
        super();
    }

    /**
     * Returns the type.
     */
    public JType getType()
    {
        if (_type != null) return _type;

        // Get parent type from JFieldDecl, JStmtVarDecl
        _type = getParentType();
        if (_type == null)
            return null;

        // If array count is set, replace with type to account for it
        if (_arrayCount > 0) {
            JavaType javaType = _type.getJavaType();
            JType type2 = JType.createTypeForTypeAndToken(javaType, _startToken);
            type2._arrayCount = _type._arrayCount + _arrayCount;
            for (int i = 0; i < _arrayCount && type2._javaType != null; i++)
                type2._javaType = type2._javaType.getArrayType();
            _type = type2;
            _type._parent = this;
        }

        // Return
        return _type;
    }

    /**
     * Sets the type.
     */
    public void setType(JType aType)
    {
        replaceChild(_type, _type = aType);
    }

    /**
     * Returns the parent type (JFieldDecl, JStmtVarDecl).
     */
    private JType getParentType()
    {
        // Handle FieldDecl.VarDecl: return type from parent
        JNode parentNode = getParent();
        if (parentNode instanceof JFieldDecl)
            return ((JFieldDecl) parentNode).getType();

        // Handle ExprVarDecl VarDecl: Return type from parent
        if (parentNode instanceof JExprVarDecl)
            return ((JExprVarDecl) parentNode).getType();

        // Handle JExprLambda VarDecl: Get decl for this param and create new type
        if (parentNode instanceof JExprLambda)
            return ((JExprLambda) parentNode).createTypeNodeForLambdaParameterVarDecl(this);

        // Return unknown
        return null;
    }

    /**
     * Returns the identifier.
     */
    @Override
    public JExprId getId()  { return _id; }

    /**
     * Sets the identifier.
     */
    public void setId(JExprId anId)
    {
        replaceChild(_id, _id = anId);
        if (_id != null)
            setName(_id.getName());
    }

    /**
     * Returns the array count.
     */
    public int getArrayCount()  { return _arrayCount; }

    /**
     * Sets the array count.
     */
    public void setArrayCount(int aValue)
    {
        _arrayCount = aValue;
    }

    /**
     * Returns the initializer expression.
     */
    public JExpr getInitExpr()  { return _initExpr; }

    /**
     * Sets the initializer expression.
     */
    public void setInitExpr(JExpr anExpr)
    {
        replaceChild(_initExpr, _initExpr = anExpr);
    }

    /**
     * Returns the declaring class, if field variable.
     */
    public JavaClass getDeclaringClass()
    {
        // If Field, return Declaring class
        boolean isField = getParent() instanceof JFieldDecl;
        if (isField) {
            JClassDecl classDecl = getParent(JClassDecl.class);
            return classDecl.getEvalClass();
        }

        // Return
        return null;
    }

    /**
     * Returns the Java type.
     */
    public JavaType getJavaType()
    {
        JType type = getType();
        return type != null ? type.getJavaType() : null;
    }

    /**
     * Returns the Java class.
     */
    public JavaClass getJavaClass()
    {
        JType type = getType();
        return type != null ? type.getJavaClass() : getJavaClassForName("java.lang.Object");
    }

    /**
     * Tries to resolve the class declaration for this node.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        // Get name - if not set, just bail
        String name = getName();
        if (name == null)
            return null;

        // If part of a JFieldDecl, get JavaDecl for field
        JNode par = getParent();
        if (par instanceof JFieldDecl) {
            JClassDecl enclosingClassDecl = getEnclosingClassDecl();
            if (enclosingClassDecl == null)
                return null;
            JavaClass enclosingClass = enclosingClassDecl.getJavaClass();
            if (enclosingClass == null)
                return null;
            return enclosingClass.getDeclaredFieldForName(name);
        }

        // Get the eval type
        JavaType evalType = getJavaType();
        if (evalType == null) // Can happen for Lambdas
            evalType = getJavaClassForClass(Object.class);
        if (evalType == null) // Can happen when not in project (like diff file)
            return null;

        // Otherwise, return JavaLocalVar for Name/EvalType (UniqueId might help for debugging)
        Resolver resolver = getResolver();
        String uniqueId = getUniqueId(this, name, evalType);
        return new JavaLocalVar(resolver, name, evalType, uniqueId);
    }

    /**
     * Override to try to resolve from type.
     */
    @Override
    protected JavaType getResolvedTypeForTypeVar(JavaTypeVariable aTypeVar)
    {
        // If VarDecl type is parameterized type, try to resolve given type var
        JavaType javaType = getJavaType();
        if (javaType != null) {
            JavaType resolvedType = javaType.getResolvedTypeForTypeVariable(aTypeVar);
            if (resolvedType != null)
                return resolvedType;
        }

        // Do normal version
        return super.getResolvedTypeForTypeVar(aTypeVar);
    }

    /**
     * Override to avoid NPE when parsing incomplete method def.
     */
    @Override
    protected String getNameImpl()  { return ""; }

    /**
     * Override to check valid assignment type.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        // If not var and type has errors, return them
        JType type = getType();
        if (type == null)
            return NodeError.newErrorArray(this, "Missing type");
        if (!type.isVarType()) {
            NodeError[] typeErrors = type.getErrors();
            if (typeErrors.length > 0)
                return typeErrors;
        }

        // If initializer expression set, check type
        JExpr initExpr = getInitExpr();
        if (initExpr != null) {

            // If initializer has errors, just return them
            NodeError[] initializerErrors = initExpr.getErrors();
            if (initializerErrors.length > 0)
                return initializerErrors;

            // If invalid assign type, return error
            JavaClass varClass = getEvalClass();
            if (varClass != null) {
                JavaClass initClass = initExpr.getEvalClass();
                if (initClass == null && varClass.isPrimitive())
                    return NodeError.newErrorArray(this, "Incompatible types: <nulltype> cannot be converted to " + varClass.getName());
                if (!varClass.isAssignableFrom(initClass))
                    return NodeError.newErrorArray(this, "Invalid assignment type");
            }
        }

        // If type has errors, just return it
        NodeError[] typeErrors = type.getErrors();
        if (typeErrors.length > 0)
            return typeErrors;

        // Return normal version
        return super.getErrorsImpl();
    }

    /**
     * Returns an identifier string describing where this variable declaration is defined.
     */
    protected static String getUniqueId(JNode aNode, String aName, JavaType aType)
    {
        // Get enclosing class string
        String enclosingPathStr = "";
        JClassDecl enclosingClass = aNode.getEnclosingClassDecl();
        if (enclosingClass != null)
            enclosingPathStr = enclosingClass.getName() + '.';

        // Add enclosing method
        JMemberDecl enclosingExec = aNode.getParent(JMemberDecl.class);
        if (enclosingExec != null)
            enclosingPathStr += enclosingExec.getName() + '.';

        // Return TypeId.VarName
        return enclosingPathStr + aType.getId() + ' ' + aName;
    }
}