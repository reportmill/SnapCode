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
    private Resolver _resolver;

    // A PrefixMatcher to match current id prefix to completions with fancy camel case matching
    private DeclMatcher _prefixMatcher;

    // The set of completions
    private Set<JavaDecl> _completions = new HashSet<>();

    // An identifier matcher
    private static Matcher _idMatcher;

    // Constant for no matches
    private static List<JavaDecl> NO_MATCHES = Collections.emptyList();

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
    public List<JavaDecl> getCompletionsForId(JExprId anId)
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

        // Get prefix matcher
        String prefix = anId.getName();
        JClassDecl classDecl = anId.getEnclosingClassDecl();
        JavaClass callingClass = classDecl != null ? classDecl.getJavaClass() : null;
        _prefixMatcher = prefix != null ? new DeclMatcher(prefix, callingClass) : null;
        if (_prefixMatcher == null)
            return NO_MATCHES;

        // If body decl id, add body decl completions
        if (isBodyDeclId(anId))
            addCompletionsForBodyDeclId(anId);

        // Add completions for id
        else addCompletionsForId(anId);

        // If alloc expression, replace classes with constructors
        if (isAllocExprId(anId))
            replaceClassesWithConstructors();

        // If no matches, just return
        if (_completions.isEmpty())
            return NO_MATCHES;

        // Get receiving class - If just Object, clear it out
        JavaClass receivingClass = ReceivingClass.getReceivingClass(anId);
        if (receivingClass != null && receivingClass.getName().equals("java.lang.Object"))
            receivingClass = null;

        // Return sorted list
        return _completions.stream().sorted(new DeclCompare(_prefixMatcher, receivingClass)).toList();
    }

    /**
     * Find completions for any node (name/string)
     *   - Local variable names in scope
     *   - Enclosing class methods
     *   - Class names
     *   - Package names
     */
    private void addCompletionsForId(JExprId anId)
    {
        // If id is scoped, do special case
        JExpr scopeExpr = anId.getScopeExpr();
        if (scopeExpr != null) {
            addCompletionsForScopedId(scopeExpr);
            return;
        }

        // If id is JVarDecl.Id, only offer camel case name
        JNode parent = anId.getParent();
        if (parent instanceof JVarDecl varDecl && anId == varDecl.getId()) {
            addCompletionsForNewVarDeclType(varDecl.getJavaType());
            return;
        }

        // If parent is class, add class words
        if (parent instanceof JClassDecl)
            addWordCompletions(JavaWord.CLASS_WORDS);

        // Add reserved word completions (public, private, for, white, etc.)
        else if (parent instanceof JStmtExpr) {
            addWordCompletions(JavaWord.MODIFIERS);
            addWordCompletions(JavaWord.CLASS_WORDS);
            addWordCompletions(JavaWord.STATEMENT_WORDS);
        }

        // Add global literal completions (true, false, null, this, super)
        addGlobalLiteralCompletions();

        // Get variables with prefix of name and add to completions
        List<JVarDecl> varDecls = _prefixMatcher.getVarDeclsForId(anId);
        for (JVarDecl varDecl : varDecls)
            addCompletionDecl(varDecl.getDecl());

        // Get enclosing class
        JClassDecl enclosingClassDecl = anId.getEnclosingClassDecl();
        JavaClass enclosingClass = enclosingClassDecl != null ? enclosingClassDecl.getEvalClass() : null;

        // Get whether in static context
        JBodyDecl enclosingDecl = anId.getParent(JBodyDecl.class);
        boolean isStatic = enclosingDecl != null && enclosingDecl.isStatic();

        // Add methods of enclosing class
        while (enclosingClass != null) {

            // Add matching members and inner classes for enclosing class
            JavaMember[] matchingMembers = _prefixMatcher.getMembersForClass(enclosingClass, isStatic);
            addCompletionDecls(matchingMembers);
            JavaClass[] matchingInnerClasses = _prefixMatcher.getInnerClassesForClass(enclosingClass);
            addCompletionDecls(matchingInnerClasses);

            // Get next enclosing class
            enclosingClassDecl = enclosingClassDecl.getEnclosingClassDecl();
            enclosingClass = enclosingClassDecl != null ? enclosingClassDecl.getEvalClass() : null;
        }

        // Add completions for type id
        addCompletionsForTypeId();

        // Add matches for static imports
        JFile jfile = anId.getFile();
        JImportDecl[] staticImportDecls = jfile.getStaticImportDecls();
        JavaMember[] matchingMembers = _prefixMatcher.getMembersForStaticImports(staticImportDecls);
        addCompletionDecls(matchingMembers);
    }

    /**
     * Adds class and package completions for given type id.
     */
    private void addCompletionsForTypeId()
    {
        // Get matching classes and add completion decl
        JavaClass[] matchingClasses = _prefixMatcher.getClassesForResolver(_resolver);
        addCompletionDecls(matchingClasses);

        // Get matching packages and add
        JavaPackage rootPackage = _resolver.getJavaPackageForName("");
        JavaPackage[] rootPackages = rootPackage.getPackages();
        JavaPackage[] matchingPackages = ArrayUtils.filter(rootPackages, pkg -> _prefixMatcher.matchesString(pkg.getSimpleName()));
        addCompletionDecls(matchingPackages);
    }

    /**
     * Find completions for scoped id (e.g.: something.something).
     *   - Class or package names (if parent is package)
     *   - Fields or methods names (if parent evaluates to type)
     */
    private void addCompletionsForScopedId(JExpr scopeExpr)
    {
        // Get scope expression decl
        JavaDecl scopeDecl = scopeExpr.getDecl();
        if (scopeDecl instanceof JavaLocalVar || scopeDecl instanceof JavaMember)
            scopeDecl = scopeExpr.getEvalType();

        // If no scope decl, get completions for string
        if (scopeDecl == null)
            return;

        // Handle parent is Package: Add packages and classes with prefix
        if (scopeDecl instanceof JavaPackage parentPackage) {

            // Get parent package children that match
            JavaDecl[] packageChildren = parentPackage.getChildren();
            JavaDecl[] matchingChildren = ArrayUtils.filter(packageChildren, decl -> _prefixMatcher.matchesString(decl.getSimpleName()));

            // Add matching children (skip non-public classes)
            for (JavaDecl matchingDecl : matchingChildren) {
                if (matchingDecl instanceof JavaClass matchingClass) {
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
                else if (_prefixMatcher.matchesString("class")) {
                    JavaField classField = JavaField.createField(scopeExprEvalClass, "class", Class.class);
                    addCompletionDecl(classField);
                }
            }

            // Get matching members (fields, methods) for class and add
            JavaMember[] matchingMembers = _prefixMatcher.getMembersForClass(scopeExprEvalClass, staticMembersOnly);
            addCompletionDecls(matchingMembers);

            // If class name literal, get matching inner classes
            if (staticMembersOnly) {
                JavaClass[] matchingInnerClasses = _prefixMatcher.getInnerClassesForClass(scopeExprEvalClass);
                addCompletionDecls(matchingInnerClasses);
            }
        }
    }

    /**
     * Adds word completions for matcher.
     */
    private void addWordCompletions(JavaWord[] javaWords)
    {
        for (JavaWord word : javaWords)
            if (_prefixMatcher.matchesString(word.getName()))
                addCompletionDecl(word);
    }

    /**
     * Add Global Literals (true, false, null, this, super).
     */
    private void addGlobalLiteralCompletions()
    {
        JavaLocalVar[] globalLiters = _resolver.getGlobalLiterals();
        for (JavaDecl literal : globalLiters)
            if (_prefixMatcher.matchesString(literal.getName()))
                addCompletionDecl(literal);
    }

    /**
     * Adds a single completion for a new variable declaration using the type name.
     */
    private void addCompletionsForNewVarDeclType(JavaType varDeclEvalType)
    {
        // Get type from var decl
        if (varDeclEvalType == null || varDeclEvalType.isPrimitive() || varDeclEvalType.isArray() && varDeclEvalType.getComponentType().isPrimitive())
            return;

        // Get suggested var name from type name
        String typeName = varDeclEvalType.getSimpleName();
        String varName = StringUtils.firstCharLowerCase(typeName);

        // If prefixMatcher matches suggested var name, add type name
        if (_prefixMatcher.matchesString(varName)) {
            JavaDecl nameDecl = new JavaWord(varName, JavaWord.WordType.Unknown);
            addCompletionDecl(nameDecl);
        }

        // If Swing class with "J" prefix, create/add suggestion for name without "J"
        if (varName.length() > 1 && varName.charAt(0) == 'j' && Character.isUpperCase(varName.charAt(1))) {
            String varNameStripJ = StringUtils.firstCharLowerCase(varName.substring(1));
            if (_prefixMatcher.matchesString(varNameStripJ)) {
                JavaDecl nameDecl = new JavaWord(varNameStripJ, JavaWord.WordType.Unknown);
                addCompletionDecl(nameDecl);
            }
        }

        // If '_' prefix, create/add suggestion for type name with '_' prefix
        if (_prefixMatcher.matchesString('_' + varName)) {
            JavaDecl nameDecl = new JavaWord('_' + varName, JavaWord.WordType.Unknown);
            addCompletionDecl(nameDecl);
        }
    }

    /**
     * Adds completions for body decl id.
     */
    private void addCompletionsForBodyDeclId(JExprId anId)
    {
        // Get super class
        JClassDecl classDecl = anId.getEnclosingClassDecl();
        JavaClass superClass = classDecl.getSuperClass();

        // Add methods of super classes
        while (superClass != null) {
            JavaMember[] matchingMembers = _prefixMatcher.getMembersForClass(superClass, false);
            addCompletionDecls(matchingMembers);
            superClass = superClass.getSuperClass();
        }

        // If type, add type completions
        if (anId.getParent() instanceof JType) {

            // Add enclosing class inner classes that match prefix
            JavaClass enclosingClass = classDecl.getJavaClass();
            JavaClass[] matchingInnerClasses = _prefixMatcher.getInnerClassesForClass(enclosingClass);
            addCompletionDecls(matchingInnerClasses);

            // Add any classes that match prefix
            addCompletionsForTypeId();

            // Add modifiers and class declaration words
            addWordCompletions(JavaWord.MODIFIERS);
            addWordCompletions(JavaWord.CLASS_WORDS);
        }

        // If id is method decl id and no params defined, add camel case type name (not sure why incomplete field decl parses as method decl)
        if (anId.getParent() instanceof JMethodDecl methodDecl && anId == methodDecl.getId() && methodDecl.getParameters().length == 0)
            addCompletionsForNewVarDeclType(methodDecl.getReturnType().getJavaType());
    }

    /**
     * Adds completion.
     */
    private void addCompletionDecl(JavaDecl aDecl)
    {
        if (aDecl == null) return;
        _completions.add(aDecl);
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
        JavaDecl[] completions = _completions.toArray(new JavaDecl[0]);
        for (int i = completions.length - 1; i >= 0; i--) {
            JavaDecl decl = completions[i];
            if (decl instanceof JavaClass javaClass) {
                if (!javaClass.isPrimitive()) {
                    _completions.remove(decl);
                    JavaConstructor[] constructors = javaClass.getDeclaredConstructors();
                    addCompletionDecls(constructors);
                }
            }
        }
    }

    /**
     * Returns whether id is body decl id.
     */
    public static boolean isBodyDeclId(JExprId idExpr)
    {
        // If parent is method decl id, return true
        JNode parent = idExpr.getParent();
        if (parent instanceof JMethodDecl methodDecl && idExpr == methodDecl.getId())
            return true;

        // If parent is type and its parent is method decl type, return true
        if (parent instanceof JType) {
            if (parent.getParent() instanceof JMethodDecl methodDecl && parent == methodDecl.getReturnType())
                return true;
        }

        // Return not body decl id
        return false;
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
}