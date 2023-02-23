/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.apptools;
import javakit.ide.JavaTextArea;
import snap.geom.Insets;
import snap.gfx.Color;
import snap.gfx.Image;
import snap.text.TextDoc;
import snap.view.*;
import snap.viewx.TextPane;
import snap.viewx.WebPage;
import snap.web.WebURL;
import snapcode.app.JavaPage;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import snapcode.util.HelpFile;
import snapcode.util.HelpSection;
import snapcode.util.MarkDownDoc;

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

    // The TextArea showing the help text
    private TextArea  _helpTextArea;

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
        TextDoc sectionText = aSection.getMarkDownDoc();
        _helpTextArea.setTextDoc(sectionText);
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
        searchText.getLabel().setImage(Image.get(TextPane.class, "Find.png"));
        TextField.setBackLabelAlignAnimatedOnFocused(searchText, true);
        //searchText.addEventFilter(e -> ViewUtils.runLater(() -> textFieldKeyTyped(e)), KeyPress);

        // Get/configure SplitView
        SplitView splitView = getView("SplitView", SplitView.class);
        splitView.setBorder(null);
        splitView.setDividerSpan(2);
        splitView.getDivider().setPaintable(false);

        // Get TopicListArea and configure
        ListView<HelpSection> topicListView = getView("TopicListView", ListView.class);
        topicListView.setFocusWhenPressed(false);
        _topicListArea = topicListView.getListArea();
        _topicListArea.setName("TopicListArea");
        _topicListArea.setCellConfigure(cell -> configureTopicListAreaCell(cell));
        _topicListArea.setCellPadding(new Insets(4, 4, 3, 4));

        // Get SectionTextArea
        TextView helpTextView = getView("HelpTextView", TextView.class);
        _helpTextArea = helpTextView.getTextArea();
        _helpTextArea.setPadding(8, 8, 8, 8);

        // Get HelpSections and set in TopicListArea
        HelpFile helpFile = getHelpFile();
        HelpSection[] sections = helpFile.getSections();
        _topicListArea.setItems(sections);

        // Set ScrollView BarSize to mini
        ScrollView topicListScrollView = topicListView.getScrollView();
        ScrollView helpTextScrollView = helpTextView.getScrollView();
        topicListScrollView.setBarSize(14);
        helpTextScrollView.setBarSize(14);
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

        // Get JavaPage (just return if not found)
        WebPage selPage = _pagePane.getSelPage();
        JavaPage javaPage = selPage instanceof JavaPage ? (JavaPage) selPage : null;
        if (javaPage == null)
            return;

        // If current line not empty, select end
        JavaTextArea javaTextArea = javaPage.getTextArea();
        if (!javaTextArea.getSel().isEmpty() || javaTextArea.getSel().getStartLine().length() > 1)
            javaTextArea.setSel(javaTextArea.length(), javaTextArea.length());

        // Add help
        javaTextArea.replaceCharsWithContent(helpCode);

        // Run app
        EvalTool evalTool = _workspaceTools.getToolForClass(EvalTool.class);
        if (evalTool != null) {

            _workspaceTools.showToolForClass(EvalTool.class);
            if (evalTool.isAutoRun())
                evalTool.runApp(false);
        }

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
        MarkDownDoc markDown = selSection.getMarkDownDoc();

        // Get selection char index from SectionTextArea
        int selStart = _helpTextArea.getSelStart();
        int selEnd = _helpTextArea.getSelEnd();
        int selCharIndex = (selStart + selEnd) / 2;

        // Get the code for selection char index
        MarkDownDoc.MarkDownRun codeRun = markDown.getCodeRunForCharIndex(selCharIndex);
        if (codeRun == null)
            return null;

        // Get code string and return
        String helpStr = markDown.subSequence(codeRun.startCharIndex, codeRun.endCharIndex).toString();
        return helpStr;
    }

    /**
     * Returns the tool title.
     */
    @Override
    public String getTitle()  { return "Help"; }
}
