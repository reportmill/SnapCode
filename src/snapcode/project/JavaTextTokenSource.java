package snapcode.project;
import snap.parse.ParseToken;
import snap.parse.Tokenizer;
import snap.text.TextLine;
import snap.text.TextToken;

/**
 * A Tokenizer subclass to supply tokens from JavaText lines.
 */
public class JavaTextTokenSource extends Tokenizer {

    // The JavaTextDoc
    private JavaTextDoc _javaText;

    // The current text line
    private TextLine _textLine;

    // The current token index in line
    private int _tokenIndex;

    /**
     * Constructor.
     */
    public JavaTextTokenSource(JavaTextDoc javaText)
    {
        super();
        _javaText = javaText;
        setCharIndex(0);
    }

    /**
     * Override to set text line, charIndex and tokenIndex.
     */
    @Override
    public void setCharIndex(int aValue)
    {
        super.setCharIndex(aValue);

        // Set text line
        _textLine = _javaText.getLineForCharIndex(_charIndex);

        // Set token index
        int charIndexInLine = _charIndex - _textLine.getStartCharIndex();
        TextToken textToken = _textLine.getTokenForCharIndex(charIndexInLine);
        _tokenIndex = textToken != null ? textToken.getIndex() : _textLine.getTokenCount();
    }

    /**
     * Override to get next token from JavaText TextLines.
     */
    @Override
    public ParseToken getNextToken()
    {
        // If no more lines, return null
        if (_textLine == null)
            return null;

        // If more line tokens, return next
        if (_tokenIndex < _textLine.getTokenCount()) {
            TextToken textToken = _textLine.getToken(_tokenIndex++);
            if (textToken.getName() == Tokenizer.SINGLE_LINE_COMMENT || textToken.getName() == Tokenizer.MULTI_LINE_COMMENT)
                return getNextToken();

            _charIndex = textToken.getEndCharIndex();
            return textToken;
        }

        // Set next text line
        _charIndex = _textLine.getEndCharIndex();
        _textLine = _textLine.getNext();
        _tokenIndex = 0;
        return getNextToken();
    }
}
