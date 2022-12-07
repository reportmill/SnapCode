/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import javakit.ide.*;
import javakit.parse.JClassDecl;
import javakit.parse.JMethodDecl;
import javakit.parse.JNode;
import javakit.parse.JeplTextDoc;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.props.PropChange;
import snap.text.TextBoxLine;
import snap.text.TextDoc;
import snap.text.TextStyle;
import snap.util.CharSequenceUtils;
import snap.view.*;
import snap.viewx.TextPane;
import java.util.Objects;

/**
 * This TextPane subclass adds customizations for JavaShell.
 */
public class EditPane extends TextPane {

    // The DocPane
    private DocPane  _docPane;

    // The JeplTextDoc
    protected JeplTextDoc  _jeplDoc;

    // The TextArea
    private JavaTextArea  _textArea;

    // LineNumView
    private LineHeaderView  _lineNumView;

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
     * Returns the JeplTextDoc.
     */
    public JeplTextDoc getJeplDoc()  { return _jeplDoc; }

    /**
     * Sets the JeplTextDoc.
     */
    public void setJeplDoc(JeplTextDoc aJeplDoc)
    {
        _jeplDoc = aJeplDoc;
        _jeplDoc.addPropChangeListener(pc -> textDocDidPropChange(pc));
    }

    /**
     * Returns the default font.
     */
    public Font getCodeFont()  { return JavaTextUtils.getCodeFont(); }

    /**
     * Override to return a JavaTextArea.
     */
    public JavaTextArea getTextArea()  { return (JavaTextArea) super.getTextArea(); }

    /**
     * Creates the TextArea.
     */
    protected TextArea createTextArea()
    {
        JavaTextArea textArea = new JavaTextArea();
        textArea.setShowPrintMargin(false);
        textArea.setFocusPainted(true);
        return textArea;
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        // Do normal version
        super.initUI();

        // Basic config
        BorderView borderView = getUI(BorderView.class);
        borderView.setGrowHeight(true);
        borderView.getBottom().setBorder(Color.GRAY9, 1);

        // Configure ToolBar
        RowView toolBar = (RowView) borderView.getTop();
        toolBar.setPadding(6, 6, 6, 6);
        toolBar.setSpacing(5);

        // Get/configure TextArea
        _textArea = getTextArea();
        _textArea.setGrowWidth(true);
        enableEvents(_textArea, KeyPress);
        _textArea.addPropChangeListener(pc -> textAreaDidPropChange(pc), JavaTextArea.SelectedNode_Prop);

        // Create/config LineNumView
        _lineNumView = new LineHeaderView(null, _textArea);
        _lineNumView.setShowLineMarkers(false);
        Font codeFont = getCodeFont();
        //_lineNumView.setDefaultStyle(_lineNumView.getDefaultStyle().copyFor(codeFont));
        //_lineNumView.updateLines();

        // Create ScrollGroup for TextArea and LineNumView
        ScrollGroup scrollGroup = new ScrollGroup();
        scrollGroup.setBorder(Color.GRAY9, 1);
        scrollGroup.setGrowWidth(true);
        scrollGroup.setContent(_textArea);
        scrollGroup.setLeftView(_lineNumView);
        scrollGroup.setMinWidth(200);

        // Replace TextPane center with scrollGroup
        borderView.setCenter(scrollGroup);

        // Get/set JeplTextDoc
        JeplTextDoc jeplDoc = _docPane.getJeplDoc();
        jeplDoc.setDefaultStyle(new TextStyle(codeFont));
        setJeplDoc(jeplDoc);
        _textArea.setTextDoc(jeplDoc);
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Do normal version
        super.resetUI();

        // Reset NodePathBox
        resetNodePathBox();
    }

    /**
     * Resets the NodePath box.
     */
    private void resetNodePathBox()
    {
        // Clear path box and add Lin/Col position label
        RowView nodePathBox = getView("BottomBox", RowView.class);
        while (nodePathBox.getChildCount() > 1)
            nodePathBox.removeChild(1);

        // Get Path node labels
        Label[] pathNodeLabels = getLabelsForSelNodePath();
        for (Label pathNodeLabel : pathNodeLabels) {
            pathNodeLabel.setOwner(this);
            enableEvents(pathNodeLabel, MouseRelease);
            nodePathBox.addChild(pathNodeLabel);
        }
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


        // Handle NodePathLabel
        else if (anEvent.equals("NodePathLabel")) {
            JNode clickedNode = (JNode) anEvent.getView().getProp("JNode");
            JavaTextArea javaTextArea = getTextArea();
            JNode deepNode = javaTextArea.getDeepNode();
            javaTextArea.setSel(clickedNode.getStartCharIndex(), clickedNode.getEndCharIndex());
            _textArea.setDeepNode(deepNode);
        }

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
     * Override to add trailing colon.
     */
    @Override
    public String getSelectionInfo()  { return super.getSelectionInfo() + ": "; }

    /**
     * Returns labels for
     */
    private Label[] getLabelsForSelNodePath()
    {
        // Get JavaTextPane version
        JavaTextArea javaTextArea = getTextArea();
        Label[] pathNodeLabels = JavaTextPane.getLabelsForSelNodePath(javaTextArea, JClassDecl.class);

        // If last label is ClassLabel, reconfigure
        Label lastLabel = pathNodeLabels[pathNodeLabels.length - 1];
        if (Objects.equals(lastLabel.getName(), "ClassLabel")) {

            // Get JavaDoc URL for sel node
            JNode selNode = javaTextArea.getSelNode();
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
     * Called when TextDoc does prop change.
     */
    private void textDocDidPropChange(PropChange aPC)
    {
        // Get PropName
        String propName = aPC.getPropName();

        // Handle CharsChange
        if (propName == TextDoc.Chars_Prop) {
            CharSequence chars = (CharSequence) (aPC.getNewValue() != null ? aPC.getNewValue() : aPC.getOldValue());
            if (CharSequenceUtils.indexOfNewline(chars, 0) >= 0)
                _lineNumView.repaint();
        }

        // Handle DefaultTextStyle, ParentTextStyle changes (reset to update font size)
        else if (propName == TextDoc.DefaultTextStyle_Prop || propName == TextDoc.ParentTextStyle_Prop)
            _lineNumView.resetAll();
    }

    /**
     * Called when JavaTextArea does prop change.
     */
    private void textAreaDidPropChange(PropChange aPC)
    {
        String propName = aPC.getPropName();

        if (propName == JavaTextArea.SelectedNode_Prop)
            resetLater();
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