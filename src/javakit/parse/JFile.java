/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import java.util.function.Supplier;
import javakit.resolver.*;
import snap.util.ListUtils;
import snap.web.WebFile;

/**
 * The top level Java part describing a Java file.
 */
public class JFile extends JNode {

    // The source file for this JFile
    protected WebFile  _sourceFile;

    // The resolver for source file
    protected Resolver _resolver;

    // The resolver for source file
    protected Supplier<Resolver> _resolverSupplier;

    // The full Java text as chars (optional)
    private CharSequence _javaChars;

    // The package declaration
    protected JPackageDecl  _packageDecl;

    // The list of imports
    protected List<JImportDecl>  _importDecls = new ArrayList<>();

    // The list of class declarations
    protected List<JClassDecl>  _classDecls = new ArrayList<>();

    // The static imports
    private JImportDecl[] _staticImportDecls;

    // The parse exception, if one was hit
    protected Exception  _exception;

    // An array to hold unused imports
    protected JImportDecl[]  _unusedImports;

    /**
     * Constructor.
     */
    public JFile()
    {
        super();
    }

    /**
     * Returns the WebFile for this JFile.
     */
    public WebFile getSourceFile()  { return _sourceFile; }

    /**
     * Sets the WebFile for this JFile.
     */
    public void setSourceFile(WebFile aFile)
    {
        _sourceFile = aFile;
    }

    /**
     * Returns whether file is Repl Java instead of just java.
     */
    public boolean isRepl()  { return  _sourceFile != null && (_sourceFile.getFileType().equals("jepl") || _sourceFile.getFileType().equals("jmd")); }

    /**
     * Returns the Resolver.
     */
    public Resolver getResolver()
    {
        // If already set, just return
        if (_resolver != null) return _resolver;

        // Get Resolver
        Resolver resolver = _resolverSupplier != null ? _resolverSupplier.get() : null;
        if (resolver == null)
            System.err.println("JFile.getResolver: No source file or project");

        // Set, return
        return _resolver = resolver;
    }

    /**
     * Sets the callback to get resolver.
     */
    public void setResolverSupplier(Supplier<Resolver> aResolverSupplier)
    {
        _resolverSupplier = aResolverSupplier;
    }

    /**
     * Returns the Java file characters.
     */
    public CharSequence getJavaChars()  { return _javaChars; }

    /**
     * Sets the Java file characters.
     */
    public void setJavaChars(CharSequence theChars)  { _javaChars = theChars; }

    /**
     * Returns the package declaration.
     */
    public JPackageDecl getPackageDecl()  { return _packageDecl; }

    /**
     * Sets the package declaration.
     */
    public void setPackageDecl(JPackageDecl aPD)
    {
        replaceChild(_packageDecl, _packageDecl = aPD);
    }

    /**
     * Returns the package name.
     */
    public String getPackageName()
    {
        return _packageDecl != null ? _packageDecl.getName() : null;
    }

    /**
     * Returns the import statements.
     */
    public List<JImportDecl> getImportDecls()  { return _importDecls; }

    /**
     * Adds an import declaration.
     */
    public void addImportDecl(JImportDecl importDecl)
    {
        // Get index to insert after last import (JeplParser can add extra imports after Class decl added)
        int index = getChildCount();
        if (!_importDecls.isEmpty() && !_classDecls.isEmpty()) {
            JImportDecl lastImportDecl = _importDecls.get(_importDecls.size() - 1);
            index = _children.indexOf(lastImportDecl) + 1;
        }

        _importDecls.add(importDecl);
        addChild(importDecl, index);
    }

    /**
     * Returns the JClassDecl for the file.
     */
    public JClassDecl getClassDecl()  { return !_classDecls.isEmpty() ? _classDecls.get(0) : null; }

    /**
     * Returns the JClassDecls for the file.
     */
    public List<JClassDecl> getClassDecls()  { return _classDecls; }

    /**
     * Adds a JClassDecls for the file.
     */
    public void addClassDecl(JClassDecl aClassDecl)
    {
        _classDecls.add(aClassDecl);
        addChild(aClassDecl);
    }

    /**
     * Returns static imports.
     */
    public JImportDecl[] getStaticImportDecls()
    {
        if (_staticImportDecls != null) return _staticImportDecls;
        JImportDecl[] staticImports = ListUtils.filterToArray(_importDecls, importDecl -> importDecl.isStatic(), JImportDecl.class);
        return _staticImportDecls = staticImports;
    }

    /**
     * Override to return this file node.
     */
    @Override
    public JFile getFile()  { return this; }

    /**
     * Override to get name from ClassDecl.
     */
    @Override
    protected String getNameImpl()
    {
        JClassDecl classDecl = getClassDecl();
        return classDecl != null ? classDecl.getName() : null;
    }

    /**
     * Returns the type class of this file.
     */
    @Override
    protected JavaDecl getDeclImpl()
    {
        JClassDecl classDecl = getClassDecl();
        return classDecl != null ? classDecl.getJavaClass() : null;
    }

    /**
     * Override to check for package name, import class name, static import class member.
     */
    @Override
    protected JavaDecl getDeclForChildId(JExprId anExprId)
    {
        // See if it's a known class name using imports
        String idName = anExprId.getName();
        String className = getImportClassName(idName);
        JavaClass javaClass = className != null ? getJavaClassForName(className) : null;
        if (javaClass != null)
            return javaClass;

        // See if it's a known static import class member
        JavaMember field = getStaticImportMemberForNameAndParamTypes(idName, null);
        if (field != null)
            return field;

        // If name is known package name, return package
        if (isKnownPackageName(idName))
            return getJavaPackageForName(idName);

        // Do normal version
        return super.getDeclForChildId(anExprId);
    }

    /**
     * Override - from old getDeclForChildNode(). Is it really needed ???
     */
    @Override
    protected JavaType getJavaTypeForChildType(JType aJType)
    {
        // See if it's a known class name using imports
        String typeName = aJType.getName();
        String className = getImportClassName(typeName);
        JavaClass javaClass = className != null ? getJavaClassForName(className) : null;
        if (javaClass != null)
            return javaClass;

        // These were from legacy getDeclForChildIdOrType() - I think they are bogus and should go
        if (isKnownPackageName(typeName)) //return getJavaPackageForName(typeName);
            System.err.println("JFile.getJavaTypeForChildType: Shouldn't find package: " + typeName);
        JavaMember field = getStaticImportMemberForNameAndParamTypes(typeName, null);
        if (field != null) //return field;
            System.err.println("JFile.getJavaTypeForChildType: Shouldn't find member: " + typeName);

        // Do normal version
        return super.getJavaTypeForChildType(aJType);
    }

    /**
     * Returns an import that can be used to resolve the given class name.
     */
    public JImportDecl getImportForClassName(String aName)
    {
        // Handle fully specified name
        if (isKnownClassName(aName))
            return null;

        // Find matching or containing import for name
        JImportDecl match = ListUtils.findMatch(_importDecls, id -> id.matchesName(aName));
        if (match == null)
            match = ListUtils.findMatch(_importDecls, id -> id.containsName(aName));

        // Remove match from UnusedImports and return
        if (match != null) {
            if (match.isInclusive())
                match.addFoundClassName(aName);
            match._used = true;
        }

        // Return
        return match;
    }

    /**
     * Returns a Class name for given name referenced in file.
     */
    public String getImportClassName(String aName)
    {
        // Handle fully specified name (or java.lang name)
        JavaClass knownClass = getJavaClassForName(aName);
        if (knownClass != null)
            return knownClass.getName();

        // If name has parts, handle them separately
        if (aName.indexOf('.') > 0) {

            // Get import part names
            String[] names = aName.split("\\.");
            String className = getImportClassName(names[0]);
            if (className == null)
                return null;

            // Get JavaClass for name
            JavaClass javaClass = getJavaClassForName(className);
            for (int i = 1; javaClass != null && i < names.length; i++)
                javaClass = javaClass.getDeclaredClassForName(names[i]);

            // Return class name
            return javaClass != null ? javaClass.getName() : null;
        }

        // Try "java.lang" + name
        JavaClass javaLangClass = getJavaClassForName("java.lang." + aName);
        if (javaLangClass != null)
            return javaLangClass.getName();

        // If file declares package, see if it's in package
        String packageName = getPackageName();
        if (packageName != null && !packageName.isEmpty()) {
            String className = packageName + '.' + aName;
            if (isKnownClassName(className))
                return className;
        }

        // If import declaration for name, return import class
        JImportDecl importDecl = getImportForClassName(aName);
        if (importDecl != null)
            return importDecl.getImportClassName(aName);

        // Return not found
        return null;
    }

    /**
     * Returns a Class name for given name referenced in file.
     */
    public JavaMember getStaticImportMemberForNameAndParamTypes(String aName, JavaClass[] paramTypes)
    {
        // If static import for name, look for member there
        JImportDecl importDecl = getStaticImportForNameAndParamTypes(aName, paramTypes);
        if (importDecl != null)
            return importDecl.getImportMemberForNameAndParamTypes(aName, paramTypes);

        // Return not found
        return null;
    }

    /**
     * Returns an import that can be used to resolve the given field/method/class name (optional params for method).
     */
    private JImportDecl getStaticImportForNameAndParamTypes(String aName, JavaClass[] paramTypes)
    {
        // Get static imports (e.g.: "import static xxx.*")
        JImportDecl[] staticImportDecls = getStaticImportDecls();

        // Iterate over static imports to see if any can resolve name from static field/method/class
        for (int i = staticImportDecls.length - 1; i >= 0; i--) {
            JImportDecl importDecl = staticImportDecls[i];
            JavaMember member = importDecl.getImportMemberForNameAndParamTypes(aName, paramTypes);
            if (member != null) {
                if (importDecl.isInclusive())
                    importDecl.addFoundClassName(aName);
                importDecl._used = true;
                return importDecl;
            }
        }

        // Return not found
        return null;
    }

    /**
     * Returns unused imports for file.
     */
    public JImportDecl[] getUnusedImports()
    {
        // If already set, just return
        if (_unusedImports != null) return _unusedImports;

        // Get import decls
        List<JImportDecl> importDecls = getImportDecls();
        if (isRepl()) {
            importDecls = ListUtils.filter(importDecls, imp -> imp.getStartToken() != imp.getEndToken());
        }

        // Resolve class names
        Set<JImportDecl> unusedImportDecls = new HashSet<>(importDecls);
        resolveClassNames(this, unusedImportDecls);

        // Set array and return
        return _unusedImports = unusedImportDecls.toArray(new JImportDecl[0]);
    }

    /**
     * Forces all nodes to resolve class names.
     */
    private void resolveClassNames(JNode aNode, Set<JImportDecl> theImports)
    {
        if (aNode instanceof JImportDecl || aNode instanceof JPackageDecl)
            return;

        // Handle JType
        if (aNode instanceof JType || aNode instanceof JExprId) {

            // Get JavaClass - just return if not found or primitive or 'java.lang' class
            JavaClass javaClass = aNode.getEvalClass();
            if (javaClass == null || javaClass.isPrimitive())
                return;
            JavaPackage javaPackage = javaClass.getPackage();
            if (javaPackage == null || javaPackage.getName().equals("java.lang"))
                return;

            // Get JavaClass.ClassName - just return if java.lang
            String className = javaClass.getClassName();

            // Find import for class name
            JImportDecl classImport = ListUtils.findMatch(theImports, id -> className.startsWith(id.getName()));
            if (classImport != null)
                theImports.remove(classImport);
        }

        // Recurse for children
        for (JNode child : aNode.getChildren()) {
            resolveClassNames(child, theImports);
            if (theImports.isEmpty())
                break;
        }
    }

    /**
     * Returns the exception if one was hit.
     */
    public Exception getException()  { return _exception; }

    /**
     * Sets the exception.
     */
    public void setException(Exception anException)
    {
        _exception = anException;
    }

    /**
     * Returns any current declaration errors.
     */
    public NodeError[] getDeclarationErrors()
    {
        JClassDecl classDecl = getClassDecl();
        return classDecl != null ? classDecl.getErrors() : NodeError.NO_ERRORS;
    }

    /**
     * Init from another JFile.
     */
    protected void init(JFile aJFile)
    {
        _name = aJFile._name;
        _startToken = aJFile._startToken;
        _endToken = aJFile._endToken;
        _children = aJFile._children;
        for (JNode c : _children) c._parent = this;

        _sourceFile = aJFile._sourceFile;
        _resolver = aJFile._resolver;
        _packageDecl = aJFile._packageDecl;
        _importDecls = aJFile._importDecls;
        _classDecls = aJFile._classDecls;
        _exception = aJFile._exception;
    }

    /** Print expanded imports. */
    /*private void printExpandedExports()
    {
        // If no expansions, just return
        boolean hasExp = false; for (JImportDecl i : getImportDecls()) if(i.isInclusive()) hasExp = true;
        if(!hasExp) return;

        // Print expansions
        System.out.println("Expanded imports in file " + getClassName() + ":");
        for (JImportDecl imp : getImportDecls()) {
            if (imp.isInclusive() && !imp.isStatic() && imp.getFoundClassNames().size()>0) {
                System.out.print("    " + imp.getString().trim().replace(';',':') + ' ');
                List <String> names = new ArrayList(imp.getFoundClassNames());
                String last = names.size()>0? names.get(names.size()-1):null;
                for (String n : names) {
                    System.out.print(n); if (n!=last) System.out.print(", "); else System.out.println(); }
            }
        }
    }*/
}