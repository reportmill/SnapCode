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
    private String _prefix;

    // The calling class
    private JavaClass _callingClass;

    // The Matcher
    private Matcher _matcher;

    // Constant for preferred packages
    public static final String[] COMMON_PACKAGES = { "java.lang", "java.util", "java.io", "snap.view", "snap.geom", "snap.gfx" };

    // Constant for empty members
    private static final int MATCH_LIMIT = 20;

    /**
     * Constructor.
     */
    public DeclMatcher(String aPrefix, JavaClass callingClass)
    {
        _prefix = aPrefix;
        _callingClass = callingClass;

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
     * Returns all matching classes.
     */
    public JavaClass[] getClassesForResolver(Resolver aResolver)
    {
        // Create list
        List<JavaClass> matchingClasses = new ArrayList<>(MATCH_LIMIT);

        // Search root package
        JavaPackage rootPackage = aResolver.getJavaPackageForName("");
        findClassesForPackage(rootPackage, matchingClasses, MATCH_LIMIT);

        // Search COMMON_PACKAGES
        for (String commonPackageName : COMMON_PACKAGES) {
            JavaPackage commonPackage = aResolver.getJavaPackageForName(commonPackageName);
            findClassesForPackage(commonPackage, matchingClasses, MATCH_LIMIT);
            if (matchingClasses.size() >= MATCH_LIMIT)
                return matchingClasses.toArray(new JavaClass[0]);
        }

        // Search all packages
        findClassesForPackageDeep(rootPackage, matchingClasses, MATCH_LIMIT);

        // Return
        return matchingClasses.toArray(new JavaClass[0]);
    }

    /**
     * Find classes in package.
     */
    private void findClassesForPackage(JavaPackage packageNode, List<JavaClass> matchingClasses, int limit)
    {
        // Get all package classes
        JavaClass[] classes = packageNode.getClasses();

        // Iterate over classes and add matching public classes to list
        for (JavaClass cls : classes) {

            // If class matches, add to list
            if (matchesClass(cls)) {
                matchingClasses.add(cls);
                if (matchingClasses.size() >= limit)
                    return;
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
        if (!ArrayUtils.contains(COMMON_PACKAGES, packageName) && !packageName.isEmpty()) {
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
     * Returns matching members (fields, methods) for given class.
     */
    public JavaMember[] getMembersForClass(JavaClass aClass, boolean staticOnly)
    {
        Set<JavaMember> matchingMembers = new HashSet<>();
        findFieldsForClass(aClass, staticOnly, matchingMembers);
        findMethodsForClass(aClass, staticOnly, matchingMembers);
        return matchingMembers.toArray(new JavaMember[0]);
    }

    /**
     * Returns matching fields for given class (with option for static only).
     */
    private void findFieldsForClass(JavaClass aClass, boolean staticOnly, Set<JavaMember> matchingFields)
    {
        // Add declared fields for class
        JavaField[] fields = aClass.getDeclaredFields();
        for (JavaField field : fields) {
            if (matchesField(field, staticOnly))
                matchingFields.add(field);
        }

        // Add fields for super classes
        JavaClass superClass = aClass.getSuperClass();
        if (superClass != null)
            findFieldsForClass(superClass, staticOnly, matchingFields);

        // Add fields for interfaces
        JavaClass[] interfaces = aClass.getInterfaces();
        for (JavaClass intf : interfaces)
            findFieldsForClass(intf, staticOnly, matchingFields);
    }

    /**
     * Returns methods that match given matcher.
     */
    private void findMethodsForClass(JavaClass aClass, boolean staticOnly, Set<JavaMember> matchingMethods)
    {
        // Add declared methods for class
        JavaMethod[] methods = aClass.getDeclaredMethods();
        for (JavaMethod method : methods) {
            if (matchesMethod(method, staticOnly)) {
                if (!isOverrideAlreadyAdded(matchingMethods, method))
                    matchingMethods.add(method);
            }
        }

        // Add methods for super classes
        JavaClass superClass = aClass.getSuperClass();
        if (superClass != null)
            findMethodsForClass(superClass, staticOnly, matchingMethods);

        // Iterate over class interfaces and recurse
        JavaClass[] interfaces = aClass.getInterfaces();
        for (JavaClass interf : interfaces)
            findMethodsForClass(interf, staticOnly, matchingMethods);
    }

    /**
     * Returns inner classes that match given matcher.
     */
    public JavaClass[] getInnerClassesForClass(JavaClass aClass)
    {
        Set<JavaClass> matchingClasses = new HashSet<>();

        // Iterate over super classes
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {

            // Get inner classes
            JavaClass[] innerClasses = cls.getDeclaredClasses();
            for (JavaClass innerClass : innerClasses) {
                if (matchesClass(innerClass))
                    matchingClasses.add(innerClass);
            }
        }

        // Return array
        return matchingClasses.toArray(new JavaClass[0]);
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
        JVarDecl[] varDecls = withVarDecls.getVarDecls();

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
        JInitializerDecl[] initializerDecls = classDecl.getInitializerDecls();

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
     * Returns whether this matcher matches given string.
     */
    public boolean matchesString(String aString)
    {
        return _matcher.reset(aString).lookingAt();
    }

    /**
     * Returns whether this matcher matches given class.
     */
    private boolean matchesClass(JavaClass aClass)
    {
        // If class not accessible, return false
        if (!isAccessibleModifiers(aClass.getModifiers(), aClass))
            return false;

        // If name doesn't match, return false
        if (!matchesString(aClass.getSimpleName()))
            return false;

        // Return matches
        return true;
    }

    /**
     * Returns whether field matches with option for looking for statics.
     */
    private boolean matchesField(JavaField field, boolean staticOnly)
    {
        // If field not accessible, just return
        if (!isAccessibleMember(field))
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
        // If method not accessible, just return
        if (!isAccessibleMember(method))
            return false;

        // If name doesn't match, return false
        if (!matchesString(method.getName()))
            return false;

        // If StaticOnly, return if static
        if (staticOnly)
            return method.isStatic();

        // Return matches
        return true;
    }

    /**
     * Returns whether given member is accessible.
     */
    private boolean isAccessibleMember(JavaMember member)
    {
        int modifiers = member.getModifiers();
        JavaClass declaringClass = member.getDeclaringClass();
        return isAccessibleModifiers(modifiers, declaringClass);
    }

    /**
     * Returns whether given member modifiers are accessible for given declaring class and matcher's calling class.
     */
    private boolean isAccessibleModifiers(int modifiers, JavaClass declaringClass)
    {
        // If public, return true
        if (Modifier.isPublic(modifiers))
            return true;

        // If calling class unknown, return false
        if (_callingClass == null)
            return false;

        // If private, return whether declaring class and calling class match
        if (Modifier.isPrivate(modifiers))
            return declaringClass == _callingClass;

        // Since protected or package private, return true if packages match
        if (_callingClass.getPackage() == declaringClass.getPackage())
            return true;

        // If protected, return true if subclass
        if (Modifier.isProtected(modifiers)) {
            for (JavaClass cls = _callingClass.getSuperClass(); cls != null; cls = cls.getSuperClass())
                if (cls == declaringClass)
                    return true;
        }

        // Return not accessible
        return false;
    }

    /**
     * Returns whether an override method has already been added to given list.
     */
    private static boolean isOverrideAlreadyAdded(Set<JavaMember> memberSet, JavaMethod newMethod)
    {
        // Iterate over methods in set and return true if any method overrides given method
        for (JavaMember member : memberSet) {
            if (member instanceof JavaMethod) {
                JavaMethod method = (JavaMethod) member;
                for (JavaMethod superMethod = method.getSuper(); superMethod != null; superMethod = superMethod.getSuper()) {
                    if (newMethod == superMethod)
                        return true;
                }
            }
        }

        // Return method has no override
        return false;
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
        String regexStr = !aStr.isEmpty() ? getSkipCharsRegexForLiteralString(aStr) : "";
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
}
