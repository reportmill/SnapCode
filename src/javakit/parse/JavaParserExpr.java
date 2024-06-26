/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.parse.ParseHandler;
import snap.parse.ParseNode;
import snap.parse.Parser;
import snap.parse.ParseToken;
import java.util.ArrayList;
import java.util.List;

/**
 * A parser for java statements.
 */
@SuppressWarnings({"unused", "StringEquality"})
public class JavaParserExpr extends Parser {

    /**
     * Expression Handler: ConditionalExpr (AssignOp Expression)?
     */
    public static class ExpressionHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                // Handle ConditionalExpr
                case "ConditionalExpr":
                    _part = aNode.getCustomNode(JExpr.class);
                    break;

                // Handle Assign Op
                case "AssignOp":
                    ParseToken token = aNode.getStartToken();
                    String opStr = token.getString();
                    _part = new JExprAssign(opStr, _part, null);
                    break;

                // Handle Expression: Add to end of Math or Assign expression
                case "Expression":
                    JExpr expr = aNode.getCustomNode(JExpr.class);
                    if (_part instanceof JExprMath)
                        ((JExprMath) _part).setOperand(expr, 1);
                    else if (_part instanceof JExprAssign)
                        ((JExprAssign) _part).setValueExpr(expr);
                    break;
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

                // Handle Identifier
                case "Identifier":
                    JExprId idExpr = aNode.getCustomNode(JExprId.class);
                    if (_part == null)
                        _part = idExpr;
                    else ((JExprDot) _part).setExpr(idExpr);
                    break;

                // Handle "." Identifier
                case ".": _part = new JExprDot(_part, null); break;
            }
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * Type Handler: LookAhead(2) ReferenceType | PrimitiveType
     *   - ReferenceType: PrimitiveType (LookAhead(2) "[" "]")+ | ClassType (LookAhead(2) "[" "]")*
     */
    public static class TypeHandler extends JNodeParseHandler<JType> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                // Handle ClassType, PrimitiveType
                case "ClassType":
                case "PrimitiveType":
                    _part = aNode.getCustomNode(JType.class);
                    break;

                // Handle ReferenceType."["
                case "[":
                    JType part = getPart();
                    part.setArrayCount(part.getArrayCount() + 1);
                    break;
            }
        }

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
            // Get type
            JType type = getPart();

            // Handle Identifier: Add to type
            if (anId == "Identifier") {
                JExprId id = aNode.getCustomNode(JExprId.class);
                type.addId(id);
            }

            // Handle TypeArgs (since no TypeArgsHandler, TypeArgHandler or ReferenceTypeHandler, just watch for JType)
            else {
                Object customNode = aNode.getCustomNode();
                if (customNode instanceof JType) {
                    JType typeArg = (JType) customNode;
                    type.addTypeArg(typeArg);
                }
            }
        }

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

                // Handle Type
                case "Type":
                    _part = aNode.getCustomNode(JType.class);
                    break;

                // Handle void
                case "void":
                    JType type = getPart();
                    JExprId idExpr = new JExprId(aNode);
                    type.addId(idExpr);
                    type.setPrimitive(true);
                    break;
            }
        }

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

                // Handle ConditionalOrExpr
                case "ConditionalOrExpr":
                    _part = aNode.getCustomNode(JExpr.class);
                    break;

                // Handle Expression
                case "Expression":
                    JExpr part = aNode.getCustomNode(JExpr.class);
                    JExprMath opExpr = _part instanceof JExprMath ? (JExprMath) _part : null;
                    if (opExpr == null || opExpr.getOp() != JExprMath.Op.Conditional)
                        _part = new JExprMath(JExprMath.Op.Conditional, _part, part);
                    else opExpr.setOperand(part, 2);
                    break;
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
    public static class ShiftExprHandler extends BinaryExprHandler { }

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
            if (aNode.getCustomNode() instanceof JExpr) {
                JExpr part = aNode.getCustomNode(JExpr.class);
                if (_part instanceof JExprMath)
                    ((JExprMath) _part).addOperand(part);
                else _part = part;
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
        public static JExprMath.Op getOpForString(String anId)
        {
            switch (anId) {
                case "||": return JExprMath.Op.Or;
                case "&&": return JExprMath.Op.And;
                case "|": return JExprMath.Op.BitOr;
                case "^": return JExprMath.Op.BitXOr;
                case "&": return JExprMath.Op.BitAnd;
                case "==": return JExprMath.Op.Equal;
                case "!=": return JExprMath.Op.NotEqual;
                case "<": return JExprMath.Op.LessThan;
                case ">": return JExprMath.Op.GreaterThan;
                case "<=": return JExprMath.Op.LessThanOrEqual;
                case ">=": return JExprMath.Op.GreaterThanOrEqual;
                case "<<": return JExprMath.Op.ShiftLeft;
                case "ShiftRight": return JExprMath.Op.ShiftRight;
                case "ShiftRightUnsigned": return JExprMath.Op.ShiftRightUnsigned;
                case "+": return JExprMath.Op.Add;
                case "-": return JExprMath.Op.Subtract;
                case "*": return JExprMath.Op.Multiply;
                case "/": return JExprMath.Op.Divide;
                case "%": return JExprMath.Op.Mod;
                default: System.err.println("BinaryExprHandler.getOpForString: Unknown op string: " + anId); return null;
            }
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
            if (aNode.getCustomNode() instanceof JExpr) {
                JExpr part = aNode.getCustomNode(JExpr.class);
                if (_part instanceof JExprMath)
                    ((JExprMath) _part).addOperand(part);
                else _part = part;
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

                // Handle "++"
                case "++": _part = new JExprMath(JExprMath.Op.PreIncrement); break;

                // Handle "--"
                case "--": _part = new JExprMath(JExprMath.Op.PreDecrement); break;

                // Handle PrimaryExpr
                case "PrimaryExpr":
                    JExpr expr = aNode.getCustomNode(JExpr.class);
                    _part.addOperand(expr);
                    break;
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
            // Handle Type node
            if (anId == "Type")
                getPart().setType(aNode.getCustomNode(JType.class));

            // Handle UnaryExpr
            else if (aNode.getCustomNode() != null)
                getPart().setExpr(aNode.getCustomNode(JExpr.class));
        }

        protected Class<JExprCast> getPartClass()  { return JExprCast.class; }
    }

    /**
     * InstanceOfExpr Handler.
     */
    public static class InstanceOfExprHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle expression
            if (aNode.getCustomNode() instanceof JExpr)
                _part = aNode.getCustomNode(JExpr.class);

            // Handle Type node
            if (anId == "Type") {
                JExprInstanceOf ie = new JExprInstanceOf();
                ie.setExpr(_part);
                ie.setType(aNode.getCustomNode(JType.class));
                _part = ie;
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

                // Handle PrimaryPrefix
                case "PrimaryPrefix":
                    _part = aNode.getCustomNode(JExpr.class);
                    break;

                // Handle PrimarySuffix: Join prefix and suffix
                case "PrimarySuffix":
                    JExpr expr = aNode.getCustomNode(JExpr.class);
                    _part = JExpr.joinPrimaryPrefixAndSuffixExpressions(_part, expr);
                    break;
            }
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * PrimaryPrefix Handler.
     *     Literal |
     *     LookAhead ((Identifier ".")* "this") (Identifier ".")* "this" |
     *     "super" "." Identifier |
     *     LookAhead (ClassType "." "super" "." Identifier) ClassType "." "super" "." Identifier |
     *     LambdaExpr |
     *     "(" Expression ")" |
     *     AllocExpr |
     *     LookAhead (ResultType "." "class") ResultType "." "class" |
     *     Name
     */
    public static class PrimaryPrefixHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                // Handle Literal
                case "Literal":
                    _part = aNode.getCustomNode(JExprLiteral.class);
                    break;

                // Handle Identifier of [ (Identifier ".")* this ] and [ "super" "." Identifier ]
                case "Identifier": {
                    JExprId idExpr = aNode.getCustomNode(JExprId.class);
                    addExpr(idExpr);
                    break;
                }

                // Handle '.'
                case ".": _part = new JExprDot(_part, null); break;

                // Handle "this"/"super" of [ (Identifier ".")* this ] and [ "super" "." Identifier ]
                case "this":
                case "super": {
                    JExprId superExpr = new JExprId(aNode);
                    addExpr(superExpr);
                    break;
                }

                // Handle ClassType.super and ResultType.class
                case "ClassType":
                case "ResultType":
                    JType classType = aNode.getCustomNode(JType.class);
                    _part = new JExprType(classType);
                    break;

                // Handle LambdaExpr
                case "LambdaExpr":
                    _part = aNode.getCustomNode(JExprLambda.class);
                    break;

                // Handle '('
                case "(": _part = new JExprParen(null); break;

                // Handle "(" Expression ")"
                case "Expression":
                    JExprParen parenExpr = (JExprParen) _part;
                    JExpr innerExpr = aNode.getCustomNode(JExpr.class);
                    parenExpr.setExpr(innerExpr);
                    break;

                // Handle AllocExpr
                case "AllocExpr":
                    _part = aNode.getCustomNode(JExpr.class);
                    break;

                // Handle ResultType "." "class"
                case "class": {
                    JExprId classExpr = new JExprId(aNode);
                    addExpr(classExpr);
                    break;
                }

                // Handle Name
                case "Name":
                    _part = (JExpr) aNode.getCustomNode();
                    break;
            }
        }

        /**
         * Add expression to current part.
         */
        private void addExpr(JExpr anExpr)
        {
            if (_part == null)
                _part = anExpr;
            else if (_part instanceof JExprDot) {
                JExprDot dotExpr = (JExprDot) _part;
                if (dotExpr.getExpr() == null)
                    dotExpr.setExpr(anExpr);
                else System.err.println("JavaParserExpr.PrimaryPrefixHandler.addExpr: Can't add to full dot expr: " + dotExpr.getString() + " + " + anExpr.getString());
            }
            else System.err.println("JavaParserExpr.PrimaryPrefixHandler.addExpr: Can't add to expr: " + _part.getString());
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * PrimarySuffix Handler.
     *     LookAhead(2) "." "super" |
     *     LookAhead(2) "." "this" |
     *     LookAhead(2) "." AllocExpr |
     *     LookAhead(2) "." TypeArgs? Identifier |
     *     "[" Expression "]" |
     *     "::" (Identifier | "new") |
     *     Arguments
     */
    public static class PrimarySuffixHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                // Handle [ "." "super" ] and [ "." "this" ]
                case "super":
                case "this":
                    JExpr thisExpr = new JExprId(aNode.getString());
                    if (_part instanceof JExprDot)
                        ((JExprDot) _part).setExpr(thisExpr);
                    else System.err.println("JavaParserExpr.PrimarySuffixHandler.parseOne: Unexpected dot expr: " + _part);
                    break;

                // Handle AllocExpr
                case "AllocExpr": _part = aNode.getCustomNode(JExpr.class); break;

                // Handle MemberSelector: TypeArgs Identifier (currently handed below without TypeArgs)
                //else if(anId=="TypeArgs") _part = aNode.getCustomNode(JavaExpression.class);

                // Handle "[" Expression
                case "[": _part = new JExprArrayIndex(null, null); break;

                // Handle "[" Expression "]"
                case "Expression":
                    JExpr arrayIndexExpr = aNode.getCustomNode(JExpr.class);
                    if (_part instanceof JExprArrayIndex)
                        ((JExprArrayIndex) _part).setIndexExpr(arrayIndexExpr);
                    else System.err.println("JavaParserExpr.PrimarySuffixHandler.parseOne: Unexpected array index expr: " + _part);
                    break;

                // Handle ("." | "::") Identifier
                case "Identifier":
                    JExprId id = aNode.getCustomNode(JExprId.class);
                    if (_part instanceof JExprDot)
                        ((JExprDot) _part).setExpr(id);
                    else if (_part instanceof JExprMethodRef)
                        ((JExprMethodRef) _part).setMethodId(id);
                    else _part = id;
                    break;

                // Handle "." Identifier
                case ".": _part = new JExprDot(null, null); break;

                // Handle "::" Identifier: Set part to JExprMethodRef
                case "::": _part = new JExprMethodRef(null, null); break;

                // Handle "new" from: "::" (Identifier | new):
                case "new":
                    JExprId newId = new JExprId("new");
                    ((JExprMethodRef) _part).setMethodId(newId);
                    break;

                // Handle Arguments
                case "Arguments":
                    JExpr[] argExprs = aNode.getCustomNode(JExpr[].class);
                    _part = new JExprMethodCall(null, argExprs);
                    break;
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
            // Handle Expression
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
        protected Class getPartClass()  { return JExpr[].class; }
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

                // Handle PrimitiveType, ClassType
                case "PrimitiveType":
                case "ClassType":
                    JType type = aNode.getCustomNode(JType.class);
                    allocExpr.setType(type);
                    break;

                // Handle ArrayDimsAndInits
                case "Expression":
                    if (allocType != null && allocType.isArrayType()) {
                        JExpr dimsOrInitsExpr = aNode.getCustomNode(JExpr.class);
                        allocExpr.setArrayDims(dimsOrInitsExpr);
                    }
                    break;

                // Handle ArrayDimsAndInits ArrayInit
                case "ArrayInit":
                    JExprArrayInit arrayInits = aNode.getCustomNode(JExprArrayInit.class);
                    allocExpr.setArrayInit(arrayInits);
                    break;

                // Handle TypeArgs, ArrayDimsAndInits
                case "[":
                    if (allocType != null)
                        allocType.setArrayCount(allocType.getArrayCount() + 1);
                    break;

                // Handle Arguments
                case "Arguments":
                    JExpr[] argExprs = aNode.getCustomNode(JExpr[].class);
                    allocExpr.setArgs(argExprs);
                    break;

                // Handle ClassBody
                case "ClassBody":
                    JBodyDecl[] classBodyDecls = aNode.getCustomNode(JBodyDecl[].class);
                    allocExpr.setClassBodyDecls(classBodyDecls);
                    break;
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
                case "ArrayInit":
                case "Expression":
                    JExpr expr = aNode.getCustomNode(JExpr.class);
                    arrayInitExpr.addExpr(expr);
                    break;
            }
        }

        @Override
        protected Class getPartClass()  { return JExprArrayInit.class; }
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

                // Handle Identifier
                case "Identifier":
                    JVarDecl vd = new JVarDecl();
                    vd.setId(aNode.getCustomNode(JExprId.class));
                    lambdaExpr.addParameter(vd);
                    break;

                // Handle FormalParams
                case "FormalParams":
                    JVarDecl[] formalParams = aNode.getCustomNode(JVarDecl[].class);
                    lambdaExpr.setParameters(formalParams);
                    break;

                // Handle Expression
                case "Expression":
                    lambdaExpr.setExpr(aNode.getCustomNode(JExpr.class));
                    break;

                // Handle Block
                case "Block":
                    lambdaExpr.setBlock(aNode.getCustomNode(JStmtBlock.class));
                    break;
            }
        }

        protected Class<JExprLambda> getPartClass()  { return JExprLambda.class; }
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

                // Handle BooleanLiteral
                case "BooleanLiteral":
                    literalExpr.setLiteralType(JExprLiteral.LiteralType.Boolean);
                    break;

                // Handle IntegerLiteral
                case "IntegerLiteral": {
                    int len = valueStr.length();
                    char c = valueStr.charAt(len - 1);
                    if (c == 'l' || c == 'L')
                        literalExpr.setLiteralType(JExprLiteral.LiteralType.Long);
                    else literalExpr.setLiteralType(JExprLiteral.LiteralType.Integer);
                    break;
                }

                // Handle FloatLiteral
                case "FloatLiteral": {
                    int len = valueStr.length();
                    char c = valueStr.charAt(len - 1);
                    if (c == 'f' || c == 'F')
                        literalExpr.setLiteralType(JExprLiteral.LiteralType.Float);
                    else literalExpr.setLiteralType(JExprLiteral.LiteralType.Double);
                    break;
                }

                // Handle CharacterLiteral
                case "CharacterLiteral":
                    literalExpr.setLiteralType(JExprLiteral.LiteralType.Character);
                    break;

                // Handle StringLiteral
                case "StringLiteral":
                    literalExpr.setLiteralType(JExprLiteral.LiteralType.String);
                    break;
            }

            // Set value string
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
        protected void parsedOne(ParseNode aNode, String anId)
        {
        }

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
            // Do normal version
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