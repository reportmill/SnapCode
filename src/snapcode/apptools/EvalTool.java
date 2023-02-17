package snapcode.apptools;
import javakit.ide.JavaTextArea;
import javakit.ide.JavaTextPane;
import javakit.parse.JMethodDecl;
import javakit.parse.JNode;
import snap.gfx.Color;
import snap.gfx.Image;
import snap.text.TextBoxLine;
import snap.view.*;
import snap.viewx.TextPane;
import snap.viewx.WebPage;
import snapcode.app.JavaPage;
import snapcode.app.PagePane;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import snapcode.appjr.EvalView;

/**
 * This class manages the run/eval UI.
 */
public class EvalTool extends WorkspaceTool {

    // Whether to auto run code
    private boolean  _autoRun;

    // Whether auto run was requested
    private boolean  _autoRunRequested;

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
        try { _evalView.runApp(_autoRunRequested); }
        finally {
            _resetEvalValuesRun = null;
            _autoRunRequested = false;
        }

        //_docPane.hideDrawer();
    }

    /**
     * Whether run is running.
     */
    public boolean isRunning()  { return _evalView.isRunning(); }

    /**
     * Cancels run.
     */
    public void cancelRun()
    {
        _evalView.cancelRun();
        resetLater();
    }

    /**
     * Clear eval values.
     */
    public void clearEvalValues()
    {
        _evalView.removeChildren();
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

        // Create/config EvalView
        _evalView = new EvalView(this) {
            @Override
            public JavaTextPane<?> getJavaTextPane()
            {
                PagePane pagePane = _workspacePane.getPagePane();
                WebPage selPage = pagePane.getSelPage();
                JavaPage javaPage = selPage instanceof JavaPage ? (JavaPage) selPage : null;
                return javaPage != null ? javaPage.getTextPane() : null;
            }
        };
        _evalView.setGrowHeight(true);

        // Create/config ScrollView
        ScrollView scrollView = new ScrollView(_evalView);
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
        setViewText("RunButton", !_evalView.isRunning() ? "Run" : "Cancel");
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
            _evalView.removeChildren();

        // Handle SamplesButton
        //if (anEvent.equals("SamplesButton")) _docPane.showSamples();
    }

    /**
     * Title.
     */
    @Override
    public String getTitle()  { return "Run / Debug"; }
}
