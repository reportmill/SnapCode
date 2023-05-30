/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.parse.*;
import java.lang.reflect.Modifier;
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
            JModifiers part = getPart();
            switch (anId) {
                case "public": part.addValue(Modifier.PUBLIC); break;
                case "static": part.addValue(Modifier.STATIC); break;
                case "protected": part.addValue(Modifier.PROTECTED); break;
                case "private": part.addValue(Modifier.PRIVATE); break;
                case "final": part.addValue(Modifier.FINAL); break;
                case "abstract": part.addValue(Modifier.ABSTRACT); break;
                case "synchronized": part.addValue(Modifier.SYNCHRONIZED); break;
                case "native": part.addValue(Modifier.NATIVE); break;
                case "transient": part.addValue(Modifier.TRANSIENT); break;
                case "volatile": part.addValue(Modifier.VOLATILE); break;
                case "strictfp": part.addValue(Modifier.STRICT); break;
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
     * BlockStatement Handler - translates VarDeclStmt and ClassDecl to JavaStatements.
     */
    public static class BlockStatementHandler extends JNodeParseHandler<JStmt> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            switch (anId) {

                // Handle VarDeclStmt
                case "VarDeclStmt": _part = aNode.getCustomNode(JStmtVarDecl.class); break;

                // Handle Statement
                case "Statement": _part = aNode.getCustomNode(JStmt.class); break;

                // Handle ClassDecl
                case "ClassDecl": {
                    JStmtClassDecl scd = new JStmtClassDecl();
                    scd.setClassDecl(aNode.getCustomNode(JClassDecl.class));
                    _part = scd;
                } break;
            }
        }

        protected Class<JStmt> getPartClass()  { return JStmt.class; }
    }

    /**
     * FormalParam Handler.
     */
    public static class FormalParamHandler extends JNodeParseHandler<JVarDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Type
            if (anId == "Type")
                getPart().setType(aNode.getCustomNode(JType.class));

            // Handle vararg
            else if (anId == "...")
                getPart().getType().setArrayCount(getPart().getType().getArrayCount() + 1);

            // Handle Identifier
            else if (anId == "Identifier")
                getPart().setId(aNode.getCustomNode(JExprId.class));

            // Handle ("[" "]")*
            else if (anId == "[")
                getPart().getType().setArrayCount(getPart().getArrayCount() + 1);
        }

        protected Class<JVarDecl> getPartClass()  { return JVarDecl.class; }
    }

    /**
     * VarDecl Handler.
     */
    public static class VarDeclHandler extends JNodeParseHandler<JVarDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Identifier
            if (anId == "Identifier")
                getPart().setId(aNode.getCustomNode(JExprId.class));

            // Handle ("[" "]")*
            else if (anId == "[")
                getPart().setArrayCount(getPart().getArrayCount() + 1);

            // Handle VarInit ArrayInit
            else if (anId == "ArrayInit")
                getPart().setArrayInits(aNode.getCustomNode(List.class));

            // Handle VarInit Expression
            else if (anId == "Expression")
                getPart().setInitializer(aNode.getCustomNode(JExpr.class));
        }

        protected Class<JVarDecl> getPartClass()  { return JVarDecl.class; }
    }

    /**
     * VarDeclStmt Handler.
     */
    public static class VarDeclStmtHandler extends JNodeParseHandler<JStmtVarDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Modifiers
            if (anId == "Modifiers")
                getPart().setMods(aNode.getCustomNode(JModifiers.class));

            // Handle Type
            else if (anId == "Type")
                getPart().setType(aNode.getCustomNode(JType.class));

            // Handle VarDecl(s)
            else if (anId == "VarDecl") {
                JVarDecl vd = aNode.getCustomNode(JVarDecl.class);
                getPart().addVarDecl(vd);
            }
        }

        protected Class<JStmtVarDecl> getPartClass()  { return JStmtVarDecl.class; }
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
     * ExprStatement Handler.
     */
    public static class ExprStatementHandler extends JNodeParseHandler<JStmtExpr> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get part and custom node
            JStmtExpr exprStmt = getPart();
            Object customNode = aNode.getCustomNode();

            // Handle JavaExpression rules
            if (customNode instanceof JExpr) {
                JExpr expr = (JExpr) customNode;

                // If no expression yet, just set
                JExpr stmtExpr = exprStmt.getExpr();
                if (stmtExpr == null)
                    exprStmt.setExpr(expr);

                // Otherwise assume it is broken assignment stmt and fill it in
                else {
                    JExprAssign assignExpr = new JExprAssign("=", stmtExpr, expr);
                    exprStmt.setExpr(assignExpr);
                }
            }

            // Handle post increment/decrement
            else if (anId == "++" || anId == "--") {
                JExpr expr = exprStmt.getExpr();
                JExprMath.Op op = anId == "++" ? JExprMath.Op.PostIncrement : JExprMath.Op.PostDecrement;
                JExprMath unaryExpr = new JExprMath(op, expr);
                exprStmt.setExpr(unaryExpr);
            }

            // Shouldn't be possible
            else System.err.println("ExprStatementHandler: Unexpected node: " + anId);
        }

        protected Class<JStmtExpr> getPartClass()  { return JStmtExpr.class; }
    }

    /**
     * SwitchStatement Handler: { "switch" "(" Expression ")" "{" (SwitchLabel BlockStatement*)* "}" }
     */
    public static class SwitchStatementHandler extends JNodeParseHandler<JStmtSwitch> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get Switch statement
            JStmtSwitch switchStmt = getPart();

            // Handle Expression
            if (anId == "Expression")
                switchStmt.setExpr(aNode.getCustomNode(JExpr.class));

            // Handle SwitchLabel
            else if (anId == "SwitchLabel")
                switchStmt.addSwitchCase(aNode.getCustomNode(JStmtSwitchCase.class));

            // Handle BlockStatement
            else if (anId == "BlockStatement") {
                List<JStmtSwitchCase> switchCases = switchStmt.getSwitchCases();
                JStmtSwitchCase switchCase = switchCases.get(switchCases.size() - 1);
                JStmt blockStmt = aNode.getCustomNode(JStmt.class);
                if (blockStmt != null) // Can be null when parse fails
                    switchCase.addStatement(blockStmt);
            }
        }

        protected Class<JStmtSwitch> getPartClass()  { return JStmtSwitch.class; }
    }

    /**
     * SwitchLabel Handler.
     */
    public static class SwitchLabelHandler extends JNodeParseHandler<JStmtSwitchCase> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get Switch case
            JStmtSwitchCase switchCase = getPart();

            // Handle Expression
            if (anId == "Expression") {
                JExpr caseExpr = aNode.getCustomNode(JExpr.class);
                switchCase.setExpr(caseExpr);
            }

            // Handle "default"
            else if (anId == "default")
                switchCase.setDefault(true);
        }

        protected Class<JStmtSwitchCase> getPartClass()  { return JStmtSwitchCase.class; }
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

                // Handle Type
                case "Type": {
                    JType type = aNode.getCustomNode(JType.class);
                    JStmtVarDecl svd = new JStmtVarDecl();
                    svd.setType(type);
                    forStmt.setInitDecl(svd);
                } break;

                // Handle Identifier
                case "Identifier": {
                    JExprId idExpr = aNode.getCustomNode(JExprId.class);
                    JVarDecl varDecl = new JVarDecl();
                    varDecl.setId(idExpr);
                    forStmt.getInitDecl().addVarDecl(varDecl);
                } break;

                // Handle ForInit VarDeclStmt
                case "VarDeclStmt": {
                    JStmtVarDecl varDeclStmt = aNode.getCustomNode(JStmtVarDecl.class);
                    forStmt.setInitDecl(varDeclStmt);
                } break;

                // Handle ForInit ExprStatement(s) or ForUpdate ExprStatement(s)
                case "ExprStatement": {
                    JStmtExpr se = aNode.getCustomNode(JStmtExpr.class);
                    if (_partIndex == 0)
                        forStmt.addInitStmt(se);
                    else forStmt.addUpdateStmt(se);
                } break;

                // Handle init or conditional Expression
                case "Expression": {
                    JExpr condExpr = aNode.getCustomNode(JExpr.class);
                    forStmt.setConditional(condExpr);
                } break;

                // Handle separator
                case ";": {
                    _partIndex++;
                    forStmt._forEach = false;
                } break;

                // Handle Statement
                case "Statement": {
                    JStmt stmt = aNode.getCustomNode(JStmt.class);
                    forStmt.setStatement(stmt);
                } break;
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

            // Handle Block
            if (anId == "Block") {
                JStmtBlock blockStmt = aNode.getCustomNode(JStmtBlock.class);
                if (tryStmt.getBlock() == null)
                    tryStmt.setBlock(blockStmt);
                else tryStmt.addStatementBlock(blockStmt);
            }

            // Handle FormalParam
            else if (anId == "FormalParam") {
                JStmtTryCatch catchNode = new JStmtTryCatch();
                catchNode.setParameter(aNode.getCustomNode(JVarDecl.class));
                tryStmt.addCatchBlock(catchNode);
            }
        }

        protected Class<JStmtTry> getPartClass()  { return JStmtTry.class; }
    }
}