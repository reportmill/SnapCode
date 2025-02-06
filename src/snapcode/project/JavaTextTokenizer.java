package snapcode.project;
import javakit.parse.JavaParser;
import snap.gfx.Color;
import snap.parse.*;
import snap.text.TextLine;
import snap.text.TextStyle;
import snap.text.TextToken;
import java.util.ArrayList;
import java.util.List;

/**
 * A Tokenizer subclass to return TextToken.
 */
public class JavaTextTokenizer extends Tokenizer {

    // The current text line
    private TextLine _textLine;

    // Constants for Syntax Coloring
    private static Color COMMENT_COLOR = new Color("#3F7F5F"); //336633
    private static Color RESERVED_WORD_COLOR = new Color("#660033");
    private static Color STRING_LITERAL_COLOR = new Color("#C80000"); // CC0000

    // A code tokenizer
    public static JavaTextTokenizer SHARED = new JavaTextTokenizer();

    /**
     * Constructor.
     */
    public JavaTextTokenizer()
    {
        super();
        enableCodeComments();
        enableSpecialTokens();

        // Set regexes from grammar
        Grammar grammar = JavaParser.getShared().getGrammar();
        setRegexesForGrammar(grammar);
    }

    /**
     * Returns the parse tokens for given tokenizer and text line.
     */
    public synchronized TextToken[] createTokensForTextLine(TextLine aTextLine)
    {
        // Simple case
        if (aTextLine.isWhiteSpace())
            return new TextToken[0];

        // Get tokens in line
        List<TextToken> textTokens = new ArrayList<>();
        try {
            TextToken textToken = getFirstToken(aTextLine);
            while (textToken != null) {
                textTokens.add(textToken);
                textToken = (TextToken) getNextToken();
            }
        }

        // If tokenizer hits invalid chars, just add remaining line chars to bogus token
        catch (Exception e) {
            int tokenStart = getCharIndex();
            int tokenEnd = aTextLine.length();
            TextToken parseToken = createTokenForProps("ERROR", "ERROR", tokenStart, tokenEnd);
            textTokens.add(parseToken);
            System.out.println("JavaTextTokenizer.createTokensForTextLine: Parse error: " + e);
        }

        // Return
        return textTokens.toArray(new TextToken[0]);
    }

    /**
     * Override to create TextTokens.
     */
    @Override
    public TextToken createTokenForProps(String aName, String aPattern, int startCharIndex, int endCharIndex)
    {
        TextStyle textStyle = _textLine.getRun(0).getTextStyle();
        TextToken textToken = new TextToken(_textLine, startCharIndex, endCharIndex, textStyle);
        textToken.setName(aName);
        textToken.setPattern(aPattern);

        // Get/set token color
        Color color = getColorForParseToken(textToken);
        if (color != null)
            textToken.setTextColor(color);

        // Return
        return textToken;
    }

    /**
     * Returns the first token for tokenizer and text line.
     */
    private TextToken getFirstToken(TextLine aTextLine)
    {
        // Get previous token to pickup MultilineComments/TextBlocks (do this first, since it may require use of Text.Tokenizer)
        TextToken prevToken = getPreviousTokenForTextLine(aTextLine);

        // Reset input for Tokenizer
        setInput(aTextLine);
        _textLine = aTextLine;
        setLastToken(prevToken);

        // Return next token
        return (TextToken) getNextToken();
    }

    /**
     * Checks the given token for syntax coloring.
     */
    public static Color getColorForParseToken(ParseToken aToken)
    {
        // Handle comments
        String tokenName = aToken.getName();
        if (tokenName == Tokenizer.SINGLE_LINE_COMMENT || tokenName == Tokenizer.MULTI_LINE_COMMENT)
            return COMMENT_COLOR;

        // Handle reserved words
        char firstPatternChar = aToken.getPattern().charAt(0);
        if (Character.isLetter(firstPatternChar))
            return RESERVED_WORD_COLOR;

        // Handle string literals
        if (tokenName == "StringLiteral" || tokenName == "CharacterLiteral")
            return STRING_LITERAL_COLOR;

        // Return none
        return null;
    }

    /**
     * Returns the previous token for given text line.
     */
    private static TextToken getPreviousTokenForTextLine(TextLine aTextLine)
    {
        TextLine prevLine = aTextLine.getPrevious();
        TextToken prevToken = prevLine != null ? prevLine.getLastToken() : null;

        // If previous line was empty, iterate over available previous lines till previous token found
        while (prevToken == null && prevLine != null) {
            prevLine = prevLine.getPrevious();
            prevToken = prevLine != null ? prevLine.getLastToken() : null;
        }

        return prevToken;
    }
}
