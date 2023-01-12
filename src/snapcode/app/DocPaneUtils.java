/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import javakit.ide.JavaTextUtils;
import javakit.parse.JavaTextDoc;
import javakit.parse.JavaTextDocBuilder;
import javakit.parse.JeplTextDoc;
import javakit.resolver.Resolver;
import snap.gfx.Font;
import snap.props.PropObject;
import snap.text.TextStyle;
import snap.util.SnapUtils;
import snapcharts.data.DoubleArray;

/**
 * This View subclass shows snippets.
 */
public class DocPaneUtils {

    /**
     * Creates a JavaTextDoc for given Resolver.
     */
    public static JeplTextDoc createJeplTextDoc()
    {
        // Create/config/set Doc
        JeplTextDoc jeplDoc = new JeplTextDoc();

        // Get template Java text string
        JavaTextDocBuilder javaTextDocBuilder = jeplDoc.getJavaTextDocBuilder();
        javaTextDocBuilder.setSuperClassName(ChartsREPL.class.getName());
        javaTextDocBuilder.addImport("snapcharts.data.*");
        javaTextDocBuilder.addImport("snapcode.app.*");

        // Create/set resolver
        Resolver resolver = createResolver();
        jeplDoc.setResolver(resolver);

        // Set code font
        Font codeFont = JavaTextUtils.getCodeFont();
        jeplDoc.setDefaultStyle(new TextStyle(codeFont));

        // Return
        return jeplDoc;
    }

    /**
     * Creates the Resolver.
     */
    private static Resolver createResolver()
    {
        // Create resolver
        Resolver resolver = Resolver.newResolverForClassLoader(JavaTextDoc.class.getClassLoader());

        // For TeaVM: Link up StaticResolver
        if (Resolver.isTeaVM) {
            javakit.resolver.StaticResolver.shared()._next = new StaticResolver();
        }

        // For Desktop: Add class paths for SnapKit, SnapCode and SnapCharts
        if (!SnapUtils.isTeaVM) {
            resolver.addClassPathForClass(PropObject.class);
            resolver.addClassPathForClass(QuickCharts.class);
            resolver.addClassPathForClass(DoubleArray.class);
        }

        // Add more common class names from SnapCode
        javakit.resolver.ClassTreeWeb.addCommonClassNames(MORE_COMMON_CLASS_NAMES);

        // Return
        return resolver;
    }

    /**
     * Common class names for browser.
     */
    private static String[] MORE_COMMON_CLASS_NAMES = {
            Quick3D.class.getName(),
            QuickCharts.class.getName(),
            QuickData.class.getName(),
            QuickDraw.class.getName(),
            QuickDrawPen.class.getName(),
            DoubleArray.class.getName()
    };
}
