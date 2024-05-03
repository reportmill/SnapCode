package snapcode.javatext;
import javakit.parse.*;
import javakit.resolver.JavaClass;
import snap.text.TextLine;

/**
 * This class his a helper for JavaTextArea with methods for working with nodes.
 */
public class JavaTextAreaNodeHpr {

    // The JavaTextArea
    private JavaTextArea _javaTextArea;

    /**
     * Constructor.
     */
    public JavaTextAreaNodeHpr(JavaTextArea javaTextArea)
    {
        _javaTextArea = javaTextArea;
    }

    /**
     * Returns the selected node's class.
     */
    public JavaClass getSelNodeEvalClass()
    {
        // Get first parent JNode that resolves to class
        JNode selNode = _javaTextArea.getSelNode();
        while (selNode != null && selNode.getEvalClass() == null)
            selNode = selNode.getParent();

        // Return eval class
        return selNode != null ? selNode.getEvalClass() : null;
    }

    /**
     * Insets a node.
     */
    public void insertNode(JNode baseNode, JNode insertNode, int aPos)
    {
        // If base node is file, complain and return
        if (baseNode instanceof JFile) {
            System.out.println("Can't add to file");
            return;
        }

        // If base node is statement expr
        if (baseNode instanceof JStmtExpr && insertNode instanceof JStmtExpr) {

            // Get baseNode class and selNode class
            JavaClass baseNodeClass = baseNode.getEvalClass();
            JavaClass selNodeClass = getSelNodeEvalClass();

            //
            if (baseNodeClass == selNodeClass && !baseNodeClass.getName().equals("void")) {

                // Get insert string and index
                String nodeStr = insertNode.getString();
                String insertStr = '.' + nodeStr;
                int insertCharIndex = baseNode.getEndCharIndex();

                // Replace chars and return
                _javaTextArea.replaceChars(insertStr, insertCharIndex - 1, insertCharIndex);
                _javaTextArea.setSel(insertCharIndex, insertCharIndex + nodeStr.length());
                return;
            }
        }

        // Get insert char index for base node
        int insertCharIndex = aPos < 0 ? getCharIndexBeforeNode(baseNode) : aPos > 0 ? getCharIndexAfterNode(baseNode) : getCharIndexInNode(baseNode);

        // Get string for insert node
        String indentStr = getIndentStringForNode(baseNode, aPos);
        String nodeStr = insertNode.getString().trim().replace("\n", "\n" + indentStr);
        String insertStr = indentStr + nodeStr + '\n';

        // Replace chars
        _javaTextArea.replaceChars(insertStr, insertCharIndex, insertCharIndex);
        _javaTextArea.setSel(insertCharIndex + indentStr.length(), insertCharIndex + indentStr.length() + nodeStr.trim().length());
    }

    /**
     * Replaces a JNode with string.
     */
    public void replaceNodeWithString(JNode aNode, String aString)
    {
        int startCharIndex = aNode.getStartCharIndex();
        int endCharIndex = aNode.getEndCharIndex();
        _javaTextArea.replaceChars(aString, startCharIndex, endCharIndex);
    }

    /**
     * Removes a node.
     */
    public void removeNode(JNode aNode)
    {
        int startCharIndex = getCharIndexBeforeNode(aNode);
        int endCharIndex = getCharIndexAfterNode(aNode);
        _javaTextArea.replaceChars(null, startCharIndex, endCharIndex);
    }

    /**
     * Returns char index before given node.
     */
    public int getCharIndexBeforeNode(JNode aNode)
    {
        int nodeStartCharIndex = aNode.getStartCharIndex();
        JExpr scopeExpr = aNode instanceof JExpr ? ((JExpr) aNode).getScopeExpr() : null;
        if (scopeExpr != null)
            return scopeExpr.getEndCharIndex();

        TextLine textLine = _javaTextArea.getLineForCharIndex(nodeStartCharIndex);
        return textLine.getStartCharIndex();
    }

    /**
     * Returns char index after given node.
     */
    public int getCharIndexAfterNode(JNode aNode)
    {
        int nodeEndCharIndex = aNode.getEndCharIndex();
        JNode nodeParent = aNode.getParent();
        JExprDot dotExpr = nodeParent instanceof JExprDot ? (JExprDot) nodeParent : null;
        if (dotExpr != null)
            return dotExpr.getExpr().getEndCharIndex();

        TextLine textLine = _javaTextArea.getLineForCharIndex(nodeEndCharIndex);
        return textLine.getEndCharIndex();
    }

    /**
     * Returns in the node.
     */
    public int getCharIndexInNode(JNode aNode)
    {
        int nodeStartCharIndex = aNode.getStartCharIndex();
        while (nodeStartCharIndex < _javaTextArea.length() && _javaTextArea.charAt(nodeStartCharIndex) != '{')
            nodeStartCharIndex++;

        TextLine textLine = _javaTextArea.getLineForCharIndex(nodeStartCharIndex);
        return textLine.getEndCharIndex();
    }

    /**
     * Returns the indent.
     */
    private String getIndentStringForNode(JNode aNode, int aPos)
    {
        int nodeStartCharIndex = aNode.getStartCharIndex();
        TextLine textLine = _javaTextArea.getLineForCharIndex(nodeStartCharIndex);
        String indentStr = textLine.getIndentString();
        if (aPos == 0)
            indentStr += "    ";
        return indentStr;
    }
}
