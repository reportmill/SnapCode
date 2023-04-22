/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.view.View;
import snap.view.ViewOwner;

/**
 * A WebPage subclass to wrap a ViewOwner in a WebPage.
 */
public class ViewOwnerPage extends WebPage {

    // The ViewOwner
    private ViewOwner  _owner;

    /**
     * Creates a new ViewOwnerPage for given ViewOwner.
     */
    public ViewOwnerPage(ViewOwner anOwner)
    {
        _owner = anOwner;
    }

    /**
     * Returns the ViewOwner for this page.
     */
    public ViewOwner getViewOwner()  { return _owner; }

    /**
     * Override to return ViewOwner UI.
     */
    protected View createUI()
    {
        return getViewOwner().getUI();
    }

    /**
     * Override to forward to ViewOwner.
     */
    public Object getFirstFocus()
    {
        return getViewOwner().getFirstFocus();
    }
}