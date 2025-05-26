/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;
import javakit.resolver.*;
import snap.parse.ParseToken;
import snap.util.ArrayUtils;
import snap.util.ObjectArray;

/**
 * This class represents a Java class declaration.
 */
public class JClassDecl extends JMemberDecl implements WithVarDeclsX, WithTypeParameters {

    // The type of class (Class, Interface, Enum, Annotation, Record)
    protected ClassType  _classType = ClassType.Class;

    // TypeVars
    private JTypeVar[] _typeVars = new JTypeVar[0];

    // The formal parameters (for records)
    protected JVarDecl[] _params;

    // The formal parameters as fields (for records)
    protected JFieldDecl[] _paramFieldDecls;

    // The formal parameters as methods (for records)
    protected JMethodDecl[] _paramMethodDecls;

    // The extends list
    protected JType[] _extendsTypes = JType.EMPTY_TYPES_ARRAY;

    // The implements list
    protected JType[] _implementsTypes = JType.EMPTY_TYPES_ARRAY;

    // The list of fields, methods, enums annotations and child classes
    protected ObjectArray<JBodyDecl> _bodyDecls = new ObjectArray<>(JBodyDecl.class);

    // The list of fields, methods, enums annotations and child classes
    protected JMemberDecl[] _memberDecls;

    // The field declarations
    protected JFieldDecl[]  _fieldDecls;

    // The constructor declarations
    protected JConstrDecl[]  _constrDecls;

    // The method declarations
    protected JMethodDecl[]  _methodDecls;

    // The initializer declarations
    protected JInitializerDecl[] _initializerDecls;

    // An array of class declarations that are members of this class
    protected JClassDecl[]  _classDecls;

    // The enum constants (if ClassType Enum)
    protected JEnumConst[] _enumConstants = new JEnumConst[0];

    // The Java class
    private JavaClass _javaClass;

    // An array of VarDecls held by JFieldDecls
    private JVarDecl[] _varDecls;

    // The class type
    public enum ClassType { Class, Interface, Enum, Annotation, Record }

    /**
     * Constructor.
     */
    public JClassDecl()
    {
        super();
    }

    /**
     * Returns the full class name.
     */
    public String getClassName()
    {
        // If enclosing class declaration, return ThatClassName$ThisName, otherwise return JFile.Name
        String className = getName();
        if (className == null)
            return null;

        // If enclosing class, get name from it
        JClassDecl enclosingClassDecl = getEnclosingClassDecl();
        if (enclosingClassDecl != null) {
            String enclosingClassName = enclosingClassDecl.getEvalClassName();
            if (enclosingClassName != null)
                className = enclosingClassName + '$' + className;
        }

        // Otherwise get full name from file
        else {
            JFile jfile = getFile();
            String packageName = jfile.getPackageName();
            if (packageName != null && !packageName.isEmpty())
                className = packageName + '.' + className;
        }

        // Return
        return className;
    }

    /**
     * Returns the simple name.
     */
    public String getSimpleName()
    {
        if (isAnonymousClass())
            return "";
        return getName();
    }

    /**
     * Returns the JTypeVar(s).
     */
    public JTypeVar[] getTypeParamDecls()  { return _typeVars; }

    /**
     * Sets the JTypeVar(s).
     */
    public void setTypeVars(JTypeVar[] theTVs)
    {
        // Remove old type vars
        Stream.of(_typeVars).forEach(tvar -> removeChild(tvar));

        // Set new
        _typeVars = theTVs;

        // Add new type vars
        Stream.of(_typeVars).forEach(tvar -> addChild(tvar));
    }

    /**
     * Returns the list of formal parameters.
     */
    public JVarDecl[] getParameters()  { return _params; }

    /**
     * Returns the list of formal parameters.
     */
    public void setParameters(JVarDecl[] varDecls)
    {
        if (_paramFieldDecls != null)
            Stream.of(_paramFieldDecls).forEach(this::removeChild);
        if (_paramMethodDecls != null)
            Stream.of(_paramMethodDecls).forEach(this::removeChild);

        _params = varDecls;

        _paramFieldDecls = ArrayUtils.mapNonNull(_params, this::createFieldDeclForParam, JFieldDecl.class);
        Stream.of(_paramFieldDecls).forEach(this::addChild);

        _paramMethodDecls = ArrayUtils.mapNonNull(_params, this::createMethodDeclForParam, JMethodDecl.class);
        Stream.of(_paramMethodDecls).forEach(this::addChild);
    }

    /**
     * Creates a field decl for a record param.
     */
    private JFieldDecl createFieldDeclForParam(JVarDecl varDecl)
    {
        // Create field modifiers
        int startCharIndex = varDecl.getStartCharIndex();
        ParseToken emptyToken = new ParseToken.Builder().name("").pattern("").text("")
                .startCharIndex(startCharIndex).endCharIndex(startCharIndex).build();
        JModifiers modifiers = new JModifiers(Modifier.PRIVATE);
        modifiers.setStartToken(emptyToken);
        modifiers.setEndToken(emptyToken);

        JFieldDecl fieldDecl = new JFieldDecl();
        fieldDecl.setModifiers(modifiers);
        fieldDecl.addVarDecl(varDecl);
        return fieldDecl;
    }

    /**
     * Creates a method decl for a record param.
     */
    private JMethodDecl createMethodDeclForParam(JVarDecl varDecl)
    {
        JExprId varDeclId = varDecl.getId();
        if (varDeclId == null)
            return null;

        // Create method modifiers
        int startCharIndex = varDecl.getStartCharIndex();
        ParseToken emptyToken = new ParseToken.Builder().name("").pattern("").text("")
                .startCharIndex(startCharIndex).endCharIndex(startCharIndex).build();
        JModifiers modifiers = new JModifiers(Modifier.PUBLIC);
        modifiers.setStartToken(emptyToken);
        modifiers.setEndToken(emptyToken);

        JMethodDecl methodDecl = new JMethodDecl();
        methodDecl.setModifiers(modifiers);
        methodDecl.setReturnType(varDecl.getType());
        varDecl.getType().setParent(varDecl);
        methodDecl.setId(varDeclId);
        varDeclId.setParent(varDecl);
        methodDecl.setStartToken(emptyToken);
        methodDecl.setEndToken(emptyToken);
        return methodDecl;
    }

    /**
     * Returns the extends type.
     */
    public JType getExtendsType()  { return _extendsTypes.length > 0 ? _extendsTypes[0] : null; }

    /**
     * Returns the extends types.
     */
    public JType[] getExtendsTypes()  { return _extendsTypes; }

    /**
     * Adds an extends type.
     */
    public void addExtendsType(JType aType)
    {
        _extendsTypes = ArrayUtils.add(_extendsTypes, aType);
        addChild(aType);
    }

    /**
     * Returns the implements types.
     */
    public JType[] getImplementsTypes()  { return _implementsTypes; }

    /**
     * Adds an implements type.
     */
    public void addImplementsType(JType aType)
    {
        _implementsTypes = ArrayUtils.add(_implementsTypes, aType);
        addChild(aType);
    }

    /**
     * Returns the enum constants.
     */
    public JEnumConst[] getEnumConstants()  { return _enumConstants; }

    /**
     * Adds an enum constant.
     */
    public void addEnumConstant(JEnumConst anEC)
    {
        _enumConstants = ArrayUtils.add(_enumConstants, anEC);
        addChild(anEC);
    }

    /**
     * Returns the superclass.
     */
    public JavaClass getSuperClass()
    {
        // Get extends class
        JType extendsType = getExtendsType();
        JavaClass extendsClass = extendsType != null ? extendsType.getEvalClass() : null;

        // If no ExtendsClass return Object.class (but complain if it was declared but not found)
        if (extendsClass == null) {
            if (extendsType != null)
                System.err.println("JClassDecl: Couldn't find superclass: " + extendsType.getName());
            return getJavaClassForClass(Object.class);
        }

        // Return
        return extendsClass;
    }

    /**
     * Returns implemented interfaces.
     */
    public JavaClass[] getInterfaces()
    {
        return ArrayUtils.mapNonNull(_implementsTypes, interfType -> interfType.getEvalClass(), JavaClass.class);
    }

    /**
     * Returns the class type.
     */
    public ClassType getClassType()  { return _classType; }

    /**
     * Sets the class type.
     */
    public void setClassType(ClassType aType)  { _classType = aType; }

    /**
     * Returns whether class type is Class.
     */
    public boolean isClass()  { return _classType == ClassType.Class; }

    /**
     * Returns whether class type is Interface.
     */
    public boolean isInterface()  { return _classType == ClassType.Interface; }

    /**
     * Returns whether class type is Enum.
     */
    public boolean isEnum()  { return _classType == ClassType.Enum; }

    /**
     * Returns whether class type is Record.
     */
    public boolean isRecord()  { return _classType == ClassType.Record; }

    /**
     * Returns whether class is anonymous class.
     */
    public boolean isAnonymousClass()
    {
        return getId() == null;
    }

    /**
     * Returns the list of body declarations.
     */
    public JBodyDecl[] getBodyDecls()  { return _bodyDecls.getArray(); }

    /**
     * Sets the body declarations.
     */
    public void setBodyDecls(JBodyDecl[] bodyDecls)
    {
        for (JBodyDecl bodyDecl : bodyDecls)
            addBodyDecl(bodyDecl);
    }

    /**
     * Adds a body declaration.
     */
    public void addBodyDecl(JBodyDecl bodyDecl)
    {
        _bodyDecls.add(bodyDecl);
        addChild(bodyDecl);
    }

    /**
     * Returns the list of member declarations.
     */
    public JMemberDecl[] getMemberDecls()
    {
        if (_memberDecls != null) return _memberDecls;
        return _memberDecls = ArrayUtils.filterByClass(getBodyDecls(), JMemberDecl.class);
    }

    /**
     * Returns the class field declarations.
     */
    public JFieldDecl[] getFieldDecls()
    {
        if (_fieldDecls != null) return _fieldDecls;

        JFieldDecl[] fieldDecls = ArrayUtils.filterByClass(getBodyDecls(), JFieldDecl.class);

        // If Record, prepend record params as fields
        if (isRecord() && _paramFieldDecls != null)
            fieldDecls = ArrayUtils.addAll(_paramFieldDecls, fieldDecls);

        return _fieldDecls = fieldDecls;
    }

    /**
     * Returns the class constructor declarations.
     */
    public JConstrDecl[] getConstructorDecls()
    {
        if (_constrDecls != null) return _constrDecls;
        return _constrDecls = ArrayUtils.filterByClass(getBodyDecls(), JConstrDecl.class);
    }

    /**
     * Returns the JMethodDecl for given name.
     */
    public JConstrDecl getConstructorDeclForTypes(JavaType[] argTypes)
    {
        // Get compatible constructor
        JavaClass javaClass = getJavaClass();
        JavaConstructor constructor = JavaClassUtils.getCompatibleConstructor(javaClass, new JavaClass[0]);

        // Get constructor decls and return the one that has matching constructor
        JConstrDecl[] constrDecls = getConstructorDecls();
        return ArrayUtils.findMatch(constrDecls, constrDecl -> constrDecl.getDecl() == constructor);
    }

    /**
     * Returns the class method declarations.
     */
    public JMethodDecl[] getMethodDecls()
    {
        if (_methodDecls != null) return _methodDecls;

        JMethodDecl[] methodDecls = ArrayUtils.filterByClass(getBodyDecls(), JMethodDecl.class);

        if (isRecord() && _paramMethodDecls != null)
            methodDecls = ArrayUtils.addAll(_paramMethodDecls, methodDecls);

        return _methodDecls = methodDecls;
    }

    /**
     * Returns the JMethodDecl for given name.
     */
    public JMethodDecl getMethodDeclForNameAndTypes(String aName, JavaType[] argTypes)
    {
        // Get methods
        JMethodDecl[] methodDecls = getMethodDecls();

        // Iterate over each and return if match
        for (JMethodDecl methodDecl : methodDecls)
            if (methodDecl.getName().equals(aName))
                return methodDecl;

        // Return not found
        return null;
    }

    /**
     * Returns the class initializer declarations.
     */
    public JInitializerDecl[] getInitializerDecls()
    {
        if (_initializerDecls != null) return _initializerDecls;
        return _initializerDecls = ArrayUtils.filterByClass(getBodyDecls(), JInitializerDecl.class);
    }

    /**
     * Returns inner class declarations.
     */
    public JClassDecl[] getDeclaredClassDecls()
    {
        if (_classDecls != null) return _classDecls;
        return _classDecls = ArrayUtils.filterByClass(getBodyDecls(), JClassDecl.class);
    }

    /**
     * Returns inner class declarations and anonymous class declarations in Alloc expressions.
     */
    public JClassDecl[] getEnclosedClassDecls()
    {
        // Get class decls from body decls
        List<JClassDecl> cds = new ArrayList<>();
        for (JBodyDecl bodyDecl : getBodyDecls())
            findEnclosedClassDecls(bodyDecl, cds);

        // Return array
        return cds.toArray(new JClassDecl[0]);
    }

    /**
     * Finds inner class declarations and anonymous class declarations in Alloc expressions.
     */
    private void findEnclosedClassDecls(JNode aNode, List<JClassDecl> theCDs)
    {
        // Handle Class decl
        if (aNode instanceof JClassDecl)
            theCDs.add((JClassDecl) aNode);

        // Otherwise recurse
        else for (JNode c : aNode.getChildren())
            findEnclosedClassDecls(c, theCDs);
    }

    /**
     * Returns the Java class.
     */
    public JavaClass getJavaClass()
    {
        if (_javaClass != null) return _javaClass;
        String className = getClassName();
        JavaClass javaClass = className != null ? getJavaClassForName(className) : null;
        if (javaClass == null && getResolver() != null)
            javaClass = new JavaClass(getResolver(), this);
        return _javaClass = javaClass;
    }

    /**
     * Returns the simple name.
     */
    @Override
    protected String getNameImpl()
    {
        // Get anonymous class name (number really)
        if (isAnonymousClass())
            return getAnonymousClassName();

        // Return not found
        System.err.println("JClassDecl.getNameImpl: Name not found");
        return null;
    }

    /**
     * Returns the simple name.
     */
    private String getAnonymousClassName()
    {
        // Get enclosingClass and inner class decls
        JClassDecl enclosingClassDecl = getEnclosingClassDecl();
        JClassDecl[] classDecls = enclosingClassDecl != null ? enclosingClassDecl.getEnclosedClassDecls() : new JClassDecl[0];
        int anonymousIndex = 1;

        // Iterate over inner class decls and return anonymousIndex when this class decl found
        for (JClassDecl classDecl : classDecls) {
            if (classDecl == this)
                return Integer.toString(anonymousIndex);
            if (classDecl.isAnonymousClass())
                anonymousIndex++;
        }

        // Return not found
        System.err.println("JClassDecl.getAnonymousClassName: Anonymous inner class not found");
        return null;
    }

    /**
     * Override to return Java class.
     */
    @Override
    protected JavaClass getDeclImpl()  { return getJavaClass(); }

    /**
     * Override to check field declarations for id.
     */
    @Override
    protected JavaDecl getDeclForChildId(JExprId anExprId)
    {
        // If it's "this", set class and return ClassField
        String name = anExprId.getName();
        if (name.equals("this"))
            return getDecl();

        // If it's "super", set class and return ClassField
        if (name.equals("super"))
            return getSuperClass();

        // Iterate over enum constants
        if (isEnum()) {
            JEnumConst[] enumConstants = getEnumConstants();
            for (JEnumConst enumConst : enumConstants) {
                if (name.equals(enumConst.getName()))
                    return enumConst.getDecl();
            }
        }

        // See if it's a field reference from superclass
        JavaClass superClass = getSuperClass();
        if (superClass != null) {
            JavaField field = superClass.getFieldForName(name);
            if (field != null)
                return field;
        }

        // Check interfaces (id could be interface static field)
        JType[] implementsTypes = getImplementsTypes();
        for (JType implementsType : implementsTypes) {
            JavaClass interf = implementsType.getEvalClass();
            JavaField field2 = interf != null ? interf.getDeclaredFieldForName(name) : null;
            if (field2 != null)
                return field2;
        }

        // Look for InnerClass of given name
        JavaClass evalClass = getEvalClass();
        if (evalClass != null) {
            JavaClass innerClass = evalClass.getDeclaredClassForName(name);
            if (innerClass != null)
                return innerClass;
        }

        // Do normal version
        return super.getDeclForChildId(anExprId);
    }

    /**
     * Returns the JavaDecl most closely associated with given child JType node.
     */
    @Override
    protected JavaType getJavaTypeForChildType(JType type)
    {
        // Look for JTypeVar for given type name
        String typeName = type.getName();
        JTypeVar typeVar = getTypeParamDeclForName(typeName);
        if (typeVar != null)
            return typeVar.getTypeVariable();

        // Look for InnerClass of given name
        JavaClass evalClass = getEvalClass();
        if (evalClass != null) {
            JavaClass innerClass = evalClass.getDeclaredClassForName(typeName);
            if (innerClass != null)
                return innerClass;
        }

        // Do normal version
        return super.getJavaTypeForChildType(type);
    }

    /**
     * Returns VarDecls encapsulated by class (JFieldDecl VarDecls).
     */
    @Override
    public JVarDecl[] getVarDecls()
    {
        // If already set, just return
        if (_varDecls != null) return _varDecls;

        // Get FieldDecls.VarDecls
        JFieldDecl[] fieldDecls = getFieldDecls();
        Stream<JVarDecl> varDeclsStream = Stream.of(fieldDecls).flatMap(fieldDecl -> Stream.of(fieldDecl.getVarDecls()));
        JVarDecl[] varDecls = varDeclsStream.toArray(size -> new JVarDecl[size]);

        // Set and return
        return _varDecls = varDecls;
    }

    /**
     * Override to provide hack to look for VarDecls in previous initializers.
     */
    @Override
    public JVarDecl getVarDeclForId(JExprId anId)
    {
        // Do normal version
        JVarDecl varDecl = WithVarDeclsX.super.getVarDeclForId(anId);
        if (varDecl != null)
            return varDecl;

        // If this class is inner class, just return not found
        if (getParent(JClassDecl.class) != null)
            return null;

        // Try ReplHack for top level class
        return getVarDeclForIdReplHack(anId);
    }

    /**
     * REPL hack - Get/search initializers before this method for unresolved ids.
     */
    protected JVarDecl getVarDeclForIdReplHack(JExprId idExpr)
    {
        JInitializerDecl[] initializerDecls = getInitializerDecls();

        // Iterate over initializers to find matching var decl (break at one holding given id)
        for (JInitializerDecl initializerDecl : initializerDecls) {

            // If id expression is before initializer start, just break
            if (idExpr.getStartCharIndex() < initializerDecl.getEndCharIndex())
                break;

            // If initializer block has var decl for id, return it
            JStmtBlock blockStmt = initializerDecl.getBlock();
            JVarDecl varDecl = blockStmt.getVarDeclForId(idExpr);
            if (varDecl != null)
                return varDecl;
        }

        // Return not found
        return null;
    }

    /**
     * Returns a resolved type for given type.
     */
    @Override
    protected JavaType getResolvedTypeForTypeVar(JavaTypeVariable aTypeVar)
    {
        // If this class is subclass of parameterized type with given type var, return resolved type
        JavaClass javaClass = getJavaClass();
        if (javaClass != null) {
            JavaType resolvedType = javaClass.getResolvedTypeForTypeVariable(aTypeVar);
            if (resolvedType != null)
                return resolvedType;
        }

        // Do normal version
        return super.getResolvedTypeForTypeVar(aTypeVar);
    }

    /**
     * Override to return errors for ExtendsList, ImplementsList, BodyDecls, TypeVars.
     */
    @Override
    protected NodeError[] getErrorsImpl()
    {
        NodeError[] errors = NodeError.NO_ERRORS;

        // Get errors for extends list
        JType[] extendsTypes = getExtendsTypes();
        errors = NodeError.addNodeErrorsForNodes(errors, extendsTypes);

        // Get errors for implements list
        JType[] implementsTypes = getImplementsTypes();
        errors = NodeError.addNodeErrorsForNodes(errors, implementsTypes);

        // Get errors for BodyDecls
        JBodyDecl[] bodyDecls = getBodyDecls();
        errors = NodeError.addNodeErrorsForNodes(errors, bodyDecls);

        // Get errors for type vars
        JTypeVar[] typeVars = getTypeParamDecls();
        errors = NodeError.addNodeErrorsForNodes(errors, typeVars);

        // Get errors for EnumConstants
        JEnumConst[] enumConst = getEnumConstants();
        errors = NodeError.addNodeErrorsForNodes(errors, enumConst);

        // Return
        return errors;
    }

    /**
     * Returns the part name.
     */
    public String getNodeString()
    {
        switch (getClassType()) {
            case Interface: return "InterfaceDecl";
            case Enum: return "EnumDecl";
            case Annotation: return "AnnotationDecl";
            case Record: return "RecordDecl";
            default: return "ClassDecl";
        }
    }
}