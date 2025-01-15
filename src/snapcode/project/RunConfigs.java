package snapcode.project;
import snap.util.ListUtils;
import snapcode.util.Settings;
import snap.web.WebFile;
import snap.web.WebSite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A class to manage a list of RunConfigs for a site.
 */
public class RunConfigs {

    // The site
    private WebSite _site;

    // The list of Run Configurations
    private List<RunConfig> _runConfigs;

    /**
     * Constructor for given site.
     */
    public RunConfigs(WebSite aSite)
    {
        _site = aSite;
    }

    /**
     * Returns the primary RunConfig (or null, if none).
     */
    public RunConfig getRunConfig()
    {
        List<RunConfig> runConfigs = getRunConfigs();
        return !runConfigs.isEmpty() ? runConfigs.get(0) : null;
    }

    /**
     * Returns the RunConfigs.
     */
    public List<RunConfig> getRunConfigs()
    {
        if (_runConfigs != null) return _runConfigs;
        return _runConfigs = readFile();
    }

    /**
     * Returns the RunConfig for given name.
     */
    public RunConfig getRunConfigForName(String aString)
    {
        return ListUtils.findMatch(getRunConfigs(), runConfig -> runConfig.getName().equals(aString));
    }

    /**
     * Reads the RunConfigs from file.
     */
    public List<RunConfig> readFile()
    {
        List<RunConfig> runConfigs = new ArrayList<>();
        List<Map<String,Object>> runConfigMaps = getSettings().getList("RunConfigs");

        // If existing RunConfig maps found, create RunConfigs
        if (runConfigMaps != null) {
            for (Map<String,Object> map : runConfigMaps) {
                String name = (String) map.get("Name");
                String cname = (String) map.get("MainClassName");
                String appArgs = (String) map.get("AppArgs");
                String vmArgs = (String) map.get("VMArgs");
                runConfigs.add(new RunConfig().setName(name).setMainClassName(cname).setAppArgs(appArgs).setVMArgs(vmArgs));
            }
        }

        // Return
        return runConfigs;
    }

    /**
     * Saves RunConfigs to file.
     */
    public void writeFile()
    {
        List<RunConfig> runConfigs = getRunConfigs();
        Settings.SettingsList<Map<String,String>> runConfigMaps = getSettings().getList("RunConfigs", true);

        for (int i = 0, iMax = runConfigs.size(); i < iMax; i++) {
            RunConfig runConfig = runConfigs.get(i);
            Settings runConfigSetting = runConfigMaps.getSettings(i, true);
            runConfigSetting.put("Name", runConfig.getName());
            runConfigSetting.put("MainClassName", runConfig.getMainClassName());
            runConfigSetting.put("AppArgs", runConfig.getAppArgs());
            runConfigSetting.put("VMArgs", runConfig.getVMArgs());
        }
        while (runConfigMaps.size() > runConfigs.size())
            runConfigMaps.remove(runConfigMaps.size() - 1);

        // Save file
        if (getFile().isUpdateSet())
            getFile().save();
    }

    /**
     * Returns the project settings.
     */
    protected Settings getSettings()
    {
        return Settings.get(getFile());
    }

    /**
     * Returns the RunConfigs file.
     */
    protected WebFile getFile()
    {
        WebSite sandboxSite = _site.getSandboxSite();
        return sandboxSite.createFileForPath("/settings/run_configs", false);
    }

    /**
     * Returns the RunConfigs for a given site.
     */
    public static synchronized RunConfigs getRunConfigsForProjectSite(WebSite aSite)
    {
        RunConfigs runConfigs = (RunConfigs) aSite.getProp(RunConfigs.class.getName());
        if (runConfigs == null)
            aSite.setProp(RunConfigs.class.getName(), runConfigs = new RunConfigs(aSite));
        return runConfigs;
    }
}