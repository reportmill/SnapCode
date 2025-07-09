/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import javakit.parse.*;
import snap.text.TextModel;
import snapcode.project.BuildIssue;
import snapcode.project.JavaAgent;
import snapcode.project.JavaTextModel;
import javakit.resolver.JavaDecl;
import snap.gfx.*;
import snap.props.PropChange;
import snap.util.*;
import snap.view.*;
import snap.viewx.TextPane;
import snapcode.project.JavaTextUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A panel for editing Java files.
 */
public class JavaTextPane extends TextPane {

    // The JavaTextArea
    protected JavaTextArea  _textArea;

    // The RowHeader
    protected LineHeadView  _lineNumView;

    // The OverView
    protected LineFootView  _lineFootView;

    // A runnable to check file for errors after delay
    private Runnable _checkFileRun = () -> checkFileForErrors();

    /**
     * Constructor.
     */
    public JavaTextPane()
    {
        super();
    }

    /**
     * Returns the JavaTextArea.
     */
    @Override
    public JavaTextArea getTextArea()  { return (JavaTextArea) super.getTextArea(); }

    /**
     * Returns the JavaTextModel.
     */
    public JavaTextModel getJavaTextModel()  { return (JavaTextModel) _textArea.getTextModel(); }

    /**
     * Creates the JavaTextArea.
     */
    @Override
    protected JavaTextArea createTextArea()  { return new JavaTextArea(); }

    /**
     * Override to add views.
     */
    @Override
    protected View createUI()
    {
        // Get normal UI
        BorderView borderView = (BorderView) super.createUI();

        // Get TextArea
        _textArea = getTextArea();

        // Create/configure LineNumView, LineFootView
        _lineNumView = new LineHeadView(this);
        _lineFootView = new LineFootView(this);

        // Create ScrollGroup for JavaTextArea and LineNumView
        ScrollGroup scrollGroup = new ScrollGroup();
        scrollGroup.setBorder(Color.GRAY9, 1);
        scrollGroup.setGrowWidth(true);
        scrollGroup.setMinWidth(200);
        scrollGroup.setContent(_textArea);
        scrollGroup.setLeftView(_lineNumView);

        // Replace TextPane center with scrollGroup
        borderView.setCenter(scrollGroup);
        borderView.setRight(_lineFootView);

        // Return
        return borderView;
    }

    /**
     * Initialize UI panel.
     */
    @Override
    protected void initUI()
    {
        // Do normal version
        super.initUI();

        // Start listening for TextArea mouse/drag events
        _textArea.addEventHandler(this::handleTextAreaMouseEvent, MousePress, MouseRelease);
        _textArea.addEventHandler(this::handleTextAreaDragEvent, DragEvents);

        // Add listener to initialize settings menu
        getView("SettingsButton").addEventHandler(this::handleSettingsButtonMousePress, MousePress);
    }

    /**
     * Reset UI.
     */
    protected void resetUI()
    {
        // Do normal version
        super.resetUI();

        // Reset JavaDocButton
        JavaDoc javaDoc = getJavaDoc();
        setViewVisible("JavaDocButton", javaDoc != null);
        String javaDocButtonText = javaDoc != null ? (javaDoc.getSimpleName() + " Doc") : null;
        setViewText("JavaDocButton", javaDocButtonText);

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

        // Create and add Path node labels
        Label[] pathNodeLabels = getLabelsForSelNodePath();
        for (Label pathNodeLabel : pathNodeLabels)
            nodePathBox.addChild(pathNodeLabel);
    }

    /**
     * Respond to UI controls.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        String eventName = anEvent.getName();

        switch (eventName) {

            // Handle JavaDocButton
            case "JavaDocButton":
                JavaDoc javaDoc = getJavaDoc();
                if (javaDoc != null)
                    javaDoc.openUrl();
                break;

            // Handle FontSizeText, IncreaseFontButton, DecreaseFontButton
            case "FontSizeText": case "IncreaseFontButton": case "DecreaseFontButton":
                super.respondUI(anEvent);
                JavaTextUtils.setDefaultJavaFontSize(_textArea.getTextFont().getSize());
                break;

            // Handle OpenDeclarationMenuItem, ShowReferencesMenuItem, ShowDeclarationsMenuItem
            case "OpenDeclarationMenuItem": openDeclaration(_textArea.getSelNode()); break;
            case "ShowReferencesMenuItem": showReferences(_textArea.getSelNode()); break;
            case "ShowDeclarationsMenuItem": showDeclarations(_textArea.getSelNode()); break;

            // Handle ShowScopeBoxesMenuItem
            case "ShowScopeBoxesMenuItem":
                JavaTextArea.setShowScopeBoxes(!JavaTextArea.isShowScopeBoxes());
                _textArea.repaint();
                break;

            // Do normal version
            default: super.respondUI(anEvent); break;
        }
    }

    /**
     * Called when TextArea gets MouseEvent.
     */
    private void handleTextAreaMouseEvent(ViewEvent anEvent)
    {
        // Handle PopupTrigger
        if (anEvent.isPopupTrigger()) { //anEvent.consume();
            Menu contextMenu = createContextMenu();
            contextMenu.showMenuAtXY(_textArea, anEvent.getX(), anEvent.getY());
        }

        // Handle MouseClick: If alt-down, open JavaDoc. If HoverNode, open declaration
        else if (anEvent.isMouseClick()) {

            // If alt is down and there is JavaDoc, open it
            if (anEvent.isAltDown()) {
                JavaDoc javaDoc = getJavaDoc();
                if (javaDoc != null) {
                    javaDoc.openUrl();
                    return;
                }
            }

            // If there is a hover node, open it
            JavaTextArea textArea = getTextArea();
            JNode hoverNode = textArea.getHoverNode();
            if (hoverNode != null)
                openDeclaration(hoverNode);
        }
    }

    /**
     * Called when text area gets drag events.
     */
    private void handleTextAreaDragEvent(ViewEvent anEvent)
    {
        Clipboard clipboard = anEvent.getClipboard();

        // Handle drag over: Accept
        if (anEvent.isDragOver()) {
            if (clipboard.hasString())
                anEvent.acceptDrag();
            return;
        }

        // Handle drop
        if (anEvent.isDragDrop()) {
            anEvent.acceptDrag();
            handleTextAreaDropString(clipboard);
            anEvent.dropComplete();
        }
    }

    /**
     * Called to drop string.
     */
    private void handleTextAreaDropString(Clipboard clipboard)
    {
        if (!clipboard.isLoaded()) {
            clipboard.addLoadListener(() -> handleTextAreaDropString(clipboard)); return; }
        String string = clipboard.getString();
        _textArea.replaceCharsWithContent(string);
    }

    /**
     * Called when NodePathLabel gets MouseRelease.
     */
    private void handleNodePathLabelMouseRelease(ViewEvent anEvent)
    {
        JavaTextArea javaTextArea = getTextArea();
        JNode clickedNode = (JNode) anEvent.getView().getProp("JNode");
        JNode deepNode = javaTextArea.getDeepNode();
        javaTextArea.setSel(clickedNode.getStartCharIndex(), clickedNode.getEndCharIndex());
        javaTextArea.setDeepNode(deepNode);
    }

    /**
     * Called when SettingsButton gets mouse press event.
     */
    private void handleSettingsButtonMousePress(ViewEvent anEvent)
    {
        // Reset text on ShowScopeBoxesMenuItem
        MenuButton settingsButton = getView("SettingsButton", MenuButton.class);
        MenuItem showScopeBoxesMenu = settingsButton.getItemForName("ShowScopeBoxesMenuItem");
        if (showScopeBoxesMenu != null)
            showScopeBoxesMenu.setText(JavaTextArea.isShowScopeBoxes() ? "Hide Scope Boxes" : "Show Scope Boxes");

        // Reset text on ShowSnapCodeMenuItem
        MenuItem showSnapCodeMenu = settingsButton.getItemForName("ShowSnapCodeMenuItem");
        if (showSnapCodeMenu != null)
            showSnapCodeMenu.setVisible(!getJavaTextModel().isJepl() && !getJavaTextModel().isJMD());
    }

    /**
     * Save file.
     */
    public void saveChanges()
    {
        // Hide Popup
        getTextArea().getPopup().hide();

        // Do normal version
        super.saveChanges();
    }

    /**
     * Returns the JavaDoc for currently selected node.
     */
    public JavaDoc getJavaDoc()
    {
        JNode selNode = _textArea.getSelNode();
        return JavaDoc.getJavaDocForNode(selNode);
    }

    /**
     * Creates the ContextMenu.
     */
    protected Menu createContextMenu()
    {
        // Create MenuItems
        ViewBuilder<MenuItem> viewBuilder = new ViewBuilder<>(MenuItem.class);
        viewBuilder.name("OpenDeclarationMenuItem").text("Open Declaration").save();
        viewBuilder.name("ShowReferencesMenuItem").text("Show References").save();
        viewBuilder.name("ShowDeclarationsMenuItem").text("Show Declarations").save();

        // Create context menu
        Menu contextMenu = viewBuilder.buildMenu("ContextMenu", null);
        contextMenu.setOwner(this);

        // Return
        return contextMenu;
    }

    /**
     * Override to add trailing colon.
     */
    @Override
    public String getSelectionInfo()  { return super.getSelectionInfo() + ": "; }

    /**
     * Open declaration.
     */
    public void openDeclaration(JNode aNode)  { }

    /**
     * Open a super declaration.
     */
    public void openSuperDeclaration(JExecutableDecl aMethodOrConstrDecl)  { }

    /**
     * Show References.
     */
    public void showReferences(JNode aNode)  { }

    /**
     * Show declarations.
     */
    public void showDeclarations(JNode aNode)  { }

    /**
     * Returns the ProgramCounter line.
     */
    public int getProgramCounterLine()  { return -1; }

    /**
     * Called when JavaTextArea changes.
     */
    @Override
    protected void handleTextAreaPropChange(PropChange aPC)
    {
        // Do normal version
        super.handleTextAreaPropChange(aPC);

        switch ( aPC.getPropName()) {

            // Handle SelectedNode change: Reset UI
            case JavaTextArea.SelNode_Prop:
                resetLater();
                _lineFootView.resetAll();
                break;

            // Handle SourceText
            case TextArea.SourceText_Prop:
                _lineNumView.resetAll();
                _lineFootView.resetAll();
                break;

            // Handle Selection
            case TextArea.Selection_Prop: _lineNumView.repaint(); break;
        }
    }

    /**
     * Called when TextModel does prop change.
     */
    @Override
    protected void handleSourceTextPropChange(PropChange aPC)
    {
        String propName = aPC.getPropName();

        // Handle TextModel.CharsChange: If added/removed newline, reset LineNumView, LineFootView
        if (propName == TextModel.Chars_Prop) {
            CharSequence chars = (CharSequence) (aPC.getNewValue() != null ? aPC.getNewValue() : aPC.getOldValue());
            if (CharSequenceUtils.indexOfNewline(chars, 0) >= 0)
                _lineNumView.resetAll();
            _lineFootView.resetAll();
            checkFileForErrorsAfterDelay();
        }
    }

    /**
     * Called when a build/break-point marker changes.
     */
    public void handleBuildIssueOrBreakPointMarkerChange()
    {
        _lineNumView.resetAll();
        _lineFootView.resetAll();
        _textArea.repaint();
    }

    /**
     * Register to check file for errors after slight delay.
     */
    protected void checkFileForErrorsAfterDelay()
    {
        // Register to call checkFileForErrors after delay
        ViewUtils.runDelayedCancelPrevious(_checkFileRun, 1000);

        // Clear build issues
        JavaTextModel javaTextModel = getJavaTextModel();
        JavaAgent javaAgent = javaTextModel.getAgent();
        javaAgent.setBuildIssues(new BuildIssue[0]);
    }

    /**
     * Check file for errors.
     */
    private void checkFileForErrors()
    {
        // If popup is showing, skip check
        JavaPopupList javaPopupList = getTextArea().getPopup();
        if (javaPopupList.isShowing())
            return;

        // Do check
        JavaTextModel javaTextModel = getJavaTextModel();
        JavaAgent javaAgent = javaTextModel.getAgent();
        javaAgent.checkFileForErrors();
    }

    /**
     * Returns labels for
     */
    protected Label[] getLabelsForSelNodePath()
    {
        // If Jepl, labels root should be JClassDecl
        JavaTextModel javaTextModel = getJavaTextModel();
        if (javaTextModel.isJepl() || javaTextModel.isJMD())
            return getLabelsForSelNodePathForJepl();

        // Do normal version
        return getLabelsForSelNodePath(JFile.class);
    }

    /**
     * Returns labels for
     */
    protected Label[] getLabelsForSelNodePathForJepl()
    {
        // Get JavaTextPane version
        Label[] pathNodeLabels = getLabelsForSelNodePath(JClassDecl.class);

        // If last label is ClassLabel, reconfigure
        Label lastLabel = pathNodeLabels[pathNodeLabels.length - 1];
        if (Objects.equals(lastLabel.getName(), "ClassLabel")) {

            // If JavaDoc found, modify label
            JavaDoc javaDoc = getJavaDoc();
            if (javaDoc != null) {
                lastLabel.setName("JavaDocLabel");
                lastLabel.setToolTip("Open JavaDoc");
            }
        }

        // Return
        return pathNodeLabels;
    }

    /**
     * Returns an array of labels for selected JNode hierarchy.
     */
    private Label[] getLabelsForSelNodePath(Class<? extends JNode> excludeClass)
    {
        // Get SelNode and DeepNode
        JNode selNode = _textArea.getSelNode();
        JNode deepNode = _textArea._deepNode;
        Font font = Font.Arial11;
        List<Label> pathLabels = new ArrayList<>();

        // Get label builder
        ViewBuilder<Label> labelBuilder = new ViewBuilder<>(Label.class);

        // Iterate up from DeepPart and add parts
        for (JNode jnode = deepNode; jnode != null; jnode = jnode.getParent()) {

            // Create label for node
            Label label = labelBuilder.name("NodePathLabel").text(jnode.getNodeString()).font(font).build();
            label.setProp("JNode", jnode);
            label.addEventHandler(this::handleNodePathLabelMouseRelease, MouseRelease);
            if (jnode == selNode)
                label.setFill(Color.LIGHTGRAY);
            pathLabels.add(0, label);

            // If last part, break
            JNode parentNode = jnode.getParent();
            if (parentNode == null || excludeClass != null && excludeClass.isAssignableFrom(parentNode.getClass()))
                break;

            // Add separator
            Label separator = labelBuilder.text(" \u2022 ").font(font).build();
            pathLabels.add(0, separator);
        }

        // Add Eval Type Name of selected node to end
        JavaDecl evalType = selNode != null ? selNode.getEvalType() : null;
        if (evalType != null) {
            String str = " (" + evalType.getSimpleName() + ')';
            String toolTip = evalType.getName();
            Label classLabel = labelBuilder.name("ClassLabel").text(str).font(font).toolTip(toolTip).build();
            pathLabels.add(classLabel);
        }

        // Return
        return pathLabels.toArray(new Label[0]);
    }
}