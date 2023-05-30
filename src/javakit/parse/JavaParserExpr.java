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
     * Expression Handler.
     */
    public static class ExpressionHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle ConditionalExpr
            switch (anId) {
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
            getPart().setName(aNode.getString());
        }

        protected Class<JExprId> getPartClass()  { return JExprId.class; }
    }

    /**
     * Name Handler.
     */
    public static class NameHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            if (anId == "Identifier") {
                JExprId idExpr = aNode.getCustomNode(JExprId.class);
                _part = JExpr.joinExpressions(_part, idExpr);
            }
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * Type Handler.
     */
    public static class TypeHandler extends JNodeParseHandler<JType> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle PrimitiveType
            switch (anId) {
                case "PrimitiveType":
                    _part = aNode.getCustomNode(JType.class);
                    break;

                // Handle ReferenceType."["
                case "[":
                    getPart().setArrayCount(getPart().getArrayCount() + 1);
                    break;

                // Handle ClassType
                case "ClassType":
                    _part = aNode.getCustomNode(JType.class);
                    break;
            }
        }

        protected Class<JType> getPartClass()  { return JType.class; }
    }

    /**
     * ClassType Handler.
     */
    public static class ClassTypeHandler extends JNodeParseHandler<JType> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle: Identifier [ TypeArgs ] ( "." Identifier [ TypeArgs ] ) *
            if (anId == "Identifier")
                if (getPart().getName() == null) getPart().setName(aNode.getString());
                else getPart().setName(getPart().getName() + '.' + aNode.getString());

                // Handle TypeArgs (ReferenceType)
            else if (aNode.getCustomNode() instanceof JType) {
                JType type = aNode.getCustomNode(JType.class);
                getPart().addTypeArg(type);
            }
        }

        protected Class<JType> getPartClass()  { return JType.class; }
    }

    /**
     * PrimitiveType Handler.
     */
    public static class PrimitiveTypeHandler extends JNodeParseHandler<JType> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle primitive types
            if (anId == "boolean" || anId == "char" || anId == "byte" || anId == "short" ||
                    anId == "int" || anId == "long" || anId == "float" || anId == "double")
                getPart().setName(anId);
            getPart().setPrimitive(true);
        }

        protected Class<JType> getPartClass()  { return JType.class; }
    }

    /**
     * ResultType Handler.
     */
    public static class ResultTypeHandler extends JNodeParseHandler<JType> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Type
            switch (anId) {
                case "Type":
                    _part = aNode.getCustomNode(JType.class);
                    break;

                // Handle void
                case "void":
                    getPart().setName("void");
                    getPart().setPrimitive(true);
                    break;
            }
        }

        protected Class<JType> getPartClass()  { return JType.class; }
    }

    /**
     * ConditionalExpr Handler.
     */
    public static class ConditionalExprHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle ConditionalOrExpr
            switch (anId) {
                case "ConditionalOrExpr":
                    _part = aNode.getCustomNode(JExpr.class);
                    break;
            }

            // Handle Expression
            if (anId == "Expression") {
                JExpr part = aNode.getCustomNode(JExpr.class);
                JExprMath opExpr = _part instanceof JExprMath ? (JExprMath) _part : null;
                if (opExpr == null || opExpr.op != JExprMath.Op.Conditional)
                    _part = new JExprMath(JExprMath.Op.Conditional, _part, part);
                else opExpr.setOperand(part, 2);
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
     * OpExpr Handler.
     */
    public static abstract class BinaryExprHandler extends ParseHandler<JExpr> {

        // The Op
        JExprMath.Op _op;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle KeyChain
            if (aNode.getCustomNode() instanceof JExpr) {
                JExpr part = aNode.getCustomNode(JExpr.class);
                if (_part == null)
                    _part = part;
                else {
                    _part = new JExprMath(_op, _part, part);
                    _op = null;
                }
            }

            // Handle Ops
            else _op = getOpForString(anId);
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
                default: throw new RuntimeException("Unknown op string: " + anId);
            }
        }
    }

    /**
     * UnaryExpr Handler.
     */
    public static class UnaryExprHandler extends JNodeParseHandler<JExpr> {

        // The current op
        JExprMath.Op _op;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle JavaExpression rules: PreIncrementExpr, PreDecrementExpr, UnaryExprNotPlusMinus
            if (aNode.getCustomNode() instanceof JExpr) {
                JExpr part = aNode.getCustomNode(JExpr.class);
                _part = _op == null ? part : new JExprMath(_op, part);
            }

            // Handle unary ops (ignore '+')
            else if (anId == "-") _op = JExprMath.Op.Negate;
            else if (anId == "~") _op = JExprMath.Op.BitComp;
            else if (anId == "!") _op = JExprMath.Op.Not;

            // Handle post Increment/Decrement
            else if (anId == "++" || anId == "--") {
                _op = anId == "++" ? JExprMath.Op.PostIncrement : JExprMath.Op.PostDecrement;
                if (_part != null) _part = new JExprMath(_op, _part);
            }
        }

        /**
         * Override to clear op.
         */
        public JExpr parsedAll()
        {
            _op = null;
            return super.parsedAll();
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * PreIncrementExpr Handler.
     */
    public static class PreIncrementExprHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            if (anId == "PrimaryExpr")
                _part = new JExprMath(JExprMath.Op.PreIncrement, aNode.getCustomNode(JExpr.class));
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * PreDecrementExpr Handler.
     */
    public static class PreDecrementExprHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            if (anId == "PrimaryExpr")
                _part = new JExprMath(JExprMath.Op.PreDecrement, aNode.getCustomNode(JExpr.class));
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
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
     * PrimaryExpr Handler.
     */
    public static class PrimaryExprHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle PrimaryPrefix
            switch (anId) {
                case "PrimaryPrefix":
                    _part = aNode.getCustomNode(JExpr.class);
                    break;

                // Handle PrimarySuffix: Join prefix and suffix
                case "PrimarySuffix":
                    JExpr expr = aNode.getCustomNode(JExpr.class);
                    _part = JExpr.joinExpressions(_part, expr);
                    break;
            }
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * PrimaryPrefix Handler.
     */
    public static class PrimaryPrefixHandler extends JNodeParseHandler<JExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Literal
            switch (anId) {
                case "Literal":
                    _part = aNode.getCustomNode(JExprLiteral.class);
                    break;

                // Handle Identifier of [ (Identifier ".")* this ] and [ "super" "." Identifier ]
                case "Identifier": {
                    JExprId idExpr = aNode.getCustomNode(JExprId.class);
                    _part = JExpr.joinExpressions(_part, idExpr);
                    break;
                }

                // Handle "this"/"super" of [ (Identifier ".")* this ] and [ "super" "." Identifier ]
                case "this":
                case "super":
                    JExprId id = new JExprId(aNode.getString());
                    id.setStartToken(aNode.getStartToken());
                    id.setEndToken(aNode.getEndToken());
                    _part = JExpr.joinExpressions(_part, id);
                    break;

                // Handle ClassType (using above to handle the rest: "." "super" "." Identifier
                case "ClassType":
                    JType classType = aNode.getCustomNode(JType.class);
                    _part = new JExprType(classType);
                    break;

                // Handle LambdaExpr
                case "LambdaExpr":
                    _part = aNode.getCustomNode(JExprLambda.class);
                    break;

                // Handle "(" Expression ")"
                case "Expression":
                    JExpr innerExpr = aNode.getCustomNode(JExpr.class);
                    _part = new JExprParen(innerExpr);
                    _part.setStartToken(getStartToken());
                    break;

                // Handle AllocExpr
                case "AllocExpr":
                    _part = aNode.getCustomNode(JExpr.class);
                    break;

                // Handle ResultType "." "class"
                case "ResultType":
                    JType resultType = aNode.getCustomNode(JType.class);
                    _part = new JExprType(resultType);
                    break;
                case "class": {
                    JExprId idExpr = new JExprId("class");
                    idExpr.setStartToken(aNode.getStartToken());
                    idExpr.setEndToken(aNode.getEndToken());
                    _part = JExpr.joinExpressions(_part, idExpr);
                    break;
                }

                // Handle Name
                case "Name":

                    // Handle Name chain expression
                    JExpr namePrime = aNode.getCustomNode(JExpr.class);
                    if (namePrime instanceof JExprChain) {
                        JExprChain nameChain = (JExprChain) namePrime;
                        for (int i = 0, iMax = nameChain.getExprCount(); i < iMax; i++)
                            _part = JExpr.joinExpressions(_part, nameChain.getExpr(i));
                    }

                    // Handle simple Name
                    else _part = JExpr.joinExpressions(_part, namePrime);
                    break;
            }
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * PrimarySuffix Handler.
     */
    public static class PrimarySuffixHandler extends JNodeParseHandler<JExpr> {

        boolean _methodRef;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle [ "." "super" ] and [ "." "this" ]
            switch (anId) {
                case "super":
                case "this":
                    _part = new JExprId(aNode.getString());
                    break;

                // Handle AllocExpr
                case "AllocExpr":
                    _part = aNode.getCustomNode(JExpr.class);
                    break;

                // Handle MemberSelector: TypeArgs Identifier (currently handed below without TypeArgs)
                //else if(anId=="TypeArgs") _part = aNode.getCustomNode(JavaExpression.class);

                // Handle "[" Expression "]"
                case "Expression":
                    _part = new JExprArrayIndex(null, aNode.getCustomNode(JExpr.class));
                    break;

                // Handle ("." | "::") Identifier
                case "Identifier":
                    JExprId id = aNode.getCustomNode(JExprId.class);
                    if (_methodRef) {
                        _part = new JExprMethodRef(null, id);
                        _methodRef = false;
                    }
                    else _part = id;
                    break;

                // Handle "::" Identifier
                case "::":
                    _methodRef = true;
                    break;

                // Handle Arguments
                case "Arguments":
                    List argsList = aNode.getCustomNode(List.class);
                    _part = new JExprMethodCall(null, argsList);
                    break;
            }
        }

        protected Class<JExpr> getPartClass()  { return JExpr.class; }
    }

    /**
     * Arguments Handler
     */
    public static class ArgumentsHandler extends ParseHandler<ArrayList<JExpr>> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            ArrayList<JExpr> argsExpr = getPart();

            // Handle Expression
            if (anId == "Expression")
                argsExpr.add(aNode.getCustomNode(JExpr.class));
        }

        @Override
        protected Class getPartClass()  { return ArrayList.class; }
    }

    /**
     * AllocExpr Handler.
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

            // Handle PrimitiveType
            switch (anId) {
                case "PrimitiveType":
                    allocExpr.setType(aNode.getCustomNode(JType.class));
                    break;

                // Handle ArrayDimsAndInits
                case "Expression":
                    if (allocType != null && allocType.isArrayType())
                        allocExpr.setArrayDims(aNode.getCustomNode(JExpr.class));
                    break;

                // Handle ArrayDimsAndInits ArrayInit
                case "ArrayInit":
                    allocExpr.setArrayInits(aNode.getCustomNode(List.class));
                    break;

                // Handle ClassType
                case "ClassType":
                    allocExpr.setType(aNode.getCustomNode(JType.class));
                    break;

                // Handle TypeArgs, ArrayDimsAndInits
                case "[":
                    if (allocType != null)
                        allocType.setArrayCount(allocType.getArrayCount() + 1);
                    break;

                // Handle Arguments
                case "Arguments":
                    allocExpr.setArgs(aNode.getCustomNode(List.class));
                    break;

                // Handle ClassBody
                case "ClassBody":
                    JClassDecl classDecl = aNode.getCustomNode(JClassDecl.class);
                    classDecl.addExtendsType(allocType);
                    allocExpr.setClassDecl(classDecl);
                    break;
            }
        }

        /**
         * Override to make sure there is a type.
         */
        @Override
        public JExprAlloc parsedAll()
        {
            // Do normal version
            JExprAlloc allocExpr = super.parsedAll();

            // If no type, add bogus
            if (allocExpr.getType() == null) {
                ParseToken token = allocExpr.getStartToken();
                JType type = new JType.Builder().name("Object").token(token).build();
                allocExpr.setType(type);
            }

            // Return
            return allocExpr;
        }

        protected Class<JExprAlloc> getPartClass()  { return JExprAlloc.class; }
    }

    /**
     * ArrayInit Handler
     */
    public static class ArrayInitHandler extends ParseHandler<ArrayList<JExpr>> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            ArrayList<JExpr> arrayInitExpr = getPart();

            // Handle Expression
            if (anId == "Expression")
                arrayInitExpr.add(aNode.getCustomNode(JExpr.class));
        }

        @Override
        protected Class getPartClass()  { return ArrayList.class; }
    }

    /**
     * LambdaExpr Handler.
     */
    public static class LambdaExprHandler extends JNodeParseHandler<JExprLambda> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Identifier
            switch (anId) {
                case "Identifier":
                    JVarDecl vd = new JVarDecl();
                    vd.setId(aNode.getCustomNode(JExprId.class));
                    getPart().addParam(vd);
                    break;

                // Handle FormalParam
                case "FormalParam":
                    getPart().addParam(aNode.getCustomNode(JVarDecl.class));
                    break;

                // Handle Expression
                case "Expression":
                    getPart().setExpr(aNode.getCustomNode(JExpr.class));
                    break;

                // Handle Block
                case "Block":
                    getPart().setBlock(aNode.getCustomNode(JStmtBlock.class));
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

            // Handle BooleanLiteral
            switch (anId) {
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