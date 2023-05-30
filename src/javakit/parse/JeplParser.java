/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import javakit.resolver.JavaType;
import javakit.resolver.Resolver;
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
    public JeplParser(JeplTextDoc aJeplTextDoc)
    {
        super();

        // Get/set rule for JeplFile
        ParseRule jeplRule = getRule("JeplFile");
        jeplRule.setHandler(new JeplFileHandler(aJeplTextDoc));
        setRule(jeplRule);
    }

    /**
     * JeplFile Handler.
     */
    public static class JeplFileHandler extends JNodeParseHandler<JFile> {

        // The JeplTextDoc that created this object
        private JeplTextDoc  _jeplTextDoc;

        // A running ivar for batches of statements
        JInitializerDecl  _initDecl;

        /**
         * Constructor.
         */
        public JeplFileHandler(JeplTextDoc aJeplTextDoc)
        {
            super();
            _jeplTextDoc = aJeplTextDoc;
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

            // Handle BlockStatement
            if (anId == "BlockStatement") {

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
            }

            // Handle Modifiers
            else if (anId == "Modifiers") {
                // Ignore for now
            }

            // Handle MethodDecl
            else if (anId == "MethodDecl") {
                JMethodDecl methodDecl = aNode.getCustomNode(JMethodDecl.class);
                classDecl.addMemberDecl(methodDecl);
                jfile.setEndToken(endToken);
                _initDecl = null;
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
            String[] importNames = _jeplTextDoc.getImports();
            for (String importName : importNames)
                addImportToJFile(jfile, importName);

            // Create/add ClassDecl
            JClassDecl classDecl = new JClassDecl();
            classDecl.setName("JavaShellREPL");
            classDecl.setStartToken(startToken);
            jfile.addClassDecl(classDecl);

            // Add Superclass
            String superClassName = _jeplTextDoc.getSuperClassName();
            JType extendsType = new JType.Builder().name(superClassName).token(startToken).build();
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
        protected ParseHandler createBackupHandler()
        {
            System.err.println("JeplParser.createBackupHandler: This should never get called");
            return new JeplFileHandler(_jeplTextDoc);
        }
    }

    /**
     * Creates and adds JImportDecl to JFile for given import path.
     */
    private static void addImportToJFile(JFile aFile, String anImportPathName)
    {
        // Get inclusive and path info
        boolean isInclusive = anImportPathName.endsWith(".*");
        String importPathName = isInclusive ? anImportPathName.substring(0, anImportPathName.length() - 2) : anImportPathName;

        // Create/configure/add ImportDecl
        JImportDecl importDecl = new JImportDecl();
        importDecl.setName(importPathName);
        importDecl.setInclusive(isInclusive);
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

        // Create VarDeclStatement and add VarDecl
        JStmtVarDecl varDeclStmt = new JStmtVarDecl();
        varDeclStmt.addVarDecl(varDecl);

        // Swap VarDecl statement in for expr statement
        JStmtBlock blockStmt = exprStmt.getParent(JStmtBlock.class);
        int index = blockStmt.removeStatement(exprStmt);
        blockStmt.addStatement(varDeclStmt, index);

        // Get initializer type
        JavaType initType = initializer.getEvalType();
        if (initType == null) {
            System.out.println("JeplTextDocUtils.fixIncompleteVarDecl: Failed to get init type for " + initializer.getString());
            Resolver resolver = exprStmt.getResolver();
            initType = resolver.getJavaClassForClass(Object.class);
        }

        // Create bogus type from initializer
        JType type = new JType.Builder().token(assignTo.getStartToken()).type(initType).build();
        varDecl.setType(type);
    }
}
