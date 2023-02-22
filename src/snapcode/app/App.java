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
public class App extends AppBase {

    /**
     * Standard main implementation.
     */
    public static void main(final String[] args)
    {
        SwingUtilities.invokeLater(() -> new App());
    }

    /**
     * Constructor.
     */
    public App()
    {
        super();

        // Set App Prefs class
        Prefs prefs = Prefs.getPrefsForName("SnapCodePro");
        Prefs.setDefaultPrefs(prefs);

        // Install Exception reporter
        ExceptionReporter er = new ExceptionReporter("SnapCode");
        er.setToAddress("support@reportmill.com");
        er.setInfo("SnapCode Version 1, Build Date: " + SnapUtils.getBuildInfo());
        Thread.setDefaultUncaughtExceptionHandler(er);

        // Show WelcomePanel
        showWelcomePanel();
    }

    /**
     * Override to show WelcomePanel.
     */
    @Override
    public void showWelcomePanel()
    {
        WelcomePanel.getShared().showPanel();
    }

    /**
     * Exits the application.
     */
    @Override
    public void quitApp()
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