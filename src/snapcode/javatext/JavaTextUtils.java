/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.resolver.JavaDecl;
import snap.gfx.Image;

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
}