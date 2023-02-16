/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.appjr;
import javakit.ide.*;
import javakit.parse.*;
import snap.gfx.Color;
import snap.view.*;

/**
 * This JavaTextPane subclass adds customizations for JavaShell.
 */
public class EditPane<T extends JavaTextDoc> extends JavaTextPane<T> {

    // The DocPane
    private DocPane  _docPane;

    /**
     * Constructor.
     */
    public EditPane(DocPane aDocPane)
    {
        super();
        _docPane = aDocPane;
    }

    /**
     * Returns the EvalPane.
     */
    public EvalPane getEvalPane()  { return _docPane.getEvalPane(); }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        // Do normal version
        super.initUI();

        // Basic config
        BorderView borderView = getUI(BorderView.class);
        borderView.getBottom().setBorder(Color.GRAY9, 1);

        // Hide LineFootView
        borderView.setRight(null);
    }

    /**
     * Respond to UI changes.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle KeyPress
        if (anEvent.isKeyPress()) {

            // Handle Enter key: Re-evaluate if not inside method
            if (anEvent.isEnterKey()) {
                EvalPane evalPane = getEvalPane();
                evalPane.autoRunIfDesirable(getTextArea());
            }

            // Handle Backspace/Delete: Clear EvalPane
            if ((anEvent.isBackSpaceKey() || anEvent.isDeleteKey()) && _textArea.length() == 0)
                getEvalPane().clearEvalValues();
        }

        // Handle SaveButton
        else if (anEvent.equals("SaveButton"))
            _docPane.save();

        // Handle JavaDocLabel
        else if (anEvent.equals("JavaDocLabel")) {
            JavaTextArea javaTextArea = getTextArea();
            JNode selNode = javaTextArea.getSelNode();
            JavaDoc javaDoc = JavaDoc.getJavaDocForNode(selNode);
            if (javaDoc != null)
                javaDoc.openUrl();
        }

        // Do normal version
        else super.respondUI(anEvent);
    }
}