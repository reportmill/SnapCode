package snapcode.project;
import javakit.parse.*;
import snap.util.ListUtils;
import snapcode.apptools.RunToolUtils;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class generates a Java class from a parsed Jepl file.
 */
public class JeplToJava {

    // The JFile
    private JFile _jfile;

    // The current indent
    private String _indent = "";

    // The JInitializer to be used as main method
    private JInitializerDecl _mainMethodInitializer;

    // The snippet text
    private JavaText _javaText;

    /**
     * Constructor.
     */
    public JeplToJava(JFile jFile)
    {
        _jfile = jFile;
        _javaText = new JavaText();
    }

    /**
     * Returns the java text.
     */
    public JavaText getJavaText()
    {
        // Start snippet text
        _javaText.addSnippetForNode(_jfile);

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

            // If no main method, set main method initializer
            JMethodDecl mainMethod = classDecl.getMethodDeclForNameAndTypes("main", null);
            if (mainMethod == null) {
                JInitializerDecl[] initializers = classDecl.getInitializerDecls();
                if (initializers.length > 0)
                    _mainMethodInitializer = initializers[initializers.length - 1];
            }

            // Append top level class
            try { appendClassDecl(classDecl); }
            catch (Exception e) { e.printStackTrace(); }
        }

        // Close snippet text
        _javaText.closeText();

        // Debug: Write to file
        //snap.util.SnapUtils.writeBytes(_javaText.toString().getBytes(), "/tmp/" + _jfile.getName() + ".java");

        // Return
        return _javaText;
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

        // Append method signature
        appendMethodSignatureForDecl(methodDecl);

        // Append method body
        appendMethodBody(methodDecl);
    }

    /**
     * Appends method signature.
     */
    private void appendMethodSignatureForDecl(JMethodDecl methodDecl)
    {
        // If main, forward to special version
        if (methodDecl.getName().equals("main")) {
            appendMainMethodSignature();
            return;
        }

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
    }

    /**
     * Appends main method signature.
     */
    private void appendMainMethodSignature()
    {
        // If run local, just make main method
        boolean runLocal = RunToolUtils.isRunLocal(_jfile.getSourceFile());
        if (runLocal)
            appendString("public static void main(String[] args) throws Exception\n");

        // Make main method with runLater call
        else {
            appendString("public static void main(String[] args) throws Exception { ViewUtils.runLater(() -> main2()); }\n");
            appendIndent();
            appendString("public static void main2() { try { main3(); } catch(Exception e) { e.printStackTrace(); } }\n");
            appendIndent();
            appendString("public static void main3() throws Exception\n");
        }
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
        appendIndent();

        // If main initializer, append main method signature, otherwise make it static
        if (initializerDecl == _mainMethodInitializer)
            appendMainMethodSignature();
        else appendString("static ");

        // Append { body }
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
    public void appendChar(char aChar)  { _javaText._sb.append(aChar); }

    /**
     * Append String.
     */
    public void appendString(String aString)  { _javaText._sb.append(aString); }

    /**
     * Append Node name.
     */
    public void appendNodeName(JNode aNode)
    {
        _javaText.addSnippetForNode(aNode);
        _javaText._sb.append(aNode.getName());
    }

    /**
     * Append Node string.
     */
    public void appendNodeString(JNode aNode)
    {
        _javaText.addSnippetForNode(aNode);
        _javaText._sb.append(aNode.getString());
    }

    /**
     * Append Node names.
     */
    public void appendNodeNames(List<? extends JNode> nodeList, String aDelimiter)
    {
        if (!nodeList.isEmpty())
            _javaText.addSnippetForNode(nodeList.get(0));
        String nodeNamesStr = ListUtils.mapToStringsAndJoin(nodeList, JNode::getName, aDelimiter);
        appendString(nodeNamesStr);
    }

    /**
     * Append Node strings.
     */
    public void appendNodeStrings(List<? extends JNode> nodeList, String aDelimiter)
    {
        if (!nodeList.isEmpty())
            _javaText.addSnippetForNode(nodeList.get(0));
        String nodeStringsStr = ListUtils.mapToStringsAndJoin(nodeList, JNode::getString, aDelimiter);
        appendString(nodeStringsStr);
    }

    /**
     * Append Node with string.
     */
    //public void appendNodeWithString(JNode aNode, String aString)  { _sb.append(aString); }

    /**
     * This class holds a node and a snippet of code.
     */
    private static class Snippet {

        // The node
        private JNode _jnode;

        // The start char index in SnippetText
        protected int _startCharIndex;

        // The string builder
        protected StringBuilder _sb;

        /**
         * Constructor.
         */
        public Snippet(JNode jnode, int startCharIndex)
        {
            _jnode = jnode;
            _startCharIndex = startCharIndex;
            _sb = new StringBuilder();
        }

        public JNode node()  { return _jnode; }

        public int length()  { return _sb.length(); }

        public int startCharIndex()  { return _startCharIndex; }

        public int endCharIndex()  { return _startCharIndex + length(); }

        public char charAt(int i)  { return _sb.charAt(i); }
    }

    /**
     * This class holds the Java text for a Jepl file in a form that can map char indexes from Java back to Jepl.
     */
    public static class JavaText implements CharSequence {

        // The list of snippets
        private List<Snippet> _snippets = new ArrayList<>();

        // The current length
        private int _length;

        // The current string builder
        private StringBuilder _sb;

        // The string
        private String _string = "";

        /**
         * Constructor.
         */
        public JavaText()
        {
            super();
            _sb = new StringBuilder();
        }

        /**
         * Returns the Jepl char index for given Java char index.
         */
        public int getJeplCharIndexForJavaCharIndex(int charIndex)
        {
            Snippet snippet = getSnippetForCharIndex(charIndex);
            int snippetCharIndex = charIndex - snippet.startCharIndex();
            return snippet._jnode.getStartCharIndex() + snippetCharIndex;
        }

        /**
         * Returns the Jepl line index for given Jepl char index.
         */
        public int getJeplLineIndexForJeplCharIndex(int jeplCharIndex)
        {
            Snippet snippet = _snippets.get(0);
            JFile jFile = snippet._jnode.getFile();
            JNode jNode = jFile.getNodeForCharIndex(jeplCharIndex);
            return jNode != null ? jNode.getLineIndex() : 0;
        }

        /**
         * CharSequence method.
         */
        @Override
        public int length()  { return _length; }

        /**
         * CharSequence method.
         */
        @Override
        public char charAt(int i)
        {
            Snippet snippet = getSnippetForCharIndex(i);
            return snippet.charAt(i - snippet._startCharIndex);
        }

        /**
         * CharSequence method.
         */
        @Override
        public CharSequence subSequence(int startCharIndex, int endCharIndex)
        {
            // If indexes out of range, complain
            if (startCharIndex > length() || endCharIndex > length())
                throw new IndexOutOfBoundsException();

            // Get string buffer for return chars
            int length = endCharIndex - startCharIndex;
            StringBuilder subSequence = new StringBuilder(length);

            // Get successive snippets and fill string builder to get chars
            while (subSequence.length() < length) {
                Snippet snippet = getSnippetForCharIndex(startCharIndex);
                int copyLength = Math.min(endCharIndex - snippet.startCharIndex(), snippet.length());
                subSequence.append(snippet._sb.subSequence(0, copyLength));
                startCharIndex += copyLength;
            }

            // Return
            return subSequence;
        }

        /**
         * Returns the snippet for given index.
         */
        private Snippet getSnippetForCharIndex(int charIndex)
        {
            for (Snippet snippet : _snippets) {
                if (charIndex < snippet.endCharIndex())
                    return snippet;
            }

            // If index at end, return last
            if (charIndex == length())
                return _snippets.get(_snippets.size() - 1);

            // Throw index out of bounds exception
            throw new IndexOutOfBoundsException();
        }

        /**
         * Adds a snippet for given node.
         */
        private void addSnippetForNode(JNode jnode)
        {
            _length += _sb.length();
            Snippet snippet = new Snippet(jnode, _length);
            _snippets.add(snippet);
            _sb = snippet._sb;
        }

        /**
         * Makes sure the last snippet is processed.
         */
        private void closeText()
        {
            _length += _sb.length();
            _sb = null;
            _string = subSequence(0, length()).toString();
        }

        /**
         * Standard to string.
         */
        @Override
        public String toString()  { return _string; }
    }
}
