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
            // Get modifiers
            JModifiers modifiers = getPart();

            switch (anId) {
                case "public": modifiers.addValue(Modifier.PUBLIC); break;
                case "static": modifiers.addValue(Modifier.STATIC); break;
                case "protected": modifiers.addValue(Modifier.PROTECTED); break;
                case "private": modifiers.addValue(Modifier.PRIVATE); break;
                case "final": modifiers.addValue(Modifier.FINAL); break;
                case "abstract": modifiers.addValue(Modifier.ABSTRACT); break;
                case "synchronized": modifiers.addValue(Modifier.SYNCHRONIZED); break;
                case "native": modifiers.addValue(Modifier.NATIVE); break;
                case "transient": modifiers.addValue(Modifier.TRANSIENT); break;
                case "volatile": modifiers.addValue(Modifier.VOLATILE); break;
                case "strictfp": modifiers.addValue(Modifier.STRICT); break;
                case "default": break; // Should we really treat as modifier? No support in java.lang.reflect.Modifier.
                default: break; // "Modifer" or Annotation
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
            // Get assert statement
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
            // Get labeled statement
            JStmtLabeled labeledStmt = getPart();

            switch (anId) {

                // Handle Identifier
                case "Identifier": {
                    JExprId exprId = aNode.getCustomNode(JExprId.class);
                    labeledStmt.setLabel(exprId);
                } break;

                // Handle Statement
                case "Statement": {
                    JStmt stmt = aNode.getCustomNode(JStmt.class);
                    labeledStmt.setStatement(stmt);
                } break;
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
            // Handle Statements
            JStmtBlock block = getPart();
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

                // Handle VarDeclExpr
                case "VarDeclExpr": {
                    JExprVarDecl varDeclExpr = aNode.getCustomNode(JExprVarDecl.class);
                    _part = new JStmtVarDecl(varDeclExpr);
                    break;
                }

                // Handle Statement
                case "Statement": _part = aNode.getCustomNode(JStmt.class); break;

                // Handle ClassDecl, EnumDecl
                case "ClassDecl": case "EnumDecl": {
                    JStmtClassDecl scd = new JStmtClassDecl();
                    scd.setClassDecl(aNode.getCustomNode(JClassDecl.class));
                    _part = scd;
                } break;
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
        protected Class getPartClass()  { return JVarDecl[].class; }
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
            // Get variable declaration
            JVarDecl varDecl = getPart();

            switch (anId) {

                // Handle Type
                case "Type":
                    varDecl.setType(aNode.getCustomNode(JType.class));
                    break;

                // Handle vararg: Fix this
                case "...": {
                    JType varType = varDecl.getType();
                    varType.setArrayCount(varType.getArrayCount() + 1);
                    break;
                }

                // Handle Identifier
                case "Identifier":
                    varDecl.setId(aNode.getCustomNode(JExprId.class));
                    break;

                // Handle ("[" "]")*
                case "[": {
                    JType varType = varDecl.getType();
                    varType.setArrayCount(varType.getArrayCount() + 1);
                    break;
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
            // Get variable declaration
            JVarDecl varDecl = getPart();

            switch (anId) {

                // Handle Identifier
                case "Identifier":
                    JExprId idExpr = aNode.getCustomNode(JExprId.class);
                    varDecl.setId(idExpr);
                    break;

                // Handle ("[" "]")*
                case "[":
                    int arrayCount = varDecl.getArrayCount() + 1;
                    varDecl.setArrayCount(arrayCount);
                    break;

                // Handle ArrayInit, VarInit Expression
                case "ArrayInit":
                case "Expression":
                    JExpr initExpr = aNode.getCustomNode(JExpr.class);
                    varDecl.setInitExpr(initExpr);
                    break;
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
            // Get variable declaration expression
            JExprVarDecl varDeclExpr = getPart();

            switch (anId) {

                // Handle Modifiers
                case "Modifiers":
                    varDeclExpr.setMods(aNode.getCustomNode(JModifiers.class));
                    break;

                // Handle Type
                case "Type":
                    varDeclExpr.setType(aNode.getCustomNode(JType.class));
                    break;

                // Handle VarDecl(s)
                case "VarDecl":
                    JVarDecl varDecl = aNode.getCustomNode(JVarDecl.class);
                    varDeclExpr.addVarDecl(varDecl);
                    break;
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
            // Get expression statement
            JStmtExpr exprStmt = getPart();

            switch (anId) {

                // Handle PreIncrementExpr, PrimaryExpr: Set expression statement expression
                case "PreIncrementExpr":
                case "PrimaryExpr": {
                    JExpr expr = (JExpr) aNode.getCustomNode();
                    exprStmt.setExpr(expr);
                    break;
                }

                // Handle "++", "--": Reset expression statement expression to pre/post increment math expression
                case "++":
                case "--": {
                    JExpr expr = exprStmt.getExpr();
                    JExprMath.Op op = anId == "++" ? JExprMath.Op.PostIncrement : JExprMath.Op.PostDecrement;
                    JExprMath unaryExpr = new JExprMath(op, expr);
                    exprStmt.setExpr(unaryExpr);
                    break;
                }

                // Handle AssignOp: Reset expression statement expression to assign expression
                case "AssignOp": {
                    JExpr expr = exprStmt.getExpr();
                    ParseToken token = aNode.getStartToken();
                    String opStr = token.getString();
                    JExprAssign assignExpr = new JExprAssign(opStr, expr, null);
                    exprStmt.setExpr(assignExpr);
                    break;
                }

                // Handle Expression: Should be assign expression
                case "Expression": {
                    JExpr expr = (JExpr) aNode.getCustomNode();
                    JExprAssign assignExpr = (JExprAssign) exprStmt.getExpr();
                    assignExpr.setValueExpr(expr);
                    break;
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
            // Get Switch statement
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
            // Get if statement
            JStmtIf ifStmt = getPart();

            switch (anId) {

                // Handle Expression
                case "Expression": {
                    JExpr condExpr = aNode.getCustomNode(JExpr.class);
                    ifStmt.setConditional(condExpr);
                } break;

                // Handle Statement
                case "Statement": {
                    JStmt bodyStmt = aNode.getCustomNode(JStmt.class);
                    if (ifStmt.getStatement() == null)
                        ifStmt.setStatement(bodyStmt);
                    else ifStmt.setElseStatement(bodyStmt);
                } break;
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
            // Get while statement
            JStmtWhile whileStmt = getPart();

            switch (anId) {

                // Handle Expression
                case "Expression": {
                    JExpr condExpr = aNode.getCustomNode(JExpr.class);
                    whileStmt.setConditional(condExpr);
                } break;

                // Handle Statement
                case "Statement": {
                    JStmt bodyStmt = aNode.getCustomNode(JStmt.class);
                    whileStmt.setStatement(bodyStmt);
                } break;
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
            // Get do statement
            JStmtDo doStmt = getPart();

            switch (anId) {

                // Handle Statement
                case "Statement": {
                    JStmt bodyStmt = aNode.getCustomNode(JStmt.class);
                    doStmt.setStatement(bodyStmt);
                } break;

                // Handle Expression
                case "Expression": {
                    JExpr condExpr = aNode.getCustomNode(JExpr.class);
                    doStmt.setConditional(condExpr);
                } break;
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
            // Get ForStmt
            JStmtFor forStmt = getPart();

            switch (anId) {

                // Handle VarDeclExpr
                case "VarDeclExpr": {
                    JExprVarDecl varDeclExpr = aNode.getCustomNode(JExprVarDecl.class);
                    forStmt.setVarDeclExpr(varDeclExpr);
                    break;
                }

                // Handle Expression
                case "Expression": {
                    JExpr expr = aNode.getCustomNode(JExpr.class);
                    if (expr == null)
                        return;

                    // Handle ForEach expression or basic for conditional
                    if (forStmt.isForEach())
                        forStmt.setIterableExpr(expr);

                    // Handle basic for statement
                    else {
                        switch (_partIndex) {
                            case 0: forStmt.addInitExpr(expr); break;
                            case 1: forStmt.setConditional(expr); break;
                            case 2: forStmt.addUpdateExpr(expr); break;
                        }
                    }

                    break;
                }

                // Handle basic for separator
                case ";":
                    _partIndex++;
                    break;

                // Handle ForEach separator
                case ":":
                    forStmt._forEach = true;
                    break;

                // Handle Statement
                case "Statement": {
                    JStmt stmt = aNode.getCustomNode(JStmt.class);
                    forStmt.setStatement(stmt);
                    break;
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
            // Get break statement
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
            // Get continue statement
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
            // Get return statement
            JStmtReturn returnStmt = getPart();

            // Handle Expression
            if (anId == "Expression")
                returnStmt.setExpr(aNode.getCustomNode(JExpr.class));
        }

        protected Class<JStmtReturn> getPartClass()  { return JStmtReturn.class; }
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
            // Get throw statement
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
            // Get sync statement
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
            // Get try statement
            JStmtTry tryStmt = getPart();

            switch (anId) {

                // Handle Resource: VarDeclExpr | PrimaryExpr
                case "VarDeclExpr":
                case "PrimaryExpr": {
                    JExpr resourceExpr = aNode.getCustomNode(JExpr.class);
                    tryStmt.addResource(resourceExpr);
                    break;
                }

                // Handle Block
                case "Block": {
                    JStmtBlock blockStmt = aNode.getCustomNode(JStmtBlock.class);
                    if (tryStmt.getBlock() == null)
                        tryStmt.setBlock(blockStmt);
                    else tryStmt.addStatementBlock(blockStmt);
                    break;
                }

                // Handle FormalParam
                case "FormalParam": {
                    JStmtTryCatch catchNode = new JStmtTryCatch();
                    catchNode.setParameter(aNode.getCustomNode(JVarDecl.class));
                    tryStmt.addCatchBlock(catchNode);
                    break;
                }
            }
        }

        protected Class<JStmtTry> getPartClass()  { return JStmtTry.class; }
    }
}