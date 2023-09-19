package snapcode.apptools;
import javakit.parse.NodeError;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.text.TextBlock;
import snap.text.TextStyle;
import snapcharts.repl.DefaultConsole;
import snapcode.project.BuildIssue;
import snapcode.util.ExceptionUtil;

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

        // Handle NodeError, BuildIssue: Map to error TextBlock
        if (aValue instanceof NodeError)
            aValue = createTextBlockForNodeErrors(new NodeError[] { (NodeError) aValue });
        else if (aValue instanceof NodeError[])
            aValue = createTextBlockForNodeErrors((NodeError[]) aValue);
        else if (aValue instanceof BuildIssue[])
            aValue = createTextBlockForBuildIssues((BuildIssue[]) aValue);

        // Handle Exception
        else if (aValue instanceof Exception)
            aValue = ExceptionUtil.getTextBlockForException((Exception) aValue);

        // Do normal version
        super.show(aValue);
    }

    /**
     * Creates TextBlock for NodeErrors.
     */
    private static TextBlock createTextBlockForNodeErrors(NodeError[] nodeErrors)
    {
        // Get exception string
        String errorString = "";
        for (int i = 0; i < nodeErrors.length; i++) {
            errorString += "Error: " + nodeErrors[i].getString();
            if (i + 1 < nodeErrors.length)
                errorString += '\n';
        }

        // Return view for error string
        return createTextBlockForErrorString(errorString);
    }

    /**
     * Creates TextBlock for BuildIssues.
     */
    private static TextBlock createTextBlockForBuildIssues(BuildIssue[] buildIssues)
    {
        // Get error string
        String errorString = "";
        for (int i = 0; i < buildIssues.length; i++) {
            errorString += "Error: " + buildIssues[i].getText();
            if (i + 1 < buildIssues.length)
                errorString += '\n';
        }

        // Return view for error string
        return createTextBlockForErrorString(errorString);
    }

    /**
     * Creates TextBlock for BuildIssues.
     */
    private static TextBlock createTextBlockForErrorString(String errorString)
    {
        // Create TextArea
        TextBlock textBlock = new TextBlock();
        Color ERROR_COLOR = Color.get("#CC0000");
        TextStyle textStyle = textBlock.getStyleForCharIndex(0);
        TextStyle textStyle2 = textStyle.copyFor(ERROR_COLOR).copyFor(Font.Arial12);
        textBlock.setDefaultStyle(textStyle2);
        if (textBlock.isRichText())
            textBlock.setStyle(textStyle2, 0, textBlock.length());

        // Add chars
        textBlock.addChars(errorString);

        // Return
        return textBlock;
    }
}
