/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.view.View;
import snap.view.ViewController;

/**
 * A WebPage subclass to wrap a view controller in a WebPage.
 */
public class ViewOwnerPage extends WebPage {

    // The view controller
    private ViewController _owner;

    /**
     * Constructor.
     */
    public ViewOwnerPage(ViewController anOwner)
    {
        _owner = anOwner;
    }

    /**
     * Returns the view controller for this page.
     */
    public ViewController getViewOwner()  { return _owner; }

    /**
     * Override to return controller UI.
     */
    protected View createUI()
    {
        return getViewOwner().getUI();
    }

    /**
     * Override to forward to controller.
     */
    public Object getFirstFocus()
    {
        return getViewOwner().getFirstFocus();
    }
}