/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import snap.util.Prefs;
import snap.util.SnapUtils;
import snap.view.WindowView;
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
        // Hide open WorkspacePane
        WorkspacePane workspacePane = WindowView.getOpenWindowOwner(WorkspacePane.class);
        if (workspacePane != null)
            workspacePane.hide();

        // Flush prefs and exit
        Prefs.getDefaultPrefs().flush();
        System.exit(0);
    }
}