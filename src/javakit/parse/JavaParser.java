/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import snap.parse.*;

/**
 * A parser for java files.
 */
@SuppressWarnings({"unused", "StringEquality"})
public class JavaParser extends JavaParserStmt {

    // The exception, if one was hit
    private Exception  _exception;

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
     * Override so subclasses will find grammar file.
     */
    protected ParseRule createRule()
    {
        // Create rule
        ParseRule rule = ParseUtils.loadRule(JavaParser.class, null);
        assert (rule != null);

        // Install handlers from list
        for (Class<? extends ParseHandler<?>> handlerClass : _handlerClasses)
            ParseUtils.installHandlerForClass(handlerClass, rule);

        // Return
        return rule;
    }

    /**
     * Parses for java file for given char input.
     */
    public synchronized JFile parseFile(CharSequence anInput)
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
     * Parses for a statement for given char input and char index.
     */
    public synchronized JStmt parseStatement(CharSequence charInput, int charIndex, int lineIndex)
    {
        _exception = null;
        ParseRule stmtRule = getRule("Statement");
        setInput(charInput);
        setCharIndex(charIndex);
        getTokenizer().setLineIndex(lineIndex);
        return parseCustom(stmtRule, JStmt.class);
    }

    /**
     * Parses for an expression for given char input.
     */
    public synchronized JExpr parseExpression(CharSequence charInput)
    {
        _exception = null;
        ParseRule exprRule = getRule("Expression");
        setInput(charInput);
        return parseCustom(exprRule, JExpr.class);
    }

    /**
     * Parses for file imports for given char input.
     */
    public synchronized JFile parseFileImports(CharSequence charInput)
    {
        _exception = null;
        ParseRule importsRule = getRule("JavaFileImports");
        setInput(charInput);
        return parseCustom(importsRule, JFile.class);
    }

    /**
     * Override to ignore exception.
     */
    protected void parseFailed(ParseRule aRule, ParseHandler<?> aHandler)
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
                _part.setModifiers(_mods);
                _mods = null;
            }
        }

        protected Class<JClassDecl> getPartClass()  { return JClassDecl.class; }
    }

    /**
     * ClassDecl Handler: ("class" | "interface") Identifier TypeParams? ExtendsList? ImplementsList? ClassBody
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

            switch (anId) {

                // Handle ClassBody (JavaMembers): ClassDecl, EnumDecl, ConstrDecl, FieldDecl, MethodDecl, AnnotationDecl
                case "ClassBody":
                    JBodyDecl[] bodyDecls = aNode.getCustomNode(JBodyDecl[].class);
                    classDecl.setBodyDecls(bodyDecls);
                    break;

                // Handle "class" or "interface"
                case "interface": classDecl.setClassType(JClassDecl.ClassType.Interface); break;

                // Handle Identifier
                case "Identifier": {
                    JExprId classId = aNode.getCustomNode(JExprId.class);
                    classDecl.setId(classId);
                    break;
                }

                // Handle TypeParams
                case "TypeParams": {
                    List<JTypeVar> typeVars = aNode.getCustomNode(List.class);
                    classDecl.setTypeVars(typeVars.toArray(new JTypeVar[0]));
                    break;
                }

                // Handle ExtendsList or ImplementsList mode and extendsList/implementsList
                case "extends": _extending = true; break;
                case "implements": _extending = false; break;
                case "ClassType":
                    JType type = aNode.getCustomNode(JType.class);
                    if (_extending)
                        classDecl.addExtendsType(type);
                    else classDecl.addImplementsType(type);
                    break;
            }
        }

        protected Class<JClassDecl> getPartClass()  { return JClassDecl.class; }
    }

    /**
     * ClassBodyHandler: "{" ClassBodyDecl* "}"
     */
    public static class ClassBodyHandler extends ParseHandler<JBodyDecl[]> {

        // List of BodyDecls
        private List<JBodyDecl> _bodyDecls = new ArrayList<>();

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            if (anId == "ClassBodyDecl") {
                JBodyDecl bodyDecl = aNode.getCustomNode(JBodyDecl.class);
                if (bodyDecl != null) // Can be null if parse only found ClassBodyDecl modifiers
                    _bodyDecls.add(bodyDecl);
            }
        }

        /**
         * Override to return array.
         */
        public JBodyDecl[] parsedAll()  { return _bodyDecls.toArray(new JBodyDecl[0]); }

        /**
         * Override to clear BodyDecls list.
         */
        @Override
        public void reset()
        {
            super.reset();
            _bodyDecls.clear();
        }

        @Override
        protected Class getPartClass()  { return JBodyDecl[].class; }
    }

    /**
     * ClassBodyDecl Handler: LookAhead(2) Initializer | Modifiers MemberDecl | ";"
     */
    public static class ClassBodyDeclHandler extends JNodeParseHandler<JBodyDecl> {

        // Modifiers
        private JModifiers _modifiers;

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Handle Modifiers
            if (anId == "Modifiers")
                _modifiers = aNode.getCustomNode(JModifiers.class);

            // Handle Initializer or Member
            else if (aNode.getCustomNode() instanceof JBodyDecl) {
                _part = aNode.getCustomNode(JBodyDecl.class);
                if (_part instanceof JMemberDecl)
                    ((JMemberDecl) _part).setModifiers(_modifiers);
                _modifiers = null;
            }
        }

        /**
         * Override to clear modifiers.
         */
        @Override
        public void reset()
        {
            super.reset();
            _modifiers = null;
        }

        @Override
        protected Class<JBodyDecl> getPartClass()  { return JBodyDecl.class; }
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
     * EnumDecl Handler: "enum" Identifier ImplementsList? "{" (EnumConstant (LookAhead(2) "," EnumConstant)*)? ","? (";" ClassBodyDecl*)? "}"
     */
    public static class EnumDeclHandler extends JNodeParseHandler<JClassDecl> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get enum decl
            JClassDecl enumDecl = getPart();

            switch (anId) {

                // Handle Identifier
                case "Identifier":
                    enumDecl.setId(aNode.getCustomNode(JExprId.class));
                    break;

                // Handle ImplementsList ClassType
                case "ClassType":
                    enumDecl.getImplementsTypes().add(aNode.getCustomNode(JType.class));
                    break;

                // Handle EnumConstant
                case "EnumConstant":
                    enumDecl.addEnumConstant(aNode.getCustomNode(JEnumConst.class));
                    break;

                // Handle ClassBodyDecl
                case "ClassBodyDecl":
                    JBodyDecl bodyDecl = aNode.getCustomNode(JBodyDecl.class);
                    enumDecl.addBodyDecl(bodyDecl);
                    break;
            }
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

            switch (anId) {

                // Handle Modifiers
                case "Modifiers":
                    enumConst.setModifiers(aNode.getCustomNode(JModifiers.class));
                    break;

                // Handle name Identifier
                case "Identifier":
                    enumConst.setId(aNode.getCustomNode(JExprId.class));
                    break;

                // Handle Arguments
                case "Arguments":
                    enumConst.setArgs(aNode.getCustomNode(List.class));
                    break;

                // Handle ClassBody
                case "ClassBody":
                    JBodyDecl[] bodyDecls = aNode.getCustomNode(JBodyDecl[].class);
                    enumConst.setClassBody(bodyDecls);
                    break;
            }
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
        protected Class getPartClass()  { return ArrayList.class; }
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
                case "TypeParams": {
                    List<JTypeVar> typeVars = aNode.getCustomNode(List.class);
                    methodDecl.setTypeVars(typeVars.toArray(new JTypeVar[0]));
                    break;
                }

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
                case "TypeParams": {
                    List<JTypeVar> typeVars = aNode.getCustomNode(List.class);
                    constrDecl.setTypeVars(typeVars.toArray(new JTypeVar[0]));
                    break;
                }

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
     * ConstrCall Handler: (Identifier ".")* (LookAhead(2) "this" ".")? TypeArgs? ("this" | "super") Arguments ";"
     */
    public static class ConstrCallHandler extends JNodeParseHandler<JStmtConstrCall> {

        /**
         * ParseHandler method.
         */
        protected void parsedOne(ParseNode aNode, String anId)
        {
            // Get constructor call statement
            JStmtConstrCall constrCallStmt = getPart();

            switch (anId) {

                // Handle Identifier
                case "Identifier":
                    constrCallStmt.addId(aNode.getCustomNode(JExprId.class));
                    break;

                // Handle "this", "super"
                case "this":
                case "super":
                    JExprId id = new JExprId(aNode);
                    constrCallStmt.addId(id);
                    break;

                // Handle Arguments
                case "Arguments":
                    constrCallStmt.setArgs(aNode.getCustomNode(List.class));
                    break;
            }
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
        InstanceOfExprHandler.class, CastExprHandler.class,
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
        EmptyStatementHandler.class, VarDeclExprHandler.class, VarDeclHandler.class,
        FormalParamHandler.class, BlockStatementHandler.class, BlockHandler.class,
        LabeledStatementHandler.class, AssertStatementHandler.class, ModifiersHandler.class,
        StatementHandler.class, ConstrCallHandler.class, ThrowsListHandler.class,
        ConstrDeclHandler.class, MethodDeclHandler.class, FieldDeclHandler.class,
        TypeParamsHandler.class, TypeParamHandler.class, EnumConstantHandler.class,
        EnumDeclHandler.class, InitializerHandler.class, ClassBodyHandler.class, ClassBodyDeclHandler.class,
        ClassDeclHandler.class, TypeDeclHandler.class, ImportDeclHandler.class,
        PackageDeclHandler.class, JavaFileImportsHandler.class, JavaFileHandler.class
    };
}