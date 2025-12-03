package snapcode.debug;
import com.sun.jdi.*;
import javakit.parse.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to evaluate expressions.
 */
public class ExprEval {

    /**
     * Evaluate expression.
     */
    public static Object eval(DebugApp anApp, String anExpr)
    {
        // Parse expression
        JavaParser javaParser = JavaParser.getShared();
        JExpr expr = javaParser.parseExpression(anExpr);

        // Evaluate expression
        ObjectReference oref = anApp.thisObject();
        try { return evalExpr(anApp, oref, expr); }
        catch (Exception e) { return e; }
    }

    /**
     * Evaluate JExpr.
     */
    public static Value evalExpr(DebugApp anApp, ObjectReference anOR, JExpr anExpr) throws Exception
    {
        if (anExpr instanceof JExprLiteral)
            return evalLiteral(anApp, (JExprLiteral) anExpr);
        if (anExpr instanceof JExprId)
            return evalIdentifier(anApp, anOR, (JExprId) anExpr);
        if (anExpr instanceof JExprMethodCall)
            return evalMethod(anApp, anOR, (JExprMethodCall) anExpr);
        if (anExpr instanceof JExprMath)
            return evalMathExpr(anApp, anOR, (JExprMath) anExpr);
        if (anExpr instanceof JExprArrayIndex)
            return evalArrayIndex(anApp, anOR, (JExprArrayIndex) anExpr);
        if (anExpr instanceof JExprDot)
            return evalExprChain(anApp, anOR, (JExprDot) anExpr);
        return null;
    }

    /**
     * Evaluate JLiteral.
     */
    private static Value evalLiteral(DebugApp anApp, JExprLiteral aLiteral)
    {
        VirtualMachine aVM = anApp._vm;
        return switch (aLiteral.getLiteralType()) {
            case Boolean -> aVM.mirrorOf((Boolean) aLiteral.getValue());
            case Integer -> aVM.mirrorOf((Integer) aLiteral.getValue());
            case Long -> aVM.mirrorOf((Long) aLiteral.getValue());
            case Float -> aVM.mirrorOf((Float) aLiteral.getValue());
            case Double -> aVM.mirrorOf((Double) aLiteral.getValue());
            case Character -> aVM.mirrorOf((Character) aLiteral.getValue());
            case String -> aVM.mirrorOf((String) aLiteral.getValue());
            case Null -> null;
        };
    }

    /**
     * Evaluate JIdentifier.
     */
    private static Value evalIdentifier(DebugApp anApp, ObjectReference anOR, JExprId anId) throws Exception
    {
        // Get identifier name
        String name = anId.getName();

        // If name is "this", return Frame ThisObject
        if (name.equals("this")) return anApp.thisObject();

        // Check for local variable
        StackFrame frame = anApp.getCurrentFrame();
        LocalVariable lvar = frame.visibleVariableByName(name);
        if (lvar != null)
            return frame.getValue(lvar);

        // Check for field
        ReferenceType refType = anOR.referenceType();
        Field field = refType.fieldByName(name);
        if (field != null)
            return anOR.getValue(field);

        // Complain
        throw new RuntimeException("Identifier not found: " + name);
    }

    /**
     * Evaluate JMethodCall.
     */
    private static Value evalMethod(DebugApp anApp, ObjectReference anOR, JExprMethodCall anExpr) throws Exception
    {
        ObjectReference thisObj = anApp.thisObject();
        List<Value> args = new ArrayList<>();
        for (JExpr arg : anExpr.getArgs())
            args.add(evalExpr(anApp, thisObj, arg));
        return anApp.invokeMethod(anOR, anExpr.getName(), args);
    }

    /**
     * Evaluate JExprArrayIndex.
     */
    static Value evalArrayIndex(DebugApp debugApp, ObjectReference objRef, JExprArrayIndex anExpr) throws Exception
    {
        // Get array reference (return null if not found)
        JExpr arrayExpr = anExpr.getArrayExpr();
        Value arrayRefValue = evalExpr(debugApp, objRef, arrayExpr);
        if (!(arrayRefValue instanceof ArrayReference arrayRef))
            return null;

        // Get Index
        JExpr indexExpr = anExpr.getIndexExpr();
        ObjectReference thisObj = debugApp.thisObject();
        Value arrayIndexValue = evalExpr(debugApp, thisObj, indexExpr);
        if (!(arrayIndexValue instanceof PrimitiveValue primitiveValue))
            return null;

        // Return array value at index
        int index = primitiveValue.intValue();
        return arrayRef.getValue(index);
    }

    /**
     * Evaluate JExprChain.
     */
    static Value evalExprChain(DebugApp anApp, ObjectReference anOR, JExprDot anExpr) throws Exception
    {
        ObjectReference or = anOR;

        // Eval prefix
        JExpr prefixExpr = anExpr.getPrefixExpr();
        Object prefixVal = evalExpr(anApp, or, prefixExpr);
        if (prefixVal instanceof ObjectReference)
            or = (ObjectReference) prefixVal;

        // Eval expression
        JExpr expr = anExpr.getExpr();
        return evalExpr(anApp, or, expr);
    }

    /**
     * Evaluate JExprMath.
     */
    static Value evalMathExpr(DebugApp anApp, ObjectReference anOR, JExprMath anExpr) throws Exception
    {
        // Get first value
        JExprMath.Op op = anExpr.getOp();
        int opCount = anExpr.getOperandCount();
        JExpr expr1 = anExpr.getOperand(0);
        Value val1 = evalExpr(anApp, anOR, expr1);

        // Handle Unary
        if (opCount == 1) {
            if (op == JExprMath.Op.Not) {
                if (val1.type() instanceof BooleanType) {
                    boolean val = ((PrimitiveValue) val1).booleanValue();
                    return anApp._vm.mirrorOf(!val);
                }
                throw new RuntimeException("Logical Not MathExpr not boolean: " + anExpr);
            }
            if (op == JExprMath.Op.Negate) { // Need to not promote everything to double
                if (val1.type() instanceof PrimitiveType) {
                    double val = ((PrimitiveValue) val1).doubleValue();
                    return anApp._vm.mirrorOf(-val);
                }
                throw new RuntimeException("Numeric Negate MathExpr not numeric: " + anExpr);
            }
            else switch (op) {
                case Not:
                default: throw new RuntimeException("Operator not supported " + anExpr.getOp());
                //PreIncrement, PreDecrement, BitComp, PostIncrement, PostDecrement
            }
        }

        // Handle Binary
        else if (opCount == 2) {
            JExpr expr2 = anExpr.getOperand(1);
            Value val2 = evalExpr(anApp, anOR, expr2);
            // BitOr, BitXOr, BitAnd, InstanceOf, ShiftLeft, ShiftRight, ShiftRightUnsigned,
            return switch (op) {
                case Add -> add(anApp, val1, val2);
                case Subtract -> subtract(anApp, val1, val2);
                case Multiply -> multiply(anApp, val1, val2);
                case Divide -> divide(anApp, val1, val2);
                case Mod -> mod(anApp, val1, val2);
                case Equal, NotEqual, LessThan, GreaterThan, LessThanOrEqual, GreaterThanOrEqual -> compareNumeric(anApp, val1, val2, op);
                case Or, And -> compareLogical(anApp, val1, val2, op);
                default -> throw new RuntimeException("Operator not supported " + anExpr.getOp());
            };
        }

        // Handle ternary
        else if (opCount == 3 && op == JExprMath.Op.Conditional) {
            if (!(val1 instanceof PrimitiveValue primitiveValue))
                throw new RuntimeException("Ternary conditional expr not bool: " + expr1);
            boolean result = primitiveValue.booleanValue();
            JExpr expr = result ? anExpr.getOperand(1) : anExpr.getOperand(2);
            return evalExpr(anApp, anOR, expr);
        }

        // Complain
        throw new RuntimeException("Invalid MathExpr " + anExpr);
    }

    /**
     * Add two values.
     */
    private static Value add(DebugApp anApp, Value aVal1, Value aVal2)
    {
        if (aVal1 instanceof StringReference || aVal2 instanceof StringReference)
            return anApp._vm.mirrorOf(anApp.toString(aVal1) + anApp.toString(aVal2));
        if (aVal1 instanceof PrimitiveValue && aVal2 instanceof PrimitiveValue) {
            double result = ((PrimitiveValue) aVal1).doubleValue() + ((PrimitiveValue) aVal2).doubleValue();
            return value(anApp, result, aVal1, aVal2);
        }
        throw new RuntimeException("Can't add types " + aVal1 + " + " + aVal2);
    }

    /**
     * Subtract two values.
     */
    private static Value subtract(DebugApp anApp, Value aVal1, Value aVal2)
    {
        if (aVal1 instanceof PrimitiveValue && aVal2 instanceof PrimitiveValue) {
            double result = ((PrimitiveValue) aVal1).doubleValue() - ((PrimitiveValue) aVal2).doubleValue();
            return value(anApp, result, aVal1, aVal2);
        }
        throw new RuntimeException("Can't subtract types " + aVal1 + " + " + aVal2);
    }

    /**
     * Multiply two values.
     */
    private static Value multiply(DebugApp anApp, Value aVal1, Value aVal2)
    {
        if (aVal1 instanceof PrimitiveValue && aVal2 instanceof PrimitiveValue) {
            double result = ((PrimitiveValue) aVal1).doubleValue() * ((PrimitiveValue) aVal2).doubleValue();
            return value(anApp, result, aVal1, aVal2);
        }
        throw new RuntimeException("Can't multiply types " + aVal1 + " + " + aVal2);
    }

    /**
     * Divide two values.
     */
    private static Value divide(DebugApp anApp, Value aVal1, Value aVal2)
    {
        if (aVal1 instanceof PrimitiveValue && aVal2 instanceof PrimitiveValue) {
            double result = ((PrimitiveValue) aVal1).doubleValue() / ((PrimitiveValue) aVal2).doubleValue();
            return value(anApp, result, aVal1, aVal2);
        }
        throw new RuntimeException("Can't divide types " + aVal1 + " + " + aVal2);
    }

    /**
     * Mod two values.
     */
    private static Value mod(DebugApp anApp, Value aVal1, Value aVal2)
    {
        if (aVal1 instanceof PrimitiveValue && aVal2 instanceof PrimitiveValue) {
            double result = ((PrimitiveValue) aVal1).longValue() % ((PrimitiveValue) aVal2).longValue();
            return value(anApp, result, aVal1, aVal2);
        }
        throw new RuntimeException("Can't mod types " + aVal1 + " + " + aVal2);
    }

    /**
     * Compare two numeric values.
     */
    private static Value compareNumeric(DebugApp anApp, Value aVal1, Value aVal2, JExprMath.Op anOp)
    {
        if (aVal1 instanceof PrimitiveValue && aVal2 instanceof PrimitiveValue) {
            double v1 = ((PrimitiveValue) aVal1).doubleValue(), v2 = ((PrimitiveValue) aVal2).doubleValue();
            boolean val = compareNumeric(v1, v2, anOp);
            return anApp._vm.mirrorOf(val);
        }
        throw new RuntimeException("Can't numeric compare types " + aVal1 + " + " + aVal2);
    }

    /**
     * Compare two numeric values.
     */
    private static boolean compareNumeric(double aVal1, double aVal2, JExprMath.Op anOp)
    {
        return switch (anOp) {
            case Equal -> aVal1 == aVal2;
            case NotEqual -> aVal1 != aVal2;
            case LessThan -> aVal1 < aVal2;
            case GreaterThan -> aVal1 > aVal2;
            case LessThanOrEqual -> aVal1 <= aVal2;
            case GreaterThanOrEqual -> aVal1 >= aVal2;
            default -> throw new RuntimeException("Not a compare op " + anOp);
        };
    }

    /**
     * Compare two boolean values.
     */
    private static Value compareLogical(DebugApp anApp, Value aVal1, Value aVal2, JExprMath.Op anOp)
    {
        if (aVal1 instanceof PrimitiveValue && aVal2 instanceof PrimitiveValue) {
            boolean v1 = ((PrimitiveValue) aVal1).booleanValue();
            boolean v2 = ((PrimitiveValue) aVal2).booleanValue();
            boolean val = compareLogical(v1, v2, anOp);
            return anApp._vm.mirrorOf(val);
        }
        throw new RuntimeException("Can't logical compare types " + aVal1 + " + " + aVal2);
    }

    /**
     * Compare two values.
     */
    private static boolean compareLogical(boolean aVal1, boolean aVal2, JExprMath.Op anOp)
    {
        if (anOp == JExprMath.Op.And) return aVal1 && aVal2;
        if (anOp == JExprMath.Op.Or) return aVal1 && aVal2;
        throw new RuntimeException("Not a compare op " + anOp);
    }

    /**
     * Return value of appropriate type for given number and original two values.
     */
    private static Value value(DebugApp anApp, double aValue, Value aVal1, Value aVal2)
    {
        Type type1 = aVal1.type(), type2 = aVal2.type();
        if (type1 instanceof DoubleType || type2 instanceof DoubleType)
            return anApp._vm.mirrorOf(aValue);
        if (type1 instanceof FloatType || type2 instanceof FloatType)
            return anApp._vm.mirrorOf((float) aValue);
        if (type1 instanceof LongType || type2 instanceof LongType)
            return anApp._vm.mirrorOf((long) aValue);
        if (type1 instanceof IntegerType || type2 instanceof IntegerType)
            return anApp._vm.mirrorOf((int) aValue);
        throw new RuntimeException("Can't discern value type for " + aVal1 + " and " + aVal2);
    }

}