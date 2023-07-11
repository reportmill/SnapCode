/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.parse.*;
import javakit.resolver.*;
import snap.util.ArrayUtils;
import snap.util.SnapUtils;
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
    public static final String[] COMMON_PACKAGES = { "java.util", "java.lang", "java.io", "snap.view", "java.awt", "javax.swing" };

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

        // If WebVM, limit to common packages for now
        if (SnapUtils.isWebVM) {
            for (String commonPackageName : COMMON_PACKAGES) {
                JavaPackage commonPackage = aResolver.getJavaPackageForName(commonPackageName);
                if (commonPackage == null) // WebVM
                    continue;
                findClassesForPackageDeep(commonPackage, matchingClasses, limit);
                if (matchingClasses.size() >= limit)
                    return matchingClasses.toArray(new JavaClass[0]);
            }
        }

        // Search all packages
        else findClassesForPackageDeep(rootPackage, matchingClasses, limit);

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
        String packageName = aPackageNode.getFullName();
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
            List<JavaField> fields = cls.getDeclaredFields();
            for (JavaField field : fields) {
                if (matchesString(field.getName())) {
                    if (!staticOnly || field.isStatic())
                        matchingFields = ArrayUtils.add(matchingFields, field);
                }
            }

            // Should iterate over class interfaces, too
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
            List<JavaMethod> methods = cls.getDeclaredMethods();
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
     * Finds JVarDecls for given prefix matcher and adds them to given list.
     */
    public List<JVarDecl> getVarDeclsForJNode(JNode aNode, List<JVarDecl> theVariables)
    {
        // Handle JClassDecl
        if (aNode instanceof JClassDecl)
            getVarDeclsForJClassDecl((JClassDecl) aNode, theVariables);

        // Handle JExecutableDecl
        else if (aNode instanceof JExecutableDecl)
            getVarDeclsForJExecutableDecl((JExecutableDecl) aNode, theVariables);

        // Handle JInitializerDecl
        else if (aNode instanceof JInitializerDecl)
            getVarDeclsForJInitializerDecl((JInitializerDecl) aNode, theVariables);

        // Handle JStmtBlock
        else if (aNode instanceof JStmtBlock)
            getVarDeclsForJStmtBlock((JStmtBlock) aNode, theVariables);

        // If Parent, forward on
        JNode parent = aNode.getParent();
        if (parent != null)
            getVarDeclsForJNode(parent, theVariables);

        // Return
        return theVariables;
    }

    /**
     * Returns whether method matches with option for looking for statics.
     */
    private boolean matchesMethod(JavaMethod method, boolean staticOnly)
    {
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
     * Get VarDecls for JClassDecl - search Class fields.
     */
    private void getVarDeclsForJClassDecl(JClassDecl classDecl, List<JVarDecl> varDeclList)
    {
        // Iterate over FieldDecls and see if any contains matching varDecls
        JFieldDecl[] fieldDecls = classDecl.getFieldDecls();
        for (JFieldDecl fieldDecl : fieldDecls) {
            List<JVarDecl> varDecls = fieldDecl.getVarDecls();
            getVarDeclsForJVarDecls(varDecls, varDeclList);
        }
    }

    /**
     * Get VarDecls for JExecutableDecl - search method/constructor params.
     */
    private void getVarDeclsForJExecutableDecl(JExecutableDecl executableDecl, List<JVarDecl> varDeclList)
    {
        // Get Executable.Parameters and search
        List<JVarDecl> params = executableDecl.getParameters();
        getVarDeclsForJVarDecls(params, varDeclList);

        // REPL hack - find initializer before this method and run for its block
        JClassDecl classDecl = executableDecl.getEnclosingClassDecl();
        JInitializerDecl[] initDecls = classDecl.getInitDecls();

        // REPL hack - find initializer before this method and run for its block
        for (JInitializerDecl initDecl : initDecls) {
            if (initDecl.getStartCharIndex() < executableDecl.getStartCharIndex()) {
                JStmtBlock blockStmt = initDecl.getBlock();
                getVarDeclsForJStmtBlock(blockStmt, varDeclList);
            }
            else break;
        }
    }

    /**
     * Get VarDecls for JInitializerDecl - REPL hack to check prior JInitDecls for VarDecl matching node name.
     */
    private void getVarDeclsForJInitializerDecl(JInitializerDecl anInitDecl, List<JVarDecl> varDeclList)
    {
        // Get enclosing class initDecls
        JClassDecl classDecl = anInitDecl.getEnclosingClassDecl();
        JInitializerDecl[] initDecls = classDecl.getInitDecls();

        // Iterate over initDecls
        for (JInitializerDecl initDecl : initDecls) {

            // Stop when we hit given InitDecl
            if (initDecl == anInitDecl)
                break;

            // Get InitDecl.Block and search
            JStmtBlock initDeclBlock = initDecl.getBlock();
            getVarDeclsForJStmtBlock(initDeclBlock, varDeclList);
        }
    }

    /**
     * Get VarDecls for JStmtBlock.
     */
    private void getVarDeclsForJStmtBlock(JStmtBlock blockStmt, List<JVarDecl> varDeclList)
    {
        // Get statements and search
        List<JStmt> statements = blockStmt.getStatements();

        // Iterate over statements and see if any JStmtVarDecl contains variable with that name
        for (JStmt stmt : statements) {

            // Skip non-VarDecl statements
            if (!(stmt instanceof JStmtVarDecl))
                continue;

            // Get varDeclStmt.VarDecls
            JStmtVarDecl varDeclStmt = (JStmtVarDecl) stmt;
            List<JVarDecl> varDecls = varDeclStmt.getVarDecls();
            getVarDeclsForJVarDecls(varDecls, varDeclList);
        }
    }

    /**
     * Get VarDecls for JVarDecl list.
     */
    private void getVarDeclsForJVarDecls(List<JVarDecl> varDecls, List<JVarDecl> varDeclList)
    {
        for (JVarDecl varDecl : varDecls)
            if (matchesString(varDecl.getName()))
                varDeclList.add(varDecl);
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
}
