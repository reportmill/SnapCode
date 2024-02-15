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

    // Whether completer has been primed
    private static boolean _isPrimed, _isPriming;

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
     * Returns completion for id expression node.
     */
    public JavaDecl[] getCompletionsForId(JExprId anId)
    {
        // Get resolver for id node
        _resolver = anId.getResolver();
        if (_resolver == null) {
            JFile jfile = anId.getFile();
            JClassDecl classDecl = jfile.getClassDecl();
            String className = classDecl != null ? classDecl.getName() : "Unknown";
            System.err.println("JavaCompleter: No resolver for source file: " + className);
            return NO_MATCHES;
        }

        // If completions not yet primed, start prime and return no matches
        if (!_isPrimed)
            return primeCompletions(_resolver);

        // Get prefix matcher
        String prefix = anId.getName();
        DeclMatcher prefixMatcher = prefix != null ? new DeclMatcher(prefix) : null;
        if (prefixMatcher == null)
            return NO_MATCHES;

        // Add completions for id
        addCompletionsForId(anId, prefixMatcher);

        // If alloc expression, replace classes with constructors
        if (isAllocExprId(anId))
            replaceClassesWithConstructors();

        // If no matches, just return
        if (_list.size() == 0)
            return NO_MATCHES;

        // Get receiving class - If just Object, clear it out
        JavaClass receivingClass = ReceivingClass.getReceivingClass(anId);
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
    private void addCompletionsForId(JExprId anId, DeclMatcher prefixMatcher)
    {
        // If id is scoped, do special case
        JExpr scopeExpr = anId.getScopeExpr();
        if (scopeExpr != null) {
            addCompletionsForScopedId(scopeExpr, prefixMatcher);
            return;
        }

        // Handle JVarDecl.Id: Only offer camel case name
        JNode parent = anId.getParent();
        if (parent instanceof JVarDecl) {
            JVarDecl varDecl = (JVarDecl) parent;
            if (anId == varDecl.getId()) {
                addCompletionsForNewVarDecl((JVarDecl) parent);
                return;
            }
        }

        // Add reserved word completions (public, private, for, white, etc.)
        addWordCompletions(prefixMatcher);

        // Get variables with prefix of name and add to completions
        List<JVarDecl> varDecls = prefixMatcher.getVarDeclsForId(anId);
        for (JVarDecl varDecl : varDecls)
            addCompletionDecl(varDecl.getDecl());

        // Get enclosing class
        JClassDecl enclosingClassDecl = anId.getEnclosingClassDecl();
        JavaClass enclosingClass = enclosingClassDecl != null ? enclosingClassDecl.getEvalClass() : null;

        // Add methods of enclosing class
        while (enclosingClass != null) {

            // Add matching members and inner classes for enclosing class
            JavaMember[] matchingMembers = prefixMatcher.getMembersForClass(enclosingClass, false);
            addCompletionDecls(matchingMembers);
            JavaClass[] matchingInnerClasses = prefixMatcher.getInnerClassesForClass(enclosingClass);
            addCompletionDecls(matchingInnerClasses);

            // Get next enclosing class
            enclosingClassDecl = enclosingClassDecl.getEnclosingClassDecl();
            enclosingClass = enclosingClassDecl != null ? enclosingClassDecl.getEvalClass() : null;
        }

        // Get matching classes and add completion decl
        JavaClass[] matchingClasses = prefixMatcher.getClassesForResolver(_resolver);
        addCompletionDecls(matchingClasses);

        // Get matching packages and add
        JavaPackage rootPackage = _resolver.getJavaPackageForName("");
        JavaPackage[] rootPackages = rootPackage.getPackages();
        JavaPackage[] matchingPackages = ArrayUtils.filter(rootPackages, pkg -> prefixMatcher.matchesString(pkg.getSimpleName()));
        addCompletionDecls(matchingPackages);

        // Add matches for static imports
        JFile jfile = anId.getFile();
        JImportDecl[] staticImportDecls = jfile.getStaticImportDecls();
        JavaMember[] matchingMembers = prefixMatcher.getMembersForStaticImports(staticImportDecls);
        addCompletionDecls(matchingMembers);
    }

    /**
     * Find completions for scoped id (e.g.: something.something).
     *   - Class or package names (if parent is package)
     *   - Fields or methods names (if parent evaluates to type)
     */
    private void addCompletionsForScopedId(JExpr scopeExpr, DeclMatcher prefixMatcher)
    {
        // Get scope expression decl
        JavaDecl scopeDecl = scopeExpr.getDecl();
        if (scopeDecl instanceof JavaLocalVar || scopeDecl instanceof JavaMember)
            scopeDecl = scopeExpr.getEvalType();

        // If no scope decl, get completions for string
        if (scopeDecl == null)
            return;

        // Handle parent is Package: Add packages and classes with prefix
        if (scopeDecl instanceof JavaPackage) {

            // Get parent package name
            JavaPackage parentPackage = (JavaPackage) scopeDecl;
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
        else if (scopeDecl instanceof JavaType) {

            // Get ScopeExpr.EvalClass
            JavaClass scopeExprEvalClass = scopeDecl.getEvalClass();

            // Get whether expression is class name
            boolean staticMembersOnly = scopeExpr.isClassNameLiteral();
            if (staticMembersOnly) {

                // If parent is MethodRef, reset value so we get all methods
                if (scopeExpr.getParent() instanceof JExprMethodRef)
                    staticMembersOnly = false;

                // Add completion for "ClassName.class"
                else if (prefixMatcher.matchesString("class")) {
                    JavaField classField = getClassField(scopeExprEvalClass);
                    addCompletionDecl(classField);
                }
            }

            // Get matching members (fields, methods) for class and add
            JavaMember[] matchingMembers = prefixMatcher.getMembersForClass(scopeExprEvalClass, staticMembersOnly);
            addCompletionDecls(matchingMembers);

            // If class name literal, get matching inner classes
            if (staticMembersOnly) {
                JavaClass[] matchingInnerClasses = prefixMatcher.getInnerClassesForClass(scopeExprEvalClass);
                addCompletionDecls(matchingInnerClasses);
            }
        }
    }

    /**
     * Adds word completions for matcher.
     */
    private void addWordCompletions(DeclMatcher prefixMatcher)
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
    private void addCompletionsForNewVarDecl(JVarDecl varDecl)
    {
        // Get type from var decl
        JavaType evalType = varDecl.getJavaType();
        if (evalType == null || evalType.isPrimitive() || evalType.isArray() && evalType.getComponentType().isPrimitive())
            return;

        // Get suggested var name from type name
        String typeName = evalType.getSimpleName();
        String varName = StringUtils.firstCharLowerCase(typeName);
        String prefix = varDecl.getName().toLowerCase();

        // If Swing class with "J" prefix, create/add suggestion for name without "J"
        if (varName.length() > 1 && varName.charAt(0) == 'j' && Character.isUpperCase(varName.charAt(1))) {
            String varNameNoJ = StringUtils.firstCharLowerCase(varName.substring(1));
            if (varNameNoJ.toLowerCase().startsWith(prefix))
                addCompletionDecl(new JavaWord(varNameNoJ, JavaWord.WordType.Unknown));
        }

        // Create/add suggestion from type name
        JavaDecl nameDecl = new JavaWord(varName, JavaWord.WordType.Unknown);
        if (varName.toLowerCase().startsWith(prefix))
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
     * Adds completion.
     */
    private void addCompletionDecls(JavaDecl[] theDecls)
    {
        for (JavaDecl decl : theDecls)
            addCompletionDecl(decl);
    }

    /**
     * Replaces classes with constructors.
     */
    private void replaceClassesWithConstructors()
    {
        for (int i = _list.size() - 1; i >= 0; i--) {
            JavaDecl decl = _list.get(i);
            if (decl instanceof JavaClass) {
                JavaClass javaClass = (JavaClass) decl;
                if (!javaClass.isPrimitive()) {
                    _list.remove(i);
                    JavaConstructor[] constructors = javaClass.getDeclaredConstructors();
                    addCompletionDecls(constructors);
                }
            }
        }
    }

    /**
     * Returns whether id expression is the type identifier of alloc expression.
     */
    private static boolean isAllocExprId(JExprId idExpr)
    {
        JNode parent = idExpr.getParent();
        if (parent instanceof JType) {
            JNode typeParent = parent.getParent();
            if (typeParent instanceof JExprAlloc)
                return true;
        }
        return false;
    }

    /**
     * Returns the class field for a given Class (for Class.class).
     */
    private JavaField getClassField(JavaClass classClass)
    {
        JavaField.FieldBuilder fb = new JavaField.FieldBuilder();
        fb.init(_resolver, classClass.getClassName());
        JavaField javaField = fb.name("class").type(Class.class).build();
        return javaField;
    }

    /**
     * Prime completions for given resolver.
     */
    private static JavaDecl[] primeCompletions(Resolver aResolver)
    {
        if (!_isPriming)
            new Thread(() -> primeCompletionsImpl(aResolver)).start();
        _isPriming = true;
        return NO_MATCHES;
    }

    /**
     * Prime completions for given resolver.
     */
    private static void primeCompletionsImpl(Resolver aResolver)
    {
        DeclMatcher declMatcher = new DeclMatcher("a");
        declMatcher.getClassesForResolver(aResolver);
        _isPrimed = true;
    }
}