package snapcode.debug;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.text.TextBlock;
import snap.text.TextLink;
import snap.text.TextStyle;
import snap.util.Convert;
import snapcode.apptools.RunTool;
import snapcode.project.JavaTextDocUtils;

/**
 * A TextBlock to hold system console output.
 */
public class ConsoleText extends TextBlock {

    // The RunTool
    private RunTool _runTool;

    // Constants
    private static Color ERROR_COLOR = new Color("CC0000");

    /**
     * Constructor.
     */
    public ConsoleText()
    {
        super();
        setRichText(true);

        // Set font
        TextStyle textStyle = getDefaultStyle();
        Font codeFont = JavaTextDocUtils.getCodeFont();
        TextStyle codeTextStyle = textStyle.copyFor(codeFont);
        setDefaultStyle(codeTextStyle);
    }

    /**
     * Sets the RunTool.
     */
    public void setRunTool(RunTool runTool)  { _runTool = runTool; }

    /**
     * Appends given string to text with option for whether text is error.
     */
    public void appendString(String aStr, boolean isError)
    {
        // Handle overflow
        if (length() > 100000) return;

        // Get color
        Color color = isError ? ERROR_COLOR : Color.BLACK;

        // Get default style modified for color
        TextStyle style = getStyleForCharIndex(length());
        if (!style.getColor().equals(color))
            style = style.copyFor(color);

        // Look for a StackFrame reference: " at java.pkg.Class(Class.java:55)" and add as link if found
        int start = 0;
        for (int i = aStr.indexOf(".java:"); i > 0; i = aStr.indexOf(".java:", start)) {

            // Get start/end of Java file name inside parens (if parens not found, just add chars and continue)
            int parenStartIndex = aStr.lastIndexOf("(", i);
            int parenEndIndex = aStr.indexOf(")", i);
            if (parenStartIndex < start || parenEndIndex < 0) {
                addChars(aStr.substring(start, start = i + 6), style, length());
                continue;
            }

            // Get chars before parens and add
            String prefix = aStr.substring(start, parenStartIndex + 1);
            addChars(prefix, style, length());

            // Get link text, link address, TextLink
            String linkText = aStr.substring(parenStartIndex + 1, parenEndIndex);
            String linkAddr = getLink(prefix, linkText);
            TextLink textLink = new TextLink(linkAddr);

            // Get TextStyle for link and add link chars
            TextStyle lstyle = style.copyFor(textLink);
            addChars(linkText, lstyle, length());

            // Update start to end of link text and continue
            start = parenEndIndex;
        }

        // Add remainder normally
        addChars(aStr.substring(start), style, length());
    }

    /**
     * Returns a link for a StackString.
     */
    protected String getLink(String aPrefix, String linkedText)
    {
        // Get start/end of full class path for .java
        int start = aPrefix.indexOf("at ");
        if (start < 0)
            return "/Unknown";
        start += 3;
        int end = aPrefix.indexOf('$');
        if (end < start)
            end = aPrefix.lastIndexOf('.');
        if (end < start)
            end = aPrefix.length() - 1;

        // Create link from path and return
        String path = aPrefix.substring(start, end);
        path = '/' + path.replace('.', '/') + ".java";
        path = _runTool.getSourceURL(path);
        String lineStr = linkedText.substring(linkedText.indexOf(":") + 1);
        int line = Convert.intValue(lineStr);
        if (line > 0)
            path += "#LineNumber=" + line;

        // Return
        return path;
    }
}
