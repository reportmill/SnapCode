/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.apptools;
import snapcode.javatext.JavaTextArea;
import snap.geom.Insets;
import snap.gfx.Color;
import snap.gfx.Image;
import snap.view.*;
import snap.viewx.TextPane;
import snapcode.project.Project;
import snapcode.util.*;
import snapcode.webbrowser.WebPage;
import snap.web.WebURL;
import snapcode.app.JavaPage;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;

/**
 * This class shows a help file for the app.
 */
public class HelpTool extends WorkspaceTool {

    // The HelpFile
    private HelpFile _helpFile;

    // The selected section
    private HelpSection _selSection;

    // The ListArea showing HelpSections
    private ListArea<HelpSection>  _topicListArea;

    // The ScrollView showing the help text
    private ScrollView _helpTextScrollView;

    /**
     * Constructor.
     */
    public HelpTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
        _helpFile = new HelpFile();
    }

    /**
     * Returns the HelpFile.
     */
    public HelpFile getHelpFile()  { return _helpFile; }

    /**
     * Sets the HelpFile.
     */
    public void setHelpFile(HelpFile aHelpFile)
    {
        // If already set, just return
        if (aHelpFile == _helpFile) return;

        // Set
        _helpFile = aHelpFile;

        // If UI is set, update
        if (_topicListArea != null) {
            HelpSection[] sections = _helpFile.getSections();
            _topicListArea.setItems(sections);
            HelpSection selSection = sections.length > 0 ? sections[0] : null;
            setSelSection(selSection);
            _topicListArea.setSelItem(selSection);
        }
    }

    /**
     * Returns the selected section.
     */
    public HelpSection getSelSection()  { return _selSection; }

    /**
     * Sets the selected section.
     */
    public void setSelSection(HelpSection aSection)
    {
        // If already set, just return
        if (aSection == getSelSection()) return;

        // Set SelSection
        _selSection = aSection;

        // Update SectionTextArea
        MarkDownView sectionText = aSection.getMarkDownView();
        _helpTextScrollView.setContent(sectionText);
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Set background
        View ui = getUI();
        ui.setFill(Color.WHITE);

        // Get/configure SearchText: radius, prompt, image, animation
        TextField searchText = getView("SearchTextField", TextField.class);
        searchText.getLabel().setImage(Image.getImageForClassResource(TextPane.class, "Find.png"));
        TextField.setBackLabelAlignAnimatedOnFocused(searchText, true);
        //searchText.addEventFilter(e -> ViewUtils.runLater(() -> textFieldKeyTyped(e)), KeyPress);

        // Get/configure SplitView
        SplitView splitView = getView("SplitView", SplitView.class);
        splitView.setBorder(null);
        splitView.getDivider().setPaintable(false);

        // Get TopicListArea and configure
        ListView<HelpSection> topicListView = getView("TopicListView", ListView.class);
        topicListView.setFocusWhenPressed(false);
        _topicListArea = topicListView.getListArea();
        _topicListArea.setName("TopicListArea");
        _topicListArea.setCellConfigure(cell -> configureTopicListAreaCell(cell));
        _topicListArea.setCellPadding(new Insets(4, 4, 3, 4));

        // Get HelpTextScrollView
        _helpTextScrollView = getView("HelpTextScrollView", ScrollView.class);
        _helpTextScrollView.setBarSize(14);

        // Get HelpSections and set in TopicListArea
        HelpFile helpFile = getHelpFile();
        HelpSection[] sections = helpFile.getSections();
        _topicListArea.setItems(sections);

        // Set ScrollView BarSize to mini
        ScrollView topicListScrollView = topicListView.getScrollView();
        topicListScrollView.setBarSize(14);
    }

    private void configureTopicListAreaCell(ListCell<HelpSection> aCell)
    {
        HelpSection helpSection = aCell.getItem();
        if (helpSection != null) {
            aCell.setText(helpSection.getHeader());
            if (aCell.getTextFill() == Color.BLACK)
                aCell.setTextFill(Color.GRAY3);
        }
    }

    /**
     * Override to load real help file.
     */
    @Override
    protected void initShowing()
    {
        loadRealHelpFile();
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Update TopicListArea
        if (_topicListArea.getSelItem() != getSelSection())
            _topicListArea.setSelItem(getSelSection());
    }

    /**
     * Respond UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle TopicListArea
        if (anEvent.equals("TopicListArea")) {
            HelpSection section = (HelpSection) anEvent.getSelItem();
            setSelSection(section);
        }

        // Handle AddCodeButton
        if (anEvent.equals("AddCodeButton"))
            addHelpCodeToDoc();
    }

    /**
     * Finds help code in current help file and sends to DocPane.Doc.
     */
    private void addHelpCodeToDoc()
    {
        // Get Help String
        String helpCode = getHelpCode();
        if (helpCode == null)
            return;

        // Add SnapCharts if needed
        addSnapChartsToProjectIfNeeded();

        // Get JavaPage (just return if not found)
        WebPage selPage = _pagePane.getSelPage();
        JavaPage javaPage = selPage instanceof JavaPage ? (JavaPage) selPage : null;
        if (javaPage == null)
            return;

        // Set help code in text area
        JavaTextArea javaTextArea = javaPage.getTextArea();
        javaTextArea.setSel(0, javaTextArea.length());
        javaTextArea.replaceCharsWithContent(helpCode);
        hideTool();

        // Focus on text area
        javaTextArea.requestFocus();
    }

    /**
     * Loads the real help file.
     */
    private void loadRealHelpFile()
    {
        if (_helpFile.getSections().length > 0) return;

        // Create run to load real HelpFile and set
        Runnable loadHelpFileRun = () -> {
            WebURL helpFileURL = WebURL.getURL(HelpFile.class, "HelpFile.md");
            HelpFile helpFile = new HelpFile(helpFileURL);
            runLater(() -> setHelpFile(helpFile));
        };

        // Create, start thead for loadHelpFileRun
        new Thread(loadHelpFileRun).start();
    }

    /**
     * Returns help code.
     */
    private String getHelpCode()
    {
        // Get current section and MarkDown doc
        HelpSection selSection = getSelSection();
        MarkDownView markDown = selSection.getMarkDownView();

        // Return selected code block node text
        MDNode codeBlockNode = markDown.getSelCodeBlockNode();
        return codeBlockNode != null ? codeBlockNode.getText() : null;
    }

    /**
     * Adds SnapCharts to project.
     */
    private void addSnapChartsToProjectIfNeeded()
    {
        // If not needed, just return
        HelpSection selSection = getSelSection();
        String header = selSection.getHeader().toLowerCase();
        if (!header.contains(" chart") && !header.contains(" datasets"))
            return;

        // Add
        Project project = getSelProject();
        project.getBuildFile().setIncludeSnapChartsRuntime(true);
    }

    /**
     * Returns the tool title.
     */
    @Override
    public String getTitle()  { return "Help"; }
}
