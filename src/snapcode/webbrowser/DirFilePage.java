/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.gfx.Image;
import snap.view.*;
import snap.web.WebFile;
import snapcode.util.FileIcons;

import java.util.Arrays;
import java.util.List;

/**
 * A WebPage subclass for directories.
 */
public class DirFilePage extends WebPage {

    // The file browser
    private BrowserView<WebFile> _fileBrowser;

    // The page browser
    private WebBrowser _pageBrowser;

    /**
     * Constructor.
     */
    public DirFilePage()
    {
        super();
    }

    /**
     * Creates a file pane for the given file in the requested mode.
     */
    protected View createUI()
    {
        // Create/configure FileBrowser
        _fileBrowser = new BrowserView<>();
        _fileBrowser.setName("FileBrowser");
        _fileBrowser.setPrefWidth(400);
        _fileBrowser.setGrowWidth(true);
        _fileBrowser.setResolver(new FileTreeResolver());
        _fileBrowser.setItems(getFile().getFiles());
        _fileBrowser.addEventHandler(this::handleFileBrowserMouseRelease, MouseRelease);

        // Create and configure PageBrowser
        _pageBrowser = new WebBrowser();
        _pageBrowser.setGrowWidth(true);

        // Add to row view and return
        RowView rowView = new RowView();
        rowView.setFillHeight(true);
        rowView.setChildren(_fileBrowser, _pageBrowser);
        return rowView;
    }

    /**
     * Respond to UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle FileBrowser click
        if (anEvent.equals("FileBrowser")) {
            WebFile file = _fileBrowser.getSelItem();
            if (file == null)
                return;
            _pageBrowser.setSelFile(file.isFile() ? file : null);
            getUI().relayout();
        }
    }

    /**
     * Called when FileBrowser gets mouse release.
     */
    private void handleFileBrowserMouseRelease(ViewEvent anEvent)
    {
        // Handle FileBrowser double-click
        if (anEvent.isMouseClick() && anEvent.getClickCount() == 2) {
            WebFile file = _fileBrowser.getSelItem();
            if (file != null)
                getBrowser().setSelFile(file);
        }
    }

    /**
     * A TreeResolver for WebFile
     */
    public static class FileTreeResolver extends TreeResolver<WebFile> {

        /**
         * Returns the parent of given item.
         */
        public WebFile getParent(WebFile anItem)
        {
            return anItem.getParent();
        }

        // Return whether file is directory
        public boolean isParent(WebFile anObj)
        {
            return anObj.isDir();
        }

        // Return child files
        public List<WebFile> getChildren(WebFile aParent)
        {
            return Arrays.asList(aParent.getFiles());
        }

        // Return child file name
        public String getText(WebFile aFile)
        {
            return aFile.getName();
        }

        // Return child file icon
        public Image getImage(WebFile aFile)
        {
            return FileIcons.getFileIconImage(aFile);
        }
    }

}