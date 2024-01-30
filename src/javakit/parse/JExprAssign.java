/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaDecl;

/**
 * A class to represent assignment expressions (including +=, -=, etc.).
 */
public class JExprAssign extends JExpr {

    // The left side expression
    private JExpr _leftSideExpr;

    // The operator
    private Op _op;

    // The assigned expression
    private JExpr _valueExpr;

    // Constants for op
    public enum Op {

        Assign ("="),
        Add ("+="), Subtract ("-="), Multiply ("*="), Divide ("/="), Mod ("%="),
        And ("&="), Or ("|="), Xor ("^="),
        ShiftLeft ("<<="), ShiftRight (">>="), ShiftRightUnsigned (">>>=");

        // The Op string
        private String  _string;

        /** Constructor. */
        Op(String aString)  { _string = aString; }

        /** Returns Op string. */
        String getString()  { return _string; }
    }

    /**
     * Constructor for given op string, target id expression and value expression.
     */
    public JExprAssign(String aString, JExpr aFirst, JExpr aSecond)
    {
        _op = getOpForString(aString);
        setLeftSideExpr(aFirst);
        if (aSecond != null)
            addChild(aSecond);
    }

    /**
     * Returns the op.
     */
    public Op getOp()  { return _op; }

    /**
     * Returns the assignee expression.
     */
    public JExpr getLeftSideExpr()  { return _leftSideExpr; }

    /**
     * Returns the assignee expression.
     */
    public void setLeftSideExpr(JExpr anExpr)
    {
        replaceChild(_leftSideExpr, _leftSideExpr = anExpr);
    }

    /**
     * Returns the value expression.
     */
    public JExpr getValueExpr()  { return _valueExpr; }

    /**
     * Sets the value expression.
     */
    public void setValueExpr(JExpr anExpr)
    {
        replaceChild(_valueExpr, _valueExpr = anExpr);
    }

    /**
     * Returns the class name for expression.
     */
    protected JavaDecl getDeclImpl()
    {
        JExpr target = getLeftSideExpr();
        return target.getEvalType();
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()
    {
        if (_op == Op.Assign)
            return "AssignExpr";
        return "Assign" + _op + "Expr";
    }

    /**
     * Override to check valid assignment type.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        // Get left side expression and errors - just return if errors found
        JExpr leftSideExpr = getLeftSideExpr();
        NodeError[] leftSideErrors = leftSideExpr.getErrors();
        if (leftSideErrors.length > 0)
            return leftSideErrors;

        // Get value expression and errors - just return if errors found
        JExpr valueExpr = getValueExpr();
        if (valueExpr == null)
            return NodeError.newErrorArray(this, "Missing assignment value");
        NodeError[] valueExprErrors = valueExpr.getErrors();
        if (valueExprErrors.length > 0)
            return valueExprErrors;

        // Get assign to class - return error if null (impossible since no left side errors)
        JavaClass assignToClass = leftSideExpr.getEvalClass();
        if (assignToClass == null)
            return NodeError.newErrorArray(this, "Can't resolve type: " + leftSideExpr.getName());

        // Get assign to class and value class - return error if no match
        JavaClass valueClass = valueExpr.getEvalClass();
        if (valueClass == null && assignToClass.isPrimitive())
            return NodeError.newErrorArray(this, "Incompatible types: <nulltype> cannot be converted to " + assignToClass.getName());
        if (!assignToClass.isAssignableFrom(valueClass))
            return NodeError.newErrorArray(this, "Invalid assignment type");

        // Return
        return super.getErrorsImpl();
    }

    /**
     * Returns the Op string for op.
     */
    public static Op getOpForString(String aString)
    {
        for (Op op : Op.values())
            if (op.getString().equals(aString))
                return op;
        throw new RuntimeException("JExprAssign: Unknown Op: " + aString);
    }
}