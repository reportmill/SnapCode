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
import snap.util.ArrayUtils;
import snap.util.StringUtils;

/**
 * A class to provide code completion suggestions for a given JNode.
 */
public class NodeCompleter {

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
        while (enclosingClass != null) {
            JavaMember[] matchingMembers = prefixMatcher.getMembersForClass(enclosingClass, false);
            for (JavaMember matchingMember : matchingMembers)
                addCompletionDecl(matchingMember);
            enclosingClassDecl = enclosingClassDecl.getEnclosingClassDecl();
            enclosingClass = enclosingClassDecl != null ? enclosingClassDecl.getEvalClass() : null;
        }

        // Get matching classes and add completion decl
        JavaClass[] matchingClasses = prefixMatcher.getClassesForResolver(_resolver);
        for (JavaClass matchingClass : matchingClasses)
            addCompletionDecl(matchingClass);

        // Get matching packages and add
        JavaPackage rootPackage = _resolver.getJavaPackageForName("");
        JavaPackage[] rootPackages = rootPackage.getPackages();
        JavaPackage[] matchingPackages = ArrayUtils.filter(rootPackages, pkg -> prefixMatcher.matchesString(pkg.getSimpleName()));
        for (JavaPackage matchingPkg : matchingPackages)
            addCompletionDecl(matchingPkg);

        // Add matches for static imports
        JFile jfile = aNode.getFile();
        JImportDecl[] staticImportDecls = jfile.getStaticImportDecls();
        JavaMember[] matchingMembers = prefixMatcher.getMembersForStaticImports(staticImportDecls);
        for (JavaMember matchingMember : matchingMembers)
            addCompletionDecl(matchingMember);
    }

    /**
     * Find completions for JExprId.
     *   - Class or package names (if parent is package)
     *   - Fields or methods names (if parent evaluates to type)
     */
    private void getCompletionsForExprId(JExprId anId, DeclMatcher prefixMatcher)
    {
        // Handle JVarDecl.Id: Only offer camel case name
        JNode parent = anId.getParent();
        if (parent instanceof JVarDecl) {
            JVarDecl varDecl = (JVarDecl) parent;
            if (anId == varDecl.getId()) {
                getCompletionsForNewVarDecl((JVarDecl) parent);
                return;
            }
        }

        // Get parent expression - if none, forward to basic getCompletionsForNodeString()
        JExpr parExpr = anId.getScopeExpr();
        JavaDecl parentDecl = parExpr != null ? parExpr.getDecl() : null;
        if (parentDecl == null) {
            getCompletionsForNodeString(anId, prefixMatcher);
            return;
        }

        // Handle parent is Package: Add packages and classes with prefix
        if (parentDecl instanceof JavaPackage) {

            // Get parent package name
            JavaPackage parentPackage = (JavaPackage) parentDecl;
            JavaDecl[] packageChildren = parentPackage.getChildren();

            // Get matching children
            JavaDecl[] matchingChildren = ArrayUtils.filter(packageChildren, decl -> prefixMatcher.matchesString(decl.getSimpleName()));

            // Add matching children (skip non-public classes)
            for (JavaDecl matchingDecl : matchingChildren) {
                if (matchingDecl instanceof JavaClass) {
                    JavaClass matchingClass = (JavaClass) matchingDecl;
                    if (!Modifier.isPublic(matchingClass.getModifiers()))
                        continue;
                }
                addCompletionDecl(matchingDecl);
            }
        }

        // Handle anything else with a parent class
        else if (parentDecl instanceof JavaType) {

            // Get ParentExpr.EvalClass
            JavaClass parExprEvalClass = parentDecl.getEvalClass();

            // Get matching members (fields, methods) for class and add
            JavaMember[] matchingMembers = prefixMatcher.getMembersForClass(parExprEvalClass, false);
            for (JavaMember matchingMember : matchingMembers)
                addCompletionDecl(matchingMember);
        }
    }

    /**
     * Find completions for JType:
     *   - Constructors (if parent is alloc expr)
     *   - Class names (all other cases)
     */
    private void getCompletionsForType(JType aJType, DeclMatcher prefixMatcher)
    {
        // Handle JType as AllocExpr
        JNode typeParent = aJType.getParent();
        if (typeParent instanceof JExprAlloc) {

            // Get all matching classes
            JavaClass[] matchingClasses = prefixMatcher.getClassesForResolver(_resolver);

            // Iterate over classes and add constructors
            for (JavaClass matchingClass : matchingClasses) {

                // Get Constructors
                List<JavaConstructor> constructors = matchingClass.getDeclaredConstructors();

                // Add constructors
                for (JavaConstructor constructor : constructors)
                    addCompletionDecl(constructor);

                // Handle primitive
                if (matchingClass.isPrimitive())
                    addCompletionDecl(matchingClass);
            }
        }

        // Handle single token nodes
        if (aJType.getStartToken() == aJType.getEndToken())
            getCompletionsForNodeString(aJType, prefixMatcher);
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
     * Adds a single completion for a new variable declaration using the type name.
     */
    private void getCompletionsForNewVarDecl(JVarDecl varDecl)
    {
        // Remove other completions
        _list.clear();

        // Get type from var decl
        JType varDeclType = varDecl.getType();
        if (varDeclType == null)
            return;
        JavaType evalType = varDeclType.getDecl();

        // Get suggested var name from type name
        String typeName = evalType != null ? evalType.getSimpleName() : varDeclType.getSimpleName();
        String varName = StringUtils.firstCharLowerCase(typeName);

        // If Swing class with "J" prefix, create/add suggestion for name without "J"
        if (varName.length() > 1 && varName.charAt(0) == 'j' && Character.isUpperCase(varName.charAt(1))) {
            String varNameNoJ = StringUtils.firstCharLowerCase(varName.substring(1));
            addCompletionDecl(new JavaWord(varNameNoJ, JavaWord.WordType.Unknown));
        }

        // Create/add suggestion from type name
        JavaDecl nameDecl = new JavaWord(varName, JavaWord.WordType.Unknown);
        addCompletionDecl(nameDecl);
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