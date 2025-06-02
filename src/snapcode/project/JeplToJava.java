package snapcode.project;
import javakit.parse.*;
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
    public CharSequence getJava()
    {
        // Append package
        JPackageDecl pkgDecl = _jfile.getPackageDecl();
        if (pkgDecl != null) {
            appendNodeString(pkgDecl);
            appendString(";\n");
        }

        // Append imports
        List<JImportDecl> importDecls = _jfile.getImportDecls();
        appendNodeStrings(importDecls, "\n");
        if (!importDecls.isEmpty())
            appendChar('\n');
        appendChar('\n');

        // Append class
        JClassDecl classDecl = _jfile.getClassDecl();
        if (classDecl != null) {
            try {
                appendClassDecl(classDecl);
            }
            catch (Exception e) { e.printStackTrace(); }
        }

        // Debug: Write to file
        snap.util.SnapUtils.writeBytes(_sb.toString().getBytes(), "/tmp/" + _jfile.getName() + ".java");

        // Return
        return _sb;
    }

    /**
     * Appends a class decl.
     */
    private void appendClassDecl(JClassDecl classDecl)
    {
        appendIndent();

        // Append class modifiers
        JModifiers mods = classDecl.getModifiers();
        if (classDecl.getEnclosingClassDecl() != null)
            mods = new JModifiers(Modifier.PUBLIC | Modifier.STATIC);
        appendModifiers(mods);

        // Append class name
        String classTypeString = classDecl.isClass() ? "class" : classDecl.isInterface() ? "interface" :
                classDecl.isEnum() ? "enum" : "record";
        appendString(classTypeString);
        appendChar(' ');
        appendNodeName(classDecl);

        // If record, append parameters
        if (classDecl.isRecord())
            appendParameters(classDecl.getParameters());

        // Append extends
        List<JType> extendsTypes = List.of(classDecl.getExtendsTypes());
        if (!extendsTypes.isEmpty()) {
            appendString(" extends ");
            appendNodeNames(extendsTypes, ", ");
        }

        // Append implements
        List<JType> implementsTypes = List.of(classDecl.getImplementsTypes());
        if (!implementsTypes.isEmpty()) {
            appendString(" implements ");
            appendNodeNames(implementsTypes, ", ");
        }

        // Append class decl body
        appendString(" {\n");
        indent();

        // Handle enum: append constants
        if (classDecl.isEnum()) {
            appendIndent();
            List<JEnumConst> enumConsts = List.of(classDecl.getEnumConstants());
            appendNodeNames(enumConsts, ", ");
        }

        // Append all class members
        JBodyDecl[] bodyDecls = classDecl.getBodyDecls();
        Stream.of(bodyDecls).forEach(this::appendBodyDecl);

        // Outdent
        outdent();
        appendIndent();
        appendString("}\n");
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
        else if (bodyDecl instanceof JClassDecl)
            appendClassDecl((JClassDecl) bodyDecl);
    }

    /**
     * Appends a field decl.
     */
    private void appendFieldDecl(JFieldDecl fieldDecl)
    {
        // Append indent
        appendChar('\n');
        appendIndent();

        // Append modifiers
        JModifiers mods = fieldDecl.getModifiers();
        appendModifiers(mods);

        // Append field type
        JType fieldType = fieldDecl.getType();
        appendNodeString(fieldType);
        appendChar(' ');

        // Append field vars
        List<JVarDecl> varDecls = List.of(fieldDecl.getVarDecls());
        appendNodeStrings(varDecls, ", ");
        appendString(";\n");
    }

    /**
     * Appends a method decl.
     */
    private void appendMethodDecl(JMethodDecl methodDecl)
    {
        // Append indent
        appendChar('\n');
        appendIndent();

        // Append modifiers - make top level Jepl methods 'public static'
        JModifiers mods = methodDecl.getModifiers();
        if (methodDecl.getEnclosingClassDecl().getEnclosingClassDecl() == null)
            mods = new JModifiers(Modifier.PUBLIC | Modifier.STATIC); // methodDecl.getMods()
        appendModifiers(mods);

        // Append return type
        JType returnType = methodDecl.getReturnType();
        if (returnType != null)
            appendNodeString(returnType);
        appendChar(' ');

        // Append name
        appendNodeName(methodDecl);

        // Append parameters
        appendParameters(methodDecl.getParameters());

        // Append method body
        appendMethodBody(methodDecl);
    }

    /**
     * Appends a constructor decl.
     */
    private void appendConstructorDecl(JConstrDecl constrDecl)
    {
        // Append indent
        appendChar('\n');
        appendIndent();

        // Append modifiers - make all Jepl constructors public
        JModifiers mods = new JModifiers(Modifier.PUBLIC); // constrDecl.getMods()
        appendModifiers(mods);

        // Append class name
        appendNodeName(constrDecl);

        // Append parameters
        appendParameters(constrDecl.getParameters());

        // Append method body
        appendMethodBody(constrDecl);
    }

    /**
     * Appends initializer decl as main method.
     */
    private void appendInitializerDecl(JInitializerDecl initializerDecl)
    {
        appendChar('\n');

        // If run local, just make main method
        boolean runLocal = RunToolUtils.isRunLocal(_jfile.getSourceFile());
        if (runLocal) {
            appendIndent();
            appendString("public static void main(String[] args) throws Exception\n");
        }

        // Make main method with runLater call
        else {
            appendIndent();
            appendString("public static void main(String[] args) throws Exception { ViewUtils.runLater(() -> main2(args)); }\n");
            appendIndent();
            appendString("public static void main2(String[] args) { try { main3(args); } catch(Exception e) { e.printStackTrace(); } }\n");

            // Handle Jepl file - convert initializer to main method
            appendChar('\n');
            appendIndent();
            appendString("public static void main3(String[] args) throws Exception\n");
        }

        // Append { body }
        appendIndent();
        appendString("{\n");
        appendMethodBody(initializerDecl);
        appendIndent();
        appendString("}\n");
    }

    /**
     * Appends the method body.
     */
    private void appendMethodBody(JMemberDecl memberDecl)
    {
        // Get the block statement as string and add
        JStmtBlock blockStmt = ((WithBlockStmt) memberDecl).getBlock();
        appendNodeString(blockStmt);

        // Append trailing newline
        appendChar('\n');
    }

    /**
     * Appends modifiers.
     */
    private void appendModifiers(JModifiers modifiers)
    {
        appendNodeString(modifiers);
        appendChar(' ');
    }

    /**
     * Appends parameters.
     */
    private void appendParameters(JVarDecl[] varDecls)
    {
        // Append parameters
        appendChar('(');
        appendNodeStrings(List.of(varDecls), ", ");
        appendString(")\n");
    }

    /**
     * Indent.
     */
    private void indent()  { _indent += "    "; }

    /**
     * Outdent.
     */
    private void outdent()  { _indent = _indent.substring(0, _indent.length() - 4); }

    /**
     * Appends indent.
     */
    private void appendIndent()  { appendString(_indent); }

    /**
     * Append char.
     */
    public void appendChar(char aChar)  { _sb.append(aChar); }

    /**
     * Append String.
     */
    public void appendString(String aString)  { _sb.append(aString); }

    /**
     * Append Node name.
     */
    public void appendNodeName(JNode aNode)  { _sb.append(aNode.getName()); }

    /**
     * Append Node string.
     */
    public void appendNodeString(JNode aNode)  { _sb.append(aNode.getString()); }

    /**
     * Append Node names.
     */
    public void appendNodeNames(List<? extends JNode> nodeList, String aDelimiter)
    {
        String nodeNamesStr = ListUtils.mapToStringsAndJoin(nodeList, JNode::getName, aDelimiter);
        appendString(nodeNamesStr);
    }

    /**
     * Append Node strings.
     */
    public void appendNodeStrings(List<? extends JNode> nodeList, String aDelimiter)
    {
        String nodeStringsStr = ListUtils.mapToStringsAndJoin(nodeList, JNode::getString, aDelimiter);
        appendString(nodeStringsStr);
    }

    /**
     * Append Node with string.
     */
    //public void appendNodeWithString(JNode aNode, String aString)  { _sb.append(aString); }
}
