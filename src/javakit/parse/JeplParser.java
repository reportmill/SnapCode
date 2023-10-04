/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.parse.*;

/**
 * This class.
 */
public class JeplParser extends JavaParser {

    // A special zero length ParseToken for programmatically created nodes at file start
    private static ParseToken PHANTOM_TOKEN = new ParseToken.Builder().name("InputStart").pattern("").text("").build();

    /**
     * Constructor.
     */
    public JeplParser(String[] importNames, String superClassName)
    {
        super();

        // Get/set rule for JeplFile
        ParseRule jeplRule = getRule("JeplFile");
        JeplFileHandler jeplFileHandler = new JeplFileHandler(importNames, superClassName);
        jeplRule.setHandler(jeplFileHandler);
        setRule(jeplRule);
    }

    /**
     * JeplFile Handler.
     */
    public static class JeplFileHandler extends JNodeParseHandler<JFile> {

        // The import names
        private String[] _importNames;

        // The super class name
        private String _superClassName;

        // A running ivar for batches of statements
        JInitializerDecl  _initDecl;

        /**
         * Constructor.
         */
        public JeplFileHandler(String[] importNames, String superClassName)
        {
            super();
            _importNames = importNames;
            _superClassName = superClassName;
        }

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get JFile and update end token
            JFile jfile = getPart();
            ParseToken endToken = aNode.getEndToken();
            jfile.setEndToken(endToken);

            // Get ClassDecl - create/add if not yet set
            JClassDecl classDecl = jfile.getClassDecl();

            switch (anId) {

                // Handle BlockStatement
                case "BlockStatement":

                    // If no current InitDecl, create (with statement block) and add
                    if (_initDecl == null) {

                        // Create InitDecl and add to class
                        _initDecl = new JInitializerDecl();
                        _initDecl.setStartToken(aNode.getStartToken());
                        classDecl.addMemberDecl(_initDecl);

                        // Create block statement and add to InitDecl
                        JStmtBlock blockStmt = new JStmtBlock();
                        blockStmt.setStartToken(aNode.getStartToken());
                        _initDecl.setBlock(blockStmt);
                    }

                    // Add block statement to current InitDecl.Block
                    JStmtBlock initDeclBlock = _initDecl.getBlock();
                    JStmt blockStmt = aNode.getCustomNode(JStmt.class);
                    initDeclBlock.addStatement(blockStmt);

                    // Update end tokens
                    _initDecl.setEndToken(endToken);
                    classDecl.setEndToken(endToken);
                    break;

                // Handle ImportDecl
                case "ImportDecl":
                    JImportDecl importDecl = aNode.getCustomNode(JImportDecl.class);
                    jfile.addImportDecl(importDecl);
                    break;

                // Handle Modifiers: Ignore for now
                case "Modifiers": break;

                // Handle MethodDecl
                case "MethodDecl":
                    JMethodDecl methodDecl = aNode.getCustomNode(JMethodDecl.class);
                    classDecl.addMemberDecl(methodDecl);
                    jfile.setEndToken(endToken);
                    _initDecl = null;
                    break;
            }
        }

        /**
         * Override to create JFile with implied ClassDecl and ImportDecls.
         */
        @Override
        protected JFile createPart()
        {
            // Do normal version
            JFile jfile = super.createPart();
            ParseToken startToken = PHANTOM_TOKEN; //getStartToken();
            jfile.setStartToken(startToken);

            // Create/add JImportDecls
            for (String importName : _importNames)
                addImportToJFile(jfile, importName);

            // Create/add ClassDecl
            JClassDecl classDecl = new JClassDecl();
            classDecl.setName("JavaShellREPL");
            classDecl.setStartToken(startToken);
            jfile.addClassDecl(classDecl);

            // Add Superclass
            JType extendsType = JType.createTypeForNameAndToken(_superClassName, startToken);
            classDecl.addExtendsType(extendsType);

            _initDecl = null;

            // Return
            return jfile;
        }

        protected Class<JFile> getPartClass()  { return JFile.class; }

        /**
         * This should never get called.
         */
        @Override
        protected ParseHandler<JFile> createBackupHandler()
        {
            System.err.println("JeplParser.createBackupHandler: This should never get called");
            return new JeplFileHandler(_importNames, _superClassName);
        }
    }

    /**
     * Creates and adds JImportDecl to JFile for given import path.
     */
    private static void addImportToJFile(JFile aFile, String anImportPathName)
    {
        // Get import path name and static/inclusive
        String importPathName = anImportPathName;
        boolean isStatic = anImportPathName.startsWith("static ");
        if (isStatic)
            importPathName = importPathName.substring("static ".length()).trim();
        boolean isInclusive = importPathName.endsWith(".*");
        if (isInclusive)
            importPathName = importPathName.substring(0, importPathName.length() - 2);

        // Create/configure/add ImportDecl
        JImportDecl importDecl = new JImportDecl();
        importDecl.setName(importPathName);
        importDecl.setInclusive(isInclusive);
        importDecl.setStatic(isStatic);
        importDecl.setStartToken(PHANTOM_TOKEN);
        importDecl.setEndToken(PHANTOM_TOKEN);
        aFile.addImportDecl(importDecl);
    }

    /**
     * Returns an array of statements for given JFile.
     */
    public static void findAndFixIncompleteVarDecls(JNode aJNode)
    {
        // Handle expression statement
        if (aJNode instanceof JStmtExpr) {
            JStmtExpr exprStmt = (JStmtExpr) aJNode;
            if (isIncompleteVarDecl(exprStmt))
                fixIncompleteVarDecl(exprStmt);
        }

        // Otherwise recurse
        else {
            for (int i = 0, iMax = aJNode.getChildCount(); i < iMax; i++) {
                JNode child = aJNode.getChild(i);
                findAndFixIncompleteVarDecls(child);
            }
        }
    }

    /**
     * Returns whether expression statement is really a variable decl without type.
     */
    private static boolean isIncompleteVarDecl(JStmtExpr exprStmt)
    {
        // Get expression
        JExpr expr = exprStmt.getExpr();

        // If assignment, check for undefined 'AssignTo' type
        if (expr instanceof JExprAssign) {
            JExprAssign assignExpr = (JExprAssign) expr;
            JExpr assignTo = assignExpr.getIdExpr();
            if (assignTo instanceof JExprId && assignTo.getDecl() == null && assignExpr.getValueExpr() != null)
                return true;
        }

        // Return
        return false;
    }

    /**
     * Fixes incomplete VarDecl.
     */
    private static void fixIncompleteVarDecl(JStmtExpr exprStmt)
    {
        // Get expr statement, assign expression and assign-to expression
        JExprAssign assignExpr = (JExprAssign) exprStmt.getExpr();
        JExprId assignTo = (JExprId) assignExpr.getIdExpr();

        // Create VarDecl from Id and initializer
        JVarDecl varDecl = new JVarDecl();
        varDecl.setId(assignTo);
        JExpr initializer = assignExpr.getValueExpr();
        varDecl.setInitializer(initializer);

        // Create VarDecl statement with 'var' type and add VarDecl
        JExprVarDecl varDeclExpr = new JExprVarDecl();
        JType varType = JType.createTypeForNameAndToken("var", exprStmt.getStartToken());
        varDeclExpr.setType(varType);
        varDeclExpr.addVarDecl(varDecl);
        JStmtVarDecl varDeclStmt = new JStmtVarDecl();
        varDeclStmt.setVarDeclExpr(varDeclExpr);

        // Swap VarDecl statement in for expr statement
        JStmtBlock blockStmt = exprStmt.getParent(JStmtBlock.class);
        int index = blockStmt.removeStatement(exprStmt);
        blockStmt.addStatement(varDeclStmt, index);
    }
}
