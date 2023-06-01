/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.parse.*;
import javakit.resolver.*;
import snap.util.SnapUtils;
import snap.util.StringUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * This class handles matching typed chars to JavaDecls.
 */
public class DeclMatcher {

    // The prefix
    private String  _prefix;

    // The Matcher
    private Matcher  _matcher;

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
     * Returns matching root packages.
     */
    public ClassTree.PackageNode[] getPackagesForClassTree(ClassTree aClassTree)
    {
        ClassTree.PackageNode rootPackage = aClassTree.getRootPackage();
        return getMatchingPackages(rootPackage.packages);
    }

    /**
     * Returns matching packages for given package name.
     */
    public ClassTree.PackageNode[] getChildPackagesForClassTreePackageName(ClassTree aClassTree, String aPkgName)
    {
        // Get dir for package name
        ClassTree.PackageNode packageNode = aClassTree.getPackageForName(aPkgName);
        if (packageNode == null)
            return new ClassTree.PackageNode[0];
        return getMatchingPackages(packageNode.packages);
    }

    /**
     * Returns matching classes in given package name.
     */
    public ClassTree.ClassNode[] getClassesForClassTreePackageName(ClassTree aClassTree, String aPkgName)
    {
        ClassTree.PackageNode packageNode = aClassTree.getPackageForName(aPkgName);
        return getMatchingClasses(packageNode.classes);
    }

    /**
     * Returns all matching classes in ClassTree.
     */
    public ClassTree.ClassNode[] getClassesForClassTree(ClassTree aClassTree)
    {
        // If less than 3 letters, return common names for prefix
        ClassTree.ClassNode[] classes = aClassTree.getAllClasses();
        if (getPrefix().length() < 3)
            classes = aClassTree.getCommonClasses();
        return getMatchingClasses(classes);
    }

    /**
     * Returns matching packages in given packages array.
     */
    private ClassTree.PackageNode[] getMatchingPackages(ClassTree.PackageNode[] thePackages)
    {
        Stream<ClassTree.PackageNode> packagesStream = Stream.of(thePackages);
        Stream<ClassTree.PackageNode> matchingPackagesStream = packagesStream.filter(pkg -> matchesString(pkg.simpleName));
        ClassTree.PackageNode[] matchingPackages = matchingPackagesStream.toArray(size -> new ClassTree.PackageNode[size]);
        return matchingPackages;
    }

    /**
     * Returns matching classes in given classes array.
     */
    private ClassTree.ClassNode[] getMatchingClasses(ClassTree.ClassNode[] theClasses)
    {
        Stream<ClassTree.ClassNode> classesStream = Stream.of(theClasses);
        Stream<ClassTree.ClassNode> matchingClassesStream = classesStream.filter(cls -> matchesString(cls.simpleName));
        ClassTree.ClassNode[] matchingClasses = matchingClassesStream.toArray(size -> new ClassTree.ClassNode[size]);
        return matchingClasses;
    }

    /**
     * Returns a compatible method for given name and param types.
     */
    public List<JavaField> getFieldsForClass(JavaClass aClass)
    {
        // Create return list of prefix fields
        List<JavaField> matchingFields = new ArrayList<>();

        // Iterate over classes
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {

            // Get Class fields
            List<JavaField> fields = cls.getDeclaredFields();
            for (JavaField field : fields)
                if (matchesString(field.getName()))
                    matchingFields.add(field);

            // Should iterate over class interfaces, too
        }

        // Return
        return matchingFields;
    }

    /**
     * Returns methods that match given matcher.
     */
    public JavaMethod[] getMethodsForClass(JavaClass aClass)
    {
        Set<JavaMethod> matchingMethods = new HashSet<>();
        getMethodsForClassImpl(aClass, matchingMethods);
        return matchingMethods.toArray(new JavaMethod[0]);
    }

    /**
     * Returns methods that match given matcher.
     */
    private void getMethodsForClassImpl(JavaClass aClass, Set<JavaMethod> matchingMethods)
    {
        // Iterate over super classes
        for (JavaClass cls = aClass; cls != null; cls = cls.getSuperClass()) {

            // Get Class methods
            List<JavaMethod> methods = cls.getDeclaredMethods();
            for (JavaMethod method : methods)
                if (matchesString(method.getName()) && method.getSuper() == null)
                    matchingMethods.add(method);

            // Iterate over class interfaces and recurse
            JavaClass[] interfaces = cls.getInterfaces();
            for (JavaClass interf : interfaces)
                getMethodsForClassImpl(interf, matchingMethods);

            // Help TeaVM: Thinks that interfaces subclass Object
            if (SnapUtils.isTeaVM && cls.isInterface()) {
                JavaClass superClass = cls.getSuperClass();
                if (superClass.getClassName().equals("java.lang.Object"))
                    break;
            }
        }
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
        String regexStr = getSkipCharsRegexForLiteralString(aStr);
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
        StringBuffer regexSB = new StringBuffer().append(char0);

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
