package snapcode.util;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.text.TextStyle;

/**
 * Utility methods for Markdown.
 */
public class MDUtils {

    // The header 1 style
    private static TextStyle _header1Style;

    // The header 2 style
    private static TextStyle  _header2Style;

    // The content style
    private static TextStyle  _contentStyle;

    // The code style
    private static TextStyle  _codeStyle;

    /**
     * Returns the header 1 style.
     */
    public static TextStyle getHeader1Style()
    {
        // If already set, just return
        if (_header1Style != null) return _header1Style;

        // Create, configure
        TextStyle textStyle = TextStyle.DEFAULT;
        Font headerFont = new Font("Arial Bold", 24);
        Color headerColor = Color.BLACK;
        TextStyle headerStyle = textStyle.copyFor(headerFont, headerColor);

        // Set, return
        return _header1Style = headerStyle;
    }

    /**
     * Returns the header 2 style.
     */
    public static TextStyle getHeader2Style()
    {
        // If already set, just return
        if (_header2Style != null) return _header2Style;

        // Create, configure
        TextStyle textStyle = TextStyle.DEFAULT;
        Font headerFont = new Font("Arial Bold", 20);
        Color headerColor = Color.GRAY3;
        TextStyle headerStyle = textStyle.copyFor(headerFont, headerColor);

        // Set, return
        return _header2Style = headerStyle;
    }

    /**
     * Returns the content style.
     */
    public static TextStyle getContentStyle()
    {
        // If already set, just return
        if (_contentStyle != null) return _contentStyle;

        // Create, configure
        TextStyle textStyle = TextStyle.DEFAULT;
        Font contentFont = Font.Arial14;
        Color contentColor = Color.GRAY3;
        TextStyle contentStyle = textStyle.copyFor(contentFont, contentColor);

        // Set, return
        return _contentStyle = contentStyle;
    }

    /**
     * Returns the code style.
     */
    public static TextStyle getCodeStyle()
    {
        // If already set, just return
        if (_codeStyle != null) return _codeStyle;

        // Get code font
        Font codeFont = null;
        String[] names = { "Monaco", "Consolas", "Courier" };
        for (String name : names) {
            codeFont = new Font(name, 13);
            if (codeFont.getFamily().startsWith(name))
                break;
        }

        // Create, configure
        TextStyle textStyle = TextStyle.DEFAULT;
        Color codeColor = Color.GRAY3;
        TextStyle codeStyle = textStyle.copyFor(codeFont, codeColor);

        // Set, return
        return _codeStyle = codeStyle;
    }
}
