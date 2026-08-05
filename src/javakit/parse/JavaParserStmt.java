/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.parse.*;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * A parser for java statements.
 */
@SuppressWarnings({"unused", "StringEquality"})
public class JavaParserStmt extends JavaParserExpr {

    /**
     * Statement Handler.
     */
    public static class StatementHandler extends JNodeParseHandler<JStmt> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle any child with JavaStatement
            if (aNode.getCustomNode() instanceof JStmt)
                _part = aNode.getCustomNode(JStmt.class);
        }

        protected Class<JStmt> getPartClass()  { return JStmt.class; }
    }

    /**
     * Modifiers Handler.
     * Modifiers { Modifier* }
     * Modifier { "public" | "static" | "protected" | "private" | "final" | "abstract" | ... | Annotation }
     */
    public static class ModifiersHandler extends JNodeParseHandler<JModifiers> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JModifiers modifiers = getPart();

            switch (anId) {
                case "public" -> modifiers.addValue(Modifier.PUBLIC);
                case "static" -> modifiers.addValue(Modifier.STATIC);
                case "protected" -> modifiers.addValue(Modifier.PROTECTED);
                case "private" -> modifiers.addValue(Modifier.PRIVATE);
                case "final" -> modifiers.addValue(Modifier.FINAL);
                case "abstract" -> modifiers.addValue(Modifier.ABSTRACT);
                case "synchronized" -> modifiers.addValue(Modifier.SYNCHRONIZED);
                case "native" -> modifiers.addValue(Modifier.NATIVE);
                case "transient" -> modifiers.addValue(Modifier.TRANSIENT);
                case "volatile" -> modifiers.addValue(Modifier.VOLATILE);
                case "strictfp" -> modifiers.addValue(Modifier.STRICT);
                case "default" -> modifiers.addDefault();
                case "sealed" -> modifiers.addSealed();
                default -> { } // "Modifier" or Annotation
            }
        }

        protected Class<JModifiers> getPartClass()  { return JModifiers.class; }
    }

    /**
     * AssertStatement Handler.
     */
    public static class AssertStatementHandler extends JNodeParseHandler<JStmtAssert> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JStmtAssert assertStmt = getPart();

            // Handle condition
            if (assertStmt.getConditional() == null) {
                JExpr condExpr = aNode.getCustomNode(JExpr.class);
                assertStmt.setConditional(condExpr);
            }

            // Handle expression
            else {
                JExpr expr = aNode.getCustomNode(JExpr.class);
                assertStmt.setExpr(expr);
            }
        }

        protected Class<JStmtAssert> getPartClass()  { return JStmtAssert.class; }
    }

    /**
     * LabeledStatement Handler.
     */
    public static class LabeledStatementHandler extends JNodeParseHandler<JStmtLabeled> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JStmtLabeled labeledStmt = getPart();

            switch (anId) {

                case "Identifier" -> {
                    JExprId exprId = aNode.getCustomNode(JExprId.class);
                    labeledStmt.setLabel(exprId);
                }

                case "Statement" -> {
                    JStmt stmt = aNode.getCustomNode(JStmt.class);
                    labeledStmt.setStatement(stmt);
                }
            }
        }

        protected Class<JStmtLabeled> getPartClass()  { return JStmtLabeled.class; }
    }

    /**
     * Block (Statement) Handler.
     */
    public static class BlockHandler extends JNodeParseHandler<JStmtBlock> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JStmtBlock block = getPart();

            // Handle Statements
            if (aNode.getCustomNode() instanceof JStmt)
                block.addStatement(aNode.getCustomNode(JStmt.class));
        }

        protected Class<JStmtBlock> getPartClass()  { return JStmtBlock.class; }
    }

    /**
     * BlockStatement Handler - translates VarDeclExpr and ClassDecl to JavaStatements.
     */
    public static class BlockStatementHandler extends JNodeParseHandler<JStmt> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                case "VarDeclExpr" -> {
                    JExprVarDecl varDeclExpr = aNode.getCustomNode(JExprVarDecl.class);
                    _part = new JStmtVarDecl(varDeclExpr);
                }

                case "Statement" -> _part = aNode.getCustomNode(JStmt.class);

                case "ClassDecl", "EnumDecl" -> {
                    JStmtClassDecl scd = new JStmtClassDecl();
                    scd.setClassDecl(aNode.getCustomNode(JClassDecl.class));
                    _part = scd;
                }
            }
        }

        protected Class<JStmt> getPartClass()  { return JStmt.class; }
    }

    /**
     * FormalParams Handler: "(" (FormalParam ("," FormalParam)*)? ")"
     */
    public static class FormalParamsHandler extends ParseHandler<JVarDecl[]> {

        // List of FormalParams
        private List<JVarDecl> _formalParams = new ArrayList<>();

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            if (anId == "FormalParam") {
                JVarDecl formalParam = aNode.getCustomNode(JVarDecl.class);
                _formalParams.add(formalParam);
            }
        }

        /**
         * Override to return array.
         */
        public JVarDecl[] parsedAll()  { return _formalParams.toArray(new JVarDecl[0]); }

        /**
         * Override to clear FormalParams list.
         */
        @Override
        public void reset()
        {
            super.reset();
            _formalParams.clear();
        }

        @Override
        protected Class<JVarDecl[]> getPartClass()  { return JVarDecl[].class; }
    }

    /**
     * FormalParam Handler: Modifiers ("final" | Annotation)? Type "..."? Identifier ("[" "]")*
     */
    public static class FormalParamHandler extends JNodeParseHandler<JVarDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JVarDecl varDecl = getPart();

            switch (anId) {

                // Handle Type: Check for union type (like in catch clause)
                case "Type" -> {
                    JType type = aNode.getCustomNode(JType.class);
                    if (varDecl.getType() != null)
                        type = JTypeUnion.addTypeToType(varDecl.getType(), type);
                    varDecl.setType(type);
                }

                // Handle vararg: Fix this
                case "..." -> {
                    JType varType = varDecl.getType();
                    varType.setArrayCount(varType.getArrayCount() + 1);
                }

                // Handle Identifier
                case "Identifier" -> varDecl.setId(aNode.getCustomNode(JExprId.class));

                // Handle ("[" "]")*
                case "[" -> {
                    JType varType = varDecl.getType();
                    varType.setArrayCount(varType.getArrayCount() + 1);
                }
            }
        }

        protected Class<JVarDecl> getPartClass()  { return JVarDecl.class; }
    }

    /**
     * VarDecl Handler: Identifier ("[" "]")* ("=" VarInit)?
     * VarInit: ArrayInit | Expression
     */
    public static class VarDeclHandler extends JNodeParseHandler<JVarDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JVarDecl varDecl = getPart();

            switch (anId) {

                case "Identifier" -> {
                    JExprId idExpr = aNode.getCustomNode(JExprId.class);
                    varDecl.setId(idExpr);
                }

                // Handle ("[" "]")*
                case "[" -> {
                    int arrayCount = varDecl.getArrayCount() + 1;
                    varDecl.setArrayCount(arrayCount);
                }

                // Handle ArrayInit, VarInit Expression
                case "ArrayInit", "Expression" -> {
                    JExpr initExpr = aNode.getCustomNode(JExpr.class);
                    varDecl.setInitExpr(initExpr);
                }
            }
        }

        protected Class<JVarDecl> getPartClass()  { return JVarDecl.class; }
    }

    /**
     * VarDeclExpr Handler: Modifiers Type VarDecl ("," VarDecl)*
     */
    public static class VarDeclExprHandler extends JNodeParseHandler<JExprVarDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JExprVarDecl varDeclExpr = getPart();

            switch (anId) {

                case "Modifiers" -> varDeclExpr.setMods(aNode.getCustomNode(JModifiers.class));

                case "Type" -> varDeclExpr.setType(aNode.getCustomNode(JType.class));

                case "VarDecl" -> {
                    JVarDecl varDecl = aNode.getCustomNode(JVarDecl.class);
                    varDeclExpr.addVarDecl(varDecl);
                }
            }
        }

        protected Class<JExprVarDecl> getPartClass()  { return JExprVarDecl.class; }
    }

    /**
     * EmptyStatement Handler.
     */
    public static class EmptyStatementHandler extends JNodeParseHandler<JStmtEmpty> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            getPart();
        }

        protected Class<JStmtEmpty> getPartClass()  { return JStmtEmpty.class; }
    }

    /**
     * ExprStatement Handler: PreIncrementExpr | PrimaryExpr ("++" | "--" | AssignOp Expression)?
     */
    public static class ExprStatementHandler extends JNodeParseHandler<JStmtExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JStmtExpr exprStmt = getPart();

            switch (anId) {

                // Handle PreIncrementExpr, PrimaryExpr: Set expression statement expression
                case "PreIncrementExpr", "PrimaryExpr" -> {
                    JExpr expr = (JExpr) aNode.getCustomNode();
                    exprStmt.setExpr(expr);
                }

                // Handle "++", "--": Reset expression statement expression to pre/post increment math expression
                case "++", "--" -> {
                    JExpr expr = exprStmt.getExpr();
                    JExprMath.Op op = anId == "++" ? JExprMath.Op.PostIncrement : JExprMath.Op.PostDecrement;
                    JExprMath unaryExpr = new JExprMath(op, expr);
                    exprStmt.setExpr(unaryExpr);
                }

                // Handle AssignOp: Reset expression statement expression to assign expression
                case "AssignOp" -> {
                    JExpr expr = exprStmt.getExpr();
                    ParseToken token = aNode.getStartToken();
                    String opStr = token.getString();
                    JExprAssign assignExpr = new JExprAssign(opStr, expr, null);
                    exprStmt.setExpr(assignExpr);
                }

                // Handle Expression: Should be assign expression
                case "Expression" -> {
                    JExpr expr = (JExpr) aNode.getCustomNode();
                    JExprAssign assignExpr = (JExprAssign) exprStmt.getExpr();
                    assignExpr.setValueExpr(expr);
                }
            }
        }

        protected Class<JStmtExpr> getPartClass()  { return JStmtExpr.class; }
    }

    /**
     * SwitchStatement Handler: SwitchExpr
     */
    public static class SwitchStatementHandler extends JNodeParseHandler<JStmtSwitch> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JStmtSwitch switchStmt = getPart();

            // Handle SwitchExpr
            if (anId == "SwitchExpr") {
                JExprSwitch switchExpr = aNode.getCustomNode(JExprSwitch.class);
                switchStmt.setSwitchExpr(switchExpr);
            }
        }

        protected Class<JStmtSwitch> getPartClass()  { return JStmtSwitch.class; }
    }

    /**
     * IfStatement Handler.
     */
    public static class IfStatementHandler extends JNodeParseHandler<JStmtIf> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JStmtIf ifStmt = getPart();

            switch (anId) {

                case "Expression" -> {
                    JExpr condExpr = aNode.getCustomNode(JExpr.class);
                    ifStmt.setConditional(condExpr);
                }

                case "Statement" -> {
                    JStmt bodyStmt = aNode.getCustomNode(JStmt.class);
                    if (ifStmt.getStatement() == null)
                        ifStmt.setStatement(bodyStmt);
                    else ifStmt.setElseStatement(bodyStmt);
                }
            }
        }

        protected Class<JStmtIf> getPartClass()  { return JStmtIf.class; }
    }

    /**
     * WhileStatement Handler.
     */
    public static class WhileStatementHandler extends JNodeParseHandler<JStmtWhile> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JStmtWhile whileStmt = getPart();

            switch (anId) {

                case "Expression" -> {
                    JExpr condExpr = aNode.getCustomNode(JExpr.class);
                    whileStmt.setConditional(condExpr);
                }

                case "Statement" -> {
                    JStmt bodyStmt = aNode.getCustomNode(JStmt.class);
                    whileStmt.setStatement(bodyStmt);
                }
            }
        }

        protected Class<JStmtWhile> getPartClass()  { return JStmtWhile.class; }
    }

    /**
     * DoStatement Handler.
     */
    public static class DoStatementHandler extends JNodeParseHandler<JStmtDo> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JStmtDo doStmt = getPart();

            switch (anId) {

                case "Statement" -> {
                    JStmt bodyStmt = aNode.getCustomNode(JStmt.class);
                    doStmt.setStatement(bodyStmt);
                }

                case "Expression" -> {
                    JExpr condExpr = aNode.getCustomNode(JExpr.class);
                    doStmt.setConditional(condExpr);
                }
            }
        }

        protected Class<JStmtDo> getPartClass()  { return JStmtDo.class; }
    }

    /**
     * ForStatement Handler.
     */
    public static class ForStatementHandler extends JNodeParseHandler<JStmtFor> {

        // The current part index (0 = init, 1 = conditional, 2 = update)
        private int  _partIndex = 0;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JStmtFor forStmt = getPart();

            switch (anId) {

                case "VarDeclExpr" -> {
                    JExprVarDecl varDeclExpr = aNode.getCustomNode(JExprVarDecl.class);
                    forStmt.setVarDeclExpr(varDeclExpr);
                }

                case "Expression" -> {
                    JExpr expr = aNode.getCustomNode(JExpr.class);
                    if (expr == null)
                        return;

                    // Handle ForEach expression or basic for conditional
                    if (forStmt.isForEach())
                        forStmt.setIterableExpr(expr);

                    // Handle basic for statement
                    else {
                        switch (_partIndex) {
                            case 0 -> forStmt.addInitExpr(expr);
                            case 1 -> forStmt.setConditional(expr);
                            case 2 -> forStmt.addUpdateExpr(expr);
                        }
                    }
                }


                // Handle basic for separator
                case ";" -> _partIndex++;

                // Handle ForEach separator
                case ":" -> forStmt._forEach = true;

                case "Statement" -> {
                    JStmt stmt = aNode.getCustomNode(JStmt.class);
                    forStmt.setStatement(stmt);
                }
            }
        }

        /**
         * Override to clear partIndex.
         */
        public JStmtFor parsedAll()
        {
            _partIndex = 0;
            return super.parsedAll();
        }

        protected Class<JStmtFor> getPartClass()  { return JStmtFor.class; }
    }

    /**
     * BreakStatement Handler.
     */
    public static class BreakStatementHandler extends JNodeParseHandler<JStmtBreak> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JStmtBreak breakStmt = getPart();

            // Handle Identifier
            if (anId == "Identifier") {
                JExprId labelId = aNode.getCustomNode(JExprId.class);
                breakStmt.setLabel(labelId);
            }
        }

        protected Class<JStmtBreak> getPartClass()  { return JStmtBreak.class; }
    }

    /**
     * ContinueStatement Handler.
     */
    public static class ContinueStatementHandler extends JNodeParseHandler<JStmtContinue> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JStmtContinue continueStmt = getPart();

            // Handle Identifier
            if (anId == "Identifier")
                continueStmt.setLabel(aNode.getCustomNode(JExprId.class));
        }

        protected Class<JStmtContinue> getPartClass()  { return JStmtContinue.class; }
    }

    /**
     * ReturnStatement Handler.
     */
    public static class ReturnStatementHandler extends JNodeParseHandler<JStmtReturn> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JStmtReturn returnStmt = getPart();

            // Handle Expression
            if (anId == "Expression")
                returnStmt.setExpr(aNode.getCustomNode(JExpr.class));
        }

        protected Class<JStmtReturn> getPartClass()  { return JStmtReturn.class; }
    }

    /**
     * YieldStatement Handler.
     */
    public static class YieldStatementHandler extends JNodeParseHandler<JStmtYield> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JStmtYield yieldStmt = getPart();

            // Handle Expression
            if (anId == "Expression")
                yieldStmt.setExpr(aNode.getCustomNode(JExpr.class));
        }

        protected Class<JStmtYield> getPartClass()  { return JStmtYield.class; }
    }

    /**
     * ThrowStatement Handler.
     */
    public static class ThrowStatementHandler extends JNodeParseHandler<JStmtThrow> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JStmtThrow throwStmt = getPart();

            // Handle Expression
            if (anId == "Expression")
                throwStmt.setExpr(aNode.getCustomNode(JExpr.class));
        }

        protected Class<JStmtThrow> getPartClass()  { return JStmtThrow.class; }
    }

    /**
     * SynchronizedStatement Handler.
     */
    public static class SynchronizedStatementHandler extends JNodeParseHandler<JStmtSynchronized> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JStmtSynchronized syncStmt = getPart();

            // Handle Expression
            if (anId == "Expression")
                syncStmt.setExpression(aNode.getCustomNode(JExpr.class));

            // Handle Block
            else if (anId == "Block")
                syncStmt.setBlock(aNode.getCustomNode(JStmtBlock.class));
        }

        protected Class<JStmtSynchronized> getPartClass()  { return JStmtSynchronized.class; }
    }

    /**
     * TryStatement Handler.
     */
    public static class TryStatementHandler extends JNodeParseHandler<JStmtTry> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            JStmtTry tryStmt = getPart();

            switch (anId) {

                // Handle Resource: VarDeclExpr | PrimaryExpr
                case "VarDeclExpr", "PrimaryExpr" -> {
                    JExpr resourceExpr = aNode.getCustomNode(JExpr.class);
                    tryStmt.addResource(resourceExpr);
                }

                case "Block" -> {
                    JStmtBlock blockStmt = aNode.getCustomNode(JStmtBlock.class);
                    if (tryStmt.getBlock() == null)
                        tryStmt.setBlock(blockStmt);
                    else tryStmt.addStatementBlock(blockStmt);
                }

                case "FormalParam" -> {
                    JStmtTryCatch catchNode = new JStmtTryCatch();
                    catchNode.setParameter(aNode.getCustomNode(JVarDecl.class));
                    tryStmt.addCatchBlock(catchNode);
                }
            }
        }

        protected Class<JStmtTry> getPartClass()  { return JStmtTry.class; }
    }
}