/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.util;

/**
 * This class represents a section of help from a HelpFile.
 */
public class HelpSection {

    // The HelpFile that holds section
    private HelpFile _helpFile;

    // The section text
    private String  _sectionText;

    // The header
    private String  _header;

    // The content
    private String  _content;

    // The MarkDownText
    private MarkDownText _markDownText;

    /**
     * Constructor.
     */
    public HelpSection(HelpFile aHelpFile, String sectionText)
    {
        super();

        // Set ivars
        _helpFile = aHelpFile;
        _sectionText = sectionText;

        // Assume first line is header
        int headerEnd = sectionText.indexOf('\n');
        if (headerEnd > 0) {
            String header = sectionText.substring(0, headerEnd);
            setHeader(header);
            sectionText = sectionText.substring(headerEnd);
        }

        // Set content
        setContent(sectionText);
    }

    /**
     * Returns the HelpFile.
     */
    public HelpFile getHelpFile()  { return _helpFile; }

    /**
     * Returns the header.
     */
    public String getHeader()  { return _header; }

    /**
     * Sets the header.
     */
    public void setHeader(String aString)
    {
        _header = aString;
    }

    /**
     * Returns the content.
     */
    public String getContent()  { return _content; }

    /**
     * Sets the content.
     */
    public void setContent(String aString)
    {
        _content = aString;
    }

    /**
     * Returns the MarkDownText.
     */
    public MarkDownText getMarkDownText()
    {
        // If already set, just return
        if (_markDownText != null) return _markDownText;

        // Create and configure
        MarkDownText markDownText = new MarkDownText();
        markDownText.setMarkDown("# " + _sectionText);

        // Set and return
        return _markDownText = markDownText;
    }
}
