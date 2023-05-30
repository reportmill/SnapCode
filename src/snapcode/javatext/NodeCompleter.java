/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javakit.parse.*;
import javakit.resolver.*;
import snap.parse.ParseToken;

/**
 * A class to provide code completion suggestions for a given JNode.
 */
public class NodeCompleter {

    // The node
    private JNode  _node;

    // The resolver
    private Resolver  _resolver;

    // The list of completions
    private List<JavaDecl>  _list = new ArrayList<>();

    // An identifier matcher
    private static Matcher  _idMatcher;

    // Constant for no matches
    private static JavaDecl[]  NO_MATCHES = new JavaDecl[0];

    /**
     * Constructor.
     */
    public NodeCompleter()
    {
        super();

        // Create/set IdMatcher
        if (_idMatcher == null) {
            String regexStr = "[$_a-zA-Z][$\\w]*";
            Pattern pattern = Pattern.compile(regexStr);
            _idMatcher = pattern.matcher("");
        }
    }

    /**
     * Returns completion for JNode (should be JType or JIdentifier).
     */
    public JavaDecl[] getCompletionsForNode(JNode aNode)
    {
        // Set node
        _node = aNode;

        // Get SourceFile Project
        _resolver = aNode.getResolver();
        if (_resolver == null) {
            JFile jfile = aNode.getFile();
            JClassDecl classDecl = jfile.getClassDecl();
            String className = classDecl != null ? classDecl.getName() : "Unknown";
            System.err.println("JavaCompleter: No resolver for source file: " + className);
            return NO_MATCHES;
        }

        // Get prefix matcher
        String prefix = getNodeString(aNode);
        DeclMatcher prefixMatcher = prefix != null ? new DeclMatcher(prefix) : null;
        if (prefixMatcher == null)
            return NO_MATCHES;

        // Add reserved word completions (public, private, for, white, etc.)
        addWordCompletionsForMatcher(prefixMatcher);

        // Add completions for node
        if (aNode instanceof JExprId)
            getCompletionsForExprId((JExprId) aNode, prefixMatcher);
        else if (aNode instanceof JType)
            getCompletionsForType((JType) aNode, prefixMatcher);
        else if (aNode.getStartToken() == aNode.getEndToken())
            getCompletionsForNodeString(aNode, prefixMatcher);

        // If no matches, just return
        if (_list.size() == 0)
            return NO_MATCHES;

        // Get receiving class - If just Object, clear it out
        JavaClass receivingClass = ReceivingClass.getReceivingClass(aNode);
        if (receivingClass != null && receivingClass.getName().equals("java.lang.Object"))
            receivingClass = null;

        // Get array and sort
        JavaDecl[] decls = _list.toArray(new JavaDecl[0]);
        Arrays.sort(decls, new DeclCompare(prefixMatcher, receivingClass));
        return decls;
    }

    /**
     * Find completions for any node (name/string)
     *   - Local variable names in scope
     *   - Enclosing class methods
     *   - Class names
     *   - Package names
     */
    private void getCompletionsForNodeString(JNode aNode, DeclMatcher prefixMatcher)
    {
        // Get variables with prefix of name and add to completions
        List<JVarDecl> varDecls = prefixMatcher.getVarDeclsForJNode(aNode, new ArrayList<>());
        for (JVarDecl varDecl : varDecls)
            addCompletionDecl(varDecl.getDecl());

        // Get enclosing class
        JClassDecl enclosingClassDecl = aNode.getEnclosingClassDecl();
        JavaClass enclosingClass = enclosingClassDecl != null ? enclosingClassDecl.getEvalClass() : null;

        // Add methods of enclosing class
        while (enclosingClassDecl != null && enclosingClass != null) {
            JavaMethod[] matchingMethods = prefixMatcher.getMethodsForClass(enclosingClass);
            for (JavaMethod matchingMethod : matchingMethods)
                addCompletionDecl(matchingMethod);
            enclosingClassDecl = enclosingClassDecl.getEnclosingClassDecl();
            enclosingClass = enclosingClassDecl != null ? enclosingClassDecl.getEvalClass() : null;
        }

        // Get matching classes
        ClassTree classTree = getClassTree();
        ClassTree.ClassNode[] matchingClasses = prefixMatcher.getClassesForClassTree(classTree);

        // Iterate over classes and add if public
        for (ClassTree.ClassNode matchingClass : matchingClasses) {
            JavaClass javaClass = _resolver.getJavaClassForName(matchingClass.fullName);
            if (javaClass == null || !Modifier.isPublic(javaClass.getModifiers()))
                continue;
            addCompletionDecl(javaClass);
        }

        // Get matching packages and add
        ClassTree.PackageNode[] matchingPackages = prefixMatcher.getPackagesForClassTree(classTree);
        for (ClassTree.PackageNode matchingPkg : matchingPackages)
            addJavaPackageForName(matchingPkg.fullName);
    }

    /**
     * Find completions for JExprId.
     *   - Class or package names (if parent is package)
     *   - Fields or methods names (if parent evaluates to type)
     */
    private void getCompletionsForExprId(JExprId anId, DeclMatcher prefixMatcher)
    {
        // Get parent expression - if none, forward to basic getCompletionsForNodeString()
        JExpr parExpr = anId.getParentExpr();
        if (parExpr == null) {
            getCompletionsForNodeString(anId, prefixMatcher);
            return;
        }

        // Handle parent is Package: Add packages and classes with prefix
        if (parExpr instanceof JExprId && ((JExprId) parExpr).isPackageName()) {

            // Get parent package name
            JExprId parId = (JExprId) parExpr;
            String parPkgName = parId.getPackageName();

            // Get matching classes for classes in parent package with prefix
            ClassTree classTree = getClassTree();
            ClassTree.ClassNode[] matchingClasses = prefixMatcher.getClassesForClassTreePackageName(classTree, parPkgName);

            // Iterate over matching classes and add public classes
            for (ClassTree.ClassNode matchingClass : matchingClasses) {
                JavaClass javaClass = _resolver.getJavaClassForName(matchingClass.fullName);
                if (javaClass == null || !Modifier.isPublic(javaClass.getModifiers()))
                    continue;
                addCompletionDecl(javaClass);
            }

            // Get package names for packages in parent package with prefix
            ClassTree.PackageNode[] packageChildren = prefixMatcher.getChildPackagesForClassTreePackageName(classTree, parPkgName);
            for (ClassTree.PackageNode pkg : packageChildren)
                addJavaPackageForName(pkg.fullName);
        }

        // Handle anything else with a parent class
        else if (parExpr.getEvalType() != null) {

            // Get ParentExpr.EvalClass
            JavaType parExprEvalType = parExpr.getEvalType();
            JavaClass parExprEvalClass = parExprEvalType.getEvalClass();

            // Get matching fields for class and add
            List<JavaField> matchingFields = prefixMatcher.getFieldsForClass(parExprEvalClass);
            for (JavaField matchingField : matchingFields)
                addCompletionDecl(matchingField);

            // Get matching methods for class and add
            JavaMethod[] matchingMethods = prefixMatcher.getMethodsForClass(parExprEvalClass);
            for (JavaMethod matchingMethod : matchingMethods)
                addCompletionDecl(matchingMethod);
        }
    }

    /**
     * Find completions for JType:
     *   - Constructors (if parent is alloc expr)
     *   - Class names (all other cases)
     */
    private void getCompletionsForType(JType aJType, DeclMatcher prefixMatcher)
    {
        // Get all matching classes
        ClassTree classTree = getClassTree();
        ClassTree.ClassNode[] matchingClasses = prefixMatcher.getClassesForClassTree(classTree);

        // Handle JType as AllocExpr
        JNode typeParent = aJType.getParent();
        if (typeParent instanceof JExprAlloc) {

            // Iterate over classes and add constructors
            for (ClassTree.ClassNode matchingClass : matchingClasses) {

                // Get class (skip if not found or not public)
                JavaClass javaClass = _resolver.getJavaClassForName(matchingClass.fullName);
                if (javaClass == null || !Modifier.isPublic(javaClass.getModifiers()))
                    continue;

                // Get Constructors
                List<JavaConstructor> constructors = javaClass.getConstructors();

                // Add constructors
                for (JavaConstructor constructor : constructors)
                    addCompletionDecl(constructor);

                // Handle primitive
                if (javaClass.isPrimitive())
                    addCompletionDecl(javaClass);
            }
        }

        // Handle single token nodes
        else if (aJType.getStartToken() == aJType.getEndToken())
            getCompletionsForNodeString(aJType, prefixMatcher);

        // Handle normal JType
        else {
            System.err.println("NodeCompleter.getCompletionsForType: Not sure types can have multiple tokens here: " + aJType);
            for (ClassTree.ClassNode matchingClass : matchingClasses) {
                JavaClass javaClass = _resolver.getJavaClassForName(matchingClass.fullName);
                addCompletionDecl(javaClass);
            }
        }
    }

    /**
     * Adds word completions for matcher.
     */
    private void addWordCompletionsForMatcher(DeclMatcher prefixMatcher)
    {
        // Add JavaWords
        for (JavaWord word : JavaWord.ALL)
            if (prefixMatcher.matchesString(word.getName()))
                addCompletionDecl(word);

        // Add Global Literals (true, false, null, this, super
        JavaLocalVar[] globalLiters = _resolver.getGlobalLiterals();
        for (JavaDecl literal : globalLiters)
            if (prefixMatcher.matchesString(literal.getName()))
                addCompletionDecl(literal);
    }

    /**
     * Returns the ClassTree for current Resolver.
     */
    private ClassTree getClassTree()
    {
        ClassPathInfo classPathInfo = _resolver.getClassPathInfo();
        return classPathInfo.getClassTree();
    }

    /**
     * Adds completion.
     */
    private void addCompletionDecl(JavaDecl aDecl)
    {
        if (aDecl == null) return;
        _list.add(aDecl);
    }

    /**
     * Adds a JavaDecl for object.
     */
    private void addJavaPackageForName(String aPackageName)
    {
        JavaDecl javaDecl = _node.getJavaPackageForName(aPackageName);
        addCompletionDecl(javaDecl);
    }

    /**
     * Returns a string for node.
     */
    private String getNodeString(JNode aNode)
    {
        // Handle simple Id node
        if (aNode instanceof JExprId)
            return aNode.getName();

        // Handle no node: Not sure how this can happen yet
        ParseToken startToken = aNode.getStartToken();
        if (startToken == null) {
            System.err.println("NodeCompleter.getNodeString: Node with no tokens: " + aNode);
        }

        // Handle any node with only one token with id string
        else if (startToken == aNode.getEndToken()) {
            String str = startToken.getString();
            if (_idMatcher.reset(str).lookingAt())
                return str;
        }

        // Return not found
        return null;
    }
}