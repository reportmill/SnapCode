package snapcode.apptools;
import snap.view.*;
import snap.web.WebSite;
import snapcode.app.*;
import snapcode.project.RunConfig;
import snapcode.project.RunConfigs;
import java.util.Collections;
import java.util.List;

/**
 * Manages a list of run configurations for project.
 */
public class RunConfigsTool extends WorkspaceTool {

    // The selected RunConfig
    private RunConfig _selRunConfig;

    /**
     * Constructor.
     */
    public RunConfigsTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Returns the List of RunConfigs.
     */
    public List<RunConfig> getRunConfigs()
    {
        WebSite rootSite = getRootSite();
        return rootSite != null ? RunConfigs.getRunConfigsForProjectSite(rootSite).getRunConfigs() : Collections.emptyList();
    }

    /**
     * Returns the selected run config.
     */
    public RunConfig getSelRunConfig()
    {
        List<RunConfig> runConfigs = getRunConfigs();
        if (_selRunConfig == null && !runConfigs.isEmpty())
            _selRunConfig = runConfigs.get(0);
        return _selRunConfig;
    }

    /**
     * Sets the selected run config.
     */
    public void setSelRunConfig(RunConfig aConfig)
    {
        _selRunConfig = aConfig;
    }

//    /**
//     * Override to add menu button.
//     */
//    protected View createUI()
//    {
//        // Do normal version
//        RowView superUI = (RowView) super.createUI();
//
//        // Add MenuButton - was from MainToolBar
//        MenuButton menuButton = new MenuButton();
//        menuButton.setName("RunMenuButton");
//        menuButton.setPrefSize(15, 14);
//        menuButton.setMargin(22, 0, 0, 0);
//        menuButton.setItems(Arrays.asList(getRunMenuButtonItems()));
//        menuButton.getGraphicAfter().setPadding(0, 0, 0, 0);
//        superUI.addChild(menuButton, 5);
//
//        // Return
//        return superUI;
//    }

    @Override
    protected void resetUI()
    {
        // Update NameText
        RunConfig selRunConfig = getSelRunConfig();
        setViewText("NameText", selRunConfig != null ? selRunConfig.getName() : "");
        setViewEnabled("NameText", selRunConfig != null);

        // Update MainClassText
        setViewText("MainClassText", selRunConfig != null ? selRunConfig.getMainClassName() : "");
        setViewEnabled("MainClassText", selRunConfig != null);

        // Update RunConfigsList
        List<RunConfig> runConfigs = getRunConfigs();
        setViewItems("RunConfigsList", runConfigs);
        setViewSelItem("RunConfigsList", selRunConfig);

        // Update AppArgsText
        setViewText("AppArgsText", selRunConfig != null ? selRunConfig.getAppArgs() : "");
        setViewEnabled("AppArgsText", selRunConfig != null);

        // Update VMArgsText
        setViewText("VMArgsText", selRunConfig != null ? selRunConfig.getVMArgs() : "");
        setViewEnabled("VMArgsText", selRunConfig != null);
    }

    /**
     * Respond to UI changes.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle RunConfigsList
        if (anEvent.equals("RunConfigsList")) {
            Object selItem = anEvent.getSelItem();
            if (selItem instanceof RunConfig) {
                RunConfig runConfig = (RunConfig) selItem;
                setSelRunConfig(runConfig);
            }
        }

        // Handle NameText, MainClassText, AppArgsText, VMArgsText
        RunConfig selRunConfig = getSelRunConfig();
        if (anEvent.equals("NameText") && selRunConfig != null)
            selRunConfig.setName(anEvent.getStringValue());
        if (anEvent.equals("MainClassText") && selRunConfig != null)
            selRunConfig.setMainClassName(anEvent.getStringValue());
        if (anEvent.equals("AppArgsText") && selRunConfig != null)
            selRunConfig.setAppArgs(anEvent.getStringValue());
        if (anEvent.equals("VMArgsText") && selRunConfig != null)
            selRunConfig.setVMArgs(anEvent.getStringValue());

        // Handle AddButton
        if (anEvent.equals("AddButton")) {
            RunConfig runConfig = new RunConfig().setName("Untitled");
            getRunConfigs().add(runConfig);
            setSelRunConfig(runConfig);
        }

        // Handle RemoveButton
        if (anEvent.equals("RemoveButton") && !getRunConfigs().isEmpty()) {
            getRunConfigs().remove(getSelRunConfig());
            setSelRunConfig(null);
        }

//        // Handle RunConfigMenuItems
//        if (anEvent.getName().endsWith("RunConfigMenuItem")) {
//            String configName = anEvent.getName().replace("RunConfigMenuItem", "");
//            DebugTool debugTool = _workspaceTools.getDebugTool();
//            debugTool.runConfigForName(configName, false);
//            setRunMenuButtonItems();
//        }

        // Save RunConfigs
        RunConfigs.getRunConfigsForProjectSite(getRootSite()).writeFile();
        // getToolBar().setRunMenuButtonItems();
    }

    /**
     * Sets the RunMenuButton items.
     */
    public void setRunMenuButtonItems()
    {
        MenuButton rmb = getView("RunMenuButton", MenuButton.class);
        rmb.setMenuItems(getRunMenuButtonItems());
        for (MenuItem mi : rmb.getMenuItems())
            mi.setOwner(this);
    }

    /**
     * Creates a pop-up menu for preview edit button (currently with look and feel options).
     */
    private MenuItem[] getRunMenuButtonItems()
    {
        ViewBuilder<MenuItem> mib = new ViewBuilder<>(MenuItem.class);

        // Add RunConfigs MenuItems
        List<RunConfig> runConfigs = getRunConfigs();
        for (RunConfig runConfig : runConfigs) {
            String name = runConfig.getName() + "RunConfigMenuItem";
            mib.name(name).text(name).save();
        }

        // Add separator
        if (!runConfigs.isEmpty())
            mib.save();

        // Add RunConfigsMenuItem
        mib.name("RunConfigsMenuItem").text("Run Configurations...").save();

        // Return MenuItems
        return mib.buildAll();
    }

    /**
     * Return better title.
     */
    public String getTitle()
    {
        return "Run Configs";
    }
}