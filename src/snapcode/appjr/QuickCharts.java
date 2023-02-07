/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.appjr;
import snapcharts.data.DataSet;
import snapcharts.model.Chart;
import snapcharts.model.Trace;
import snapcharts.model.TraceType;
import snapcharts.modelx.Contour3DTrace;

/**
 * This is a REPL base class specific for charts.
 */
public class QuickCharts extends QuickData {

    /**
     * Creates and returns a Chart.
     */
    public static Chart chart(Object ... theObjects)
    {
        // Get DataSet from theObjects
        DataSet dataSet = dataSet(theObjects);

        // Get TraceType
        TraceType traceType = TraceType.Scatter;
        if (dataSet.getDataType().hasZ())
            traceType = TraceType.Contour;

        // Create Trace for DataSet
        Trace trace = Trace.newTraceForClass(traceType.getTraceClass());
        trace.setDataSet(dataSet);

        // Create Chart with Trace
        Chart chart = new Chart();
        chart.getAxisX().setTitle("X");
        chart.getAxisY().setTitle("Y");
        chart.addTrace(trace);

        // Set title from DataSet.Name (or type)
        String dataSetName = dataSet.getName();
        if (dataSetName == null) dataSetName = dataSet.getDataType() + " Data";
        chart.getHeader().setTitle("Chart of " + dataSetName);

        // Return
        return chart;
    }

    /**
     * Creates and returns a Plot3D.
     */
    public static Chart chart3D(Object ... theObjects)
    {
        // Get DataSet from theObjects
        DataSet dataSet = dataSet(theObjects);

        // Create Trace for DataSet
        Trace trace = new Contour3DTrace();
        trace.setDataSet(dataSet);

        // Create Chart with Trace
        Chart chart = new Chart();
        chart.getAxisX().setTitle("X");
        chart.getAxisY().setTitle("Y");
        chart.getAxisZ().setTitle("Z");
        chart.addTrace(trace);

        // Set title from DataSet.Name (or type)
        String dataSetName = dataSet.getName();
        if (dataSetName == null) dataSetName = dataSet.getDataType() + " Data";
        chart.getHeader().setTitle("Chart of " + dataSetName);

        // Return
        return chart;
    }

    /**
     * Conveniences.
     */
    public static Chart plot(Object ... theObjects)  { return chart(theObjects); }
    public static Chart plot3D(Object ... theObjects)  { return chart3D(theObjects); }
}
