/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import snap.util.Prefs;
import snap.util.SnapUtils;
import snap.viewx.ExceptionReporter;
import javax.swing.*;

/**
 * SnapCode Application entry point.
 */
public class App {

    /**
     * Main method to run panel.
     */
    public static void main(final String[] args)
    {
        // Mac specific stuff
        //if (SnapUtils.isMac) new AppleAppHandler().init();

        // Config/init JavaFX and invoke real main on event thread
        SwingUtilities.invokeLater(() -> new App(args));
    }

    /**
     * Main method to run panel.
     */
    public App(String[] args)
    {
        // Set App Prefs class
        Prefs prefs = Prefs.getPrefsForName("SnapCodePro");
        Prefs.setDefaultPrefs(prefs);

        // Install Exception reporter
        ExceptionReporter er = new ExceptionReporter("SnapCode");
        er.setToAddress("support@reportmill.com");
        er.setInfo("SnapCode Version 1, Build Date: " + SnapUtils.getBuildInfo());
        Thread.setDefaultUncaughtExceptionHandler(er);

        // Show open data source panel
        WelcomePanel.getShared().setOnQuit(() -> quitApp());
        WelcomePanel.getShared().showPanel();
    }

    /**
     * Exits the application.
     */
    public static void quitApp()
    {
        SwingUtilities.invokeLater(() -> quitAppImpl());
    }

    /**
     * Exits the application (real version).
     */
    private static void quitAppImpl()
    {
        if (AppPane.getOpenAppPane() != null) AppPane.getOpenAppPane().hide();
        Prefs.getDefaultPrefs().flush();
        System.exit(0);
    }

//    /**
//     * A class to handle apple events.
//     */
//    private static class AppleAppHandler implements PreferencesHandler, QuitHandler {
//
//        /**
//         * Initializes Apple Application handling.
//         */
//        public void init()
//        {
//            //System.setProperty("apple.laf.useScreenMenuBar", "true"); // 1.4
//            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "SnapCode");
//            com.apple.eawt.Application app = com.apple.eawt.Application.getApplication();
//            app.setPreferencesHandler(this);
//            app.setQuitHandler(this);
//            _appHand = this;
//        }
//
//        /**
//         * Handle Preferences.
//         */
//        public void handlePreferences(PreferencesEvent arg0)
//        {
//            AppPane appPane = AppPane.getOpenAppPane();
//            if (appPane == null) return;
//            appPane.getBrowser().setFile(appPane.getRootSite().getRootDir());
//        }
//
//        /**
//         * Handle QuitRequest.
//         */
//        public void handleQuitRequestWith(QuitEvent arg0, QuitResponse arg1)
//        {
//            App.quitApp();
//        }
//    }
//
//    static AppleAppHandler _appHand;

}