package snapcode.app;
import javakit.resolver.StaticResolverGen;
import snap.util.ArrayUtils;
import snap.web.WebFile;
import snap.web.WebURL;

/**
 * A StaticResolverGen for SnapCharts.
 */
public class StaticResolverGenSC extends StaticResolverGen {

    /**
     * Standard main implementation.
     */
    public static void main(String[] args)
    {
        // Add WhiteList strings from parent?
        _whiteListStrings = ArrayUtils.addAll(_whiteListStrings, StaticResolverGen._whiteListStrings);

        _package = "snapcode.app";
        StaticResolverGenSC codeGen = new StaticResolverGenSC();
        codeGen.generateStaticResolverForClasses(_javaClasses, _whiteListStrings, _blackListStrings);

        WebFile webFile = WebURL.getURL("/tmp/StaticResolver.java").createFile(false);
        webFile.setText(_sb.toString());
        webFile.save();
    }

    // Packages
    private static Class<?>[]  _javaClasses = {

            snapcharts.data.DoubleArray.class,
            snapcharts.data.DataArray.class,
            snapcharts.data.DataSet.class,
            ChartsREPL.class,
            Quick3D.class,
            QuickCharts.class,
            QuickData.class,
            QuickDraw.class,
            QuickDrawPen.class,
    };

    // WhiteList
    protected static String[] _whiteListStrings = {

            // Object
            "clone",

            // DoubleArray
            "length", "map", "filter", "doubleArray", "toArray", "of", "fromMinMax", "fromMinMaxCount",

            // DataArray

            // DataSet

            // ChartsREPL
            "print", "println",

            // Quick3D
            "createCube", "createImage3D",

            // QuickCharts
            "chart", "chart3D",

            // QuickData
            "doubleArray", "dataArray", "dataSet", "minMaxArray", "mapXY",
            "getTextForSource", "getImageForSource",

            // QuickDraw, QuickDrawPen
            "setGridSpacing", "getPen", "setPenColor", "createDrawView",
            "getColor", "setColor", "getWidth", "setWidth", "getDirection", "setDirection",
            "moveTo", "lineTo", "closePath", "forward", "turn", "getAnimPen",
    };
    private static String[] _blackListStrings = {

            "java.lang.String.getBytes(int,int,byte[],int)",

    };
}
