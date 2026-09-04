/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import java.util.stream.Stream;
import javakit.resolver.*;
import snap.util.ArrayUtils;

/**
 * This class represents a Java class declaration.
 */
public class JClassDecl extends JMemberDecl implements WithVarDeclsX, WithTypeParameters {

    // The type of class (Class, Interface, Enum, Annotation, Record)
    protected ClassType _classType = ClassType.Class;

    // TypeVars
    private JTypeVar[] _typeVars = new JTypeVar[0];

    // The formal parameters (for records)
    protected JVarDecl[] _params;

    // The extends list
    protected JType[] _extendsTypes = JType.EMPTY_TYPES_ARRAY;

    // The implements list
    protected JType[] _implementsTypes = JType.EMPTY_TYPES_ARRAY;

    // The permitted subclasses
    protected JType[] _permittedSubclasses = JType.EMPTY_TYPES_ARRAY;

    // The list of fields, methods, enums annotations and child classes
    protected JBodyDecl[] _bodyDecls = new JBodyDecl[0];

    // The list of fields, methods, enums annotations and child classes
    protected JMemberDecl[] _memberDecls;

    // The field declarations
    protected JFieldDecl[] _fieldDecls;

    // The constructor declarations
    protected JConstrDecl[] _constrDecls;

    // The method declarations
    protected JMethodDecl[] _methodDecls;

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
        Stream.of(_typeVars).forEach(this::removeChild);
        _typeVars = theTVs;
        Stream.of(_typeVars).forEach(this::addChild);
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
        _params = varDecls;
        Stream.of(_params).forEach(this::addChild);
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
     * Returns the permitted subclasses.
     */
    public JType[] getPermittedSubclasses()  { return _permittedSubclasses; }

    /**
     * Adds a permitted subclass.
     */
    public void addPermittedSubclass(JType classType)
    {
        _permittedSubclasses = ArrayUtils.add(_permittedSubclasses, classType);
        addChild(classType);
    }

    /**
     * Returns the enum constants.
     */
    public JEnumConst[] getEnumConstants()  { return _enumConstants; }

    /**
     * Adds an enum constant.
     */
    public void addEnumConstant(JEnumConst enumConst)
    {
        _enumConstants = ArrayUtils.add(_enumConstants, enumConst);
        addChild(enumConst);
    }

    /**
     * Returns the superclass.
     */
    public JavaClass getSuperClass()
    {
        // Get extends class
        JType extendsType = getExtendsType();
        JavaClass superClass = extendsType != null ? extendsType.getEvalClass() : null;

        // If no superclass, return Object.class (but complain if it was declared but not found)
        if (superClass == null) {
            if (extendsType != null)
                System.err.println("JClassDecl: Couldn't find superclass: " + extendsType.getName());
            return getJavaClassForClass(Object.class);
        }

        return superClass;
    }

    /**
     * Returns implemented interfaces.
     */
    public JavaClass[] getInterfaces()
    {
        return ArrayUtils.mapNonNull(_implementsTypes, JType::getEvalClass, JavaClass.class);
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
    public JBodyDecl[] getBodyDecls()  { return _bodyDecls; }

    /**
     * Sets the body declarations.
     */
    public void setBodyDecls(JBodyDecl[] bodyDecls)
    {
        _bodyDecls = bodyDecls;
        Stream.of(_bodyDecls).forEach(this::addChild);
    }

    /**
     * Adds a body declaration.
     */
    public void addBodyDecl(JBodyDecl bodyDecl)
    {
        _bodyDecls = ArrayUtils.add(_bodyDecls, bodyDecl);
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
        return _fieldDecls = ArrayUtils.filterByClass(getBodyDecls(), JFieldDecl.class);
    }

    /**
     * Returns all field var decls.
     */
    public JVarDecl[] getFieldVarDecls()
    {
        JFieldDecl[] fieldDecls = getFieldDecls();
        Stream<JVarDecl> varDeclsStream = Stream.of(fieldDecls).flatMap(fieldDecl -> Stream.of(fieldDecl.getVarDecls()));
        return _varDecls = varDeclsStream.toArray(JVarDecl[]::new);
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
        return _methodDecls = ArrayUtils.filterByClass(getBodyDecls(), JMethodDecl.class);
    }

    /**
     * Returns the JMethodDecl for given name.
     */
    public JMethodDecl getMethodDeclForNameAndTypes(String aName, JavaType ... argTypes)
    {
        JMethodDecl[] methodDecls = getMethodDecls();
        return ArrayUtils.findMatch(methodDecls, methodDecl -> methodDecl.getName().equals(aName));
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
        List<JClassDecl> classDecls = new ArrayList<>();
        for (JBodyDecl bodyDecl : getBodyDecls())
            findEnclosedClassDecls(bodyDecl, classDecls);
        return classDecls.toArray(new JClassDecl[0]);
    }

    /**
     * Finds inner class declarations and anonymous class declarations in Alloc expressions.
     */
    private void findEnclosedClassDecls(JNode aNode, List<JClassDecl> classDecls)
    {
        // Handle Class decl
        if (aNode instanceof JClassDecl)
            classDecls.add((JClassDecl) aNode);

        // Otherwise recurse
        else for (JNode child : aNode.getChildren())
            findEnclosedClassDecls(child, classDecls);
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
     * Returns the JavaDecl most closely associated with given child JType node.
     */
    @Override
    protected JavaType getJavaTypeForChildType(JType childType)
    {
        // Look for JTypeVar for given type name
        String typeName = childType.getName();
        JTypeVar typeVar = getTypeParamDeclForName(typeName);
        if (typeVar != null)
            return typeVar.getTypeVariable();

        // See if this class matches name or has inner class of name
        JavaClass thisClass = getEvalClass();
        if (thisClass != null) {
            if (thisClass.getSimpleName().equals(typeName))
                return thisClass;
            JavaClass innerClass = thisClass.getDeclaredClassForName(typeName);
            if (innerClass != null)
                return innerClass;
        }

        // Do normal version
        return super.getJavaTypeForChildType(childType);
    }

    /**
     * Returns VarDecls encapsulated by class (JFieldDecl VarDecls).
     */
    @Override
    public JVarDecl[] getVarDecls()
    {
        if (_varDecls != null) return _varDecls;
        _varDecls = getFieldVarDecls();
        if (isRecord())
            _varDecls = ArrayUtils.addAll(getParameters(), _varDecls);
        return _varDecls;
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
     * Returns the part name.
     */
    public String getNodeString()
    {
        return switch (getClassType()) {
            case Interface -> "InterfaceDecl";
            case Enum -> "EnumDecl";
            case Annotation -> "AnnotationDecl";
            case Record -> "RecordDecl";
            default -> "ClassDecl";
        };
    }
}