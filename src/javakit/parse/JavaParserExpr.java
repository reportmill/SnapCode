/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.parse.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A parser for java expressions.
 */
@SuppressWarnings({"unused", "StringEquality"})
public class JavaParserExpr extends Parser {

    /**
     * Expression Handler: LambdaExpr | ConditionalExpr (AssignOp Expression)?
     */
    public static class ExpressionHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                case "ConditionalExpr", "LambdaExpr" -> _part = aNode.getCustomNode(JExpr.class);

                case "AssignOp" -> {
                    ParseToken token = aNode.getStartToken();
                    String opStr = token.getString();
                    _part = new JExprAssign(opStr, _part, null);
                }

                // Handle Expression: Add to end of Math or Assign expression
                case "Expression" -> {
                    JExpr expr = aNode.getCustomNode(JExpr.class);
                    if (_part instanceof JExprMath mathExpr)
                        mathExpr.setOperand(expr, 1);
                    else if (_part instanceof JExprAssign assignExpr)
                        assignExpr.setValueExpr(expr);
                }
            }
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * Identifier Handler.
     */
    public static class IdentifierHandler extends JNodeParseHandler<JExprId> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            String idStr = aNode.getString();
            _part = new JExprId(idStr);
        }

        protected Class<JExprId> getPartClass()  { return JExprId.class; }
    }

    /**
     * Name Handler: Identifier (LookAhead(2) "." Identifier)*
     */
    public static class NameHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                case "Identifier" -> {
                    JExprId idExpr = aNode.getCustomNode(JExprId.class);
                    if (_part == null)
                        _part = idExpr;
                    else ((JExprDot) _part).setExpr(idExpr);
                }

                // Handle "." Identifier
                case "." -> _part = new JExprDot(_part, null);
            }
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * Type Handler: LookAhead(2) ReferenceType | PrimitiveType
     */
    public static class TypeHandler extends JNodeParseHandler<JType> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                case "ReferenceType", "PrimitiveType" -> _part = aNode.getCustomNode(JType.class);
            }
        }

        @Override
        protected Class<JType> getPartClass()  { return JType.class; }
    }

    /**
     * ReferenceType: PrimitiveType (LookAhead(2) "[" "]")+ | ClassType (LookAhead(2) "[" "]")*
     */
    public static class ReferenceTypeHandler extends JNodeParseHandler<JType> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                case "PrimitiveType", "ClassType" -> _part = aNode.getCustomNode(JType.class);

                // Handle ReferenceType."["
                case "[" -> {
                    JType part = getPart();
                    part.setArrayCount(part.getArrayCount() + 1);
                }
            }
        }

        @Override
        protected Class<JType> getPartClass()  { return JType.class; }
    }

    /**
     * ClassType Handler: Identifier (LookAhead(2) TypeArgs)? (LookAhead(2) "." Identifier (LookAhead(2) TypeArgs)?)*
     */
    public static class ClassTypeHandler extends JNodeParseHandler<JType> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JType type = getPart();

            switch (anId) {

                case "Identifier" -> {
                    JExprId id = aNode.getCustomNode(JExprId.class);
                    type.addId(id);
                }

                case "TypeArgs" -> {
                    JType[] typeArgs = aNode.getCustomNode(JType[].class);
                    type.setTypeArgs(typeArgs);
                }
            }
        }

        @Override
        protected Class<JType> getPartClass()  { return JType.class; }
    }

    /**
     * TypeArgs Handler: "<" (TypeArg ("," TypeArg)*)? ">"
     */
    public static class TypeArgsHandler extends ParseHandler<JType[]> {

        // List of argument expressions
        private List<JType> _typeArgs = new ArrayList<>();

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            if (anId == "TypeArg") {
                JType typeArg = aNode.getCustomNode(JType.class);
                if (typeArg != null)
                    _typeArgs.add(typeArg);
            }
        }

        /**
         * Override to return array.
         */
        public JType[] parsedAll()  { return _typeArgs.toArray(JType.EMPTY_TYPES_ARRAY); }

        /**
         * Override to clear FormalParams list.
         */
        @Override
        public void reset()
        {
            super.reset();
            _typeArgs.clear();
        }

        @Override
        protected Class<JType[]> getPartClass()  { return JType[].class; }
    }

    /**
     * TypeArg Handler: ReferenceType | "?" WildcardBounds?
     *  - And WildcardBounds { "extends" ReferenceType | "super" ReferenceType }
     */
    public static class TypeArgHandler extends JNodeParseHandler<JType> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                // Handle ReferenceType (and WildcardBounds)
                case "ReferenceType" -> {
                    JType referenceType = aNode.getCustomNode(JType.class);
                    if (_part == null)
                        _part = referenceType;
                    else _part.setWildcardBounds(referenceType);
                }

                // Handle "?" (Wildcard)
                case "?" -> {
                    _part = new JType();
                    _part.addId(new JExprId(aNode));
                }
            }
        }

        @Override
        protected Class<JType> getPartClass()  { return JType.class; }
    }

    /**
     * PrimitiveType Handler: "boolean" | "char" | "byte" | "short" | "int" | "long" | "float" | "double"
     */
    public static class PrimitiveTypeHandler extends JNodeParseHandler<JType> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JExprId idExpr = new JExprId(aNode);
            _part = new JType();
            _part.addId(idExpr);
            _part.setPrimitive(true);
        }

        @Override
        protected Class<JType> getPartClass()  { return JType.class; }
    }

    /**
     * ResultType Handler: "void" | Type
     */
    public static class ResultTypeHandler extends JNodeParseHandler<JType> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                case "Type" -> _part = aNode.getCustomNode(JType.class);

                case "void" -> {
                    JType type = getPart();
                    JExprId idExpr = new JExprId(aNode);
                    type.addId(idExpr);
                    type.setPrimitive(true);
                }
            }
        }

        @Override
        protected Class<JType> getPartClass()  { return JType.class; }
    }

    /**
     * ConditionalExpr Handler: ConditionalOrExpr ("?" Expression ":" Expression)?
     */
    public static class ConditionalExprHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                case "ConditionalOrExpr" -> _part = aNode.getCustomNode(JExpr.class);

                case "Expression" -> {
                    JExpr part = aNode.getCustomNode(JExpr.class);
                    JExprMath opExpr = _part instanceof JExprMath ? (JExprMath) _part : null;
                    if (opExpr == null || opExpr.getOp() != JExprMath.Op.Conditional)
                        _part = new JExprMath(JExprMath.Op.Conditional, _part, part);
                    else opExpr.setOperand(part, 2);
                }
            }
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * ConditionalOrExpr Handler.
     */
    public static class ConditionalOrExprHandler extends BinaryExprHandler { }

    /**
     * ConditionalAndExpr Handler.
     */
    public static class ConditionalAndExprHandler extends BinaryExprHandler { }

    /**
     * InclusiveOrExpr Handler.
     */
    public static class InclusiveOrExprHandler extends BinaryExprHandler { }

    /**
     * ExclusiveOrExpr Handler.
     */
    public static class ExclusiveOrExprHandler extends BinaryExprHandler { }

    /**
     * AndExpr Handler.
     */
    public static class AndExprHandler extends BinaryExprHandler { }

    /**
     * EqualityExpr Handler.
     */
    public static class EqualityExprHandler extends BinaryExprHandler { }

    /**
     * RelationalExpr Handler.
     */
    public static class RelationalExprHandler extends BinaryExprHandler { }

    /**
     * ShiftExpr Handler.
     */
    public static class ShiftExprHandler extends BinaryExprHandler {

        @Override
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Eat ">" tokens
            if (anId == ">")
                return;
            super.parsedOne(aNode, anId);
        }
    }

    /**
     * AdditiveExpr Handler.
     */
    public static class AdditiveExprHandler extends BinaryExprHandler { }

    /**
     * MultiplicativeExpr Handler.
     */
    public static class MultiplicativeExprHandler extends BinaryExprHandler { }

    /**
     * BinaryExpr Handler.
     */
    public static abstract class BinaryExprHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle KeyChain
            if (aNode.getCustomNode() instanceof JExpr expr) {
                if (_part instanceof JExprMath)
                    ((JExprMath) _part).addOperand(expr);
                else _part = expr;
            }

            // Handle Ops
            else {
                JExprMath.Op op = getOpForString(anId);
                if (op != null)
                    _part = new JExprMath(op, _part);
            }
        }

        @Override
        protected Class<JExpr> getPartClass()  { return JExpr.class; }

        /**
         * Returns a JExprMath.Op for given op string.
         */
        private static JExprMath.Op getOpForString(String anId)
        {
            return switch (anId) {
                case "||" -> JExprMath.Op.Or;
                case "&&" -> JExprMath.Op.And;
                case "|" -> JExprMath.Op.BitOr;
                case "^" -> JExprMath.Op.BitXOr;
                case "&" -> JExprMath.Op.BitAnd;
                case "==" -> JExprMath.Op.Equal;
                case "!=" -> JExprMath.Op.NotEqual;
                case "<" -> JExprMath.Op.LessThan;
                case ">" -> JExprMath.Op.GreaterThan;
                case "<=" -> JExprMath.Op.LessThanOrEqual;
                case ">=" -> JExprMath.Op.GreaterThanOrEqual;
                case "<<" -> JExprMath.Op.ShiftLeft;
                case "ShiftRight" -> JExprMath.Op.ShiftRight;
                case "ShiftRightUnsigned" -> JExprMath.Op.ShiftRightUnsigned;
                case "+" -> JExprMath.Op.Add;
                case "-" -> JExprMath.Op.Subtract;
                case "*" -> JExprMath.Op.Multiply;
                case "/" -> JExprMath.Op.Divide;
                case "%" -> JExprMath.Op.Mod;
                default -> {
                    System.err.println("BinaryExprHandler.getOpForString: Unknown op string: " + anId);
                    yield null;
                }
            };
        }
    }

    /**
     * UnaryExpr Handler: ("+" | "-") UnaryExpr | PreIncrementExpr | UnaryExprNotPlusMinus
     */
    public static class UnaryExprHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle any expression rule: UnaryExpr, PreIncrementExpr, UnaryExprNotPlusMinus
            if (aNode.getCustomNode() instanceof JExpr expr) {
                if (_part instanceof JExprMath)
                    ((JExprMath) _part).addOperand(expr);
                else _part = expr;
            }

            // Handle unary ops (ignore '+')
            else if (anId == "-")
                _part = new JExprMath(JExprMath.Op.Negate);
            else if (anId == "!")
                _part = new JExprMath(JExprMath.Op.Not);
            else if (anId == "~")
                _part = new JExprMath(JExprMath.Op.BitComp);

            // Handle post Increment/Decrement
            else if (anId == "++")
                _part = new JExprMath(JExprMath.Op.PostIncrement, _part);
            else if (anId == "--")
                _part = new JExprMath(JExprMath.Op.PostDecrement, _part);
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * PreIncrementExpr Handler: "++" PrimaryExpr
     */
    public static class PreIncrementExprHandler extends JNodeParseHandler<JExprMath> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                case "++" -> _part = new JExprMath(JExprMath.Op.PreIncrement);

                case "--" -> _part = new JExprMath(JExprMath.Op.PreDecrement);

                case "PrimaryExpr" -> {
                    JExpr expr = aNode.getCustomNode(JExpr.class);
                    _part.addOperand(expr);
                }
            }
        }

        protected Class<JExprMath> getPartClass()  { return JExprMath.class; }
    }

    /**
     * CastExpr Handler.
     */
    public static class CastExprHandler extends JNodeParseHandler<JExprCast> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                case "PrimitiveType", "ReferenceType" -> {
                    JType type = aNode.getCustomNode(JType.class);
                    getPart().setType(type);
                }

                // Handle UnaryExpr, UnaryExprNotPlusMinus
                default -> {
                    if (aNode.getCustomNode() instanceof JExpr expr)
                        getPart().setExpr(expr);
                }
            }
        }

        protected Class<JExprCast> getPartClass()  { return JExprCast.class; }
    }

    /**
     * InstanceOfExpr Handler: RelationalExpr ("instanceof" (PatternExpr | Type))?
     */
    public static class InstanceOfExprHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                case "Type" -> {
                    JType type = aNode.getCustomNode(JType.class);
                    JExprInstanceOf instanceOfExpr = new JExprInstanceOf();
                    instanceOfExpr.setExpr(_part);
                    instanceOfExpr.setType(type);
                    _part = instanceOfExpr;
                }

                case "PatternExpr" -> {
                    JExprPattern patternExpr = aNode.getCustomNode(JExprPattern.class);
                    JExprInstanceOf instanceOfExpr = new JExprInstanceOf();
                    instanceOfExpr.setExpr(_part);
                    instanceOfExpr.setPattern(patternExpr);
                    _part = instanceOfExpr;
                }

                // Handle nested expression
                default -> {
                    if (aNode.getCustomNode() instanceof JExpr expr)
                        _part = expr;
                }
            }
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * PrimaryExpr Handler: PrimaryPrefix PrimarySuffix*
     */
    public static class PrimaryExprHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                case "PrimaryPrefix" -> _part = aNode.getCustomNode(JExpr.class);

                case "PrimarySuffix" -> {
                    JExpr suffixExpr = aNode.getCustomNode(JExpr.class);
                    _part = joinExpressions(_part, suffixExpr);
                }
            }
        }

        /**
         * Joins two expressions together and returns the result.
         */
        private static JExpr joinExpressions(JExpr prefixExpr, JExpr suffixExpr)
        {
            switch (suffixExpr) {

                // Handle method call: Set scope expression
                case JExprMethodCall methodCallExpr -> {
                    methodCallExpr.setScopeExpr(prefixExpr);
                    return methodCallExpr;
                }

                // Handle MethodRef: Set scope expression
                case JExprMethodRef methodRef -> {
                    methodRef.setScopeExpr(prefixExpr);
                    return methodRef;
                }

                // Handle ArrayIndex: Set ArrayExpr
                case JExprArrayIndex arrayIndexExpr -> {
                    arrayIndexExpr.setArrayExpr(prefixExpr);
                    return arrayIndexExpr;
                }

                // Handle field, ...
                default -> { return new JExprDot(prefixExpr, suffixExpr); }
            }
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * PrimaryPrefix Handler.
     *     Literal |
     *     LookAhead ((Identifier ".")* "this") (Identifier ".")* "this" |
     *     "super" "." Identifier Arguments? |
     *     LookAhead (ClassType "." "super" "." Identifier) ClassType "." "super" "." Identifier Arguments? |
     *     "(" Expression ")" |
     *     AllocExpr |
     *     LookAhead (ResultType "." "class") ResultType "." "class" |
     *     LookAhead (ReferenceType "::") ReferenceType |
     *     Identifier Arguments?
     */
    public static class PrimaryPrefixHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                case "Literal" -> _part = aNode.getCustomNode(JExprLiteral.class);

                // Handle Identifier of [ (Identifier ".")* this ] and [ "super" "." Identifier ]
                case "Identifier" -> {
                    JExprId idExpr = aNode.getCustomNode(JExprId.class);
                    addExpr(idExpr);
                }

                case "." -> _part = new JExprDot(_part, null);

                // Handle "this"/"super" of [ (Identifier ".")* this ] and [ "super" "." Identifier ]
                case "this", "super" -> {
                    JExprId superExpr = new JExprId(aNode);
                    addExpr(superExpr);
                }

                // Handle ClassType.super and ResultType.class and ReferenceType::
                case "ClassType", "ResultType", "ReferenceType" -> {
                    JType classType = aNode.getCustomNode(JType.class);
                    _part = new JExprType(classType);
                }

                case "(" -> _part = new JExprParen(null);

                // Handle "(" Expression ")"
                case "Expression" -> {
                    JExprParen parenExpr = (JExprParen) _part;
                    JExpr innerExpr = aNode.getCustomNode(JExpr.class);
                    parenExpr.setExpr(innerExpr);
                }

                case "AllocExpr" -> _part = aNode.getCustomNode(JExprAlloc.class);

                // Handle ResultType "." "class"
                case "class" -> {
                    JExprId classExpr = new JExprId(aNode);
                    addExpr(classExpr);
                }

                case "Arguments" -> {
                    JExpr[] argExprs = aNode.getCustomNode(JExpr[].class);
                    if (_part instanceof JExprId idExpr)
                        _part = new JExprMethodCall(idExpr, argExprs);
                    else if (_part instanceof JExprDot dotExpr && dotExpr.getExpr() instanceof JExprId idExpr)
                        dotExpr.setExpr(new JExprMethodCall(idExpr, argExprs));
                    else System.err.println("JavaParserExpr.PrimaryPrefixHandler: Method call missing id");
                }
            }
        }

        /**
         * Add expression to current part.
         */
        private void addExpr(JExpr anExpr)
        {
            if (_part == null)
                _part = anExpr;
            else if (_part instanceof JExprDot dotExpr && dotExpr.getExpr() == null)
                dotExpr.setExpr(anExpr);
            else System.err.println("JavaParserExpr.PrimaryPrefixHandler.addExpr: Can't add to expr: " + _part.getString());
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * PrimarySuffix Handler.
     *     LookAhead(2) "." "super" |
     *     LookAhead(2) "." "this" |
     *     LookAhead(2) "." AllocExpr |
     *     LookAhead(2) "." TypeArgs? Identifier Arguments? |
     *     "[" Expression "]" |
     *     "::" (Identifier | "new") |
     */
    public static class PrimarySuffixHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                case "super", "this" -> _part = new JExprId(aNode);

                case "AllocExpr" -> _part = aNode.getCustomNode(JExprAlloc.class);

                // Handle array index: "[" Expression "]"
                case "[" -> _part = new JExprArrayIndex(null, null);
                case "Expression" -> {
                    JExpr arrayIndexExpr = aNode.getCustomNode(JExpr.class);
                    if (_part instanceof JExprArrayIndex)
                        ((JExprArrayIndex) _part).setIndexExpr(arrayIndexExpr);
                    else System.err.println("JavaParserExpr.PrimarySuffixHandler: Unexpected array index expr: " + _part);
                }

                // Handle ("." | "::") Identifier
                case "Identifier" -> {
                    JExprId id = aNode.getCustomNode(JExprId.class);
                    if (_part instanceof JExprMethodRef methodRefExpr)
                        methodRefExpr.setMethodId(id);
                    else _part = id;
                }

                // Handle "::" Identifier: Set part to JExprMethodRef
                case "::" -> _part = new JExprMethodRef(null, null);

                // Handle "new" from: "::" (Identifier | new):
                case "new" -> {
                    JExprId newId = new JExprId(aNode);
                    ((JExprMethodRef) _part).setMethodId(newId);
                }

                case "Arguments" -> {
                    JExpr[] argExprs = aNode.getCustomNode(JExpr[].class);
                    if (_part instanceof JExprId idExpr)
                        _part = new JExprMethodCall(idExpr, argExprs);
                    else System.err.println("JavaParserExpr.PrimarySuffixHandler: Method call missing id");
                }

                // Handle TypeArgs: e.g.: List<String> list = Collections.<String>emptyList();
                //case "TypeArgs" -> _part = aNode.getCustomNode(JExpr.class);
            }
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * Arguments Handler
     */
    public static class ArgumentsHandler extends ParseHandler<JExpr[]> {

        // List of argument expressions
        private List<JExpr> _argExprs = new ArrayList<>();

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            if (anId == "Expression") {
                JExpr argExpr = aNode.getCustomNode(JExpr.class);
                if (argExpr != null)
                    _argExprs.add(argExpr);
            }
        }

        /**
         * Override to return array.
         */
        public JExpr[] parsedAll()  { return _argExprs.toArray(JExpr.EMPTY_EXPR_ARRAY); }

        /**
         * Override to clear FormalParams list.
         */
        @Override
        public void reset()
        {
            super.reset();
            _argExprs.clear();
        }

        @Override
        protected Class<JExpr[]> getPartClass()  { return JExpr[].class; }
    }

    /**
     * AllocExpr Handler:
     *     LookAhead(2) "new" PrimitiveType ArrayDimsAndInits |
     *     "new" ClassType TypeArgs? (ArrayDimsAndInits | Arguments ClassBody?)
     */
    public static class AllocExprHandler extends JNodeParseHandler<JExprAlloc> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get AllocExpr and Type (always create part)
            JExprAlloc allocExpr = getPart();
            JType allocType = allocExpr.getType();

            switch (anId) {

                case "PrimitiveType", "ClassType" -> {
                    JType type = aNode.getCustomNode(JType.class);
                    allocExpr.setType(type);
                }

                // Handle ArrayDimsAndInits
                case "Expression" -> {
                    if (allocType != null && allocType.isArrayType()) {
                        JExpr dimsOrInitsExpr = aNode.getCustomNode(JExpr.class);
                        allocExpr.setArrayDims(dimsOrInitsExpr);
                    }
                }

                // Handle ArrayDimsAndInits ArrayInit
                case "ArrayInit" -> {
                    JExprArrayInit arrayInits = aNode.getCustomNode(JExprArrayInit.class);
                    allocExpr.setArrayInit(arrayInits);
                }

                // Handle TypeArgs, ArrayDimsAndInits
                case "[" -> {
                    if (allocType != null)
                        allocType.setArrayCount(allocType.getArrayCount() + 1);
                }

                case "Arguments" -> {
                    JExpr[] argExprs = aNode.getCustomNode(JExpr[].class);
                    allocExpr.setArgs(argExprs);
                }

                case "ClassBody" -> {
                    JBodyDecl[] classBodyDecls = aNode.getCustomNode(JBodyDecl[].class);
                    allocExpr.setClassBodyDecls(classBodyDecls);
                }
            }
        }

        @Override
        protected Class<JExprAlloc> getPartClass()  { return JExprAlloc.class; }
    }

    /**
     * ArrayInit Handler: "{" (VarInit (LookAhead(2) "," VarInit)*)? ","? "}"
     * VarInit: ArrayInit | Expression
     */
    public static class ArrayInitHandler extends JNodeParseHandler<JExprArrayInit> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Usually an array of JExpr - but array of list if multidimensional array
            JExprArrayInit arrayInitExpr = getPart();

            switch (anId) {

                // Handle Expression, ArrayInit (nested)
                case "ArrayInit", "Expression" -> {
                    JExpr expr = aNode.getCustomNode(JExpr.class);
                    arrayInitExpr.addExpr(expr);
                }
            }
        }

        @Override
        protected Class<JExprArrayInit> getPartClass()  { return JExprArrayInit.class; }
    }

    /**
     * LambdaExpr Handler.
     */
    public static class LambdaExprHandler extends JNodeParseHandler<JExprLambda> {

        /**
         * ParseHandler method:
         *     LookAhead (Identifier "->") Identifier "->" (Expression | Block) |
         *     LookAhead ("(" Identifier ("," Identifier)* ")" "->") "(" Identifier ("," Identifier)* ")" "->" (Expression | Block) |
         *     LookAhead (FormalParams "->") FormalParams "->" (Expression | Block)
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JExprLambda lambdaExpr = getPart();

            switch (anId) {

                case "Identifier" -> {
                    JVarDecl vd = new JVarDecl();
                    vd.setId(aNode.getCustomNode(JExprId.class));
                    lambdaExpr.addParameter(vd);
                }

                case "FormalParams" -> {
                    JVarDecl[] formalParams = aNode.getCustomNode(JVarDecl[].class);
                    lambdaExpr.setParameters(formalParams);
                }

                case "Expression" -> lambdaExpr.setExpr(aNode.getCustomNode(JExpr.class));

                case "Block" -> lambdaExpr.setBlock(aNode.getCustomNode(JStmtBlock.class));
            }
        }

        protected Class<JExprLambda> getPartClass()  { return JExprLambda.class; }
    }

    /**
     * SwitchExpr Handler: "switch" "(" Expression ")" "{" SwitchEntry* "}"
     */
    public static class SwitchExprHandler extends JNodeParseHandler<JExprSwitch> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JExprSwitch switchExpr = getPart();

            switch (anId) {

                case "Expression" -> {
                    JExpr selectorExpr = aNode.getCustomNode(JExpr.class);
                    switchExpr.setSelector(selectorExpr);
                }

                case "SwitchEntry" -> {
                    JSwitchEntry switchEntry = aNode.getCustomNode(JSwitchEntry.class);
                    switchExpr.addEntry(switchEntry);
                }
            }
        }

        protected Class<JExprSwitch> getPartClass()  { return JExprSwitch.class; }
    }

    /**
     * SwitchEntry Handler:
     *     (
     *         "case" (
     *             LookAhead(3) NullLiteral "," "default" |
     *             LookAhead (PatternExpr) PatternExpr ("when" ConditionalExpr)? |
     *             ConditionalExpr ("," ConditionalExpr)*
     *         ) |
     *         "default"
     *     )
     *     (
     *         ":" BlockStatement* |
     *         "->" ( (Expression ";") | Block | ThrowStatement )
     *     )
     */
    public static class SwitchEntryHandler extends JNodeParseHandler<JSwitchEntry> {

        /**
         * ParseHandler method.
         */
        @Override
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JSwitchEntry switchEntry = getPart();

            switch (anId) {

                case "NullLiteral" -> System.err.println("SwitchEntryHandler: NullLiteral support not implemented");

                case "default" -> switchEntry.setDefault(true);

                case "PatternExpr" -> {
                    JExprPattern patternExpr = aNode.getCustomNode(JExprPattern.class);
                    switchEntry.addLabel(patternExpr);
                }

                // Handle ConditionalExpr: Set expr as either guard (if label is pattern) or label
                case "ConditionalExpr" -> {
                    JExpr expr = aNode.getCustomNode(JExpr.class);
                    if (switchEntry.getLabel() instanceof JExprPattern)
                        switchEntry.setGuard(expr);
                    else switchEntry.addLabel(expr);
                }

                case "BlockStatement", "Block", "ThrowStatement" -> {
                    JStmt stmt = aNode.getCustomNode(JStmt.class);
                    switchEntry.addStatement(stmt);
                }

                case "Expression" -> {
                    JExpr entryExpr = aNode.getCustomNode(JExpr.class);
                    JStmtExpr exprStmt = new JStmtExpr();
                    exprStmt.setExpr(entryExpr);
                    switchEntry.addStatement(exprStmt);
                }
            }
        }

        @Override
        protected Class<JSwitchEntry> getPartClass()  { return JSwitchEntry.class; }
    }

    /**
     * PatternExpr Handler:
     *     LookAhead (TypePatternExpr) TypePatternExpr |
     *     RecordPatternExpr
     *  Where:
     *    TypePatternExpr { Modifiers Type Identifier }
     *    RecordPatternExpr {	Modifiers ReferenceType PatternList }
     *    PatternList { "(" PatternExpr ("," PatternExpr)* ")" }
     */
    public static class PatternExprHandler extends JNodeParseHandler<JExprPattern> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JExprPattern patternExpr = getPart();

            switch (anId) {

                case "Modifiers" -> {
                    JModifiers modifiers = aNode.getCustomNode(JModifiers.class);
                    patternExpr.setModifiers(modifiers);
                }

                case "Type", "ReferenceType" -> {
                    JType type = aNode.getCustomNode(JType.class);
                    patternExpr.setType(type);
                }

                case "Identifier" -> {
                    JExprId idExpr = aNode.getCustomNode(JExprId.class);
                    patternExpr.setId(idExpr);
                }

                case "PatternExpr" -> {
                    JExprPattern exprPattern = aNode.getCustomNode(JExprPattern.class);
                    patternExpr.addPatternExpr(exprPattern);
                }
            }
        }

        protected Class<JExprPattern> getPartClass()  { return JExprPattern.class; }
    }

    /**
     * Literal Handler.
     */
    public static class LiteralHandler extends JNodeParseHandler<JExprLiteral> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get node string
            JExprLiteral literalExpr = getPart();
            String valueStr = aNode.getString();

            switch (anId) {

                case "BooleanLiteral" -> literalExpr.setLiteralType(JExprLiteral.LiteralType.Boolean);

                case "IntegerLiteral" -> {
                    int len = valueStr.length();
                    char c = valueStr.charAt(len - 1);
                    if (c == 'l' || c == 'L')
                        literalExpr.setLiteralType(JExprLiteral.LiteralType.Long);
                    else literalExpr.setLiteralType(JExprLiteral.LiteralType.Integer);
                }

                case "FloatLiteral" -> {
                    int len = valueStr.length();
                    char c = valueStr.charAt(len - 1);
                    if (c == 'f' || c == 'F')
                        literalExpr.setLiteralType(JExprLiteral.LiteralType.Float);
                    else literalExpr.setLiteralType(JExprLiteral.LiteralType.Double);
                }

                case "CharacterLiteral" -> literalExpr.setLiteralType(JExprLiteral.LiteralType.Character);

                case "StringLiteral" -> literalExpr.setLiteralType(JExprLiteral.LiteralType.String);

                case "TextBlock" -> {
                    literalExpr.setLiteralType(JExprLiteral.LiteralType.String);
                    valueStr = null;
                }
                case "TextBlockMore" -> {
                    if (literalExpr._valueStr != null && !valueStr.equals(Tokenizer.TEXT_BLOCK_PATTERN))
                        valueStr = literalExpr._valueStr + valueStr;
                }
            }

            // Set value string
            if (valueStr != null)
                literalExpr.setValueString(valueStr);
        }

        protected Class<JExprLiteral> getPartClass()  { return JExprLiteral.class; }
    }

    /**
     * AnnotationDecl Handler.
     * TODO
     */
    public static class AnnotationDeclHandler extends JNodeParseHandler<JClassDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)  { }

        /**
         * Override to create JClassDecl with ClassType Annotation.
         */
        public JClassDecl createPart()
        {
            JClassDecl cd = new JClassDecl();
            cd.setClassType(JClassDecl.ClassType.Enum);
            return cd;
        }
    }

    /**
     * A base ParseHandler implementation for JNodes.
     */
    public abstract static class JNodeParseHandler<T extends JNode> extends ParseHandler<T> {

        /**
         * ParseHandler method.
         */
        public final void parsedOne(ParseNode aNode)
        {
            super.parsedOne(aNode);

            // Set start/end token
            if (_part != null) {
                if (_part.getStartToken() == null)
                    _part.setStartToken(getStartToken());
                _part.setEndToken(aNode.getEndToken());
            }
        }

        /**
         * Override to set part start.
         */
        protected T createPart()
        {
            T part = super.createPart();
            ParseToken token = getStartToken();
            part.setStartToken(token);
            return part;
        }

        /**
         * Returns the part class.
         */
        protected Class<T> getPartClass()
        {
            throw new RuntimeException(getClass().getName() + ": getPartClass not implemented");
        }
    }
}