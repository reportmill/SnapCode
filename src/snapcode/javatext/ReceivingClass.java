/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.parse.*;
import javakit.resolver.*;
import java.util.List;

/**
 * This class provides functionality for determining the class a node should be.
 *
 * Only works for assignment values, param values, math expression operands, conditional expressions.
 */
public class ReceivingClass {

    /**
     * Returns the assignable type of given node assuming it's the receiving expression of assign or a method arg.
     */
    public static JavaClass getReceivingClass(JNode aNode)
    {
        // If MethodCall arg, return arg class
        JavaType argType = getMethodCallArgType(aNode);
        if (argType != null)
            return argType.getEvalClass();

        // If node is Assign Right-Hand-Side, return assignment Left-Hand-Side class
        JExprAssign assExpr = aNode.getParent(JExprAssign.class);
        if (assExpr != null)
            return assExpr.getEvalClass();

        // If node is JVarDecl Initializer, return JVarDecl class
        JVarDecl initVarDecl = getVarDeclForInitializer(aNode);
        if (initVarDecl != null)
            return initVarDecl.getEvalClass();

        // If node is JExprMath, return op class
        JExprMath mathExpr = aNode.getParent(JExprMath.class);
        if (mathExpr != null)
            return mathExpr.getEvalClass();

        // If node is expression and top parent is conditional statement, return boolean
        JExpr exp = aNode instanceof JExpr ? (JExpr) aNode : aNode.getParent(JExpr.class);
        if (exp != null) {

            // Get top expression
            while (exp.getParent() instanceof JExpr)
                exp = (JExpr) exp.getParent();

            // If parent is conditional expression and expr is the conditional, return boolean
            JNode par = exp.getParent();
            if (par instanceof JStmtConditional) {
                JStmtConditional cond = (JStmtConditional) par;
                if (exp == cond.getConditional())
                    return aNode.getJavaClassForClass(Boolean.class);
            }
        }

        // Return null since no assignment type found for class
        return null;
    }

    /**
     * Returns the method call parent of given node, if available.
     */
    private static JExprMethodCall getMethodCall(JNode aNode)
    {
        JNode node = aNode;
        while (node != null && !(node instanceof JStmt) && !(node instanceof JMemberDecl)) {
            if (node instanceof JExprMethodCall)
                return (JExprMethodCall) node;
            node = node.getParent();
        }
        return null;
    }

    /**
     * Return the method call arg class of node, if node is MethodCall arg.
     */
    private static JavaType getMethodCallArgType(JNode aNode)
    {
        // Get methodc all
        JExprMethodCall methodCall = getMethodCall(aNode);
        if (methodCall == null)
            return null;

        // Get Arg index for node
        int argIndex = getMethodCallArgIndex(methodCall, aNode);
        if (argIndex < 0)
            return null;

        // Get method
        JavaMethod method = methodCall.getDecl();
        if (method == null)
            return null;

        // Get arg type and return
        JavaType argType = argIndex < method.getParameterCount() ? method.getParameterType(argIndex) : null;
        return argType;
    }

    /**
     * Return the method call arg index of node.
     */
    private static int getMethodCallArgIndex(JExprMethodCall aMethodCall, JNode aNode)
    {
        // Get methodCall for node
        JExprMethodCall methodCall = aMethodCall != null ? aMethodCall : getMethodCall(aNode);
        if (methodCall == null) return -1;

        // Get args
        List<JExpr> args = methodCall.getArgs();

        // Iterate over args and return index if found
        JNode node = aNode;
        while (node != methodCall) {
            for (int i = 0, iMax = args.size(); i < iMax; i++)
                if (args.get(i) == node)
                    return i;
            node = node.getParent();
        }

        // Return not found
        return -1;
    }

    /**
     * Returns the JVarDecl for given node, if node is initializer.
     */
    private static JVarDecl getVarDeclForInitializer(JNode aNode)
    {
        JNode node = aNode;
        while (node != null && !(node instanceof JStmt) && !(node instanceof JMemberDecl)) {
            if (node instanceof JExpr) {
                JExpr expr = (JExpr) node;
                if (expr.getParent() instanceof JVarDecl) {
                    JVarDecl vd = (JVarDecl) expr.getParent();
                    if (vd.getInitializer() == expr)
                        return vd;
                }
            }
            node = node.getParent();
        }

        // Return not found
        return null;
    }
}
