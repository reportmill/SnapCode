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

        // If there is superfluous indent, remove from lines and reset string
        int minIndent = getMinIndentLengthForStrings(lines);
        if (minIndent > 0) {

            // Get indent string
            String indentStr = " ".repeat(minIndent);

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

    /**
     * Returns the minimum number of leading spaces in given array of strings (disregards empty or whitespace lines).
     */
    private static int getMinIndentLengthForStrings(String[] lines)
    {
        int minIndent = Integer.MAX_VALUE;

        // Iterate over lines
        for (String line : lines) {

            // Skip empty/whitespace lines
            if (CharSequenceUtils.isWhiteSpace(line))
                continue;

            // Get indent for line
            int indent = CharSequenceUtils.getIndentLength(line);
            minIndent = Math.min(minIndent, indent);
        }

        // Return
        return minIndent != Integer.MAX_VALUE ? minIndent : 0;
    }
}