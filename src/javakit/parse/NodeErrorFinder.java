package javakit.parse;
import javakit.resolver.*;
import snap.parse.ParseException;
import snap.util.ArrayUtils;
import java.util.*;

/**
 * Finds errors for given node.
 */
public class NodeErrorFinder {

    /**
     * Return node errors for given file.
     */
    public static NodeError[] getNodeErrorsForFile(JFile jfile)
    {
        // Get node errors from nodes
        List<NodeError> errorsList = new ArrayList<>();
        findAllNodeErrors(jfile, errorsList);

        // Add node errors for parse exceptions
        NodeError[] parseErrors = getNodeErrorForFileParseException(jfile);
        if (parseErrors.length > 0) {
            Collections.addAll(errorsList, parseErrors);
            Collections.sort(errorsList);
        }

        // Return array
        return errorsList.toArray(NodeError.NO_ERRORS);
    }

    /**
     * Find all node errors in given node and adds to given list.
     */
    private static void findAllNodeErrors(JNode aNode, List<NodeError> errorsList)
    {
        // Get node errors
        NodeError[] errors = aNode.getErrors();
        if (errors.length > 0)
            Collections.addAll(errorsList, errors);

        // If StmtExpr just return (we get anything below this automatically)
        if (aNode instanceof JStmtExpr)
            return;

        List<JNode> children = aNode.getChildren();
        for (JNode child : children)
            findAllNodeErrors(child, errorsList);
    }

    /**
     * Returns the node errors.
     */
    public static NodeError[] getErrorsForNode(JNode node)
    {
        return switch (node) {

            case JExprAlloc allocExpr -> getErrorsForAllocExpr(allocExpr);
            case JExprAssign assignExpr -> getErrorsForAssignExpr(assignExpr);
            case JExprCast castExpr -> getErrorsForCastExpr(castExpr);
            case JExprId idExpr -> getErrorsForIdExpr(idExpr);
            case JExprMath mathExpr -> getErrorsForMathExpr(mathExpr);
            case JExprMethodCall methodCallExpr -> getErrorsForMethodCall(methodCallExpr);
            case JExprMethodRef methodRefExpr -> getErrorsForMethodRef(methodRefExpr);
            case JExprParen parenExpr -> getErrorsForParenExpr(parenExpr);
            case JExprVarDecl varDeclExpr -> getErrorsForVarDeclExpr(varDeclExpr);
            case JFieldDecl fieldDecl -> getErrorsForFieldDecl(fieldDecl);
            case JMethodDecl methodDecl -> getErrorsForMethodDecl(methodDecl);
            case JExecutableDecl execDecl -> getErrorsForExecutableDecl(execDecl);
            case JImportDecl importDecl -> getErrorsForImportDecl(importDecl);
            case JStmtFor forStmt -> getErrorsForForStmt(forStmt);
            case JStmtConditional conditionalStmt -> getErrorsForConditionalStmt(conditionalStmt);
            case JType type -> getErrorsForType(type);
            case JVarDecl varDecl -> getErrorsForVarDecl(varDecl);
            default -> getErrorsForPlainNode(node);
        };
    }

    /**
     * Returns the node errors.
     */
    private static NodeError[] getErrorsForPlainNode(JNode node)
    {
        NodeError[] errors = NodeError.NO_ERRORS;

        // Iterate over children and add any errors for each
        for (JNode child : node.getChildren()) {
            NodeError[] childErrors = child.getErrors();
            if (childErrors.length > 0)
                errors = ArrayUtils.addAll(errors, childErrors);
        }

        // Return
        return errors;
    }

    /**
     * Errors for alloc expr.
     */
    private static NodeError[] getErrorsForAllocExpr(JExprAlloc allocExpr)
    {
        NodeError[] errors = getErrorsForPlainNode(allocExpr);

        // If decl resolved, just return
        JavaDecl classOrConstructor = allocExpr.getDecl();
        if (classOrConstructor != null)
            return errors;

        // Handle unresolved type
        JType type = allocExpr.getType();
        if (type == null)
            return NodeError.addError(errors, allocExpr, "Identifier expected", 0);

        // Handle can't find constructor
        String constrString = getConstructorStringForAllocExpr(allocExpr);
        String errorString = "Can't resolve constructor: " + constrString;
        return NodeError.addError(errors, allocExpr, errorString);
    }

    /**
     * Returns a string for constructor.
     */
    private static String getConstructorStringForAllocExpr(JExprAlloc allocExpr)
    {
        // Get class name and arg types
        JType type = allocExpr.getType();
        String className = type != null ? type.getName() : null;
        if (className == null)
            return "No name found";

        JExpr[] args = allocExpr.getArgs();
        JavaType[] argTypes = ArrayUtils.map(args, expr -> expr != null ? expr.getDeclEvalType() : null, JavaType.class);
        String argTypeString = ArrayUtils.mapToStringsAndJoin(argTypes, argType -> argType != null ? argType.getSimpleName() : "null", ",");
        String argTypesString = '(' + argTypeString + ')';
        return className + argTypesString;
    }

    /**
     * Errors for assign expr.
     */
    private static NodeError[] getErrorsForAssignExpr(JExprAssign assignExpr)
    {
        // Get left side expression and errors - just return if errors found
        JExpr leftSideExpr = assignExpr.getLeftSideExpr();
        NodeError[] leftSideErrors = leftSideExpr.getErrors();
        if (leftSideErrors.length > 0)
            return leftSideErrors;

        // Get value expression and errors - just return if errors found
        JExpr valueExpr = assignExpr.getValueExpr();
        if (valueExpr == null)
            return NodeError.newErrorArray(assignExpr, "Missing assignment value");
        NodeError[] valueExprErrors = valueExpr.getErrors();
        if (valueExprErrors.length > 0)
            return valueExprErrors;

        // Get assign to class - return error if null (impossible since no left side errors)
        JavaClass assignToClass = leftSideExpr.getEvalClass();
        if (assignToClass == null)
            return NodeError.newErrorArray(assignExpr, "Can't resolve type: " + leftSideExpr.getName());

        // Get assign to class and value class - return error if no match
        //JavaClass valueClass = valueExpr.getEvalClass();
        //if (valueClass == null && assignToClass.isPrimitive())
        //    return NodeError.newErrorArray(this, "Incompatible types: <nulltype> cannot be converted to " + assignToClass.getName());
        //if (!assignToClass.isAssignableFrom(valueClass))
        //    return NodeError.newErrorArray(this, "Invalid assignment type");

        return getErrorsForPlainNode(assignExpr);
    }

    /**
     * Errors for cast expr.
     */
    private static NodeError[] getErrorsForCastExpr(JExprCast castExpr)
    {
        // If Type or Expr have errors, just return them
        NodeError[] errors = getErrorsForPlainNode(castExpr);
        if (errors.length > 0)
            return errors;

        // Handle missing type or expression
        if (castExpr.getType() == null)
            return NodeError.newErrorArray(castExpr, "Missing or incomplete cast type");
        if (castExpr.getExpr() == null)
            return NodeError.newErrorArray(castExpr, "Missing or incomplete cast expression");

        // Maybe add an "isCastable()" check? If common superclass is Object and expression class isn't Object, return false?

        return errors;
    }

    /**
     * Errors for JExprId.
     */
    private static NodeError[] getErrorsForIdExpr(JExprId idExpr)
    {
        // If Parent is WithId, just return
        if (idExpr.getParent() instanceof WithId withId && withId.getId() == idExpr)
            return NodeError.NO_ERRORS;

        // Handle can't resolve id
        JavaDecl decl = idExpr.getDecl();
        if (decl == null)
            return NodeError.newErrorArray(idExpr, "Can't resolve id: " + idExpr.getName());

        return NodeError.NO_ERRORS;
    }

    /**
     * Errors for math expression.
     */
    private static NodeError[] getErrorsForMathExpr(JExprMath mathExpr)
    {
        // If missing operands, return error
        int opCountActual = mathExpr.getOperandCount();
        int opCountExpected = mathExpr.getOp().getOperandCount();
        if (opCountActual < opCountExpected)
            return NodeError.newErrorArray(mathExpr, "Missing operand");

        // Do normal version
        return getErrorsForPlainNode(mathExpr);
    }

    /**
     * Errors for MethodCall.
     */
    private static NodeError[] getErrorsForMethodCall(JExprMethodCall methodCall)
    {
        // If any arg errors, return them
        for (JExpr arg : methodCall.getArgs()) {
            NodeError[] argErrors = arg.getErrors();
            if (argErrors.length > 0)
                return argErrors;
        }

        // Handle can't resolve method
        JavaMethod method = methodCall.getMethod();
        if (method == null) {

            // If no method exists for name, return can't resolve method name
            boolean hasAnyMethodForName = methodCall.getMethodAny() != null;
            String methodString = getMethodString(methodCall, hasAnyMethodForName);
            return NodeError.newErrorArray(methodCall, "Can't find method: " + methodString);
        }

        // If missing args, complain
        int paramCount = method.getParameterCount();
        int argCount = methodCall.getArgCount();
        if (paramCount > argCount && !method.isVarArgs())
            return NodeError.newErrorArray(methodCall, "Missing args, " + paramCount + " expected, " + argCount + " provided");

        return NodeError.NO_ERRORS;
    }

    /**
     * Returns a string for method.
     */
    private static String getMethodString(JExprMethodCall methodCall, boolean withArgs)
    {
        // Get method name and arg types
        String methodName = methodCall.getName();
        if (methodName == null)
            return "No name found";
        if (!withArgs)
            return methodName + "()";

        JExpr[] args = methodCall.getArgs();
        JavaType[] argTypes = ArrayUtils.map(args, expr -> expr != null ? expr.getEvalType() : null, JavaType.class);
        String argTypeString = ArrayUtils.mapToStringsAndJoin(argTypes, type -> type != null ? type.getSimpleName() : "null", ",");
        String argTypesString = '(' + argTypeString + ')';
        String methodString = methodName + argTypesString;

        // Get scope node class name
        JavaDecl scopeEvalType = methodCall.getScopeEvalType();
        String scopeClassName = scopeEvalType != null ? scopeEvalType.getEvalClassName() : null;
        if (scopeClassName != null)
            methodString = scopeClassName + '.' + methodString;

        return methodString;
    }

    /**
     * Errors for MethodRef.
     */
    private static NodeError[] getErrorsForMethodRef(JExprMethodRef methodRef)
    {
        // If scope expression has errors, return them
        JExpr scopeExpr = methodRef.getScopeExpr();
        NodeError[] scopeExprErrors = scopeExpr.getErrors();
        if (scopeExprErrors.length > 0)
            return scopeExprErrors;

        // If no MethodId expression has errors, return them
        if (methodRef.getMethodId() == null)
            return NodeError.newErrorArray(methodRef, "Method reference method name not specified");

        // If MethodId has errors, return them
        NodeError[] methodIdErrors = methodRef.getMethodId().getErrors();
        if (methodIdErrors.length > 0)
            return methodIdErrors;

        return getErrorsForPlainNode(methodRef);
    }

    /**
     * Errors for JStmtExpr.
     */
    private static NodeError[] getErrorsForParenExpr(JExprParen parenExpr)
    {
        NodeError[] errors = getErrorsForPlainNode(parenExpr);

        // Handle missing statement
        if (parenExpr.getExpr() == null)
            errors = NodeError.addError(errors, parenExpr, "Missing or incomplete expression", 0);

        return errors;
    }

    /**
     * Errors for JExprVarDecl.
     */
    private static NodeError[] getErrorsForVarDeclExpr(JExprVarDecl varDecl)
    {
        // Handle compound var
        JType type = varDecl.getType();
        if (type.isVarType() && varDecl.getVarDecls().length > 1)
            return NodeError.newErrorArray(type, "'var' is not allowed in a compound declaration");

        // If any child VarDecls has errors, just return that
        for (JVarDecl childVarDecl : varDecl.getVarDecls()) {
            NodeError[] varDeclErrors = childVarDecl.getErrors();
            if (varDeclErrors.length > 0)
                return varDeclErrors;
        }

        return getErrorsForPlainNode(varDecl);
    }

    /**
     * Errors for JFieldDecl.
     */
    private static NodeError[] getErrorsForFieldDecl(JFieldDecl fieldDecl)
    {
        JType returnType = fieldDecl.getType();
        return returnType.getErrors();
    }

    /**
     * Errors for JMethodDecl.
     */
    private static NodeError[] getErrorsForMethodDecl(JMethodDecl methodDecl)
    {
        // Get errors for type
        JType returnType = methodDecl.getReturnType();
        if (returnType == null) {
            JNode errorNode = methodDecl.getChildCount() > 0 ? methodDecl.getChild(0) : methodDecl; // Typing "List<" can cause method decl with no children
            return NodeError.newErrorArray(errorNode, "Missing return type");
        }

        // Do normal version
        return getErrorsForExecutableDecl(methodDecl);
    }

    /**
     * Errors for JExecutableDecl.
     */
    private static NodeError[] getErrorsForExecutableDecl(JExecutableDecl execDecl)
    {
        NodeError[] errors = NodeError.NO_ERRORS;

        // Get errors for params
        JVarDecl[] parameters = execDecl.getParameters();
        errors = addNodeErrorsForNodes(errors, parameters);

        // Get errors for throws list
        JExpr[] throwsList = execDecl.getThrowsList();
        errors = addNodeErrorsForNodes(errors, throwsList);

        // Get errors for type vars
        JTypeVar[] typeVars = execDecl.getTypeParamDecls();
        errors = addNodeErrorsForNodes(errors, typeVars);

        // Return
        return errors;
    }

    /**
     * Errors for JImportDecl.
     */
    private static NodeError[] getErrorsForImportDecl(JImportDecl importDecl)
    {
        // Handle missing package or class name
        if (importDecl.getName().isEmpty())
            return NodeError.newErrorArray(importDecl, "Import needs package or class name");

        // Handle super errors
        NodeError[] superErrors = getErrorsForPlainNode(importDecl);
        if (superErrors.length > 0)
            return superErrors;

        // Handle missing wildcard
        if (importDecl.getDecl() instanceof JavaPackage && !importDecl.isInclusive())
            return NodeError.newErrorArray(importDecl, "Import needs to end with class name or wildcard (*)");

        return NodeError.NO_ERRORS;
    }

    /**
     * Errors for JStmtFor.
     */
    private static NodeError[] getErrorsForForStmt(JStmtFor forStmt)
    {
        // Handle var decl errors
        JExprVarDecl varDeclExpr = forStmt.getVarDeclExpr();
        if (varDeclExpr != null) {
            NodeError[] varDeclErrors = varDeclExpr.getErrors();
            if (varDeclErrors.length > 0)
                return varDeclErrors;
        }

        // Handle for each
        if (forStmt.isForEach()) {

            // Handle missing or invalid iterable expression
            JExpr iterableExpr = forStmt.getIterableExpr();
            if (iterableExpr == null)
                return NodeError.newErrorArray(forStmt, "Missing iterable or array");
            NodeError[] iterableErrors = iterableExpr.getErrors();
            if (iterableErrors.length > 0)
                return iterableErrors;

            // Handle iterable expression not iterable
            JavaClass iterableClass = iterableExpr.getEvalClass();
            boolean isArrayOrIterable = iterableClass.isArray() || forStmt.getJavaClassForClass(Iterable.class).isAssignableFrom(iterableClass);
            if (!isArrayOrIterable)
                return NodeError.newErrorArray(iterableExpr, "Expression must be array or iterable");

            // Handle iterable type not assignable to var decl
            assert (varDeclExpr != null);
            JavaClass varDeclClass = varDeclExpr.getEvalClass();
            JavaClass iterationClass = forStmt.getForEachIterationTypeResolved();
            if (iterationClass != null && !varDeclClass.isAssignableFrom(iterationClass))
                return NodeError.newErrorArray(iterableExpr, "Incompatible types: " +
                        iterationClass.getSimpleName() + " cannot be assigned to " + varDeclClass.getSimpleName());
        }

        // Handle basic
        else {

            // If errors in init expressions, return
            JExpr[] initExprs = forStmt.getInitExprs();
            for (JExpr initExpr : initExprs) {
                NodeError[] initErrors = initExpr.getErrors();
                if (initErrors.length > 0)
                    return initErrors;
            }

            // If errors in conditional expression, return
            JExpr condExpr = forStmt.getConditional();
            if (condExpr != null) {
                NodeError[] condErrors = condExpr.getErrors();
                if (condErrors.length > 0)
                    return condErrors;
            }

            // If errors in update expressions, return
            JExpr[] updateExprs = forStmt.getUpdateExprs();
            for (JExpr updateExpr : updateExprs) {
                NodeError[] updateErrors = updateExpr.getErrors();
                if (updateErrors.length > 0)
                    return updateErrors;
            }
        }

        return getErrorsForConditionalStmt(forStmt);
    }

    /**
     * Override to provide errors for conditional statements.
     */
    private static NodeError[] getErrorsForConditionalStmt(JStmtConditional conditionalStmt)
    {
        NodeError[] errors = getErrorsForPlainNode(conditionalStmt);
        if (errors.length > 0)
            return errors;

        // Handle missing conditional
        if (conditionalStmt.getConditional() == null && !(conditionalStmt instanceof JStmtFor))
            return NodeError.newErrorArray(conditionalStmt, "Missing conditional");

        // Handle missing statement
        if (conditionalStmt.getStatement() == null)
            return NodeError.newErrorArray(conditionalStmt, "Missing statement block");

        return errors;
    }

    /**
     * Errors for JType.
     */
    private static NodeError[] getErrorsForType(JType type)
    {
        JavaType javaType = type.getJavaType();
        if (javaType != null)
            return NodeError.NO_ERRORS;

        // Let compiler handle 'var' errors
        if (type.isVarType())
            return NodeError.NO_ERRORS;

        // Return
        return NodeError.newErrorArray(type, "Can't resolve type: " + type.getName());
    }

    /**
     * Errors for JVarDecl.
     */
    private static NodeError[] getErrorsForVarDecl(JVarDecl varDecl)
    {
        // If not var and type has errors, return them
        JType type = varDecl.getType();
        if (type == null)
            return NodeError.newErrorArray(varDecl, "Missing type");
        if (!type.isVarType()) {
            NodeError[] typeErrors = type.getErrors();
            if (typeErrors.length > 0)
                return typeErrors;
        }

        // If initializer expression set, check for errors
        JExpr initExpr = varDecl.getInitExpr();
        if (initExpr != null) {
            NodeError[] initializerErrors = initExpr.getErrors();
            if (initializerErrors.length > 0)
                return initializerErrors;
        }

        // If type has errors, just return it
        NodeError[] typeErrors = type.getErrors();
        if (typeErrors.length > 0)
            return typeErrors;

        return getErrorsForPlainNode(varDecl);
    }

    /**
     * Returns the errors for a list of nodes.
     */
    private static NodeError[] addNodeErrorsForNodes(NodeError[] errors, JNode[] nodes)
    {
        for (JNode node : nodes)
            errors = ArrayUtils.addAll(errors, node.getErrors());
        return errors;
    }

    /**
     * Returns the node error for a JFile parse exception.
     */
    public static NodeError[] getNodeErrorForFileParseException(JFile jfile)
    {
        // Get exception - just return null if none
        Exception exception = jfile.getException();
        if (exception == null)
            return NodeError.NO_ERRORS;

        // Get last node
        JNode lastNode = null;
        if (exception instanceof ParseException) {
            int charIndex = ((ParseException) exception).getCharIndex();
            lastNode = jfile.getNodeForCharIndex(charIndex);
        }
        if (lastNode == null)
            lastNode = getLastNodeForNode(jfile);
        if (lastNode == null || lastNode.getStartToken() == null)
            return NodeError.NO_ERRORS;

        // Return error for exception message
        String msg = exception.getMessage();
        return NodeError.newErrorArray(lastNode, msg);
    }

    /**
     * Returns the last node for given node.
     */
    private static JNode getLastNodeForNode(JNode aNode)
    {
        JNode lastNode = aNode;
        while (lastNode.getChildCount() > 0)
            lastNode = lastNode.getLastChild();
        return lastNode;
    }
}
