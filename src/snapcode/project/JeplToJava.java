package snapcode.project;
import javakit.parse.*;
import javakit.resolver.JavaType;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snapcode.apptools.RunToolUtils;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class generates a Java class from a parsed Jepl file.
 */
public class JeplToJava {

    // The JFile
    private JFile _jfile;

    // The Java version
    private int _javaVersion;

    // The StringBuilder
    private StringBuilder _sb;

    // The current indent
    private String _indent = "";

    /**
     * Constructor.
     */
    public JeplToJava(JFile jFile, int javaVersion)
    {
        _jfile = jFile;
        _sb = new StringBuilder();
        _javaVersion = javaVersion;
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
        if (!importDeclsStr.isEmpty())
            _sb.append('\n');
        _sb.append('\n');

        // Append class
        JClassDecl classDecl = _jfile.getClassDecl();
        if (classDecl != null) {
            try {
                appendClassDecl(classDecl);
            }
            catch (Exception e) { e.printStackTrace(); }
        }

        // Return string
        String javaString = _sb.toString();
        //snap.util.SnapUtils.writeBytes(javaString.getBytes(), "/tmp/" + _jfile.getName() + ".java");
        return javaString;
    }

    /**
     * Appends a class decl.
     */
    private void appendClassDecl(JClassDecl classDecl)
    {
        _sb.append(_indent);

        // Append class modifiers
        JModifiers mods = classDecl.getModifiers();
        appendModifiers(mods);

        // Append class name
        String classTypeString = classDecl.isClass() ? "class" : classDecl.isInterface() ? "interface" : "enum";
        _sb.append(classTypeString).append(' ');
        String className = classDecl.getName();
        _sb.append(className);

        // Append extends
        JType[] extendsTypes = classDecl.getExtendsTypes();
        if (extendsTypes.length > 0) {
            _sb.append(" extends ");
            String extendsTypesStr = ArrayUtils.mapToStringsAndJoin(extendsTypes, JType::getName, ", ");
            _sb.append(extendsTypesStr);
        }

        // Append implements
        JType[] implementsTypes = classDecl.getImplementsTypes();
        if (implementsTypes.length > 0) {
            _sb.append(" implements ");
            String extendsTypesStr = ArrayUtils.mapToStringsAndJoin(implementsTypes, JType::getName, ", ");
            _sb.append(extendsTypesStr);
        }

        // Append class decl body
        _sb.append(" {\n");
        indent();

        // Append all class members
        JBodyDecl[] bodyDecls = classDecl.getBodyDecls();
        Stream.of(bodyDecls).forEach(this::appendBodyDecl);

        // Outdent
        outdent();
        _sb.append(_indent);
        _sb.append("}\n");
    }

    /**
     * Appends a body decl.
     */
    private void appendBodyDecl(JBodyDecl bodyDecl)
    {
        if (bodyDecl instanceof JFieldDecl)
            appendFieldDecl((JFieldDecl) bodyDecl);
        else if (bodyDecl instanceof JMethodDecl)
            appendMethodDecl((JMethodDecl) bodyDecl);
        else if (bodyDecl instanceof JConstrDecl)
            appendConstructorDecl((JConstrDecl) bodyDecl);
        else if (bodyDecl instanceof JInitializerDecl)
            appendInitializerDecl((JInitializerDecl) bodyDecl);
    }

    /**
     * Appends a field decl.
     */
    private void appendFieldDecl(JFieldDecl fieldDecl)
    {
        // Append indent
        _sb.append('\n');
        _sb.append(_indent);

        // Append modifiers
        JModifiers mods = fieldDecl.getModifiers();
        appendModifiers(mods);

        // Append field type
        JType fieldType = fieldDecl.getType();
        String fieldTypeStr = fieldType.getString();
        _sb.append(fieldTypeStr);
        _sb.append(' ');

        // Append field vars
        JVarDecl[] varDecls = fieldDecl.getVarDecls();
        String varDeclsStr = ArrayUtils.mapToStringsAndJoin(varDecls, JVarDecl::getString, ", ");
        _sb.append(varDeclsStr);
        _sb.append(";\n");
    }

    /**
     * Appends a method decl.
     */
    private void appendMethodDecl(JMethodDecl methodDecl)
    {
        // Append indent
        _sb.append('\n');
        _sb.append(_indent);

        // Append modifiers - make all Jepl methods 'public static'
        JModifiers mods = new JModifiers(Modifier.PUBLIC | Modifier.STATIC); // methodDecl.getMods()
        appendModifiers(mods);

        // Append return type
        JType returnType = methodDecl.getReturnType();
        String returnTypeStr = returnType != null ? returnType.getString() : "";
        _sb.append(returnTypeStr);
        _sb.append(' ');

        // Append name
        String methodName = methodDecl.getName();
        _sb.append(methodName);

        // Append parameters
        appendParameters(methodDecl);

        // Append method body
        appendMethodBody(methodDecl);
    }

    /**
     * Appends a constructor decl.
     */
    private void appendConstructorDecl(JConstrDecl constrDecl)
    {
        // Append indent
        _sb.append('\n');
        _sb.append(_indent);

        // Append modifiers - make all Jepl constructors public
        JModifiers mods = new JModifiers(Modifier.PUBLIC); // constrDecl.getMods()
        appendModifiers(mods);

        // Append class name
        String className = constrDecl.getName();
        _sb.append(className);

        // Append parameters
        appendParameters(constrDecl);

        // Append method body
        appendMethodBody(constrDecl);
    }

    /**
     * Appends initializer decl as main method.
     */
    private void appendInitializerDecl(JInitializerDecl initializerDecl)
    {
        _sb.append('\n');

        // If run local, just make main method
        boolean runLocal = RunToolUtils.isRunLocal(_jfile.getSourceFile());
        if (runLocal)
            _sb.append(_indent).append("public static void main(String[] args) throws Exception\n");

        // Make main method with runLater call
        else {
            _sb.append(_indent).append("public static void main(String[] args) throws Exception { ViewUtils.runLater(() -> main2(args)); }\n");
            _sb.append(_indent).append("public static void main2(String[] args) { try { main3(args); } catch(Exception e) { e.printStackTrace(); } }\n");

            // Handle Jepl file - convert initializer to main method
            _sb.append('\n');
            _sb.append(_indent).append("public static void main3(String[] args) throws Exception\n");
        }

        // Append { body }
        _sb.append(_indent).append("{\n");
        appendMethodBody(initializerDecl);
        _sb.append(_indent).append("}\n");
    }

    /**
     * Appends the method body.
     */
    private void appendMethodBody(JMemberDecl memberDecl)
    {
        // Get the block statement as string and add
        JStmtBlock blockStmt = ((WithBlockStmt) memberDecl).getBlock();
        String blockStmtStr = blockStmt.getString();
        _sb.append(blockStmtStr);

        // Fix var decls and statements with missing semicolons
        if (_javaVersion < 9) {
            int sbStart = _sb.length() - blockStmtStr.length();
            int blockStart = blockStmt.getStartCharIndex();
            int offset = sbStart - blockStart;
            fixVarDecls(blockStmt, offset);
        }

        // Append trailing newline
        _sb.append('\n');
    }

    /**
     * This method converts 'var' declarations to actual and adds missing semicolons.
     */
    private void fixVarDecls(JNode aNode, int offset)
    {
        // If var decl, check for 'var' and replace with type if found
        if (aNode instanceof JVarDecl) {
            JVarDecl varDecl = (JVarDecl) aNode;
            JType varDeclType = varDecl.getType();
            if (varDeclType.isVarType()) {
                JavaType type = varDeclType.getEvalType();
                String typeName = type.getFullName().replace('$', '.');
                int typeStart = varDeclType.getStartCharIndex() + offset;
                _sb.replace(typeStart, typeStart + 3, typeName);
            }
        }

        // Otherwise Recurse into children
        else {
            List<JNode> children = aNode.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                JNode child = children.get(i);
                fixVarDecls(child, offset);
            }
        }
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
     * Appends parameters.
     */
    private void appendParameters(JExecutableDecl methodDecl)
    {
        // Append parameters
        _sb.append('(');
        JVarDecl[] varDecls = methodDecl.getParameters();
        String varDeclsStr = ArrayUtils.mapToStringsAndJoin(varDecls, JVarDecl::getString, ", ");
        _sb.append(varDeclsStr);
        _sb.append(")\n");
    }

    /**
     * Indent.
     */
    private void indent()  { _indent += "    "; }

    /**
     * Outdent.
     */
    private void outdent()  { _indent = _indent.substring(0, _indent.length() - 4); }
}
