/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.parse.*;
import javakit.resolver.*;
import snap.util.ArrayUtils;
import snap.util.StringUtils;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class handles matching typed chars to JavaDecls.
 */
public class DeclMatcher {

    // The prefix
    private String  _prefix;

    // The Matcher
    private Matcher  _matcher;

    // Constant for preferred packages
    public static final String[] COMMON_PACKAGES = { "java.util", "java.lang", "java.io", "snap.view", "snap.gfx", "snap.geom", "snap.util" };

    // Constant for empty members
    private static final JavaField[] EMPTY_FIELDS_ARRAY = new JavaField[0];
    private static final JavaMember[] EMPTY_MEMBERS_ARRAY = new JavaMember[0];

    /**
     * Constructor.
     */
    public DeclMatcher(String aPrefix)
    {
        _prefix = aPrefix;
        _matcher = getSkipCharsMatcherForLiteralString(_prefix);
    }

    /**
     * Returns the prefix string.
     */
    public String getPrefix()  { return _prefix; }

    /**
     * Returns the matcher.
     */
    public Matcher getMatcher()  { return _matcher; }

    /**
     * Returns whether this matcher matches given string.
     */
    public boolean matchesString(String aString)
    {
        return _matcher.reset(aString).lookingAt();
    }

    /**
     * Returns all matching classes.
     */
    public JavaClass[] getClassesForResolver(Resolver aResolver)
    {
        // Create list
        List<JavaClass> matchingClasses = new ArrayList<>();
        int limit = 20;

        // Search root package
        JavaPackage rootPackage = aResolver.getJavaPackageForName("");
        findClassesForPackage(rootPackage, matchingClasses, limit);

        // Search COMMON_PACKAGES
        for (String commonPackageName : COMMON_PACKAGES) {
            JavaPackage commonPackage = aResolver.getJavaPackageForName(commonPackageName);
            if (commonPackage == null) // WebVM
                continue;
            findClassesForPackage(commonPackage, matchingClasses, limit);
            if (matchingClasses.size() >= limit)
                return matchingClasses.toArray(new JavaClass[0]);
        }

        // Search all packages
        findClassesForPackageDeep(rootPackage, matchingClasses, limit);

        // Return
        return matchingClasses.toArray(new JavaClass[0]);
    }

    /**
     * Find classes in package.
     */
    private void findClassesForPackage(JavaPackage packageNode, List<JavaClass> matchingClasses, int limit)
    {
        // Get all package classes
        JavaClass[] classNodes = packageNode.getClasses();

        // Iterate over classes and add matching public classes to list
        for (JavaClass classNode : classNodes) {

            // If class name matches and is public add to list
            if (matchesString(classNode.getSimpleName())) {
                if (Modifier.isPublic(classNode.getModifiers())) {
                    matchingClasses.add(classNode);
                    if (matchingClasses.size() >= limit)
                        return;
                }
            }
        }
    }

    /**
     * Find classes in package.
     */
    private void findClassesForPackageDeep(JavaPackage aPackageNode, List<JavaClass> matchingClasses, int limit)
    {
        // If not common or root package, check package classes
        String packageName = aPackageNode.getName();
        if (!ArrayUtils.contains(COMMON_PACKAGES, packageName) && packageName.length() > 0) {
            findClassesForPackage(aPackageNode, matchingClasses, limit);
            if (matchingClasses.size() >= limit)
                return;
        }

        // Get child packages
        JavaPackage[] childPackages = aPackageNode.getPackages();

        // Iterate over child packages and look for matches
        for (JavaPackage childPackage : childPackages) {

            // Recurse (return if limit was hit)
            findClassesForPackageDeep(childPackage, matchingClasses, limit);
            if (matchingClasses.size() >= limit)
                return;
        }
    }

    /**
     * Returns matching fields for given class (with option for static only).
     */
    public JavaField[] getFieldsForClass(JavaClass aClass, boolean staticOnly)
    {
        // Create return list of prefix fields
        JavaField[] matchingFields = EMPTY_FIELDS_ARRAY;

        // Iterate over classes
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {

            // Get Class fields
            JavaField[] fields = cls.getDeclaredFields();
            for (JavaField field : fields) {
                if (matchesField(field, staticOnly))
                    matchingFields = ArrayUtils.add(matchingFields, field);
            }
        }

        // Return
        return matchingFields;
    }

    /**
     * Returns methods that match given matcher.
     */
    public JavaMethod[] getMethodsForClass(JavaClass aClass, boolean staticOnly)
    {
        Set<JavaMethod> matchingMethods = new HashSet<>();
        findMethodsForClass(aClass, staticOnly, matchingMethods);
        return matchingMethods.toArray(new JavaMethod[0]);
    }

    /**
     * Returns methods that match given matcher.
     */
    private void findMethodsForClass(JavaClass aClass, boolean staticOnly, Set<JavaMethod> matchingMethods)
    {
        // Iterate over super classes
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {

            // Get Class methods
            JavaMethod[] methods = cls.getDeclaredMethods();
            for (JavaMethod method : methods) {
                if (matchesMethod(method, staticOnly))
                    matchingMethods.add(method);
            }

            // Iterate over class interfaces and recurse
            JavaClass[] interfaces = cls.getInterfaces();
            for (JavaClass interf : interfaces)
                findMethodsForClass(interf, staticOnly, matchingMethods);
        }
    }

    /**
     * Returns matching members (fields, methods) for given class.
     */
    public JavaMember[] getMembersForClass(JavaClass aClass, boolean staticOnly)
    {
        JavaMember[] matchingMembers = EMPTY_MEMBERS_ARRAY;

        // Look for matching fields
        JavaField[] matchingFields = getFieldsForClass(aClass, staticOnly);
        if (matchingFields.length > 0)
            matchingMembers = ArrayUtils.addAll(matchingMembers, matchingFields);

        // Add matching methods
        JavaMember[] matchingMethods = getMethodsForClass(aClass, staticOnly);
        if (matchingMethods.length > 0)
            matchingMembers = ArrayUtils.addAll(matchingMembers, matchingMethods);

        // Return
        return matchingMembers;
    }

    /**
     * Returns whether field matches with option for looking for statics.
     */
    private boolean matchesField(JavaField field, boolean staticOnly)
    {
        // If not public, just return - need to eventually handle protected/private
        if (!field.isPublic())
            return false;

        // If name doesn't match, return false
        if (!matchesString(field.getName()))
            return false;

        // If StaticOnly, return if static
        if (staticOnly)
            return field.isStatic();

        // Return matches
        return true;
    }

    /**
     * Returns whether method matches with option for looking for statics.
     */
    private boolean matchesMethod(JavaMethod method, boolean staticOnly)
    {
        // If not public, just return - need to eventually handle protected/private
        if (!method.isPublic())
            return false;

        // If name doesn't match, return false
        if (!matchesString(method.getName()))
            return false;

        // If StaticOnly, return if static
        if (staticOnly)
            return method.isStatic();

        // If super exists, return false (will find super version when searching super class)
        if (method.getSuper() != null)
            return false;

        // Return matches
        return true;
    }

    /**
     * Returns matching JavaMembers for given static imports.
     */
    public JavaMember[] getMembersForStaticImports(JImportDecl[] staticImportDecls)
    {
        JavaMember[] matchingMembers = new JavaMember[0];

        // Iterate over static imports and add matching members for classes
        for (JImportDecl staticImportDecl : staticImportDecls) {
            JavaClass importClass = staticImportDecl.getEvalClass();
            if (importClass == null)
                continue;
            JavaMember[] matchingMembersForClass = getMembersForClass(importClass, true);
            if (matchingMembersForClass.length > 0)
                matchingMembers = ArrayUtils.addAll(matchingMembers, matchingMembersForClass);
        }

        // Return
        return matchingMembers;
    }

    /**
     * Returns matching VarDecls in parent nodes (i.e., in scope) for given id expression.
     */
    public List<JVarDecl> getVarDeclsForId(JExprId idExpr)
    {
        List<JVarDecl> matchingVarDecls = new ArrayList<>();

        // Iterate up parents to look for VarDecls prior to id expression that match prefix
        for (JNode parentNode = idExpr.getParent(); parentNode != null; parentNode = parentNode.getParent()) {
            if (parentNode instanceof WithVarDecls) // Local vars, method params, class fields, statement params, etc.
                findVarDeclsForIdInWithVarDecls(idExpr, (WithVarDecls) parentNode, matchingVarDecls);
        }

        // ReplHack: If Repl file, add matching var decls from previous initializers
        JFile jFile = idExpr.getFile();
        if (jFile.isRepl()) {
            JClassDecl classDecl = jFile.getClassDecl();
            findVarDeclsForIdReplHack(idExpr, classDecl, matchingVarDecls);
        }

        // Return
        return matchingVarDecls;
    }

    /**
     * Finds VarDecls for given id expression that match prefix and adds them to given list.
     */
    private void findVarDeclsForIdInWithVarDecls(JExprId idExpr, WithVarDecls withVarDecls, List<JVarDecl> matchingVarDecls)
    {
        List<JVarDecl> varDecls = withVarDecls.getVarDecls();

        // Iterate over var decls and add those that match prefix and are declared before id expression
        for (JVarDecl varDecl : varDecls) {

            // If name doesn't match or id expression is before end of var decl, skip
            if (!matchesString(varDecl.getName()))
                continue;
            if (idExpr.getStartCharIndex() < varDecl.getEndCharIndex())
                continue;

            // If WithVarDecls node is ForEach statement and id expression is before iterable expression end, skip
            if (withVarDecls instanceof JStmtFor) {
                JStmtFor forStmt = (JStmtFor) withVarDecls;
                JExpr iterableExpr = forStmt.getIterableExpr();
                if (iterableExpr != null && idExpr.getStartCharIndex() < iterableExpr.getEndCharIndex())
                    continue;
            }

            // Add to list
            matchingVarDecls.add(varDecl);
        }
    }

    /**
     * REPL hack - Find matching var decls from previous initializers and add to given list.
     */
    private void findVarDeclsForIdReplHack(JExprId idExpr, JClassDecl classDecl, List<JVarDecl> matchingVarDecls)
    {
        JInitializerDecl[] initializerDecls = classDecl.getInitDecls();

        // Iterate over initializers to find matching var decls (break at one holding given id)
        for (JInitializerDecl initializerDecl : initializerDecls) {

            // If id expression is before initializer end, just break
            if (idExpr.getStartCharIndex() < initializerDecl.getEndCharIndex())
                break;

            // Find matching var decls in initializer
            JStmtBlock blockStmt = initializerDecl.getBlock();
            findVarDeclsForIdInWithVarDecls(idExpr, blockStmt, matchingVarDecls);
        }
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        String className = getClass().getSimpleName();
        String propStrings = toStringProps();
        return className + " { " + propStrings + " }";
    }

    /**
     * Standard toStringProps implementation.
     */
    public String toStringProps()
    {
        StringBuffer sb = new StringBuffer();
        StringUtils.appendProp(sb, "Prefix", _prefix);
        StringUtils.appendProp(sb, "Pattern", _matcher.pattern().pattern());
        return sb.toString();
    }

    /**
     * Returns a regex Matcher for given literal string that allows for skipping chars before any uppercase chars.
     * For instance, "AL" or "ArrLi" will both match ArrayList with this matcher.
     * Use matcher.reset(str).lookingAt() to check prefix (like string.startWith()).
     */
    private static Matcher getSkipCharsMatcherForLiteralString(String aStr)
    {
        String regexStr = aStr.length() > 0 ? getSkipCharsRegexForLiteralString(aStr) : "";
        int flags = regexStr.length() > aStr.length() ? 0 : Pattern.CASE_INSENSITIVE;
        Pattern pattern = Pattern.compile(regexStr, flags);
        Matcher matcher = pattern.matcher("");
        return matcher;
    }

    /**
     * Returns a regex string for given literal string that allows for skipping chars before any uppercase chars.
     * For instance, "AL" or "ArrLi" will both match ArrayList.
     * "AL" returns "A[^L]*L"
     * "ArrLi" returns "Arr[^L]*Li"
     */
    private static String getSkipCharsRegexForLiteralString(String aStr)
    {
        // Start regex with first char as is
        char char0 = aStr.charAt(0);
        StringBuilder regexSB = new StringBuilder().append(char0);

        // Iterate over successive chars to generate regex
        for (int i = 1; i < aStr.length(); i++) {
            char prefixChar = aStr.charAt(i);

            // Handle upper case: turn 'A' into "[^A]*A"
            if (Character.isUpperCase(prefixChar) || Character.isDigit(prefixChar))
                regexSB.append("[^").append(prefixChar).append("]*").append(prefixChar);

            // Otherwise, just append char
            else regexSB.append(prefixChar);
        }

        // Return string
        return regexSB.toString();
    }

    /**
     * Prime completions for given resolver.
     */
    public static void primeCompletions(JFile jFile)
    {
        Resolver resolver = jFile.getResolver();
        DeclMatcher declMatcher = new DeclMatcher("x");
        JavaClass[] classes = declMatcher.getClassesForResolver(resolver);
        for (JavaClass cls : classes)
            cls.getDeclaredMethods();
    }
}
