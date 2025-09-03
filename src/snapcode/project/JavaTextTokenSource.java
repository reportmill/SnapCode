package snapcode.project;
import snap.parse.ParseToken;
import snap.parse.Tokenizer;
import snap.text.TextLine;
import snap.text.TextToken;

/**
 * A Tokenizer subclass to supply tokens from JavaText lines.
 */
public class JavaTextTokenSource extends Tokenizer {

    // The JavaTextModel
    private JavaTextModel _javaText;

    // The current text line
    private TextLine _textLine;

    // The current token index in line
    private int _tokenIndex;

    /**
     * Constructor.
     */
    public JavaTextTokenSource(JavaTextModel javaText)
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
        TextToken textToken = _textLine.getNextTokenForCharIndex(charIndexInLine);
        _tokenIndex = textToken != null ? textToken.getIndex() : _textLine.getTokenCount();
    }

    /**
     * Override to get next token from JavaText TextLines.
     */
    @Override
    protected ParseToken getNextTokenImpl()
    {
        // If no more lines, return null
        if (_textLine == null)
            return null;

        // If no more line tokens, reset to next line
        while (_tokenIndex >= _textLine.getTokenCount()) {
            _tokenIndex = 0;
            _textLine = _textLine.getNext();
            if (_textLine == null)
                return null;
        }

        // Return next token
        TextToken nextToken = _textLine.getToken(_tokenIndex++);
        _charIndex = nextToken.getEndCharIndex();
        return nextToken;
    }
}
