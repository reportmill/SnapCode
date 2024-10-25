package snapcode.app;
import snap.view.TextArea;
import snap.viewx.TextPane;

/**
 * A TextPane subclass to edit Java Markdown.
 */
public class JMDPane extends TextPane {

    /**
     * Constructor.
     */
    public JMDPane()
    {
        super();
    }

    @Override
    protected TextArea createTextArea()
    {
        TextArea textArea = super.createTextArea();
        textArea.setPadding(5,5, 5,5);
        textArea.setSyncTextFont(false);
        return textArea;
    }
}
