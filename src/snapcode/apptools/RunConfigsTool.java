package snapcode.apptools;
import snap.view.*;
import snapcode.app.*;
import java.util.Arrays;
import java.util.List;

/**
 * Manages a list of run configurations for project.
 */
public class RunConfigsTool extends ProjectTool {

    // The selected RunConfig
    private RunConfig  _runConfig;

    /**
     * Constructor.
     */
    public RunConfigsTool(ProjectPane projectPane)
    {
        super(projectPane);
    }

    /**
     * Returns the List of RunConfigs.
     */
    public List<RunConfig> getRunConfigs()
    {
        return RunConfigs.get(getRootSite()).getRunConfigs();
    }

    /**
     * Returns the selected run config.
     */
    public RunConfig getSelectedRunConfig()
    {
        if (_runConfig == null && getRunConfigs().size() > 0)
            _runConfig = getRunConfigs().get(0);
        return _runConfig;
    }

    /**
     * Sets the selected run config.
     */
    public void setSelectedRunConfig(RunConfig aConfig)
    {
        _runConfig = aConfig;
    }

//    /**
//     * Override to add menu button.
//     */
//    protected View createUI()
//    {
//        // Do normal version
//        RowView superUI = (RowView) super.createUI();
//
//        // Add MenuButton - was from AppPaneToolBar
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
        RunConfig selRunConfig = getSelectedRunConfig();
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
                setSelectedRunConfig(runConfig);
            }
        }

        // Handle NameText, MainClassText, AppArgsText, VMArgsText
        RunConfig selRunConfig = getSelectedRunConfig();
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
            RunConfig rc = new RunConfig().setName("Untitled");
            getRunConfigs().add(rc);
            setSelectedRunConfig(rc);
        }

        // Handle RemoveButton
        if (anEvent.equals("RemoveButton") && getRunConfigs().size() > 0) {
            getRunConfigs().remove(getSelectedRunConfig());
            setSelectedRunConfig(null);
        }

//        // Handle RunConfigMenuItems
//        if (anEvent.getName().endsWith("RunConfigMenuItem")) {
//            String configName = anEvent.getName().replace("RunConfigMenuItem", "");
//            DebugTool debugTool = _projTools.getDebugTool();
//            debugTool.runConfigForName(configName, false);
//            setRunMenuButtonItems();
//        }

        // Save RunConfigs
        RunConfigs.get(getRootSite()).writeFile();
        //getAppPane().getToolBar().setRunMenuButtonItems();
    }

    /**
     * Sets the RunMenuButton items.
     */
    public void setRunMenuButtonItems()
    {
        MenuButton rmb = getView("RunMenuButton", MenuButton.class);
        rmb.setItems(Arrays.asList(getRunMenuButtonItems()));
        for (MenuItem mi : rmb.getItems())
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
        if (runConfigs.size() > 0)
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