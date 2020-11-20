/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.text;
import javakit.parse.JFile;
import javakit.parse.JavaParser;
import snap.parse.*;
import snap.gfx.*;
import snap.text.*;
import javakit.parse.JavaParser.JavaTokenizer;
import snap.util.StringUtils;
import snap.view.ViewUtils;

/**
 * A text implementation specifically for Java.
 */
public class JavaTextBox extends TextBox {

    // The JavaParser for this text
    JavaTextParser        _parser = new JavaTextParser();
    
    // The Java file
    JFilePlus             _jfile;
    
    // Images
    public static Image LVarImage = Image.get(JavaTextBox.class, "LocalVariable.png");
    public static Image FieldImage = Image.get(JavaTextBox.class,"PublicField.png");
    public static Image MethodImage = Image.get(JavaTextBox.class, "PublicMethod.png");
    public static Image ClassImage = Image.get(JavaTextBox.class, "PublicClass.png");
    public static Image PackageImage = Image.get(JavaTextBox.class, "Package.png");
    public static Image CodeImage = Image.get(JavaTextBox.class, "Code.png");

    // Colors
    static Color      _commentColor = new Color("#3F7F5F"); //336633
    static Color      _reservedWordColor = new Color("#660033");
    static Color      _stringLiteralColor = new Color("#C80000"); // CC0000

    /**
     * Creates a new JavaText.
     */
    public JavaTextBox()  { getRichText().setPlainText(true); }

    /**
     * Returns the JFile (parsed representation of Java file).
     */
    public JFilePlus getJFile()  { return _jfile!=null? _jfile : (_jfile=createJFile()); }

    /**
     * Creates the JFile.
     */
    protected JFilePlus createJFile()
    {
        JFile jfile = _parser.getJavaFile(getString());
        jfile.setSourceFile(getSourceFile());
        return new JFilePlus(this, jfile);
    }

    /**
     * Reloads symbols.
     */
    public void reloadSymbols()
    {
        ViewUtils.runLater(() -> {
            _jfile = null; getJFile(); });
    }

    /**
     * Override to clear JFile.
     */
    public void setString(String aString)  { super.setString(aString); _jfile = null; }

    /**
     * Override to return JavaTextLine.
     */
    public JavaTextLine getLine(int anIndex)  { return (JavaTextLine)super.getLine(anIndex); }

    /**
     * Override to return JavaTextLine.
     */
    public JavaTextLine getLineAt(int anIndex)  { return (JavaTextLine)super.getLineAt(anIndex); }

    /**
     * Override to do full parse when newline typed.
     */
    public void replaceChars(CharSequence theChars, TextStyle theStyle, int aStart, int anEnd)
    {
        // Do normal version
        super.replaceChars(theChars, theStyle, aStart, anEnd);

        // If newline, do full parse
        if(theChars!=null && StringUtils.indexOfNewline(theChars, 0)>=0)
            reloadSymbols();
    }

    /**
     * Override to adjust build issues start/end.
     */
    public void updateLines(int aStart, int endOld, int endNew)
    {
        // Get whether update is Add or Remove
        boolean isAdd = endNew>endOld; int length = boxlen();

        // Get whether last line in update range has unterminated comment
        boolean utermComment = getLineCount()>0 && getLineAt(endOld).isUnterminatedComment();

        // Do normal version (just return if setting everything)
        super.updateLines(aStart, endOld, endNew); if(isAdd && length==0) return;

        // If unterminated comment state changed, update successive lines until it stops
        if(utermComment!=getLineAt(endNew).isUnterminatedComment()) {
            int start = getLineAt(endNew).getEnd(), end = getRichText().indexOf("*/", start); if(end<0) end = length();
            super.updateLines(start, end, end);
        }

        // Update JFile
        getJFile().updateChars(aStart, endOld, endNew);
    }

    /**
     * Create and return TextBoxLine for given RichTextLine, start char index and line index.
     */
    protected TextBoxLine createLine(RichTextLine aTextLine, int aStart, int aLineIndex)
    {
        // Get iteration variables
        TextStyle style = aTextLine.getRun(0).getStyle(); Exception exception = null;
        int start = 0; double x = 0;

        // Create new line (just return if last line in text)
        JavaTextLine line = new JavaTextLine(this, style, aTextLine, aStart);
        if(aStart>0) {
            line.resetSizes(); return line; }

        // See if this line is InMultilineComment (do this first, since it may require use of Text.Tokenizer)
        line._utermCmnt = aLineIndex>0 && getLine(aLineIndex-1).isUnterminatedComment();

        // Get tokenizer
        JavaTokenizer tokenizer = _parser.getRealTokenizer();
        tokenizer.setInput(aTextLine);

        // Get first line token: Handle if already in Multi-line
        Token token = line.isUnterminatedComment()? tokenizer.getMultiLineCommentTokenMore(null) :
            tokenizer.getNextSpecialToken();
        if(token==null)
            try { token = tokenizer.getNextToken(); }
            catch(Exception e) { exception = e; }

        // Get line parse tokens and create TextTokens
        while(token!=null) {

            // Get token start/end
            int tokenStart = token.getInputStart(), tokenEnd = token.getInputEnd();

            // Get token x
            while(start<tokenStart) { char c = aTextLine.charAt(start); //if(start>run.getEnd()) run = run.getNext();
                if(c=='\t') x += style.getCharAdvance(' ')*4;
                else x += style.getCharAdvance(c); start++; }

            // Get token width
            double w = 0;
            while(start<tokenEnd) { char c = aTextLine.charAt(start); //if(start>run.getEnd()) run = run.getNext();
                w += style.getCharAdvance(c); start++; }

            // Create TextToken
            JavaTextToken textToken = new JavaTextToken(line, style, tokenStart, tokenEnd);
            textToken._tokenizer = _parser.getTokenizer(); textToken._token = token;
            textToken.setX(x); textToken.setWidth(w); x += w; w = 0;
            Color color = getColor(token); if(color!=null) textToken.setColor(color);
            line.addToken(textToken);

            // Update inMultilineComment for current token
            line._utermCmnt = token.getName()=="MultiLineComment" && !token.getString().endsWith("*/");

            // Get next token
            token = tokenizer.getNextSpecialToken();
            if(token==null)
                try { token = tokenizer.getNextToken(); }
                catch(Exception e) { exception = e; break; }
        }

        // If exception was hit, create token for rest of line
        if(exception!=null) {

            // Get token width
            double w = 0; int tokenStart = start, tokenEnd = aTextLine.length();
            while(start<aTextLine.length()) { char c = aTextLine.charAt(start); //if(start>run.getEnd()) run = run.getNext();
                w += style.getCharAdvance(c); start++; }

            // Create TextToken
            JavaTextToken textToken = new JavaTextToken(line, style, tokenStart, tokenEnd);
            textToken.setX(x); textToken.setWidth(w); x += w; w = 0;
            line.addToken(textToken);
        }

        // Return line
        line.resetSizes();
        return line;
    }

    /**
     * Checks the given token for syntax coloring.
     */
    private Color getColor(Token aToken)
    {
        // Handle comments
        if(aToken.getName()=="SingleLineComment" || aToken.getName()=="MultiLineComment")
            return JavaTextBox._commentColor;

        // Handle reserved words
        else if(Character.isLetter(aToken.getPattern().charAt(0)))
            return JavaTextBox._reservedWordColor;

        // Handle string literals
        else if(aToken.getName()=="StringLiteral" || aToken.getName()=="CharacterLiteral")
            return JavaTextBox._stringLiteralColor;
        return null;
    }

    /**
     * A TextLine subclass specifically for JavaText.
     */
    public class JavaTextLine extends TextBoxLine {

        /** Creates a new JavaTextLine. */
        public JavaTextLine(TextBox aBox, TextStyle aStartStyle, RichTextLine aTextLine, int theRTLStart)
        { super(aBox, aStartStyle, aTextLine, theRTLStart); }

        /** Override to return JavaTextToken. */
        public JavaTextToken getToken(int anIndex)  { return (JavaTextToken)super.getToken(anIndex); }

        /** Returns whether line is an unterminated comment. */
        public boolean isUnterminatedComment()  { return _utermCmnt; }  boolean _utermCmnt;

        /** Returns the x for tab at given x. */
        protected double getXForTabAtIndexAndX(int aCharInd, double aX)
        {
            TextStyle style = getStyleAt(aCharInd);
            return aX + style.getCharAdvance(' ')*4;
        }
    }

    /**
     * A TextToken subclass specifically for JavaText.
     */
    public static class JavaTextToken extends TextBoxToken implements Token {

        // The tokenizer that provided this token
        Tokenizer    _tokenizer;

        // The parse token
        Token        _token;

        /** Creates a new Token for given box line, run and character start/end.  */
        public JavaTextToken(TextBoxLine aLine, TextStyle aStyle, int aStart, int aEnd) { super(aLine,aStyle,aStart,aEnd); }

        /** The Tokenizer that provided this token. */
        public Tokenizer getTokenizer()  { return _tokenizer; }

        /** Parse Token method. */
        public String getName()  { return _token!=null? _token.getName() : null; }

        /** Parse Token method. */
        public String getPattern()  { return _token!=null? _token.getPattern() : null; }

        /** Parse Token method. */
        public int getInputStart()  { return getLine().getStart() + getStart(); }

        /** Parse Token method. */
        public int getInputEnd()  { return getLine().getStart() + getEnd(); }

        /** Parse Token method. */
        public int getLineIndex()  { return getLine().getIndex(); }

        /** Parse Token method. */
        public int getLineStart()  { return getLine().getStart(); }

        /** Parse Token method. */
        public int getColumnIndex()  { return getStart(); }

        /** Parse Token method. */
        public Token getSpecialToken()  { return null; }

        /** Returns whether this token is SpecialToken (Java comment). */
        public boolean isSpecialToken()  { return getName()!=null && getName().endsWith("Comment"); }
    }

    /**
     * A JavaParser specifically for JavaText.
     */
    public class JavaTextParser extends JavaParser {

        // The text tokenizer
        JavaTextTokenizer   _textTokenizer = new JavaTextTokenizer();

        /** Returns tokenizer that gets tokens from text. */
        public JavaTextTokenizer getTokenizer()  { return _textTokenizer; }

        /** Returns the original tokenizer. */
        public JavaTokenizer getRealTokenizer()  { return super.getTokenizer(); }
    }

    /**
     * A tokenizer that gets tokens from text lines.
     */
    public class JavaTextTokenizer extends JavaParser.JavaTokenizer {

        // The current line
        JavaTextLine   _line;

        // The token index on line
        int            _tokenIndex;

        // Override to reset tokenizer
        public void setInput(CharSequence anInput)  { super.setInput(anInput); _line = null; }

        /** Sets the input start. */
        public void setCharIndex(int aStart)
        {
            _line = getLineAt(aStart); _tokenIndex = 0;
            while(_tokenIndex<_line.getTokenCount()) {
                if(aStart<_line.getToken(_tokenIndex).getEnd() + _line.getStart())
                    break;
                _tokenIndex++;
            }
        }

        // Override to get token from next line
        public Token getNextToken()
        {
            // If line is out of tokens, get next line
            if(_line==null || _tokenIndex>=_line.getTokenCount()) {
                JavaTextLine line = getNextLine(_line);
                while(line!=null && line.getTokenCount()==0) line = getNextLine(line);
                if(line==null) return null;
                _line = line; _tokenIndex = 0;
            }

            // Return token for line
            JavaTextToken token = _line.getToken(_tokenIndex++);
            if(token.isSpecialToken()) return getNextToken();
            return token;
        }

        // Returns the next line
        private JavaTextLine getNextLine(TextBoxLine aLine)
        {
            int index = aLine!=null? aLine.getIndex()+1 : 0;
            return index<getLineCount()? getLine(index) : null;
        }
    }
}