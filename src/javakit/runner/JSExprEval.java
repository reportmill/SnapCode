/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.runner;
import java.lang.reflect.*;
import java.util.*;
import javakit.parse.*;
import static javakit.runner.JSExprEvalUtils.*;
import javakit.resolver.*;
import snap.util.*;

/**
 * A class to evaluate expressions.
 */
public class JSExprEval {

    // The JavaShell
    private JavaShell _javaShell;

    /**
     * Constructor.
     */
    public JSExprEval(JavaShell javaShell)
    {
        _javaShell = javaShell;
    }

    /**
     * Evaluates given expression on given object reference.
     */
    public Object evalExpr(JExpr anExpr, Object thisObject) throws Exception
    {
        // Handle Literal
        if (anExpr instanceof JExprLiteral)
            return evalLiteralExpr((JExprLiteral) anExpr);

        // Handle variable
        if (anExpr instanceof JExprId)
            return evalIdExpr((JExprId) anExpr, thisObject);

        // Handle dot expression
        if (anExpr instanceof JExprDot)
            return evalExprDot((JExprDot) anExpr, thisObject);

        // Handle math expression
        if (anExpr instanceof JExprMath)
            return evalMathExpr((JExprMath) anExpr, thisObject);

        // Handle method call
        if (anExpr instanceof JExprMethodCall)
            return evalMethodCallExpr((JExprMethodCall) anExpr, thisObject);

        // Handle VarDecl expression
        if (anExpr instanceof JExprVarDecl)
            return evalVarDeclExpr((JExprVarDecl) anExpr, thisObject);

        // Handle assign expression
        if (anExpr instanceof JExprAssign)
            return evalAssignExpr((JExprAssign) anExpr, thisObject);

        // Handle array dereference
        if (anExpr instanceof JExprArrayIndex)
            return evalArrayIndexExpr((JExprArrayIndex) anExpr, thisObject);

        // Handle array initializer
        if (anExpr instanceof JExprArrayInit)
            return evalArrayInitExpr((JExprArrayInit) anExpr, thisObject);

        // Handle alloc expression
        if (anExpr instanceof JExprAlloc)
            return evalAllocExpr((JExprAlloc) anExpr, thisObject);

        // Handle cast expression
        if (anExpr instanceof JExprCast)
            return evalCastExpr((JExprCast) anExpr, thisObject);

        // Handle paren expression
        if (anExpr instanceof JExprParen) {
            JExpr innerExpr = ((JExprParen) anExpr).getExpr();
            return evalExpr(innerExpr, thisObject);
        }

        // Handle Instanceof expression
        if (anExpr instanceof JExprInstanceOf)
            return evalInstanceOfExpr(((JExprInstanceOf) anExpr), thisObject);

        // Handle lambda expression
        if (anExpr instanceof JExprLambda)
            return evalLambdaExpr((JExprLambda) anExpr, thisObject);

        // Handle method ref expression
        if (anExpr instanceof JExprMethodRef)
            return evalMethodRefExpr((JExprMethodRef) anExpr, thisObject);

        // Handle Type expression
        if (anExpr instanceof JExprType)
            return evalTypeExpr((JExprType) anExpr);

        // Complain
        throw new RuntimeException("JSExprEval.evalExpr: Unsupported expression " + anExpr.getClass());
    }

    /**
     * Evaluate JExprLiteral.
     */
    private Object evalLiteralExpr(JExprLiteral aLiteral)
    {
        switch (aLiteral.getLiteralType()) {
            case Boolean: return (Boolean) aLiteral.getValue();
            case Integer: return (Integer) aLiteral.getValue();
            case Long: return (Long) aLiteral.getValue();
            case Float: return (Float) aLiteral.getValue();
            case Double: return (Double) aLiteral.getValue();
            case Character: return (Character) aLiteral.getValue();
            case String: return (String) aLiteral.getValue();
            case Null: return null;
            default: throw new RuntimeException("No Literal Type");
        }
    }

    /**
     * Evaluate JExprId.
     */
    private Object evalIdExpr(JExprId anId, Object thisObject) throws Exception
    {
        // Get identifier name
        String name = anId.getName();

        // Handle common "this", "class" and array.length (not sure about this)
        if (name.equals("this"))
            return thisObject;
        if (name.equals("class"))
            return thisObject;
        if (name.equals("length") && isArray(thisObject))
            return Array.getLength(thisObject);

        // If LocalVar, get from stack
        JavaDecl idDecl = anId.getDecl();
        if (idDecl instanceof JavaLocalVar)
            return _javaShell._varStack.getStackValueForNode(anId);

        // Handle Field
        if (idDecl instanceof JavaField) {
            JavaField field = (JavaField) idDecl;
            return field.get(thisObject);
        }

        // Handle class literal
        if (idDecl instanceof JavaClass) {
             if (anId.isClassNameLiteral())
                return idDecl;
             else System.out.println("JSExprEval.evalIdExpr: id is class but not literal: " + name);
        }

        // Return
        System.out.println("JSExprEval.evalIdExpr: Can't evaluate id: " + name);
        return null;
    }

    /**
     * Evaluate JExprMethodCall.
     */
    private Object evalMethodCallExpr(JExprMethodCall methodCallExpr, Object thisObject) throws Exception
    {
        // Get arg values
        int argCount = methodCallExpr.getArgCount();
        Object[] argValues = new Object[argCount];
        for (int i = 0; i < argCount; i++) {
            JExpr argExpr = methodCallExpr.getArg(i);
            argValues[i] = evalExpr(argExpr, thisObject);
        }

        // Get method (if null, throw exception)
        JavaMethod method = methodCallExpr.getMethod();
        if (method == null)
            throw new NoSuchMethodException("JSExprEval: Method not found for " + methodCallExpr.getName());

        // If object null, throw NullPointerException
        if (thisObject == null && !method.isStatic())
            throw new NullPointerException("JSExprEval: Can't call " + methodCallExpr.getName() + " on null");

        // Look for local MethodDecl
        JMethodDecl methodDecl = method.getMethodDecl();
        if (methodDecl != null)
            return _javaShell.callMethodDecl(methodDecl, thisObject, argValues);

        // Handle var args packaging
        if (method.isVarArgs())
            argValues = method.repackageArgsForVarArgsMethod(argValues);

        // Invoke method
        Object value = method.invoke(thisObject, argValues);
        return value;
    }

    /**
     * Evaluate JExprArrayIndex.
     */
    private Object evalArrayIndexExpr(JExprArrayIndex anExpr, Object thisObject) throws Exception
    {
        // Get Array
        JExpr arrayExpr = anExpr.getArrayExpr();
        Object arrayObj = evalExpr(arrayExpr, thisObject);
        if (!isArray(arrayObj))
            return null;

        // Get Index
        JExpr indexExpr = anExpr.getIndexExpr();
        Object indexObj = evalExpr(indexExpr, thisObject); //if (!isPrimitive(indexObj)) return null;
        int index = intValue(indexObj);

        // Return Array value at index
        return Array.get(arrayObj, index);
    }

    /**
     * Evaluate JExprArrayInit.
     */
    private Object evalArrayInitExpr(JExprArrayInit arrayInitExpr, Object thisObject) throws Exception
    {
        JavaClass javaClass = arrayInitExpr.getArrayClass();
        Class<?> realClass = javaClass.getRealClass();

        // Create array
        int arrayLen = arrayInitExpr.getExprCount();
        Class<?> compClass = realClass.getComponentType();
        Object array = Array.newInstance(compClass, arrayLen);

        // Iterate over arg expressions and get evaluated values
        for (int i = 0; i < arrayLen; i++) {

            // Get array value at index
            JExpr initExpr = arrayInitExpr.getExpr(i);
            Object initValue = evalExpr(initExpr, thisObject);

            // Set value
            initValue = castOrConvertValueToPrimitiveClass(initValue, compClass);
            Array.set(array, i, initValue);
        }

        // Return
        return array;
    }

    /**
     * Evaluate JExprAlloc.
     */
    protected Object evalAllocExpr(JExprAlloc anExpr, Object thisObject) throws Exception
    {
        // Get real class for expression
        JavaDecl exprDecl = anExpr.getDecl();
        JavaClass javaClass = exprDecl.getEvalClass();
        Class<?> realClass = javaClass.getRealClass();

        // Handle array
        if (javaClass.isArray())
            return evalAllocArrayExpr(anExpr, thisObject, javaClass);

        // Special case
        List<JExpr> argExprs = anExpr.getArgs();
        int argCount = argExprs.size();
        if (argCount == 0)
            return realClass.newInstance();

        // Get constructor
        JavaConstructor javaConstructor = (JavaConstructor) exprDecl;

        // Get arg info
        Object[] argValues = new Object[argCount];

        // Iterate over arg expressions and get evaluated values
        for (int i = 0; i < argCount; i++) {
            JExpr argExpr = argExprs.get(i);
            argValues[i] = evalExpr(argExpr, thisObject);
        }

        // Invoke constructor
        Constructor<?> constructor = javaConstructor.getConstructor();
        return constructor.newInstance(argValues);
    }

    /**
     * Evaluate JExprAlloc for array.
     */
    protected Object evalAllocArrayExpr(JExprAlloc anExpr, Object thisObject, JavaClass javaClass) throws Exception
    {
        Class<?> realClass = javaClass.getRealClass();

        // Handle simple dimensions
        JExpr dimensionExpr = anExpr.getArrayDims(); // Should be a list
        if (dimensionExpr != null) {

            // Get dimension
            Object dimensionObj = evalExpr(dimensionExpr, thisObject);
            int arrayLen = intValue(dimensionObj);

            // Create/return array
            Class<?> compClass = realClass.getComponentType();
            return Array.newInstance(compClass, arrayLen);
        }

        // Handle inits
        JExprArrayInit arrayInitExpr = anExpr.getArrayInit();
        return evalArrayInitExpr(arrayInitExpr, thisObject);
    }

    /**
     * Evaluate JExprCast.
     */
    protected Object evalCastExpr(JExprCast aCastExpr, Object thisObject) throws Exception
    {
        // Get expression and evaluate
        JExpr expr = aCastExpr.getExpr();
        Object value = evalExpr(expr, thisObject);

        // Get type - if not primative, just return
        JType type = aCastExpr.getType();
        JavaType typeClass = type != null ? type.getJavaType() : null;
        if (typeClass == null) {
            System.out.println("JSExprEval: Couldn't get type for cast expression: " + aCastExpr);
            return value;
        }

        // If not primitve, just return value
        if (!typeClass.isPrimitive())
            return value;

        // If value is null, complain
        if (value == null)
            throw new RuntimeException("JSExprEval: Trying to cast null to " + typeClass.getClassName());

        // If valueClass is assignable to type class, just return value
        Class<?> valueClass = value.getClass();
        Class<?> castClass = typeClass.getEvalClass().getRealClass();
        if (castClass.isAssignableFrom(valueClass))
            return value;

        // Cast value and return
        Object castValue = castOrConvertValueToPrimitiveClass(value, castClass);
        return castValue;
    }

    /**
     * Evaluate JExprInstanceOf.
     */
    protected Object evalInstanceOfExpr(JExprInstanceOf anInstanceOfExpr, Object thisObject) throws Exception
    {
        // Get expression and evaluate
        JExpr expr = anInstanceOfExpr.getExpr();
        Object value = evalExpr(expr, thisObject);

        // Get type and class
        JType type = anInstanceOfExpr.getType();
        JavaClass typeClass = type.getEvalClass();
        Class<?> typeRealClass = typeClass.getRealClass();

        // Return whether value is instance of
        return typeRealClass.isInstance(value);
    }

    /**
     * Evaluate JExprDot.
     */
    private Object evalExprDot(JExprDot anExpr, Object thisObject) throws Exception
    {
        Object val = thisObject;

        // Eval prefix expression (if not package name)
        JExpr prefixExpr = anExpr.getPrefixExpr();
        Object prefixVal = null;
        if (!(prefixExpr.getDecl() instanceof JavaPackage))
            prefixVal = evalExpr(prefixExpr, val);

        // Eval expression
        JExpr expr = anExpr.getExpr();
        return evalExpr(expr, prefixVal);
    }

    /**
     * Evaluate JExprMath.
     */
    private Object evalMathExpr(JExprMath anExpr, Object thisObject) throws Exception
    {
        // Handle Unary
        int opCount = anExpr.getOperandCount();
        if (opCount == 1)
            return evalMathExprUnary(anExpr, thisObject);

        // Handle Binary
        if (opCount == 2)
            return evalMathExprBinary(anExpr, thisObject);

        // Handle ternary
        JExpr conditionalExpr = anExpr.getOperand(0);
        Object val1 = evalExpr(conditionalExpr, thisObject);

        // Get resulting expression
        boolean result = boolValue(val1);
        JExpr resultExpr = result ? anExpr.getOperand(1) : anExpr.getOperand(2);

        // Evaluate resulting expression and return
        return evalExpr(resultExpr, thisObject);
    }

    /**
     * Evaluate JExprMath unary expression.
     */
    private Object evalMathExprUnary(JExprMath anExpr, Object thisObject) throws Exception
    {
        JExpr unaryExpr = anExpr.getOperand(0);
        Object val1 = evalExpr(unaryExpr, thisObject);
        JExprMath.Op op = anExpr.getOp();

        switch (op) {

            // Handle Not
            case Not:
                if (isBoolean(val1))
                    return !boolValue(val1);
                throw new RuntimeException("Logical Not MathExpr not boolean: " + anExpr);

                // Handle Negate
            case Negate: {
                if (isNumberOrChar(val1))
                    return -doubleValue(val1);
                throw new RuntimeException("Numeric Negate Expr not numeric: " + anExpr);
            }

            // Handle Increment
            case PreIncrement: {
                if (isNumberOrChar(val1)) {
                    Object val2 = add(val1, 1);
                    setAssignExprValue(unaryExpr, thisObject, val2);
                    return val2;
                }
                throw new RuntimeException("Numeric PreIncrement Expr not numeric: " + anExpr);
            }

            // Handle Decrement
            case PreDecrement: {
                if (isNumberOrChar(val1)) {
                    Object val2 = add(val1, -1);
                    setAssignExprValue(unaryExpr, thisObject, val2);
                    return val2;
                }
                throw new RuntimeException("Numeric PreDecrement Expr not numeric: " + anExpr);
            }

            // Handle Increment
            case PostIncrement: {
                if (isNumberOrChar(val1)) {
                    setAssignExprValue(unaryExpr, thisObject, add(val1, 1));
                    return val1;
                }
                throw new RuntimeException("Numeric PostIncrement Expr not numeric: " + anExpr);
            }

            // Handle Decrement
            case PostDecrement: {
                if (isNumberOrChar(val1)) {
                    setAssignExprValue(unaryExpr, thisObject, add(val1, -1));
                    return val1;
                }
                throw new RuntimeException("Numeric PostDecrement Expr not numeric: " + anExpr);
            }

            // Handle Bitwise compliment
            case BitComp: {
                if (isLong(val1))
                    return ~longValue(val1);
                if (isFixedPointOrChar(val1))
                    return ~intValue(val1);
                throw new RuntimeException("Bitwise compliment Expr not fixed point value: " + anExpr);
            }

            // Handle unknown (BitComp?)
            default: throw new RuntimeException("Operator not supported " + anExpr.getOp());
        }
    }

    /**
     * Evaluate JExprMath binary expression.
     */
    private Object evalMathExprBinary(JExprMath anExpr, Object thisObject) throws Exception
    {
        JExpr expr1 = anExpr.getOperand(0);
        Object val1 = evalExpr(expr1, thisObject);
        JExpr expr2 = anExpr.getOperand(1);
        Object val2 = evalExpr(expr2, thisObject);
        JExprMath.Op op = anExpr.getOp();

        // Handle binary op
        switch (op) {

            // Handle add, subtract, multiply, divide, mod
            case Add: return add(val1, val2);
            case Subtract: return subtract(val1, val2);
            case Multiply: return multiply(val1, val2);
            case Divide: return divide(val1, val2);
            case Mod: return mod(val1, val2);

            // Handle equal/not-equal
            case Equal:
            case NotEqual: return compareEquals(val1, val2, op);

            // Handle compare numeric
            case LessThan:
            case GreaterThan:
            case LessThanOrEqual:
            case GreaterThanOrEqual: return compareNumeric(val1, val2, op);

            // Handle compare logical
            case Or:
            case And: return compareLogical(val1, val2, op);

            // Handle bitwise
            case BitAnd:
            case BitOr:
            case BitXOr:
            case ShiftLeft:
            case ShiftRight:
            case ShiftRightUnsigned: return evalBitwise(val1, val2, op);

            // Handle unsupported: BitOr, BitXOr, BitAnd, InstanceOf, ShiftLeft, ShiftRight, ShiftRightUnsigned
            default: throw new RuntimeException("Operator not supported " + anExpr.getOp());
        }
    }

    /**
     * Handle JExprVarDecl.
     */
    private Object evalVarDeclExpr(JExprVarDecl anExpr, Object thisObject) throws Exception
    {
        // Get list
        JVarDecl[] varDecls = anExpr.getVarDecls();
        List<Object> vals = new ArrayList<>();

        // Iterate over VarDecls
        for (JVarDecl varDecl : varDecls) {

            // If initializer expression, evaluate and set local var
            JExpr initExpr = varDecl.getInitExpr();
            if (initExpr != null) {
                JExprId varId = varDecl.getId();
                Object val = evalExpr(initExpr, thisObject);
                _javaShell.setExprIdValue(varId, val);
                vals.add(val);
            }
        }

        // If one value, just return it
        if (vals.size() == 1)
            return vals.get(0);

        // Otherwise, return joined string
        return ListUtils.joinStrings(vals, ", ");
    }

    /**
     * Handle JExprAssign.
     */
    private Object evalAssignExpr(JExprAssign anExpr, Object thisObject) throws Exception
    {
        // Get value expression/value
        JExpr valExpr = anExpr.getValueExpr();
        Object value = evalExpr(valExpr, thisObject);

        // Get name expression/name
        JExpr assignToExpr = anExpr.getLeftSideExpr();

        // If op not simple, perform math
        JExprAssign.Op assignOp = anExpr.getOp();
        if (assignOp != JExprAssign.Op.Assign) {

            // Get AssignToExpr value
            Object assignToValue = evalExpr(assignToExpr, thisObject);

            // Get value with assign op
            switch (assignOp) {
                case Add: value = add(assignToValue, value); break;
                case Subtract: value = subtract(assignToValue, value); break;
                case Multiply: value = multiply(assignToValue, value); break;
                case Divide: value = divide(assignToValue, value); break;
                case Mod: value = mod(assignToValue, value); break;
                case And: value = evalBitwise(assignToValue, value, JExprMath.Op.BitAnd); break;
                case Or: value = evalBitwise(assignToValue, value, JExprMath.Op.BitOr); break;
                case Xor: value = evalBitwise(assignToValue, value, JExprMath.Op.BitXOr); break;
                case ShiftLeft: value = evalBitwise(assignToValue, value, JExprMath.Op.ShiftLeft); break;
                case ShiftRight: value = evalBitwise(assignToValue, value, JExprMath.Op.ShiftRight); break;
                case ShiftRightUnsigned: value = evalBitwise(assignToValue, value, JExprMath.Op.ShiftRightUnsigned); break;
                default: throw new RuntimeException("JSExprEval.evalAssignExpr: Op not yet supported: " + assignOp);
            }
        }

        // Set value
        Object assignedValue = setAssignExprValue(assignToExpr, thisObject, value);
        return assignedValue;
    }

    /**
     * Handle JExprLambda.
     */
    private Object evalLambdaExpr(JExprLambda aLambdaExpr, Object thisObject)
    {
        return LambdaWrapper.getWrappedLambdaExpression(_javaShell, thisObject, aLambdaExpr);
    }

    /**
     * Handle JExprMethodRef.
     */
    private Object evalMethodRefExpr(JExprMethodRef methodRefExpr, Object thisObject) throws Exception
    {
        // If MethodRef is HelperMethod and instance method on specified scope instance, evaluate to get target object
        Object target = null;
        if (methodRefExpr.getType() == JExprMethodRef.Type.HelperMethod) {
            JavaMethod method = methodRefExpr.getMethod();
            if (!method.isStatic()) {
                JExpr prefixExpr = methodRefExpr.getPrefixExpr();
                target = evalExpr(prefixExpr, thisObject);
            }
        }

        // Return a proxy object which is a dynamically generated implementation of lambda functional interface class
        return LambdaWrapper.getWrappedMethodRefExpression(methodRefExpr, target);
    }

    /**
     * Handle JExprType.
     */
    private Object evalTypeExpr(JExprType typeExpr)
    {
        JavaClass evalClass = typeExpr.getEvalClass();
        Class<?> realClass = evalClass != null ? evalClass.getRealClass() : null;
        if (realClass == null)
            throw new RuntimeException("JSExprEval.evalTypeExpr: Can't find type for expr: " + typeExpr);
        return realClass;
    }

    /**
     * Sets an assignment value for given assignTo expression and value.
     */
    private Object setAssignExprValue(JExpr assignToExpr, Object thisObject, Object aValue) throws Exception
    {
        // Handle ExprId
        if (assignToExpr instanceof JExprId)
            return _javaShell.setExprIdValue((JExprId) assignToExpr, aValue);

        // Handle array
        if (assignToExpr instanceof JExprArrayIndex)
            return setExprArrayIndexValue((JExprArrayIndex) assignToExpr, thisObject, aValue);

        // I don't think this can happen
        throw new RuntimeException("JExprEval.setAssignExprValue: Unexpected assign to class: " + assignToExpr.getClass());
    }

    /**
     * Sets an assignment value for given identifier expression and value.
     */
    private Object setExprArrayIndexValue(JExprArrayIndex arrayIndexExpr, Object thisObject, Object aValue) throws Exception
    {
        // Get array
        JExpr arrayExpr = arrayIndexExpr.getArrayExpr();
        Object array = _javaShell._varStack.getStackValueForNode(arrayExpr);

        // Get Index
        JExpr indexExpr = arrayIndexExpr.getIndexExpr();
        Object indexObj = evalExpr(indexExpr, thisObject); //if (!isPrimitive(indexObj)) return null;
        int index = intValue(indexObj);

        // Set value and return
        Array.set(array, index, aValue);
        return aValue;
    }
}