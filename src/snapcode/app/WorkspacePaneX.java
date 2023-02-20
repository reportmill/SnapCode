package snapcode.app;

/**
 * This WorkspacePane subclass enables full JRE dev features.
 */
public class WorkspacePaneX extends WorkspacePane {

    /**
     * Creates the WorkspaceTools.
     */
    protected WorkspaceTools createWorkspaceTools()  { return new WorkspaceToolsX(this); }
}
