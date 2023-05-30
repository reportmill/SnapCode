/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.resolver.JavaDecl;
import snap.gfx.Font;
import snap.gfx.Image;

/**
 * This class contains utility support for Java text.
 */
public class JavaTextUtils {

    // The recommended default font for code
    private static Font  _codeFont;

    /**
     * Returns a suitable code font.
     */
    public static Font getCodeFont()
    {
        if (_codeFont == null) {
            String[] names = { "Monaco", "Consolas", "Courier"};
            for (String name : names) {
                _codeFont = new Font(name, 12);
                if (_codeFont.getFamily().startsWith(name))
                    break;
            }
        }

        // Return
        return _codeFont;
    }

    // Images
    public static Image LVarImage = Image.getImageForClassResource(JavaTextUtils.class, "LocalVariable.png");
    public static Image FieldImage = Image.getImageForClassResource(JavaTextUtils.class, "PublicField.png");
    public static Image MethodImage = Image.getImageForClassResource(JavaTextUtils.class, "PublicMethod.png");
    public static Image ClassImage = Image.getImageForClassResource(JavaTextUtils.class, "PublicClass.png");
    public static Image PackageImage = Image.getImageForClassResource(JavaTextUtils.class, "Package.png");
    public static Image CodeImage = Image.getImageForClassResource(JavaTextUtils.class, "Code.png");
    public static Image DefaultImage;

    /**
     * Returns an icon image for given JavaDecl.
     */
    public static Image getImageForJavaDecl(JavaDecl aDecl)
    {
        switch (aDecl.getType()) {
            case VarDecl: return LVarImage;
            case Field: return FieldImage;
            case Method: return MethodImage;
            case Class: return ClassImage;
            case Package: return PackageImage;
            default:
                if (DefaultImage == null)
                    DefaultImage = Image.getImageForSize(16, 16, true);
                return DefaultImage;
        }
    }

    /**
     * Removes superfluous indent from a string.
     */
    public static String removeExtraIndentFromString(String str)
    {
        // If starts with newline, strip it
        if (str.startsWith("\n"))
            str = str.substring(1);
        boolean endsWithNewline = str.endsWith("\n");

        // Get string as lines
        String[] lines = str.split("\n");
        int minIndent = 99;

        // Get minimum indent for given lines
        for (String line : lines) {
            if (line.trim().length() == 0)
                continue;
            int indent = 0;
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == ' ')
                    indent++;
                else break;
            }
            minIndent = Math.min(minIndent, indent);
        }

        // If there is superfluous indent, remove from lines and reset string
        if (minIndent > 0) {

            // Get indent string
            String indentStr = " ";
            for (int i = 1; i < minIndent; i++) indentStr += ' ';

            // Remove indent string from lines
            for (int i = 0; i < lines.length; i++)
                lines[i] = lines[i].replaceFirst(indentStr, "");

            // Rebuild string
            str = String.join("\n", lines);
            if (endsWithNewline)
                str += "\n";
        }

        // Return
        return str;
    }
}