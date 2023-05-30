package snapcode.app;
import snapcode.javatext.JavaTextUtils;
import snapcode.project.JeplTextDoc;
import snapcode.project.JeplAgent;
import snapcode.project.Project;
import snapcode.project.BuildFile;
import javakit.resolver.Resolver;
import snap.gfx.Font;
import snap.props.PropObject;
import snap.text.TextStyle;
import snap.util.SnapUtils;
import snapcharts.data.DoubleArray;
import snapcode.repl.*;

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
        if (Resolver.isTeaVM) {
            javakit.resolver.StaticResolver.shared()._next = new StaticResolver();
        }

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
        jeplTextDoc.addImport("snapcode.repl.*");
        jeplTextDoc.setSuperClassName(ReplObject.class.getName());

        // Set code font
        Font codeFont = JavaTextUtils.getCodeFont();
        jeplTextDoc.setDefaultStyle(new TextStyle(codeFont));
    }

    /**
     * Configures JeplTextDoc project to make sure it references SnapKit, SnapCode and SnapCharts.
     */
    public static void configureJeplDocProject(JeplTextDoc jeplTextDoc)
    {
        // If TeaVM, just return
        if (SnapUtils.isTeaVM) return;

        // Get BuildFile
        JeplAgent jeplAgent = jeplTextDoc.getAgent();
        Project proj = jeplAgent.getProject();
        BuildFile buildFile = proj.getBuildFile();

        // Add dependencies to BuildFile for SnapKit, SnapCode and SnapCharts
        buildFile.addLibPathForClass(PropObject.class);
        buildFile.addLibPathForClass(QuickCharts.class);
        buildFile.addLibPathForClass(DoubleArray.class);
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
