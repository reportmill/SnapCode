package snapcode.util;
import snap.gfx.Color;
import snap.text.RichText;
import snap.text.TextDoc;
import snap.text.TextLink;
import snap.text.TextStyle;
import snap.util.Convert;
import snap.util.StringUtils;

/**
 * This class holds utility methods for exceptions.
 */
public class ExceptionUtil {

    // Constants
    private static Color ERROR_COLOR = Color.get("#CC0000");

    /**
     * Returns a TextDoc for given exception.
     */
    public static TextDoc getTextDocForException(Exception anException)
    {
        String str = StringUtils.getStackTraceString(anException);

        if (true) {
            TextDoc textDoc = new TextDoc();
            textDoc.setDefaultStyle(textDoc.getDefaultStyle().copyFor(ERROR_COLOR));
            textDoc.addChars(str);
            return textDoc;
        }

        TextDoc textDoc = new RichText();
        appendString(textDoc, str, ERROR_COLOR);
        return textDoc;
    }

    /**
     * Appends text with given color.
     */
    private static void appendString(TextDoc aTextDoc, String aStr, Color aColor)
    {
        // Get default style modified for color
        TextStyle style = aTextDoc.getStyleForCharIndex(aTextDoc.length());
        style = style.copyFor(aColor);

        // Look for a StackFrame reference: " at java.pkg.Class(Class.java:55)" and add as link if found
        int start = 0;
        for (int i = aStr.indexOf(".java:"); i > 0; i = aStr.indexOf(".java:", start)) {

            // Get start/end of Java file name inside parens (if parens not found, just add chars and continue)
            int s = aStr.lastIndexOf("(", i);
            int e = aStr.indexOf(")", i);
            if (s < start || e < 0) {
                String str = aStr.substring(start, start = i + 6);
                aTextDoc.addChars(str, style, aTextDoc.length());
                continue;
            }

            // Get chars before parens and add
            String prefix = aStr.substring(start, s + 1);
            aTextDoc.addChars(prefix, style, aTextDoc.length());

            // Get link text, link address, TextLink
            String linkText = aStr.substring(s + 1, e);
            String linkAddr = getLink(prefix, linkText);
            TextLink textLink = new TextLink(linkAddr);

            // Get TextStyle for link and add link chars
            TextStyle lstyle = style.copyFor(textLink);
            aTextDoc.addChars(linkText, lstyle, aTextDoc.length());

            // Update start to end of link text and continue
            start = e;
        }

        // Add remainder normally
        String remainderStr = aStr.substring(start);
        aTextDoc.addChars(remainderStr, style, aTextDoc.length());
    }

    /**
     * Returns a link for a StackString.
     */
    private static String getLink(String aPrefix, String linkedText)
    {
        // Get start/end of full class path for .java
        int start = aPrefix.indexOf("at ");
        if (start < 0) return "/Unknown";
        start += 3;
        int end = aPrefix.indexOf('$');
        if (end < start) end = aPrefix.lastIndexOf('.');
        if (end < start) end = aPrefix.length() - 1;

        // Create link from path and return
        String path = aPrefix.substring(start, end);
        path = '/' + path.replace('.', '/') + ".java";
        path = getSourceURL(path);
        String lineStr = linkedText.substring(linkedText.indexOf(":") + 1);
        int line = Convert.intValue(lineStr);
        if (line > 0) path += "#LineNumber=" + line;
        return path;
    }

    /**
     * Returns a source URL for path.
     */
    private static String getSourceURL(String aPath)
    {
        if (aPath.startsWith("/java/") || aPath.startsWith("/javax/"))
            return "http://reportmill.com/jars/8u05/src.zip!" + aPath;
        if (aPath.startsWith("/javafx/"))
            return "http://reportmill.com/jars/8u05/javafx-src.zip!" + aPath;

        return aPath;
        //WebFile file = proj.getProjectSet().getSourceFile(aPath);
        //return file != null ? file.getURL().getString() : aPath;
    }

}
