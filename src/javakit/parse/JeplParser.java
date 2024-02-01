/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import snap.parse.*;

import java.lang.reflect.Modifier;

/**
 * This JavaParser subclass supports parsing Java code with implicit class and main declarations.
 */
public class JeplParser extends JavaParser {

    // A special zero length ParseToken for programmatically created nodes at file start
    private static ParseToken PHANTOM_TOKEN = new ParseToken.Builder().name("InputStart").pattern("").text("").build();

    /**
     * Constructor.
     */
    public JeplParser(String className, String[] importNames, String superClassName)
    {
        super();

        // Get/set rule for JeplFile
        ParseRule jeplRule = getRule("JeplFile");
        JeplFileHandler jeplFileHandler = new JeplFileHandler(className, importNames, superClassName);
        jeplRule.setHandler(jeplFileHandler);
        setRule(jeplRule);
    }

    /**
     * JeplFile Handler.
     */
    public static class JeplFileHandler extends JNodeParseHandler<JFile> {

        // The class name
        private String _className;

        // The import names
        private String[] _importNames;

        // The super class name
        private String _superClassName;

        // A running ivar for batches of statements
        JInitializerDecl  _initDecl;

        // The TypeDecl Modifiers
        JModifiers _mods;

        /**
         * Constructor.
         */
        public JeplFileHandler(String className, String[] importNames, String superClassName)
        {
            super();
            _className = className;
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
                        classDecl.addBodyDecl(_initDecl);

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
                case "Modifiers":
                    _mods = aNode.getCustomNode(JModifiers.class);
                    break;

                // Handle MethodDecl
                case "MethodDecl": {
                    JMethodDecl methodDecl = aNode.getCustomNode(JMethodDecl.class);
                    methodDecl.setModifiers(_mods); _mods = null;
                    classDecl.addBodyDecl(methodDecl);
                    jfile.setEndToken(endToken);
                    _initDecl = null;
                    break;
                }

                // Handle EnumDecl
                case "EnumDecl": {
                    JClassDecl enumDecl = aNode.getCustomNode(JClassDecl.class);
                    enumDecl.setModifiers(_mods); _mods = null;
                    classDecl.addBodyDecl(enumDecl);
                    jfile.setEndToken(endToken);
                    _initDecl = null;
                    break;
                }
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
            JModifiers modifiers = new JModifiers(Modifier.PUBLIC);
            modifiers.setStartToken(startToken);
            modifiers.getString();
            classDecl.setModifiers(modifiers);
            classDecl.setName(_className);
            classDecl.setStartToken(startToken);
            jfile.addClassDecl(classDecl);

            // Add Superclass
            JType extendsType = JType.createTypeForNameAndToken(_superClassName, startToken);
            extendsType.getString(); // Cache string
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
            return new JeplFileHandler(_className, _importNames, _superClassName);
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
        importDecl.getString(); // Cache string
        aFile.addImportDecl(importDecl);
    }
}
