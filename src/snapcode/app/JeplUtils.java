package snapcode.app;
import snapcode.javatext.JavaTextUtils;
import snapcode.project.*;
import snap.gfx.Font;
import snap.props.PropObject;
import snap.text.TextStyle;
import snap.util.SnapUtils;
import snapcharts.data.DoubleArray;
import snapcharts.repl.*;

/**
 * Utilities to support Jepl files.
 */
public class JeplUtils {

    // Whether JavaKit has been configured for this app
    private static boolean  _didInitJavaKit;

    /**
     * Initialize JavaKit for this app.
     */
    public static void initJavaKitForThisApp()
    {
        // If already did init, just return
        if (_didInitJavaKit) return;
        _didInitJavaKit = true;

        // Set JeplAgent config
        JeplTextDoc.setJeplDocConfig(jtd -> configureJeplDoc(jtd));

        // For TeaVM: Link up StaticResolver
        //if (Resolver.isTeaVM) javakit.resolver.StaticResolver.shared()._next = new StaticResolver();

        // Add more common class names from SnapCode
        javakit.resolver.ClassTreeWeb.addCommonClassNames(MORE_COMMON_CLASS_NAMES);
    }

    /**
     * Configures default JeplTextDocs.
     */
    private static void configureJeplDoc(JeplTextDoc jeplTextDoc)
    {
        // Set imports and SuperClassName
        jeplTextDoc.addImport("snapcharts.data.*");
        jeplTextDoc.addImport("snapcharts.repl.*");
        jeplTextDoc.addImport("static snapcharts.repl.ReplObject.*");
        jeplTextDoc.addImport("static snapcharts.repl.QuickCharts.*");
        jeplTextDoc.addImport("static snapcharts.repl.QuickData.*");
        //jeplTextDoc.setSuperClassName(ReplObject.class.getName());

        // Set code font
        Font codeFont = JavaTextUtils.getCodeFont();
        jeplTextDoc.setDefaultStyle(new TextStyle(codeFont));
    }

    /**
     * Configures JeplTextDoc project to make sure it references SnapKit, SnapCode and SnapCharts.
     */
    public static void configureJeplDocProject(JavaTextDoc javaTextDoc)
    {
        // If TeaVM, just return
        if (SnapUtils.isTeaVM) return;

        // Get BuildFile
        JavaAgent javaAgent = javaTextDoc.getAgent();
        Project proj = javaAgent.getProject();
        BuildFile buildFile = proj.getBuildFile();

        // Add dependencies to BuildFile for SnapKit and SnapCharts
        buildFile.addMavenDependencyForGroupAndPackageAndVersion("com.reportmill", "snapkit", "2023.06");
        buildFile.addMavenDependencyForGroupAndPackageAndVersion("com.reportmill", "snapcharts", "2023.06");
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
