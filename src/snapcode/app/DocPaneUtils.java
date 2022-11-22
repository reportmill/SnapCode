/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import javakit.parse.JavaTextDoc;
import javakit.parse.JavaTextDocBuilder;
import javakit.parse.JeplTextDoc;
import javakit.resolver.Resolver;
import snap.props.PropObject;
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

        // Link up StaticResolver for TeaVM
        if (Resolver.isTeaVM) {
            javakit.resolver.StaticResolver.shared()._next = new StaticResolver();
            javakit.resolver.ClassTreeWeb.addCommonClassNames(MORE_COMMON_CLASS_NAMES);
        }

        // Add class paths for SnapKit and SnapCharts
        if (!SnapUtils.isTeaVM) {
            resolver.addClassPathForClass(PropObject.class);
            resolver.addClassPathForClass(DoubleArray.class);
        }

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
            DoubleArray.class.getName()
    };
}
