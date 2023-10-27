/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
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

    // The array initializer expressions (if array)
    protected List<JExpr> _arrayInitExprs = Collections.EMPTY_LIST;

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
            JType type2 = JType.createTypeForTypeAndToken(_type.getEvalType(), _startToken);
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
     * Returns the array init expressions (if array).
     */
    public List<JExpr> getArrayInitExprs()  { return _arrayInitExprs; }

    /**
     * Sets the array init expressions (if array).
     */
    public void setArrayInitExprs(List<JExpr> theArrayInits)
    {
        // Remove old expressions if set
        if (_arrayInitExprs != null && _arrayInitExprs.size() > 0)
            _arrayInitExprs.forEach(expr -> removeChild(expr));

        // Set new
        _arrayInitExprs = theArrayInits;

        // Add new expressions
        if (_arrayInitExprs != null)
            _arrayInitExprs.forEach(expr -> addChild(expr));
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
                if (!varClass.isAssignableFrom(initClass))
                    return NodeError.newErrorArray(this, "Invalid assignment type");
            }
        }

        // If any array inits have error, return that
        List<JExpr> arrayInitExprs = getArrayInitExprs();
        for (JExpr arrayInitExpr : arrayInitExprs) {
            NodeError[] arrayInitExprErrors = arrayInitExpr.getErrors();
            if (arrayInitExprErrors.length > 0)
                return arrayInitExprErrors;
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
        JMemberDecl enclosingExec = aNode.getEnclosingMemberDecl();
        if (enclosingExec != null)
            enclosingPathStr += enclosingExec.getName() + '.';

        // Return TypeId.VarName
        return enclosingPathStr + aType.getId() + ' ' + aName;
    }
}