/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.view.TextArea;
import snap.web.WebResponse;

/**
 * A page subclass for FileNotFound errors.
 */
public class FileNotFoundPage extends TextPage {

    /**
     * Constructor.
     */
    public FileNotFoundPage()
    {
        super();
    }

    /**
     * Override to load text from class info string.
     */
    @Override
    protected void loadTextAreaText()
    {
        TextArea textArea = new TextArea();
        String fileNotFoundString = getFileNotFoundString();
        textArea.setText(fileNotFoundString);
    }

    /**
     * Returns info string.
     */
    private String getFileNotFoundString()
    {
        WebResponse resp = getResponse();
        if(resp == null)
            return "No Response found";

        return "FileNotFound: " + "\n\n" +
                "  - The requested URL " + resp.getURL().getString() + " was not found on this server.\n\n" +
                "  - Response Code: " + resp.getCode() + ' ' + resp.getCodeString();
    }

    /**
     * Returns the page title.
     */
    public String getTitle()  { return "File Not Found: " + getURL().getString(); }
}