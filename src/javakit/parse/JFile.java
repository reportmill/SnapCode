/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.util.*;
import javakit.project.Project;
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

    // The full Java string (optional)
    private String  _javaFileString;

    // The package declaration
    protected JPackageDecl  _packageDecl;

    // The list of imports
    protected List<JImportDecl>  _importDecls = new ArrayList<>();

    // The list of class declarations
    protected List<JClassDecl>  _classDecls = new ArrayList<>();

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
     * Returns the Resolver.
     */
    public Resolver getResolver()
    {
        // If already set, just return
        if (_resolver != null) return _resolver;

        // Get Resolver
        Project proj = _sourceFile != null ? Project.getProjectForFile(_sourceFile) : null;
        Resolver resolver = proj != null ? proj.getResolver() : null;
        if (resolver == null)
            System.err.println("JFile.getResolver: No source file or project");

        // Set, return
        return _resolver = resolver;
    }

    /**
     * Sets the resolver.
     */
    public void setResolver(Resolver aResolver)
    {
        _resolver = aResolver;
    }

    /**
     * Returns the Java file string if set.
     */
    public String getJavaFileString()  { return _javaFileString; }

    /**
     * Sets the Java file string.
     */
    public void setJavaFileString(String aString)  { _javaFileString = aString; }

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
    public void addImportDecl(JImportDecl anID)
    {
        _importDecls.add(anID);
        addChild(anID, -1);
    }

    /**
     * Returns the JClassDecl for the file.
     */
    public JClassDecl getClassDecl()
    {
        return _classDecls.size() > 0 ? _classDecls.get(0) : null;
    }

    /**
     * Returns the JClassDecls for the file.
     */
    public List<JClassDecl> getClassDecls()  { return _classDecls; }

    /**
     * Adds a JClassDecls for the file.
     */
    public void addClassDecl(JClassDecl aCD)
    {
        _classDecls.add(aCD);
        addChild(aCD, -1);
    }

    /**
     * Override to return this file node.
     */
    public JFile getFile()
    {
        return this;
    }

    /**
     * Override to get name from ClassDecl.
     */
    protected String getNameImpl()
    {
        JClassDecl classDecl = getClassDecl();
        return classDecl != null ? classDecl.getName() : null;
    }

    /**
     * Returns the type class of this file.
     */
    protected JavaDecl getDeclImpl()
    {
        JClassDecl classDecl = getClassDecl();
        return classDecl != null ? classDecl.getDecl() : null;
    }

    /**
     * Override to check for package name, import class name, static import class member.
     */
    @Override
    protected JavaDecl getDeclForChildExprIdNode(JExprId anExprId)
    {
        // Get node info
        String name = anExprId.getName();

        // If it's in JPackageDecl, it's a Package
        if (isKnownPackageName(name))
            return getJavaPackageForName(name);

        // See if it's a known class name using imports
        String className = getImportClassName(name);
        JavaClass javaClass = className != null ? getJavaClassForName(className) : null;
        if (javaClass != null)
            return javaClass;

        // See if it's a known static import class member
        JavaDecl field = getImportClassMember(name, null);
        if (field != null)
            return field;

        // Do normal version
        return super.getDeclForChildExprIdNode(anExprId);
    }

    /**
     * Override - from old getDeclForChildNode(). Is it really needed ???
     */
    @Override
    protected JavaDecl getDeclForChildTypeNode(JType aJType)
    {
        // Get node info
        String name = aJType.getName();

        // If it's in JPackageDecl, it's a Package
        if (isKnownPackageName(name))
            return getJavaPackageForName(name);

        // See if it's a known class name using imports
        String className = getImportClassName(name);
        JavaClass javaClass = className != null ? getJavaClassForName(className) : null;
        if (javaClass != null)
            return javaClass;

        // See if it's a known static import class member
        JavaDecl field = getImportClassMember(name, null);
        if (field != null)
            return field;

        // Do normal version
        return super.getDeclForChildTypeNode(aJType);
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
                javaClass = javaClass.getInnerClassForName(names[i]);

            // Return class name
            return javaClass != null ? javaClass.getName() : null;
        }

        // Try "java.lang" + name
        JavaClass javaLangClass = getJavaClassForName("java.lang." + aName);
        if (javaLangClass != null)
            return javaLangClass.getName();

        // If file declares package, see if it's in package
        String packageName = getPackageName();
        if (packageName != null && packageName.length() > 0) {
            String className = packageName + '.' + aName;
            if (isKnownClassName(className))
                return className;
        }

        // Get import for name
        JImportDecl imp = getImportForClassName(aName);
        if (imp != null)
            return imp.getImportClassName(aName);

        // Return not found
        return null;
    }

    /**
     * Returns a Class name for given name referenced in file.
     */
    public JavaMember getImportClassMember(String aName, JavaType[] theParams)
    {
        // If static import for name, look for member there
        JImportDecl importDecl = getStaticImport(aName, theParams);
        if (importDecl != null)
            return importDecl.getImportMember(aName, theParams);

        // Return not found
        return null;
    }

    /**
     * Returns an import that can be used to resolve the given name.
     */
    private JImportDecl getStaticImport(String aName, JavaType[] theParams)
    {
        // Iterate over imports to see if any can resolve name
        for (int i = _importDecls.size() - 1; i >= 0; i--) {
            JImportDecl importDecl = _importDecls.get(i);

            // If import is static ("import static xxx.*") and name/params is known field/method, return member
            if (importDecl.isStatic()) {
                JavaMember member = importDecl.getImportMember(aName, theParams);
                if (member != null) {
                    if (importDecl.isInclusive())
                        importDecl.addFoundClassName(aName);
                    importDecl._used = true;
                    return importDecl;
                }
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

        // Resolve class names
        Set<JImportDecl> importDecls = new HashSet<>(getImportDecls());
        resolveClassNames(this, importDecls);

        // Set, return
        return _unusedImports = importDecls.toArray(new JImportDecl[0]);
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

            // Get JavaClass - just return if not found or primitive
            JavaClass javaClass = aNode.getEvalClass();
            if (javaClass == null || javaClass.isPrimitive())
                return;

            // Get JavaClass.ClassName - just return if java.lang
            String className = javaClass.getClassName();
            if (className.startsWith("java.lang."))
                return;

            // Find import for class name
            JImportDecl classImport = ListUtils.findMatch(theImports, id -> className.startsWith(id.getName()));
            if (classImport != null)
                theImports.remove(classImport);
        }

        // Recurse for children
        for (JNode child : aNode.getChildren()) {
            resolveClassNames(child, theImports);
            if (theImports.size() == 0)
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