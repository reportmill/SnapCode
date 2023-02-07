package snapcode.app;
import snap.view.Label;
import snap.view.View;
import snap.viewx.WebPage;

/**
 * A WebPage for Playground.
 */
public class JavaShellPage extends WebPage {

    // The Playground
    //JavaShellPane _javaShellPane = new JavaShellPane();

    /**
     * Creates a new PGPage.
     */
    public JavaShellPage()
    {
    }

    /**
     * Returns the Playground.
     */
    //public JavaShellPane getPlayground()  { return _javaShellPane; }

    /**
     * Creates UI panel.
     */
    protected View createUI()  { return new Label("Swap in JavaShell here"); }
    //{ return _javaShellPane.getUI(); }

}