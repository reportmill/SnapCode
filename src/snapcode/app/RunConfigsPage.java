package snapcode.app;
import snap.view.ScrollView;
import snap.view.View;
import snap.view.ViewEvent;
import snap.viewx.WebBrowser;
import snap.viewx.WebPage;
import snap.web.WebSite;
import java.util.List;

/**
 * Manages a list of run configurations for project.
 */
public class RunConfigsPage extends WebPage {

    // The selected RunConfig
    RunConfig _runConfig;

    /**
     * Returns the AppPane.
     */
    public AppPane getAppPane()
    {
        WebBrowser browser = getBrowser();
        return browser.getOwner(AppPane.class);
    }

    /**
     * Returns the Project.
     */
    public WebSite getSelectedSite()
    {
        return getAppPane().getRootSite();
    }

    /**
     * Returns the List of RunConfigs.
     */
    public List<RunConfig> getRunConfigs()
    {
        return RunConfigs.get(getSelectedSite()).getRunConfigs();
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

    /**
     * Override to put in Page pane.
     */
    protected View createUI()
    {
        return new ScrollView(super.createUI());
    }

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

        // Save RunConfigs
        RunConfigs.get(getSelectedSite()).writeFile();
        getAppPane().getToolBar().setRunMenuButtonItems();
    }

    /**
     * Override to suppress.
     */
    public void reload()
    {
    }

    /**
     * Return better title.
     */
    public String getTitle()
    {
        return getSelectedSite().getName() + " Run Configurations";
    }

}