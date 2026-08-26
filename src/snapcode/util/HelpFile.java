/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.util;
import snap.util.ListUtils;
import snap.util.MarkdownNode;
import snap.viewx.MarkdownView;
import snap.web.WebURL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class manages a help file for HelpPane.
 */
public class HelpFile {

    // The source URL
    private WebURL _sourceURL;

    // The sections
    private List<HelpSection> _sections = Collections.emptyList();

    /**
     * Constructor.
     */
    public HelpFile()
    {
        super();
    }

    /**
     * Constructor.
     */
    public HelpFile(WebURL aURL)
    {
        super();

        setSourceURL(aURL);
    }

    /**
     * Returns the source URL.
     */
    public WebURL getSourceURL()  { return _sourceURL; }

    /**
     * Sets the source URL.
     */
    protected void setSourceURL(WebURL aSourceURL)
    {
        _sourceURL = aSourceURL;

        readFileFromURL(_sourceURL);
    }

    /**
     * Returns the sections.
     */
    public List<HelpSection> getSections()  { return _sections; }

    /**
     * Sets the sections.
     */
    public void setSections(List<HelpSection> theSections)
    {
        _sections = theSections;
    }

    /**
     * Returns the help section for id.
     */
    public HelpSection getSectionForHeaderId(String aHeaderId)
    {
        String headerId = aHeaderId.replace('-', ' ');
        HelpSection helpSection = ListUtils.findMatch(getSections(), section -> section.getHeader().equalsIgnoreCase(headerId));
        if (helpSection != null)
            return helpSection;

        // Search sections for header id - if found, set code block
        helpSection = ListUtils.findMatch(getSections(), section -> section.getCodeBlockNodeForHeaderId(headerId) != null);
        if (helpSection != null) {
            MarkdownNode codeBlockForHeaderId = helpSection.getCodeBlockNodeForHeaderId(headerId);
            if (codeBlockForHeaderId != null) {
                MarkdownView markdownView = helpSection.getMarkdownView();
                markdownView.setSelCodeBlockNode(codeBlockForHeaderId);
            }
        }

        return helpSection;
    }

    /**
     * Reads the file.
     */
    protected void readFileFromURL(WebURL aURL)
    {
        // Get full text
        String text = aURL.getText();
        text = text.replace("##", "<H2>");

        // Get text for sections
        String[] sectionTexts = text.split("#");
        List<HelpSection> sections = new ArrayList<>();

        // Iterate over sectionTexts and create/add sections
        for (String sectionText : sectionTexts) {

            // Get trimmed - just skip if empty
            sectionText = sectionText.trim();
            if (sectionText.isEmpty()) continue;
            sectionText = sectionText.replace("<H2>", "##");

            // Create/add section
            HelpSection section = new HelpSection(this, sectionText);
            sections.add(section);
        }

        // Set sections and return
        setSections(sections);
    }
}
