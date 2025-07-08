/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.resolver.JavaDecl;
import snap.gfx.Image;
import snap.util.CharSequenceUtils;

/**
 * This class contains utility support for Java text.
 */
public class JavaTextUtils {

    // Images
    public static Image LVarImage = Image.getImageForClassResource(JavaTextUtils.class, "LocalVariable.png");
    public static Image FieldImage = Image.getImageForClassResource(JavaTextUtils.class, "PublicField.png");
    public static Image MethodImage = Image.getImageForClassResource(JavaTextUtils.class, "PublicMethod.png");
    public static Image ClassImage = Image.getImageForClassResource(JavaTextUtils.class, "PublicClass.png");
    public static Image PackageImage = Image.getImageForClassResource(JavaTextUtils.class, "Package.png");
    public static Image WordImage = Image.getImageForClassResource(JavaTextUtils.class, "Breakpoint.png");
    //public static Image CodeImage = Image.getImageForClassResource(JavaTextUtils.class, "Code.png");
    public static Image DefaultImage = Image.getImageForSize(16, 16, true);

    /**
     * Returns an icon image for given JavaDecl.
     */
    public static Image getImageForJavaDecl(JavaDecl aDecl)
    {
        return switch (aDecl.getType()) {
            case VarDecl -> LVarImage;
            case Field -> FieldImage;
            case Method -> MethodImage;
            case Class -> ClassImage;
            case Package -> PackageImage;
            case Word -> WordImage;
            default -> DefaultImage;
        };
    }

    /**
     * Removes superfluous indent from a string.
     */
    public static String removeExtraIndentFromString(String str)
    {
        // Get string as lines
        String[] lines = str.split("\n");
        int minIndent = 99;

        // Get minimum indent for given lines
        for (String line : lines) {
            if (CharSequenceUtils.isWhiteSpace(line))
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
            boolean endsWithNewline = str.endsWith("\n");
            str = String.join("\n", lines);
            if (endsWithNewline)
                str += "\n";
        }

        // Return
        return str;
    }
}