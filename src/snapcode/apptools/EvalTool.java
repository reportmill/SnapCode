package snapcode.apptools;
import javakit.ide.JavaTextArea;
import javakit.parse.JMethodDecl;
import javakit.parse.JNode;
import snap.gfx.Color;
import snap.gfx.Image;
import snap.text.TextBoxLine;
import snap.view.*;
import snap.viewx.TextPane;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;

/**
 * This class manages the run/eval UI.
 */
public class EvalTool extends WorkspaceTool {

    // Whether to auto run code
    private boolean  _autoRun;

    // Whether auto run was requested
    protected boolean  _autoRunRequested;

    // EvalRunner
    protected EvalToolRunner  _evalRunner;

    // EvalView
    protected EvalView  _evalView;

    // For resetEntriesLater
    private Runnable  _resetEvalValuesRun;

    // For resetEntriesLater
    private Runnable  _resetEvalValuesRunReal = () -> runAppNow();

    /**
     * Constructor.
     */
    public EvalTool(WorkspacePane workspacePane)
    {
        super(workspacePane);

        // Create runner
        _evalRunner = new EvalToolRunner(this);
    }

    /**
     * Returns whether to automatically run when enter key is pressed in edit pane.
     */
    public boolean isAutoRun()  { return _autoRun; }

    /**
     * Sets whether to automatically run when enter key is pressed in edit pane.
     */
    public void setAutoRun(boolean aValue)  { _autoRun = aValue; }

    /**
     * Runs Java code.
     */
    public void runApp(boolean autoRunRequested)
    {
        _autoRunRequested = autoRunRequested;
        if (_resetEvalValuesRun == null)
            runLater(_resetEvalValuesRun = _resetEvalValuesRunReal);
        resetLater();
    }

    /**
     * Reset Repl values.
     */
    protected void runAppNow()
    {
        try { _evalRunner.runApp(); }
        finally {
            _resetEvalValuesRun = null;
            _autoRunRequested = false;
        }
    }

    /**
     * Whether run is running.
     */
    public boolean isRunning()  { return _evalRunner.isRunning(); }

    /**
     * Cancels run.
     */
    public void cancelRun()
    {
        _evalRunner.cancelRun();
        resetLater();
    }

    /**
     * Clear eval values.
     */
    public void clearEvalValues()
    {
        _evalView.resetDisplay();
    }

    /**
     * Triggers auto run if it makes sense. Called when newline is entered.
     */
    public void autoRunIfDesirable(JavaTextArea textArea)
    {
        // If AutoRun not set, just return
        if (!isAutoRun())
            return;

        // If inside method decl, just return
        JNode selNode = textArea.getSelNode();
        if (selNode != null && selNode.getParent(JMethodDecl.class) != null)
            return;

        // If previous line is empty whitespace, just return
        TextBoxLine textLine = textArea.getSel().getStartLine();
        TextBoxLine prevLine = textLine.getPrevious();
        if (prevLine.isWhiteSpace())
            return;

        // Trigger auto run
        runApp(true);
    }

    /**
     * Override to add EvalView.
     */
    @Override
    protected View createUI()
    {
        // Do normal version
        ColView colView = (ColView) super.createUI();

        // Add separator
        RectView separator = new RectView();
        separator.setPrefSize(1, 1);
        separator.setFill(Color.GRAY8);
        colView.addChild(separator);

        // Create/config EvalView
        _evalView = new EvalView(this);
        _evalView.setGrowHeight(true);

        // Create/config ScrollView
        ScrollView scrollView = new ScrollView(_evalView);
        scrollView.setBorder(null);
        scrollView.setFillWidth(true);
        scrollView.setGrowHeight(true);
        colView.addChild(scrollView);

        // Return
        return colView;
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Set DeleteButton image
        getView("RunButton", ButtonBase.class).setImageAfter(getImage("pkg.images/Run.png"));
        getView("RunButton", ButtonBase.class).getLabel().setTextFill(Color.GRAY);
        getView("DeleteButton", ButtonBase.class).setImage(Image.get(TextPane.class, "pkg.images/Edit_Delete.png"));
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Update RunButton, AutoRunCheckBox
        setViewText("RunButton", !_evalRunner.isRunning() ? "Run" : "Cancel");
        setViewValue("AutoRunCheckBox", isAutoRun());
    }

    /**
     * Respond to UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle RunButton
        if (anEvent.equals("RunButton")) {
            if (!isRunning())
                runApp(false);
            else cancelRun();
        }

        // Handle AutoRunCheckBox
        if (anEvent.equals("AutoRunCheckBox")) {
            setAutoRun(!isAutoRun());
            if (isAutoRun())
                runApp(true);
        }

        // Handle DeleteButton
        if (anEvent.equals("DeleteButton"))
            clearEvalValues();
    }

    /**
     * Title.
     */
    @Override
    public String getTitle()  { return "Run / Debug"; }
}
