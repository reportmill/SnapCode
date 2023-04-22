package snapcode.webbrowser;
import snap.view.ParentView;
import snap.view.View;
import snap.view.ViewArchiver;
import snap.web.WebFile;
import snapbuild.app.EditorPane;
import snapbuild.app.EditorUtils;

/**
 * Provides an EditorPane as WebPage.
 */
public class SnapBuilderPage extends WebPage {

    // The EditorPane
    private EditorPane _editorPane;

    /**
     * Constructor.
     */
    public SnapBuilderPage()
    {
        super();
        _editorPane = new EditorPane();
    }

    /**
     * Override to wrap ReportPage in pane with EditButton.
     */
    protected View createUI()
    {
        // Set file in EditorPane
        WebFile file = getFile();
        _editorPane.openSource(file);

        // Return EditorPane.UI
        return _editorPane.getUI();
    }

    /**
     * Creates a new file for use with showNewFilePanel method.
     */
    protected WebFile createNewFile(String aPath)
    {
        // Create file
        WebFile newFile = super.createNewFile(aPath);

        // Create text
        ParentView newDocView = EditorUtils.createNewDocView();
        ViewArchiver viewArchiver = new ViewArchiver();
        String fileText = viewArchiver.writeToXML(newDocView).getString();
        newFile.setText(fileText);

        // Return
        return newFile;
    }
}