/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import java.util.stream.Stream;
import javakit.resolver.*;

/**
 * A Java member for ClassDecl.
 */
public class JClassDecl extends JMemberDecl {

    // The type of class (Class, Interface, Enum, Annotation)
    protected ClassType  _classType = ClassType.Class;

    // TypeVars
    protected List<JTypeVar>  _typeVars;

    // The extends list
    protected List<JType>  _extendsTypes = new ArrayList<>();

    // The implements list
    protected List<JType>  _implementsTypes = new ArrayList<>();

    // The list of fields, methods, enums annotations and child classes
    protected List<JMemberDecl>  _members = new ArrayList<>();

    // The enum constants (if ClassType Enum)
    protected List<JEnumConst>  _enumConstants = new ArrayList<>();

    // The field declarations
    protected JFieldDecl[]  _fieldDecls;

    // The constructor declarations
    protected JConstrDecl[]  _constrDecls;

    // The method declarations
    protected JMethodDecl[]  _methodDecls;

    // The initializer declarations
    protected JInitializerDecl[]  _initDecls;

    // An array of class declarations that are members of this class
    protected JClassDecl[]  _classDecls;

    // The class type
    public enum ClassType { Class, Interface, Enum, Annotation }

    /**
     * Constructor.
     */
    public JClassDecl()
    {
        super();
    }

    /**
     * Returns the simple name.
     */
    public String getSimpleName()
    {
        return getName();
    }

    /**
     * Returns the JTypeVar(s).
     */
    public List<JTypeVar> getTypeVars()  { return _typeVars; }

    /**
     * Sets the JTypeVar(s).
     */
    public void setTypeVars(List<JTypeVar> theTVs)
    {
        if (_typeVars != null) for (JNode n : _typeVars) removeChild(n);
        _typeVars = theTVs;
        if (_typeVars != null) for (JNode n : _typeVars) addChild(n, -1);
    }

    /**
     * Returns the extends list.
     */
    public List<JType> getExtendsTypes()
    {
        return _extendsTypes;
    }

    /**
     * Returns the extends list.
     */
    public void addExtendsType(JType aType)
    {
        _extendsTypes.add(aType);
        addChild(aType, -1);
    }

    /**
     * Returns the implements list.
     */
    public List<JType> getImplementsTypes()  { return _implementsTypes; }

    /**
     * Returns the implements list.
     */
    public void addImplementsType(JType aType)
    {
        _implementsTypes.add(aType);
        addChild(aType, -1);
    }

    /**
     * Returns the list of enum constants.
     */
    public List<JEnumConst> getEnumConstants()  { return _enumConstants; }

    /**
     * Adds an enum constant.
     */
    public void addEnumConstant(JEnumConst anEC)
    {
        _enumConstants.add(anEC);
        addChild(anEC, -1);
    }

    /**
     * Returns the superclass.
     */
    public JavaClass getSuperClass()
    {
        // Get extends class
        List<JType> extendsTypes = _extendsTypes;
        JType extendsType = extendsTypes.size() > 0 ? extendsTypes.get(0) : null;
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
        List<JavaClass> interfaceClasses = new ArrayList<>();
        for (JType interfType : _implementsTypes) {
            JavaClass interfaceClass = interfType.getEvalClass();
            if (interfaceClass != null)
                interfaceClasses.add(interfaceClass);
        }

        // Return array
        return interfaceClasses.toArray(new JavaClass[0]);
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
     * Returns whether class is anonymous class.
     */
    public boolean isAnonymousClass()
    {
        return getId() == null;
    }

    /**
     * Returns the list of member declarations.
     */
    public List<JMemberDecl> getMemberDecls()
    {
        return _members;
    }

    /**
     * Adds a member declaration.
     */
    public void addMemberDecl(JMemberDecl aDecl)
    {
        _members.add(aDecl);
        addChild(aDecl, -1);
    }

    /**
     * Returns the class field declarations.
     */
    public JFieldDecl[] getFieldDecls()
    {
        // If already set, just return
        if (_fieldDecls != null) return _fieldDecls;

        // Get fields from members
        Stream<JMemberDecl> membersStream = _members.stream();
        Stream<JMemberDecl> fieldsStream = membersStream.filter(m -> m instanceof JFieldDecl);
        JFieldDecl[] fields = fieldsStream.toArray(size -> new JFieldDecl[size]);

        // Set/return
        return _fieldDecls = fields;
    }

    /**
     * Returns the class constructor declarations.
     */
    public JConstrDecl[] getConstructorDecls()
    {
        // If already set, just return
        if (_constrDecls != null) return _constrDecls;

        // Get constructors from members
        Stream<JMemberDecl> membersStream = _members.stream();
        Stream<JMemberDecl> constructorsStream = membersStream.filter(m -> m instanceof JConstrDecl);
        JConstrDecl[] constructors = constructorsStream.toArray(size -> new JConstrDecl[size]);

        // Set/return
        return _constrDecls = constructors;
    }

    /**
     * Returns the JConstructorDecl for given name.
     */
    public JConstrDecl getConstructorDecl(String aName, Class<?>[] theClasses)
    {
        for (JConstrDecl cd : getConstructorDecls())
            if (cd.getName().equals(aName))
                return cd;
        return null;
    }

    /**
     * Returns the class method declarations.
     */
    public JMethodDecl[] getMethodDecls()
    {
        // If already set, just return
        if (_methodDecls != null) return _methodDecls;

        // Get constructors from members
        Stream<JMemberDecl> membersStream = _members.stream();
        Stream<JMemberDecl> methodsStream = membersStream.filter(m -> m instanceof JMethodDecl);
        JMethodDecl[] methods = methodsStream.toArray(size -> new JMethodDecl[size]);

        // Set/return
        return _methodDecls = methods;
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
    public JInitializerDecl[] getInitDecls()
    {
        // If already set, just return
        if (_initDecls != null) return _initDecls;

        // Get constructors from members
        Stream<JMemberDecl> membersStream = _members.stream();
        Stream<JMemberDecl> methodsStream = membersStream.filter(m -> m instanceof JInitializerDecl);
        JInitializerDecl[] initDecls = methodsStream.toArray(size -> new JInitializerDecl[size]);

        // Set/return
        return _initDecls = initDecls;
    }

    /**
     * Returns the class constructor declarations.
     */
    public JClassDecl[] getClassDecls()
    {
        // If already set, just return
        if (_classDecls != null) return _classDecls;

        // Get class decls from members
        List<JClassDecl> cds = new ArrayList<>();
        for (JMemberDecl mbr : _members)
            getClassDecls(mbr, cds);

        // Set, return
        return _classDecls = cds.toArray(new JClassDecl[0]); // Return class declarations
    }

    /**
     * Returns the class constructor declarations.
     */
    private void getClassDecls(JNode aNode, List<JClassDecl> theCDs)
    {
        // Handle Class decl
        if (aNode instanceof JClassDecl)
            theCDs.add((JClassDecl) aNode);

        // Otherwise recurse
        else for (JNode c : aNode.getChildren())
            getClassDecls(c, theCDs);
    }

    /**
     * Returns the class declaration for class name.
     */
    public JClassDecl getClassDecl(String aName)
    {
        int index = aName.indexOf('.');
        String name = index > 0 ? aName.substring(0, index) : aName;
        String remainder = index >= 0 ? aName.substring(index + 1) : null;

        // Iterate over ClassDecls
        for (JClassDecl classDecl : getClassDecls())
            if (classDecl.getSimpleName().equals(name))
                return remainder != null ? classDecl.getClassDecl(remainder) : classDecl;

        // Return not found
        return null;
    }

    /**
     * Returns the simple name.
     */
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
    protected String getAnonymousClassName()
    {
        // Get enclosingClass and inner class decls
        JClassDecl enclosingClassDecl = getEnclosingClassDecl();
        JClassDecl[] classDecls = enclosingClassDecl != null ? enclosingClassDecl.getClassDecls() : new JClassDecl[0];
        int anonymousIndex = 0;

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
     * Returns the class declaration.
     */
    public JavaClass getDecl()  { return (JavaClass) super.getDecl(); }

    /**
     * Returns the class declaration.
     */
    protected JavaClass getDeclImpl()
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
            if (packageName != null && packageName.length() > 0)
                className = packageName + '.' + className;
        }

        // Get class for name - if not found, use SuperClass (assume this class not compiled)
        JavaClass javaClass = getJavaClassForName(className);
        if (javaClass == null) {
            //javaClass = getSuperClass();
            Resolver resolver = getResolver();
            javaClass = new JavaClass(resolver, this, className);
        }

        // Otherwise see if we need to update
        else if (javaClass.getUpdater() instanceof JavaClassUpdaterDecl) {
            JavaClassUpdaterDecl updater = (JavaClassUpdaterDecl) javaClass.getUpdater();
            updater.setClassDecl(this);
        }

        // Return
        return javaClass;
    }

    /**
     * Override to check field declarations for id.
     */
    @Override
    protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
    {
        // If class id, return class declaration
        if (anExprId == _id)
            return getDecl();

        // If it's "this", set class and return ClassField
        String name = anExprId.getName();
        if (name.equals("this"))
            return getDecl();

        // If it's "super", set class and return ClassField
        if (name.equals("super"))
            return getSuperClass();

        // Iterate over fields and return declaration if found
        JFieldDecl[] fieldDecls = getFieldDecls();
        for (JFieldDecl fieldDecl : fieldDecls) {
            List<JVarDecl> fieldVarDecls = fieldDecl.getVarDecls();
            for (JVarDecl fieldVarDecl : fieldVarDecls)
                if (Objects.equals(fieldVarDecl.getName(), name))
                    return fieldVarDecl.getDecl();
        }

        // Iterate over enum constants
        if (isEnum()) {
            List<JEnumConst> enumConstants = getEnumConstants();
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

        // Check interfaces:  Not sure what's going on here
        List<JType> implementsTypes = getImplementsTypes();
        for (JType implementsType : implementsTypes) {
            JavaClass interf = implementsType.getEvalClass();
            JavaField field2 = interf != null ? interf.getFieldForName(name) : null;
            if (field2 != null)
                return field2;
        }

        // Look for JTypeVar for given name
        JTypeVar typeVar = getTypeVar(name);
        if (typeVar != null)
            return typeVar.getDecl();

        // Look for InnerClass of given name
        JavaClass evalClass = getEvalClass();
        if (evalClass != null) {
            JavaClass innerClass = evalClass.getInnerClassForName(name);
            if (innerClass != null)
                return innerClass;
        }

        // Do normal version
        return super.getDeclForChildExprIdNode(anExprId);
    }

    /**
     * Returns the JavaDecl most closely associated with given child JType node.
     */
    @Override
    protected JavaDecl getDeclForChildTypeNode(JType type)
    {
        // Get parent of nested type
        JType parentType = type;
        while (parentType.getParent() instanceof JType)
            parentType = (JType) parentType.getParent();

        // If parent of nested type is this JClassDecl, either check for TypeVar or forward to file
        if (parentType.getParent() == this) {

            // Check for TypeVar
            if (type.getParent() instanceof JType) {
                JType par = (JType) type.getParent();
                JavaType baseType = par.getBaseDecl();
                if (baseType instanceof JavaClass) {
                    JavaClass baseClass = (JavaClass) baseType;
                    String typeName = type.getName();
                    JavaDecl typeVarType = baseClass.getTypeVarForName(typeName);
                    if (typeVarType != null)
                        return typeVarType;
                }
            }

            // Forward to file
            return super.getDeclForChildTypeNode(type);
        }

        // If it's "this", set class and return ClassField - from old getDeclForChildNode() is this really needed ???
        String name = type.getName();
        if (name.equals("this"))
            return getDecl();

        // If it's "super", set class and return ClassField - from old getDeclForChildNode() is this really needed ???
        if (name.equals("super"))
            return getSuperClass();

        // See if it's a field reference from superclass - from old getDeclForChildNode() is this really needed ???
        JavaClass superClass = getSuperClass();
        if (superClass != null) {
            JavaField field = superClass.getFieldForName(name);
            if (field != null)
                return field;
        }

        // Look for JTypeVar for given name - from old getDeclForChildNode() is this really needed ???
        JTypeVar typeVar = getTypeVar(name);
        if (typeVar != null)
            return typeVar.getDecl();

        // Look for InnerClass of given name - from old getDeclForChildNode() is this really needed ???
        JavaClass evalClass = getEvalClass();
        if (evalClass != null) {
            JavaClass innerClass = evalClass.getInnerClassForName(name);
            if (innerClass != null)
                return innerClass;
        }

        // Do normal version
        return super.getDeclForChildTypeNode(type);
    }

    /**
     * Override to resolve Decl.EvalType from ParentExpr.EvalType.
     */
    protected JavaType getEvalTypeImpl(JNode aNode)
    {
        // Handle JType: See if class decl can resolve
        if (aNode instanceof JType) {

            // Get type
            JType type = (JType) aNode;
            JavaType typeType = type.getDecl();
            JavaType evalType = typeType.getEvalType();

            // If eval type is TypeVar, see if it corresponds to this class
            if (evalType instanceof JavaTypeVariable) {
                JavaClass javaClass = getDecl();
                JavaType evalType2 = javaClass.getResolvedType(evalType);
                if (evalType2 != evalType.getEvalType())
                    return evalType2;
            }
        }

        // Do normal version
        return super.getEvalTypeImpl(aNode);
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
            default: return "ClassDecl";
        }
    }
}