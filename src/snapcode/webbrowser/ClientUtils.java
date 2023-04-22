/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import java.io.File;
import java.util.*;
import snap.util.FileUtils;
import snapcode.util.Settings;
import snap.util.SnapUtils;
import snap.util.StringUtils;
import snap.web.*;

/**
 * Some utility methods for Snap.
 */
public class ClientUtils {

    // The file holding SnapCode settings
    private static WebFile _userLocalSettingsFile;

    /**
     * Returns the settings.
     */
    public static Settings getUserLocalSettings()
    {
        WebFile userLocalSettingsFile = getUserLocalSettingsFile();
        return Settings.get(userLocalSettingsFile);
    }

    /**
     * Saves the settings.
     */
    public static void saveUserLocalSettings()
    {
        WebFile userLocalSettingsFile = getUserLocalSettingsFile();
        try { userLocalSettingsFile.save(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Returns the settings file.
     */
    private static WebFile getUserLocalSettingsFile()
    {
        if (_userLocalSettingsFile != null) return _userLocalSettingsFile;
        return _userLocalSettingsFile = getUserLocalSettingsFileImpl();
    }

    /**
     * Returns the settings file.
     */
    private static WebFile getUserLocalSettingsFileImpl()
    {
        // Get native settings file
        File dir = FileUtils.getUserHomeDir("SnapCode", true);
        File file = new File(dir, "SnapUserLocal.settings");

        // Get URL and web file
        WebURL settingsFileURL = WebURL.getURL(file);
        WebFile settingsFile = settingsFileURL.getFile();
        if (settingsFile == null)
            settingsFile = settingsFileURL.createFile(false);

        // Return
        return settingsFile;
    }

    /**
     * Sets login info for given URL (if different than what has been previously saved for URL).
     *
     * @return Whether login info was different than URL data source.
     */
    public static boolean setAccess(WebSite aSite)
    {
        String sid = aSite.getURL().getString();
        String un1 = aSite.getUserName(), pw1 = aSite.getPassword();
        String un2 = getUserName(sid), pw2 = getPassword(sid);
        if (!SnapUtils.equals(un1, un2) || !SnapUtils.equals(pw1, pw2)) {
            aSite.setUserName(un2);
            aSite.setPassword(pw2);
            return true;
        }
        return false;
    }

    /**
     * Sets the access info for given URL to system.
     */
    public static void setAccess(WebSite aSite, String aUserName, String aPW)
    {
        String sid = aSite.getURL().getString();
        setUserName(sid, aUserName);
        setPassword(sid, aPW);
        saveUserLocalSettings();
    }

    /**
     * Returns the user name for (site) URL, if previously entered/recorded.
     */
    public static String getUserName(String aSourceId)
    {
        Map map = getKnownSiteMap(aSourceId, false);
        return map != null ? (String) map.get("User") : null;
    }

    /**
     * Sets the UserName for given (site) URL.
     */
    private static void setUserName(String aSourceId, String aString)
    {
        getKnownSiteMap(aSourceId, true).put("User", aString);
    }

    /**
     * Returns the password for (site) URL, if previously entered/recorded.
     */
    public static String getPassword(String aSourceId)
    {
        Map map = getKnownSiteMap(aSourceId, false);
        return map != null ? (String) map.get("PW") : null;
    }

    /**
     * Sets the password for (site) URL.
     */
    private static void setPassword(String aSourceId, String aString)
    {
        getKnownSiteMap(aSourceId, true).put("PW", aString);
    }

    /**
     * Returns the user name for a site/host, if previously entered/recorded.
     */
    public static String getHostUserName(String aURL)
    {
        List<Map> maps = getKnownSitesMaps(false);
        if (maps == null) return null;
        for (Map map : maps)
            if (StringUtils.startsWithIC((String) map.get("URL"), aURL))
                return (String) map.get("User");
        return null;
    }

    /**
     * Returns a list of known sites.
     */
    public static List<String> getKnownSites()
    {
        List<Map> ksMaps = getKnownSitesMaps(false);
        if (ksMaps == null) return Collections.emptyList();
        List<String> ksNames = new ArrayList<>(ksMaps.size());
        for (Map map : ksMaps)
            ksNames.add((String) map.get("URL"));
        return ksNames;
    }

    /**
     * Returns a list of known sites.
     */
    private static Map getKnownSiteMap(String aURL, boolean doCreate)
    {
        List<Map> maps = getKnownSitesMaps(doCreate);
        if (maps == null)
            return null;

        for (Map map : maps)
            if (StringUtils.equalsIC(aURL, (String) map.get("URL")))
                return map;
        if (doCreate) {
            Map map = new HashMap<>();
            map.put("URL", aURL);
            maps.add(map);
            return map;
        }
        return null;
    }

    /**
     * Returns a list of known sites.
     */
    private static List getKnownSitesMaps(boolean doCreate)
    {
        return getUserLocalSettings().getList("KnownSites", doCreate);
    }
}