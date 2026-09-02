/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaType;

/**
 * An class to represent expressions that include an operator (math, logical, etc.).
 */
public class JExprMath extends JExpr {

    // The operator
    private Op _op;

    // Constants for op
    public enum Op {

        // Binary numeric math
        Add(2), Subtract(2),
        Multiply(2), Divide(2), Mod(2),

        // Binary numeric compare
        Equal(2), NotEqual(2),
        LessThan(2), GreaterThan(2),
        LessThanOrEqual(2), GreaterThanOrEqual(2),

        // Binary logical
        Or(2), And(2), Not(1),

        // Bitwise binary math
        BitOr(2), BitXOr(2), BitAnd(2),

        // Conditional
        Conditional(3),

        // Bitwise shift
        ShiftLeft(2), ShiftRight(2), ShiftRightUnsigned(2),

        // Unary
        PreIncrement(1), PreDecrement(1),
        PostIncrement(1), PostDecrement(1),

        // Unary
        Negate(1), BitComp(1);

        // Operand count
        private final int _operandCount;

        // Constructor
        Op(int operandCount)  { _operandCount = operandCount; }

        // Returns the number of operands
        public int getOperandCount() { return _operandCount; }
    }

    /**
     * Constructor for given op and LeftHand expression.
     */
    public JExprMath(Op anOp)
    {
        _op = anOp;
    }

    /**
     * Constructor for given op and LeftHand expression.
     */
    public JExprMath(Op anOp, JExpr aFirst)
    {
        _op = anOp;
        if (aFirst != null)
            addOperand(aFirst);
    }

    /**
     * Constructor for given op and LeftHand/RightHand expressions.
     */
    public JExprMath(Op anOp, JExpr aFirst, JExpr aSecond)
    {
        _op = anOp;
        addOperand(aFirst);
        addOperand(aSecond);
    }

    /**
     * Returns the op.
     */
    public Op getOp()  { return _op; }

    /**
     * Returns the operand count.
     */
    public int getOperandCount()
    {
        return getChildCount();
    }

    /**
     * Returns the specified operand.
     */
    public JExpr getOperand(int anIndex)
    {
        return (JExpr) getChild(anIndex);
    }

    /**
     * Adds an operand.
     */
    public void addOperand(JExpr anExpr)
    {
        addChild(anExpr);
    }

    /**
     * Sets the specified operand.
     */
    public void setOperand(JExpr anExpr, int anIndex)
    {
        if (anIndex < getChildCount())
            replaceChild(getChild(anIndex), anExpr);
        else addChild(anExpr);
    }

    /**
     * Returns the class name for expression.
     */
    protected JavaDecl getDeclImpl()
    {
        // If missing operands, return error
        int opCountActual = getOperandCount();
        int opCountExpected = _op.getOperandCount();
        if (opCountActual < opCountExpected)
            return null;

        return switch (_op) {

            // Handle binary numeric math ops
            case Add, Subtract, Multiply, Divide, Mod -> getEvalTypeMath();

            // Handle binary numeric compare ops
            // Handle binary/unary logic
            case Equal, NotEqual, LessThan, GreaterThan, LessThanOrEqual, GreaterThanOrEqual, Or, And, Not ->
                    getJavaClassForClass(boolean.class);

            // Handle conditional
            case Conditional -> getEvalTypeConditional();

            // Handle binary bitwise ops
            case BitOr, BitXOr, BitAnd, ShiftLeft, ShiftRight, ShiftRightUnsigned -> getOperand(0).getEvalType();

            // Handle unary ops
            case PreIncrement, PreDecrement, PostIncrement, PostDecrement, Negate, BitComp -> getOperand(0).getEvalType();
        };
    }

    /**
     * Returns the class name for math expression.
     */
    private JavaDecl getEvalTypeMath()
    {
        // Get operand eval types (just return if either is null)
        int childCount = getChildCount();
        JavaType evalType1 = childCount > 0 ? getOperand(0).getEvalType() : null;
        JavaType evalType2 = childCount > 1 ? getOperand(1).getEvalType() : null;
        if (evalType1 == null || evalType1 == evalType2)
            return evalType2;
        if (evalType2 == null)
            return evalType1;

        // Handle promotions: String, Double, Float, Long, Int
        String c1 = evalType1.getClassName();
        String c2 = evalType2.getClassName();
        if (isString(c1)) return evalType1;
        if (isString(c2)) return evalType2;
        if (isDouble(c1)) return evalType1;
        if (isDouble(c2)) return evalType2;
        if (isFloat(c1)) return evalType1;
        if (isFloat(c2)) return evalType2;
        if (isLong(c1)) return evalType1;
        if (isLong(c2)) return evalType2;
        if (isInt(c1)) return evalType1;
        if (isInt(c2)) return evalType2;
        return evalType1;
    }

    /**
     * Returns whether type names are numbers.
     */
    private boolean isString(String aName)
    {
        return aName.equals("java.lang.String");
    }

    private boolean isDouble(String aName)
    {
        return aName.equals("double") || aName.equals("java.lang.Double");
    }

    private boolean isFloat(String aName)
    {
        return aName.equals("float") || aName.equals("java.lang.Float");
    }

    private boolean isLong(String aName)
    {
        return aName.equals("long") || aName.equals("java.lang.Long");
    }

    private boolean isInt(String aName)
    {
        return aName.equals("int") || aName.equals("java.lang.Integer");
    }

    /**
     * Returns common ancestor of conditional true/false expressions.
     */
    private JavaType getEvalTypeConditional()
    {
        // If both true/false expressions not set, just bail
        if (getChildCount() < 3)
            return getJavaClassForClass(Object.class);

        // Get true/false expressions and eval types
        JExpr trueExpr = getOperand(1);
        JExpr falseExpr = getOperand(2);
        JavaType trueExprType = trueExpr.getEvalType();
        JavaType falseExprType = falseExpr.getEvalType();

        // If either evals to null, use the other
        if (trueExprType == null)
            return falseExprType;
        if (falseExprType == null)
            return trueExprType;

        // Return common type between true/false types
        return trueExprType.getCommonAncestor(falseExprType);
    }

    /**
     * Override to customize for math expression.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        // If missing operands, return error
        int opCountActual = getOperandCount();
        int opCountExpected = _op.getOperandCount();
        if (opCountActual < opCountExpected)
            return NodeError.newErrorArray(this, "Missing operand");

        // Do normal version
        return super.getErrorsImpl();
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()  { return _op + "Expr"; }

    /**
     * Returns the Op string for op.
     */
    public static String getOpString(Op anOp)
    {
        return switch (anOp) {
            case Add -> "+";
            case Subtract, Negate -> "-";
            case Multiply -> "*";
            case Divide -> "/";
            case Mod -> "%";
            case Equal -> "==";
            case NotEqual -> "!=";
            case LessThan -> "<";
            case GreaterThan -> ">";
            case LessThanOrEqual -> "<=";
            case GreaterThanOrEqual -> ">=";
            case Or -> "||";
            case And -> "&&";
            case Not -> "!";
            case BitOr -> "|";
            case BitXOr -> "^";
            case BitAnd -> "&";
            case Conditional -> "?";
            case ShiftLeft -> "<<";
            case ShiftRight -> ">>";
            case ShiftRightUnsigned -> ">>>";
            case PreIncrement -> "++";
            case PreDecrement -> "--";
            case BitComp -> "<DUNNO>";
            case PostIncrement -> "++";
            case PostDecrement -> "--";
        };
    }
}