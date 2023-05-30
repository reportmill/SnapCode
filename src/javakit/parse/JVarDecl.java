/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import javakit.resolver.*;
import snap.util.ListUtils;

/**
 * A JNode to represent a defined variable.
 * Found in JFieldDecl, JStmtVarDecl, Method/Catch FormalParam(s)
 */
public class JVarDecl extends JNode {

    // The type
    protected JType  _type;

    // The variable name
    protected JExprId  _id;

    // The variable dimension (if defined with variable instead of type)
    protected int  _arrayCount;

    // The initializer
    protected JExpr  _initializer;

    // The array initializer (if array)
    protected List<JExpr>  _arrayInits = Collections.EMPTY_LIST;

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
            JType type2 = new JType.Builder().name(_type._name).token(_startToken).type(_type.getEvalType()).build();
            type2._arrayCount = _type._arrayCount + _arrayCount;
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
        JNode par = getParent();
        if (par instanceof JFieldDecl)
            return ((JFieldDecl) par).getType();

        // Handle StatementVarDecl VarDecl: Return type from parent
        if (par instanceof JStmtVarDecl)
            return ((JStmtVarDecl) par).getType();

        // Handle JExprLambda VarDecl: Get decl for this param and create new type
        if (par instanceof JExprLambda) {

            // Get decl for this param (resolve if needed)
            JExprLambda lambda = (JExprLambda) par;
            JavaMethod lambdaMethod = lambda.getMethod();
            if (lambdaMethod == null)
                return null;

            // Get index
            List<JVarDecl> lambdaParams = lambda.getParams();
            int argIndex = ListUtils.indexOfId(lambdaParams, this);
            if (argIndex < 0 || argIndex >= lambdaMethod.getParamCount())
                return null;

            // Get param type for lambda arg index
            JavaType paramType = lambdaMethod.getParamType(argIndex);
            if (!paramType.isResolvedType()) {
                JavaType lambdaType = lambda.getDecl();
                paramType = lambdaType.getResolvedType(paramType);
            }

            // Create type for type decl and return
            JType type = new JType.Builder().type(paramType).token(_startToken).build();
            type._parent = this;
            return type;
        }

        // Return unknown
        return null;
    }

    /**
     * Returns the identifier.
     */
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
     * Returns the initializer.
     */
    public JExpr getInitializer()  { return _initializer; }

    /**
     * Sets the initializer.
     */
    public void setInitializer(JExpr anExpr)
    {
        replaceChild(_initializer, _initializer = anExpr);
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
     * Tries to resolve the class declaration for this node.
     */
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
            JavaClass enclosingClass = enclosingClassDecl.getDecl();
            if (enclosingClass == null)
                return null;
            JavaField field = enclosingClass.getFieldForName(name);
            return field;
        }

        // Get the eval type
        JType varDeclType = getType();
        JavaType evalType = varDeclType != null ? varDeclType.getDecl() : null;
        if (evalType == null) // Can happen for Lambdas
            evalType = getJavaClassForClass(Object.class);

        // Otherwise, return JavaLocalVar for Name/EvalType (UniqueId might help for debugging)
        Resolver resolver = getResolver();
        String uniqueId = getUniqueId(this, name, evalType);
        return new JavaLocalVar(resolver, name, evalType, uniqueId);
    }

    /**
     * Override to resolve id node.
     */
    protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
    {
        // VarDecl.Id evaluates same as whole
        if (anExprId == _id)
            return getDecl();

        // Do normal version
        return super.getDeclForChildExprIdNode(anExprId);
    }

    /**
     * Override to avoid NPE when parsing incomplete method def.
     */
    @Override
    protected String getNameImpl()
    {
        return "";
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
        JMemberDecl enclosingExec = aNode.getEnclosingMemberDecl();
        if (enclosingExec != null)
            enclosingPathStr += enclosingExec.getName() + '.';

        // Return TypeId.VarName
        return enclosingPathStr + aType.getId() + ' ' + aName;
    }
}