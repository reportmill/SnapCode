/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.web.WebSite;

/**
 * This Workspace subclass is enhanced to work with full JDK.
 */
public class WorkspaceX extends Workspace {

    /**
     * Constructor.
     */
    public WorkspaceX()
    {
        super();
    }

    /**
     * Creates a project for given site.
     */
    @Override
    protected Project createProjectForSite(WebSite aSite)
    {
        return new ProjectX(this, aSite);
    }
}
