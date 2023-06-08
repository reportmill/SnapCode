package snapcode.apptools;
import javakit.parse.NodeError;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.text.TextDoc;
import snap.text.TextStyle;
import snapcharts.repl.DefaultConsole;
import snapcode.project.BuildIssue;

/**
 * This REPL Console subclass allows customization.
 */
public class EvalToolConsole extends DefaultConsole {

    // The EvalTool that owns this console
    private EvalTool _evalTool;

    // Constants
    public static final int MAX_OUTPUT_COUNT = 1000;

    /**
     * Constructor.
     */
    public EvalToolConsole(EvalTool evalTool)
    {
        super();
        _evalTool = evalTool;
    }

    /**
     * Called by shell when there is output.
     */
    @Override
    public void show(Object aValue)
    {
        // If too much output, bail
        if (getItemCount() > MAX_OUTPUT_COUNT) {
            boolean isRunning = _evalTool.isRunning();
            if (!isRunning)
                return;
            _evalTool.cancelRun();
            return;
        }

        // Handle NodeError, BuildIssue: Map to error TextDoc
        if (aValue instanceof NodeError)
            aValue = createTextDocForNodeErrors(new NodeError[] { (NodeError) aValue });
        else if (aValue instanceof NodeError[])
            aValue = createTextDocForNodeErrors((NodeError[]) aValue);
        else if (aValue instanceof BuildIssue[])
            aValue = createTextDocForBuildIssues((BuildIssue[]) aValue);

        // Do normal version
        super.show(aValue);
    }

    /**
     * Creates content view for ViewOwner.
     */
    private static TextDoc createTextDocForNodeErrors(NodeError[] nodeErrors)
    {
        // Get exception string
        String errorString = "";
        for (int i = 0; i < nodeErrors.length; i++) {
            errorString += "Error: " + nodeErrors[i].getString();
            if (i + 1 < nodeErrors.length)
                errorString += '\n';
        }

        // Return view for error string
        return createTextDocForErrorString(errorString);
    }

    /**
     * Creates content view for BuildIssues.
     */
    private static TextDoc createTextDocForBuildIssues(BuildIssue[] buildIssues)
    {
        // Get error string
        String errorString = "";
        for (int i = 0; i < buildIssues.length; i++) {
            errorString += "Error: " + buildIssues[i].getText();
            if (i + 1 < buildIssues.length)
                errorString += '\n';
        }

        // Return view for error string
        return createTextDocForErrorString(errorString);
    }

    /**
     * Creates content view for BuildIssues.
     */
    private static TextDoc createTextDocForErrorString(String errorString)
    {
        // Create TextArea
        TextDoc textDoc = new TextDoc();
        Color ERROR_COLOR = Color.get("#CC0000");
        TextStyle textStyle = textDoc.getStyleForCharIndex(0);
        TextStyle textStyle2 = textStyle.copyFor(ERROR_COLOR).copyFor(Font.Arial12);
        textDoc.setDefaultStyle(textStyle2);
        if (textDoc.isRichText())
            textDoc.setStyle(textStyle2, 0, textDoc.length());

        // Add chars
        textDoc.addChars(errorString);

        // Return
        return textDoc;
    }
}
