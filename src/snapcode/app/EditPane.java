/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import javakit.ide.*;
import javakit.parse.*;
import snap.gfx.Color;
import snap.text.TextBoxLine;
import snap.view.*;
import java.util.Objects;

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
            if (anEvent.isEnterKey())
                autoRunIfDesirable();

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

    /**
     * Returns labels for
     */
    @Override
    protected Label[] getLabelsForSelNodePath()
    {
        // Get JavaTextPane version
        Label[] pathNodeLabels = JavaTextPane.getLabelsForSelNodePath(_textArea, JClassDecl.class);

        // If last label is ClassLabel, reconfigure
        Label lastLabel = pathNodeLabels[pathNodeLabels.length - 1];
        if (Objects.equals(lastLabel.getName(), "ClassLabel")) {

            // Get JavaDoc URL for sel node
            JNode selNode = _textArea.getSelNode();
            JavaDoc javaDoc = JavaDoc.getJavaDocForNode(selNode);

            // If JavaDoc found, modify label
            if (javaDoc != null) {
                lastLabel.setName("JavaDocLabel");
                lastLabel.setToolTip("Open JavaDoc");
            }
        }

        // Return
        return pathNodeLabels;
    }

    /**
     * Triggers auto run if it makes sense. Called when newline is entered.
     */
    private void autoRunIfDesirable()
    {
        // If EvalPane.AutoRun not set, just return
        EvalPane evalPane = getEvalPane();
        if (!evalPane.isAutoRun())
            return;

        // If inside method decl, just return
        JavaTextArea textArea = getTextArea();
        JNode selNode = textArea.getSelNode();
        if (selNode != null && selNode.getParent(JMethodDecl.class) != null)
            return;

        // If previous line is empty whitespace, just return
        TextBoxLine textLine = textArea.getSel().getStartLine();
        TextBoxLine prevLine = textLine.getPrevious();
        if (prevLine.isWhiteSpace())
            return;

        // Trigger auto run
        evalPane.runApp(true);
    }
}