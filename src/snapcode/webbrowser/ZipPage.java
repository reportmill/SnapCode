/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.gfx.Image;
import snap.view.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.util.FileIcons;
import java.util.List;

/**
 * A WebPage subclass for Zip/Jar files.
 */
public class ZipPage extends WebPage {

    // The file browser
    private BrowserView<WebFile> _fileBrowser;

    // The page browser
    private WebBrowser _pageBrowser;

    /**
     * Constructor.
     */
    public ZipPage()
    {
        super();
    }

    /**
     * Returns the root files.
     */
    public WebFile[] getFiles()
    {
        WebSite site = getFile().getURL().getAsSite();
        return site.getRootDir().getFilesArray();
    }

    /**
     * Creates a file pane for the given file in the requested mode.
     */
    protected View createUI()
    {
        // Create FileBrowser and put in ScrollView
        _fileBrowser = new BrowserView<>();
        _fileBrowser.setName("FileBrowser");
        _fileBrowser.setPrefColCount(3);
        _fileBrowser.setPrefHeight(350);
        _fileBrowser.setResolver(new FileTreeResolver());
        _fileBrowser.setItems(getFiles());
        _fileBrowser.addEventHandler(this::handleFileBrowserMouseRelease, MouseRelease);

        // Create PageBrowser
        _pageBrowser = new WebBrowser();
        _pageBrowser.setGrowHeight(true);

        // Put FileBrowser and PageBrowser in VBox
        ColView vbox = new ColView();
        vbox.setFillWidth(true);
        vbox.setChildren(_fileBrowser, _pageBrowser);
        return vbox;
    }

    /**
     * Respond to UI.
     */
    private void handleFileBrowserMouseRelease(ViewEvent anEvent)
    {
        WebFile file = _fileBrowser.getSelItem();
        if (file != null)
            _pageBrowser.setSelFile(file.isFile() ? file : null);
    }

    /**
     * A TreeResolver for WebFile
     */
    public static class FileTreeResolver extends TreeResolver<WebFile> {

        // Returns the parent of given item.
        public WebFile getParent(WebFile anItem)  { return anItem.getParent(); }

        // Return whether file is directory
        public boolean isParent(WebFile anObj)  { return anObj.isDir(); }

        // Return child files
        public List<WebFile> getChildren(WebFile aParent)  { return aParent.getFiles(); }

        // Return child file name
        public String getText(WebFile aFile)  { return aFile.getName(); }

        // Return child file icon
        public Image getImage(WebFile aFile)  { return FileIcons.getFileIconImage(aFile); }
    }
}