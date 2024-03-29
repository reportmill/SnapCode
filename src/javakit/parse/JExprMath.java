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
        private int _operandCount;

        // Constructor
        Op(int operandCount)  { _operandCount = operandCount; }

        // Returns the number of operands
        public int getOperandCount() { return _operandCount; }
    }

    /**
     * Creates a new expression.
     */
    public JExprMath()
    {
    }

    /**
     * Creates a new expression for given op and LeftHand expression.
     */
    public JExprMath(Op anOp)
    {
        _op = anOp;
    }

    /**
     * Creates a new expression for given op and LeftHand expression.
     */
    public JExprMath(Op anOp, JExpr aFirst)
    {
        _op = anOp;
        if (aFirst != null)
            addOperand(aFirst);
    }

    /**
     * Creates a new expression for given op and LeftHand/RightHand expressions.
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

        switch (_op) {

            // Handle binary numeric math ops
            case Add:
            case Subtract:
            case Multiply:
            case Divide:
            case Mod: return getEvalTypeMath();

            // Handle binary numeric compare ops
            case Equal:
            case NotEqual:
            case LessThan:
            case GreaterThan:
            case LessThanOrEqual:
            case GreaterThanOrEqual:

            // Handle binary/unary logic
            case Or:
            case And:
            case Not: return getJavaClassForClass(boolean.class);

            // Handle conditional
            case Conditional: return getEvalTypeConditional();

            // Handle binary bitwise ops
            case BitOr:
            case BitXOr:
            case BitAnd:
            case ShiftLeft:
            case ShiftRight:
            case ShiftRightUnsigned: return getOperand(0).getEvalType();

            // Handle unary ops
            case PreIncrement:
            case PreDecrement:
            case PostIncrement:
            case PostDecrement:
            case Negate:
            case BitComp: return getOperand(0).getEvalType();

            // Handle bogus
            default: return getJavaClassForClass(boolean.class);
        }
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
        switch (anOp) {
            case Add: return "+";
            case Subtract: return "-";
            case Multiply: return "*";
            case Divide: return "/";
            case Mod: return "%";
            case Equal: return "==";
            case NotEqual: return "!=";
            case LessThan: return "<";
            case GreaterThan: return ">";
            case LessThanOrEqual: return "<=";
            case GreaterThanOrEqual: return ">=";
            case Or: return "||";
            case And: return "&&";
            case Not: return "!";
            case BitOr: return "|";
            case BitXOr: return "^";
            case BitAnd: return "&";
            case Conditional: return "?";
            case ShiftLeft: return "<<";
            case ShiftRight: return ">>";
            case ShiftRightUnsigned: return ">>>";
            case PreIncrement: return "++";
            case PreDecrement: return "--";
            case Negate: return "-";
            case BitComp: return "<DUNNO>";
            case PostIncrement: return "++";
            case PostDecrement: return "--";
            default: throw new RuntimeException("JExprMath: Unknown Op: " + anOp);
        }
    }

}