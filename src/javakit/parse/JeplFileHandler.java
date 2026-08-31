package javakit.parse;
import snap.parse.*;
import java.util.List;

/**
 * JeplFile Handler.
 */
public class JeplFileHandler extends JavaParserExpr.JNodeParseHandler<JFile> {

    // The class name
    private String _className;

    // The import names
    private List<String> _importNames;

    // A running ivar for batches of statements
    JInitializerDecl _initDecl;

    // The TypeDecl Modifiers
    JModifiers _mods;

    // A special zero length ParseToken for programmatically created nodes at file start
    private static ParseToken PHANTOM_TOKEN = new ParseToken.Builder().name("InputStart").pattern("").text("").build();

    /**
     * Constructor.
     */
    public JeplFileHandler(String className, List<String> importNames)
    {
        super();
        _className = className;
        _importNames = importNames;
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

            // Handle ImportDecl
            case "ImportDecl" -> {
                JImportDecl importDecl = aNode.getCustomNode(JImportDecl.class);
                jfile.addImportDecl(importDecl);
            }

            // Handle Modifiers: Hold on to mods
            case "Modifiers" -> _mods = aNode.getCustomNode(JModifiers.class);

            // Handle MethodDecl, FieldDecl
            case "MethodDecl", "FieldDecl" -> {
                JMemberDecl methodDecl = aNode.getCustomNode(JMemberDecl.class);
                methodDecl.setModifiers(_mods);
                _mods = null;
                classDecl.addBodyDecl(methodDecl);
                jfile.setEndToken(endToken);
                _initDecl = null;
            }

            // Handle ClassDecl, EnumDecl
            case "ClassDecl", "EnumDecl" -> {
                JClassDecl innerClassDecl = aNode.getCustomNode(JClassDecl.class);
                innerClassDecl.setModifiers(_mods);
                _mods = null;
                classDecl.addBodyDecl(innerClassDecl);
                jfile.setEndToken(endToken);
                _initDecl = null;
            }

            // Handle BlockStatement
            case "BlockStatement" -> {

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
        _importNames.forEach(importName -> addImportToJFile(jfile, importName));

        // Create/add ClassDecl
        JClassDecl classDecl = new JClassDecl();
        classDecl.setName(_className);
        classDecl.setStartToken(startToken);
        jfile.addClassDecl(classDecl);

        _initDecl = null;
        return jfile;
    }

    protected Class<JFile> getPartClass()
    {
        return JFile.class;
    }

    /**
     * This should never get called.
     */
    @Override
    protected ParseHandler<JFile> createBackupHandler()
    {
        System.err.println("JeplParser.createBackupHandler: This should never get called");
        return new JeplFileHandler(_className, _importNames);
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
        importDecl.getString();
        aFile.addImportDecl(importDecl);
    }
}
