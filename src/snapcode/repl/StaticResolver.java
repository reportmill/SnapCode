package snapcode.repl;
import javakit.resolver.*;
import java.io.PrintStream;

/**
 * Provide reflection info for TeaVM.
 */
public class StaticResolver extends javakit.resolver.StaticResolver {

    /**
     * Returns the declared fields for given class.
     */
    public JavaField[] getFieldsForClass(Resolver aResolver, String aClassName)
    {
        fb.init(aResolver, aClassName);

        switch (aClassName) {

            // Handle java.lang.System
            case "java.lang.System":
                fb.name("out").type(PrintStream.class).save();
                return fb.name("err").type(PrintStream.class).buildAll();

            // Handle anything else
            default:
                if (_next != null) return _next.getFieldsForClass(aResolver, aClassName);
                return new JavaField[0];
        }
    }

    /**
     * Returns the declared methods for given class.
     */
    public JavaMethod[] getMethodsForClass(Resolver aResolver, String aClassName)
    {
        mb.init(aResolver, aClassName);

        switch (aClassName) {

            // Handle snapcharts.data.DoubleArray
            case "snapcharts.data.DoubleArray":
                mb.name("clone").returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("clone").returnType(java.lang.Object.class).save();
                mb.name("clone").returnType(snapcharts.data.NumberArray.class).save();
                mb.name("clone").returnType(snapcharts.data.DataArray.class).save();
                mb.name("toArray").returnType(double[].class).save();
                mb.name("filter").paramTypes(java.util.function.DoublePredicate.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("map").paramTypes(java.util.function.DoubleUnaryOperator.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("of").paramTypes(java.lang.Object[].class).returnType(snapcharts.data.DoubleArray.class).varArgs().save();
                mb.name("fromMinMax").paramTypes(double.class,double.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("doubleArray").returnType(double[].class).save();
                return mb.name("fromMinMaxCount").paramTypes(double.class,double.class,int.class).returnType(snapcharts.data.DoubleArray.class).buildAll();

            // Handle snapcharts.data.DataArray
            case "snapcharts.data.DataArray":
                mb.name("length").returnType(int.class).save();
                mb.name("toString").returnType(java.lang.String.class).save();
                return mb.name("getName").returnType(java.lang.String.class).buildAll();

            // Handle snapcharts.data.DataSet
            case "snapcharts.data.DataSet":
                mb.name("clone").returnType(java.lang.Object.class).save();
                mb.name("clone").returnType(snapcharts.data.DataSet.class).save();
                mb.name("clone").returnType(snap.props.PropObject.class).save();
                mb.name("getName").returnType(java.lang.String.class).save();
                mb.name("getPropValue").paramTypes(java.lang.String.class).returnType(java.lang.Object.class).save();
                return mb.name("setPropValue").paramTypes(java.lang.String.class,java.lang.Object.class).returnType(void.class).buildAll();

            // Handle snapcode.repl.ReplObject
            case "snapcode.repl.ReplObject":
                mb.name("println").paramTypes(java.lang.Object.class).returnType(void.class).save();
                return mb.name("print").paramTypes(java.lang.Object.class).returnType(void.class).buildAll();

            // Handle snapcode.repl.Quick3D
            case "snapcode.repl.Quick3D":
                mb.name("createCube").returnType(snap.gfx3d.CameraView.class).save();
                return mb.name("createImage3D").paramTypes(snap.gfx.Image.class).returnType(snap.gfx3d.CameraView.class).buildAll();

            // Handle snapcode.repl.QuickCharts
            case "snapcode.repl.QuickCharts":
                mb.name("chart").paramTypes(java.lang.Object[].class).returnType(snapcharts.model.Chart.class).varArgs().save();
                return mb.name("chart3D").paramTypes(java.lang.Object[].class).returnType(snapcharts.model.Chart.class).varArgs().buildAll();

            // Handle snapcode.repl.QuickData
            case "snapcode.repl.QuickData":
                mb.name("dataArray").paramTypes(java.lang.Object.class).returnType(snapcharts.data.DataArray.class).save();
                mb.name("dataSet").paramTypes(java.lang.Object[].class).returnType(snapcharts.data.DataSet.class).varArgs().save();
                mb.name("mapXY").paramTypes(double[].class,double[].class,java.util.function.DoubleBinaryOperator.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("mapXY").paramTypes(snapcharts.data.DoubleArray.class,snapcharts.data.DoubleArray.class,java.util.function.DoubleBinaryOperator.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("doubleArray").paramTypes(java.lang.Object[].class).returnType(snapcharts.data.DoubleArray.class).varArgs().save();
                mb.name("minMaxArray").paramTypes(double.class,double.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("minMaxArray").paramTypes(double.class,double.class,int.class).returnType(snapcharts.data.DoubleArray.class).save();
                mb.name("getTextForSource").paramTypes(java.lang.Object.class).returnType(java.lang.String.class).save();
                return mb.name("getImageForSource").paramTypes(java.lang.Object.class).returnType(snap.gfx.Image.class).buildAll();

            // Handle snapcode.repl.QuickDraw
            case "snapcode.repl.QuickDraw":
                mb.name("isShowGrid").returnType(boolean.class).save();
                mb.name("isAnimate").returnType(boolean.class).save();
                mb.name("setAnimate").paramTypes(boolean.class).returnType(void.class).save();
                mb.name("getPen").paramTypes(int.class).returnType(QuickDrawPen.class).save();
                mb.name("getPen").returnType(QuickDrawPen.class).save();
                mb.name("moveTo").paramTypes(double.class,double.class).returnType(void.class).save();
                mb.name("lineTo").paramTypes(double.class,double.class).returnType(void.class).save();
                mb.name("closePath").returnType(void.class).save();
                mb.name("forward").paramTypes(double.class).returnType(void.class).save();
                mb.name("turn").paramTypes(double.class).returnType(void.class).save();
                mb.name("createDrawView").paramTypes(int.class,int.class).returnType(QuickDraw.class).save();
                mb.name("createDrawView").returnType(QuickDraw.class).save();
                mb.name("setShowGrid").paramTypes(boolean.class).returnType(void.class).save();
                mb.name("getGridSpacing").returnType(double.class).save();
                mb.name("setGridSpacing").paramTypes(double.class).returnType(void.class).save();
                return mb.name("setPenColor").paramTypes(snap.gfx.Color.class).returnType(void.class).buildAll();

            // Handle snapcode.repl.QuickDrawPen
            case "snapcode.repl.QuickDrawPen":
                mb.name("getColor").returnType(snap.gfx.Color.class).save();
                mb.name("setColor").paramTypes(snap.gfx.Color.class).returnType(void.class).save();
                mb.name("getWidth").returnType(double.class).save();
                mb.name("setWidth").paramTypes(double.class).returnType(void.class).save();
                mb.name("moveTo").paramTypes(double.class,double.class).returnType(void.class).save();
                mb.name("lineTo").paramTypes(double.class,double.class).returnType(void.class).save();
                mb.name("closePath").returnType(void.class).save();
                mb.name("forward").paramTypes(double.class).returnType(void.class).save();
                mb.name("turn").paramTypes(double.class).returnType(void.class).save();
                mb.name("getAnimPen").returnType(QuickDrawPen.class).save();
                mb.name("getDirection").returnType(double.class).save();
                return mb.name("setDirection").paramTypes(double.class).returnType(void.class).buildAll();

            // Handle anything else
            default:
                if (_next != null) return _next.getMethodsForClass(aResolver, aClassName);
                return new JavaMethod[0];
        }
    }

    /**
     * Invokes methods for given method id, object and args.
     */
    public Object invokeMethod(String anId, Object anObj, Object ... theArgs) throws Exception
    {
        switch (anId) {

            // Handle snapcharts.data.DoubleArray
            case "snapcharts.data.DoubleArray.clone()":
                return ((snapcharts.data.DoubleArray) anObj).clone();
            case "snapcharts.data.DoubleArray.toArray()":
                return ((snapcharts.data.DoubleArray) anObj).toArray();
            case "snapcharts.data.DoubleArray.filter(java.util.function.DoublePredicate)":
                return ((snapcharts.data.DoubleArray) anObj).filter((java.util.function.DoublePredicate) theArgs[0]);
            case "snapcharts.data.DoubleArray.map(java.util.function.DoubleUnaryOperator)":
                return ((snapcharts.data.DoubleArray) anObj).map((java.util.function.DoubleUnaryOperator) theArgs[0]);
            case "snapcharts.data.DoubleArray.of(java.lang.Object[])":
                return snapcharts.data.DoubleArray.of(theArgs);
            case "snapcharts.data.DoubleArray.fromMinMax(double,double)":
                return snapcharts.data.DoubleArray.fromMinMax(doubleVal(theArgs[0]),doubleVal(theArgs[1]));
            case "snapcharts.data.DoubleArray.fromMinMaxCount(double,double,int)":
                return snapcharts.data.DoubleArray.fromMinMaxCount(doubleVal(theArgs[0]),doubleVal(theArgs[1]),intVal(theArgs[2]));

            // Handle snapcharts.data.DataArray
            case "snapcharts.data.DataArray.length()":
                return ((snapcharts.data.DataArray) anObj).length();
            case "snapcharts.data.DataArray.getName()":
                return ((snapcharts.data.DataArray) anObj).getName();

            // Handle snapcharts.data.DataSet
            case "snapcharts.data.DataSet.clone()":
                return ((snapcharts.data.DataSet) anObj).clone();
            case "snapcharts.data.DataSet.getName()":
                return ((snapcharts.data.DataSet) anObj).getName();

            // Handle snapcode.repl.ReplObject
            case "snapcode.repl.ReplObject.println(java.lang.Object)":
                ReplObject.println(theArgs[0]); return null;
            case "snapcode.repl.ReplObject.print(java.lang.Object)":
                ReplObject.print(theArgs[0]); return null;

            // Handle snapcode.repl.Quick3D
            case "snapcode.repl.Quick3D.createCube()":
                return Quick3D.createCube();
            case "snapcode.repl.Quick3D.createImage3D(snap.gfx.Image)":
                return Quick3D.createImage3D((snap.gfx.Image) theArgs[0]);

            // Handle snapcode.repl.QuickCharts
            case "snapcode.repl.QuickCharts.chart(java.lang.Object[])":
                return QuickCharts.chart(theArgs);
            case "snapcode.repl.QuickCharts.chart3D(java.lang.Object[])":
                return QuickCharts.chart3D(theArgs);

            // Handle snapcode.repl.QuickData
            case "snapcode.repl.QuickData.dataArray(java.lang.Object)":
                return QuickData.dataArray(theArgs[0]);
            case "snapcode.repl.QuickData.dataSet(java.lang.Object[])":
                return QuickData.dataSet(theArgs);
            case "snapcode.repl.QuickData.mapXY(double[],double[],java.util.function.DoubleBinaryOperator)":
                return QuickData.mapXY((double[]) theArgs[0],(double[]) theArgs[1],(java.util.function.DoubleBinaryOperator) theArgs[2]);
            case "snapcode.repl.QuickData.mapXY(snapcharts.data.DoubleArray,snapcharts.data.DoubleArray,java.util.function.DoubleBinaryOperator)":
                return QuickData.mapXY((snapcharts.data.DoubleArray) theArgs[0],(snapcharts.data.DoubleArray) theArgs[1],(java.util.function.DoubleBinaryOperator) theArgs[2]);
            case "snapcode.repl.QuickData.doubleArray(java.lang.Object[])":
                return QuickData.doubleArray(theArgs);
            case "snapcode.repl.QuickData.minMaxArray(double,double)":
                return QuickData.minMaxArray(doubleVal(theArgs[0]),doubleVal(theArgs[1]));
            case "snapcode.repl.QuickData.minMaxArray(double,double,int)":
                return QuickData.minMaxArray(doubleVal(theArgs[0]),doubleVal(theArgs[1]),intVal(theArgs[2]));
            case "snapcode.repl.QuickData.getTextForSource(java.lang.Object)":
                return QuickData.getTextForSource(theArgs[0]);
            case "snapcode.repl.QuickData.getImageForSource(java.lang.Object)":
                return QuickData.getImageForSource(theArgs[0]);

            // Handle snapcode.repl.QuickDraw
            case "snapcode.repl.QuickDraw.isShowGrid()":
                return ((QuickDraw) anObj).isShowGrid();
            case "snapcode.repl.QuickDraw.isAnimate()":
                return ((QuickDraw) anObj).isAnimate();
            case "snapcode.repl.QuickDraw.setAnimate(boolean)":
                ((QuickDraw) anObj).setAnimate(boolVal(theArgs[0])); return null;
            case "snapcode.repl.QuickDraw.getPen(int)":
                return ((QuickDraw) anObj).getPen(intVal(theArgs[0]));
            case "snapcode.repl.QuickDraw.getPen()":
                return ((QuickDraw) anObj).getPen();
            case "snapcode.repl.QuickDraw.moveTo(double,double)":
                ((QuickDraw) anObj).moveTo(doubleVal(theArgs[0]),doubleVal(theArgs[1])); return null;
            case "snapcode.repl.QuickDraw.lineTo(double,double)":
                ((QuickDraw) anObj).lineTo(doubleVal(theArgs[0]),doubleVal(theArgs[1])); return null;
            case "snapcode.repl.QuickDraw.closePath()":
                ((QuickDraw) anObj).closePath(); return null;
            case "snapcode.repl.QuickDraw.forward(double)":
                ((QuickDraw) anObj).forward(doubleVal(theArgs[0])); return null;
            case "snapcode.repl.QuickDraw.turn(double)":
                ((QuickDraw) anObj).turn(doubleVal(theArgs[0])); return null;
            case "snapcode.repl.QuickDraw.createDrawView(int,int)":
                return QuickDraw.createDrawView(intVal(theArgs[0]),intVal(theArgs[1]));
            case "snapcode.repl.QuickDraw.createDrawView()":
                return QuickDraw.createDrawView();
            case "snapcode.repl.QuickDraw.setShowGrid(boolean)":
                ((QuickDraw) anObj).setShowGrid(boolVal(theArgs[0])); return null;
            case "snapcode.repl.QuickDraw.getGridSpacing()":
                return ((QuickDraw) anObj).getGridSpacing();
            case "snapcode.repl.QuickDraw.setGridSpacing(double)":
                ((QuickDraw) anObj).setGridSpacing(doubleVal(theArgs[0])); return null;
            case "snapcode.repl.QuickDraw.setPenColor(snap.gfx.Color)":
                ((QuickDraw) anObj).setPenColor((snap.gfx.Color) theArgs[0]); return null;

            // Handle snapcode.repl.QuickDrawPen
            case "snapcode.repl.QuickDrawPen.getColor()":
                return ((QuickDrawPen) anObj).getColor();
            case "snapcode.repl.QuickDrawPen.setColor(snap.gfx.Color)":
                ((QuickDrawPen) anObj).setColor((snap.gfx.Color) theArgs[0]); return null;
            case "snapcode.repl.QuickDrawPen.getWidth()":
                return ((QuickDrawPen) anObj).getWidth();
            case "snapcode.repl.QuickDrawPen.setWidth(double)":
                ((QuickDrawPen) anObj).setWidth(doubleVal(theArgs[0])); return null;
            case "snapcode.repl.QuickDrawPen.moveTo(double,double)":
                ((QuickDrawPen) anObj).moveTo(doubleVal(theArgs[0]),doubleVal(theArgs[1])); return null;
            case "snapcode.repl.QuickDrawPen.lineTo(double,double)":
                ((QuickDrawPen) anObj).lineTo(doubleVal(theArgs[0]),doubleVal(theArgs[1])); return null;
            case "snapcode.repl.QuickDrawPen.closePath()":
                ((QuickDrawPen) anObj).closePath(); return null;
            case "snapcode.repl.QuickDrawPen.forward(double)":
                ((QuickDrawPen) anObj).forward(doubleVal(theArgs[0])); return null;
            case "snapcode.repl.QuickDrawPen.turn(double)":
                ((QuickDrawPen) anObj).turn(doubleVal(theArgs[0])); return null;
            case "snapcode.repl.QuickDrawPen.getAnimPen()":
                return ((QuickDrawPen) anObj).getAnimPen();
            case "snapcode.repl.QuickDrawPen.getDirection()":
                return ((QuickDrawPen) anObj).getDirection();
            case "snapcode.repl.QuickDrawPen.setDirection(double)":
                ((QuickDrawPen) anObj).setDirection(doubleVal(theArgs[0])); return null;

            // Handle anything else
            default:
                if (_next != null) return _next.invokeMethod(anId, anObj, theArgs);
                throw new NoSuchMethodException("Unknown method: " + anId);
        }
    }

    /**
     * Returns the declared constructors for given class.
     */
    public JavaConstructor[] getConstructorsForClass(Resolver aResolver, String aClassName)
    {
        cb.init(aResolver, aClassName);

        switch (aClassName) {

            // Handle snapcharts.data.DoubleArray
            case "snapcharts.data.DoubleArray":
                return cb.paramTypes(double[].class).buildAll();

            // Handle snapcode.repl.QuickDraw
            case "snapcode.repl.QuickDraw":
                return cb.paramTypes(int.class,int.class).buildAll();

            // Handle anything else
            default:
                if (_next != null) return _next.getConstructorsForClass(aResolver, aClassName);
                return cb.save().buildAll();
        }
    }

    /**
     * Invokes constructors for given constructor id and args.
     */
    public Object invokeConstructor(String anId, Object ... theArgs) throws Exception
    {
        switch (anId) {

            // Handle snapcharts.data.DoubleArray
            case "snapcharts.data.DoubleArray(double[])":
                return new snapcharts.data.DoubleArray((double[]) theArgs[0]);

            // Handle snapcode.repl.QuickDraw
            case "snapcode.repl.QuickDraw(int,int)":
                return new QuickDraw(intVal(theArgs[0]),intVal(theArgs[1]));

            // Handle anything else
            default:
                if (_next != null) return _next.invokeConstructor(anId, theArgs);
                throw new NoSuchMethodException("Unknown constructor: " + anId);
        }
    }
}
