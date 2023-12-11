package snapcode.project;
import javakit.parse.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class generates a Java class with dispatches to JavaShell to process source.
 */
public class JavaWriter {

    // The JFile
    private JFile _jfile;

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
        for (JImportDecl importDecl : importDecls) {
            String importDeclStr = importDecl.getString();
            _sb.append(importDeclStr);
            _sb.append(";\n");
        }

        // Append class
        JClassDecl classDecl = _jfile.getClassDecl();
        appendClassDecl(classDecl);

        // Return string
        return _sb.toString();
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
        _sb.append(className).append(' ');

        // Append extends

        // Append class decl body
        _sb.append("{\n");
        indent();

        // Get class members and append
        List<JMemberDecl> memberDecls = classDecl.getMemberDecls();
        for (JMemberDecl memberDecl : memberDecls)
            appendMemberDecl(memberDecl);

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

        // Append modifiers
        JModifiers mods = methodDecl.getMods();
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
        appendMethodBody(methodDecl, mods, methodName, varDecls);
    }

    /**
     * Appends the method body:
     *    Object[] __args = { param1, param2, ... };
     *    snapcharts.repl.CallHandler.Call("className", "methodName", args);
     */
    private void appendMethodBody(JMethodDecl methodDecl, JModifiers mods, String methodName, List<JVarDecl> varDecls)
    {
        // Append method body open
        _sb.append(_indent).append("{\n");
        indent();

        // Append args array declaration: Object[] args = { param1, param2, ... };
        _sb.append(_indent);
        _sb.append("Object[] __args = { ");
        String varDeclNamesStr = varDecls.stream().map(JVarDecl::getName).collect(Collectors.joining(", "));
        _sb.append(varDeclNamesStr);
        _sb.append(" };\n");

        // Append call to JavaShell
        _sb.append(_indent);
        _sb.append("snapcharts.repl.CallHandler.Call(");

        // Append class name arg
        JClassDecl classDecl = methodDecl.getEnclosingClassDecl();
        String className = classDecl.getName();
        _sb.append("\"").append(className);
        _sb.append("\", ");

        // Append method name arg
        _sb.append("\"").append(methodName);
        _sb.append("\", ");

        // Append 'this' and '__args' args
        _sb.append(mods.isStatic() ? "null, " : "this, ");
        _sb.append("__args);\n");

        // Append method body close
        outdent();
        _sb.append(_indent).append("}\n");
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
