package snapcode.util;
import snap.util.CharSequenceUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * This class parses a Markdown file and returns a tree of nodes. Supported syntax is:
 *     # My Main Header                   |  Header1
 *     ## My Subheader                    |  Header2
 *     * My list item 1                   |  ListItem (reads till next ListItem or blank line)
 *     [My Link Text](http:...)           |  Link
 *     ![My Image Text](http:...)         |  Image
 *     @ [ Header1:CustomName]            |  Directive - to redefine rendering
 *     ``` My Code ```                    |  CodeBlock
 *     ---                                |  Separator (maybe soon)
 *     > My block quote                   |  BlockQuote (maybe soon)
 */
public class MDParser {

    // The input
    private CharSequence _input;

    // The char index
    private int _charIndex;

    // Constants
    public static final String LIST_ITEM_MARKER = "* ";
    public static final String LINK_MARKER = "[";
    public static final String IMAGE_MARKER = "![";
    public static final String DIRECTIVE_MARKER = "@[";
    public static final String CODE_BLOCK_MARKER = "```";

    /**
     * Constructor.
     */
    public MDParser()
    {
        super();
    }

    /**
     * Parses given text string and returns node.
     */
    public MDNode parseMarkdownChars(CharSequence theChars)
    {
        // Set input chars
        _input = theChars;
        _charIndex = 0;

        // Create rootNode and child nodes
        MDNode rootNode = new MDNode(MDNode.NodeType.Root, null);
        List<MDNode> childNodes = new ArrayList<>();

        // Read nodes
        while (hasChars()) {
            MDNode nextNode = parseNextNode();
            if (nextNode != null)
                childNodes.add(nextNode);
        }

        // Set child nodes
        rootNode.setChildNodes(childNodes.toArray(new MDNode[0]));

        // Return
        return rootNode;
    }

    /**
     * Parses the next node.
     */
    private MDNode parseNextNode()
    {
        // Skip white space - just return if no more chars
        skipWhiteSpace();
        if (!hasChars())
            return null;

        // Handle headers
        if (nextCharsStartWith("#"))
            return parseHeaderNode();

        // Handle link
        if (nextCharsStartWith(LINK_MARKER))
            return parseLinkNode();

        // Handle image
        if (nextCharsStartWith(IMAGE_MARKER))
            return parseImageNode();

        // Handle code block
        if (nextCharsStartWith(CODE_BLOCK_MARKER))
            return parseCodeBlockNode();

        // Handle list item
        if (nextCharsStartWith(LIST_ITEM_MARKER))
            return parseListNode();

        // Handle directive
        if (nextCharsStartWith(DIRECTIVE_MARKER))
            return parseDirectiveNode();

        // Return text node
        return parseTextNode();
    }

    /**
     * Parses a Header node.
     */
    private MDNode parseHeaderNode()
    {
        // Get header level by counting hash chars
        int headerLevel = 0;
        while (hasChars() && nextChar() == '#') {
            headerLevel++;
            eatChar();
        }

        // Get Header level NodeType and chars till line end
        MDNode.NodeType nodeType = headerLevel == 1 ? MDNode.NodeType.Header1 : MDNode.NodeType.Header2;
        String charsTillLineEnd = getCharsTillLineEnd().toString().trim();

        // Return header node
        return new MDNode(nodeType, charsTillLineEnd);
    }

    /**
     * Parses a text node.
     */
    private MDNode parseTextNode()
    {
        String charsTillLineEnd = getCharsTillLineEnd().toString().trim();

        while (true) {
            String moreChars = getCharsTillLineEnd().toString().trim();
            if (moreChars.length() == 0)
                break;
            charsTillLineEnd += ' ' + moreChars;
        }

        // Return text node
        return new MDNode(MDNode.NodeType.Text, charsTillLineEnd);
    }

    /**
     * Parses a code block node.
     */
    private MDNode parseCodeBlockNode()
    {
        eatChars(CODE_BLOCK_MARKER.length());
        String charsTillBlockEnd = getCharsTillMatchingTerminator(CODE_BLOCK_MARKER).toString();
        return new MDNode(MDNode.NodeType.CodeBlock, charsTillBlockEnd);
    }

    /**
     * Parses a list node.
     */
    private MDNode parseListNode()
    {
        // Create list node and listItemNodes
        MDNode listNode = new MDNode(MDNode.NodeType.Text, null);
        List<MDNode> listItemNodes = new ArrayList<>();

        // Parse available list items and add to listItemNodes
        while (nextCharsStartWith(LIST_ITEM_MARKER)) {
            MDNode listItemNode = parseListItemNode();
            listItemNodes.add(listItemNode);
        }

        // Add listItemsNodes to list node and return
        listNode.setChildNodes(listItemNodes.toArray(new MDNode[0]));
        return listNode;
    }

    /**
     * Parses a list item node. These can contain
     */
    private MDNode parseListItemNode()
    {
        // Eat identifier chars
        eatChars(LIST_ITEM_MARKER.length());

        // Get all chars
        StringBuilder allChars = new StringBuilder();

        // Get chars till next list item or empty line
        while (true) {
            CharSequence lineChars = getCharsTillLineEnd();
            allChars.append(lineChars);
            if (nextCharsStartWith(LIST_ITEM_MARKER))
                break;
            if (isAtEmptyLine())
                break;
            allChars.append(' ');
        }

        // Create ListItem node and return
        return new MDNode(MDNode.NodeType.ListItem, allChars.toString());
    }

    /**
     * Parses a link node.
     */
    private MDNode parseLinkNode()
    {
        // Eat marker chars
        eatChars(LINK_MARKER.length());

        // Get chars till link close
        String linkText = getCharsTillMatchingTerminator("]").toString().trim();

        // Create link node
        MDNode linkNode = new MDNode(MDNode.NodeType.Link, linkText);

        // Look for url marker
        skipWhiteSpace();
        if (nextCharsStartWith("(")) {
            String urlText = getCharsTillMatchingTerminator(")").toString().trim();
            linkNode.setOtherText(urlText);
        }

        // Return
        return linkNode;
    }

    /**
     * Parses an image node.
     */
    private MDNode parseImageNode()
    {
        // Eat first char
        eatChar();

        // Parse link node and reassign NodeType to Image
        MDNode imageNode = parseLinkNode();
        imageNode._nodeType = MDNode.NodeType.Image;

        // Return
        return imageNode;
    }

    /**
     * Parses a directive node.
     */
    private MDNode parseDirectiveNode()
    {
        // Eat marker chars
        eatChars(DIRECTIVE_MARKER.length());

        // Get chars till link close
        String directiveText = getCharsTillMatchingTerminator("]").toString().trim();

        // Create directive node
        MDNode directiveNode = new MDNode(MDNode.NodeType.Link, directiveText);

        // Return
        return directiveNode;
    }

    /**
     * Returns the char at given char index.
     */
    private char charAt(int charIndex)  { return _input.charAt(charIndex); }

    /**
     * Returns whether there are more chars.
     */
    private boolean hasChars()  { return _charIndex < _input.length(); }

    /**
     * Returns the next char.
     */
    private char nextChar()  { return _input.charAt(_charIndex); }

    /**
     * Advances charIndex by one.
     */
    private void eatChar()  { _charIndex++; }

    /**
     * Advances charIndex by given char count.
     */
    private void eatChars(int charCount)  { _charIndex += charCount; }

    /**
     * Returns whether next chars start with given string.
     */
    private boolean nextCharsStartWith(CharSequence startChars)
    {
        // If not enough chars, return false
        int charsLeft = _input.length() - _charIndex;
        if (charsLeft < startChars.length())
            return false;

        // Iterate over startChars and return false if any don't match nextChars
        for (int charIndex = 0; charIndex < startChars.length(); charIndex++) {
            if (startChars.charAt(charIndex) != _input.charAt(_charIndex + charIndex))
                return false;
        }

        // Return true
        return true;
    }

    /**
     * Skips whitespace.
     */
    private void skipWhiteSpace()
    {
        while (_charIndex < _input.length() && Character.isWhitespace(nextChar()))
            _charIndex++;
    }

    /**
     * Returns the chars till line end.
     */
    private CharSequence getCharsTillLineEnd()
    {
        // Get startCharIndex and eatChars till line end or text end
        int startCharIndex = _charIndex;
        while (hasChars() && !CharSequenceUtils.isLineEndChar(nextChar()))
            eatChar();

        // Get endCharIndex and eatChar for line end
        int endCharIndex = _charIndex;
        if (hasChars())
            eatChar();

        // Return chars
        return getCharsForCharRange(startCharIndex, endCharIndex);
    }

    /**
     * Returns the chars till matching terminator.
     */
    private CharSequence getCharsTillMatchingTerminator(CharSequence endChars)
    {
        // If leading newline, just skip it
        if (hasChars() && CharSequenceUtils.isLineEndChar(nextChar()))
            eatChar();

        // Get startCharIndex and eatChars till matching chars or text end
        int startCharIndex = _charIndex;
        while (hasChars() && !nextCharsStartWith(endChars))
            eatChar();

        // Get endCharIndex and eatChars for matching chars
        int endCharIndex = _charIndex;
        if (CharSequenceUtils.isLineEndChar(charAt(endCharIndex - 1)))
            endCharIndex--;

        // Get endCharIndex and eatChars for matching chars
        if (hasChars())
            eatChars(endChars.length());

        // Return chars
        return getCharsForCharRange(startCharIndex, endCharIndex);
    }

    /**
     * Returns chars for char range.
     */
    private CharSequence getCharsForCharRange(int startCharIndex, int endCharIndex)
    {
        return _input.subSequence(startCharIndex, endCharIndex);
    }

    /**
     * Returns the length of leading whitespace chars for given char sequence.
     */
    private boolean isAtEmptyLine()
    {
        // Get leading space chars
        for (int i = 0; i < _input.length(); i++) {
            char loopChar = _input.charAt(i);
            if (!Character.isWhitespace(loopChar))
                return false;
            if (loopChar == '\n')
                break;
        }

        // Return
        return true;
    }
}
