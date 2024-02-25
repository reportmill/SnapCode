package snapcode.project;
import javakit.parse.*;
import javakit.resolver.JavaType;
import snap.util.ListUtils;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class generates a Java class from a parsed Jepl file.
 */
public class JeplToJava {

    // The JFile
    private JFile _jfile;

    // The StringBuilder
    private StringBuilder _sb;

    // The current indent
    private String _indent = "";

    /**
     * Constructor.
     */
    public JeplToJava(JFile jFile)
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
        String importDeclsStr = ListUtils.mapToStringsAndJoin(importDecls, JImportDecl::getString, "\n");
        _sb.append(importDeclsStr);
        if (importDeclsStr.length() > 0)
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
        List<JVarDecl> varDecls = fieldDecl.getVarDecls();
        String varDeclsStr = varDecls.stream().map(JVarDecl::getString).collect(Collectors.joining(", "));
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
        JType returnType = methodDecl.getType();
        String returnTypeStr = returnType.getString();
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
     * Appends an initializer decl.
     */
    private void appendInitializerDecl(JInitializerDecl initializerDecl)
    {
        // Handle Jepl file - convert initializer to main method
        _sb.append('\n');
        _sb.append(_indent);
        _sb.append("public static void main(String[] args) throws Exception\n");
        _sb.append("{\n");

        // Append body
        appendMethodBody(initializerDecl);
        _sb.append("}\n");
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
        int sbStart = _sb.length() - blockStmtStr.length();
        int blockStart = blockStmt.getStartCharIndex();
        int offset = sbStart - blockStart;
        fixVarDeclsAndSemicolons(blockStmt, offset);

        // Append trailing newline
        _sb.append('\n');
    }

    /**
     * This method converts 'var' declarations to actual and adds missing semicolons.
     */
    private void fixVarDeclsAndSemicolons(JNode aNode, int offset)
    {
        List<JNode> children = aNode.getChildren();

        // If node is statement with missing semicolon, add it
        if (aNode instanceof JStmt && children.size() > 0) {
            JNode lastChild = children.get(children.size() - 1);
            if (!(lastChild instanceof JStmt)) {
                int lastCharIndex = aNode.getEndCharIndex() - 1 + offset;
                if (_sb.charAt(lastCharIndex) != ';')
                    _sb.insert(lastCharIndex + 1, ';');
            }
        }

        // Recurse into children
        for (int i = children.size() - 1; i >= 0; i--) {
            JNode child = children.get(i);
            fixVarDeclsAndSemicolons(child, offset);
        }

        // Fix var decl with 'var'
        if (aNode instanceof JVarDecl) {
            JVarDecl varDecl = (JVarDecl) aNode;
            JType varDeclType = varDecl.getType();
            if (varDeclType.isVarType()) {
                JavaType type = varDeclType.getEvalType();
                String typeName = type.getFullName();
                int typeStart = varDeclType.getStartCharIndex() + offset;
                _sb.replace(typeStart, typeStart + 3, typeName);
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
        List<JVarDecl> varDecls = methodDecl.getParameters();
        String varDeclsStr = varDecls.stream().map(JVarDecl::getString).collect(Collectors.joining(", "));
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
