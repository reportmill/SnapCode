package snapcode.project;
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
    WebSite _site;

    // The list of Run Configurations
    List<RunConfig> _runConfigs;

    /**
     * Creates a new RunConfigs for given site.
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
        List<RunConfig> cfs = getRunConfigs();
        return cfs.size() > 0 ? cfs.get(0) : null;
    }

    /**
     * Returns the RunConfigs.
     */
    public List<RunConfig> getRunConfigs()
    {
        return _runConfigs != null ? _runConfigs : (_runConfigs = readFile());
    }

    /**
     * Returns the RunConfig for given name.
     */
    public RunConfig getRunConfig(String aString)
    {
        for (RunConfig rc : getRunConfigs())
            if (rc.getName().equals(aString))
                return rc;
        return null;
    }

    /**
     * Reads the RunConfigs from file.
     */
    public List<RunConfig> readFile()
    {
        List<RunConfig> runConfigs = new ArrayList<>();
        List<Map<String,Object>> rconfMaps = getSettings().getList("RunConfigs");

        // If existing RunConfig maps found, create RunConfigs
        if (rconfMaps != null) {
            for (Map<String,Object> map : rconfMaps) {
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
        List<RunConfig> rconfs = getRunConfigs();
        Settings.SettingsList<Map> rconfMaps = getSettings().getList("RunConfigs", true);

        for (int i = 0, iMax = rconfs.size(); i < iMax; i++) {
            RunConfig rconf = rconfs.get(i);
            Settings rcSetting = rconfMaps.getSettings(i, true);
            rcSetting.put("Name", rconf.getName());
            rcSetting.put("MainClassName", rconf.getMainClassName());
            rcSetting.put("AppArgs", rconf.getAppArgs());
            rcSetting.put("VMArgs", rconf.getVMArgs());
        }
        while (rconfMaps.size() > rconfs.size()) rconfMaps.remove(rconfMaps.size() - 1);

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
        WebFile file = _site.getSandbox().getFileForPath("/settings/run_configs");
        if (file == null) file = _site.getSandbox().createFileForPath("/settings/run_configs", false);
        return file;
    }

    /**
     * Returns the RunConfigs for a given site.
     */
    public static synchronized RunConfigs get(WebSite aSite)
    {
        RunConfigs rcs = (RunConfigs) aSite.getProp(RunConfigs.class.getName());
        if (rcs == null) aSite.setProp(RunConfigs.class.getName(), rcs = new RunConfigs(aSite));
        return rcs;
    }

}