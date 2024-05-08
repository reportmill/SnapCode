package snapcode.util;
import snap.util.CharSequenceUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * This class parses a Markdown file and returns a tree of nodes.
 */
public class MDParser {

    // The input
    private CharSequence _input;

    // The char index
    private int _charIndex;

    // Constants
    public static final String CODE_MARKER = "```";

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

        // Handle code block
        if (nextCharsStartWith(CODE_MARKER))
            return parseCodeBlockNode();

        // Handle list item
        if (nextCharsStartWith("* "))
            return parseListItemNode();

        // Handle link
        if (nextCharsStartWith("["))
            return parseLinkNode();

        // Handle image
        if (nextCharsStartWith("!["))
            return parseImageNode();

        // Handle directive
        if (nextCharsStartWith("@["))
            return parseDirectiveNode();

        // Return text node
        return parseTextNode();
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
        return new MDNode(MDNode.NodeType.Content, charsTillLineEnd);
    }

    /**
     * Parses a code block node.
     */
    private MDNode parseCodeBlockNode()
    {
        eatChars(CODE_MARKER.length());
        String charsTillBlockEnd = getCharsTillMatchingTerminator(CODE_MARKER).toString();
        return new MDNode(MDNode.NodeType.Code, charsTillBlockEnd);
    }

    /**
     * Parses a list item node.
     */
    private MDNode parseListItemNode()
    {
        return null;
    }

    /**
     * Parses a link node.
     */
    private MDNode parseLinkNode()
    {
        return null;
    }

    /**
     * Parses an image node.
     */
    private MDNode parseImageNode()
    {
        return null;
    }

    /**
     * Parses a directive node.
     */
    private MDNode parseDirectiveNode()
    {
        return null;
    }
}
