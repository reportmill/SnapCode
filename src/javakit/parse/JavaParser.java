/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import java.util.stream.Stream;
import snap.parse.*;

/**
 * A parser for java files.
 */
@SuppressWarnings({"unused", "StringEquality"})
public class JavaParser extends JavaParserStmt {

    // The exception, if one was hit
    private Exception  _exception;

    // An expression parser created from subset of JavaParser
    private Parser  _exprParser;

    // A statement parser created from subset of JavaParser
    private Parser  _stmtParser;

    // An imports parser created from subset of JavaParser
    private Parser  _importsParser;

    // The shared parser
    private static JavaParser  _shared;

    /**
     * Constructor.
     */
    public JavaParser()
    {
        super();
    }

    /**
     * Returns the shared parser.
     */
    public static JavaParser getShared()
    {
        if (_shared != null) return _shared;
        return _shared = new JavaParser();
    }

    /**
     * Returns the shared expression parser.
     */
    public Parser getExprParser()
    {
        if (_exprParser != null) return _exprParser;
        ParseRule exprRule = getShared().getRule("Expression");
        return _exprParser = new Parser(exprRule);
    }

    /**
     * Returns the shared expression parser.
     */
    public Parser getStmtParser()
    {
        if (_stmtParser != null) return _stmtParser;
        ParseRule stmtRule = getShared().getRule("Statement");
        return _stmtParser = new Parser(stmtRule);
    }

    /**
     * Returns the shared imports parser.
     */
    public Parser getImportsParser()
    {
        if (_importsParser != null) return _importsParser;
        Parser javaParser = new JavaParser();
        ParseRule importsRule = javaParser.getRule("JavaFileImports");
        javaParser.setRule(importsRule);
        return _importsParser = javaParser;
    }

    /**
     * Override so subclasses will find grammar file.
     */
    protected ParseRule createRule()
    {
        // Create rule
        ParseRule rule = ParseUtils.loadRule(JavaParser.class, null);

        // Install handlers from list
        for (Class<? extends ParseHandler<?>> handlerClass : _handlerClasses)
            ParseUtils.installHandlerForClass(handlerClass, rule);

        // Return
        return rule;
    }

    /**
     * Returns a JavaFile for input Java.
     */
    public JFile getJavaFile(CharSequence anInput)
    {
        // Clear exception
        _exception = null;

        // If no input, just return
        if (anInput.length() == 0)
            return new JFile();

        // Get parse node
        ParseNode node = null;
        try {
            node = parse(anInput);
        }

        // Catch ParseException
        catch (ParseException e) {
            if (_exception == null)
                _exception = e;
        }

        // Catch other exception (probably Tokenizer)
        catch (Exception e) {
            _exception = e;
            ParseToken token = getToken();
            if (token != null) {
                int lineNum = token.getLineIndex() + 1;
                System.err.println("JavaParser.getJavaFile: Exception at line " + lineNum);
            }
            e.printStackTrace();
        }

        // Get JFile
        JFile jfile = node != null ? node.getCustomNode(JFile.class) : null;
        if (jfile == null)
            jfile = new JFile();

        // Set string
        jfile.setJavaFileString(anInput.toString());

        // Set Exception
        jfile.setException(_exception);

        // Return
        return jfile;
    }

    /**
     * Override to ignore exception.
     */
    protected void parseFailed(ParseRule aRule, ParseHandler aHandler)
    {
        if (_exception == null) {
            _exception = new ParseException(this, aRule);
            //System.err.println("JavaParse: " + _exception);
        }
    }

    /**
     * Override to declare tokenizer as JavaTokenizer.
     */
    public CodeTokenizer getTokenizer()
    {
        return (CodeTokenizer) super.getTokenizer();
    }

    /**
     * Returns the tokenizer.
     */
    @Override
    protected Tokenizer createTokenizer()
    {
        CodeTokenizer tokenizer = new CodeTokenizer();
        tokenizer.setReadSingleLineComments(true);
        tokenizer.setReadMultiLineComments(true);
        return tokenizer;
    }

    /**
     * Java File Handler.
     */
    public static class JavaFileHandler extends JNodeParseHandler<JFile> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle PackageDecl
            if (anId == "PackageDecl")
                getPart().setPackageDecl(aNode.getCustomNode(JPackageDecl.class));

            // Handle ImportDecl
            else if (anId == "ImportDecl")
                getPart().addImportDecl(aNode.getCustomNode(JImportDecl.class));

            // Handle TypeDecl
            else if (anId == "TypeDecl") {
                if (aNode.getCustomNode() != null)
                    getPart().addClassDecl(aNode.getCustomNode(JClassDecl.class));
            }
        }

        protected Class<JFile> getPartClass()  { return JFile.class; }
    }

    /**
     * Java File Handler.
     */
    public static class JavaFileImportsHandler extends JNodeParseHandler<JFile> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle PackageDecl
            if (anId == "PackageDecl")
                getPart().setPackageDecl(aNode.getCustomNode(JPackageDecl.class));

            // Handle ImportDecl
            else if (anId == "ImportDecl")
                getPart().addImportDecl(aNode.getCustomNode(JImportDecl.class));
        }

        protected Class<JFile> getPartClass()  { return JFile.class; }
    }

    /**
     * PackageDecl Handler.
     */
    public static class PackageDeclHandler extends JNodeParseHandler<JPackageDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get packageDecl
            JPackageDecl packageDecl = getPart();

            // Handle Modifiers
            //if(anId=="Modifiers") getPart().setMods(aNode.getCustomNode(JModifiers.class));

            // Handle Name
            if (anId == "Name") {
                JExpr nameExpr = aNode.getCustomNode(JExpr.class);
                packageDecl.setNameExpr(nameExpr);
            }
        }

        protected Class<JPackageDecl> getPartClass()  { return JPackageDecl.class; }
    }

    /**
     * ImportDecl Handler.
     */
    public static class ImportDeclHandler extends JNodeParseHandler<JImportDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get importDecl
            JImportDecl importDecl = getPart();

            // Handle static
            if (anId == "static")
                importDecl.setStatic(true);

            // Handle Name
            else if (anId == "Name") {
                JExpr nameExpr = aNode.getCustomNode(JExpr.class);
                importDecl.setNameExpr(nameExpr);
            }

            // Handle '*'
            else if (anId == "*")
                importDecl.setInclusive(true);
        }

        protected Class<JImportDecl> getPartClass()  { return JImportDecl.class; }
    }

    /**
     * TypeDecl Handler.
     */
    public static class TypeDeclHandler extends JNodeParseHandler<JClassDecl> {

        // The TypeDecl Modifiers
        JModifiers _mods;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle modifiers
            if (anId == "Modifiers")
                _mods = aNode.getCustomNode(JModifiers.class);

                // Handle ClassDecl, EnumDecl or AnnotationDecl
            else if (aNode.getCustomNode() instanceof JClassDecl) {
                _part = aNode.getCustomNode(JClassDecl.class);
                _part.setMods(_mods);
                _mods = null;
            }
        }

        protected Class<JClassDecl> getPartClass()  { return JClassDecl.class; }
    }

    /**
     * ClassDecl Handler.
     */
    public static class ClassDeclHandler extends JNodeParseHandler<JClassDecl> {

        // Whether in 'extends' mode (as opposed to 'implements' mode
        boolean _extending;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get class decl
            JClassDecl classDecl = getPart();

            // Handle ClassBodyDecl (JavaMembers): ClassDecl, EnumDecl,
            // ConstrDecl, FieldDecl, MethodDecl, AnnotationDecl
            if (aNode.getCustomNode() instanceof JMemberDecl) {
                JMemberDecl memberDecl = aNode.getCustomNode(JMemberDecl.class);
                classDecl.addMemberDecl(memberDecl);
            }

            // Handle "class" or "interface"
            else if (anId == "interface")
                classDecl.setClassType(JClassDecl.ClassType.Interface);

            // Handle Identifier
            else if (anId == "Identifier")
                classDecl.setId(aNode.getCustomNode(JExprId.class));

            // Handle TypeParams
            else if (anId == "TypeParams")
                classDecl.setTypeVars(aNode.getCustomNode(List.class));

            // Handle ExtendsList or ImplementsList mode and extendsList/implementsList
            else if (anId == "extends")
                _extending = true;
            else if (anId == "implements")
                _extending = false;
            else if (anId == "ClassType") {
                JType type = aNode.getCustomNode(JType.class);
                if (_extending)
                    classDecl.addExtendsType(type);
                else classDecl.addImplementsType(type);
            }
        }

        protected Class<JClassDecl> getPartClass()  { return JClassDecl.class; }
    }

    /**
     * ClassBodyDecl Handler.
     */
    public static class ClassBodyDeclHandler extends JNodeParseHandler<JMemberDecl> {

        // Modifiers
        JModifiers _mods;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Modifiers
            if (anId == "Modifiers")
                _mods = aNode.getCustomNode(JModifiers.class);

            // Handle Member
            else if (aNode.getCustomNode() instanceof JMemberDecl) {
                _part = aNode.getCustomNode(JMemberDecl.class);
                _part.setMods(_mods);
                _mods = null;
            }
        }

        protected Class<JMemberDecl> getPartClass()  { return JMemberDecl.class; }
    }

    /**
     * Initializer Handler.
     */
    public static class InitializerHandler extends JNodeParseHandler<JInitializerDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get initializer decl
            JInitializerDecl initDecl = getPart();

            // Handle "static"
            if (anId == "static")
                initDecl.setStatic(true);

            // Handle Block
            else if (anId == "Block")
                initDecl.setBlock(aNode.getCustomNode(JStmtBlock.class));
        }

        protected Class<JInitializerDecl> getPartClass()  { return JInitializerDecl.class; }
    }

    /**
     * EnumDecl Handler.
     */
    public static class EnumDeclHandler extends JNodeParseHandler<JClassDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get enum decl
            JClassDecl enumDecl = getPart();

            // Handle MethodDeclarator Identifier
            if (anId == "Identifier")
                enumDecl.setId(aNode.getCustomNode(JExprId.class));

            // Handle ImplementsList ClassType
            else if (anId == "ClassType")
                enumDecl.getImplementsTypes().add(aNode.getCustomNode(JType.class));

            // Handle EnumConstant
            else if (anId == "EnumConstant")
                enumDecl.addEnumConstant(aNode.getCustomNode(JEnumConst.class));

            // Handle ClassBodyDecl (JMemberDecl): ClassDecl, EnumDecl, ConstrDecl, FieldDecl, MethodDecl, AnnotationDecl
            else if (aNode.getCustomNode() instanceof JMemberDecl)
                enumDecl.addMemberDecl(aNode.getCustomNode(JMemberDecl.class));
        }

        /**
         * Override to set ClassType Enum.
         */
        public JClassDecl createPart()
        {
            JClassDecl cd = new JClassDecl();
            cd.setClassType(JClassDecl.ClassType.Enum);
            return cd;
        }
    }

    /**
     * EnumConstant Handler.
     */
    public static class EnumConstantHandler extends JNodeParseHandler<JEnumConst> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get enum constant
            JEnumConst enumConst = getPart();

            // Handle Modifiers
            if (anId == "Modifiers")
                enumConst.setMods(aNode.getCustomNode(JModifiers.class));

            // Handle name Identifier
            else if (anId == "Identifier")
                enumConst.setId(aNode.getCustomNode(JExprId.class));

            // Handle Arguments
            else if (anId == "Arguments")
                enumConst.setArgs(aNode.getCustomNode(List.class));

            // Handle ClassBody
            else if (anId == "ClassBody")
                enumConst.setClassBody(aNode.getString());
        }

        protected Class<JEnumConst> getPartClass()  { return JEnumConst.class; }
    }

    /**
     * TypeParam Handler.
     */
    public static class TypeParamHandler extends JNodeParseHandler<JTypeVar> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get type var
            JTypeVar typeVar = getPart();

            // Handle Identifier
            if (anId == "Identifier")
                typeVar.setId(aNode.getCustomNode(JExprId.class));

            // Handle ClassType
            else if (anId == "ClassType")
                typeVar.addType(aNode.getCustomNode(JType.class));
        }

        protected Class<JTypeVar> getPartClass()  { return JTypeVar.class; }
    }

    /**
     * TypeParams Handler.
     */
    public static class TypeParamsHandler extends ParseHandler<ArrayList<JTypeVar>> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle TypeParam
            if (anId == "TypeParam")
                getPart().add(aNode.getCustomNode(JTypeVar.class));
        }

        @Override
        protected Class getPartClass()
        {
            return ArrayList.class;
        }
    }

    /**
     * FieldDecl Handler.
     */
    public static class FieldDeclHandler extends JNodeParseHandler<JFieldDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get field decl
            JFieldDecl fieldDecl = getPart();

            switch (anId) {

                // Handle Type
                case "Type":
                    fieldDecl.setType(aNode.getCustomNode(JType.class));
                    break;

                // Handle VarDecl(s)
                case "VarDecl":
                    JVarDecl vd = aNode.getCustomNode(JVarDecl.class);
                    fieldDecl.addVarDecl(vd);
                    break;
            }
        }

        protected Class<JFieldDecl> getPartClass()  { return JFieldDecl.class; }
    }

    /**
     * MethodDecl Handler.
     */
    public static class MethodDeclHandler extends JNodeParseHandler<JMethodDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get method decl
            JMethodDecl methodDecl = getPart();

            switch (anId) {

                // Handle TypeParams
                case "TypeParams":
                    methodDecl.setTypeVars(aNode.getCustomNode(List.class));
                    break;

                // Handle ResultType
                case "ResultType":
                    methodDecl.setType(aNode.getCustomNode(JType.class));
                    break;

                // Handle MethodDeclarator Identifier
                case "Identifier":
                    methodDecl.setId(aNode.getCustomNode(JExprId.class));
                    break;

                // Handle MethodDeclarator FormalParam
                case "FormalParam":
                    methodDecl.addParam(aNode.getCustomNode(JVarDecl.class));
                    break;

                // Handle ThrowsList
                case "ThrowsList":
                    methodDecl.setThrowsList(aNode.getCustomNode(List.class));
                    break;

                // Handle Block
                case "Block":
                    methodDecl.setBlock(aNode.getCustomNode(JStmtBlock.class));
                    break;
            }
        }

        protected Class<JMethodDecl> getPartClass()  { return JMethodDecl.class; }
    }

    /**
     * ConstrDecl Handler.
     */
    public static class ConstrDeclHandler extends JNodeParseHandler<JConstrDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get constructor decl
            JConstrDecl constrDecl = getPart();

            switch (anId) {

                // Handle TypeParams
                case "TypeParams":
                    constrDecl.setTypeVars(aNode.getCustomNode(List.class));
                    break;

                // Handle Identifier
                case "Identifier":
                    constrDecl.setId(aNode.getCustomNode(JExprId.class));
                    break;

                // Handle FormalParam
                case "FormalParam":
                    constrDecl.addParam(aNode.getCustomNode(JVarDecl.class));
                    break;

                // Handle ThrowsList
                case "ThrowsList":
                    constrDecl.setThrowsList(aNode.getCustomNode(List.class));
                    break;

                // Handle BlockStatement start "{"
                case "{":
                    JStmtBlock block = new JStmtBlock();
                    block.setStartToken(aNode.getStartToken());
                    block.setEndToken(aNode.getEndToken());
                    constrDecl.setBlock(block);
                    break;

                // Handle ConstrCall
                case "ConstrCall":
                    constrDecl.getBlock().addStatement(aNode.getCustomNode(JStmtConstrCall.class));
                    break;

                // Handle BlockStatement
                case "BlockStatement":
                    constrDecl.getBlock().addStatement(aNode.getCustomNode(JStmt.class));
                    break;

                // Handle BlockStatement end
                case "}":
                    constrDecl.getBlock().setEndToken(aNode.getEndToken());
                    break;
            }
        }

        protected Class<JConstrDecl> getPartClass()  { return JConstrDecl.class; }
    }

    /**
     * ThrowsList Handler.
     */
    public static class ThrowsListHandler extends ParseHandler<ArrayList<JExpr>> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            if (anId == "Name")
                getPart().add(aNode.getCustomNode(JExpr.class));
        }

        @Override
        protected Class getPartClass()  { return ArrayList.class; }
    }

    /**
     * ConstrCall Handler.
     */
    public static class ConstrCallHandler extends JNodeParseHandler<JStmtConstrCall> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get constructor call statement
            JStmtConstrCall constrCallStmt = getPart();

            // Handle Identifier
            if (anId == "Identifier")
                constrCallStmt.addId(aNode.getCustomNode(JExprId.class));

            // Handle "this"/"super"
            else if (anId == "this" || anId == "super") {
                JExprId id = new JExprId(aNode.getString());
                id.setStartToken(aNode.getStartToken());
                id.setEndToken(aNode.getEndToken());
                constrCallStmt.addId(id);
            }

            // Handle Arguments
            else if (anId == "Arguments")
                constrCallStmt.setArgs(aNode.getCustomNode(List.class));
        }

        protected Class<JStmtConstrCall> getPartClass()  { return JStmtConstrCall.class; }
    }

    /**
     * Handler classes (from ParseUtils.printHandlerClassesForParentClass()).
     */
    private Class<? extends ParseHandler<?>>[]  _handlerClasses = new Class[] {
        AnnotationDeclHandler.class, LiteralHandler.class, LambdaExprHandler.class,
        ArrayInitHandler.class, AllocExprHandler.class, ArgumentsHandler.class,
        PrimarySuffixHandler.class, PrimaryPrefixHandler.class, PrimaryExprHandler.class,
        InstanceOfExprHandler.class, CastExprHandler.class, PreDecrementExprHandler.class,
        PreIncrementExprHandler.class, UnaryExprHandler.class, MultiplicativeExprHandler.class,
        AdditiveExprHandler.class, ShiftExprHandler.class, RelationalExprHandler.class,
        EqualityExprHandler.class, AndExprHandler.class, ExclusiveOrExprHandler.class,
        InclusiveOrExprHandler.class, ConditionalAndExprHandler.class, ConditionalOrExprHandler.class,
        ConditionalExprHandler.class, ResultTypeHandler.class, PrimitiveTypeHandler.class,
        ClassTypeHandler.class, TypeHandler.class, NameHandler.class,
        IdentifierHandler.class, ExpressionHandler.class, TryStatementHandler.class,
        SynchronizedStatementHandler.class, ThrowStatementHandler.class, ReturnStatementHandler.class,
        ContinueStatementHandler.class, BreakStatementHandler.class, ForStatementHandler.class,
        DoStatementHandler.class, WhileStatementHandler.class, IfStatementHandler.class,
        SwitchLabelHandler.class, SwitchStatementHandler.class, ExprStatementHandler.class,
        EmptyStatementHandler.class, VarDeclStmtHandler.class, VarDeclHandler.class,
        FormalParamHandler.class, BlockStatementHandler.class, BlockHandler.class,
        LabeledStatementHandler.class, AssertStatementHandler.class, ModifiersHandler.class,
        StatementHandler.class, ConstrCallHandler.class, ThrowsListHandler.class,
        ConstrDeclHandler.class, MethodDeclHandler.class, FieldDeclHandler.class,
        TypeParamsHandler.class, TypeParamHandler.class, EnumConstantHandler.class,
        EnumDeclHandler.class, InitializerHandler.class, ClassBodyDeclHandler.class,
        ClassDeclHandler.class, TypeDeclHandler.class, ImportDeclHandler.class,
        PackageDeclHandler.class, JavaFileImportsHandler.class, JavaFileHandler.class
    };

    // TeaVM needs this to exist, otherwise RuleNames.intern() != RuleName (and id == RuleName doesn't work)
    private static String[] _allRuleNames = { "JavaFile", "PackageDecl", "Annotation", "Name", "Identifier", "NormalAnnotation",
            "MemberValuePairs", "MemberValuePair", "MemberValue", "MemberValueArrayInit", "ConditionalExpr", "ConditionalOrExpr",
            "ConditionalAndExpr", "InclusiveOrExpr", "ExclusiveOrExpr", "AndExpr", "EqualityExpr", "InstanceOfExpr", "RelationalExpr",
            "ShiftExpr", "AdditiveExpr", "MultiplicativeExpr", "UnaryExpr", "PreIncrementExpr", "PrimaryExpr", "PrimaryPrefix", "Literal",
            "IntegerLiteral", "IntLiteral", "HexLiteral", "OctalLiteral", "FloatLiteral", "CharacterLiteral", "StringLiteral",
            "BooleanLiteral", "NullLiteral", "ClassType", "TypeArgs", "TypeArg", "ReferenceType", "PrimitiveType", "WildcardBounds",
            "LambdaExpr", "Expression", "AssignOp", "Block", "BlockStatement", "Modifiers", "Modifier", "Type", "VarDeclStmt",
            "VarDecl", "VarInit", "ArrayInit", "Statement", "LabeledStatement", "AssertStatement", "EmptyStatement", "ExprStatement",
            "PreDecrementExpr", "SwitchStatement", "SwitchLabel", "IfStatement", "WhileStatement", "DoStatement", "ForStatement",
            "ForInit", "ExprStmtList", "BreakStatement", "ContinueStatement", "ReturnStatement", "ThrowStatement", "SynchronizedStatement",
            "TryStatement", "FormalParam", "ClassDecl", "TypeParams", "TypeParam", "TypeBound", "ExtendsList", "ImplementsList",
            "ClassBody", "ClassBodyDecl", "Initializer", "MemberDecl", "EnumDecl", "EnumConstant", "Arguments", "ConstrDecl",
            "FormalParams", "ThrowsList", "ConstrCall", "FieldDecl", "MethodDecl", "ResultType", "AnnotationDecl", "AllocExpr",
            "ArrayDimsAndInits", "PrimarySuffix", "MemberSelector", "UnaryExprNotPlusMinus", "CastLook", "CastExpr", "PostfixExpr",
            "ShiftRightUnsigned", "ShiftRight", "SingleMemberAnnotation", "MarkerAnnotation", "ImportDecl", "TypeDecl",
            "JavaFileImports",
            "boolean", "char", "byte", "short", "int", "long", "float", "double",
            "public", "private", "protected", "static", "default", "abstract",
            "this", "super", "extends", "implements", "interface", "...",
            "+", "-", "*", "/", "++", "--", "==", "+=", "-=", "{", "[", "->",
    };

    public static String[] _allRuleNamesIntern = Stream.of(_allRuleNames).map(s -> s.intern()).toArray(size -> new String[size]);
}