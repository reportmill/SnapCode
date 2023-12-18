package snapcode.project;
import javakit.parse.*;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class generates a Java class with dispatches to JavaShell to process source.
 */
public class JavaWriter {

    // The JFile
    private JFile _jfile;

    // Whether this is repl
    private boolean _isJepl;

    // The StringBuilder
    private StringBuilder _sb;

    // The current indent
    private String _indent = "";

    /**
     * Constructor.
     */
    public JavaWriter(JFile jFile)
    {
        _jfile = jFile;
        _isJepl = _jfile.isRepl();
        _sb = new StringBuilder();
    }

    /**
     * Returns the java text.
     */
    public String getJava()
    {
        // Append package
        JPackageDecl pkgDecl = _jfile.getPackageDecl();
        if (pkgDecl != null) {
            String pkgDeclStr = pkgDecl.getString();
            _sb.append(pkgDeclStr);
            _sb.append(";\n");
        }

        // Append imports
        List<JImportDecl> importDecls = _jfile.getImportDecls();
        String importDeclsStr = ListUtils.mapToStringsAndJoin(importDecls, JImportDecl::getString, "\n");
        _sb.append(importDeclsStr);
        if (importDeclsStr.length() > 0)
            _sb.append('\n');
        _sb.append('\n');

        // Append class
        JClassDecl classDecl = _jfile.getClassDecl();
        if (classDecl != null)
            appendClassDecl(classDecl);

        // Return string
        String javaString = _sb.toString();
        snap.util.SnapUtils.writeBytes(javaString.getBytes(), "/tmp/" + _jfile.getName() + ".java");
        return javaString;
    }

    /**
     * Appends a class decl.
     */
    private void appendClassDecl(JClassDecl classDecl)
    {
        _sb.append(_indent);

        // Append class modifiers
        JModifiers mods = classDecl.getMods();
        appendModifiers(mods);

        // Append class name
        String classTypeString = classDecl.isClass() ? "class" : classDecl.isInterface() ? "interface" : "enum";
        _sb.append(classTypeString).append(' ');
        String className = classDecl.getName();
        _sb.append(className);

        // Append extends
        List<JType> extendsTypes = classDecl.getExtendsTypes();
        if (extendsTypes.size() > 0) {
            _sb.append(" extends ");
            String extendsTypesStr = extendsTypes.stream().map(JType::getName).collect(Collectors.joining(", "));
            _sb.append(extendsTypesStr);
        }

        // Append implements
        List<JType> implementsTypes = classDecl.getImplementsTypes();
        if (implementsTypes.size() > 0) {
            _sb.append(" implements ");
            String extendsTypesStr = extendsTypes.stream().map(JType::getName).collect(Collectors.joining(", "));
            _sb.append(extendsTypesStr);
        }

        // Append class decl body
        _sb.append(" {\n");
        indent();

        // Append all class members
        List<JMemberDecl> memberDecls = classDecl.getMemberDecls();
        memberDecls.forEach(this::appendMemberDecl);

        // Outdent
        outdent();
        _sb.append(_indent);
        _sb.append("}\n");
    }

    /**
     * Appends a member decl.
     */
    private void appendMemberDecl(JMemberDecl memberDecl)
    {
        if (memberDecl instanceof JFieldDecl)
            appendFieldDecl((JFieldDecl) memberDecl);
        else if (memberDecl instanceof JMethodDecl)
            appendMethodDecl((JMethodDecl) memberDecl);
        else if (memberDecl instanceof JConstrDecl)
            appendConstructorDecl((JConstrDecl) memberDecl);
        else if (memberDecl instanceof JInitializerDecl)
            appendInitializerDecl((JInitializerDecl) memberDecl);
    }

    /**
     * Appends a field decl.
     */
    private void appendFieldDecl(JFieldDecl fieldDecl)
    {

    }

    /**
     * Appends a method decl.
     */
    private void appendMethodDecl(JMethodDecl methodDecl)
    {
        // Append indent
        _sb.append('\n');
        _sb.append(_indent);

        // Append modifiers - If Jepl, make all methods 'public static'
        JModifiers mods = methodDecl.getMods();
        if (_isJepl)
            mods = new JModifiers(Modifier.PUBLIC | Modifier.STATIC);
        appendModifiers(mods);

        // Append return type
        JType returnType = methodDecl.getType();
        String returnTypeStr = returnType.getString();
        _sb.append(returnTypeStr);
        _sb.append(' ');

        // Append name
        String methodName = methodDecl.getName();
        _sb.append(methodName);

        // Append parameters
        _sb.append('(');
        List<JVarDecl> varDecls = methodDecl.getParameters();
        String varDeclsStr = varDecls.stream().map(JVarDecl::getString).collect(Collectors.joining(", "));
        _sb.append(varDeclsStr);
        _sb.append(")\n");

        // Append method body
        appendMethodBody(methodDecl, mods, returnTypeStr, methodName, varDecls);
    }

    /**
     * Appends the method body:
     *    Object[] __args = { param1, param2, ... };
     *    [return] snapcharts.repl.CallHandler.Call("className", "methodName", args);
     */
    private void appendMethodBody(JMemberDecl memberDecl, JModifiers mods, String returnTypeStr, String methodName, List<JVarDecl> varDecls)
    {
        // Append method body open
        _sb.append(_indent).append("{\n");
        indent();

        // Append constructor call
        if (memberDecl instanceof JConstrDecl) {
            JConstrDecl constrDecl = (JConstrDecl) memberDecl;
            JStmt[] bodyStmts = constrDecl.getBlockStatements();
            if (bodyStmts.length > 0 && bodyStmts[0] instanceof JStmtConstrCall) {
                JStmtConstrCall constrCall = (JStmtConstrCall) bodyStmts[0];
                String constrCallStr = constrCall.getString();
                _sb.append(_indent);
                _sb.append(constrCallStr).append('\n');
            }
        }

        // Append args array declaration: Object[] args = { param1, param2, ... };
        if (varDecls.size() > 0) {
            _sb.append(_indent);
            _sb.append("Object[] __args = { ");
            String varDeclNamesStr = varDecls.stream().map(JVarDecl::getName).collect(Collectors.joining(", "));
            _sb.append(varDeclNamesStr);
            _sb.append(" };\n");
        }

        // Append call to JavaShell: return (type) snapcharts.repl.CallHandler.Call(
        _sb.append(_indent);
        if (!returnTypeStr.equals("void")) {
            _sb.append("return ");
            if (!returnTypeStr.equals("Object"))
                _sb.append('(').append(returnTypeStr).append(") ");
        }
        _sb.append("snapcharts.repl.CallHandler.Call(");

        // Append class name arg
        JClassDecl classDecl = memberDecl.getEnclosingClassDecl();
        String className = classDecl.getName();
        _sb.append("\"").append(className);
        _sb.append("\", ");

        // Append method name arg
        _sb.append("\"").append(methodName);
        _sb.append("\", ");

        // Append 'this' and '__args' args
        String thisObjectString = mods.isStatic() ? "null" : "this";
        _sb.append(thisObjectString).append(',');
        String argsStr = varDecls.size() > 0 ? " __args" : " null";
        _sb.append(argsStr);
        _sb.append(");\n");

        // Append method body close
        outdent();
        _sb.append(_indent).append("}\n");
    }

    /**
     * Appends a constructor decl.
     */
    private void appendConstructorDecl(JConstrDecl constrDecl)
    {
        // Append indent
        _sb.append('\n');
        _sb.append(_indent);

        // Append modifiers - If Jepl, make all constructors public
        JModifiers mods = constrDecl.getMods();
        if (_isJepl)
            mods = new JModifiers(Modifier.PUBLIC);
        appendModifiers(mods);

        // Append class name
        String className = constrDecl.getName();
        _sb.append(className);

        // Append parameters
        _sb.append('(');
        List<JVarDecl> varDecls = constrDecl.getParameters();
        String varDeclsStr = varDecls.stream().map(JVarDecl::getString).collect(Collectors.joining(", "));
        _sb.append(varDeclsStr);
        _sb.append(")\n");

        // Append method body
        appendMethodBody(constrDecl, mods, "void", "__init", varDecls);
    }

    /**
     * Appends an initializer decl.
     */
    private void appendInitializerDecl(JInitializerDecl initializerDecl)
    {
        // Handle Jepl file
        if (_isJepl) {
            _sb.append('\n');
            _sb.append(_indent);
            _sb.append("public static void main(String[] args)\n");
        }

        // Get initializer id
        JClassDecl classDecl = initializerDecl.getEnclosingClassDecl();
        JInitializerDecl[] initializerDecls = classDecl.getInitDecls();
        int initializerIndex = ArrayUtils.indexOfId(initializerDecls, initializerDecl);
        String initializerId = "__initializer" + initializerIndex;
        JModifiers modifiers = new JModifiers(Modifier.STATIC);

        // Append body
        appendMethodBody(initializerDecl, modifiers, "void", initializerId, Collections.EMPTY_LIST);
    }

    /**
     * Appends modifiers.
     */
    private void appendModifiers(JModifiers modifiers)
    {
        String modsString = modifiers.getString();
        _sb.append(modsString);
        _sb.append(' ');
    }

    /**
     * Indent.
     */
    private void indent()
    {
        _indent += "    ";
    }

    /**
     * Outdent.
     */
    private void outdent()
    {
        _indent = _indent.substring(0, _indent.length() - 4);
    }
}
